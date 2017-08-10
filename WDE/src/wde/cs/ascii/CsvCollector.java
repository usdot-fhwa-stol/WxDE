// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsvCollector.java
 */

package wde.cs.ascii;

import org.apache.log4j.Logger;
import wde.cs.CsMgr;
import wde.cs.ICollector;
import wde.dao.ObservationDao;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.util.MathUtil;
import wde.util.Text;
import wde.util.io.CharTokenizer;
import wde.util.net.NetConn;

import java.sql.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

/**
 * Provides a means of collecting data from CSV files, and converting them to
 * observation objects, which are then added to the Obs-cache.
 * <p/>
 * <p>
 * Implements {@code ICollector} to perform the collection and processing of
 * csv observations from contributing agencies.
 * </p>
 */
public class CsvCollector implements ICollector {
    private static final Logger logger = Logger.getLogger(CsvCollector.class);
    /**
     * Time to wait, in seconds, after collection attempt occurs.
     */
    protected int m_nDelay;
    /**
     * Latitude read from input for mobile data
     */
    protected int m_nLat;
    /**
     * Longitude read from input for mobile data
     */
    protected int m_nLon;
    /**
     * Elevation read from input for mobile data
     */
    protected short m_tElev;
    /**
     * Sensor index, retrieved from database.
     */
    protected int m_nSensorIndex;
    /**
     * Number of lines to skip at beginning of file.
     */
    protected int m_nSkipLines;
    /**
     * Retry collection attempt flag.
     */
    protected boolean m_bRetry;
    /**
     * Default platform code used when source does not contain this information
     */
    protected String m_sDefaultPlatformCode;
    /**
     * Timestamp accumulates pieces of the timestamp that tracks the
     * data collection time.
     */
    protected StringBuilder m_sTimestamp = new StringBuilder();
    /**
     * Platform code accumulates pieces of the platform code used to
     * lookup the location and sensor information
     */
    protected StringBuilder platformCodeSB = new StringBuilder();
    /**
     * Reference to Collector Service Manager for elevation check
     */
    protected CsMgr m_oCsMgr;
    /**
     * Interface to a current platform resolved from completed platform code
     */
    protected IPlatform m_iPlatform;
    /**
     * Last date/time of collection. Millisecond precision.
     */
    protected Date m_oLastCollection = new Date();
    /**
     * Timestamp format.
     */
    protected DateFormat m_oTsFormat;
    /**
     * Timestamp to use for filename of input file.
     */
    protected DateFormat m_oFilepath;
    /**
     * List of {@link DataValue} used to wrap collection of different csv
     * data-types through the use of
     * {@link DataValue#readData(java.lang.StringBuilder) } on extensions of
     * {@code DataValue}
     */
    protected ArrayList<DataValue> m_oColumns = new ArrayList<DataValue>();
    /**
     * Parent Collector service object.
     */
    protected CsvSvc m_oCsvSvc;
    /**
     * Used for processing objects with delimiter and newline strings.
     */
    protected CharTokenizer m_oTokenizer;
    protected String inputLine = null;
    /**
     * Instance of platform observation resolving service.
     */
    protected PlatformDao platformDao = PlatformDao.getInstance();
    /**
     * Instance of Sensor observation resolving service.
     */
    protected SensorDao sensorDao = SensorDao.getInstance();
    /**
     * Instance of observation resolving service.
     */
    protected ObservationDao obsDao = ObservationDao.getInstance();
    /**
     * Collector service Id.
     */
    int m_nId;
    /**
     * List of contributors that provides context for platform searches.
     */
    int[] m_nContribIds;
    /**
     * Query format string:
     * <pre>    {columnId}, {observation type id}, {sensor index}, </pre>
     * <pre>    {column width}, {conversion multiplier}, </pre>
     * <pre>    {collector name}, {units}, {values to ignore} </pre>
     */
    private String m_sQuery = "SELECT columnId, obsTypeId, sensorIndex, " +
            "colWidth, multiplier, className, unit, ignoreValues " +
            "FROM conf.csvcoldef WHERE collectorId = ?";

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code CsvCollector}
     * </p>
     */
    protected CsvCollector() {
    }

    /**
     * Initializes {@code CsvCollector} attributes. Sets up the timezone,
     * and timestamp formats. Initializes the character tokenizer with the
     * supplied delimiter, and newline characters. Prepares and executes
     * the query ({@code m_sQuery}), and creates the corresponding
     * {@code DataValue} handler for each column, which is then added to the
     * list of {@code DataValue} handlers ({@code m_oColumns}), then sorts
     * the list in ascending order, based off the column identifier.
     *
     * @param nId          collector service ID
     * @param nDelay       time to wait, in milliseconds, after collection time
     *                     occurs.
     * @param bRetry       retry collection attempt flag.
     * @param nCollectTzId collector timezone id.
     * @param nContentTzId content timezone id.
     * @param nTimestampId timestamp format id.
     * @param nSkipLines   configured lines to skip at the beginning of the file.
     * @param sDelimiter   configured delimiter marker.
     * @param sNewline     configured line terminator.
     * @param sFilepath    string describing timestamp formatted filename.
     * @param platformCode platform code of the platform where the obs comes from
     * @param nContribIds  array of current contributor identifiers.
     * @param oCsMgr       main {@code CsMgr} instance. Ready to manage collector
     *                     services.
     * @param oCsvSvc      parent CSV collector service, ready for query.
     * @param iConnection  connection to SQL, assumes it is connected and ready
     *                     to read.
     */
    protected CsvCollector(int nId, int nDelay, boolean bRetry,
                           int nCollectTzId, int nContentTzId, int nTimestampId, int nSkipLines,
                           String sDelimiter, String sNewline, String platformCode,
                           String sFilepath, int[] nContribIds, CsMgr oCsMgr,
                           CsvSvc oCsvSvc, Connection iConnection) {
        m_nId = nId;
        m_sDefaultPlatformCode = platformCode;

        // set the number of seconds to wait after the collection time occurs 
        // and if this information collector should retry on failure
        m_nDelay = nDelay * 1000; // convert seconds to milliseconds
        m_bRetry = bRetry;

        // set up the file content timestamp reader
        m_oTsFormat = oCsMgr.createDateFormat(nTimestampId);
        m_oTsFormat.setTimeZone(oCsMgr.createSimpleTimeZone(nContentTzId));

        // set up the filename timestamp generator
        m_oFilepath = new SimpleDateFormat(sFilepath);
        m_oFilepath.setTimeZone(oCsMgr.createSimpleTimeZone(nCollectTzId));

        // save the number of lines to skip, usually skips over file headers
        m_nSkipLines = nSkipLines;

        // set up the csv processing objects with delimiter and newline strings
        m_oTokenizer = new CharTokenizer(sDelimiter, sNewline);

        m_nContribIds = nContribIds; // save reference to list of contrib ids
        m_oCsvSvc = oCsvSvc; // save the parent collector service object
				m_oCsMgr = oCsMgr;

        // create the handler objects that interpret column information 
        // configuration information is stored in a configuration database
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = iConnection.prepareStatement(m_sQuery);
            ps.setInt(1, nId);
            rs = ps.executeQuery();

            while (rs.next()) {
                DataValue oDataHandler = (DataValue) Class.forName(
                        rs.getString(6)).newInstance();

                int nSensorIndex = rs.getInt(3);
                if (rs.wasNull()) nSensorIndex = -1;

                oDataHandler.init(rs.getInt(1), rs.getInt(2), nSensorIndex,
                        rs.getInt(4), rs.getDouble(5), rs.getString(7),
                        rs.getString(8), oCsMgr, this);

                m_oColumns.add(oDataHandler);
            }

            Collections.sort(m_oColumns);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }

    /**
     * Reads the file {@code m_oFilepath} by column, adding the observations
     * to the parent Collector Service's ({@code m_oCsvSvc}) Obs-cache.
     *
     * @param nContribId The contributor id we're interested in collecting from.
     * @param oNetConn   Input stream, previously connected.
     * @param lTimestamp Collection time, not delay-adjusted.
     */
    public void collect(NetConn oNetConn, long lTimestamp) {
        logger.debug("collect() invoked");

        // adjust the task execution time by the delay to derive
        // the actual collection time that generates filenames
        lTimestamp -= m_nDelay;

        // verify that this is a unique collection request
        if (m_oLastCollection.getTime() == lTimestamp)
            return;

        // collect column data here
        m_oLastCollection.setTime(lTimestamp);

        try {
            String path = m_oFilepath.format(m_oLastCollection);
            logger.info("Openning " + path);
            if (oNetConn.open(path)) {
                // skip the configured number of header lines
                m_oTokenizer.setInput(oNetConn);
                int nIndex = m_nSkipLines;
                while (nIndex-- > 0 && m_oTokenizer.nextSet()) ;

                // process each record
                int nColumn = 0;
                StringBuilder sColumnBuffer = new StringBuilder();
                while (m_oTokenizer.nextSet()) {
                    inputLine = m_oTokenizer.toString();

                    // clear the required platform information
                    m_sTimestamp.setLength(0);
                    platformCodeSB.setLength(0);
                    m_iPlatform = null;
                    m_nLat = Integer.MIN_VALUE;
                    m_nLon = Integer.MIN_VALUE;
                    m_tElev = Short.MIN_VALUE;

                    // process each column for the record
                    nColumn = 0;
                    while (nColumn < m_oColumns.size() &&
                            m_oTokenizer.hasTokens()) {
                        m_oTokenizer.nextToken(sColumnBuffer);
                        // remove whitespace characters as necessary
                        Text.removeWhitespace(sColumnBuffer);

                        // the column is set to null when there is no data
                        m_oColumns.get(nColumn++).readData(sColumnBuffer);
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
        logger.debug("collect() returning");
    }


    /**
     * Attempts to convert the observation timing information to a timestamp.
     * A single string is parsed, but may have been used to accumulate
     * pieces of a timestamp.
     *
     * @return millisecond value of timestamp
     */
    protected long getTimestamp() {
        long lTimestamp = 0L;
        if (m_sTimestamp.length() > 0) {
            try {
                // attempt to parse the contents of the timestamp
                lTimestamp = m_oTsFormat.parse(m_sTimestamp.toString()).getTime();
            } catch (ParseException pe) {
                logger.error("Error parsing: " + m_sTimestamp.toString());
                logger.error("Original line: " + inputLine);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("*=*= " + e.getMessage());
            }
        }
        return lTimestamp;
    }

    /**
     * Set the timestamp attribute {@code m_lTimestamp} to the time
     * value counter contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the time value counter.
     */
    public void setTimestamp(StringBuilder sBuffer) {
        // insert configured delimiter to recombine timestamp pieces
        if (m_sTimestamp.length() > 0)
            m_sTimestamp.append(" ");

        m_sTimestamp.append(sBuffer);
    }

    /**
     * Attempts to get the platform based on the platform code from the
     * {@code PlatformDao}. This method is used to find the platform needed to
     * create a valid observation.
     *
     * @return Platform
     */
    IPlatform getPlatform() {
        if (m_iPlatform != null)
            return m_iPlatform;

        String platformCode = m_sDefaultPlatformCode;
        if (platformCodeSB.length() > 0)
            platformCode = platformCodeSB.toString();

        platformCode = platformCode.trim();
        IPlatform platform = null;

        int nIndex = m_nContribIds.length; // derive the platform for this observation
        while (nIndex-- > 0 && platform == null)
            platform = platformDao.
                    getPlatformForContribId(m_nContribIds[nIndex], platformCode);

        m_iPlatform = platform; // save platform reference
        return platform;
    }

    /**
     * If the platform is found for the observation, the corresponding sensor
     * is retrieved from the {@code Sensors} cache. Finally, the Obs set for the
     * supplied observation type id is retrieved, and the observation is added.
     *
     * @param nObsTypeId
     * @param nSensorIndex
     * @param dValue
     */
    void createObs(int nObsTypeId, int nSensorIndex, double dValue) {
        logger.debug("createObs() invoked");

        // an observation must have at least a timestamp and platform
        // the sensor index is optional as it defaults to zero
        long lTimestamp = getTimestamp();

        long now = System.currentTimeMillis();
        if (lTimestamp == 0)
            return;

        boolean obsAdded = false;
        IPlatform platform = getPlatform();
        if (platform != null) {
            // if a platform is available, attempt to find the correct sensor
            ISensor iSensor = null;

            // override the default sensor index as needed
            if (nSensorIndex < 0)
                nSensorIndex = m_nSensorIndex;

            // override default geo-coordinates if not read from file
            if (m_nLat < -90000000 && m_nLon < -180000000) {
                m_nLat = MathUtil.toMicro(platform.getLocBaseLat());
                m_nLon = MathUtil.toMicro(platform.getLocBaseLong());
                m_tElev = (short) platform.getLocBaseElev();
            }

            iSensor = sensorDao.getSensor(platform.getId(), nObsTypeId, nSensorIndex);

            // if both a platform and sensor are found then get the obs set
            IObsSet iObsSet = null;
            if (iSensor != null)
                iObsSet = m_oCsvSvc.getObsSet(nObsTypeId);

            // now that we have all three components, save the observation
            if (iObsSet != null)
						{
							m_tElev = m_oCsMgr.checkElev(m_nLat, m_nLon, m_tElev);
              iObsSet.addObs(1, iSensor.getId(), lTimestamp, now, 
								m_nLat, m_nLon, m_tElev, dValue);
              obsAdded = true; // source is 1 for WxDE
            }
        }

        if (!obsAdded && m_nContribIds[0] != 4) // ignore missing NWS platforms
        {
            if (nSensorIndex < 0) // override default sensor index as needed
                nSensorIndex = m_nSensorIndex;

            int nContribId = -m_oCsvSvc.m_nId; // save the negative value of
            if (m_nContribIds.length == 1) // the collector service for sets
                nContribId = m_nContribIds[0]; // otherwise, use the contrib id

            long validWindow = ObsMgr.getInstance().getLifeTime();
            long lForecast = now + validWindow;
            long lExpired = now - validWindow;

            if (lTimestamp >= lExpired && lTimestamp <= lForecast) {

                // Add the obs in invalidObs table, 1 for WxDE
                obsDao.insertInvalidObservation(1, nContribId, platformCodeSB
                                .toString().trim(), nObsTypeId, nSensorIndex,
                        new Timestamp(lTimestamp), new Timestamp(now), m_nLat, m_nLon, dValue, null);
            }
        }
    }

    /**
     * Convert the latitude attribute {@code m_nLat} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
    void setLat(StringBuilder sBuffer) {
        try {
            m_nLat = MathUtil.toMicro(Double.parseDouble(sBuffer.toString()));
        } catch (Exception e) {
            m_nLat = Integer.MIN_VALUE;
        }
    }

    /**
     * Convert the longitude attribute {@code m_nLon} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
    void setLon(StringBuilder sBuffer) {
        try {
            m_nLon = MathUtil.toMicro(Double.parseDouble(sBuffer.toString()));
        } catch (Exception e) {
            m_nLon = Integer.MIN_VALUE;
        }
    }

    /**
     * Convert the elevation attribute {@code m_tElev} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
    void setElev(StringBuilder sBuffer) {
        try {
            m_tElev = (short) Double.parseDouble(sBuffer.toString());
        } catch (Exception e) {
            m_tElev = Short.MIN_VALUE;
        }
    }

    /**
     * Set the sensor index attribute {@code m_nSensorIndex} to the integer
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the sensor index integer.
     */
    void setSensorIndex(StringBuilder sBuffer) {
        try {
            m_nSensorIndex = Integer.parseInt(sBuffer.toString());
        } catch (Exception oException) {
            m_nSensorIndex = 0;
        }
    }

    /**
     * Set the platform code attribute {@code platformCodeSB}. Hyphenates the
     * value contained in the supplied string buffer, and appends it to the
     * platform code attribute.
     *
     * @param sBuffer The string containing the non-hyphenated platform code.
     */
    void setPlatformCode(StringBuilder sBuffer) {
        // insert hyphen for combination system and platform codes
        if (platformCodeSB.length() > 0)
            platformCodeSB.append("-");

        platformCodeSB.append(sBuffer);
    }
}
