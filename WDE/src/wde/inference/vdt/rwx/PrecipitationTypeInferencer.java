package wde.inference.vdt.rwx;

import wde.inference.InferenceResult;
import wde.inference.vdt.AbstractVdtInferencer;
import wde.inference.vdt.ValueHolder;
import wde.inference.vdt.VdtObservationResolver;

public class PrecipitationTypeInferencer extends AbstractVdtInferencer {

    @Override
    public InferenceResult doInference(VdtObservationResolver resolver) {
        ValueHolder<Float> confidenceHolder = new ValueHolder<>(0f);

        RoadSegmentAssessments rwx = new RoadSegmentAssessments(resolver);

        int precipTypeCode = rwx.precip_type_assessment(confidenceHolder);

        PrecipitationTypeInferenceResult result = (PrecipitationTypeInferenceResult) RwxInferenceResultFactory.newPrecipitationTypeInferenceResult();
        PrecipitationType precipitationType = PrecipitationType.fromCode(precipTypeCode);
        result.setPrecipitationType(precipitationType);
        result.setConfidence(confidenceHolder.getValue());

        return result;
    }

}
