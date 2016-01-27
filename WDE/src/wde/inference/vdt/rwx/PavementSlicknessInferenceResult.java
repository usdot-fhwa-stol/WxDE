package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;

public class PavementSlicknessInferenceResult extends InferenceResult<PavementSlicknessInferencer> {

    private float confidence;
    private float precipType;
    private PavementSlickness pavementSlickness;

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getConfidence() {
        return this.confidence;
    }

    public void setPavementSlickness(PavementSlickness pavementSlickness) {
        this.pavementSlickness = pavementSlickness;
    }

    public PavementSlickness getPavementSlickness() {
        return this.pavementSlickness;
    }
}
