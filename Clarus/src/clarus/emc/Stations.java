// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Stations.java
 */
package clarus.emc;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Provides means to cache {@code Station} objects, as well as modify,
 * access, copy, compare and print these objects contained in the
 * {@code DbCache}.
 *
 * <p>
 * Singleton class only allows one instance of {@code Stations} which can
 * be accessed by the {@link Stations#getInstance() } method.
 * </p>
 *
 * <p>
 * Extends {@code DbCache} to allow recurring records to be accessed
 * efficiently.
 * </p>
 */
public class Stations extends DbCache<Station>
{
    /**
     * The singleton instance of {@code Stations}
     */
	private static Stations g_oInstance = new Stations();

	/**
     * <b> Accessor </b>
     *
     * @return The singleton instance of {@code Stations}
     */
	public static Stations getInstance()
	{
		return g_oInstance;
	}
	
	/**
     * Initializes base class search and sort algorithms, as well as the
     * database query.
     *
     * <p>
     * Calls base class method {@link DbCache#run()} to begin caching
     * collected data.
     * </p>
     */
	private Stations()
	{
		// set up the sort and search algorithms
		m_oRecordSort = new SortById();
		m_oSearchSort = new SortByContribCode();
		
		m_sQuery = "SELECT t.id, t.stationCode, t.category, t.contribId, " +
			"t.locBaseLat, t.locBaseLong, t.locBaseElev, s.id, s.climateId, " +
			"s.description FROM station t, site s WHERE s.id = t.siteId";
		
		run();
	}
	
	/**
     * Prints {@code Stations} in a comma delimited manner formatted as
     * follows:
     * <p>
     * {station id}, {station code}, {station description}, {contributor id},
     * {latitude}, {longitude}, {elevation}, {site id}, {climate id}
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
		Station oStation = m_oRecords.get(nIndex);

		oPrintWriter.print(oStation.m_nId);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_sCode);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_cCat);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_sDesc);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_nContribId);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_nLat);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_nLon);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_tElev);
		oPrintWriter.print(",");
		oPrintWriter.print(oStation.m_nSiteId);
		oPrintWriter.print(",");
		oPrintWriter.println(oStation.m_nClimateId);
	}
	
	/**
     * Sets {@code oStation} attributes to those contained in the current
     * row pointed to by the specified result set.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param iResultSet set containing the {@code Station} data.
     * @param oStation the record whose parameters are to be modified.
     */
	protected void setParameters(ResultSet iResultSet, Station oStation)
	{
		try
		{
			oStation.m_nId = iResultSet.getInt(1);
			oStation.m_sCode = iResultSet.getString(2);
			oStation.m_cCat = iResultSet.getString(3).charAt(0);
			oStation.m_nContribId = iResultSet.getInt(4);
			oStation.m_nLat = toMicro(iResultSet.getDouble(5));
			oStation.m_nLon = toMicro(iResultSet.getDouble(6));
			oStation.m_tElev = (short)iResultSet.getDouble(7);
			oStation.m_nSiteId = iResultSet.getInt(8);
			oStation.m_nClimateId = iResultSet.getInt(9);
            oStation.m_sDesc = iResultSet.getString(10);
			if (oStation.m_sDesc == null) // description not required
				oStation.m_sDesc = "";
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}

	/**
     * Searches the primary {@code DbCache} records list for the record
     * containing the supplied station id.
     *
     * @param nId station id corresponding to the record of interest.
     * @return the corresponding record in the primary records list, null if
     *   not found.
     */
	public IStation getStation(int nId)
	{
		Station oSearchRecord = m_oLock.readLock();		
		oSearchRecord.m_nId = nId;
		
		oSearchRecord = search(m_oRecords, oSearchRecord, m_oRecordSort);
		m_oLock.readUnlock();
		return oSearchRecord;
	}
	
    /**
     * Searches the secondary {@code DbCache} search array for the
     * {@code Station} with the supplied contributor id and station code.
     *
     * @param nContribId contributor id corresponding to the record of interest.
     * @param sCode station code corresponding to the record of interest.
     * @return the record of interest, or null if the record is not contained in
     *   the cache.
     */
	public IStation getStation(int nContribId, String sCode)
	{
		Station oSearchRecord = m_oLock.readLock();
		oSearchRecord.m_nContribId = nContribId;
		oSearchRecord.m_sCode = sCode;
		
		oSearchRecord = search(m_oSort, oSearchRecord, m_oSearchSort);
		m_oLock.readUnlock();
		return oSearchRecord;
	}

    /**
     * Retrieves the list of cached {@code Station} objects.
     *
     * @param oList the list to contain the cached {@code Station} objects.
     */
	public void getStations(ArrayList<IStation> oList)
	{
		oList.clear();
		m_oLock.readLock();

		int nIndex = m_oRecords.size();
		oList.ensureCapacity(nIndex);
		while (nIndex-- > 0)
			oList.add(m_oRecords.get(nIndex));
		
		m_oLock.readUnlock();
	}

    /**
     * Compares the {@code Station} attributes of {@code oLhs} with those of
     * {@code oRhs}.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oLhs record to compare to {@code oRhs}
     * @param oRhs record to compare to {@code oLhs}
     * @return
     * true if the attribute values of {@code oLhs} match that of {@code oRhs},
     * false otherwise.
     */
	protected boolean recordsMatch(Station oLhs, Station oRhs)
	{
		// the numeric variables are compared first 
		// to return from the evaluation early if anything has changed
		return
		(
			oLhs.m_cCat == oRhs.m_cCat && 
			oLhs.m_nContribId == oRhs.m_nContribId && 
			oLhs.m_nLat == oRhs.m_nLat && 
			oLhs.m_nLon == oRhs.m_nLon && 
			oLhs.m_tElev == oRhs.m_tElev && 
			oLhs.m_sCode.compareTo(oRhs.m_sCode) == 0 &&
            oLhs.m_sDesc.compareTo(oRhs.m_sDesc) == 0
		);
	}
	
	/**
     * Creates a new instance of {@code Station}, with the same attribute
     * values as the supplied {@code Station}.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oStation the object to create a copy of.
     * @return The newly created copy of {@code oStation}.
     */
	protected Station copy(Station oStation)
	{
		return new Station(oStation);
	}
	
   /**
    * This is used to add a container of lockable {@code Station} objects
    * to the {@link util.threads.StripeLock} Mutex.
    * <p>
    * Specifies a required {@code DbCache} extension method, which is in turn
    * required for the base class's implementation of the interface class
    * {@code ILockFactory}.
    * </p>
    *
    * @return A new instance of {@code Station}
    *
    * @see util.threads.ILockFactory
    * @see util.threads.StripeLock
    */
	@Override
	public Station getLock()
	{
		return new Station();
	}
	
    /**
     * Converts {@code dValue} from its standard units to micro-units.
     * @param dValue value to convert to micro units.
     * @return the converted value.
     */
	public static int toMicro(double dValue)
	{
		return ((int)Math.round(dValue * 1000000.0));
	}

    /**
     * converts {@code nValue} from micro-units to its standard units.
     * @param nValue value to convert from micro units.
     * @return the converted value.
     */
	public static double fromMicro(int nValue)
	{
		return (((double)nValue) / 1000000.0);
	}


	/**
     * Specifies the primary records list sort order.
     * {@link SortById#compare(Station, Station)} compares {@code Station}
     * objects by station id.
     *
     * <p>
     * Implements {@code Comparator<Station>} to define comparison of
     * {@code Station} objects by id.
     * </p>
     */
	private class SortById implements Comparator<Station>
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
         * Compares {@code Station} objects by id.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(Station oLhs, Station oRhs)
		{
			return (oLhs.m_nId - oRhs.m_nId);
		}
	}

	/**
     * Specifies the secondary records list sort order.
     * {@link SortByContribCode#compare(Station, Station)} compares
     * {@code Station} objects  type contributor id.
     *
     * <p>
     * Implements {@code Comparator<Station>} to define comparison of
     * {@code Station} objects giving priority to contributor id, then
     * station code.
     * </p>
     */
	private class SortByContribCode implements Comparator<Station>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortByContribCode}
		 * </p>
         */
		private SortByContribCode()
		{
		}

        /**
         * Compares {@code Station} objects giving priority to contributor id,
         * then station code.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(Station oLhs, Station oRhs)
		{
			int nCompare = oLhs.m_nContribId - oRhs.m_nContribId;
			if (nCompare == 0)
				nCompare = oLhs.m_sCode.compareTo(oRhs.m_sCode);
			
			return nCompare;
		}
	}
	
	/**
     * Counts stations within the area of the station with the supplied
     * contributor id, if the stations are within the area given by the supplied
     * radius, excluding stations of zero distance.
     * 
     * @param lRadius defines the area of interest surrounding the station with
     * the supplied contributor id.
     * @param nContribId contributor id of the station of interest.
     * @param nMinCount
     * @return the number of stations within the supplied radius.
     */
	public int countNeighbors(long lRadius, int nContribId, int nMinCount)
	{
		int nCount = 0;
		long lRadiusSqr = lRadius * lRadius;
		
		for (int nOuter = 0; nOuter < m_oRecords.size(); nOuter++)
		{
			Station oStation = m_oRecords.get(nOuter);
//			if (oStation.m_nContribId != 4)
			if (oStation.m_nContribId == nContribId)
			{
				int nInnerCount = 0;

				for (int nInner = 0; nInner < m_oRecords.size(); nInner++)
				{
					Station oStationOther = m_oRecords.get(nInner);

					long lDeltaLat = oStation.m_nLat - oStationOther.m_nLat;
					long lDeltaLon = oStation.m_nLon - oStationOther.m_nLon;
					long lDistSqr = lDeltaLat * lDeltaLat + lDeltaLon * lDeltaLon;

					// zero distance should exclude the current station
					if (lDistSqr > 0 && lDistSqr <= lRadiusSqr)
						++nInnerCount;
				}

				if (nInnerCount < nMinCount)
					++nCount;
			}
		}
		
		return nCount;
	}
    
    /**
     * Counts the stations within the radius of the stations in the
	 * records list, and prints the result to standard out.
     *
     * @param lRadius radius within which to count stations.
     */
	public void countNeighbors(long lRadius)
	{
		long lRadiusSqr = lRadius * lRadius;
		
		System.out.print("[");
		for (int nOuter = 0; nOuter < m_oRecords.size(); nOuter++)
		{
			Station oStation = m_oRecords.get(nOuter);
			
			if (oStation.m_nContribId != 4)
			{
				int nInnerCount = 0;

				for (int nInner = 0; nInner < m_oRecords.size(); nInner++)
				{
					Station oStationOther = m_oRecords.get(nInner);

					long lDeltaLat = oStation.m_nLat - oStationOther.m_nLat;
					long lDeltaLon = oStation.m_nLon - oStationOther.m_nLon;
					long lDistSqr = lDeltaLat * lDeltaLat + lDeltaLon * lDeltaLon;

					// zero distance should exclude the current station
					if (lDistSqr > 0 && lDistSqr <= lRadiusSqr)
						++nInnerCount;
				}

				System.out.print("\n\t{lt:");
				System.out.print(fromMicro(oStation.m_nLat));
				System.out.print(",ln:");
				System.out.print(fromMicro(oStation.m_nLon));
				System.out.print(",ct:");
				System.out.print(nInnerCount);
				System.out.print('}');
			}
		}
		System.out.print("\n]");
	}
}
