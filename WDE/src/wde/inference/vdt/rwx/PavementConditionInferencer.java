package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;
import wde.inference.vdt.AbstractVdtInferencer;
import wde.inference.vdt.ValueHolder;
import wde.inference.vdt.VdtObservationResolver;
import wde.inference.vdt.VdtObservationTypes;
import wde.obs.IObs;

public class PavementConditionInferencer extends AbstractVdtInferencer {

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

        int pavementConditionCode = rwx.pavement_condition_assessment(precipTypeValue, precipTypeConf, precipIntensityValue,
                precipIntensityConf, confidenceHolder);

        PavementConditionInferenceResult result = (PavementConditionInferenceResult) RwxInferenceResultFactory.newPavementConditionInferenceResult();
        PavementCondition pavementCondition = PavementCondition.fromCode(pavementConditionCode);
        result.setPavementCondition(pavementCondition);
        result.setConfidence(confidenceHolder.getValue());

        return result;
    }
}
