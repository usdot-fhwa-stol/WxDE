// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * ObsMgr.java
 */
package clarus.qedc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import clarus.emc.ObsTypes;
import clarus.emc.IObsType;
import util.Config;
import util.ConfigSvc;
import util.IntKeyValue;
import util.Introsort;
import util.threads.AsyncQ;
import util.threads.ILockFactory;
import util.threads.StripeLock;

/**
 * Intermediary between {@link ClarusManager}, and the {@link ObsSetMgr}.
 * Processes observation sets, and queues observation sets to be processed
 * in the {@code ClarusMgr} processor.
 *
 * <p>
 * Singleton class whose instance can be retrieved with
 * {@link ObsMgr#getInstance()}
 * </p>
 * <p>
 * Extends {@code AsyncQ} to allow processing of observation sets, as they're
 * added to the queue.
 * </p>
 * <p>
 * Implements {@code ILockFactory} to allow mutually exclusive access of threads
 * to critical section of the {@code ObsMgr} through the use of
 * {@link StripeLock}
 * </p>
 */
public class ObsMgr extends AsyncQ<IObsSet> implements ILockFactory<ObsSetMgr>
{
    /**
     * The configured number of threads for processing observation sets. Default
     * value is 5.
     */
	private static int MAX_THREADS = 5;
    /**
     * Configured observation capacity, initially 2MB.
     */
	private static int OBS_INITIAL_CAPACITY = 524288; // 512K * 4B = 2MB
    /**
     * Configured sensor capacity, initially 512KB.
     */
	private static int SENSOR_INITIAL_CAPACITY = 131072; // 128K * 4B = 512KB
    /**
     * Observation insert query format.
     */
	private static final String QUERY_INSERT = "INSERT INTO obs (obsType, " +
		"sensorId, timestamp, latitude, longitude, elevation, value, " +
		"confidence, runFlags, passedFlags, created, updated) " +
		"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
    /**
     * Observation update query format.
     */
	private static final String QUERY_UPDATE = "UPDATE obs " + 
		"SET confidence = ?, runFlags = ?, passedFlags = ?, updated = ? " +
		"WHERE obsType = ? AND sensorId = ? AND timestamp = ?";
    /**
     * Pointer to the singleton instance of {@code ObsMgr}.
     */
	private static ObsMgr g_oInstance = new ObsMgr();
    /**
     * Flag used to indicate the received obs are archived from this instance.
     */
	private boolean m_bArchive = false;
    /**
     * Configured length of time to retain observations in the set, initially
     * 2 days.
     */
	private long m_lLifetime = 172800000L; // 2 days = 86400 * 1000 * 2
    /**
     * ArrayList of observation set managers.
     */
	private ArrayList<ObsSetMgr> m_oObsSetMgrs = new ArrayList<ObsSetMgr>();
    /**
     * Stripe lock container of observation set managers, to allow mutually
     * exclusive access to the managers.
     */
	private StripeLock<ObsSetMgr> m_oLock;
    /**
     * Persistent datasource interface reference.
     */
	private DataSource m_iDataSource;
    /**
     * Array of received obs ordered by timestamp, obs type, and sensor id.
     */
	private ArrayList<Obs> m_oObs = new ArrayList<Obs>();
    /**
     * Array of received obs ordered by the update timestamp.
     */
	private ArrayList<Obs> m_oTsObs = new ArrayList<Obs>();
    /**
     * The set of observation sets, registered by their serial number.
     */
	private ArrayList<IntKeyValue<ObsSet>> m_oObsSets =
		new ArrayList<IntKeyValue<ObsSet>>();
    /**
     * Used to assign a serial number for requested observation sets.
     */
	private AtomicInteger m_oSerial = new AtomicInteger(Integer.MIN_VALUE);
    /**
     * Used for searching the observation sets list by serial number.
     */
	private IntKeyValue<ObsSet> m_oSearchObsSet = new IntKeyValue<ObsSet>();
    /**
     * Comparable object used to sort obs arrays by timestamp, obs type,
	 * and sensor id.
     */
	private UniqueObs m_oUniqueObs = new UniqueObs();
    /**
     * Comparable object used to sort obs arrays by updated timestamp.
     */
	private UpdatedObs m_oUpdatedObs = new UpdatedObs();
    /**
     * Obs object used to find observations by timestamp.
     */
	private Obs m_oSearchObs = new Obs();
    /**
     * Pointer to the singleton instance of {@link ClarusMgr}.
     */
	private ClarusMgr m_oClarusMgr = ClarusMgr.getInstance();
	

