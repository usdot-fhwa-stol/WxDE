/************************************************************************
 * Source filename: QcProbeMessage.java
 * <p/>
 * Creation date: Oct 17, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: VDT Integration
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt.probe;

import org.apache.log4j.Logger;

public class QcProbeMessage extends ProbeMessage {

    private static final Logger logger = Logger.getLogger(QcProbeMessage.class);

    public QcProbeMessage() {

        super();

        for (String varName : variableMap.keySet())
            addVariable(varName, variableMap.get(varName));
    }

    public static void main(String[] args) {
        QcProbeMessage qpm = new QcProbeMessage();
    }

    protected void loadVariableList() {

        logger.info("calling loadVariableList");

        super.loadVariableList();

        variableMap.put("abs_range_qc_passed", Short.class);

        variableMap.put("air_temp_cat_passed", Short.class);
        variableMap.put("air_temp_crt_passed", Short.class);
        variableMap.put("air_temp_mat_passed", Short.class);
        variableMap.put("air_temp_nst_passed", Short.class);
        variableMap.put("air_temp_nvt_passed", Short.class);
        variableMap.put("air_temp_persist_passed", Short.class);
        variableMap.put("air_temp_range_qc_passed", Short.class);
        variableMap.put("air_temp_sdt_passed", Short.class);
        variableMap.put("air_temp_spatial_barnes_passed", Short.class);
        variableMap.put("air_temp_spatial_iqr_passed", Short.class);
        variableMap.put("air_temp_step_passed", Short.class);

        variableMap.put("air_temp2_cat_passed", Short.class);
        variableMap.put("air_temp2_crt_passed", Short.class);
        variableMap.put("air_temp2_mat_passed", Short.class);
        variableMap.put("air_temp2_nst_passed", Short.class);
        variableMap.put("air_temp2_nvt_passed", Short.class);
        variableMap.put("air_temp2_persist_passed", Short.class);
        variableMap.put("air_temp2_range_qc_passed", Short.class);
        variableMap.put("air_temp2_sdt_passed", Short.class);
        variableMap.put("air_temp2_spatial_barnes_passed", Short.class);
        variableMap.put("air_temp2_spatial_iqr_passed", Short.class);
        variableMap.put("air_temp2_step_passed", Short.class);

        variableMap.put("bar_press_cat_passed", Short.class);
        variableMap.put("bar_press_crt_passed", Short.class);
        variableMap.put("bar_press_mat_passed", Short.class);
        variableMap.put("bar_press_nst_passed", Short.class);
        variableMap.put("bar_press_nvt_passed", Short.class);
        variableMap.put("bar_press_persist_passed", Short.class);
        variableMap.put("bar_press_range_qc_passed", Short.class);
        variableMap.put("bar_press_sdt_passed", Short.class);
        variableMap.put("bar_press_spatial_barnes_passed", Short.class);
        variableMap.put("bar_press_spatial_iqr_passed", Short.class);
        variableMap.put("bar_press_step_passed", Short.class);

        variableMap.put("brake_boost_range_qc_passed", Short.class);
        variableMap.put("brake_status_range_qc_passed", Short.class);
        variableMap.put("cloud_mask", Short.class);
        variableMap.put("dew_temp_crt_passed", Short.class);
        variableMap.put("dew_temp_nst_passed", Short.class);
        variableMap.put("dew_temp_qc_passed", Short.class);
        variableMap.put("dew_temp_range_qc_passed", Short.class);
        variableMap.put("heading_range_qc_passed", Short.class);
        variableMap.put("hoz_accel_lat_range_qc_passed", Short.class);
        variableMap.put("hoz_accel_long_range_qc_passed", Short.class);
        variableMap.put("latitude_dft_passed", Short.class);
        variableMap.put("longitude_dft_passed", Short.class);
        variableMap.put("lights_range_qc_passed", Short.class);

        variableMap.put("model_air_temp", Float.class);
        variableMap.put("model_bar_press", Float.class);
        variableMap.put("nss_air_temp_mean", Float.class);
        variableMap.put("nss_bar_press_mean", Float.class);
        variableMap.put("nss_dew_temp_mean", Float.class);
        variableMap.put("nss_hourly_precip_mean", Float.class);
        variableMap.put("nss_prevail_vis_mean", Float.class);
        variableMap.put("nss_wind_dir_mean", Float.class);
        variableMap.put("nss_wind_speed_mean", Float.class);

        // psn is missing

        variableMap.put("radar_cref", Float.class);
        variableMap.put("radar_precip_flag", Short.class);
        variableMap.put("radar_precip_type", Short.class);
        variableMap.put("road_segment_id", Integer.class);

        // source_id is missing

        variableMap.put("speed_range_qc_passed", Short.class);
        variableMap.put("stab_range_qc_passed", Short.class);
        variableMap.put("steering_angle_range_qc_passed", Short.class);
        variableMap.put("steering_rate_range_qc_passed", Short.class);
        variableMap.put("surface_temp_cat_passed", Short.class);
        variableMap.put("surface_temp_crt_passed", Short.class);
        variableMap.put("surface_temp_nvt_passed", Short.class);
        variableMap.put("surface_temp_persist_passed", Short.class);
        variableMap.put("surface_temp_range_qc_passed", Short.class);
        variableMap.put("surface_temp_spatial_barnes_passed", Short.class);
        variableMap.put("surface_temp_spatial_iqr_passed", Short.class);
        variableMap.put("surface_temp_step_passed", Short.class);

        // tire_*** are missing

        variableMap.put("trac_range_qc_passed", Short.class);
        variableMap.put("wiper_status_range_qc_passed", Short.class);
        variableMap.put("yaw_rate_range_qc_passed", Short.class);
    }
}
