package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;

public class PrecipitationTypeInferenceResult extends InferenceResult<PrecipitationTypeInferencer> {

    private float confidence;
    private PrecipitationType precipType;

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getConfidence() {
        return this.confidence;
    }

    public void setPrecipitationType(PrecipitationType precipType) {
        this.precipType = precipType;
    }

    public PrecipitationType getPrecipitationType() {
        return precipType;
    }
}