    /**
     * Returns the singleton instance of {@code ObsMgr}.
     * @return the instance of {@code ObsMgr}.
     */
	public static ObsMgr getInstance()
	{
		return g_oInstance;
	}
	

    /**
     * <b> Default Constructor </b>
     * <p>
     * Configures the observation manager. Initializes and sorts the observation
     * set managers. Registers the observation manager with the Clarus
     * system.
     * </p>
     */
	private ObsMgr()
	{
		// apply the ObsMgr configuration
		ConfigSvc oConfigSvc = ConfigSvc.getInstance();
		Config oConfig = oConfigSvc.getConfig(this);
		
		// increase the queue depth for more thread concurrency
		MAX_THREADS = oConfig.getInt("queuedepth", MAX_THREADS);
		setMaxThreads(MAX_THREADS);
		m_oLock = new StripeLock<ObsSetMgr>(this, MAX_THREADS);

		// read the maximum time an obs can remain in any cache
		m_lLifetime = oConfig.getLong("lifetime", m_lLifetime);

		// save the datasource name to establish database connections later
		m_iDataSource = m_oClarusMgr.
			getDataSource(oConfig.getString("datasource", null));

		// determine if this obs mgr instance should archive received obs
		m_bArchive = (oConfig.getInt("archive", 0) != 0);

		OBS_INITIAL_CAPACITY =
			oConfig.getInt("obsInitialCapacity", OBS_INITIAL_CAPACITY);

		SENSOR_INITIAL_CAPACITY =
			oConfig.getInt("sensorInitialCapacity", SENSOR_INITIAL_CAPACITY);

		// apply the default configuration
		oConfig = oConfigSvc.getConfig("_default");
		String[] sCacheTypes = oConfig.getStringArray("obstype");
		if (sCacheTypes != null && sCacheTypes.length > 0)
		{
			// initialize the obs set managers
			int nIndex = sCacheTypes.length;
			m_oObsSetMgrs.ensureCapacity(nIndex);
			ObsTypes oCacheObsTypes = ObsTypes.getInstance();
			while(nIndex-- > 0)
			{
				// resolve the obs type name to an obs type id
				IObsType iObsType = 
					oCacheObsTypes.getObsType(sCacheTypes[nIndex]);
				
				if (iObsType != null)
				{
					m_oObsSetMgrs.add(new ObsSetMgr(iObsType.getId(), 
						m_lLifetime, MAX_THREADS, OBS_INITIAL_CAPACITY,
						SENSOR_INITIAL_CAPACITY));
				}
			}
			Collections.sort(m_oObsSetMgrs);
		}		

		// register obs mgr with system manager
		m_oClarusMgr.register(getClass().getName(), this);
		System.out.println(getClass().getName());
	}
	

    /**
     * Finds and returns the observation set manager of the supplied type,
     * from the list of obs set managers ({@code m_oObsSetMgrs}).
     *
     * @param nObsType type of observation set manager to retrieve.
     * 
     * @return The observation set manager containing observations of the
     * supplied type {@code nObsType} if found in the list of observation
     * set managers. Otherwise null is returned.
     */
	private ObsSetMgr getObsSetMgr(int nObsType)
	{
		ObsSetMgr oObsSetMgr = null;
		
		// obs set managers are not created after initialization
		ObsSetMgr oSearch = m_oLock.readLock();
		oSearch.m_nObsType = nObsType;
		
		int nIndex = Collections.binarySearch(m_oObsSetMgrs, oSearch);
		if (nIndex >= 0)
			oObsSetMgr = m_oObsSetMgrs.get(nIndex);
		
		m_oLock.readUnlock();

		// null is returned when no obs set manager is available
		return oObsSetMgr;
	}


