package wde.compute.algo;

import wde.compute.Inference;
import wde.compute.InferenceResult;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.Observation;

import java.util.Set;

import static wde.compute.algo.PavementCondition.DRY_FROZEN_PAVEMENT;
import static wde.compute.algo.PavementCondition.DRY_PAVEMENT;
import static wde.compute.algo.PavementCondition.DRY_WET_PAVEMENT;
import static wde.compute.algo.PavementCondition.HYDROPLANE;
import static wde.compute.algo.PavementCondition.WET_PAVEMENT;
import static wde.compute.algo.PavementCondition.fabs;
import static wde.compute.algo.PrecipitationIntensity.LIGHT_PRECIP;
import static wde.compute.algo.PrecipitationIntensity.MODERATE_PRECIP;
import static wde.compute.algo.PrecipitationIntensity.ROAD_SPLASH;
import static wde.compute.algo.PrecipitationType.MIX;
import static wde.compute.algo.PrecipitationType.NO_PRECIP;
import static wde.compute.algo.PrecipitationType.RAIN;

public class PavementSlickness extends Inference {
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

        boolean pavement_slickness = false;

        // Determine precipitation interest
        float precip_intrst;
        if (precip_intensity != Integer.MAX_VALUE && precip_type != Integer.MAX_VALUE) {
            if (precip_intensity == NO_PRECIP)
                precip_intrst = -1;
            else if (precip_type == RAIN) {
                if (precip_intensity == LIGHT_PRECIP || precip_intensity == MODERATE_PRECIP || precip_intensity == ROAD_SPLASH)
                    precip_intrst = -0.5f;
                else // precip_intensity == HEAVY_PRECIP
                    precip_intrst = 0;
            } else if (precip_type == MIX) {
                precip_intrst = 0.5f;
            } else // precip_type == SNOW
            {
                if (precip_intensity == LIGHT_PRECIP || precip_intensity == MODERATE_PRECIP || precip_intensity == ROAD_SPLASH)
                    precip_intrst = 0.5f;
                else // precip_intensity == HEAVY_PRECIP
                    precip_intrst = 1;
            }
        } else
            precip_intrst = Integer.MAX_VALUE;

        // Determine pavement condition interest
        float pav_cond_intrst;
        if (pavement_condition != Integer.MAX_VALUE) {
            if (pavement_condition == DRY_PAVEMENT)
                pav_cond_intrst = -1;
            else if (pavement_condition == WET_PAVEMENT || pavement_condition == DRY_WET_PAVEMENT || pavement_condition == HYDROPLANE)
                pav_cond_intrst = 0;
            else if (pavement_condition == DRY_FROZEN_PAVEMENT)
                pav_cond_intrst = 0.5f;
            else // pavement_condition == SNOW_COVERED || pavement_condition == ICE_COVERED || pavement_condition == BLACK_ICE
                pav_cond_intrst = 1;
        } else
            pav_cond_intrst = Float.NaN;

        // For stability-interest, yaw-iqr-interest, yaw-median-interest:
        // If input to each interest component is missing
        // set the interest to 0.
        // Since we are doing a weighted sum, interest values of 0
        // will contribute nothing to the sum.
        // Note that even if inputs are not missing, interest values
        // can still be 0.

        // Determine if we have any abs, trac or stab data
        boolean stab_fields_flag = false;
        int num_abs_engaged = getRelatedObsValue(ObservationTypes.num_abs_engaged, obs).intValue();
        int num_trac_engaged = getRelatedObsValue(ObservationTypes.num_trac_engaged, obs).intValue();
        int num_stab_engaged = getRelatedObsValue(ObservationTypes.num_stab_engaged, obs).intValue();

        if (num_abs_engaged != Integer.MAX_VALUE || num_trac_engaged != Integer.MAX_VALUE ||
                num_stab_engaged != Integer.MAX_VALUE)
            stab_fields_flag = true;

        // Determine stability interest
        float stab_intrst;
        if (stab_fields_flag) {
            boolean abs_engaged_flag = abs_engaged(obs);
            boolean trac_engaged_flag = trac_engaged(obs);
            boolean stab_engaged_flag = stab_engaged(obs);
            int num_engaged = 0;
            if (abs_engaged_flag)
                num_engaged++;
            if (trac_engaged_flag)
                num_engaged++;
            if (stab_engaged_flag)
                num_engaged++;

            // If we have at least two out three, set stab_intrst to 1
            if (num_engaged >= 2)
                stab_intrst = 1;
            else
                stab_intrst = 0;
        } else
            stab_intrst = 0;

