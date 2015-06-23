// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * TimezoneFactor.java
 */

package clarus.cs;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.SimpleTimeZone;

/**
 * Provides a means of querying the database for timezone information, storing
 * these records, and converting them to the a more standard Java timezone
 * format {@link SimpleTimeZone} through the use of {@link TimezoneRecord}
 *
 */
public class TimezoneFactory
{
    /**
     * Format of the timezone query:
     * <pre>
     *  {id}, {offset}, {timezone name}, {start month}, {start day},
     *  {start day of week}, {start time}, {end month}, {end day},
     *  {end day of week}, {end time}, {dst Savings}
     */
	private static String TIMEZONE_QUERY = "SELECT id, rawOffset, tzName, " + 
		"startMonth, startDay, startDayOfWeek, startTime, " + 
		"endMonth, endDay, endDayOfWeek, endTime, dstSavings FROM timezone";

    /**
     * Used to resolve date names.
     *
     * @see java.util.Calendar
     */
	private static Class CALENDAR_CLASS;

    /**
     * Used to save the timezone format strings from the database query.
     */
	private ArrayList<TimezoneRecord> m_oTimezones = 
		new ArrayList<TimezoneRecord>();

    /**
     *  Used for searching the {@code TimezoneRecord} list.
     */
	private TimezoneRecord m_oSearchTimezone = new TimezoneRecord();
	
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code TimezoneFactory}
	 * </p>
	 */
	private TimezoneFactory()
	{
	}

	/**
     * Initializes the static calendar class. Queries the timezone-database.
     * Saves the timezone format strings by their id into the 
     * {@code TimezoneRecord} list {@code m_oTimezones}.
     *
     * @param iConnection Connection to the database, assumes connection has
     * been established, and is ready for queries.
     */
	TimezoneFactory(Connection iConnection)
	{
		try
		{
			// initialize the static calendar class used to resolve date names
			if (CALENDAR_CLASS == null)
				CALENDAR_CLASS = Class.forName("java.util.Calendar");
			
			ResultSet iResultSet = iConnection.createStatement().
				executeQuery(TIMEZONE_QUERY);
			
			// save the timestamp format strings by their id
			while (iResultSet.next())
				m_oTimezones.add(new TimezoneRecord(iResultSet));

			iResultSet.close();
			Collections.sort(m_oTimezones);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	
	/**
     * Creates a {@see SimpleTimeZone} object from the {@see TimezoneRecord}
     * stored at the supplied index ({@code nIndex}) in the
     * {@code TimezoneRecord} list
     * ({@see TimezoneFactory#m_oTimezones m_oTimezones})
     *
     * @param nIndex The index of the {@code TimezoneRecord} object.
     *
     * @return
     * <pre>
     * null - The list does not contain the record.
     * else - SimpleTimezone object created from the record
     *        {@code m_oTimezones[nIndex]}.
     * </pre>
     */
	public SimpleTimeZone createSimpleTimeZone(int nIndex)
	{
		m_oSearchTimezone.setId(nIndex);		
		nIndex = Collections.binarySearch(m_oTimezones, m_oSearchTimezone);
		if (nIndex < 0)
			return null;
		
		return m_oTimezones.get(nIndex).createSimpleTimezone();
	}
	
	/**
     * Provides a means of gathering, storing, and sorting timezone records from
     * a database {@code ResultSet}. These records can then be used directly to
     * create {@code SimpleTimeZone} objects.
     *
     * <p>
     * Implements {@code Comparable<TimezoneRecord>} enforcing an order on lists
     * of {@code TimezoneRecord} objects.
     * </p>
     *
     * @see SimpleTimeZone
     */
	private class TimezoneRecord implements Comparable<TimezoneRecord>
	{
        /**
         * Timezone  identification number.
         */
		private int m_nId;
        /**
         * Base timezone offset in ms to GMT.
         */
		private int m_nRawOffset;
        /**
         * Timezone name.
         */
		private String m_sName;
        /**
         * Daylight Savings Time starting month.
         * 
         * @see java.util.Calendar#MONTH MONTH
         */
		private int m_nStartMonth;
        /**
         * Daylight Savings Time starting day of month.
         *
         */
		private int m_nStartDay;
        /**
         * Daylight Savings Time starting day of week.
         *
         * @see java.util.Calendar#DAY_OF_WEEK DAY_OF_WEEK
         */
		private int m_nStartDayOfWeek;
        /**
         * Daylight Savings Time starting time in local time.
         */
		private int m_nStartTime;
        /**
         * Daylight Savings Time end month.
         *
         * @see java.util.Calendar#MONTH MONTH
         */
		private int m_nEndMonth;
        /**
         * Daylight Savings Time end day of month.
         */
		private int m_nEndDay;
        /**
         * Daylight Savings Time end day of week.
         *
         * @see java.util.Calendar#DAY_OF_WEEK DAY_OF_WEEK
         */
		private int m_nEndDayOfWeek;
        /**
         * Daylight Savings Time ending time in local time.
         */
		private int m_nEndTime;
        /**
         * Amount of time in ms saved during daylight savings time.
         */
		private int m_nDstSavings;
		
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code TimezoneRecord}
		 * </p>
         */
		private TimezoneRecord()
		{
		}
		
		/**
         * Initializes attributes to values obtained from the supplied result
         * set.
         * 
         * @param iResultSet The result set to gather the data from. Assumes
         * the query has been made and assigned to the result set.
         */
		private TimezoneRecord(ResultSet iResultSet)
		{
			try
			{
				m_nId = iResultSet.getInt(1);
				m_nRawOffset = iResultSet.getInt(2);
				m_sName = iResultSet.getString(3);
				m_nStartMonth = introspect(iResultSet.getString(4));
				m_nStartDay = iResultSet.getInt(5);
				m_nStartDayOfWeek = introspect(iResultSet.getString(6));
				m_nStartTime = iResultSet.getInt(7);
				m_nEndMonth = introspect(iResultSet.getString(8));
				m_nEndDay = iResultSet.getInt(9);
				m_nEndDayOfWeek = introspect(iResultSet.getString(10));
				m_nEndTime = iResultSet.getInt(11);
				m_nDstSavings = iResultSet.getInt(12);
			}
			catch (Exception oException)
			{
				oException.printStackTrace();
			}
		}
		
		/**
         * Converts a {@see java.util.Calendar} fieldname to its corresponding
         * integer value.
         *
         * @param sFieldName The field name to convert.
         *
         * @return Corresponding integer.
         *
         * @throws java.lang.Exception
         */
		private int introspect(String sFieldName) throws Exception
		{
			if (sFieldName == null || sFieldName.length() == 0)
				return 0;
			
			int nMultiplier = 1;
			if (sFieldName.startsWith("-"))
			{
				// preserve the sign information from the specified field name
				nMultiplier = -1;
				// remove the - from the string for the lookup operation
				sFieldName = sFieldName.substring(1);
			}
			Field oField = CALENDAR_CLASS.getField(sFieldName);
			
			// apply the proper sign to the field value
			return (nMultiplier * oField.getInt(oField));
		}
		
		/**
         * Set the {@code m_nId} attribute value.
         *
         * @param nId The value to assign the attribute value to.
         */
		void setId(int nId)
		{
			m_nId = nId;
		}
		
		/**
         * Creates a {@see SimpleTimeZone} object with the corresponding
         * {@code TimezoneRecord} attributes.
         *
         * @return The newly created {@code SimpleTimeZone} object.
         *
         * @see SimpleTimeZone
         */
		SimpleTimeZone createSimpleTimezone()
		{
			if (m_nDstSavings == 0)
				return new SimpleTimeZone(m_nRawOffset, m_sName);
			
			return new SimpleTimeZone(m_nRawOffset, m_sName, m_nStartMonth, 
				m_nStartDay, m_nStartDayOfWeek, m_nStartTime, m_nEndMonth, 
				m_nEndDay, m_nEndDayOfWeek, m_nEndTime, m_nDstSavings);
		}
		

        /**
         * Compares <i> this </i> {@code TimezoneRecord} to the given
         * {@code TimezoneRecord}.
         *
         * <p>
         * Required method for implementation of the interface class
         * {@see Comparable}.
         * </p>
         *
         * @param oTimezoneRecord The object to compare to <i> this </i>
         *
         * @return 0 if they're equivalent.
         */
		public int compareTo(TimezoneRecord oTimezoneRecord)
		{
			return (m_nId - oTimezoneRecord.m_nId);
		}
	}
}
