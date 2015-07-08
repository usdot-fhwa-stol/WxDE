// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ObsSetMgr.java
 */
package wde.obs;

import org.apache.log4j.Logger;
import wde.dao.DatabaseManager;
import wde.dao.ObservationDao;
import wde.util.Introsort;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.GregorianCalendar;

/**
 * Manages sets of observations of a given type. Provides methods of querying,
 * registering, comparing, and processing of observation sets.
 * <p/>
 * <p>
 * Implements {@code Comparable<ObsSetMgr>} to allow comparison of observation
 * set managers by observation type as defined by
 * {@link ObsSetMgr#compareTo(wde.obs.ObsSetMgr)}.
 * </p>
 * <p>
 * Implements {@code ILockFactory} to allow mutually exclusive access of threads
 * to critical section of the {@code ObsMgr} through the use of
 * {@link StripeLock}
 * </p>
 */
public class ObsSetMgr implements Comparable<ObsSetMgr>, ILockFactory<ObsIter> {
    private static final Logger logger = Logger.getLogger(ObsSetMgr.class);
    /**
     * Observation type to be contained in this set manager.
     */
    int m_nObsType;
    /**
     * Default capacity of the observation array lists.
     */
    int m_nDefCapacity = 1000;
    /**
     * Marks the start of the interval used to determine when to cleanup.
     */
    private long m_lLastRun;
    /**
     * Configured length of time to retain observations in the set.
     */
    private long m_lLifetime;
    /**
     * The list of observations.
     */
    private ArrayList<Observation> m_oObs;
    /**
     * Recently added observations.
     */
    private ArrayList<Observation> m_oRecentObs;
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
    ObsSetMgr() {
    }


    /**
     * Initializes attributes of new instances of {@code ObsSetMgr} with the
     * supplied values.
     *
     * @param nObsType               The type of observation to be contained in the set.
     * @param lLifetime              length of time to retain observations in the set
     * @param nLockCount             number of locks to allocate to the observation set
     *                               manager.
     * @param nObsInitialCapacity    configured observation set capacity.
     * @param nSensorInitialCapacity configured sensor capacity.
     */
    ObsSetMgr(int nObsType, long lLifetime, int nLockCount) {
        m_nObsType = nObsType;
        m_lLifetime = lLifetime;
        m_oObsByHash = new ObsByHash();
        m_oObsBySensor = new ObsBySensor();
        m_oObsByTime = new ObsByTime();
        m_oObsLock = new StripeLock<ObsIter>(this, nLockCount);
        m_oRecentObsLock = new StripeLock<ObsIter>(this, nLockCount);
    }


    /**
     * Method that accumulated initial capacity for the obs arrays
     * so that previous obs can be reloaded from storage on restart.
     *
     * @param nCapacity incremental default capacity count
     */
    void updateCapacity(int nCapacity) {
        m_nDefCapacity += nCapacity;
    }