        // Deterime yaw-iqr interest
        float yaw_iqr_intrst;
        float yaw_iqr25 = getRelatedObsValue(ObservationTypes.yaw_iqr25, obs).floatValue();
        float yaw_iqr75 = getRelatedObsValue(ObservationTypes.yaw_iqr75, obs).floatValue();
        if (!Float.isNaN(yaw_iqr25) && !Float.isNaN(yaw_iqr75)) {
            float yaw_iqr_diff = yaw_iqr75 - yaw_iqr25;
            if (yaw_iqr_diff <= 1)
                yaw_iqr_intrst = yaw_iqr_diff;
            else // > 1
                yaw_iqr_intrst = 1;
        } else
            yaw_iqr_intrst = 0;

        // Determine yaw-median interest
        float yaw_median_intrst;
        float yaw_min = getRelatedObsValue(ObservationTypes.yaw_min, obs).floatValue();
        float yaw_max = getRelatedObsValue(ObservationTypes.yaw_max, obs).floatValue();
        float yaw_median = getRelatedObsValue(ObservationTypes.yaw_median, obs).floatValue();

        if (!Float.isNaN(yaw_min) && !Float.isNaN(yaw_max) &&
                !Float.isNaN(yaw_median)) {
            float yaw_min_diff = (float) fabs(yaw_min - yaw_median);
            float yaw_max_diff = (float) fabs(yaw_max - yaw_median);
            float yaw_diff;
            if (yaw_min_diff > yaw_max_diff)
                yaw_diff = yaw_min_diff;
            else // yaw_max_diff > yaw_min_diff
                yaw_diff = yaw_max_diff;

            if (yaw_diff <= 1)
                yaw_median_intrst = yaw_diff;
            else // > 1
                yaw_median_intrst = 1;
        } else
            yaw_median_intrst = 0;

        // Set weights of fields for slickness interest;
        float precip_wgt = 0.3f;
        float pav_cond_wgt = 0.3f;
        float stab_wgt = 0.2f;
        float yaw_iqr_wgt = 0.1f;
        float yaw_median_wgt = 0.1f;

        float slickness_intrst;
        if (!Float.isNaN(precip_intrst) && !Float.isNaN(pav_cond_intrst)) {
            slickness_intrst = (precip_wgt * precip_intrst) + (pav_cond_wgt * pav_cond_intrst) + (stab_wgt * stab_intrst) +
                    (yaw_iqr_wgt * yaw_iqr_intrst) + (yaw_median_wgt * yaw_median_intrst);
        } else
            slickness_intrst = Float.NaN;