    /**
     * Creates a new observation set of the provided type that is supplied a
     * serial id. The observation set is then registered with the correct
     * observation set manager if it is monitored by this observation
     * cache.
     *
     * @param nObsType the observation type of interest.
     * @return the newly created observation set.
     */
	public synchronized ObsSet getObsSet(int nObsType)
	{
		int nIndex = 0;
		ObsSet oObsSet = null;
		
		// keep generating serial numbers until an available one is found
		while (nIndex >= 0)
		{
			int nSerial = m_oSerial.getAndIncrement();
			m_oSearchObsSet.setKey(nSerial);
			nIndex = Collections.binarySearch(m_oObsSets, m_oSearchObsSet);
			// verify that the numbered obs set is not in the list
			if (nIndex < 0)
			{
				oObsSet = new ObsSet(nObsType, nSerial);
				m_oObsSets.add(~nIndex,
					new IntKeyValue<ObsSet>(nSerial, oObsSet));
			}
		}

		return oObsSet;
	}


    /**
     * Finds the observation set manager corresponding to the observation
     * type contained by the supplied set. If it is found, the set in the
     * manager is processed via {@link ObsSetMgr#processObsSet(IObsSet)}.
     * The observation set is then queued into the clarus manager sequence
     * queue.
     * <p>
     * Overrides base class method {@link AsyncQ#run()}.
     * </p>
     * @param iObsSet observation set to process and queue.
     */
	@Override
	public void run(IObsSet iObsSet)
	{
		// first, convert the obs set interface back to an obs set object
		// so that more direct, and faster, operations can be applied
		ObsSet oObsSet = null;
		synchronized(this)
		{
			m_oSearchObsSet.setKey(iObsSet.serial());
			int nIndex = Collections.binarySearch(m_oObsSets, m_oSearchObsSet);
			// remove the obs set from the serial number list when resolved
			if (nIndex >= 0)
				oObsSet = m_oObsSets.remove(nIndex).value();
		}

		ObsSetMgr oObsSetMgr = getObsSetMgr(iObsSet.getObsType());
		if (oObsSetMgr != null)
			oObsSetMgr.processObsSet(oObsSet);

		// when an obs set contains only expired or duplicate obs 
		// there is no need to queue an empty set for the next process
		if (iObsSet.size() > 0)
			m_oClarusMgr.queue(iObsSet);

		// determine if this obs manager needs to archive obs
		if (!m_bArchive || m_iDataSource == null || oObsSet == null)
			return;

		// save the current time to apply to each obs as needed
		long lNow = System.currentTimeMillis();

		int nIndex = oObsSet.size();
		if (nIndex == 0)
			return;

		// the obs arrays are shared with dissemination services
		// and need to be synchronized
		synchronized(this)
		{
			ArrayDeque<Obs> oInsertQueue = new ArrayDeque<Obs>();
			ArrayDeque<Obs> oUpdateQueue = new ArrayDeque<Obs>();

			Obs oObs = null;
			// determine if the obs needs to be inserted
			while (nIndex-- > 0)
			{
				oObs = oObsSet.get(nIndex);

				// first, check if the obs exists
				int nSearchIndex =
					Collections.binarySearch(m_oObs, oObs, m_oUniqueObs);

				// the obs is added new when it does not exist
				if (nSearchIndex < 0)
				{
					oObs.m_lUpdated = lNow;
					m_oObs.add(~nSearchIndex, oObs);
					// the obs is also added to the end of the timestamp array
					m_oTsObs.add(oObs);

					oInsertQueue.add(oObs);
				}
				else
				{
					if (oObs.m_nQChRun != 0) // only update if flags are set
					{
						// set the existing obs updated timestamp
						Obs oCurrentObs = m_oObs.get(nSearchIndex);
						oCurrentObs.m_lUpdated = lNow;

						// copy the quality checking flags to the existing obs
						oCurrentObs.m_nQChRun = oObs.m_nQChRun;
						oCurrentObs.m_nQChFlags = oObs.m_nQChFlags;
						oCurrentObs.m_fConfidence = oObs.m_fConfidence;

						// queue finalized obs to be saved in a database
						oUpdateQueue.add(oObs);
					}
				}
			}

			// sort obs that have been added by updated timestamp
			Introsort.usort(m_oTsObs, m_oUpdatedObs);

			// remove obs that have not been updated within the last 65 minutes
			m_oSearchObs.m_lUpdated = lNow - 3900000L;
			nIndex = Collections.binarySearch(m_oTsObs,
				m_oSearchObs, m_oUpdatedObs);

			// it does not matter here if the binary search is off a bit
			if (nIndex < 0)
				nIndex = ~nIndex;

			// remove expired obs from both lists
			while (nIndex-- > 0)
			{
				// these calls result in several array copy operations
				oObs = m_oTsObs.remove(nIndex);
				int nObsIndex =
					Collections.binarySearch(m_oObs, oObs, m_oUniqueObs);

				if (nObsIndex >= 0)
					m_oObs.remove(nObsIndex);
			}

			if (oInsertQueue.isEmpty() && oUpdateQueue.isEmpty())
			  return;

			// handle inserts before updates
			try
			{
				Connection iConnection = m_iDataSource.getConnection();
				if (iConnection == null)
					return;

				Timestamp oObsTs = new Timestamp(0L);

				iConnection.setAutoCommit(false);

				PreparedStatement iQuery =
					iConnection.prepareStatement(QUERY_INSERT);

				while (!oInsertQueue.isEmpty())
				{
					try
					{
						oObs = oInsertQueue.pop();
						oObsTs.setTime(oObs.m_lTimestamp);

						iQuery.setInt(1, oObs.getTypeId());
						iQuery.setInt(2, oObs.m_nSensorId);
						iQuery.setTimestamp(3, oObsTs);
						iQuery.setInt(4, oObs.m_nLat);
						iQuery.setInt(5, oObs.m_nLon);
						iQuery.setInt(6, oObs.m_tElev);
						iQuery.setDouble(7, oObs.m_dValue);
						iQuery.setFloat(8, oObs.m_fConfidence);
						iQuery.setInt(9, oObs.m_nQChRun);
						iQuery.setInt(10, oObs.m_nQChFlags);
						iQuery.setLong(11, lNow);
						iQuery.setLong(12, lNow);

						iQuery.executeUpdate();
					}
					catch (Exception oException)
					{
						oException.printStackTrace();
					}
				}

				// commit the database changes
				iConnection.commit();
				iQuery.close();

				iQuery = iConnection.prepareStatement(QUERY_UPDATE);

				while (!oUpdateQueue.isEmpty())
				{
					try
					{
						oObs = oUpdateQueue.pop();
						oObsTs.setTime(oObs.m_lTimestamp);

						iQuery.setFloat(1, oObs.m_fConfidence);
						iQuery.setInt(2, oObs.m_nQChRun);
						iQuery.setInt(3, oObs.m_nQChFlags);
						iQuery.setLong(4, lNow);
						iQuery.setInt(5, oObs.getTypeId());
						iQuery.setInt(6, oObs.m_nSensorId);
						iQuery.setTimestamp(7, oObsTs);

						iQuery.executeUpdate();
					}
					catch (Exception oException)
					{
						oException.printStackTrace();
					}
				}

				// commit the database changes
				iConnection.commit();
				iQuery.close();

				iConnection.close();
			}
			catch (SQLException oSqlException)
			{
				System.out.println(oSqlException);
			}
		}
	}


