package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;

public class VisibilityInferenceResult extends InferenceResult<VisibilityInferencer> {

    private float confidence;
    private Visibility visibility;

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getConfidence() {
        return this.confidence;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    public Visibility getVisibility() {
        return visibility;
    }
}
