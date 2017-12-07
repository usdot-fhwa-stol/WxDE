// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file PlatformMonitor.java
 */
package wde.qeds;

import org.apache.log4j.Logger;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import wde.WDEMgr;
import wde.dao.DatabaseManager;
import wde.dao.ObservationDao;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Introsort;
import wde.util.Scheduler;
import wde.util.threads.AsyncQ;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * Wraps all cached platforms and their corresponding set of observations
 * together to be registered and processed by the WDE manager.
 * <p>
 * Singleton class whose instance can be retrieved with the
 * {@link PlatformMonitor#getInstance()}.
 * </p>
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of the observations set
 * as it is enqueued.
 * </p>
 */
public class PlatformMonitor extends AsyncQ<IObsSet> {
    private static final Logger logger = Logger.getLogger(PlatformMonitor.class);

    /**
     * Configured timeout, defaults to 14400000. Determines when platforms
     * should be flagged as having new observations (if it has been updated
     * since the timeout).
     */
    private static long TIMEOUT = 14400000;
    /**
     * Pointer to platform monitor instance.
     */
    private static PlatformMonitor g_oInstance = new PlatformMonitor();
    /**
     * List of platforms, and their corresponding observations.
     */
    private final ArrayList<PlatformObs> m_oPlatformList = new ArrayList<>();
    /**
     * Pointer to {@code WDEMgr} singleton instance.
     */
    private WDEMgr wdeMgr = WDEMgr.getInstance();
    /**
     * Pointer to {@code PlatformDao}.
     */
    private PlatformDao platformDao = PlatformDao.getInstance();
    /**
     * Pointer to {@code Sensors} cache.
     */
    private SensorDao sensorDao = SensorDao.getInstance();
    /**
     * Platform-observation pair used for searching the platform list.
     */
    private PlatformObs m_oSearchPlatform = new PlatformObs();
    /**
     * Comparator enforces ordering by platform id, lat and lon.
     */
    private SortByIdLatLon m_oSortByIdLatLon = new SortByIdLatLon();
    /**
     * Comparator enforces ordering by platform id.
     */
    private SortById m_oSortById = new SortById();
    /**
     * Comparator enforces ordering by platform code.
     */
    private SortByCode m_oSortByCode = new SortByCode();
    /**
     * Object used to schedule removal time of expired obs and mobile platforms.
     */
    private Cleanup m_oCleanup = new Cleanup();


    /**
     * Creates new instances of {@code PlatformnMonitor}. Configures the platform
     * monitor. Populates the platform list with cached platforms. Updates the
     * platform distribution group with the corresponding cached sensor
     * distribution group. Registers the platform monitor with the system
     * manager.
     */
    private PlatformMonitor() {
        // apply the platform monitor configuration
        ConfigSvc oConfigSvc = ConfigSvc.getInstance();
        Config oConfig = oConfigSvc.getConfig(this);

        // increase the queue depth for more thread concurrency
        TIMEOUT = oConfig.getLong("timeout", TIMEOUT);

        // initialize the platform list
        ArrayList<IPlatform> platforms = platformDao.getActivePlatforms();

        int nIndex = platforms.size();
        m_oPlatformList.ensureCapacity(nIndex);
        while (nIndex-- > 0) {
            IPlatform platform = platforms.get(nIndex);
            if (platform == null)
                logger.info("***********found one platform that is null");

            PlatformObs oPlatformObs = new PlatformObs(platform);
            if (platform.getContribId() == 4)
                oPlatformObs.m_nDistGroup = 1;
            m_oPlatformList.add(oPlatformObs);
        }


        initialize(); // must be last to set correct sort order

        // schedule cleanup process every five minutes
        Scheduler.getInstance().schedule(m_oCleanup, 13, 300, false);

        // register the platform monitor with system manager
        wdeMgr.register(getClass().getName(), this);
        logger.info("Completing constructor");
    }

    /**
     * <b> Accessor </b>
     *
     * @return singleton instance of {@code PlatformMonitor}.
     */
    public static PlatformMonitor getInstance() {
        return g_oInstance;
    }

    /**
     * Connects observations with platforms. If the platform is not in the list,
     * one is created and added to the list. Otherwise, the observation
     * is added to the set of observations contained by the corresponding
     * platform. The platform is then updated with the current time to indicate
     * that it contains current information.
     *
     * @param iObs individual observation to associate with a platform.
     */
    public void updatePlatform(IObs iObs) {
        PlatformObs oPlatform = null;

        // find the sensor and then the platform to which it belongs
        ISensor iSensor = sensorDao.getSensor(iObs.getSensorId());
        if (iSensor == null)
            return;

        IPlatform platform = platformDao.getPlatform(iSensor.getPlatformId());
        if (platform == null)
            return;

        synchronized (m_oPlatformList) {
            // search for the platform in the managed list
            m_oSearchPlatform.m_nId = platform.getId();

            int nPlatformIndex = Collections.binarySearch(m_oPlatformList,
                    m_oSearchPlatform, m_oSortById);

            // add new platforms as they become available
            if (nPlatformIndex < 0) {
                oPlatform = new PlatformObs(platform);
                m_oPlatformList.add(~nPlatformIndex, oPlatform);
            } else
                oPlatform = m_oPlatformList.get(nPlatformIndex);
        }

        // update the platform distribution group to the highest available
        if (iSensor.getDistGroup() > oPlatform.m_nDistGroup)
            oPlatform.m_nDistGroup = iSensor.getDistGroup();

        if (oPlatform.addObs(iObs)) {
            boolean updatePosition = false;
            int sourceId = iObs.getSourceId();
            if (sourceId == 1) {
                if (iObs.getObsTimeLong() >= oPlatform.m_lLastWxDEUpdate) {
                    oPlatform.m_lLastWxDEUpdate = iObs.getObsTimeLong();
                    updatePosition = true;
                }
            } else {
                if (iObs.getObsTimeLong() >= oPlatform.m_lLastVDTUpdate) {
                    oPlatform.m_lLastVDTUpdate = iObs.getObsTimeLong();
                    updatePosition = true;
                }
            }
            if (updatePosition) {
                oPlatform.m_nLat = iObs.getLatitude(); // copy obs location
                oPlatform.m_nLon = iObs.getLongitude(); // for mobile platforms
                oPlatform.m_tElev = iObs.getElevation();
            }
        }

        logger.debug("updatePlatform() returning");
    }

    /**
     * Traverses the provided observation set and determines which platform
     * should be updated with the new observation data. The observation set is
     * then queued to the system manager for subsequent processing.
     *
     * @param iObsSet set of observations to process.
     */
    @Override
    public void run(IObsSet iObsSet) {
        // add the obs to the platform and set the latest update timestamp
        int nObsIndex = iObsSet.size();
        while (nObsIndex-- > 0)
            updatePlatform(iObsSet.get(nObsIndex));

        // queue the obs set for the next process
        wdeMgr.queue(iObsSet);
    }

    /**
     * Retrieves the platform from the list corresponding to the provided
     * platform id.
     *
     * @param nId id of the platform of interest.
     * @return the platform corresponding to the supplied id if contained in the
     * list, else null.
     */
    public PlatformObs getPlatform(int nId, int nLat, int nLon) {
        synchronized (m_oPlatformList) {
            m_oSearchPlatform.m_nId = nId;
//            m_oSearchPlatform.m_nLat = nLat;
//            m_oSearchPlatform.m_nLon = nLon;
//

//          int nIndex = Collections.binarySearch(m_oPlatformList,
//              m_oSearchPlatform, m_oSortByIdLatLon);
            int nIndex = Collections.binarySearch(m_oPlatformList,
                    m_oSearchPlatform, m_oSortById);

            if (nIndex >= 0)
                return m_oPlatformList.get(nIndex);
        }

        return null;
    }

    /**
     * <b> Accessor </b>
     *
     * @return a copy of the platform-obs list.
     */
    public ArrayList<PlatformObs> getPlatforms() {
        ArrayList<PlatformObs> oPlatforms = new ArrayList<>();

        synchronized (m_oPlatformList) {
            int nIndex = m_oPlatformList.size();
            oPlatforms.ensureCapacity(nIndex);
            while (nIndex-- > 0)
                oPlatforms.add(m_oPlatformList.get(nIndex));
        }

        return oPlatforms;
    }

    /**
     * Populates the current set of platforms with the most recent observations
     * from storage. This is used to hot start the in-memory view that supports
     * browser map and other data interfaces.
     */
    private void initialize() {
        logger.info("restore PlatformMonitor begin");
        Introsort.usort(m_oPlatformList, m_oSortByIdLatLon); // sort by platform id

        GregorianCalendar oNow = new GregorianCalendar();
        oNow.setTimeInMillis(oNow.getTimeInMillis() - TIMEOUT);
        long lEnd = oNow.getTimeInMillis() + 2 * TIMEOUT;

        DatabaseManager oDbMgr = DatabaseManager.getInstance();
        String sConnId = oDbMgr.getConnection();

        try {
            String sPrevTable = "";
            while (oNow.getTimeInMillis() < lEnd) {
                String sTableName = String.format("obs_%d-%02d-%02d",
                        oNow.get(Calendar.YEAR), oNow.get(Calendar.MONTH) + 1,
                        oNow.get(Calendar.DAY_OF_MONTH));

                if (sTableName.compareTo(sPrevTable) == 0)
                    break; // quit when last table name needed is reached

                sPrevTable = sTableName; // save current table name
                DatabaseMetaData oDbMeta = oDbMgr.getMetaData(sConnId);
                ResultSet oTables = oDbMeta.getTables(null, null, sTableName, null);
                if (!oTables.next()) {
                    oTables.close();
                    logger.debug(sTableName + " does not exist");
                } else {
                    oTables.close();

                    String sQuery = String.format("SELECT obstypeid, sourceid, " +
                                    "sensorid, obstime, recvtime, latitude, longitude, " +
                                    "elevation, value, confvalue, qchcharflag " +
                                    "FROM obs.\"%s\" WHERE recvtime >= '%d-%02d-%02d %02d:%02d'",
                            sTableName, oNow.get(Calendar.YEAR), oNow.get(Calendar.MONTH) + 1,
                            oNow.get(Calendar.DAY_OF_MONTH), oNow.get(Calendar.HOUR_OF_DAY),
                            oNow.get(Calendar.MINUTE));

                    ResultSet oResultSet = oDbMgr.query(sConnId, sQuery);
                    while (oResultSet != null && oResultSet.next())
                        updatePlatform(ObservationDao.retrieveObs(oResultSet));

                    oResultSet.close();
                }

                // increment time by one day
                oNow.setTimeInMillis(oNow.getTimeInMillis() + TIMEOUT);
            }
        } catch (Exception oException) {
            logger.error("PlatformMonitor initialize", oException);
        }

        oDbMgr.releaseConnection(sConnId);
        logger.info("restore PlatformMonitor end");

        updatePlatforms();
    }


    private void updatePlatforms() {
        logger.info("updatePlatforms() invoked");

        long lNow = System.currentTimeMillis();
        long lExpired = lNow - TIMEOUT;
        long lExpiredMobileWxT = lNow - 1800000; // half an hour timeout
        long lExpiredMobile = lNow - 3600000; // one hour timeout

        synchronized (m_oPlatformList) {
            int nIndex = m_oPlatformList.size();
            while (nIndex-- > 0) {
                PlatformObs oPlatform = m_oPlatformList.get(nIndex);

                synchronized (oPlatform) {
                    // flag all platforms that have obs older than the timeout
                    oPlatform.m_bHasWxDEObs = (oPlatform.m_lLastWxDEUpdate >= lExpired);
                    oPlatform.m_bHasVDTObs = (oPlatform.m_lLastVDTUpdate >= lExpired);
                }

                boolean isMobile = (oPlatform.m_iPlatform.getCategory() == 'M');
                boolean isWxT = (oPlatform.m_iPlatform.getContribId() == 73);
                long lExpM = isWxT ? lExpiredMobileWxT : lExpiredMobile;
                long lExp = isMobile ? lExpM : lExpired;

                // expired mobile platforms get removed from the list
                if (isMobile && oPlatform.m_lLastWxDEUpdate < lExpM && oPlatform.m_lLastVDTUpdate < lExpM)
                    m_oPlatformList.remove(nIndex);

                IObs obs = null;
                ArrayList<IObs> obsList = oPlatform.m_oObs;
                while (obsList.size() > 0) {
                    obs = obsList.get(0);
                    if (obs.getObsTimeLong() < lExp)
                        obsList.remove(0);
                    else
                        break;
                }

            }
        }
        logger.info("updatePlatforms() returning");
    }

    /**
     * Implements {@code Comparator<PlatformObs>} to enforce an ordering based
     * on platform id.
     */
    private class SortByIdLatLon implements Comparator<PlatformObs> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code SortById}
         * </p>
         */
        private SortByIdLatLon() {
        }


        /**
         * Compares the provided {@code PlatformObs} by platform id.
         *
         * @param oLhs object to compare to {@code oRhs}
         * @param oRhs object to compare to {@code oLhs}
         * @return 0 if the objects match by platform id and geo-coordinates.
         */
        @Override
        public int compare(PlatformObs oLhs, PlatformObs oRhs) {
            int nDiff = oLhs.m_nId - oRhs.m_nId;
            if (nDiff != 0)
                return nDiff;

            nDiff = oLhs.m_nLat - oRhs.m_nLat;
            if (nDiff != 0)
                return nDiff;

            return (oLhs.m_nLon - oRhs.m_nLon);
        }
    }

    /**
     * Implements {@code Comparator<PlatformObs>} to enforce an ordering based
     * on platform id.
     */
    private class SortById implements Comparator<PlatformObs> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code SortById}
         * </p>
         */
        private SortById() {
        }


        /**
         * Compares the provided {@code PlatformObs} by platform id.
         *
         * @param oLhs object to compare to {@code oRhs}
         * @param oRhs object to compare to {@code oLhs}
         * @return 0 if the objects match by platform id and geo-coordinates.
         */
        @Override
        public int compare(PlatformObs oLhs, PlatformObs oRhs) {
            return (oLhs.m_nId - oRhs.m_nId);
        }
    }


    /**
     * Implements {@code Comparator<PlatformObs>} to enforce an ordering based
     * on platform code.
     */
    private class SortByCode implements Comparator<PlatformObs> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code SortByCode}
         * </p>
         */
        private SortByCode() {
        }


        /**
         * Compares the provided {@code PlatformObs} by platform code.
         *
         * @param oLhs object to compare to {@code oRhs}
         * @param oRhs object to compare to {@code oLhs}
         * @return 0 if the objects match by platform code.
         */
        @Override
        public int compare(PlatformObs oLhs, PlatformObs oRhs) {
            return (oLhs.m_sCode.compareTo(oRhs.m_sCode));
        }
    }


    private class Cleanup implements Runnable {
        Cleanup() {
        }


        @Override
        public void run() {
            updatePlatforms();
        }
    }
}
