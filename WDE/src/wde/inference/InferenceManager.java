package wde.inference;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.inference.vdt.VdtObservationProcessor;
import wde.obs.IObsSet;
import wde.util.threads.AsyncQ;

public class InferenceManager extends AsyncQ<IObsSet> {

    private static InferenceManager inferenceManager;

    private final Logger logger = Logger.getLogger(this.getClass());

    private WDEMgr wdeMgr = WDEMgr.getInstance();

    public InferenceManager() {
        super();

        init();
    }

    private void init() {
        getWdeMgr().register(this.getClass().getName(), this);
    }

    @Override
    public void run(IObsSet obsSet) {
        ObservationProcessor processor = new VdtObservationProcessor();
        processor.process(obsSet);

        getWdeMgr().queue(obsSet);
    }

    @Override
    public void queue(IObsSet oT) {
        super.queue(oT);
    }

    private WDEMgr getWdeMgr() {
        return WDEMgr.getInstance();
    }

    public static InferenceManager getInstance() {
        synchronized (inferenceManager) {
            if (inferenceManager == null) {
                inferenceManager = new InferenceManager();
            }
        }

        return inferenceManager;
    }
}
