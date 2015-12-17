package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;

public class PrecipitationIntensityInferenceResult extends InferenceResult<PrecipitationIntensityInferencer> {

    private PrecipitationIntensity precipitationIntensity;
    private float confidence;

    public PrecipitationIntensity getPrecipitationIntensity() {
        return precipitationIntensity;
    }

    public void setPrecipitationIntensity(PrecipitationIntensity precipitationIntensity) {
        this.precipitationIntensity = precipitationIntensity;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public float getConfidence() {
        return confidence;
    }
}
