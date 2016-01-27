package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;
import wde.inference.vdt.AbstractVdtInferencer;
import wde.inference.vdt.ValueHolder;
import wde.inference.vdt.VdtObservationResolver;
import wde.inference.vdt.VdtObservationTypes;
import wde.obs.IObs;

public class PrecipitationIntensityInferencer extends AbstractVdtInferencer {

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

        int precipIntensityCode = rwx.precip_intensity_assessment(precipTypeValue, confidenceHolder);

        PrecipitationIntensityInferenceResult result = (PrecipitationIntensityInferenceResult) RwxInferenceResultFactory.newPrecipitationIntensityInferenceResult();
        result.setPrecipitationIntensity(PrecipitationIntensity.fromCode(precipIntensityCode));
        result.setConfidence(confidenceHolder.getValue());

        return result;
    }
}
