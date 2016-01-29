package wde.compute;

import wde.WDEMgr;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.obs.ObsSet;
import wde.obs.Observation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides a means of sequencing inference algorithms, and distinguishing
 * these sequences by climate region.
 * <p>
 * Implements {@code Comparable<QChSeq>} to enforce an ordering based on
 * climate id.
 * </p>
 */
public class InferenceSeq extends Inference<InferenceSeq> {

    private int m_obsTypeId;
    private char[] m_platformFilter;

    /**
     * Container of inference algorithms, sorted by sequence id.
     */
    private final TreeSet<Inference> m_oInferences = new TreeSet<Inference>();

    InferenceSeq() {
        super(null, 0);
    }

    InferenceSeq(int obsTypeId, char[] platformFilter) {
        super(null, 0);

        this.m_obsTypeId = obsTypeId;
        this.m_platformFilter = platformFilter;
    }

    public InferenceSeq(int obsTypeId, char[] platformFilter, Class<Inference>[] inferences) {
        this(obsTypeId, platformFilter);

        for(int i = 0; i < inferences.length; ++i) {
            try {
                Class<Inference> clazz = inferences[i];
                Inference inference = clazz.newInstance();
                inference.init(this, i);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void addInference(Inference inference) {
        m_oInferences.add(inference);
    }

    public void addInferences(Collection<Inference> inferences) {
        m_oInferences.addAll(inferences);
    }

    public char[] getPlatformFilter() {
        return m_platformFilter;
    }

    @Override
    public int getObsTypeId() {
        return this.m_obsTypeId;
    }

    protected void init(int obsTypeId, char[] platformFilter) {
        m_obsTypeId = obsTypeId;
        m_platformFilter = platformFilter;
    }

    public Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs) {
        boolean bCanceled = false;

        Set<InferenceResult> aggregateResults = newInferenceResultSet();

        if (m_oInferences != null && m_oInferences.size() > 0) {

            ObsSet inferredObsSet = getObsMgr().getObsSet(obsTypeId);
            for (Inference inference : m_oInferences) {
                Set<InferenceResult> results = inference.doInference(obsTypeId, sensor, obs);

                for (InferenceResult result : results) {
                    if (result.isCanceled()) {
                        bCanceled = true;
                        break;
                    }
                }
            }

        }

        return aggregateResults;
    }

    protected void processResults(Set<InferenceResult> results) {

        for(InferenceResult result : results) {
            Set<IObs> observations = result.getObservations();
            if (observations != null || observations.size() == 0) {
                Collection<IObsSet> obsSets = buildObsSets(observations);
                for(IObsSet obsSet : obsSets) {
                    if (obsSet == null) {
                        continue;
                    }

                    WDEMgr.getInstance().queue(obsSet);
                }
            }
        }
    }

    protected Collection<IObsSet> buildObsSets(Set<IObs> observationList) {
        if (observationList == null) {
            return new ArrayList<>();
        }

        Map<Integer, IObsSet> obsSetMap = new HashMap<>();
        for (IObs obs : observationList) {
            final int obsTypeId = obs.getObsTypeId();

            //
            // Retrieve an existing IObsSet or create an existing if one doesn't already exist
            // for the specific obsTypeId.
            //
            IObsSet obsSet;
            if (!obsSetMap.containsKey(obs.getObsTypeId())) {
                //obsSet = obsSetMap.put(obsTypeId, ObsMgr.getInstance().getObsSet(obsTypeId));
                obsSet = new ObsSet(obsTypeId, ObsSet.SERIAL);
                obsSetMap.put(obsTypeId, obsSet);
            } else {
                obsSet = obsSetMap.get(obs.getObsTypeId());
            }

            obsSet.add(obs);
        }

        Collection<IObsSet> obsSets = obsSetMap.values();
        //
        // Ensure an empty list is returned at a minimum.
        //
        if (obsSets == null) {
            obsSets = new ArrayList<>();
        }

        return obsSets;
    }

    /**
     * Compares <i> this </i> {@code InferenceSeq} with the provided {@code InferenceSeq}
     * by climate id.
     *
     * @param other object to compare with <i> this </i>
     * @return 0 if the values match. > 0 if <i> this </i> is the lesser value.
     */
    @Override
    public int compareTo(InferenceSeq other) {
        if (other == this)
            return 0;

        return m_obsTypeId - other.m_obsTypeId;
    }

    public void testBuildObsSet() {
        Set<IObs> obsSet = new HashSet<>();
        for(int i = 0; i < 10; ++i) {
            obsSet.add(new Observation(i, 0, 0, 0, 0, 0, 0, 0, 0.0));
        }

        Collection<IObsSet> obsSets = buildObsSets(obsSet);
        assert(obsSets.size() == 10);
    }

    public static void main(String[] args) {
        InferenceSeq seq = new InferenceSeq();
        seq.testBuildObsSet();
    }
}
