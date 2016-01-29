package wde.compute.algo;

import wde.compute.Inference;
import wde.compute.InferenceResult;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.Observation;

import java.util.Set;

import static wde.compute.algo.PrecipitationIntensity.HEAVY_PRECIP;
import static wde.compute.algo.PrecipitationType.MIX;
import static wde.compute.algo.PrecipitationType.NO_PRECIP;
import static wde.compute.algo.PrecipitationType.RAIN;
import static wde.compute.algo.PrecipitationType.SNOW;

public class Visibility extends Inference {

    // Visibility enumeration
    public static final int VIS_NORMAL = 0;
    public static final int VIS_LOW = 1;
    public static final int VIS_HEAVY_RAIN = 2;
    public static final int VIS_HEAVY_SNOW = 3;
    public static final int VIS_BLOWING_SNOW = 4;
    public static final int VIS_FOG = 5;
    public static final int VIS_HAZE = 6;
    public static final int VIS_DUST = 7;
    public static final int VIS_SMOKE = 8;

    // Wind-speed threshold for blowing snow (m/s)
    public final int BLOWING_SNOW_WIND_SPEED = 10; // 10 m/s

    public final float precip_type_wgt = 0.20f;
    public final float precip_intensity_wgt = 0.30f;

    public final float fog_wgt = 0.50f;
    public final float gen_vis_wgt = 0.30f;

