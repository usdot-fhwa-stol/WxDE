package wde.inference.vdt.rwx;

public class RwxInferencerFactory {

    public static PrecipitationTypeInferencer newPrecipitationTypeInferencer() {
        return new PrecipitationTypeInferencer();
    }

    public static PrecipitationIntensityInferencer newPrecipitationIntensityInferencer() {
        return new PrecipitationIntensityInferencer();
    }

    public static PavementConditionInferencer newPavementConditionInferencer() {
        return new PavementConditionInferencer();
    }

    public static PavementSlicknessInferencer newPavementSlicknessInferencer() {
        return new PavementSlicknessInferencer();
    }

    public static VisibilityInferencer newVisibilityInferencer() {
        return new VisibilityInferencer();
    }
}
