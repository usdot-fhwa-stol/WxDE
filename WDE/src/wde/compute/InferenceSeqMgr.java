package wde.compute;

import org.apache.log4j.Logger;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

public class InferenceSeqMgr extends AsyncQ<IObsSet>
        implements Comparable<InferenceSeqMgr>, ILockFactory<InferenceSeq> {

    static Logger logger = Logger.getLogger(InferenceSeqMgr.class);

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
    private ArrayList<InferenceSeq> m_oSeq;
    /**
     * Provides mutually exclusive access to quality checking sequences.
     */
    private StripeLock<InferenceSeq> m_oSeqLock;
    /**
     * Provides mutually exclusive access to qualtity check results.
     */
    private StripeLock<InferenceResult> m_oResultLock;
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
    InferenceSeqMgr() {
    }


    /**
     * Initializes new instances of {@code InferenceSeqMgr} with the provided values.
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
    InferenceSeqMgr(int nObsTypeId, int nMaxThreads, Connection iConnection) {
        setObsTypeId(nObsTypeId);
        setMaxThreads(nMaxThreads);

        m_oSeq = new ArrayList<InferenceSeq>();
        m_oSeqLock = new StripeLock<InferenceSeq>(this, nMaxThreads);
        m_oResultLock =
                new StripeLock<InferenceResult>(new ResultFactory(), nMaxThreads);
        sensorDao = SensorDao.getInstance();
        platformDao = PlatformDao.getInstance();

        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            ps = iConnection.prepareStatement(QCHSEQMGR_QUERY);
            ps.setInt(1, nObsTypeId);

            rs = ps.executeQuery();
            while (rs.next())
                m_oSeq.add(new InferenceSeq(rs.getInt(1),
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

            InferenceSeq oSeq = null;
            // always search for the default climate id sequence
            InferenceSeq oSearchSeq = m_oSeqLock.readLock();
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
                InferenceResult oResult = m_oResultLock.readLock();
                oSeq.check(m_nObsTypeId, iSensor, iObs, oResult);
                m_oResultLock.readUnlock();
            }
        }
    }


    /**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link InferenceSeq} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link InferenceSeq}
     * @see ILockFactory
     * @see StripeLock
     */
    @Override
    public InferenceSeq getLock() {
        return new InferenceSeq();
    }


    /**
     * Compares <i> this </i> sequence manager with the provided sequence
     * manager by observation type.
     * <p/>
     * <p>
     * Required for the implementation of {@link Comparable}.
     * </p>
     *
     * @param oInferenceSeqMgr manager to compare with <i> this </i>
     * @return 0 if they match by observation type. &lt 0 if <i> this </i>
     * is the less than the provided manager.
     */
    public int compareTo(InferenceSeqMgr oInferenceSeqMgr) {
        return (m_nObsTypeId - oInferenceSeqMgr.m_nObsTypeId);
    }


    /**
     * Provides mutually exclusive access of results.
     * <p/>
     * <p>
     * Implements {@code ILockFactory<InferenceResult>} to provide a means of
     * accessing results in a mutually exclusive manner, through the use
     * of {@link StripeLock}.
     * </p>
     */
    private class ResultFactory implements ILockFactory<InferenceResult> {
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
         * This is used to add a container of lockable {@link InferenceResult} objects
         * to the {@link StripeLock} Mutex.
         * </p>
         *
         * @return A new instance of {@link InferenceResult}
         * @see ILockFactory
         * @see StripeLock
         */
        @Override
        public InferenceResult getLock() {
            return new InferenceResult();
        }
    }
}
