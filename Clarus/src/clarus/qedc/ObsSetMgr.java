// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ObsSetMgr.java
 */
package clarus.qedc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import util.Introsort;
import util.threads.ILockFactory;
import util.threads.StripeLock;

/**
 * Manages sets of observations of a given type. Provides methods of querying,
 * registering, comparing, and processing of observation sets.
 *
 * <p>
 * Implements {@code Comparable<ObsSetMgr>} to allow comparison of observation
 * set managers by observation type as defined by
 * {@link ObsSetMgr#compareTo(clarus.qedc.ObsSetMgr)}.
 * </p>
 * <p>
 * Implements {@code ILockFactory} to allow mutually exclusive access of threads
 * to critical section of the {@code ObsMgr} through the use of
 * {@link StripeLock}
 * </p>
 */
public class ObsSetMgr implements Comparable<ObsSetMgr>, ILockFactory<ObsIter>
{
    /**
     * Observation type to be contained in this set manager.
     */
	int m_nObsType;
    /**
     * Configured length of time to retain observations in the set.
     */
	private long m_lLifetime;
    /**
     * The list of observations.
     */
	private ArrayList<Obs> m_oObs;
    /**
     * Recently added obserations.
     */
	private ArrayList<Obs> m_oRecentObs;
    /**
     * Comparator for comparing and sorting according to hash value, then by
     * sensor id.
     */
	private ObsByHash m_oObsByHash;
    /**
     * Comparator for comparing and sorting according to observation sensor id,
     * then by timestamp.
     */
	private ObsBySensor m_oObsBySensor;
    /**
     * Comparator for comparing and sorting according to observation timestamps,
     * then by sensor id
     */
	private ObsByTime m_oObsByTime;
    /**
     * Stripe lock container of observations.
     */
	private StripeLock<ObsIter> m_oObsLock;
    /**
     * Stripe lock container of recently added observations.
     */
	private StripeLock<ObsIter> m_oRecentObsLock;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code ObsSetMgr}
	 * </p>
	 */
	ObsSetMgr()
	{
	}
	

    /**
     * Initializes attributes of new instances of {@code ObsSetMgr} with the
     * supplied values.
     *
     * @param nObsType The type of observation to be contained in the set.
     * @param lLifetime length of time to retain observations in the set
     * @param nLockCount number of locks to allocate to the observation set
     *      manager.
     * @param nObsInitialCapacity configured observation set capacity.
     * @param nSensorInitialCapacity configured sensor capacity.
     */
	ObsSetMgr(int nObsType, long lLifetime, int nLockCount,
		int nObsInitialCapacity, int nSensorInitialCapacity)
	{
		m_nObsType = nObsType;
		m_lLifetime = lLifetime;
		m_oObs = new ArrayList<Obs>(nObsInitialCapacity);
		m_oRecentObs = new ArrayList<Obs>(nSensorInitialCapacity);
		m_oObsByHash = new ObsByHash();
		m_oObsBySensor = new ObsBySensor();
		m_oObsByTime = new ObsByTime();
		m_oObsLock = new StripeLock<ObsIter>(this, nLockCount);
		m_oRecentObsLock = new StripeLock<ObsIter>(this, nLockCount);
	}


    /**
     * Removes duplicate observations from the set, then checks with the
     * existing cache for duplicates. Removes outdated observations, and updates
     * the cache with newer observation from the provided set.
     *
     * @param iObsSet observation set to process.
     */
	void processObsSet(ObsSet oObsSet)
	{
		int nIndex = 0;
		// only continue processing when a matching obs set is found
		if (oObsSet == null)
			return;
		
		// make the obs set read-only
		oObsSet.m_bReadOnly = true;

		// first check for duplicate obs within the obs set
		Introsort.usort(oObsSet, m_oObsByTime);
		int nInnerIndex = oObsSet.size();
		nIndex = --nInnerIndex;
		while (nInnerIndex-- > 0)
		{
			if (m_oObsByTime.compare(oObsSet.get(nIndex),
				oObsSet.get(nInnerIndex)) == 0)
				oObsSet.remove(nIndex);

			--nIndex;
		}

		// determine the accepted time range
		long lExpired = System.currentTimeMillis();
		long lForecast = lExpired + m_lLifetime;
		lExpired -= m_lLifetime;

		// check the obs set for duplicates against the existing cache
		nInnerIndex = oObsSet.size();
		ObsIter oSearchObs = m_oRecentObsLock.writeLock();
		while (nInnerIndex-- > 0)
		{
			Obs oObs = oObsSet.get(nInnerIndex);
			// remove obs outside the accepted time range
			if (oObs.m_lTimestamp < lExpired || oObs.m_lTimestamp > lForecast)
				oObsSet.remove(nInnerIndex);
			else
			{
				// attempt to find an existing obs to update
				oObs.m_tHash = oSearchObs.getHash(oObs.m_nLat, oObs.m_nLon);
				nIndex = Introsort.binarySearch(m_oRecentObs, oObs, m_oObsByHash);
				
				if (nIndex < 0)
					m_oRecentObs.add(~nIndex, oObs);
				else
				{
					// only update the cache with newer obs
					if (oObs.m_lTimestamp > m_oRecentObs.get(nIndex).m_lTimestamp)
						m_oRecentObs.set(nIndex, oObs);
					else
						oObsSet.remove(nInnerIndex);
				}
			}
		}
		m_oRecentObsLock.writeUnlock();

		// merge any remaining obs into the accepted obs lists
		nInnerIndex = oObsSet.size();
		if (nInnerIndex == 0)
			return;
		
		// these operations will result in list modifications
		oSearchObs = m_oObsLock.writeLock();
		oSearchObs.clear();

		int nListSize = m_oObs.size();
		while (nInnerIndex-- > 0)
		{
			Obs oObs = oObsSet.get(nInnerIndex);
			// find the lowest index for the current obs sensor
			// the index should always point to the oldest obs for a sensor
			oSearchObs.m_nSensorId = oObs.m_nSensorId;
			nIndex = ~Introsort.binarySearch(m_oObs, oSearchObs, m_oObsBySensor);

			if (nIndex < nListSize)
			{
				// find the insertion point for the new obs in the sensor subset
				int nLimit = ~Introsort.binarySearch
					(m_oObs, oObs, nIndex, nListSize, m_oObsBySensor);

				// shift the newer obs up one position when replacing older obs
				if (m_oObs.get(nIndex).m_lTimestamp < lExpired)
				{
					int nOuterIndex = nIndex;
					while (++nOuterIndex < nLimit)
						m_oObs.set(nIndex++, m_oObs.get(nOuterIndex));

					// replace the end of the range with the new obs
					m_oObs.set(nIndex, oObs);
				}
				else
					// insert the new obs when no expired obs are replaced
					m_oObs.add(nLimit, oObs);
			}
			else
				// obs that fall off the end of the list are always inserted
				m_oObs.add(nIndex, oObs);
		}

		m_oObsLock.writeUnlock();
	}


	/**
     * Gets the set of recent observations with latitude, longitude, and
     * timestamp values falling within the supplied bounds.
     * 
     * @param oObsSet ArrayList of observations found.
     * @param nLatMin minimum latitude bound.
     * @param nLonMin minimum longitude bound.
     * @param nLatMax maximum latitude bound.
     * @param nLonMax maximum longitude bound.
     * @param lPast minimum timestamp bound.
     * @param lFuture maximum timstamp bound.
     */
	void getBackground(ArrayList<IObs> oObsSet, int nLatMin, int nLonMin,
		int nLatMax, int nLonMax, long lPast, long lFuture)
	{
		ObsIter oSearchObs = m_oRecentObsLock.readLock();
		
		// initialize the hash iterator
		oSearchObs.iterator(nLatMin, nLonMin, nLatMax, nLonMax);
		while (oSearchObs.hasNext())
		{
			oSearchObs.next();
			int nIndex = Collections.binarySearch(m_oRecentObs, 
				oSearchObs, m_oObsByHash);
			
			if (nIndex < 0)
				nIndex = ~nIndex;
			
			Obs oObs = null;
			while (nIndex < m_oRecentObs.size() && 
				(oObs = m_oRecentObs.get(nIndex++)).m_tHash == oSearchObs.m_tHash)
			{
				// filter grouped obs further by exact space and time ranges
				if (oObs.m_nLon >= nLonMin && oObs.m_nLat >= nLatMin && 
					oObs.m_nLon <= nLonMax && oObs.m_nLat <= nLatMax && 
					oObs.m_lTimestamp >= lPast && oObs.m_lTimestamp <= lFuture)
						oObsSet.add(oObs);
			}
		}
		
		m_oRecentObsLock.readUnlock();
	}
	

    /**
     * Retrieve the set of observations with the supplied sensor id, that fall
     * within the given time bounds.
     *
     * @param oObsSet ArrayList of observations by sensor corresponding to the
     *  provided sensor id.
     * @param nSensorId sensor id of interest.
     * @param lPast lower time bound.
     * @param lFuture upper time bound.
     */
	void getSensors(ArrayList<IObs> oObsSet, int nSensorId,
		long lPast, long lFuture)
	{
		ObsIter oSearchObs = m_oObsLock.readLock();

		// configure the search criteria
		oSearchObs.m_nSensorId = nSensorId;
		oSearchObs.m_lTimestamp = lFuture;

		// create the list with the most recent obs first
		int nIndex = Collections.binarySearch(m_oObs, oSearchObs, m_oObsBySensor);

		// position the index even if an exact match is not found 
		// this probably means the index points to the current obs
		if (nIndex < 0)
			nIndex = ~nIndex;

		// iterate for the current sensor id within the specified timerange
		Obs oObs = null;
		while (nIndex-- > 0 &&
			(oObs = m_oObs.get(nIndex)).m_nSensorId == nSensorId &&
			oObs.m_lTimestamp >= lPast)
		{
			oObsSet.add(oObs);
		}
		
		// clear search values before releasing the lock
		oSearchObs.clear();
		m_oObsLock.readUnlock();
	}
	

	/**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link ObsIter} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link ObsIter}
     *
     * @see ILockFactory
     * @see StripeLock
     */
	public ObsIter getLock()
	{
		return new ObsIter();
	}


    /**
     * Compares <i> this </i> observation set manager to the supplied
     * observation set manager by their contained observation types.
     * @param oObsSetMgr the manager to compare to <i> this </i>
     * @return 0 if the observation types match.
     */
	public int compareTo(ObsSetMgr oObsSetMgr)
	{
		return (m_nObsType - oObsSetMgr.m_nObsType);
	}


    /**
     * Provides a means of comparing and sorting according to hash value,
     * then by sensor id with the
     * {@link ObsByHash#compare(Obs, Obs)} method.
     */
	private class ObsByHash implements Comparator<Obs>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code ObsByHash}
		 * </p>
         */
		private ObsByHash()
		{
		}


        /**
         *
         * @param oLhs the {@code Obs} to compare to {@code oRhs}
         * @param oRhs the {@code Obs} to compare to {@code oLhs}
         * @return 0 if the observations match by hash or by sensor id.
         */
		public int compare(Obs oLhs, Obs oRhs)
		{
			// compare the hash codes and then the sensor id
			int nCompare = oLhs.m_tHash - oRhs.m_tHash;
			if (nCompare == 0)
				return (oLhs.m_nSensorId - oRhs.m_nSensorId);

			return nCompare;
		}
	}


    /**
     * Provides a means of comparing and sorting according to observation
     * sensor id, then by timestamp with the
     * {@link ObsBySensor#compare(Obs, Obs)} method.
     */
	private class ObsBySensor implements Comparator<Obs>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code ObsBySensor}
		 * </p>
         */
		private ObsBySensor()
		{
		}


        /**
         * Compare by sensor id then by timestamp.
         *
         * <p>
         * *May not work well in the case of vehicle sensors.*
         * </p>
         * @param oLhs the {@code Obs} to compare to {@code oRhs}
         * @param oRhs the {@code Obs} to compare to {@code oLhs}
         * @return 0 if the observations match by sensor id or by timestamp.
         */
		public int compare(Obs oLhs, Obs oRhs)
		{
			// compare the sensor id and then the timestamp
			// this may not work well in the case of vehicle sensors
			int nCompare = oLhs.m_nSensorId - oRhs.m_nSensorId;
			if (nCompare == 0)
				return ((int)(oLhs.m_lTimestamp - oRhs.m_lTimestamp));

			return nCompare;
		}
	}
	

    /**
     * Provides a means of comparing and sorting according to observation
     * timestamps, then by sensor id with the
     * {@link ObsByTime#compare(Obs, Obs)} method.
     */
	private class ObsByTime implements Comparator<Obs>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code ObsByTime}
		 * </p>
         */
		protected ObsByTime()
		{
		}


        /**
         * Compares {@link Obs} first by timestamp, then by sensor id.
         * @param oLhs the {@code Obs} to compare to {@code oRhs}
         * @param oRhs the {@code Obs} to compare to {@code oLhs}
         * @return 0 if the observations match by timestamp or sensor id.
         */
		public int compare(Obs oLhs, Obs oRhs)
		{
			// compare the timestamp and then the sensor id
			// this may not work well in the case of vehicle sensors
			long lCompare = oLhs.m_lTimestamp - oRhs.m_lTimestamp;
			if (lCompare == 0L)
				return (oLhs.m_nSensorId - oRhs.m_nSensorId);

			return (int)lCompare;
		}
	}
}