        if (!Float.isNaN(slickness_intrst) && slickness_intrst >= 0.44)
            pavement_slickness = true;

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
                pavement_slickness ? 1 : 0
        );
        result.addObservation(pseudoObs);

        return resultSet;
    }

    public boolean wipers_on(IObs obs) {
        int num_wipers_on = get_wipers_on(obs);
        return (num_wipers_on > 0);
    }

    public boolean wipers_off(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_wipers_off, obs) > getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_off, obs) > getRelatedObsValue(ObservationTypes.num_wipers_high, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_off, obs) > getRelatedObsValue(ObservationTypes.num_wipers_low, obs));

    }

    public boolean wipers_high(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_wipers_high, obs) >= getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_high, obs) >= getRelatedObsValue(ObservationTypes.num_wipers_off, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_high, obs) >= getRelatedObsValue(ObservationTypes.num_wipers_low, obs));

    }

    public boolean wipers_intermittent(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs) >= getRelatedObsValue(ObservationTypes.num_wipers_low, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs) > getRelatedObsValue(ObservationTypes.num_wipers_high, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs) >= getRelatedObsValue(ObservationTypes.num_wipers_off, obs));

    }


    public boolean wipers_low(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_wipers_low, obs) > getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_low, obs) > getRelatedObsValue(ObservationTypes.num_wipers_high, obs) &&
                getRelatedObsValue(ObservationTypes.num_wipers_low, obs) >= getRelatedObsValue(ObservationTypes.num_wipers_off, obs));

    }


    public boolean abs_engaged(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_msg_valid_abs, obs).intValue() > 0 &&
                getRelatedObsValue(ObservationTypes.num_abs_engaged, obs).intValue() >
                        (getRelatedObsValue(ObservationTypes.num_abs_on, obs).intValue() +
                                getRelatedObsValue(ObservationTypes.num_abs_off, obs).intValue()));
    }


    public boolean abs_not_engaged(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_msg_valid_abs, obs).intValue() > 0 &&
                (getRelatedObsValue(ObservationTypes.num_abs_on, obs) +
                        getRelatedObsValue(ObservationTypes.num_abs_off, obs).intValue()) >
                        getRelatedObsValue(ObservationTypes.num_abs_engaged, obs).intValue());
    }


    public boolean trac_engaged(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_msg_valid_trac, obs).intValue() > 0 &&
                getRelatedObsValue(ObservationTypes.num_trac_engaged, obs).intValue() >
                        (getRelatedObsValue(ObservationTypes.num_trac_on, obs).intValue() +
                                getRelatedObsValue(ObservationTypes.num_trac_off, obs).intValue()));
    }


    public boolean trac_not_engaged(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_msg_valid_trac, obs) > 0 &&
                (getRelatedObsValue(ObservationTypes.num_trac_on, obs) +
                        getRelatedObsValue(ObservationTypes.num_trac_off, obs)) >
                        getRelatedObsValue(ObservationTypes.num_trac_engaged, obs));
    }


    public boolean stab_engaged(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_msg_valid_stab, obs) > 0 &&
                getRelatedObsValue(ObservationTypes.num_stab_engaged, obs) > (getRelatedObsValue(ObservationTypes.num_stab_on, obs) + getRelatedObsValue(ObservationTypes.num_stab_off, obs)));
    }


    public boolean stab_not_engaged(IObs obs) {
        return (getRelatedObsValue(ObservationTypes.num_msg_valid_stab, obs) > 0 &&
                (getRelatedObsValue(ObservationTypes.num_stab_on, obs) + getRelatedObsValue(ObservationTypes.num_stab_off, obs)) > getRelatedObsValue(ObservationTypes.num_stab_engaged, obs));
    }


    public boolean wipers_missing(IObs obs) {
    return (getRelatedObsValue(ObservationTypes.num_wipers_off, obs).isNaN() &&
                getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs).isNaN() &&
                getRelatedObsValue(ObservationTypes.num_wipers_low, obs).isNaN() &&
                getRelatedObsValue(ObservationTypes.num_wipers_high, obs).isNaN());
    }


    public boolean is_max(double val, double t1, double t2, double t3, double t4) {
        if (val > t1 &&
                val > t2 &&
                val > t3 &&
                val > t4) {
            return true;
        }
        return false;
    }


    public double get_dry_pavement(IObs obs) {
        double interest = 0.0;
        Double value = getRelatedObsValue(ObservationTypes.speed_ratio, obs);
        if (!value.isNaN()) {
            interest += value;
        }

  /*
  float air_temp = get_air_temp(this.resolver);
  if (air_temp != FILL_VALUE)
    {
      //not sure??
    }
  interest += get_interest(this.resolver.radar_ref,0,15,-0.033,0.995);
  interest += get_interest(this.resolver.radar_ref,15,22,-0.071,1.565);
  if (wipers_low(this.resolver))
    {
      interest += 1;
    }
  else if (wipers_high(this.resolver))
    {
      interest += 0.5;
    }
  if (abs_not_engaged(this.resolver))
    {
      interest += 1;
    }
  if (trac_not_engaged(this.resolver))
    {
      interest += 1;
    }
  if (stab_not_engaged(this.resolver))
    {
      interest += 1;
    }
  */


//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0, 0.2, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0.2, 0.24, -25, 6);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0, 0.5, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0.5, 1, -2, 2);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 0, 1, 1);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 1, 1.7, -1.428, 2.428);
        interest += get_interest(getRelatedObsValue(ObservationTypes.steering_angle_mean, obs), 0, 4, -0.25, 1);

        return interest;
    }

    public int get_wipers_on(IObs obs) {
        int num_wipers_on = 0;

        if (getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs).isNaN()) {
            num_wipers_on += getRelatedObsValue(ObservationTypes.num_wipers_intermittent, obs);
        }

        if (getRelatedObsValue(ObservationTypes.num_wipers_low, obs).isNaN()) {
            num_wipers_on += getRelatedObsValue(ObservationTypes.num_wipers_low, obs);
        }

        if (getRelatedObsValue(ObservationTypes.num_wipers_high, obs).isNaN()) {
            num_wipers_on += getRelatedObsValue(ObservationTypes.num_wipers_high, obs);
        }

        if (getRelatedObsValue(ObservationTypes.num_wipers_automatic, obs).isNaN()) {
            num_wipers_on += getRelatedObsValue(ObservationTypes.num_wipers_automatic, obs);
        }

        return num_wipers_on;
    }


    public double get_wet_pavement(IObs obs) {
        double interest = 0.0;

        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0, 0.8, 1.25, 0);
        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0.8, 1.0, -1.5, 2.2);

  /*
  float air_temp = get_air_temp(this.resolver);
  interest += get_interest(air_temp, -10, 0, 0.1, 1);
  interest += get_interest(air_temp, 0, 5, 1);

  interest += get_interest(this.resolver.radar_ref, 0, 10, 0.5);
  interest += get_interest(this.resolver.radar_ref, 10, 20, 0.05, 0);

  if (wipers_low(this.resolver) ||
      wipers_intermittent(this.resolver) ||
      wipers_high(this.resolver))
    {
      interest += 1;
    }
  */

        interest += get_abs_stab_trac_interest(obs, 0, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0, 0.2, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0.2, 0.3, -10, 3);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0, 0.5, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0.5, 1, -2, 2);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 0, 1, 1);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 1, 1.7, -1.428, 2.428);
        interest += get_interest(getRelatedObsValue(ObservationTypes.steering_angle_mean, obs), 0, 4, -0.25, 1);

        return interest;
    }


    public double get_snow_covered_pavement(IObs obs) {
        double interest = 0.0;
        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0, 0.6, 0.833, 0.502);
        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0.6, 1, -2, 2.2);

  /*
  interest += get_interest(this.resolver.air_temp_mean, -10, 0, 1);
  interest += get_interest(this.resolver.air_temp_mean, 0, 1, -1, 1);

  interest += get_interest(this.resolver.radar_ref,0, 20, 0.025, 0.5);
  interest += get_interest(this.resolver.radar_ref,20, 25, 1);
  if (wipers_on(this.resolver))
    {
      interest += 1;
    }
  */

        interest += get_abs_stab_trac_interest(obs, 1, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0, 0.2, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0.2, 0.4, -5, 2);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0, 0.5, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0.5, 1, -2, 2);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 0, 1, 1);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 1, 2, -1, 2);
        interest += get_interest(getRelatedObsValue(ObservationTypes.steering_angle_mean, obs), 0, 4, -0.25, 1);

        return interest;
    }


    public double get_slick_pavement(IObs obs) {
        double interest = 0.0;
        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0, 0.8, -1.25, 1);

  /*
  interest += get_interest(this.resolver.air_temp_mean, -10, -6, 1);
  interest += get_interest(this.resolver.air_temp_mean, -6, 1, -0.1428, 0.1432);

  interest += get_interest(this.resolver.radar_ref, 0, 20, 0.025, 0.5);
  interest += get_interest(this.resolver.radar_ref, 20, 25, 1);
  if (wipers_low(this.resolver) || wipers_high(this.resolver) || wipers_intermittent(this.resolver))
    {
      interest += 1;
    }
  */

        interest += get_abs_stab_trac_interest(obs, 2, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0, 0.2, 5, 0);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0.2, 0.4, 1);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0.3, 0.5, 5, -1.5);