	public synchronized void getObs(ArrayList<IObs> oObsArray, long lTimestamp)
	{
		// find the earliest occurrence of the provided timestamp
		m_oSearchObs.m_lUpdated = lTimestamp;
		int nIndex = Collections.binarySearch(m_oTsObs,
			m_oSearchObs, m_oUpdatedObs);

		if (nIndex < 0)
			nIndex = ~nIndex;
		else
		{
			// shift the index to before the lowest index of a matching obs
			// as binary search is not guaranteed to find the first occurrence
			while (nIndex >= 0 && 
				m_oTsObs.get(nIndex--).m_lUpdated >= lTimestamp);
			
			++nIndex;
		}

		// reserve enough space in the destination obs array
		oObsArray.ensureCapacity(m_oTsObs.size() - nIndex);

		// copy the obs
		while (nIndex < m_oTsObs.size())
			oObsArray.add(m_oTsObs.get(nIndex++));
	}
	

    /**
     * Gets the set of recent observations of type {@code nObsType} with
     * latitude, longitude, and timestamp values falling within the supplied
     * bounds. Store this set of observations in {@code oObsSet}.
     *
     * @param nObsType observation type of interest.
     * @param nLatMin minimum latitude bound.
     * @param nLonMin minimum longitude bound.
     * @param nLatMax maximum latitude bound.
     * @param nLonMax maximum longitude bound.
     * @param lPast minimum timestamp bound.
	 * @param lFuture maximum timstamp bound.
	 * @param oObsSet will contain the retrieved observation set.
     */
	public void getBackground(int nObsType, int nLatMin, int nLonMin,
		int nLatMax, int nLonMax, long lPast, long lFuture,
		ArrayList<IObs> oObsSet)
	{
		ObsSetMgr oObsSetMgr = getObsSetMgr(nObsType);
		if (oObsSetMgr != null)
		{
			oObsSetMgr.getBackground(oObsSet, nLatMin, 
				nLonMin, nLatMax, nLonMax, lPast, lFuture);
		}
	}
	

