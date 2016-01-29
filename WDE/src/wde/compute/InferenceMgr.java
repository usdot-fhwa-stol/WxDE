package wde.compute;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.compute.algo.PavementCondition;
import wde.compute.algo.PavementSlickness;
import wde.compute.algo.PrecipitationIntensity;
import wde.compute.algo.PrecipitationType;
import wde.compute.algo.Visibility;
import wde.obs.IObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InferenceMgr extends AsyncQ<IObsSet> implements ILockFactory<InferenceSeqMgr> {

    private static final Logger logger = Logger.getLogger(InferenceMgr.class);

    /*
     * TODO:
     *
     * This is in place until another means of location extensions is in place. This would normally
     * be something I would leave to a container to resolve and build, but one isn't being used.
     */
    private InferenceSeq[] inferenceSeqs = new InferenceSeq[]{
            new InferenceSeq(
                    2001018,
                    new char[]{'M'},
                    new Class[]{
                            PrecipitationType.class,
                            PrecipitationIntensity.class,
                            PavementSlickness.class,
                            PavementCondition.class,
                            Visibility.class
                    }
            )
    };

    /**
     * Sets the max number of threads for processing <i> this </i> as well as
     * and the sequence managers.
     */
    private static int MAX_THREADS = 5;
    /**
     * Pointer to the singleton instance of {@code QChSMgr}.
     */
    private static final InferenceMgr g_oInstance = new InferenceMgr();

    /**
     * List of sequence managers ordered by the observation-type they manage.
     */
    private Map<Integer, InferenceSeqMgr> m_seqMgrMap = new HashMap<>();
    /**
     * Lock container for quality doInference sequence managers.
     */
    private StripeLock<InferenceSeqMgr> m_oLock;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Configures the quality checking service manager, as well as its contained
     * sequence managers. Creates the connection to the configured datasource.
     * Populates the sorted sequence managers list to manage the configured
     * default observation types. Registers <i> this </i> manager with the
     * clarus manager for processing.
     * </p>
     */
    private InferenceMgr() {
        logger.info("Calling constructor");

        try {

            // apply QChSMgr configuration
            ConfigSvc oConfigSvc = ConfigSvc.getInstance();
            Config oConfig = oConfigSvc.getConfig(this);

            // increase the queue depth for more thread concurrency
            MAX_THREADS = oConfig.getInt("queuedepth", MAX_THREADS);

            setMaxThreads(MAX_THREADS);
            m_oLock = new StripeLock<>(this, MAX_THREADS);

            Connection iConnection = null;
            WDEMgr wdeMgr = WDEMgr.getInstance();

            //DataSource iDataSource = wdeMgr.getDataSource(oConfig.getString("datasource", null));

            for(InferenceSeq seq : resolveSequences()) {
                InferenceSeqMgr seqMgr = null;
                if (m_seqMgrMap.containsKey(seq.getObsTypeId())) {
                    seqMgr = m_seqMgrMap.get(seq.getObsTypeId());
                } else {
                    seqMgr = new InferenceSeqMgr(
                            seq.getObsTypeId(),
                            MAX_THREADS,
                            null
                    );

                    m_oLock.writeLock();
                    m_seqMgrMap.put(seq.getObsTypeId(), seqMgr);
                    m_oLock.writeUnlock();
                }
                seqMgr.addInferenceSeq(seq);
                m_oLock.readUnlock();
            }

            //iConnection.close();
            wdeMgr.register(getClass().getName(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Completing constructor");
    }

    /**
     * <b> Accessor </b>
     *
     * @return the singleton instance of {@code InferenceMgr}.
     */
    public static InferenceMgr getInstance() {
        return g_oInstance;
    }

    @Override
    public void run(IObsSet iObsSet) {
        // find a seq mgr to handle the obs set
        InferenceSeqMgr oSeqMgr = null;
        m_oLock.readLock();
        if (m_seqMgrMap.containsKey(iObsSet.getObsType())) {
            oSeqMgr = m_seqMgrMap.get(iObsSet.getObsType());
        }
        m_oLock.readUnlock();

        if (oSeqMgr != null) {
            oSeqMgr.run(iObsSet);
        }

        // queue obs set for next process
        WDEMgr.getInstance().queue(iObsSet);
    }

    protected Set<InferenceSeq> resolveSequences() {
        return new HashSet<>(Arrays.asList(inferenceSeqs));
    }


    /**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link InferenceSeqMgr} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link InferenceSeqMgr}
     * @see wde.util.threads.ILockFactory
     * @see StripeLock
     */
    public InferenceSeqMgr getLock() {
        return new InferenceSeqMgr();
    }

    public static void main(String[] args) {

    }
}
