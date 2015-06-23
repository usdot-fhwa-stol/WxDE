// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ClimateRecords.java
 */
package clarus.emc;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.Comparator;

import util.Introsort;

/**
 * Provides means to cache {@code ClimateRecord} objects, as well as modify,
 * access, copy, compare and print these objects contained in the
 * {@code DbCache}.
 *
 * <p>
 * Singleton class only allows one instance of {@code ClimateRecords} which can
 * be accessed by the {@link ClimateRecords#getInstance() } method.
 * </p>
 *
 * <p>
 * Extends {@code DbCache} to allow recurring records to be accessed
 * efficiently.
 * </p>
 */
public class ClimateRecords extends DbCache<ClimateRecord>
{
    /**
     * The grid cell spacing used for finding climate records.
     */
	public static final int PRECISION = 2500000;
    /**
     * Half of the grid cell spacing used to determine the cell boundaries.
     */
	public static final int OFFSET = PRECISION / 2;
    /**
     * The singleton instance of {@code ClimateRecords}.
     */
	private static ClimateRecords g_oInstance = new ClimateRecords();


	/**
     * <b> Accessor </b>
     *
     * @return The singleton instance of {@code ClimateRecords}.
     */
	public static ClimateRecords getInstance()
	{
		return g_oInstance;
	}


	/**
     * <b> Default Constructor </b>
     * <p>
     * Sets up sort algorithm to sort by id. Initializes database query to be
     * of the form:
     * <pre>    {climate id}, {period}, {observation type id}, </pre>
     * <pre>    {min observation record}, {max observation record},</pre>
     * <pre>    {average observation record} </pre>
     * Finally, {@link DbCache#run()} is called on the {@code DbCache}
     * container to begin caching queried records.
     * </p>
     */
	private ClimateRecords()
	{
		// free the secondary array resources
		m_oSort = null;
		
		// set up the primary sort algorithm
		m_oRecordSort = new SortById();
		
		m_sQuery = "SELECT obsTypeId, month, lat, lon, " +
			"lowerValue, upperValue, avgValue FROM climateRecord";
		
		run();
	}
	
	/**
     * Prints {@code ClimateRecords} in a comma delimited manner formatted as
     * follows:
     * <p>
     * {id}, {observation type}, {observation period},
     * {min value}, {max value}, {average value}
     * </p>
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     * @param oPrintWriter output stream to write to, ready for streaming.
     * @param nIndex index of the record in the primary {@code DbCache} record
     *          list.
     */
	protected void toString(PrintWriter oPrintWriter, int nIndex)
	{
		ClimateRecord oClimateRecord = m_oRecords.get(nIndex);

//		oPrintWriter.print("\t\t\t<tr><td>");
		oPrintWriter.print(oClimateRecord.m_nObsTypeId);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oClimateRecord.m_nMonth);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oClimateRecord.m_nLat);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oClimateRecord.m_nLon);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oClimateRecord.m_dMin);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oClimateRecord.m_dMax);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.println(oClimateRecord.m_dAvg);
