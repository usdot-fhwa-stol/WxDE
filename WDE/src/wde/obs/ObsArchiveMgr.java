/************************************************************************
 * Source filename: ObsArchiveMgr.java
 * <p/>
 * Creation date: May 2, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * Moved archive portion of the ObsMgr into this class so it can be
 * loaded sequentially after both the ObsMgr and QChSMgr
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.obs;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.ObsTypeDao;
import wde.metadata.ObsType;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Intermediary between {@link WDEManager}, and the {@link ObsSetMgr}.
 * Processes observation sets, and queues observation sets to be processed
 * in the {@code WDEMgr} processor.
 *
 * <p>
 * Singleton class whose instance can be retrieved with
 * {@link ObsArchiveMgr#getInstance()}
 * </p>
 * <p>
 * Extends {@code AsyncQ} to allow processing of observation sets, as they're
 * added to the queue.
 * </p>
 * <p>
 * Implements {@code ILockFactory} to allow mutually exclusive access of threads
 * to critical section of the {@code ObsArchiveMgr} through the use of
 * {@link StripeLock}
 * </p>
 */
public class ObsArchiveMgr extends AsyncQ<IObsSet> implements
        ILockFactory<ObsSetMgr> {
    private static final Logger logger = Logger.getLogger(ObsArchiveMgr.class);
    /**
     * Observation insert query format.
     */
    private static final String QUERY_INSERT = "INSERT INTO obs.obs (obsTypeId, "
            + "sourceId, sensorId, obsTime, recvTime, latitude, longitude, elevation, value, "
            + "confValue, qchcharflag) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::character[])";
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
    private static ObsArchiveMgr g_oInstance = new ObsArchiveMgr();

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
     * Persistent datasource interface reference.
     */
    private DataSource m_iDataSource;

    /**
     * Array of received obs ordered by timestamp, obs type, and sensor id.
     */
    private ArrayList<Observation> m_oObs = new ArrayList<Observation>();

    /**
     * Array of received obs ordered by the update timestamp.
     */
    private ArrayList<Observation> m_oTsObs = new ArrayList<Observation>();

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
    private ObsArchiveMgr() {
        // apply the ObsMgr configuration
        ConfigSvc oConfigSvc = ConfigSvc.getInstance();
        Config oConfig = oConfigSvc.getConfig(this);

        // increase the queue depth for more thread concurrency
        MAX_THREADS = oConfig.getInt("queuedepth", MAX_THREADS);
        setMaxThreads(MAX_THREADS);

        // read the maximum time an obs can remain in any cache
        m_lLifetime = oConfig.getLong("lifetime", m_lLifetime);

        // save the datasource name to establish database connections later
        m_iDataSource = wdeMgr.getDataSource(oConfig.getString("datasource",
                null));

        OBS_INITIAL_CAPACITY = oConfig.getInt("obsInitialCapacity",
                OBS_INITIAL_CAPACITY);

        SENSOR_INITIAL_CAPACITY = oConfig.getInt("sensorInitialCapacity",
                SENSOR_INITIAL_CAPACITY);

        // apply the default configuration
        oConfig = oConfigSvc.getConfig("_default");
        String[] sCacheTypes = oConfig.getStringArray("obstype");
        if (sCacheTypes != null && sCacheTypes.length > 0) {
            // initialize the obs set managers
            int nIndex = sCacheTypes.length;
            m_oObsSetMgrs.ensureCapacity(nIndex);
            ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();
            while (nIndex-- > 0) {
                // resolve the obs type name to an obs type id
                ObsType obsType = obsTypeDao.getObsType(sCacheTypes[nIndex]);

                if (obsType != null) {
                    m_oObsSetMgrs.add(new ObsSetMgr(Integer.valueOf(obsType
                            .getId()), m_lLifetime, MAX_THREADS));
                }
            }
            Collections.sort(m_oObsSetMgrs);
        }

        // register obs mgr with the WDE manager
        wdeMgr.register(getClass().getName(), this);
        logger.info("Completing constructor");
    }

    /**
     * Returns the singleton instance of {@code ObsArchiveMgr}.
     * @return the instance of {@code ObsArchiveMgr}.
     */
    public static ObsArchiveMgr getInstance() {
        return g_oInstance;
    }

    public synchronized void getObs(ArrayList<IObs> oObsArray, long lTimestamp) {
        // find the earliest occurrence of the provided timestamp
        m_oSearchObs.setRecvTimeLong((lTimestamp));
        int nIndex = Collections.binarySearch(m_oTsObs, m_oSearchObs, m_oUpdatedObs);

        if (nIndex < 0)
            nIndex = ~nIndex;
        else {
            // shift the index to before the lowest index of a matching obs
            // as binary search is not guaranteed to find the first occurrence
            while (nIndex >= 0 &&
                    m_oTsObs.get(nIndex--).getRecvTimeLong() >= lTimestamp) ;

            ++nIndex;
        }

        // reserve enough space in the destination obs array
        oObsArray.ensureCapacity(m_oTsObs.size() - nIndex);

        // copy the obs
        while (nIndex < m_oTsObs.size())
            oObsArray.add(m_oTsObs.get(nIndex++));
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
     * @param iObsSet observation set to process and queue.
     */
    @Override
    public void run(IObsSet iObsSet) {

        // when an obs set contains only expired or duplicate obs 
        // there is no need to queue an empty set for the next process
        if (iObsSet.size() > 0)
            wdeMgr.queue(iObsSet);

        // determine if there is anything to save to the database
        if (m_iDataSource == null || iObsSet == null) return;

        // save the current time to apply to each obs as needed
        long lNow = System.currentTimeMillis();

        ObsSet obsSet = (ObsSet) iObsSet;
        int nIndex = obsSet.size();
        if (nIndex == 0) return;

        // Since run() is reentrant, need to synchronize the following block
        // to prevent race condition
        synchronized (this) {
            ArrayDeque<Observation> oInsertQueue = new ArrayDeque<Observation>();

            Observation obs = null;
            // determine if the obs needs to be inserted
            while (nIndex-- > 0) {
                obs = obsSet.get(nIndex);

                // first, check if the obs exists
                int nSearchIndex = Collections.binarySearch(m_oObs, obs, m_oUniqueObs);

                // the obs is added new when it does not exist
                if (nSearchIndex < 0) {
                    obs.setRecvTimeLong(lNow);
                    m_oObs.add(~nSearchIndex, obs);
                    // the obs is also added to the end of the timestamp array
                    m_oTsObs.add(obs);

                    oInsertQueue.add(obs);
                }
            }

            // remove obs that have not been updated within the last 65 minutes
            m_oSearchObs.setRecvTimeLong(lNow - 3900000L);
            nIndex = Collections.binarySearch(m_oTsObs, m_oSearchObs,
                    m_oUpdatedObs);

            // it does not matter here if the binary search is off a bit
            if (nIndex < 0) nIndex = ~nIndex;

            // remove expired obs from both lists
            while (nIndex-- > 0) {
                // these calls result in several array copy operations
                obs = m_oTsObs.remove(nIndex);
                int nObsIndex = Collections.binarySearch(m_oObs, obs,
                        m_oUniqueObs);

                if (nObsIndex >= 0) m_oObs.remove(nObsIndex);
            }

            if (oInsertQueue.isEmpty()) return;

            // handle inserts before updates
            try {
                Connection iConnection = m_iDataSource.getConnection();
                if (iConnection == null) return;

                Timestamp oObsTs = new Timestamp(0L);
                Timestamp recvTs = new Timestamp(0L);

                iConnection.setAutoCommit(false);

                PreparedStatement iQuery = iConnection
                        .prepareStatement(QUERY_INSERT);

                while (!oInsertQueue.isEmpty()) {
                    try {
                        obs = oInsertQueue.pop();
                        oObsTs = obs.getObsTime();
                        recvTs = obs.getRecvTime();

                        String qchCharFlagStr = null;
                        char[] flags = obs.getQchCharFlag();
                        if (flags != null) {
                            qchCharFlagStr = "";
                            for (char aChar : flags)
                                qchCharFlagStr += "\"" + aChar + "\",";
                            qchCharFlagStr = "{"
                                    + qchCharFlagStr.substring(0,
                                    qchCharFlagStr.length() - 1) + "}";
                        }

                        iQuery.setInt(1, obs.getObsTypeId());
                        iQuery.setInt(2, 1); // 1 for WxDE
                        iQuery.setInt(3, obs.getSensorId());
                        iQuery.setTimestamp(4, oObsTs);
                        iQuery.setTimestamp(5, recvTs);
                        iQuery.setInt(6, obs.getLatitude());
                        iQuery.setInt(7, obs.getLongitude());
                        iQuery.setInt(8, obs.getElevation());
                        iQuery.setDouble(9, obs.getValue());
                        iQuery.setFloat(10, obs.getConfValue());
                        iQuery.setString(11, qchCharFlagStr);

                        iQuery.executeUpdate();
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(e.getMessage());
                    }
                }

                // commit the database changes
                iConnection.commit();
                iQuery.close();

                iConnection.close();
            } catch (SQLException oSqlException) {
                logger.error(oSqlException);
            }
        }
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
    public ObsSetMgr getLock() {
        return new ObsSetMgr();
    }

    private class UniqueObs implements Comparator<Observation> {
        public int compare(Observation oLhs, Observation oRhs) {
            int nCompare = oLhs.getSourceId() - oRhs.getSourceId();
            if (nCompare == 0) {
                nCompare = (int) (oLhs.getObsTimeLong() - oRhs.getObsTimeLong());
                if (nCompare == 0) {
                    nCompare = oLhs.getObsTypeId() - oRhs.getObsTypeId();
                    if (nCompare == 0)
                        nCompare = oLhs.getSensorId() - oRhs.getSensorId();
                }
            }
            return nCompare;
        }
    }

    private class UpdatedObs implements Comparator<Observation> {
        public int compare(Observation oLhs, Observation oRhs) {
            // cast long differences are reliable for periods up to 24 days
            return (int) (oLhs.getRecvTimeLong() - oRhs.getRecvTimeLong());
        }
    }
}
