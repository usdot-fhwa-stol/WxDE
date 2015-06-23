/************************************************************************
 * Source filename: SegmentStatistics.java
 * <p/>
 * Creation date: Nov 18, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt;

import org.apache.log4j.Logger;

public class SegmentStatistics extends NcMem {

    private static final Logger logger = Logger.getLogger(SegmentStatistics.class);

    public SegmentStatistics() {

        super();

        for (String attrName : attributeMap.keySet())
            addVariable(attrName, attributeMap.get(attrName));

        for (String varName : variableMap.keySet())
            addVariable(varName, variableMap.get(varName));
    }

    protected void loadVariableList() {

        logger.info("calling loadVariableList");

        attributeMap.put("begin_time", Double.class);
        attributeMap.put("end_time", Double.class);

        attributeMap.put("id", Integer.class);
        attributeMap.put("mid_point_latitude", Double.class);
        attributeMap.put("mid_point_longitude", Double.class);

        variableMap.put("total_num_msg", Integer.class);
        variableMap.put("model_air_temp", Float.class);
        variableMap.put("model_bar_press", Float.class);
        variableMap.put("nss_air_temp_mean", Float.class);
        variableMap.put("nss_bar_press_mean", Float.class);
        variableMap.put("nss_dew_temp_mean", Float.class);
        variableMap.put("nss_hourly_precip_mean", Float.class);
        variableMap.put("nss_prevail_vis_mean", Float.class);
        variableMap.put("nss_wind_dir_mean", Float.class);
        variableMap.put("nss_wind_speed_mean", Float.class);
        variableMap.put("radar_cref", Float.class);
        variableMap.put("radar_precip_flag", Short.class);
        variableMap.put("radar_precip_type", Short.class);
        variableMap.put("cloud_mask", Short.class);

        variableMap.put("num_msg_valid_air_temp", Integer.class);
        variableMap.put("air_temp_iqr25", Float.class);
        variableMap.put("air_temp_iqr75", Float.class);
        variableMap.put("air_temp_max", Float.class);
        variableMap.put("air_temp_mean", Float.class);
        variableMap.put("air_temp_median", Float.class);
        variableMap.put("air_temp_min", Float.class);
        variableMap.put("air_temp_stdev", Float.class);
        variableMap.put("air_temp_var", Float.class);

        variableMap.put("num_msg_valid_air_temp2", Integer.class);
        variableMap.put("air_temp2_iqr25", Float.class);
        variableMap.put("air_temp2_iqr75", Float.class);
        variableMap.put("air_temp2_max", Float.class);
        variableMap.put("air_temp2_mean", Float.class);
        variableMap.put("air_temp2_median", Float.class);
        variableMap.put("air_temp2_min", Float.class);
        variableMap.put("air_temp2_stdev", Float.class);
        variableMap.put("air_temp2_var", Float.class);

        variableMap.put("num_msg_valid_bar_press", Integer.class);
        variableMap.put("bar_press_iqr25", Float.class);
        variableMap.put("bar_press_iqr75", Float.class);
        variableMap.put("bar_press_max", Float.class);
        variableMap.put("bar_press_mean", Float.class);
        variableMap.put("bar_press_median", Float.class);
        variableMap.put("bar_press_min", Float.class);
        variableMap.put("bar_press_stdev", Float.class);
        variableMap.put("bar_press_var", Float.class);

        variableMap.put("num_msg_valid_dew_temp", Integer.class);
        variableMap.put("dew_temp_iqr25", Float.class);
        variableMap.put("dew_temp_iqr75", Float.class);
        variableMap.put("dew_temp_max", Float.class);
        variableMap.put("dew_temp_mean", Float.class);
        variableMap.put("dew_temp_median", Float.class);
        variableMap.put("dew_temp_min", Float.class);
        variableMap.put("dew_temp_stdev", Float.class);
        variableMap.put("dew_temp_var", Float.class);

        variableMap.put("num_msg_valid_heading", Integer.class);
        variableMap.put("heading_iqr25", Float.class);
        variableMap.put("heading_iqr75", Float.class);
        variableMap.put("heading_max", Float.class);
        variableMap.put("heading_mean", Float.class);
        variableMap.put("heading_median", Float.class);
        variableMap.put("heading_min", Float.class);
        variableMap.put("heading_stdev", Float.class);
        variableMap.put("heading_var", Float.class);

        variableMap.put("num_msg_valid_hoz_accel_lat", Integer.class);
        variableMap.put("hoz_accel_lat_iqr25", Float.class);
        variableMap.put("hoz_accel_lat_iqr75", Float.class);
        variableMap.put("hoz_accel_lat_max", Float.class);
        variableMap.put("hoz_accel_lat_mean", Float.class);
        variableMap.put("hoz_accel_lat_median", Float.class);
        variableMap.put("hoz_accel_lat_min", Float.class);
        variableMap.put("hoz_accel_lat_stdev", Float.class);
        variableMap.put("hoz_accel_lat_var", Float.class);

        variableMap.put("num_msg_valid_hoz_accel_lon", Integer.class);
        variableMap.put("hoz_accel_lon_iqr25", Float.class);
        variableMap.put("hoz_accel_lon_iqr75", Float.class);
        variableMap.put("hoz_accel_lon_max", Float.class);
        variableMap.put("hoz_accel_lon_mean", Float.class);
        variableMap.put("hoz_accel_lon_median", Float.class);
        variableMap.put("hoz_accel_lon_min", Float.class);
        variableMap.put("hoz_accel_lon_stdev", Float.class);
        variableMap.put("hoz_accel_lon_var", Float.class);

        variableMap.put("num_msg_valid_abs", Integer.class);
        variableMap.put("num_abs_engaged", Integer.class);
        variableMap.put("num_abs_not_equipped", Integer.class);
        variableMap.put("num_abs_off", Integer.class);
        variableMap.put("num_abs_on", Integer.class);

        variableMap.put("num_msg_valid_brakes", Integer.class);
        variableMap.put("num_brakes_all_off", Integer.class);
        variableMap.put("num_brakes_all_on", Integer.class);
        variableMap.put("num_brakes_lf_active", Integer.class);
        variableMap.put("num_brakes_lr_active", Integer.class);
        variableMap.put("num_brakes_rf_active", Integer.class);
        variableMap.put("num_brakes_rr_active", Integer.class);

        variableMap.put("num_msg_valid_brakes_boost", Integer.class);
        variableMap.put("num_brakes_boost_not_equipped", Integer.class);
        variableMap.put("num_brakes_boost_off", Integer.class);
        variableMap.put("num_brakes_boost_on", Integer.class);

        variableMap.put("num_msg_valid_lights", Integer.class);
        variableMap.put("num_lights_automatic_control", Integer.class);
        variableMap.put("num_lights_drl", Integer.class);
        variableMap.put("num_lights_fog", Integer.class);
        variableMap.put("num_lights_hazard", Integer.class);
        variableMap.put("num_lights_high_beam", Integer.class);
        variableMap.put("num_lights_left_turn", Integer.class);
        variableMap.put("num_lights_low_beam", Integer.class);
        variableMap.put("num_lights_off", Integer.class);
        variableMap.put("num_lights_parking", Integer.class);
        variableMap.put("num_lights_right_turn", Integer.class);

        variableMap.put("num_msg_valid_stab", Integer.class);
        variableMap.put("num_stab_engaged", Integer.class);
        variableMap.put("num_stab_not_equipped", Integer.class);
        variableMap.put("num_stab_off", Integer.class);
        variableMap.put("num_stab_on", Integer.class);

        variableMap.put("num_msg_valid_trac", Integer.class);
        variableMap.put("num_trac_engaged", Integer.class);
        variableMap.put("num_trac_not_equipped", Integer.class);
        variableMap.put("num_trac_off", Integer.class);
        variableMap.put("num_trac_on", Integer.class);

        variableMap.put("num_msg_valid_wipers", Integer.class);
        variableMap.put("num_wipers_automatic", Integer.class);
        variableMap.put("num_wipers_high", Integer.class);
        variableMap.put("num_wipers_intermittent", Integer.class);
        variableMap.put("num_wipers_low", Integer.class);
        variableMap.put("num_wipers_not_equipped", Integer.class);
        variableMap.put("num_wipers_off", Integer.class);
        variableMap.put("num_wipers_washer", Integer.class);

        variableMap.put("num_msg_valid_speed", Integer.class);
        variableMap.put("speed_iqr25", Float.class);
        variableMap.put("speed_iqr75", Float.class);
        variableMap.put("speed_max", Float.class);
        variableMap.put("speed_mean", Float.class);
        variableMap.put("speed_median", Float.class);
        variableMap.put("speed_min", Float.class);
        variableMap.put("speed_ratio", Float.class);
        variableMap.put("speed_stdev", Float.class);
        variableMap.put("speed_var", Float.class);

        variableMap.put("num_msg_valid_steering_angle", Integer.class);
        variableMap.put("steering_angle_iqr25", Float.class);
        variableMap.put("steering_angle_iqr75", Float.class);
        variableMap.put("steering_angle_max", Float.class);
        variableMap.put("steering_angle_mean", Float.class);
        variableMap.put("steering_angle_median", Float.class);
        variableMap.put("steering_angle_min", Float.class);
        variableMap.put("steering_angle_stdev", Float.class);
        variableMap.put("steering_angle_var", Float.class);

        variableMap.put("num_msg_valid_steering_rate", Integer.class);
        variableMap.put("steering_rate_max", Float.class);
        variableMap.put("steering_rate_mean", Float.class);
        variableMap.put("steering_rate_median", Float.class);
        variableMap.put("steering_rate_min", Float.class);
        variableMap.put("steering_rate_stdev", Float.class);
        variableMap.put("steering_rate_var", Float.class);

        variableMap.put("num_msg_valid_surface_temp", Integer.class);
        variableMap.put("surface_temp_iqr25", Float.class);
        variableMap.put("surface_temp_iqr75", Float.class);
        variableMap.put("surface_temp_max", Float.class);
        variableMap.put("surface_temp_mean", Float.class);
        variableMap.put("surface_temp_median", Float.class);
        variableMap.put("surface_temp_min", Float.class);
        variableMap.put("surface_temp_stdev", Float.class);
        variableMap.put("surface_temp_var", Float.class);

        variableMap.put("num_msg_valid_yaw", Integer.class);
        variableMap.put("yaw_iqr25", Float.class);
        variableMap.put("yaw_iqr75", Float.class);
        variableMap.put("yaw_max", Float.class);
        variableMap.put("yaw_mean", Float.class);
        variableMap.put("yaw_median", Float.class);
        variableMap.put("yaw_min", Float.class);
        variableMap.put("yaw_stdev", Float.class);
        variableMap.put("yaw_var", Float.class);
    }
}
