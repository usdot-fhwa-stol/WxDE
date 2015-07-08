// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Sensors.java
 */
package clarus.emc;

import java.io.PrintWriter;
import java.sql.Date;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Provides means to cache {@code Sensor} objects, as well as modify,
 * access, copy, compare and print these objects contained in the
 * {@code DbCache}.
 *
 * <p>
 * Singleton class only allows one instance of {@code Sensors} which can
 * be accessed by the {@link Sensors#getInstance() } method.
 * </p>
 *
 * <p>
 * Extends {@code DbCache} to allow recurring records to be accessed
 * efficiently.
 * </p>
 */
public class Sensors extends DbCache<Sensor>
{
    /**
     * Singleton instance of {@code Sensors}.
     */
	private static Sensors g_oInstance = new Sensors();
	
	/**
     * <b> Accessor </b>
     * 
     * @return The singleton instance of {@code Sensors}
     */
	public static Sensors getInstance()
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
	private Sensors()
	{
		// set up the sort and search algorithms
		m_oRecordSort = new SortById();
		m_oSearchSort = new SortByStation();
		
		m_sQuery = "SELECT s.id, s.stationId, s.sensorIndex, s.distGroup, " + 
			"s.maintBegin, s.maintEnd, q.sensorTypeId, q.obsTypeId, " + 
			"q.minRange, q.maxRange, q.ratePos, q.rateNeg, " + 
			"q.persistInterval, q.persistThreshold, q.likeThreshold " + 
			"FROM sensor s, qchparm q WHERE q.id = s.qchparmId";
		
		run();
	}
	
	/**
     * Prints {@code Sensors} in a comma delimited manner formatted as
     * follows:
     * <p>
     * {sensor id}, {station id}, {sensor index}, {distribution group},
     * {maintenance begin timestamp}, {maintenance end timestamp},
     * {sensor type id}, {observation type}, {minimum range value},
     * {max range value}, {positive rate of change}, {negative rate of change},
     * {persistance interval}, {persistance threshold}, {like threshold}
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
		Sensor oSensor = m_oRecords.get(nIndex);

		oPrintWriter.print(oSensor.m_nId);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_nStationId);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_nSensorIndex);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_nDistGroup);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_lMaintBegin);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_lMaintEnd);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_nSensorType);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_nObsType);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_dMinRange);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_dMaxRange);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_dRatePos);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_dRateNeg);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_dPersistInterval);
		oPrintWriter.print(",");
		oPrintWriter.print(oSensor.m_dPersistThreshold);
		oPrintWriter.print(",");
		oPrintWriter.println(oSensor.m_dLikeThreshold);
	}
	
	/**
     * Sets {@code oSensor} attributes to those contained in the current
     * row pointed to by the specified result set.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param iResultSet set containing the {@code Sensor} data.
     * @param oSensor the record whose parameters are to be modified.
     */
	protected void setParameters(ResultSet iResultSet, Sensor oSensor)
	{
		Date oMaintBegin = null;
		Date oMaintEnd = null;
		
		try
		{
			oSensor.m_nId = iResultSet.getInt(1);
			oSensor.m_nStationId = iResultSet.getInt(2);
			oSensor.m_nSensorIndex = iResultSet.getInt(3);
			oSensor.m_nDistGroup = iResultSet.getInt(4);
			
			oMaintBegin = iResultSet.getDate(5);
			if (oMaintBegin != null)
				oSensor.m_lMaintBegin = oMaintBegin.getTime();
			
			oMaintEnd = iResultSet.getDate(6);
			if (oMaintEnd != null)
				oSensor.m_lMaintEnd = oMaintEnd.getTime();
			
			// properly set the maintenance timestamps when there are nulls
			if (oMaintBegin == null)
			{
				if (oMaintEnd == null)
				{
					oSensor.m_lMaintBegin = Long.MAX_VALUE;
					oSensor.m_lMaintEnd = Long.MIN_VALUE;
				}
				else
					oSensor.m_lMaintBegin = Long.MIN_VALUE;
			}
			else
			{
				if (oMaintEnd == null)
					oSensor.m_lMaintEnd = Long.MAX_VALUE;
			}
			
			oSensor.m_nSensorType = iResultSet.getInt(7);
			oSensor.m_nObsType = iResultSet.getInt(8);
			oSensor.m_dMinRange = iResultSet.getDouble(9);
			oSensor.m_dMaxRange = iResultSet.getDouble(10);
			oSensor.m_dRatePos = iResultSet.getDouble(11);
			oSensor.m_dRateNeg = iResultSet.getDouble(12);
			oSensor.m_dPersistInterval = iResultSet.getDouble(13);
			oSensor.m_dPersistThreshold = iResultSet.getDouble(14);
			oSensor.m_dLikeThreshold = iResultSet.getDouble(15);
			
			// correction for potential database entry errors
			if (oSensor.m_dRateNeg > 0.0)
				oSensor.m_dRateNeg = -oSensor.m_dRateNeg;
			
			// database rates are per hour, convert to per second
			oSensor.m_dRatePos /= 3600.0;
			oSensor.m_dRateNeg /= 3600.0;			
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}

	/**
     * Searches the primary {@code DbCache} records list for the record
     * containing the supplied sensor id.
     *
     * @param nId sensor id correspoding to the record of interest.
     * @return the corresponding record in the primary records list, null if
     *   not found.
     */
	public ISensor getSensor(int nId)
	{
		Sensor oSearchRecord = m_oLock.readLock();		
		oSearchRecord.m_nId = nId;
		
		oSearchRecord = search(m_oRecords, oSearchRecord, m_oRecordSort);
		m_oLock.readUnlock();
		return oSearchRecord;
	}
	
	/**
     * Searches the secondary {@code DbCache} records list for the record
     * containing the supplied station id, observation type id, and sensor
     * index.
     *
     * @param nStationId station of interest identifier
     * @param nObsTypeId observation type of interest
     * @param nSensorIndex sensor index of interest
     * @return the corresponding {@code Sensor} retrieved from the cache.
     */
	public ISensor getSensor(int nStationId, int nObsTypeId, int nSensorIndex)
	{
		Sensor oSearchRecord = m_oLock.readLock();		
		oSearchRecord.m_nStationId = nStationId;
		oSearchRecord.m_nObsType = nObsTypeId;
		oSearchRecord.m_nSensorIndex = nSensorIndex;
		
		ISensor iSensor = null;
		int nIndex = Collections.binarySearch(m_oSort, oSearchRecord, m_oSearchSort);
		if (nIndex >= 0)
			iSensor = m_oSort.get(nIndex);

		m_oLock.readUnlock();
		return iSensor;
	}
	
    /**
     * Fills the supplied list of sensors with the cached sensors corresponding
     * to the given station id, and observation type.
     *
     * @param nStationId station id corresponding to the sensors of interest.
     * @param nObsType observation type corresponding to the sensors of
     *  interest.
     * @param oSensors the retrieved list of {@code Sensor} objects
     * corresponding to the supplied station id and observation type.
     */
	public void getSensors(int nStationId, int nObsType, 
		ArrayList<ISensor> oSensors)
	{
		Sensor oSearchRecord = m_oLock.readLock();		
		oSearchRecord.m_nStationId = nStationId;
		oSearchRecord.m_nObsType = nObsType;
		oSearchRecord.m_nSensorIndex = Integer.MIN_VALUE;
		
		int nIndex = Collections.binarySearch(m_oSort, oSearchRecord, m_oSearchSort);
		if (nIndex < 0)
		{
			nIndex = ~nIndex;
			while (nIndex < m_oSort.size() && 
				m_oSort.get(nIndex).m_nObsType == nObsType)
			{
				oSensors.add(m_oSort.get(nIndex++));
			}
		}

		m_oLock.readUnlock();
	}

    /**
     * Retrieves the list of cached {@code Sensor} objects.
     *
     * @param oList the list to contain the cached {@code Sensor} objects.
     */
	public void getSensors(ArrayList<ISensor> oList)
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
     * Compares the {@code Sensor} attributes of {@code oLhs} with those of
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
	protected boolean recordsMatch(Sensor oLhs, Sensor oRhs)
	{
		// compare the maintenance timestamps and sensor parameters first 
		// since those are the most likely values to have changed
		return
		(
			oLhs.m_lMaintBegin == oRhs.m_lMaintBegin && 
			oLhs.m_lMaintEnd == oRhs.m_lMaintEnd && 
			oLhs.m_dMinRange == oRhs.m_dMinRange && 
			oLhs.m_dMaxRange == oRhs.m_dMaxRange && 
			oLhs.m_dRatePos == oRhs.m_dRatePos && 
			oLhs.m_dRateNeg == oRhs.m_dRateNeg && 
			oLhs.m_dPersistInterval == oRhs.m_dPersistInterval && 
			oLhs.m_dPersistThreshold == oRhs.m_dPersistThreshold && 
			oLhs.m_dLikeThreshold == oRhs.m_dLikeThreshold && 
			oLhs.m_nSensorType == oRhs.m_nSensorType && 
			oLhs.m_nObsType == oRhs.m_nObsType && 
			oLhs.m_nStationId == oRhs.m_nStationId && 
			oLhs.m_nSensorIndex == oRhs.m_nSensorIndex && 
			oLhs.m_nDistGroup == oRhs.m_nDistGroup
		);
	}

	/**
     * Creates a new instance of {@code Sensor}, with the same attribute
     * values as the supplied {@code Sensor}.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oSensor the object to create a copy of.
     * @return The newly created copy of {@code oSensor}.
     */
	protected Sensor copy(Sensor oSensor)
	{
		return new Sensor(oSensor);
	}
	
    /**
     * This is used to add a container of lockable {@code Sensor} objects
     * to the {@link util.threads.StripeLock} Mutex.
     * <p>
     * Specifies a required {@code DbCache} extension method, which is in turn
     * required for the base class's implementation of the interface class
     * {@code ILockFactory}.
     * </p>
     *
     * @return A new instance of {@code Sensor}
     *
     * @see util.threads.ILockFactory
     * @see util.threads.StripeLock
     */
	@Override
	public Sensor getLock()
	{
		return new Sensor();
	}
	
	/**
     * Specifies the primary records list sort order.
     * {@link SortById#compare(Sensor, Sensor)} compares {@code Sensor} objects
     * by sensor id.
     *
     * <p>
     * Implements {@code Comparator<Sensor>} to define comparison of
     * {@code Sensor} objects by id.
     * </p>
     */
	private class SortById implements Comparator<Sensor>
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
         * Compares {@code Sensor} objects by id.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(Sensor oLhs, Sensor oRhs)
		{
			return (oLhs.m_nId - oRhs.m_nId);
		}
	}

	/**
     * Specifies the secondary records list sort order.
     * {@link SortByStation#compare(Sensor, Sensor)} compares {@code Sensor} 
     * objects, giving priority to station id, then observation type, finally
     * by sensor index.
     *
     * <p>
     * Implements {@code Comparator<Sensor>} to define comparison of
     * {@code Sensor} objects giving priority to station id, then observation
     * type, finally by sensor index.
     * </p>
     */
	private class SortByStation implements Comparator<Sensor>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortByStation}
		 * </p>
         */
		private SortByStation()
		{
		}

        /**
         * Compares {@code Sensor} objects by giving priority to station id,
         * then observation type, finally by sensor index.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(Sensor oLhs, Sensor oRhs)
		{
			if (oLhs.m_nStationId != oRhs.m_nStationId)
				return oLhs.m_nStationId - oRhs.m_nStationId;

			if (oLhs.m_nObsType != oRhs.m_nObsType)
				return oLhs.m_nObsType - oRhs.m_nObsType;

			return (oLhs.m_nSensorIndex - oRhs.m_nSensorIndex);
		}
	}
}
