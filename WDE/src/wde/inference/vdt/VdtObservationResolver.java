package wde.inference.vdt;

import wde.inference.AbstractObservationResolver;
import wde.obs.IObs;

public abstract class VdtObservationResolver extends AbstractObservationResolver {

    public static final int FILL_VALUE = -9999;

    private final IObs currentObs;
    private final VdtObservationProcessor observationProcessor;

    public VdtObservationResolver(VdtObservationProcessor processor, IObs obs) {
        super();

        this.currentObs = obs;
        this.observationProcessor = processor;
    }

    public VdtObservationProcessor getObservationProcessor() {
        return this.observationProcessor;
    }

    public IObs getCurrentObs() {
        return currentObs;
    }

    public float getFcst_precip_type() {
        return resolveFloatValue("fcst_precip_type");
    }

    public float getFcst_air_temp() {
        return resolveFloatValue("fcst_air_temp");
    }

    public float getFcst_precip_rate() {
        return resolveFloatValue("fcst_precip_rate");
    }

    public abstract float getRadarRef();

    public float getSpeed_ratio() {
        return resolveFloatValue("speed_ratio");
    }

    public int getNum_msg_valid_lights() {
        return resolveIntegerValue("num_msg_valid_lights");
    }

    public int getNum_lights_off() {
        return resolveIntegerValue("num_lights_off");
    }

    public int getNum_wipers_off() {
        return resolveIntegerValue("num_wipers_off");
    }

    public int getNum_wipers_intermittent() {
        return resolveIntegerValue("num_wipers_intermittent");
    }

    public int getNum_wipers_low() {
        return resolveIntegerValue("num_wipers_low");
    }

    public int getNum_wipers_high() {
        return resolveIntegerValue("num_wipers_high");
    }

    public float getFcst_road_temp() {
        return resolveFloatValue("fcst_road_temp");
    }

    public float getFcst_road_water_phase_type() {
        return resolveFloatValue("fcst_road_water_phase_type");
    }

    public float getSurface_temp_mean() {
        return resolveFloatValue("surface_temp_mean");
    }

    public float getFcst_visibility() {
        return resolveFloatValue("fcst_visibility");
    }

    public float getFcst_wind_speed() {
        return resolveFloatValue("fcst_wind_speed");
    }

    public float getFcst_prob_fog() {
        return resolveFloatValue("fcst_prob_fog");
    }

    public float getDew_temp_mean() {
        return resolveFloatValue("dew_temp_mean");
    }

    public float getNss_dew_temp_mean() {
        return resolveFloatValue("nss_dew_temp_mean");
    }

    public float getNss_wind_speed_mean() {
        return resolveFloatValue("nss_wind_speed_mean");
    }

    public float getNss_prevail_vis_mean() {
        return resolveFloatValue("nss_prevail_vis_mean");
    }

    public int getNum_lights_fog() {
        return resolveIntegerValue("num_lights_fog");
    }

    public int getNum_lights_high_beam() {
        return resolveIntegerValue("num_lights_high_beam");
    }

    public int getNum_abs_engaged() {
        return resolveIntegerValue("num_abs_engaged");
    }

    public int getNum_trac_engaged() {
        return resolveIntegerValue("num_trac_engaged");
    }

    public int getNum_stab_engaged() {
        return resolveIntegerValue("num_stab_engaged");
    }

    public float getYaw_iqr25() {
        return resolveFloatValue("yaw_iqr25");
    }

    public float getYaw_iqr75() {
        return resolveFloatValue("yaw_iqr75");
    }

    public float getYaw_min() {
        return resolveFloatValue("yaw_min");
    }

    public float getYaw_max() {
        return resolveFloatValue("yaw_max");
    }

    public float getYaw_median() {
        return resolveFloatValue("yaw_median");
    }

    public int getNum_abs_on() {
        return resolveIntegerValue("num_abs_on");
    }

    public int getNum_abs_off() {
        return resolveIntegerValue("num_abs_off");
    }

    public int getNum_trac_on() {
        return resolveIntegerValue("num_trac_on");
    }

    public int getNum_trac_off() {
        return resolveIntegerValue("num_trac_off");
    }

    public int getNum_stab_on() {
        return resolveIntegerValue("num_stab_on");
    }

    public int getNum_stab_off() {
        return resolveIntegerValue("num_stab_off");
    }

    public int getNum_wipers_automatic() {
        return resolveIntegerValue("num_wipers_automatic");
    }

    public float getAir_temp_mean() {
        return resolveFloatValue("air_temp_mean");
    }

    public float getAir_temp2_mean() {
        return resolveFloatValue("air_temp2_mean");
    }

    public float getNss_air_temp_mean() {
        return resolveFloatValue("nss_air_temp_mean");
    }

    public float getModel_air_temp() {
        return resolveFloatValue("model_air_temp");
    }

    public int getNum_msg_valid_abs() {
        return resolveIntegerValue("num_msg_valid_abs");
    }

    public int getNum_msg_valid_trac() {
        return resolveIntegerValue("num_msg_valid_trac");
    }

    public int getNum_msg_valid_stab() {
        return resolveIntegerValue("num_msg_valid_stab");
    }

    public double getLat_accel_mean() {
        return resolveDoubleValue("lat_accel_mean");
    }

    public double getLon_accel_mean() {
        return resolveDoubleValue("lon_accel_mean");
    }

    public double getYaw_mean() {
        return resolveDoubleValue("yaw_mean");
    }

    public double getSteering_angle_mean() {
        return resolveDoubleValue("steering_angle_mean");
    }
}
