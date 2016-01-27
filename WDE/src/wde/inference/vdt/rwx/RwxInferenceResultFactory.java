package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;

public class RwxInferenceResultFactory {

    public static InferenceResult newPrecipitationTypeInferenceResult() {
        return new PrecipitationTypeInferenceResult();
    }

    public static InferenceResult newVisibilityInferenceResult() {
        return new VisibilityInferenceResult();
    }

    public static InferenceResult newPrecipitationIntensityInferenceResult() {
        return new PrecipitationIntensityInferenceResult();
    }

    public static InferenceResult newPavementConditionInferenceResult() {
        return new PavementConditionInferenceResult();
    }

    public static InferenceResult newPavementSlicknessInferenceResult() {
        return new PavementSlicknessInferenceResult();
    }
}
