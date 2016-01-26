package wde.compute;

import wde.obs.IObs;

import java.util.HashSet;
import java.util.Set;

public class InferenceResult {

    /**
     * Indicates whether the quality doInference algorithm ran successfully or not.
     * Default false.
     */
    private boolean m_bCompleted;

    /**
     * Confidence value returned by the quality doInference. Default 0.0.
     */
    private double m_dConfidence;
    private boolean m_bCancel;
    private boolean m_bRan;
    private String m_sName;
    private int m_iObsTypeId;
    private Set<IObs> m_observationSet = new HashSet<IObs>();

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code QChResult}.
     * </p>
     */
    public InferenceResult() {
    }

    public synchronized boolean ran() {
        return m_bRan;
    }

    public synchronized void setRan() {
        m_bRan = true;
    }

    public synchronized double getConfidence() {
        return m_dConfidence;
    }

    public synchronized void setConfidence(double dConfidence) {
        m_dConfidence = dConfidence;
    }

    public synchronized void cancel() { m_bCancel = true; }

    public synchronized boolean isCanceled() { return m_bCancel; }

    public void setName(String name) {
        m_sName = name;
    }

    public String getName() {
        return m_sName;
    }

    public void setObsTypeId(int obsTypeId) {
        m_iObsTypeId = obsTypeId;
    }

    public int getObsTypeId() {
        return m_iObsTypeId;
    }

    public Set<IObs> getObservations() {
        return this.m_observationSet;
    }

    public void addObservation(IObs observation) {
        m_observationSet.add(observation);
    }
}
