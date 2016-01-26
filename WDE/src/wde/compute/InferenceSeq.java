package wde.compute;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.ObsSet;

import java.util.Collection;
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

    //    protected void init() {
//        Inference[] inferences = new Inference[]{
//                new PrecipitationType(),
//                new PrecipitationIntensity(),
//                new PavementCondition(),
//                new Visibility(),
//                new PavementSlickness()
//        };
//        m_oInferences.addAll(Arrays.asList(inferences));
//    }

    protected void init(int obsTypeId, char[] platformFilter) {
        m_obsTypeId = obsTypeId;
        m_platformFilter = platformFilter;
    }


    /**
     * Runs the inference algorithms for the provided observation. Sets the
     * observations run and pass bit-fields, and the confidence level for the
     * observation for each qch algorithm.
     * @param result
     * @param obsTypeId
     * @param sensor
     * @param obs
     */
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

                aggregateResults.addAll(results);
            }

        }

        return aggregateResults;

//        //
//        // Evaluate each inference algorithm against the observation.
//        //
//        for (int seqIndex = 0; seqIndex < m_oInferences.size(); seqIndex++) {
//            Inference inference = m_oInferences.get(seqIndex);
//            // always set the flag bit to indicate an attempted doInference
//
//            if (bContinue) {
//                // clear the result object and set the not-run weight
//                result.clear();
//
//                inference.check(obsTypeId, sensor, obs, result);
//
//                // evaluate results when the test successfully runs
//                if (result.getRun()) {
//
//                }
//            }
//        }
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
}
