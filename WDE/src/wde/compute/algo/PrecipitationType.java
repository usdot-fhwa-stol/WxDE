package wde.compute.algo;

import wde.compute.Inference;
import wde.compute.InferenceResult;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.Observation;

import java.util.Set;

public class PrecipitationType extends Inference<PrecipitationType> {

    // Precip Type enumeration
    public static final int NO_PRECIP = 0;
    public static final int RAIN = 1;
    public static final int MIX = 2;
    public static final int SNOW = 3;

    public final float air_temp_wgt = 0.50f;

    public PrecipitationType() {
        
    }

    @Override
    public synchronized Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs) {

        Set<InferenceResult> resultSet = newInferenceResultSet();
        InferenceResult result = new InferenceResult();
        resultSet.add(result);

        double mobile_air_temp = obs.getValue();
        int precip_type = NO_PRECIP;
        float confidence = 0f;
        float air_temp_input_conf = 1.0f;

        if (mobile_air_temp > 2)
            precip_type = RAIN;
        else if (mobile_air_temp < -2)
            precip_type = SNOW;
        else
            precip_type = MIX;

        IObs pseudoObs = new Observation(
                1000000,
                0,
                0,
                obs.getObsTimeLong(),
                obs.getRecvTimeLong(),
                obs.getLatitude(),
                obs.getLongitude(),
                obs.getElevation(),
                precip_type);

        confidence = confidence + (air_temp_input_conf * air_temp_wgt);

        result.setConfidence(confidence);
        result.addObservation(pseudoObs);

        return resultSet;
    }

    @Override
    public int compareTo(PrecipitationType other) {
        if (this == other)
            return 0;

        return super.compareTo(other);
    }

    protected static void main(String[] args) {
        PrecipitationType precipType1 = new PrecipitationType();
        PrecipitationType precipType2 = new PrecipitationType();

        //precipType1.doInference(0, )

    }
}