    /**
     * Retrieve the set of observations of the provided type, and supplied
     * sensor id, that fall within the given time bounds. Store this set of
	 * observations in {@code oObsSet}.
     *
     * @param nObsType observation type of interest.
     * @param nSensorId sensor of interest.
     * @param lPast lower time bound.
	 * @param lFuture upper time bound.
	 * @param oObsSet will contain the retrieved observation set.
     */
	public void getSensors(int nObsType, int nSensorId,
		long lPast, long lFuture, ArrayList<IObs> oObsSet)
	{
		ObsSetMgr oObsSetMgr = getObsSetMgr(nObsType);
		if (oObsSetMgr != null)
			oObsSetMgr.getSensors(oObsSet, nSensorId, lPast, lFuture);
	}
	

	/**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link ObsSetMgr} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link ObsSetMgr}
     *
     * @see ILockFactory
     * @see StripeLock
     */
	public ObsSetMgr getLock()
	{
		return new ObsSetMgr();
	}


	private class UniqueObs implements Comparator<Obs>
	{
		public int compare(Obs oLhs, Obs oRhs)
		{
			int nCompare = (int)(oLhs.m_lTimestamp - oRhs.m_lTimestamp);
			if (nCompare == 0)
			{
				nCompare = oLhs.m_nTypeId - oRhs.m_nTypeId;
				if (nCompare == 0)
					nCompare = oLhs.m_nSensorId - oRhs.m_nSensorId;
			}
			return nCompare;
		}
	}


	private class UpdatedObs implements Comparator<Obs>
	{
		public int compare(Obs oLhs, Obs oRhs)
		{
			// cast long differences are reliable for periods up to 24 days
			return (int)(oLhs.m_lUpdated - oRhs.m_lUpdated);
		}
	}
}