    /**
     * Parent managing component calls this method to instantiate the
     * observation arrays and populate them from previously saved observations.
     */
    void initialize() {
        if (m_oObs != null && m_oRecentObs != null)
            return; // reload task completed

        logger.info(String.format("obstypeid %d reload begin", m_nObsType));

        m_nDefCapacity *= 6; // used to calculate 20% margin
        m_oRecentObs = new ArrayList<Observation>(m_nDefCapacity / 25);
        m_oObs = new ArrayList<Observation>(m_nDefCapacity / 5);

        GregorianCalendar oNow = new GregorianCalendar();
        m_lLastRun = oNow.getTimeInMillis();
        oNow.setTimeInMillis(m_lLastRun - m_lLifetime);
        long lEnd = m_lLastRun + 2 * m_lLifetime;

        DatabaseManager oDbMgr = DatabaseManager.getInstance();
        String sConnId = oDbMgr.getConnection();

        m_oObsLock.writeLock();
        ObsIter oSearchObs = m_oRecentObsLock.writeLock();

        try {
            while (oNow.getTimeInMillis() < lEnd) {
                String tableName = String.format("obs_%d-%02d-%02d", oNow.get(GregorianCalendar.YEAR),
                        oNow.get(GregorianCalendar.MONTH) + 1, oNow.get(GregorianCalendar.DAY_OF_MONTH));
                DatabaseMetaData dbm = oDbMgr.getMetaData(sConnId);

                ResultSet tables = dbm.getTables(null, null, tableName, null);
                if (!tables.next()) {
                    tables.close();
                    logger.debug(tableName + " does not exist");
                } else {
                    tables.close();

                    String sQuery = String.format("SELECT obstypeid, sourceid, " +
                            "sensorid, obstime, recvtime, latitude, longitude, " +
                            "elevation, value, confvalue, qchcharflag FROM obs.\"" +
                            tableName + "\" WHERE obstypeid=%d", m_nObsType);

                    ResultSet oResultSet = oDbMgr.query(sConnId, sQuery);
                    while (oResultSet != null && oResultSet.next()) {
                        Observation oObs = ObservationDao.retrieveObs(oResultSet);

                        // identify the most recent obs
                        oObs.setHashValue(oSearchObs.getHash(oObs.getLatitude(), oObs.getLongitude()));
                        int nIndex = Introsort.binarySearch(m_oRecentObs, oObs, m_oObsByHash);

                        if (nIndex < 0) // add recent obs in sorted order
                            m_oRecentObs.add(~nIndex, oObs);
                        else {
                            // newer obs take precedence for this list
                            if (oObs.getObsTimeLong() > m_oRecentObs.get(nIndex).getObsTimeLong())
                                m_oRecentObs.set(nIndex, oObs);
                        }

                        m_oObs.add(oObs); // all loaded obs go in this list
                    }
                    oResultSet.close();
                }

                // increment time by one day
                oNow.setTimeInMillis(oNow.getTimeInMillis() + 86400000);
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        m_oRecentObsLock.writeUnlock();
        Introsort.usort(m_oObs, m_oObsBySensor); // sort reloaded obs
        logger.info(String.format("obstypeid %d reload end %d obs loaded",
                m_nObsType, m_oObs.size()));
        m_oObsLock.writeUnlock();
        oDbMgr.releaseConnection(sConnId);
    }

    /**
     * Removes duplicate observations from the set, then checks with the
     * existing cache for duplicates. Removes outdated observations, and updates
     * the cache with newer observation from the provided set.
     *
     * @param iObsSet observation set to process.
     */
    void processObsSet(ObsSet oObsSet) {
        if (oObsSet == null)
            return; // test for null obs set

        oObsSet.m_bReadOnly = true; // the obs is now read-only
        long lNow = new GregorianCalendar().getTimeInMillis();
        long lBegin = lNow - m_lLifetime; // initialize time references

        int nInnerIndex = oObsSet.size();
        if (!oObsSet.m_bIgnoreTime) // filter obs outside valid time range
        {
            long lEnd = lNow + m_lLifetime;
            while (nInnerIndex-- > 0) {
                long lObsTime = oObsSet.get(nInnerIndex).getObsTimeLong();
                if (lObsTime < lBegin || lObsTime > lEnd)
                    oObsSet.remove(nInnerIndex);
            }
            nInnerIndex = oObsSet.size(); // reset index for next obs filter
        }

        Introsort.usort(oObsSet, m_oObsByTime);
        int nIndex = nInnerIndex--;
        while (nInnerIndex-- > 0) // filter duplicate obs within the obs set
        {
            if (m_oObsByTime.compare(oObsSet.get(--nIndex),
                    oObsSet.get(nInnerIndex)) == 0)
                oObsSet.remove(nIndex);
        }

        boolean bClean = (lNow > m_lLastRun + 3600000); // hourly cleanup flag
        nInnerIndex = oObsSet.size(); // update most recent obs list
        ObsIter oSearchObs = m_oRecentObsLock.writeLock();

        if (bClean) // first cleanup recent obs list
        {
            m_lLastRun = lNow; // update cleanup time marker
            nIndex = m_oRecentObs.size();
            while (nIndex-- > 0)
                if (m_oRecentObs.get(nIndex).getObsTimeLong() < lBegin)
                    m_oRecentObs.remove(nIndex);
        }

        while (nInnerIndex-- > 0) {
            Observation oObs = oObsSet.get(nInnerIndex);

            // attempt to find an existing obs to update
            oObs.setHashValue(oSearchObs.getHash(oObs.getLatitude(), oObs.getLongitude()));
            nIndex = Introsort.binarySearch(m_oRecentObs, oObs, m_oObsByHash);

            if (nIndex < 0)
                m_oRecentObs.add(~nIndex, oObs);
            else {
                // only update the cache with newer obs
                if (oObs.getObsTimeLong() > m_oRecentObs.get(nIndex).getObsTimeLong())
                    m_oRecentObs.set(nIndex, oObs);
            }
        }
        m_oRecentObsLock.writeUnlock();

        nInnerIndex = oObsSet.size(); // reset source obs set index
        oSearchObs = m_oObsLock.writeLock(); // new obs are added to the list

        if (bClean) // clean up large historic obs list
        {
            nIndex = m_oObs.size();
            while (nIndex-- > 0)
                if (m_oObs.get(nIndex).getObsTimeLong() < lBegin)
                    m_oObs.remove(nIndex);
        }

        oSearchObs.clear();
        int nListSize = m_oObs.size(); // check for duplicates
        while (nInnerIndex-- > 0) {
            Observation oObs = oObsSet.get(nInnerIndex);
            // default search timestamp of 0 is never in the list and should
            // always find the lowest index for the current obs sensor
            oSearchObs.setSensorId(oObs.getSensorId());
            nIndex = ~Introsort.binarySearch(m_oObs, oSearchObs, m_oObsBySensor);

            boolean bFound = false; // linear duplicate search in obs subset
            while (!bFound && nIndex < nListSize &&
                    m_oObs.get(nIndex).getSensorId() == oObs.getSensorId()) {
                Observation oCachedObs = m_oObs.get(nIndex);
                bFound =
                        (
                                oObs.getSourceId() == oCachedObs.getSourceId() &&
                                        oObs.getObsTimeLong() == oCachedObs.getObsTimeLong() &&
                                        oObs.getObsTypeId() == oCachedObs.getObsTypeId() &&
                                        oObs.getLatitude() == oCachedObs.getLatitude() &&
                                        oObs.getLongitude() == oCachedObs.getLongitude() &&
                                        oObs.getElevation() == oCachedObs.getElevation()
                        );
                ++nIndex;
            }

            if (bFound) // remove duplicate obs from incoming obs set
                oObsSet.remove(nInnerIndex);
            else // new obs are added to the end of obs cache
                m_oObs.add(oObs);
        }

        Introsort.usort(m_oObs, m_oObsBySensor); // sort new obs into position
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
     * @param lPast   minimum timestamp bound.
     * @param lFuture maximum timstamp bound.
     */
    void getBackground(ArrayList<IObs> oObsSet, int nLatMin, int nLonMin,
                       int nLatMax, int nLonMax, long lPast, long lFuture) {
        ObsIter oSearchObs = m_oRecentObsLock.readLock();

        // initialize the hash iterator
        oSearchObs.iterator(nLatMin, nLonMin, nLatMax, nLonMax);
        while (oSearchObs.hasNext()) {
            oSearchObs.next();
            int nIndex = Collections.binarySearch(m_oRecentObs,
                    oSearchObs, m_oObsByHash);

            if (nIndex < 0)
                nIndex = ~nIndex;

            Observation oObs = null;
            while (nIndex < m_oRecentObs.size() &&
                    (oObs = m_oRecentObs.get(nIndex++)).getHashValue() == oSearchObs.getHashValue()) {
                // filter grouped obs further by exact space and time ranges
                if (oObs.getLongitude() >= nLonMin && oObs.getLatitude() >= nLatMin &&
                        oObs.getLongitude() <= nLonMax && oObs.getLatitude() <= nLatMax &&
                        oObs.getObsTimeLong() >= lPast && oObs.getObsTimeLong() <= lFuture)
                    oObsSet.add(oObs);
            }
        }

        m_oRecentObsLock.readUnlock();
    }


    /**
     * Retrieve the set of observations with the supplied sensor id, that fall
     * within the given time bounds.
     *
     * @param oObsSet   ArrayList of observations by sensor corresponding to the
     *                  provided sensor id.
     * @param nSensorId sensor id of interest.
     * @param lPast     lower time bound.
     * @param lFuture   upper time bound.
     */
    void getSensors(ArrayList<IObs> oObsSet, int nSensorId,
                    long lPast, long lFuture) {
        ObsIter oSearchObs = m_oObsLock.readLock();

        // configure the search criteria
        oSearchObs.setSensorId(nSensorId);
        oSearchObs.setObsTimeLong(lFuture);

        // create the list with the most recent obs first
        int nIndex = Collections.binarySearch(m_oObs, oSearchObs, m_oObsBySensor);

        // position the index even if an exact match is not found
        // this probably means the index points to the current obs
        if (nIndex < 0)
            nIndex = ~nIndex;

        // iterate for the current sensor id within the specified timerange
        Observation oObs = null;
        while (nIndex-- > 0 &&
                (oObs = m_oObs.get(nIndex)).getSensorId() == nSensorId &&
                oObs.getObsTimeLong() >= lPast) {
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
     * @see ILockFactory
     * @see StripeLock
     */
    public ObsIter getLock() {
        return new ObsIter();
    }


    /**
     * Compares <i> this </i> observation set manager to the supplied
     * observation set manager by their contained observation types.
     *
     * @param oObsSetMgr the manager to compare to <i> this </i>
     * @return 0 if the observation types match.
     */
    public int compareTo(ObsSetMgr oObsSetMgr) {
        return (m_nObsType - oObsSetMgr.m_nObsType);
    }


    /**
     * Provides a means of comparing and sorting according to hash value,
     * then by sensor id with the
     * {@link ObsByHash#compare(Observation, Observation)} method.
     */
    private class ObsByHash implements Comparator<Observation> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code ObsByHash}
         * </p>
         */
        private ObsByHash() {
        }

        /**
         * @param oLhs the {@code Obs} to compare to {@code oRhs}
         * @param oRhs the {@code Obs} to compare to {@code oLhs}
         * @return 0 if the observations match by hash or by sensor id.
         */
        public int compare(Observation oLhs, Observation oRhs) {
            // compare the hash codes and then the sensor id
            int nCompare = oLhs.getHashValue() - oRhs.getHashValue();
            if (nCompare == 0)
                return (oLhs.getSensorId() - oRhs.getSensorId());

            return nCompare;
        }
    }


    /**
     * Provides a means of comparing and sorting according to observation
     * sensor id, then by timestamp with the
     * {@link ObsBySensor#compare(Observation, Observation)} method.
     */
    private class ObsBySensor implements Comparator<Observation> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code ObsBySensor}
         * </p>
         */
        private ObsBySensor() {
        }


        /**
         * Compare by sensor id then by timestamp.
         * <p/>
         * <p>
         * *May not work well in the case of vehicle sensors.*
         * </p>
         *
         * @param oLhs the {@code Obs} to compare to {@code oRhs}
         * @param oRhs the {@code Obs} to compare to {@code oLhs}
         * @return 0 if the observations match by sensor id or by timestamp.
         */
        public int compare(Observation oLhs, Observation oRhs) {
            // compare the sensor id and then the timestamp
            // this may not work well in the case of vehicle sensors
            int nCompare = oLhs.getSensorId() - oRhs.getSensorId();
            if (nCompare == 0) {
//			    nCompare = oLhs.getSourceId() - oRhs.getSourceId();
//			    if (nCompare == 0)
                nCompare = ((int) (oLhs.getObsTimeLong() - oRhs.getObsTimeLong()));
            }

            return nCompare;
        }
    }


    /**
     * Provides a means of comparing and sorting according to observation
     * timestamps, then by sensor id with the
     * {@link ObsByTime#compare(Observation, Observation)} method.
     */
    private class ObsByTime implements Comparator<Observation> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code ObsByTime}
         * </p>
         */
        protected ObsByTime() {
        }


        /**
         * Compares {@link Observation} first by timestamp, then by sensor id.
         *
         * @param oLhs the {@code Obs} to compare to {@code oRhs}
         * @param oRhs the {@code Obs} to compare to {@code oLhs}
         * @return 0 if the observations match by timestamp or sensor id.
         */
        public int compare(Observation oLhs, Observation oRhs) {
            // compare the timestamp and then the sensor id
            // this may not work well in the case of vehicle sensors
            long lCompare = oLhs.getObsTimeLong() - oRhs.getObsTimeLong();
            if (lCompare == 0L) {
//			    lCompare = oLhs.getSourceId() - oRhs.getSourceId();
//			    if (lCompare == 0)
                lCompare = oLhs.getSensorId() - oRhs.getSensorId();
            }

            return (int) lCompare;
        }
    }
}