//		oPrintWriter.println("</td></tr>");
	}

	/**
     * Sets {@code oClimateRecord} attributes to those contained in the current
     * row pointed to by the specified result set.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param iResultSet set containing the {@code ClimateRecord} data.
     * @param oClimateRecord the record whose parameters are to be modified.
     */
	protected void setParameters(ResultSet iResultSet, ClimateRecord oClimateRecord)
	{
		try
		{
			oClimateRecord.m_nObsTypeId = iResultSet.getInt(1);
			// month lookups are zero-based
			oClimateRecord.m_nMonth = iResultSet.getInt(2) - 1;
			oClimateRecord.m_nLat = Stations.toMicro(iResultSet.getDouble(3));
			oClimateRecord.m_nLon = Stations.toMicro(iResultSet.getDouble(4));
			oClimateRecord.m_dMin = iResultSet.getDouble(5);
			oClimateRecord.m_dMax = iResultSet.getDouble(6);
			oClimateRecord.m_dAvg = iResultSet.getDouble(7);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	
	/**
     * Searches the primary {@code DbCache} records list for the record
     * containing the supplied id, period, and observation type.
     * 
     * @param nId id correspoding to the record of interest.
     * @param nPeriod period of the record of interest.
     * @param nObsType observation type of the record of interest.
     * @return the corresponding record in the primary records list, null if
     *   not found.
     */
	public IClimateRecord getClimateRecord(int nObsTypeId, int nMonth,
		int nLat, int nLon)
	{
		ClimateRecord oSearchRecord = m_oLock.readLock();
		
		// calculate the nearest 2.5 degree grid cell from the obs coordinates
		oSearchRecord.m_nObsTypeId = nObsTypeId;
		oSearchRecord.m_nMonth = nMonth;
		// add half of the precision value to offset the initial coordinates
		// then floor the coordinates to the precision
		oSearchRecord.m_nLat = Introsort.floor(nLat + OFFSET, PRECISION);
		oSearchRecord.m_nLon = Introsort.floor(nLon + OFFSET, PRECISION);
		
		oSearchRecord = search(m_oRecords, oSearchRecord, m_oRecordSort);
		m_oLock.readUnlock();
		return oSearchRecord;
	}
	

    /**
     * Compares the min, max, and average values of the supplied
     * {@code ClimateRecord} objects.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oLhs record to compare to {@code oRhs}
     * @param oRhs record to compare to {@code oLhs}
     * @return 
     * <pre> true if the min, max, and average values of {@code oLhs}  </pre>
     * <pre>    match that of {@code oRhs}. </pre>
     * <pre> false otherwise. </pre>
     */
	protected boolean recordsMatch(ClimateRecord oLhs, ClimateRecord oRhs)
	{
		return
		(
			oLhs.m_dMin == oRhs.m_dMin && 
			oLhs.m_dMax == oRhs.m_dMax && 
			oLhs.m_dAvg == oRhs.m_dAvg
		);
	}

	/**
     * Creates a new instance of {@code ClimateRecord}, with the same attribute
     * values as the supplied {@code ClimateRecord}.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oClimateRecord the object to create a copy of.
     * @return The newly created copy of {@code oClimateRecord}.
     */
	protected ClimateRecord copy(ClimateRecord oClimateRecord)
	{
		return new ClimateRecord(oClimateRecord);
	}


   /**
    * This is used to add a container of lockable {@code ClimateRecord} objects
    * to the {@link util.threads.StripeLock} Mutex.
    * <p>
    * Specifies a required {@code DbCache} extension method, which is in turn
    * required for the base class's implementation of the interface class
    * {@code ILockFactory}.
    * </p>
    *
    * @return A new instance of {@code ClimateRecord}
    *
    * @see util.threads.ILockFactory
    * @see util.threads.StripeLock
    */
	@Override
	public ClimateRecord getLock()
	{
		return new ClimateRecord();
	}


	/**
     * Specifies the primary records list sort order.
     * {@link SortById#compare(ClimateRecord, ClimateRecord)} gives priority to
     * the record id, then the observation type, and finally the period.
     *
     * <p>
     * Implements {@code Comparator<ClimateRecord>} to define comparison of
     * {@code ClimateRecord} objects by id.
     * </p>
     */
	private class SortById implements Comparator<ClimateRecord>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortById}
		 * </p>
         */
		private SortById()
		{
		}

        /**
         * Compares {@code ClimateRecord} objects. Gives priority to the record
         * id, then the observation type, and finally the period.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(ClimateRecord oLhs, ClimateRecord oRhs)
		{
			int nIndex = oLhs.m_nObsTypeId - oRhs.m_nObsTypeId;
			if (nIndex != 0)
				return nIndex;

			nIndex = oLhs.m_nMonth - oRhs.m_nMonth;
			if (nIndex != 0)
				return nIndex;

			nIndex = oLhs.m_nLat - oRhs.m_nLat;
			if (nIndex != 0)
				return nIndex;

			return (oLhs.m_nLon - oRhs.m_nLon);
		}
	}
}
