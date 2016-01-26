package wde.compute;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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
    private SensorDao sensorDao;
    /**
     * Pointer to the platforms cache singleton instance.
     */
    private PlatformDao platformDao;
    private Connection connection;


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
     * Queries the database to populate the sorted list of quality doInference
     * sequences, setting their climate-region id..
     *
     * @param nObsTypeId  observation type id for this manager, used for the
     *                    database query.
     * @param nMaxThreads max threads to allocate to processing the quality
     *                    doInference sequence and result {@link StripeLock} containers.
     * @param connection connection to the datasource, ready for queries prior
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

    /**
     * Quality checks the observations in the set that haven't been checked, and
     * that have sensors with distribution group 2.
     * <p>
     * Uses the default-region quality doInference sequence if the sequence isn't
     * available for the given observations climate-region.
     * </p>
     * <p>
     * Overrides {@code AsyncQ} default run method.
     * </p>
     *
     * @param iObsSet set of observations to quality doInference.
     */
    @Override
    public void run(IObsSet iObsSet) {
        // an appropriate sequence must be found for each obs in the set
        for (int nObsIndex = 0; nObsIndex < iObsSet.size(); nObsIndex++) {
            IObs iObs = iObsSet.get(nObsIndex);

            // only sensors with distgroup 0 or 2 are checked
            ISensor iSensor = sensorDao.getSensor(iObs.getSensorId());
            if (iSensor == null || iSensor.getDistGroup() == 1)
                continue;

            InferenceSeq oSeq = null;
            // always search for the default climate id sequence
            m_oSeqLock.readLock();
            for(int i = 0; i < m_oSeq.size(); ++i) {
                oSeq = m_oSeq.get(i);

                // quality doInference the obs with the selected sequence, if any
                if (oSeq != null) {
                    if (oSeq.getObsTypeId() != iObs.getObsTypeId())
                        continue;

                    final IPlatform platform = platformDao.getPlatform(iSensor.getPlatformId());

                    boolean allowed = false;
                    for(char c : oSeq.getPlatformFilter()) {
                        if (c == platform.getCategory()) {
                            allowed = true;
                        }
                    }

                    if (!allowed)
                        continue;

                    m_oResultLock.readLock();
                    InferenceResult inferData = (InferenceResult) oSeq.doInference(m_nObsTypeId, iSensor, iObs);
                    if (inferData == null){
                        continue;
                    }

                    if (inferData.ran() && !inferData.isCanceled()) {
                        int size = inferData.getObservations().size();

                        if (inferData.getObservations().size() > 0) {
                            logger.debug("The inference algorthms created " + size + " observations.");

                            for(IObsSet obsSet : buildObsSet(inferData.getObservations())) {
                                WDEMgr.getInstance().queue(obsSet);
                            }
                        }
                    }

                    m_oResultLock.readUnlock();
                }
            }
            m_oSeqLock.readUnlock();
        }
    }

    protected Collection<IObsSet> buildObsSet(Set<IObs> observationList) {
        Map<Integer, IObsSet> obsSetMap = new HashMap<>();
        for(IObs obs : observationList) {
            int obsTypeId = obs.getObsTypeId();

            IObsSet obsSet = null;
            if (!obsSetMap.containsKey(obs.getObsTypeId())) {
                obsSet = obsSetMap.put(obsTypeId, ObsMgr.getInstance().getObsSet(obsTypeId));
            } else {
                obsSet = obsSetMap.get(obs.getObsTypeId());
            }
        }

        return obsSetMap.values();
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

    public void setConnection(Connection connection) {
        this.connection = connection;
    }

    public Connection getConnection() {
        return connection;
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