//        interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0.5, 1, 1);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 0, 1, 1, 0);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 1, 2, 1);
        interest += get_interest(getRelatedObsValue(ObservationTypes.steering_angle_mean, obs), 2, 3, 1, -2);
        interest += get_interest(getRelatedObsValue(ObservationTypes.steering_angle_mean, obs), 3, 4, 1);

        return interest;
    }


    public double get_road_splash_pavement(IObs obs) {
        double interest = 0.0;

        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0, 0.8, 1.25, 0);
        interest += get_interest(getRelatedObsValue(ObservationTypes.speed_ratio, obs), 0.8, 1, -1.5, 2.2);
        
        interest += get_abs_stab_trac_interest(obs, 0.5, 1.0);
//        //interest += get_interest(getRelatedObsValue(ObservationTypes.lat_accel_mean, obs), 0.2, 0.4, -5, 2);
//        //interest += get_interest(getRelatedObsValue(ObservationTypes.lon_accel_mean, obs), 0.5, 1, -2, 2);
        interest += get_interest(getRelatedObsValue(ObservationTypes.yaw_mean, obs), 1, 1.7, -1.428, 2.428);
        interest += get_interest(getRelatedObsValue(ObservationTypes.steering_angle_mean, obs), 0, 4, 0.25, 1);

        return interest;
    }

    public double get_abs_stab_trac_interest(IObs obs, double engaged, double not_engaged) {
        double interest = 0.0;
        if (abs_not_engaged(obs)) {
            interest += not_engaged;
        }
        if (trac_not_engaged(obs)) {
            interest += not_engaged;
        }
        if (stab_not_engaged(obs)) {
            interest += not_engaged;
        }
        if (abs_engaged(obs)) {
            interest += engaged;
        }
        if (trac_engaged(obs)) {
            interest += engaged;
        }
        if (stab_engaged(obs)) {
            interest += engaged;
        }
        return interest;
    }

    public double get_interest(double val, double slope_min, double slope_max, double m, double b) {
        if (!Double.isNaN(val)) {
            return 0;
        }

        if (val >= slope_min && val < slope_max) {
            return m * val + b;
        }

        return 0;
    }


    public double get_interest(double val, double min, double max, double flat_val) {
        if (!Double.isNaN(val)) {
            return 0;
        }

        if (val >= min && val < max) {
            return flat_val;
        }

        return 0;
    }



}
