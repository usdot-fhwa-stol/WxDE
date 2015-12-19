package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;

public class PavementConditionInferenceResult extends InferenceResult<PavementConditionInferencer> {

    private float confidence;
    private float precipType;
    private PavementCondition pavementCondition;

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getConfidence() {
        return this.confidence;
    }

    public void setPavementCondition(PavementCondition pavementCondition) {
        this.pavementCondition = pavementCondition;
    }

    public PavementCondition getPavementCondition() {
        return pavementCondition;
    }
}
