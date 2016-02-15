package wde.compute;

import wde.WDEMgr;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.obs.ObsSet;
import wde.obs.Observation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Provides a means of sequencing inference algorithms, and distinguishing these sequences by observation types.
 *
 */
public class InferenceSeq extends Inference<InferenceSeq> {

    private int[] m_obsTypeIds;
    private char[] m_platformFilter;

    /**
     * Container of inference algorithms, sorted by sequence id.
     */
    private final TreeSet<Inference> m_oInferences = new TreeSet<Inference>();

    InferenceSeq() {
        super(null, 0);
    }

    InferenceSeq(int[] obsTypeIds, char[] platformFilter) {
        super(null, 0);

        //
        // Sort the arrays for later when using a binary search against the array to find an
        // obstype or filter element.
        //
        Arrays.sort(obsTypeIds);
        Arrays.sort(platformFilter);

        this.m_obsTypeIds = obsTypeIds;
        this.m_platformFilter = platformFilter;
    }

    public InferenceSeq(int[] obsTypeIds, char[] platformFilter, Class<Inference>[] inferences) {
        this(obsTypeIds, platformFilter);

        for(int i = 0; i < inferences.length; ++i) {
            try {
                Class<Inference> clazz = inferences[i];
                Inference inference = clazz.newInstance();
                inference.init(this, i);

                m_oInferences.add(inference);
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
    public int[] getObsTypeIds() {
        return this.m_obsTypeIds;
    }

    protected void init(int[] obsTypeIds, char[] platformFilter) {
        m_obsTypeIds = obsTypeIds;
        m_platformFilter = platformFilter;
    }

    public Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs) {
        boolean bCanceled = false;

        Set<InferenceResult> aggregateResults = newInferenceResultSet();

        if (m_oInferences != null && m_oInferences.size() > 0) {
            ObsSet inferredObsSet = getObsMgr().getObsSet(obsTypeId);
            for (Inference inference : m_oInferences) {
                if (inference == null)
                    continue;
                try {
                    Set<InferenceResult> results = inference.doInference(obsTypeId, sensor, obs);
                    processResults(results);

                    aggregateResults.addAll(results);
                } catch (Exception e) {
                    getLogger().error("An exception was encountered while executing/processing inference.", e);
                }
            }
        }

        return aggregateResults;
    }

    protected void processResults(Set<InferenceResult> results) throws Exception {

        for(InferenceResult result : results) {
            if (result == null || result.isCanceled()) {
                continue;
            }

            Set<IObs> observations = result.getObservations();
            if (observations != null || observations.size() == 0) {
                final Collection<IObsSet> obsSets = buildObsSets(observations);
                for(IObsSet obsSet : obsSets) {
                    if (obsSet == null) {
                        continue;
                    }

                    for(IObs obs : obsSet) {
                        getLogger().debug("Created Infered Observation: obstypeid={}, value={}, confidence={}",
                                obs.getObsTypeId(),
                                obs.getValue(),
                                obs.getConfValue());
                    }

                    WDEMgr.getInstance().queue(obsSet);
                }
            }
        }
    }

    protected Collection<IObsSet> buildObsSets(Set<IObs> observationList) throws Exception {
        if (observationList == null || observationList.size() == 0) {
            getLogger().debug("Encountered a null observation within the observation list.");
            return new ArrayList<>();
        }

        Map<Integer, IObsSet> obsSetMap = new HashMap<>();
        for (IObs obs : observationList) {
            final int obsTypeId = obs.getObsTypeId();

            //
            // Retrieve an existing IObsSet or create an existing if one doesn't already exist
            // for the specific obsTypeId.
            //
            IObsSet obsSet = ObsMgr.getInstance().getObsSet(obsTypeId);
            if (!obsSetMap.containsKey(obs.getObsTypeId())) {
                //obsSet = new ObsSet(obsTypeId, ObsSet.SERIAL);
                obsSetMap.put(obsTypeId, obsSet);
            } else {
                obsSet = obsSetMap.get(obs.getObsTypeId());
            }

            //debug purposes
            if (obsSet == null) {
                throw new Exception("obsSet was null.");
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

        return Arrays.hashCode(m_obsTypeIds) - Arrays.hashCode(other.m_obsTypeIds);
    }

    public void testBuildObsSet() throws Exception {
        Set<IObs> obsSet = new HashSet<>();
        for(int i = 0; i < 10; ++i) {
            obsSet.add(new Observation(i, 0, 0, 0, 0, 0, 0, 0, 0.0));
        }

        Collection<IObsSet> obsSets = buildObsSets(obsSet);
        assert(obsSets.size() == 10);
    }

    public static void main(String[] args) {
        InferenceSeq seq = new InferenceSeq();
        try {
            seq.testBuildObsSet();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
