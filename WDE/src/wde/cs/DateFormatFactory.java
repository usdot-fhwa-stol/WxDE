// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file DateFormatFactory.java
 */

package wde.cs;

import wde.util.ISODateFormat;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides a means to gathering Timestamps, and formatting them.
 */
public class DateFormatFactory {
    /**
     * Query format for timestamps:
     * <p/>
     * <pre>
     * {id}, {string format}
     * </pre>
     */
    private static String TIMESTAMP_QUERY =
            "SELECT id, formatstring FROM conf.timestampformat";

    /**
     * List of timestamps to operate on.
     */
    private ArrayList<TimestampRecord> m_oTimestamps =
            new ArrayList<TimestampRecord>();

    /**
     * Used as a key for searching the list of timestamps.
     */
    private TimestampRecord m_oSearchTimestamp = new TimestampRecord();

    /**
     * Queries the SQL database for the set of timestamps, and adds them to the
     * list of timestamps in ascending order based off the difference between
     * their id values.
     *
     * @param iConnection SQL database connection, should be connected prior to
     *                    calls to this method.
     */
    DateFormatFactory(Connection iConnection) {
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            ps = iConnection.prepareStatement(TIMESTAMP_QUERY);
            rs = ps.executeQuery();

            // save the timestamp format strings by their id
            while (rs.next())
                m_oTimestamps.add(new TimestampRecord(rs));

            Collections.sort(m_oTimestamps);
        } catch (Exception oException) {
            oException.printStackTrace();
        }
        try {
            rs.close();
            rs = null;
            ps.close();
            ps = null;
        } catch (SQLException se) {
            // ignore
        }
    }

    /**
     * Finds the timestamp from the list, and creates a default date format
     * based off the JVM's default locale.
     *
     * @param nIndex The index of the timestampt to create a date format from.
     * @return The newly created format.
     * @see DateFormat#DateFormat()
     */
    public DateFormat createDateFormat(int nIndex) {
        m_oSearchTimestamp.setId(nIndex);
        nIndex = Collections.binarySearch(m_oTimestamps, m_oSearchTimestamp);
        if (nIndex < 0)
            return null;

        return m_oTimestamps.get(nIndex).createDateFormat();
    }

    /**
     * Implements {@code Comparable} to allow comparisons and sorting of
     * {@code TimestampRecord}'s via the required
     * {@see TimestampRecord#compareTo} method implementation.
     */
    private class TimestampRecord implements Comparable<TimestampRecord> {
        /**
         * Timestamp ID
         */
        private int m_nId;
        /**
         * Format of the timestamp.
         */
        private String m_sFormat;

        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code TimestampRecord}
         * </p>
         */
        private TimestampRecord() {
        }

        /**
         * Sets the timestamp-id and timestamp-format name.
         *
         * @param rs The resultant set of a database timestamp query.
         */
        private TimestampRecord(ResultSet rs) {
            try {
                m_nId = rs.getInt(1);
                m_sFormat = rs.getString(2);
            } catch (Exception oException) {
                oException.printStackTrace();
            }
        }

        /**
         * Sets the member timestamp-id ({@code m_nId}).
         *
         * @param nId The id number for initialization.
         */
        void setId(int nId) {
            m_nId = nId;
        }

        /**
         * Creates a {@code DateFormat} using the supplied format and
         * the default date format for the default locale.
         *
         * @return The newly created format.
         * @see ISODateFormat#ISODateFormat(String)
         */
        public DateFormat createDateFormat() {
            return new ISODateFormat(m_sFormat);
        }

        /**
         * Required method for implementation of {@see Comparable}. The
         * comparison is based off the difference between the records Id
         * numbers.
         *
         * @param oTimestampRecord
         * @return <blockquote><pre>
         * 0 if they're the same.
         * positive if the first is greater than the second.
         * negative otherwise.
         * </pre></blockquote>
         */
        public int compareTo(TimestampRecord oTimestampRecord) {
            return (m_nId - oTimestampRecord.m_nId);
        }
    }
}
