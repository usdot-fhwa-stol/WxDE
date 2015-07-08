// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * ObsMgr.java
 */
package wde.obs;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.DatabaseManager;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.IntKeyValue;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Intermediary between {@link ClarusManager}, and the {@link ObsSetMgr}.
 * Processes observation sets, and queues observation sets to be processed
 * in the {@code WDEMgr} processor.
 * <p/>
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
public class ObsMgr extends AsyncQ<IObsSet> implements ILockFactory<ObsSetMgr> {
    private static final Logger logger = Logger.getLogger(ObsMgr.class);

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
     * Pointer to the singleton instance of {@code ObsMgr}.
     */
    private static ObsMgr g_oInstance = new ObsMgr();
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
     * Array of received obs ordered by the update timestamp.
     */
    private ArrayList<Observation> m_oTsObs = new ArrayList<Observation>();
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
     * Comparable object used to sort obs arrays by updated timestamp.
     */
    private UpdatedObs m_oUpdatedObs = new UpdatedObs();
    /**
     * Obs object used to find observations by timestamp.
     */
    private Observation m_oSearchObs = new Observation();
    /**
     * Pointer to the singleton instance of {@link WDEMgr}.
     */
    private WDEMgr wdeMgr = WDEMgr.getInstance();

    /**
     * <b> Default Constructor </b>
     * <p>
     * Configures the observation manager. Initializes and sorts the observation
     * set managers. Registers the observation manager with the Clarus
     * system.
     * </p>
     */
    private ObsMgr() {
        // apply the ObsMgr configuration
        ConfigSvc oConfigSvc = ConfigSvc.getInstance();
        Config oConfig = oConfigSvc.getConfig(this);

        // increase the queue depth for more thread concurrency
        MAX_THREADS = oConfig.getInt("queuedepth", MAX_THREADS);
        setMaxThreads(MAX_THREADS);
        m_oLock = new StripeLock<ObsSetMgr>(this, MAX_THREADS);

        // read the maximum time an obs can remain in any cache
        m_lLifetime = oConfig.getLong("lifetime", m_lLifetime);

        OBS_INITIAL_CAPACITY =
                oConfig.getInt("obsInitialCapacity", OBS_INITIAL_CAPACITY);

        SENSOR_INITIAL_CAPACITY =
                oConfig.getInt("sensorInitialCapacity", SENSOR_INITIAL_CAPACITY);

        allocate(); // pre-allocate array memory space

        // register obs mgr with the WDE manager
        wdeMgr.register(getClass().getName(), this);
        logger.info("Completing constructor");
    }

    /**
     * Returns the singleton instance of {@code ObsMgr}.
     *
     * @return the instance of {@code ObsMgr}.
     */
    public static ObsMgr getInstance() {
        return g_oInstance;
    }

    /**
     * Retrieves the count of observations by observation type to reduce later
     * array growth copy operations
     */
    private void allocate() {
        logger.info("reserve obs memory begin");

        GregorianCalendar oNow = new GregorianCalendar();
        oNow.setTimeInMillis(oNow.getTimeInMillis() - m_lLifetime);
        long lEnd = oNow.getTimeInMillis() + 2 * m_lLifetime;

        DatabaseManager oDbMgr = DatabaseManager.getInstance();
        String sConnId = oDbMgr.getConnection();

        ObsSetMgr oSearch = m_oLock.writeLock();

        try {
            while (oNow.getTimeInMillis() < lEnd) {
                String tableName = String.format("obs_%d-%02d-%02d", oNow.get(oNow.YEAR),
                        oNow.get(oNow.MONTH) + 1, oNow.get(oNow.DAY_OF_MONTH));
                DatabaseMetaData dbm = oDbMgr.getMetaData(sConnId);

                ResultSet tables = dbm.getTables(null, null, tableName, null);
                if (!tables.next()) {
                    tables.close();
                    logger.debug(tableName + " does not exist");
                    break;
                }
                tables.close();

                String sQuery = String.format("SELECT obstypeid, count(*) FROM obs.\"" + tableName + "\" GROUP BY obstypeid");

                ResultSet oResultSet = oDbMgr.query(sConnId, sQuery);
                while (oResultSet != null && oResultSet.next()) {
                    ObsSetMgr oObsSetMgr = null;
                    oSearch.m_nObsType = oResultSet.getInt(1);
                    int nIndex = Collections.binarySearch(m_oObsSetMgrs, oSearch);
                    if (nIndex < 0) {
                        oObsSetMgr = new ObsSetMgr(oSearch.m_nObsType,
                                m_lLifetime, MAX_THREADS);
                        m_oObsSetMgrs.add(~nIndex, oObsSetMgr);
                    } else
                        oObsSetMgr = m_oObsSetMgrs.get(nIndex);

                    oObsSetMgr.updateCapacity(oResultSet.getInt(2));
                }
                oResultSet.close();

                // increment time by one day
                oNow.setTimeInMillis(oNow.getTimeInMillis() + 86400000);
            }
        } catch (Exception oException) {
            // queried daily obs table does not exist
        }
        m_oLock.writeUnlock();
        oDbMgr.releaseConnection(sConnId);

        logger.info("reserve obs memory end");
    }

