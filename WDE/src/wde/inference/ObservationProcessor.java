package wde.inference;

import org.apache.log4j.Logger;
import wde.dao.ObsTypeDao;
import wde.obs.IObs;
import wde.obs.IObsSet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ObservationProcessor {

    private final Logger logger = Logger.getLogger(this.getClass());
    private final HashMap<Class, InferencerProcessorMapping> inferencerProcessorMapSet = new HashMap<>();

    private IObsSet currentObsSet;
    private Object processLock = new Object();

    private ObsTypeDao obsTypeDao;

    public ObservationProcessor() {
        this.obsTypeDao = ObsTypeDao.getInstance();

        init();
    }

    protected abstract void init();

    public Logger getLogger() {
        return this.logger;
    }

    public synchronized void process(IObsSet obsSet) {
        if (obsSet == null) {
            throw new NullPointerException("obsSet");
        }

        synchronized (processLock) {
            this.currentObsSet = obsSet;

            try {

                for (int i = 0; i < obsSet.size(); ++i) {
                    IObs obs = obsSet.get(i);
                    if (obs == null) {
                        throw new NullPointerException("obs");
                    }

                    process(obs);

                }
            } catch (Exception e) {
                getLogger().error(e.toString());
            } finally {
                this.currentObsSet = null;
            }
        }
    }

    protected abstract void process(IObs obs);

    public InferencerProcessorMapping[] getInferenceProcessorMaps() {
        return (InferencerProcessorMapping[]) this.inferencerProcessorMapSet.values().toArray();
    }

    protected ObsTypeDao getObsTypeDao() {
        return this.obsTypeDao;
    }

    protected int lookupObsTypeId(String name) {
        return this.getObsTypeDao().getObsTypeId(name);
    }

    protected IObsSet getCurrentObsSet() {
        return this.currentObsSet;
    }

    protected Map<Integer, Set<IObs>> buildObsSetIdIndex(IObsSet obsSet) {
        if (obsSet == null) {
            throw new NullPointerException("obsSet");
        }

        HashMap<Integer, Set<IObs>> obsTypeIdIndex = new HashMap<>();
        for (int i = 0; i < obsSet.size(); ++i) {
            IObs obs = obsSet.get(i);
            if (obs == null) {
                getLogger().debug("Null entries within an IObsset should not be passed to " + this.getClass().getName());
                continue;
            }

            Set<IObs> obsTypeGroup = obsTypeIdIndex.get(obs.getObsTypeId());
            if (obsTypeGroup == null) {
                obsTypeGroup = new HashSet<>();
                obsTypeIdIndex.put(obs.getObsTypeId(), obsTypeGroup);
            }

            obsTypeGroup.add(obs);
        }

        return obsTypeIdIndex;
    }

    public void addInferencer(Inferencer inferencer, InferenceResultProcessor processor) {
        if (inferencer == null)
            throw new NullPointerException("inferencer");

        synchronized (inferencerProcessorMapSet) {

            if (this.inferencerProcessorMapSet.containsKey(inferencer)) {
                getLogger().debug(String.format("Inferencer has already been registered: %s(%s)",
                        inferencer.getName(),
                        inferencer.getClass()));
            }

            if (!this.inferencerProcessorMapSet.containsKey(inferencer.getClass())) {
                this.inferencerProcessorMapSet.put(inferencer.getClass(),
                        new InferencerProcessorMapping(inferencer, processor));
            }
        }
    }

    public void addInferencers(InferencerProcessorMapping[] inferencerProcessorMappings) throws Exception {
        if (inferencerProcessorMappings.length == 0)
            throw new Exception("inferencerMapSet");

        synchronized (inferencerProcessorMapSet) {
            for (InferencerProcessorMapping inferencerProcessorMapping : inferencerProcessorMappings) {
                if (inferencerProcessorMapSet.containsKey(inferencerProcessorMapping.getInferencer().getClass())) {
                    getLogger().debug(String.format("Inferencer has already been registered: %s(%s)",
                            inferencerProcessorMapping.getInferencer().getName(),
                            inferencerProcessorMapping.getInferencer().getClass()));
                    continue;
                }

                this.inferencerProcessorMapSet.put(inferencerProcessorMapping.getInferencer().getClass(), inferencerProcessorMapping);
            }
        }
    }
}
