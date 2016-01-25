package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;
import wde.inference.vdt.AbstractVdtInferencer;
import wde.inference.vdt.ValueHolder;
import wde.inference.vdt.VdtObservationResolver;
import wde.inference.vdt.VdtObservationTypes;
import wde.obs.IObs;

public class PavementSlicknessInferencer extends AbstractVdtInferencer {

    @Override
    public InferenceResult doInference(VdtObservationResolver resolver) {
        ValueHolder<Float> confidenceHolder = new ValueHolder<>(0f);

        RoadSegmentAssessments rwx = new RoadSegmentAssessments(resolver);

        int precipTypeValue = rwx.NO_PRECIP;
        float precipTypeConf = FILL_VALUE;
        IObs precipTypeObs = resolver.resolve(VdtObservationTypes.wde_precip_type.getWdeObsTypeName());
        if (precipTypeObs != null) {
            precipTypeValue = ((Double) precipTypeObs.getValue()).intValue();
            precipTypeConf = precipTypeObs.getConfValue();
        }

        int precipIntensityValue = rwx.NO_PRECIP;
        float precipIntensityConf = FILL_VALUE;
        IObs precipIntensityObs = resolver.resolve(VdtObservationTypes.wde_precip_intensity.getWdeObsTypeName());
        if (precipIntensityObs != null) {
            precipIntensityValue = ((Double) precipIntensityObs.getValue()).intValue();
            precipTypeConf = precipTypeObs.getConfValue();
        }

        int pavementConditionValue = rwx.DRY_PAVEMENT;
        float pavementConditionConf = FILL_VALUE;
        IObs pavementConditionObs = resolver.resolve(VdtObservationTypes.wde_pavement_condition.getWdeObsTypeName());
        if (pavementConditionObs != null) {
            pavementConditionValue = ((Double) pavementConditionObs.getValue()).intValue();
            pavementConditionConf = pavementConditionObs.getConfValue();
        }

        boolean pavementSlicknessFlag = rwx.pavement_slickness_assessment(precipTypeValue, precipIntensityValue, pavementConditionValue);

        PavementSlicknessInferenceResult result = (PavementSlicknessInferenceResult) RwxInferenceResultFactory.newPavementSlicknessInferenceResult();
        PavementSlickness pavementSlickness = PavementSlickness.fromFlag(pavementSlicknessFlag);
        result.setPavementSlickness(pavementSlickness);
        result.setConfidence(confidenceHolder.getValue());

        return result;
    }
}