    @Override
    public Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs) {
        Set<InferenceResult> resultSet = newInferenceResultSet();
        InferenceResult result = new InferenceResult();
        resultSet.add(result);

        result.setName("PavementSlickness");
        result.setObsTypeId(obsTypeId);

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

        int pavement_condition = Integer.MAX_VALUE;
        IObs pavementConditionObs = getRelatedObs(1000002, obs);
        if (pavementConditionObs != null) {
            pavement_condition = ((Double) pavementConditionObs.getValue()).intValue();
        }

        int visibility = Integer.MAX_VALUE;

        // Determine what VDT air temp and dewpt we should use
        // to calculate rh for fog calculation and generic visibility calculations
        //
        float mobile_air_temp = get_air_temp(obs);
        float env_air_temp = get_env_air_temp(obs);
        float air_temp = determine_temp(mobile_air_temp, env_air_temp);

        float mobile_dew_temp = getRelatedObsValue(ObservationTypes.dew_temp_mean, obs).floatValue();
        float env_dew_temp = getRelatedObsValue(ObservationTypes.nss_dew_temp_mean, obs).floatValue();
        float dew_temp = determine_temp(mobile_dew_temp, env_dew_temp);

        float rh = getRelatedObsValue(ObservationTypes.ess_relative_Humidity, obs).floatValue();
        //float rh = calc_rh(air_temp, dew_temp);

        // Get addtional VDT variables
        float wind_speed = getRelatedObsValue(ObservationTypes.nss_wind_speed_mean, obs).floatValue();

        float speed_ratio = getRelatedObsValue(ObservationTypes.speed_ratio, obs).floatValue();
        float env_vis = getRelatedObsValue(ObservationTypes.nss_prevail_vis_mean, obs).floatValue();

        int num_msg_valid_lights = getRelatedObsValue(ObservationTypes.num_msg_valid_lights, obs).intValue();
        int num_lights_off = getRelatedObsValue(ObservationTypes.num_lights_off, obs).intValue();
        int num_lights_fog = getRelatedObsValue(ObservationTypes.num_lights_fog, obs).intValue();
        int num_lights_high_beam = getRelatedObsValue(ObservationTypes.num_lights_high_beam, obs).intValue();
        float percent_lights_off = calc_percentage((float) num_lights_off, (float) num_msg_valid_lights);
        float percent_lights_fog = calc_percentage((float) num_lights_fog, (float) num_msg_valid_lights);
        float percent_lights_high_beam = calc_percentage((float) num_lights_high_beam, (float) num_msg_valid_lights);

        // Determine percent lights on from percent lights off
        float percent_lights_on;
        if (!Float.isNaN(percent_lights_off))
            percent_lights_on = 1 - percent_lights_off;
        else
            percent_lights_on = Float.NaN;

        // Calculate fog interest
        float fog_interest = calc_fog_interest(rh, speed_ratio, env_vis, percent_lights_fog, percent_lights_high_beam);

        // Calculate generic visibility interest
        float gen_vis_interest = calc_generic_vis_interest(rh, speed_ratio, env_vis, percent_lights_on, percent_lights_fog, percent_lights_high_beam);

        // Hardwire the "input" confidence for now
        // Do we want to calculate this in the calc_fog and cal_generic_vis interest functions?
        float fog_input_conf = 1.0f;
        float gen_vis_input_conf = 1.0f;

        // VDT case precip-type and precip-intensity
        if (precip_type != Integer.MAX_VALUE && precip_intensity != Integer.MAX_VALUE) {
            if (precip_type == RAIN && precip_intensity == HEAVY_PRECIP)
                visibility = VIS_HEAVY_RAIN;
            else if (precip_type == SNOW && precip_intensity != NO_PRECIP) {
                if (!Float.isNaN(wind_speed) && wind_speed > BLOWING_SNOW_WIND_SPEED)
                    visibility = VIS_BLOWING_SNOW;
                else if (precip_intensity == HEAVY_PRECIP)
                    visibility = VIS_HEAVY_SNOW;
                else
                    visibility = VIS_NORMAL; // For LIGHT, MODERATE AND ROAD_SPLASH intensity for type SNOW
            } else if (precip_type == MIX && precip_intensity == HEAVY_PRECIP)
                visibility = VIS_LOW;
            else
                visibility = VIS_NORMAL;

            confidence = confidence + (precip_type_input_conf * precip_type_wgt) + (precip_intensity_input_conf * precip_intensity_wgt);
        } else if (precip_intensity != Integer.MAX_VALUE) // VDT case with just precip-intensity
        {
            if (precip_intensity == HEAVY_PRECIP)
                visibility = VIS_LOW;
            else
                visibility = VIS_NORMAL;

            confidence = confidence + (precip_intensity_input_conf * precip_intensity_wgt);
        } else // VDT case when precip-type and precip-intensity are missing
        {
            visibility = VIS_NORMAL;
            confidence = confidence + (gen_vis_input_conf * gen_vis_wgt);
        }

        // If visibility is still missing look for fog or generic low visibiltiy
        // We have higher confidence in the fog calculation?
        // Less confidence in generic vis function?
        if (visibility == Integer.MAX_VALUE || visibility == VIS_NORMAL) {
            if (fog_interest > 0.4001) // VDT case with fog_interest
            {
                visibility = VIS_FOG;
                confidence = confidence + (fog_input_conf * fog_wgt);
            } else if (gen_vis_interest > 0.5001) // VDT case with generic visibility interest
            {
                visibility = VIS_LOW;
                confidence = confidence + (gen_vis_input_conf * gen_vis_wgt);
            }
        }

        result.setConfidence(confidence);
        IObs pseudoObs = new Observation(
                1000004,
                0,
                0,
                obs.getObsTimeLong(),
                obs.getRecvTimeLong(),
                obs.getLatitude(),
                obs.getLongitude(),
                obs.getElevation(),
                visibility
        );
        result.addObservation(pseudoObs);

        return resultSet;

    }

    public float get_air_temp(IObs obs) {
        if (getRelatedObsValue(ObservationTypes.air_temp_mean, obs).isNaN() &&
                getRelatedObsValue(ObservationTypes.air_temp2_mean, obs).isNaN()) {
            return Float.NaN;
        }

        // Use air_temp_mean if it is there, if not fall back on air_temp2_mean
        float air_temp;
        if (!getRelatedObsValue(ObservationTypes.air_temp_mean, obs).isNaN())
            air_temp = getRelatedObsValue(ObservationTypes.air_temp_mean, obs).floatValue();
        else
            air_temp = getRelatedObsValue(ObservationTypes.air_temp2_mean, obs).floatValue();

        return air_temp;
    }

    public float get_env_air_temp(IObs obs) {
        if (getRelatedObsValue(ObservationTypes.nss_air_temp_mean, obs).isNaN() &&
                getRelatedObsValue(ObservationTypes.model_air_temp, obs).isNaN()) {
            return Float.NaN;
        }

        // Use nss_air_temp_mean if it is there, if not fall back on model_air_temp
        float env_air_temp;
        if (!getRelatedObsValue(ObservationTypes.nss_air_temp_mean, obs).isNaN())
            env_air_temp = getRelatedObsValue(ObservationTypes.nss_air_temp_mean, obs).floatValue();
        else
            env_air_temp = getRelatedObsValue(ObservationTypes.model_air_temp, obs).floatValue();

        return env_air_temp;
    }

    public float determine_temp(float mobile_temp, float env_temp) {
        float temp;

        if (!Float.isNaN(mobile_temp))
            temp = mobile_temp;
        else if (!Float.isNaN(env_temp))
            temp = env_temp;
        else
            temp = Float.NaN;

        return temp;
    }

    public float calc_fog_interest(float rh, float speed_ratio, float station_vis, float percent_fog_lights, float percent_high_beams) {
        // Determine interest components for fog
        // If input to each interest component is missing
        // set the interest to 0.
        // Since we are doing a weighted sum, interest values of 0
        // will contribute nothing to the sum.
        // Note that even if inputs are not missing, interest values
        // can still be 0.

        // Determine rh interest
        float rh_intrst;
        if (!Float.isNaN(rh)) {
            // Convert rh to a percentage
            float rh_pct = rh * 100;

            if (rh_pct < 40)
                rh_intrst = -1;
            else if (rh_pct >= 40 && rh_pct <= 60)
                rh_intrst = (rh_pct / 20) - 3;
            else if (rh_pct > 60 && rh_pct <= 80)
                rh_intrst = 0;
            else if (rh_pct > 80 && rh_pct <= 100)
                rh_intrst = (rh_pct / 20) - 4;
            else
                rh_intrst = 1;
        } else
            rh_intrst = 0;

        // Determine speed ratio interest
        float speed_ratio_intrst;
        if (!Float.isNaN(speed_ratio)) {
            if (speed_ratio < 0.2)
                speed_ratio_intrst = 5 * speed_ratio;
            else if (speed_ratio >= 0.2 && speed_ratio <= 0.5)
                speed_ratio_intrst = (float) ((-2 * speed_ratio / 3) + (17.0 / 15.0));
            else if (speed_ratio > 0.5 && speed_ratio <= 1)
                speed_ratio_intrst = (float) ((-8 * speed_ratio / 5) + (8.0 / 5.0));
            else
                speed_ratio_intrst = 0;
        } else
            speed_ratio_intrst = 0;

        // Determine station visibility interest
        float station_vis_intrst;
        if (!Float.isNaN(station_vis)) {
            // Convert visibility from km to miles
            float station_vis_mi = station_vis * 0.621371f;

            if (station_vis_mi >= 0 && station_vis_mi <= 10)
                station_vis_intrst = (-2 * station_vis_mi / 10) + 1;
            else
                station_vis_intrst = 0;
        } else
            station_vis_intrst = 0;

        // Determine fog lights interest
        float fog_lights_intrst;
        if (!Float.isNaN(percent_fog_lights)) {
            if (percent_fog_lights > 0 && percent_fog_lights <= 1)
                fog_lights_intrst = percent_fog_lights;
            else
                fog_lights_intrst = 0;
        } else
            fog_lights_intrst = 0;

        // Determine high beams interest
        float high_beams_intrst;
        if (!Float.isNaN(percent_high_beams)) {
            if (percent_high_beams > 0 && percent_high_beams < 1)
                high_beams_intrst = percent_high_beams;
            else
                high_beams_intrst = 0;
        } else
            high_beams_intrst = 0;


        // Determine station fog interest
        // Currently don't have station fog at all

        // Set weights of fields for fog interest
        float rh_wgt = 0.4f;
        float fog_lights_wgt = 0.2f;
        float high_beams_wgt = -0.2f;
        float speed_ratio_wgt = 0.2f;
        float station_vis_wgt = 0.2f; // bumbed this up since we don't have station fog
        float station_fog_wgt = 0.1f; // we currently don't have this

        // Determine fog interest
        float fog_intrst = (rh_wgt * rh_intrst) + (fog_lights_wgt * fog_lights_intrst) + (high_beams_wgt * high_beams_intrst) + (speed_ratio_wgt * speed_ratio_intrst) + (station_vis_wgt * station_vis_intrst);

        return fog_intrst;
    }


    public float calc_generic_vis_interest(float rh, float speed_ratio, float station_vis, float percent_lights_on, float percent_fog_lights, float percent_high_beams) {
        // Determine interest components for generic visibility
        // If input to each interest component is missing
        // set the interest to 0.
        // Since we are doing a weighted sum, interest values of 0
        // will contribute nothing to the sum.
        // Note that even if inputs are not missing, interest values
        // can still be 0.

        // Determine rh interest
        float rh_intrst;
        if (!Float.isNaN(rh)) {
            // Convert rh to a percentage
            float rh_pct = rh * 100;


            if (rh_pct >= 0 && rh_pct < 40)
                rh_intrst = (rh_pct / 40) - 1;
            else if (rh_pct >= 40 && rh_pct <= 60)
                rh_intrst = 0;
            else if (rh_pct > 60 && rh_pct <= 100)
                rh_intrst = (float) ((rh_pct / 40) - (3.0 / 2.0));
            else
                rh_intrst = 1;
        } else
            rh_intrst = 0;

        // Determine speed ratio interest
        float speed_ratio_intrst;
        if (!Float.isNaN(speed_ratio)) {
            if (speed_ratio < 0.2)
                speed_ratio_intrst = 5 * speed_ratio;
            else if (speed_ratio >= 0.2 && speed_ratio <= 0.5)
                speed_ratio_intrst = (float) ((-2 * speed_ratio / 3) + (17.0 / 15.0));
            else if (speed_ratio > 0.5 && speed_ratio <= 1)
                speed_ratio_intrst = (float) ((-8 * speed_ratio / 5) + (8.0 / 5.0));
            else
                speed_ratio_intrst = 0;
        } else
            speed_ratio_intrst = 0;

        // Determine station visibility interest
        float station_vis_intrst;
        if (!Float.isNaN(station_vis)) {
            // Convert visibility from km to miles
            float station_vis_mi = station_vis * 0.621371f;


            if (station_vis_mi >= 0 && station_vis_mi <= 10)
                station_vis_intrst = (-2 * station_vis_mi / 10) + 1;
            else
                station_vis_intrst = 0;
        } else
            station_vis_intrst = 0;

        // Determine lights on interest
        float lights_on_intrst;
        if (!Float.isNaN(percent_lights_on)) {
            if (percent_lights_on > 0 && percent_lights_on <= 1)
                lights_on_intrst = percent_lights_on;
            else
                lights_on_intrst = 0;
        } else
            lights_on_intrst = 0;

        // Determine fog lights interest
        float fog_lights_intrst;
        if (!Float.isNaN(percent_fog_lights)) {
            if (percent_fog_lights > 0 && percent_fog_lights <= 1)
                fog_lights_intrst = percent_fog_lights;
            else
                fog_lights_intrst = 0;
        } else
            fog_lights_intrst = 0;

        // Determine high beams interest
        float high_beams_intrst;
        if (!Float.isNaN(percent_high_beams)) {
            if (percent_high_beams > 0 && percent_high_beams < 1)
                high_beams_intrst = percent_high_beams;
            else
                high_beams_intrst = 0;
        } else
            high_beams_intrst = 0;

        // Set weights of fields for light interest
        float lights_on_wgt = 0.375f;
        float fog_lights_wgt = 0.625f;
        float high_beams_wgt = -0.125f;

        // Determine light interest
        float lights_intrst = (lights_on_wgt * lights_on_intrst) + (fog_lights_wgt * fog_lights_intrst) + (high_beams_wgt * high_beams_intrst);

        // Set weights of fields for generic visibility interest
        float rh_wgt = 0.3f;
        float speed_ratio_wgt = 0.25f;
        float lights_wgt = 0.25f;
        float station_vis_wgt = 0.2f;

        // Determine generic visibility interest
        float vis_intrst = (rh_wgt * rh_intrst) + (speed_ratio_wgt * speed_ratio_intrst) + (lights_wgt * lights_intrst) + (station_vis_wgt * station_vis_intrst);

        return vis_intrst;
    }

    public float calc_rh(float temp, float dewpt) {
        float rh;

        if (Float.isNaN(temp) || Float.isNaN(dewpt))
            return Float.NaN;

        rh = calc_pr_vapr(dewpt) / calc_pr_vapr(temp);

        return rh;
    }

    public float calc_pr_vapr(float temp) {
        float pr_vap = Float.NaN;

        if (!Float.isNaN(temp))
            pr_vap = (float) (6.112 * StrictMath.exp((17.67 * temp) / (temp + 243.5)));

        return (pr_vap);
    }

    public float calc_percentage(float num_values, float total_values) {
        float percentage;

        if (Float.isNaN(num_values) || Float.isNaN(total_values))
            return Float.NaN;

        percentage = num_values / total_values;
        return percentage;
    }
}