    /**
     * Finds and returns the observation set manager of the supplied type,
     * from the list of obs set managers ({@code m_oObsSetMgrs}).
     *
     * @param nObsType type of observation set manager to retrieve.
     * @return The observation set manager containing observations of the
     * supplied type {@code nObsType} if found in the list of observation
     * set managers. Otherwise null is returned.
     */
    private ObsSetMgr getObsSetMgr(int nObsType) {
        ObsSetMgr oObsSetMgr = null;
        ObsSetMgr oSearch = m_oLock.readLock();

        oSearch.m_nObsType = nObsType;
        int nIndex = Collections.binarySearch(m_oObsSetMgrs, oSearch);
        if (nIndex >= 0)
            oObsSetMgr = m_oObsSetMgrs.get(nIndex);

        m_oLock.readUnlock();

        if (nIndex < 0) // previously undefined obs type was requested
        {
            oSearch = m_oLock.writeLock();

            oSearch.m_nObsType = nObsType;
            nIndex = Collections.binarySearch(m_oObsSetMgrs, oSearch);
            if (nIndex < 0) // check again in case another thread created
            {
                oObsSetMgr = new ObsSetMgr(oSearch.m_nObsType,
                        m_lLifetime, MAX_THREADS);
                m_oObsSetMgrs.add(~nIndex, oObsSetMgr);
            } else
                oObsSetMgr = m_oObsSetMgrs.get(nIndex);

            m_oLock.writeUnlock();
        }

        oObsSetMgr.initialize(); // hot start from storage when necessary
        return oObsSetMgr; // obs set manager should always be available
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
    public synchronized ObsSet getObsSet(int nObsType) {
        int nIndex = 0;
        ObsSet oObsSet = null;

        // keep generating serial numbers until an available one is found
        while (nIndex >= 0) {
            int nSerial = m_oSerial.getAndIncrement();
            m_oSearchObsSet.setKey(nSerial);
            nIndex = Collections.binarySearch(m_oObsSets, m_oSearchObsSet);
            // verify that the numbered obs set is not in the list
            if (nIndex < 0) {
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
     * The observation set is then queued into the WDE manager sequence
     * queue.
     * <p>
     * Overrides base class method {@link AsyncQ#run()}.
     * </p>
     *
     * @param iObsSet observation set to process and queue.
     */
    @Override
    public void run(IObsSet iObsSet) {
        // first, convert the obs set interface back to an obs set object
        // so that more direct, and faster, operations can be applied
        ObsSet oObsSet = null;
        synchronized (this) {
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
            wdeMgr.queue(iObsSet);
    }


    /**
     * Gets the set of recent observations of type {@code nObsType} with
     * latitude, longitude, and timestamp values falling within the supplied
     * bounds. Store this set of observations in {@code oObsSet}.
     *
     * @param nObsType observation type of interest.
     * @param nLatMin  minimum latitude bound.
     * @param nLonMin  minimum longitude bound.
     * @param nLatMax  maximum latitude bound.
     * @param nLonMax  maximum longitude bound.
     * @param lPast    minimum timestamp bound.
     * @param lFuture  maximum timstamp bound.
     * @param oObsSet  will contain the retrieved observation set.
     */
    public void getBackground(int nObsType, int nLatMin, int nLonMin,
                              int nLatMax, int nLonMax, long lPast, long lFuture,
                              ArrayList<IObs> oObsSet) {
        ObsSetMgr oObsSetMgr = getObsSetMgr(nObsType);
        if (oObsSetMgr != null) {
            oObsSetMgr.getBackground(oObsSet, nLatMin,
                    nLonMin, nLatMax, nLonMax, lPast, lFuture);
        }
    }

    /**
     * Retrieve the set of observations of the provided type, and supplied
     * sensor id, that fall within the given time bounds. Store this set of
     * observations in {@code oObsSet}.
     *
     * @param nObsType  observation type of interest.
     * @param nSensorId sensor of interest.
     * @param lPast     lower time bound.
     * @param lFuture   upper time bound.
     * @param oObsSet   will contain the retrieved observation set.
     */
    public void getSensors(int nObsType, int nSensorId,
                           long lPast, long lFuture, ArrayList<IObs> oObsSet) {
        ObsSetMgr oObsSetMgr = getObsSetMgr(nObsType);
        if (oObsSetMgr != null)
            oObsSetMgr.getSensors(oObsSet, nSensorId, lPast, lFuture);
    }

    /**
     * @return
     */
    public long getLifeTime() {
        return m_lLifetime;
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
     * @see ILockFactory
     * @see StripeLock
     */
    public ObsSetMgr getLock() {
        return new ObsSetMgr();
    }

    private class UpdatedObs implements Comparator<Observation> {
        public int compare(Observation oLhs, Observation oRhs) {
            // cast long differences are reliable for periods up to 24 days
            return (int) (oLhs.getRecvTimeLong() - oRhs.getRecvTimeLong());
        }
    }
}
