package wde.compute;

public class InferenceResult {

    /**
     * Indicates whether the quality check algorithm ran successfully or not.
     * Default false.
     */
    private boolean m_bRun;
    /**
     * Indicates whether the quality check algorithm passed the observation
     * or not. Default false.
     */
    private boolean m_bPass;
    /**
     * Confidence value returned by the quality check. Default 0.0.
     */
    private double m_dConfidence;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code QChResult}.
     * </p>
     */
    public InferenceResult() {
    }


    /**
     * Sets attributes to their default values.
     */
    public void clear() {
        m_bRun = false;
        m_bPass = false;
        m_dConfidence = 0.0;
    }

    /**
     * <b> Accessor </b>
     *
     * @return run attribute value.
     */
    public boolean getRun() {
        return m_bRun;
    }

    /**
     * <b> Accessor </b>
     *
     * @return pass attribute value.
     */
    public boolean getPass() {
        return m_bPass;
    }

    /**
     * <b> Mutator </b>
     *
     * @param bPass sets the pass attribute to the provided value.
     */
    void setPass(boolean bPass) {
        m_bPass = bPass;
    }

    /**
     * <b> Accessor </b>
     *
     * @return confidence attribute value.
     */
    public double getConfidence() {
        return m_dConfidence;
    }

    /**
     * <b> Mutator </b>
     *
     * @param dConfidence sets the confidence attribute to the provided value.
     */
    void setConfidence(double dConfidence) {
        m_dConfidence = dConfidence;
    }

    /**
     * <b> Mutator </b>
     * Sets run to true.
     */
    void setRun() {
        m_bRun = true;
    }
}
