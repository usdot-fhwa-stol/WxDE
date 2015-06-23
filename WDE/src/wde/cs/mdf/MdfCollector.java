// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file MdfCollector.java
 */

package wde.cs.mdf;

import wde.cs.CsMgr;
import wde.cs.ascii.CsvCollector;
import wde.cs.ascii.CsvSvc;
import wde.cs.ascii.DataValue;
import wde.util.Text;
import wde.util.net.NetConn;

import java.sql.Connection;

/**
 * Provides a means of collecting data from MDF files, and converting them to
 * observation objects, which are then added to the Obs-cache.
 * <p/>
 * <p>
 * Implements {@code ICollector} to perform the collection and processing of
 * csv observations from contributing agencies.
 * </p>
 * <p>
 * Extends {@code CsvCollector} to perform data collection activities. Only
 * difference being MDF files contain a timestamp in the header, which is to
 * be used for all observations in the file.
 * </p>
 */
public class MdfCollector extends CsvCollector {
    /**
     * Stores the date information from the file header.
     */
    protected long m_lDate;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code MdfCollector}
     * </p>
     */
    protected MdfCollector() {
    }

    /**
     * Initializes {@code MdfCollector} attributes. Sets up the timezone,
     * and timestamp formats. Initializes the character tokenizer with the
     * supplied delimiter, and newline characters. Prepares and executes
     * the query ({@code m_sQuery}), and creates the corresponding
     * {@code DataValue} handler for each column, which is then added to the
     * list of {@code DataValue} handlers ({@code m_oColumns}), then sorts
     * the list in ascending order, based off the column identifier.
     * <p/>
     * <p>
     * Calls base class constructor ({@link CsvCollector#CsvCollector})
     * </p>
     *
     * @param nId           collector service ID
     * @param nDelay        time to wait, in milliseconds, after collection time
     *                      occurs.
     * @param bRetry        retry collection attempt flag.
     * @param nCollectTzId  collector timezone id.
     * @param nContentTzId  content timezone id.
     * @param nTimestampId  timestamp format id.
     * @param nSkipLines    configured lines to skip at the beginning of the file.
     * @param sDelimiter    configured delimiter marker.
     * @param sNewline      configured line terminator.
     * @param sPlatformCode platformCode
     * @param sFilepath     string describing timestamp formatted filename.
     * @param oCsMgr        main {@code CsMgr} instance. Ready to manage collector
     *                      services.
     * @param oCsvSvc       parent CSV collector service, ready for query.
     * @param iConnection   connection to SQL, assumes it is connected and ready
     *                      to read.
     */
    protected MdfCollector(int nId, int nDelay, boolean bRetry,
                           int nCollectTzId, int nContentTzId, int nTimestampId, int nSkipLines,
                           String sDelimiter, String sNewline, String sPlatformCode,
                           String sFilepath, int[] nContribIds, CsMgr oCsMgr, CsvSvc oCsvSvc,
                           Connection iConnection) {
        super
                (
                        nId, nDelay, bRetry, nCollectTzId, nContentTzId,
                        nTimestampId, nSkipLines, sDelimiter, sNewline, sPlatformCode,
                        sFilepath, nContribIds, oCsMgr, oCsvSvc, iConnection
                );
    }

    /**
     * Similar to base class collect method
     * ({@link CsvCollector#collect(int, NetConn, long)}) only there is only one
     * timestamp for all observations contained in the file, which is read from
     * the header.
     * <p/>
     * <p>
     * Reads the file {@code m_oFilepath} by column, adding the observations
     * to the parent Collector Service's ({@code m_oCsvSvc}) Obs-cache.
     * </p>
     *
     * @param nContribId The contributor id we're interested in collecting from.
     * @param oNetConn   Input stream, previously connected.
     * @param lTimestamp Collection time, not delay-adjusted.
     */
    @Override
    public void collect(NetConn oNetConn, long lTimestamp) {
        // adjust the task execution time by the delay to derive
        // the actual collection time that generates filenames
        lTimestamp -= m_nDelay;

        // verify that this is a unique collection request
        if (m_oLastCollection.getTime() == lTimestamp)
            return;

        // collect column data here
        m_oLastCollection.setTime(lTimestamp);

        try {
            if (oNetConn.open(m_oFilepath.format(m_oLastCollection))) {
                // the column buffer is needed to read the timestamp
                StringBuilder sColumnBuffer = new StringBuilder();

                // read the header
                m_oTokenizer.setInput(oNetConn);
                int nIndex = m_nSkipLines;
                while (nIndex-- > 0 && m_oTokenizer.nextSet()) {
                    // the second header record contains the date portion of
                    // the timestamp that applies to all the observations
                    if (nIndex == 1) {
                        // throw away the first 5 characters
                        m_oTokenizer.nextToken(5, sColumnBuffer);
                        // read and parse the date portion of the timestamp
                        m_oTokenizer.nextToken(10, sColumnBuffer);
                        m_lDate = m_oTsFormat.
                                parse(sColumnBuffer.toString()).getTime();
                    }
                }

                // process each record
                int nColumn = 0;
                while (m_oTokenizer.nextSet()) {
                    // reset the minute-of-day information
                    m_sTimestamp.setLength(0);
                    // clear the station code
                    platformCodeSB.setLength(0);
                    m_iPlatform = null;
                    m_nLat = Integer.MIN_VALUE;
                    m_nLon = Integer.MIN_VALUE;
                    m_tElev = Short.MIN_VALUE;

                    // process each column for the record
                    nColumn = 0;
                    while (nColumn < m_oColumns.size()) {
                        DataValue oDataValue = m_oColumns.get(nColumn++);
                        if (m_oTokenizer.hasTokens()) {
                            m_oTokenizer.
                                    nextToken(oDataValue.getWidth(), sColumnBuffer);
                            // remove whitespace characters as necessary
                            Text.removeWhitespace(sColumnBuffer);
                            // the column is set to null when there is no data
                            oDataValue.readData(sColumnBuffer);
                        } else
                            nColumn = m_oColumns.size();
                    }

                    // process all of the observations for the current record
                    for (nColumn = 0; nColumn < m_oColumns.size(); nColumn++)
                        m_oColumns.get(nColumn).writeData();
                }

                oNetConn.close();
            } else
                m_oLastCollection.setTime(0);
        } catch (Exception oException) {
            try {
                oNetConn.close();
            } catch (Exception oIOException) {
            }

            m_oLastCollection.setTime(0);
        }
    }

    /**
     * Attempts to convert the observation timing information to a timestamp.
     * This MDF collector has a single date in the header and the timestamp
     * information is derived from that date and the observation minute.
     *
     * @return millisecond value of timestamp
     */
    @Override
    public long getTimestamp() {
        long lTimestamp = 0L;
        if (m_sTimestamp.length() > 0) {
            try {
                long lMinuteOfDay = Integer.parseInt(m_sTimestamp.toString());
                lTimestamp = m_lDate + lMinuteOfDay * 60000;
            } catch (Exception oException) {
            }
        }
        return lTimestamp;
    }
}
