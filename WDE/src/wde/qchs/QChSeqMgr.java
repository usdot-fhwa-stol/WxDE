// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QChSeqMgr.java
 */
package wde.qchs;

import org.apache.log4j.Logger;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.qchs.algo.QChResult;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Receives observation sets to quality check, and filters them into the correct
 * quality check sequence for its climate-region id through the
 * {@link QChSeqMgr#run(wde.obs.IObsSet)} method.
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of observation sets as
 * they are enqueued.
 * </p>
 * <p>
 * Implements {@code Comparable<QChSeqMgr>} to enforce an ordering on sequence
 * managers by observation type.
 * </p>
 * <p>
 * Implements {@code ILockFactory<QChSeq>} mutually exclusive access of quality
 * check sequences through the use of {@link StripeLock} containers.
 * </p>
 */
public class QChSeqMgr extends AsyncQ<IObsSet>
        implements Comparable<QChSeqMgr>, ILockFactory<QChSeq> {
    static Logger logger = Logger.getLogger(QChSeqMgr.class);

    /**
     * Qch Sequence Manager database query format string.
     * Quality checks are based on the climate region.
     */
    private static String QCHSEQMGR_QUERY = "SELECT id, climateId " +
            "FROM conf.qchseqmgr WHERE obsTypeId = ? AND active = 1";

    /**
     * Observation type being quality checked by this manager.
     */
    private int m_nObsTypeId;
    /**
     * List of quality check sequences ordered by climate id.
     */
    private ArrayList<QChSeq> m_oSeq;
    /**
     * Provides mutually exclusive access to quality checking sequences.
     */
    private StripeLock<QChSeq> m_oSeqLock;
    /**
     * Provides mutually exclusive access to qualtity check results.
     */
    private StripeLock<QChResult> m_oResultLock;
    /**
     * Pointer to the sensors cache singleton instance.
     */
    private SensorDao sensorDao;
    /**
     * Pointer to the platforms cache singleton instance.
     */
    private PlatformDao platformDao;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code QChSeqMgr}
     * </p>
     */
    QChSeqMgr() {
    }


    /**
     * Initializes new instances of {@code QChSeqMgr} with the provided values.
     * Queries the database to populate the sorted list of quality check
     * sequences, setting their climate-region id..
     *
     * @param nObsTypeId  observation type id for this manager, used for the
     *                    database query.
     * @param nMaxThreads max threads to allocate to processing the quality
     *                    check sequence and result {@link StripeLock} containers.
     * @param iConnection connection to the datasource, ready for queries prior
     *                    to this method call.
     */
    QChSeqMgr(int nObsTypeId, int nMaxThreads, Connection iConnection) {
        setObsTypeId(nObsTypeId);
        setMaxThreads(nMaxThreads);

        m_oSeq = new ArrayList<QChSeq>();
        m_oSeqLock = new StripeLock<QChSeq>(this, nMaxThreads);
        m_oResultLock =
                new StripeLock<QChResult>(new ResultFactory(), nMaxThreads);
        sensorDao = SensorDao.getInstance();
        platformDao = PlatformDao.getInstance();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = iConnection.prepareStatement(QCHSEQMGR_QUERY);
            ps.setInt(1, nObsTypeId);

            rs = ps.executeQuery();
            while (rs.next())
                m_oSeq.add(new QChSeq(rs.getInt(1),
                        rs.getInt(2), iConnection));

            Collections.sort(m_oSeq);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }


    /**
     * <b> Mutator </b>
     *
     * @param nObsTypeId observation type id value for this sequence manager.
     */
    void setObsTypeId(int nObsTypeId) {
        m_nObsTypeId = nObsTypeId;
    }


    /**
     * Quality checks the observations in the set that haven't been checked, and
     * that have sensors with distribution group 2.
     * <p>
     * Uses the default-region quality check sequence if the sequence isn't
     * available for the given observations climate-region.
     * </p>
     * <p>
     * Overrides {@code AsyncQ} default run method.
     * </p>
     *
     * @param iObsSet set of observations to quality check.
     */
    @Override
    public void run(IObsSet iObsSet) {
        // an appropriate sequence must be found for each obs in the set
        for (int nObsIndex = 0; nObsIndex < iObsSet.size(); nObsIndex++) {
            IObs iObs = iObsSet.get(nObsIndex);

            // only quality check obs without existing quality check flags
            if (iObs.getQchCharFlag() != null)
                continue;

            // only sensors with distgroup 0 or 2 are checked
            ISensor iSensor = sensorDao.getSensor(iObs.getSensorId());
            if (iSensor == null || iSensor.getDistGroup() == 1)
                continue;

            QChSeq oSeq = null;
            // always search for the default climate id sequence
            QChSeq oSearchSeq = m_oSeqLock.readLock();
            oSearchSeq.setClimateId(0);
            int nSeqIndex = Collections.binarySearch(m_oSeq, oSearchSeq);
            if (nSeqIndex >= 0)
                oSeq = m_oSeq.get(nSeqIndex);

            // search for a specific climate region sequence when no default
            // seq is found or when there is more than one seq available
//			if (oSeq == null || m_oSeq.size() > 1)
//			{
//				// finding the climate id based the current obs is costly
//				IPlatform platform = platformDao.getPlatform(iSensor.getPlatformId());
//
//				if (platform != null)
//				{
//					oSearchSeq.setClimateId(platform.getClimateId());
//					nSeqIndex =
//						Collections.binarySearch(m_oSeq, oSearchSeq);
//					// the default is used when a climate seq is not found
//					if (nSeqIndex >= 0)
//						oSeq = m_oSeq.get(nSeqIndex);
//				}
//			}
            m_oSeqLock.readUnlock();

            // quality check the obs with the selected sequence, if any
            if (oSeq != null) {
                QChResult oResult = m_oResultLock.readLock();
                oSeq.check(m_nObsTypeId, iSensor, iObs, oResult);
                m_oResultLock.readUnlock();
            }
        }
    }


    /**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link QChSeq} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link QChSeq}
     * @see ILockFactory
     * @see StripeLock
     */
    @Override
    public QChSeq getLock() {
        return new QChSeq();
    }


    /**
     * Compares <i> this </i> sequence manager with the provided sequence
     * manager by observation type.
     * <p/>
     * <p>
     * Required for the implementation of {@link Comparable}.
     * </p>
     *
     * @param oQChSeqMgr manager to compare with <i> this </i>
     * @return 0 if they match by observation type. &lt 0 if <i> this </i>
     * is the less than the provided manager.
     */
    public int compareTo(QChSeqMgr oQChSeqMgr) {
        return (m_nObsTypeId - oQChSeqMgr.m_nObsTypeId);
    }


    /**
     * Provides mutually exclusive access of results.
     * <p/>
     * <p>
     * Implements {@code ILockFactory<QChResult>} to provide a means of
     * accessing results in a mutually exclusive manner, through the use
     * of {@link StripeLock}.
     * </p>
     */
    private class ResultFactory implements ILockFactory<QChResult> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code ResultFactory}
         * </p>
         */
        ResultFactory() {
        }


        /**
         * Required for the implementation of the interface class
         * {@code ILockFactory}.
         * <p>
         * This is used to add a container of lockable {@link QChResult} objects
         * to the {@link StripeLock} Mutex.
         * </p>
         *
         * @return A new instance of {@link QChResult}
         * @see ILockFactory
         * @see StripeLock
         */
        @Override
        public QChResult getLock() {
            return new QChResult();
        }
    }
}
