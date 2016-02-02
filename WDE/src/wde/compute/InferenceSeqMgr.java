package wde.compute;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.Connection;
import java.util.ArrayList;

public class InferenceSeqMgr extends AsyncQ<IObsSet>
        implements Comparable<InferenceSeqMgr>, ILockFactory<InferenceSeq> {

    static Logger logger = Logger.getLogger(InferenceSeqMgr.class);

    /**
     * Observation type being quality checked by this manager.
     */
    private int m_nObsTypeId;

    /**
     * List of quality doInference sequences ordered by obstypeid.
     */
    private ArrayList<InferenceSeq> m_oSeq;
    /**
     * Provides mutually exclusive access to quality checking sequences.
     */
    private StripeLock<InferenceSeq> m_oSeqLock;
    /**
     * Provides mutually exclusive access to qualtity doInference results.
     */
    private StripeLock<InferenceResult> m_oResultLock;
    /**
     * Pointer to the sensors cache singleton instance.
     */
    private final SensorDao sensorDao;
    /**
     * Pointer to the platforms cache singleton instance.
     */
    private final PlatformDao platformDao;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code QChSeqMgr}
     * </p>
     */
    InferenceSeqMgr() {
        sensorDao = SensorDao.getInstance();
        platformDao = PlatformDao.getInstance();
    }


    /**
     * Initializes new instances of {@code InferenceSeqMgr} with the provided values.
     * Queries the database to populate the sorted list of quality doInference
     * sequences, setting their climate-region id..
     *
     * @param nObsTypeId  observation type id for this manager, used for the
     *                    database query.
     * @param nMaxThreads max threads to allocate to processing the quality
     *                    doInference sequence and result {@link StripeLock} containers.
     * @param connection  connection to the datasource, ready for queries prior
     *                    to this method call.
     */
    InferenceSeqMgr(int nObsTypeId, int nMaxThreads, Connection connection) {
        setObsTypeId(nObsTypeId);
        setMaxThreads(nMaxThreads);

        m_oSeq = new ArrayList<InferenceSeq>();
        m_oSeqLock = new StripeLock<InferenceSeq>(this, nMaxThreads);
        m_oResultLock =
                new StripeLock<InferenceResult>(new ResultFactory(), nMaxThreads);
        sensorDao = SensorDao.getInstance();
        platformDao = PlatformDao.getInstance();
    }

    /**
     * <b> Mutator </b>
     *
     * @param nObsTypeId observation type id value for this sequence manager.
     */
    void setObsTypeId(int nObsTypeId) {
        m_nObsTypeId = nObsTypeId;
    }

    void addInferenceSeq(InferenceSeq seq) {
        m_oSeqLock.writeLock();
        m_oSeq.add(seq);
        m_oSeqLock.writeUnlock();
    }

    @Override
    public void run(final IObsSet obsSet) {
        try {
            if (obsSet == null) {
                throw new NullPointerException("obsSet");
            }
        } catch (Exception e) {
            logger.error(e);
            return;
        }

        m_oSeqLock.readLock();
        for (int i = 0; i < m_oSeq.size(); ++i) {

            final InferenceSeq sequence = m_oSeq.get(i);
            if (sequence == null) {
                logger.debug("A null was encountered in the sequence list.");
                continue;
            }

            final char[] platformFilter = sequence.getPlatformFilter();

            //
            // Ensure the sequence is only run if it can handle the current observation type.
            //
            if (sequence.getObsTypeId() != obsSet.getObsType()) {
                logger.trace("Moving to the next sequence. The current obstypeid doesn't match.");
                continue;
            }

            logger.debug("ObsSet obstypeid=" + obsSet.getObsType() + " size=" + obsSet.size());
            for (final IObs obs : obsSet) {
                if (obs == null) {
                    logger.debug("A null observation was encountered in provided set.");
                    continue;
                }

                logger.debug("Observation: {" + obs.toString() + "}");
                final ISensor sensor = sensorDao.getSensor(obs.getSensorId());
                if (sensor == null || sensor.getDistGroup() == 1) {
                    logger.debug("Skipped observation because sensor was either null or had a distgroup of '1'. obstypeid=" + obs.getObsTypeId());
                    continue;
                }

                final IPlatform platform = platformDao.getPlatform(sensor.getPlatformId());
                if (platform == null) {
                    logger.debug("Skipped observation because platform was not found.");
                    continue;
                }

                boolean isFiltered = false;
                for(char platformFilterCode : platformFilter) {
                    if (platformFilterCode == platform.getCategory()) {
                        isFiltered = true;
                        break;
                    }
                }

                if (isFiltered) {
                    logger.trace("Skipped observation because sequence does not support platform.");
                    continue;
                }

                final InferenceResult result = (InferenceResult) sequence.doInference(m_nObsTypeId, sensor, obs);
                if (result == null) {
                    logger.debug("Skipping inference result because null was received from sequence.");
                    continue;
                }

                if (!result.isCanceled()) {
                    final int size = result.getObservations().size();

                    if (result.getObservations().size() > 0) {
                        logger.debug("The inference algorithm created " + size + " observations.");
                    }
                }
            }
        }

        WDEMgr.getInstance().queue(obsSet);
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
