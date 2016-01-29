package wde.compute.algo;

import wde.compute.Inference;
import wde.compute.InferenceResult;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.Observation;

import java.util.Set;

public class PavementCondition extends Inference {

    // Pavement Condition enumeration
    public static final int DRY_PAVEMENT = 0;
    public static final int WET_PAVEMENT = 1;
    public static final int SNOW_COVERED = 2;
    public static final int ICE_COVERED = 3;
    public static final int HYDROPLANE = 4;
    public static final int BLACK_ICE = 5;
    public static final int DRY_WET_PAVEMENT = 6;
    public static final int DRY_FROZEN_PAVEMENT = 7;

    public final float road_temp_wgt = 0.50f;
    public final float precip_type_wgt = 0.20f;
    public final float precip_intensity_wgt = 0.30f;

    @Override
    public Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs) {
        Set<InferenceResult> resultSet = newInferenceResultSet();
        InferenceResult result = new InferenceResult();
        resultSet.add(result);

        result.setName("PavementCondition");
        result.setObsTypeId(obsTypeId);

        int pavement_condition = Integer.MAX_VALUE;
        float confidence = 0f;

        int precip_type = Integer.MAX_VALUE;
        float precip_type_input_conf = 0f;
        IObs precipTypeObs = getRelatedObs(1000000, obs);
        if (precipTypeObs != null) {
            precip_type = ((Double) precipTypeObs.getValue()).intValue();
            precip_type_input_conf = precipTypeObs.getConfValue();
        }

        int precip_intensity = Integer.MAX_VALUE;
        float precip_intensity_input_conf = 0f;
        IObs precipIntensityObs = getRelatedObs(1000001, obs);
        if (precipTypeObs != null) {
            precip_intensity = ((Double) precipIntensityObs.getValue()).intValue();
            precip_intensity_input_conf = precipIntensityObs.getConfValue();
        }

        boolean pavement_slickness = false;
        float pavement_slickness_input_conf = 0f;
        IObs pavementSlicknessObs = getRelatedObs(1000003, obs);
        if (pavementSlicknessObs != null) {
            pavement_slickness = ((Double) pavementSlicknessObs.getValue()).intValue() == 0 ? false : true;
            pavement_slickness_input_conf = pavementSlicknessObs.getConfValue();
        }

        // Get VDT fields
        double road_temp = Double.NaN; //this.resolver.getSurface_temp_mean();
        IObs surfaceTempMeanObs = getRelatedObs(2001152, obs); //segSurfaceTemperatureAvgESS
        if (surfaceTempMeanObs != null)
            road_temp = surfaceTempMeanObs.getValue();

        boolean black_ice_possible = false;
        boolean hydroplane_possible = false;

        // Hardwire the input field confidence for now
        // eventually this will be coming from the seg-stats file
        float road_temp_input_conf = 1.0f;

        // VDT case with road-temp, precip-type and precip-intensity
        if (!Double.isNaN(road_temp) && precip_type != PrecipitationType.NO_PRECIP && precip_intensity != PrecipitationType.NO_PRECIP) {
            if (precip_intensity == PrecipitationType.NO_PRECIP)
                pavement_condition = DRY_PAVEMENT;
            else if (precip_type == PrecipitationType.RAIN) {
                if (road_temp <= 0 && precip_intensity != PrecipitationIntensity.ROAD_SPLASH)
                    pavement_condition = ICE_COVERED;
                else
                    pavement_condition = WET_PAVEMENT;
            } else if (precip_type == PrecipitationType.MIX) {
                if (road_temp <= 0 && precip_intensity != PrecipitationIntensity.ROAD_SPLASH)
                    pavement_condition = ICE_COVERED;
                else
                    pavement_condition = WET_PAVEMENT;
            } else if (precip_type == PrecipitationType.SNOW) {
                if (road_temp > -1 && road_temp < 1 && precip_intensity != PrecipitationIntensity.ROAD_SPLASH)
                    pavement_condition = ICE_COVERED;
                else if (road_temp >= 1 && precip_intensity != PrecipitationIntensity.HEAVY_PRECIP)
                    pavement_condition = WET_PAVEMENT;
                else if (road_temp > 2)
                    pavement_condition = WET_PAVEMENT;
                else
                    pavement_condition = SNOW_COVERED;
            }

            if (pavement_condition == DRY_PAVEMENT && road_temp < 1)
                black_ice_possible = true;
            else if (pavement_condition == WET_PAVEMENT)
                hydroplane_possible = true;

            confidence = confidence + (road_temp_input_conf * road_temp_wgt) +
                    (precip_type_input_conf * precip_type_wgt) +
                    (precip_intensity_input_conf * precip_intensity_wgt);
        } else if (precip_type != Integer.MAX_VALUE && precip_intensity != Integer.MAX_VALUE) // VDT case with just precip-type and precip-intensity
        {
            if (precip_intensity == PrecipitationType.NO_PRECIP)
                pavement_condition = DRY_PAVEMENT;
            else if (precip_type == PrecipitationType.RAIN) {
                pavement_condition = WET_PAVEMENT;
                hydroplane_possible = true;
            } else if (precip_type == PrecipitationType.MIX && precip_intensity != PrecipitationIntensity.ROAD_SPLASH)
                pavement_condition = ICE_COVERED;
            else if (precip_type == PrecipitationType.MIX && precip_intensity == PrecipitationIntensity.ROAD_SPLASH) {
                pavement_condition = WET_PAVEMENT;
                hydroplane_possible = true;
            } else
                pavement_condition = SNOW_COVERED;

            confidence = confidence + (precip_type_input_conf * precip_type_wgt) + (precip_intensity_input_conf * precip_intensity_wgt);
        } else if (!Double.isNaN(road_temp)) // VDT case with just road-temp
        {
            if (road_temp <= 0) {
                pavement_condition = DRY_FROZEN_PAVEMENT;
                black_ice_possible = true;
            } else {
                pavement_condition = DRY_WET_PAVEMENT;
                hydroplane_possible = true;
            }
            confidence = confidence + (road_temp_input_conf * road_temp_wgt);
        } else if (precip_type != Integer.MAX_VALUE) // VDT case with just precip-type
        {
            if (precip_type == PrecipitationType.RAIN) {
                pavement_condition = DRY_WET_PAVEMENT;
                hydroplane_possible = true;
            } else if (precip_type == PrecipitationType.MIX || precip_type == PrecipitationType.SNOW) {
                pavement_condition = DRY_FROZEN_PAVEMENT;
                black_ice_possible = true;
            }
            confidence = confidence + (precip_type_input_conf * precip_type_wgt);
        }

        // Determine if there is black-ice or hydroplane hazard
        if (pavement_slickness) {
            if (black_ice_possible)
                pavement_condition = BLACK_ICE;
            else if (hydroplane_possible)
                pavement_condition = HYDROPLANE;
        }

        result.setConfidence(confidence);

        IObs pseudoObs = new Observation(
                1000002,
                0,
                0,
                obs.getObsTimeLong(),
                obs.getRecvTimeLong(),
                obs.getLatitude(),
                obs.getLongitude(),
                obs.getElevation(),
                pavement_condition
        );
        result.addObservation(pseudoObs);

        return resultSet;
    }

    /**
     * combine StrictMath.floor and StrictMath.abs to implement c++ fabs
     */
    public static float fabs(double x) {

        return (float) StrictMath.floor(StrictMath.abs(x));
    }
}
