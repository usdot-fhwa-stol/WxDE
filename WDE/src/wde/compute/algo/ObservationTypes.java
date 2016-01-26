package wde.compute.algo;

public class ObservationTypes {

    public static class Mapping {

        private final String vdtObsTypeName;
        private final String wdeObsTypeName;
        private Integer wdeObsTypeId = null;

        public Mapping(String vdtObsTypeName, String wdeObsTypeName) {
            this.vdtObsTypeName = vdtObsTypeName;
            this.wdeObsTypeName = wdeObsTypeName;
        }

        public Mapping(String vdtObsTypeName, String wdeObsTypeName, int wdeObsTypeId) {
            this.vdtObsTypeName = vdtObsTypeName;
            this.wdeObsTypeName = wdeObsTypeName;
            this.wdeObsTypeId = wdeObsTypeId;
        }

        public String getVdtObsTypeName() {
            return vdtObsTypeName;
        }

        public String getWdeObsTypeName() {
            return wdeObsTypeName;
        }

        public Integer getWdeObsTypeId() {
            return wdeObsTypeId;
        }

        public boolean hasWdeObsTypeId() {
            return wdeObsTypeId != null ? true : false;
        }
    }

    private ObservationTypes() {
    }

    public static final Mapping ess_relative_Humidity = new Mapping("", "essRelativeHumidity", 581);

    public static final Mapping wde_precip_type = new Mapping("", "wdePrecipitationType", 1000000);
    public static final Mapping wde_precip_intensity = new Mapping("", "wdePrecipitationIntesity", 1000001);
    public static final Mapping wde_pavement_condition = new Mapping("", "wdePavementCondition", 1000002);
    public static final Mapping wde_pavement_slickness = new Mapping("", "wdePavementSlickness", 1000003);
    public static final Mapping wde_visibility = new Mapping("", "wdeVisibility", 1000004);

    public static final Mapping air_temp_iqr25 = new Mapping("air_temp_iqr25", "segAirTemperatureIQR25CAN");
    public static final Mapping air_temp_iqr75 = new Mapping("air_temp_iqr75", "segAirTemperatureIQR75CAN");
    public static final Mapping air_temp_max = new Mapping("air_temp_max", "segAirTemperatureMaxCAN");
    public static final Mapping air_temp_mean = new Mapping("air_temp_mean", "segAirTemperatureAvgCAN");
    public static final Mapping air_temp_median = new Mapping("air_temp_median", "segAirTemperatureMedCAN");
    public static final Mapping air_temp_min = new Mapping("air_temp_min", "segAirTemperatureMinCAN");
    public static final Mapping air_temp_stdev = new Mapping("air_temp_stdev", "segAirTemperatureStdDevCAN");
    public static final Mapping air_temp_var = new Mapping("air_temp_var", "segAirTemperatureVarCAN");
    public static final Mapping air_temp2_iqr25 = new Mapping("air_temp2_iqr25", "segAirTemperatureIQR25AvgESS");
    public static final Mapping air_temp2_iqr75 = new Mapping("air_temp2_iqr75", "segAirTemperatureIQR75AvgESS");
    public static final Mapping air_temp2_max = new Mapping("air_temp2_max", "segAirTemperatureMaxESS");
    public static final Mapping air_temp2_mean = new Mapping("air_temp2_mean", "segAirTemperatureAvgESS");
    public static final Mapping air_temp2_median = new Mapping("air_temp2_median", "segAirTemperatureMedESS");
    public static final Mapping air_temp2_min = new Mapping("air_temp2_min", "segAirTemperatureMinESS");
    public static final Mapping air_temp2_stdev = new Mapping("air_temp2_stdev", "segAirTemperatureStdDevESS");
    public static final Mapping air_temp2_var = new Mapping("air_temp2_var", "segAirTemperatureVarESS");
    public static final Mapping bar_press_iqr25 = new Mapping("bar_press_iqr25", "segAtmosphericPressureIQR25CAN");
    public static final Mapping bar_press_iqr75 = new Mapping("bar_press_iqr75", "segAtmosphericPressureIQR75CAN");
    public static final Mapping bar_press_max = new Mapping("bar_press_max", "segAtmosphericPressureMaxCAN");
    public static final Mapping bar_press_mean = new Mapping("bar_press_mean", "segAtmosphericPressureAvgCAN");
    public static final Mapping bar_press_median = new Mapping("bar_press_median", "segAtmosphericPressureMedCAN");
    public static final Mapping bar_press_min = new Mapping("bar_press_min", "segAtmosphericPressureMinCAN");
    public static final Mapping bar_press_stdev = new Mapping("bar_press_stdev", "segAtmosphericPressureStdDevCAN");
    public static final Mapping bar_press_var = new Mapping("bar_press_var", "segAtmosphericPressureVarCAN");
    public static final Mapping dew_temp_iqr25 = new Mapping("dew_temp_iqr25", "segDewpointTempIQR25ESS");
    public static final Mapping dew_temp_iqr75 = new Mapping("dew_temp_iqr75", "segDewpointTempIQR75ESS");
    public static final Mapping dew_temp_max = new Mapping("dew_temp_max", "segDewpointTempMaxESS");
    public static final Mapping dew_temp_mean = new Mapping("dew_temp_mean", "segDewpointTempAvgESS");
    public static final Mapping dew_temp_median = new Mapping("dew_temp_median", "segDewpointTempMedESS");
    public static final Mapping dew_temp_min = new Mapping("dew_temp_min", "segDewpointTempMinESS");
    public static final Mapping dew_temp_stdev = new Mapping("dew_temp_stdev", "segDewpointTempStdDevESS ");
    public static final Mapping dew_temp_var = new Mapping("dew_temp_var", "segDewpointTempVarESS");
    public static final Mapping heading_iqr25 = new Mapping("heading_iqr25", "segHeadingIQR25");
    public static final Mapping heading_iqr75 = new Mapping("heading_iqr75", "segHeadingIQR75");
    public static final Mapping heading_max = new Mapping("heading_max", "segHeadingMax");
    public static final Mapping heading_mean = new Mapping("heading_mean", "segHeadingAvg");
    public static final Mapping heading_median = new Mapping("heading_median", "segHeadingMed");
    public static final Mapping heading_min = new Mapping("heading_min", "segHeadingMin");
    public static final Mapping heading_stdev = new Mapping("heading_stdev", "segHeadingStdDev");
    public static final Mapping heading_var = new Mapping("heading_var", "segHeadingVar");
    public static final Mapping hoz_accel_lat_iqr25 = new Mapping("hoz_accel_lat_iqr25", "segLatAccelerationIQR25");
    public static final Mapping hoz_accel_lat_iqr75 = new Mapping("hoz_accel_lat_iqr75", "segLatAccelerationIQR75");
    public static final Mapping hoz_accel_lat_max = new Mapping("hoz_accel_lat_max", "segLatAccelerationMax");
    public static final Mapping hoz_accel_lat_mean = new Mapping("hoz_accel_lat_mean", "segLatAccelerationAvg");
    public static final Mapping hoz_accel_lat_median = new Mapping("hoz_accel_lat_median", "segLatAccelerationMed");
    public static final Mapping hoz_accel_lat_min = new Mapping("hoz_accel_lat_min", "segLatAccelerationMin");
    public static final Mapping hoz_accel_lat_stdev = new Mapping("hoz_accel_lat_stdev", "segLatAccelerationStdDev");
    public static final Mapping hoz_accel_lat_var = new Mapping("hoz_accel_lat_var", "segLatAccelerationVar");
    public static final Mapping hoz_accel_lon_iqr25 = new Mapping("hoz_accel_lon_iqr25", "segLongAccelerationIQR25");
    public static final Mapping hoz_accel_lon_iqr75 = new Mapping("hoz_accel_lon_iqr75", "segLongAccelerationIQR75");
    public static final Mapping hoz_accel_lon_max = new Mapping("hoz_accel_lon_max", "segLongAccelerationMax");
    public static final Mapping hoz_accel_lon_mean = new Mapping("hoz_accel_lon_mean", "segLongAccelerationAvg");
    public static final Mapping hoz_accel_lon_median = new Mapping("hoz_accel_lon_median", "segLongAccelerationMed");
    public static final Mapping hoz_accel_lon_min = new Mapping("hoz_accel_lon_min", "segLongAccelerationMin");
    public static final Mapping hoz_accel_lon_stdev = new Mapping("hoz_accel_lon_stdev", "segLongAccelerationStdDev");
    public static final Mapping hoz_accel_lon_var = new Mapping("hoz_accel_lon_var", "segLongAccelerationVar");

    // Need to create corresponding types
    public static final Mapping model_air_temp = new Mapping("model_air_temp", "segAirTemperatureModel");
    public static final Mapping model_bar_press = new Mapping("model_bar_press", "segAtmosphericPressureModel");
    public static final Mapping nss_air_temp_mean = new Mapping("nss_air_temp_mean", "segAirTemperatureAvgNearbyESS");
    public static final Mapping nss_bar_press_mean = new Mapping("nss_bar_press_mean", "segAtmosphericPressureAvgNearbyESS");
    public static final Mapping nss_dew_temp_mean = new Mapping("nss_dew_temp_mean", "segDewpointTempAvgNearbyESS");
    public static final Mapping nss_hourly_precip_mean = new Mapping("nss_hourly_precip_mean", "segPrecipitationOneHourAvgNearbyESS");
    public static final Mapping nss_prevail_vis_mean = new Mapping("nss_prevail_vis_mean", "segVisibilityAvgNearbyESS");
    public static final Mapping nss_wind_dir_mean = new Mapping("nss_wind_dir_mean", "segWindSensorAvgDirectionNearbyESS");
    public static final Mapping nss_wind_speed_mean = new Mapping("nss_wind_speed_mean", "segWindSensorAvgSpeedNearbyESS");

    public static final Mapping num_abs_engaged = new Mapping("num_abs_engaged", "segAntiLockBrakeStatusEngagedNum");
    public static final Mapping num_abs_not_equipped = new Mapping("num_abs_not_equipped", "segAntiLockBrakeStatusUnavailableNum");
    public static final Mapping num_abs_off = new Mapping("num_abs_off", "segAntiLockBrakeStatusOffNum");
    public static final Mapping num_abs_on = new Mapping("num_abs_on", "segAntiLockBrakeStatusOnNum");
    public static final Mapping num_brakes_all_off = new Mapping("num_brakes_all_off", "segBrakeAppliedStatusOffNum");
    public static final Mapping num_brakes_all_on = new Mapping("num_brakes_all_on", "segBrakeAppliedStatusOnNum");
    public static final Mapping num_brakes_boost_not_equipped = new Mapping("num_brakes_boost_not_equipped", "segBrakeBoostAppliedUnavailableNum");
    public static final Mapping num_brakes_boost_off = new Mapping("num_brakes_boost_off", "segBrakeBoostAppliedOffNum");
    public static final Mapping num_brakes_boost_on = new Mapping("num_brakes_boost_on", "segBrakeBoostAppliedOnNum");
    public static final Mapping num_brakes_lf_active = new Mapping("num_brakes_lf_active", "segBrakeAppliedStatusLeftFrontNum");
    public static final Mapping num_brakes_lr_active = new Mapping("num_brakes_lr_active", "segBrakeAppliedStatusLeftRearNum");
    public static final Mapping num_brakes_rf_active = new Mapping("num_brakes_rf_active", "segBrakeAppliedStatusRightFrontNum");
    public static final Mapping num_brakes_rr_active = new Mapping("num_brakes_rr_active", "segBrakeAppliedStatusRightRearNum");
    public static final Mapping num_lights_automatic_control = new Mapping("num_lights_automatic_control", "segAutomaticLightControlOnNum");
    public static final Mapping num_lights_drl = new Mapping("num_lights_drl", "segDaytimeRunningLightsOnNum");
    public static final Mapping num_lights_fog = new Mapping("num_lights_fog", "segFogLightOnNum");
    public static final Mapping num_lights_hazard = new Mapping("num_lights_hazard", "segHazardSignalOnNum");
    public static final Mapping num_lights_high_beam = new Mapping("num_lights_high_beam", "segHighBeamLightsOnNum");
    public static final Mapping num_lights_left_turn = new Mapping("num_lights_left_turn", "segLeftTurnSignalOnNum");
    public static final Mapping num_lights_low_beam = new Mapping("num_lights_low_beam", "segLowBeamLightsOnNum");
    public static final Mapping num_lights_off = new Mapping("num_lights_off", "segAllLightsOffNum");
    public static final Mapping num_lights_parking = new Mapping("num_lights_parking", "segParkingLightsOnNum");
    public static final Mapping num_lights_right_turn = new Mapping("num_lights_right_turn", "segRightTurnSignalOnNum");
    public static final Mapping num_msg_valid_abs = new Mapping("num_msg_valid_abs", "segValidAntiLockBrakeNum");
    public static final Mapping num_msg_valid_air_temp = new Mapping("num_msg_valid_air_temp", "segValidAirTemperatureCANNum");
    public static final Mapping num_msg_valid_air_temp2 = new Mapping("num_msg_valid_air_temp2", "segValidAirTemperatureESSNum");
    public static final Mapping num_msg_valid_bar_press = new Mapping("num_msg_valid_bar_press", "segValidAtmosphericPressureCANNum");
    public static final Mapping num_msg_valid_brakes = new Mapping("num_msg_valid_brakes", "segValidBrakeAppliedStatusNum");
    public static final Mapping num_msg_valid_brakes_boost = new Mapping("num_msg_valid_brakes_boost", "segValidBrakeBoostAppliedNum");
    public static final Mapping num_msg_valid_dew_temp = new Mapping("num_msg_valid_dew_temp", "segValidDewpointTempNum");
    public static final Mapping num_msg_valid_heading = new Mapping("num_msg_valid_heading", "segValidHeadingNum");
    public static final Mapping num_msg_valid_hoz_accel_lat = new Mapping("num_msg_valid_hoz_accel_lat", "segValidLatAccelerationNum");
    public static final Mapping num_msg_valid_hoz_accel_lon = new Mapping("num_msg_valid_hoz_accel_lon", "segValidLongAccelerationNum");
    public static final Mapping num_msg_valid_lights = new Mapping("num_msg_valid_lights", "segValidLightsNum");
    public static final Mapping num_msg_valid_speed = new Mapping("num_msg_valid_speed", "segValidSpeedNum");
    public static final Mapping num_msg_valid_stab = new Mapping("num_msg_valid_stab", "segValidStabilityControlStatusNum");
    public static final Mapping num_msg_valid_steering_angle = new Mapping("num_msg_valid_steering_angle", "segValidSteeringWheelAngleNum");
    public static final Mapping num_msg_valid_steering_rate = new Mapping("num_msg_valid_steering_rate", "segValidSteeringWheelAngleRateOfChangeNum");
    public static final Mapping num_msg_valid_surface_temp = new Mapping("num_msg_valid_surface_temp", "segValidSurfaceTemperatureNum");
    public static final Mapping num_msg_valid_trac = new Mapping("num_msg_valid_trac", "segValidTractionControlStateNum");
    public static final Mapping num_msg_valid_wipers = new Mapping("num_msg_valid_wipers", "segValidWiperStatusNum");
    public static final Mapping num_msg_valid_yaw = new Mapping("num_msg_valid_yaw", "segValidYawNum");
    public static final Mapping num_stab_engaged = new Mapping("num_stab_engaged", "segStabilityControlStatusEngagedNum");
    public static final Mapping num_stab_not_equipped = new Mapping("num_stab_not_equipped", "segStabilityControlStatusUnavailableNum");
    public static final Mapping num_stab_off = new Mapping("num_stab_off", "segStabilityControlStatusOffNum");
    public static final Mapping num_stab_on = new Mapping("num_stab_on", "segStabilityControlStatusOnNum");
    public static final Mapping num_trac_engaged = new Mapping("num_trac_engaged", "segTractionControlStateEngagedNum");
    public static final Mapping num_trac_not_equipped = new Mapping("num_trac_not_equipped", "segTractionControlStateUnavailableNum");
    public static final Mapping num_trac_off = new Mapping("num_trac_off", "segTractionControlStateOffNum");
    public static final Mapping num_trac_on = new Mapping("num_trac_on", "segTractionControlStateOnNum");
    public static final Mapping num_wipers_automatic = new Mapping("num_wipers_automatic", "segWiperStatusAutomaticPresentNum");
    public static final Mapping num_wipers_high = new Mapping("num_wipers_high", "segWiperStatusHighNum");
    public static final Mapping num_wipers_intermittent = new Mapping("num_wipers_intermittent", "segWiperStatusIntermittentNum");
    public static final Mapping num_wipers_low = new Mapping("num_wipers_low", "segWiperStatusLowNum");
    public static final Mapping num_wipers_not_equipped = new Mapping("num_wipers_not_equipped", "segWiperStatusUnavailableNum");
    public static final Mapping num_wipers_off = new Mapping("num_wipers_off", "segWiperStatusOffNum");
    public static final Mapping num_wipers_washer = new Mapping("num_wipers_washer", "segWiperStatusWasherInUserNum");
    public static final Mapping radar_cref = new Mapping("radar_cref", "radar_cref");
    public static final Mapping radar_precip_flag = new Mapping("radar_precip_flag", "radar_precip_flag");
    public static final Mapping radar_precip_type = new Mapping("radar_precip_type", "radar_precip_type");
    public static final Mapping speed_iqr25 = new Mapping("speed_iqr25", "segSpeedIQR25CAN");
    public static final Mapping speed_iqr75 = new Mapping("speed_iqr75", "segSpeedIQR75CAN");
    public static final Mapping speed_max = new Mapping("speed_max", "segSpeedMaxCAN");
    public static final Mapping speed_mean = new Mapping("speed_mean", "segSpeedAvgCAN");
    public static final Mapping speed_median = new Mapping("speed_median", "segSpeedMedCAN");
    public static final Mapping speed_min = new Mapping("speed_min", "segSpeedMinCAN");
    public static final Mapping speed_ratio = new Mapping("speed_ratio", "segSpeedRatioCAN");
    public static final Mapping speed_stdev = new Mapping("speed_stdev", "segSpeedStdDevCAN");
    public static final Mapping speed_var = new Mapping("speed_var", "segSpeedVarCAN");
    public static final Mapping steering_angle_iqr25 = new Mapping("steering_angle_iqr25", "segSteeringWheelAngleIQR25");
    public static final Mapping steering_angle_iqr75 = new Mapping("steering_angle_iqr75", "segSteeringWheelAngleIQR75");
    public static final Mapping steering_angle_max = new Mapping("steering_angle_max", "segSteeringWheelAngleMax");
    public static final Mapping steering_angle_mean = new Mapping("steering_angle_mean", "segSteeringWheelAngleAvg");
    public static final Mapping steering_angle_median = new Mapping("steering_angle_median", "segSteeringWheelAngleMed");
    public static final Mapping steering_angle_min = new Mapping("steering_angle_min", "segSteeringWheelAngleMin");
    public static final Mapping steering_angle_stdev = new Mapping("steering_angle_stdev", "segSteeringWheelAngleStdDev");
    public static final Mapping steering_angle_var = new Mapping("steering_angle_var", "segSteeringWheelAngleVar");
    public static final Mapping steering_rate_max = new Mapping("steering_rate_max", "segSteeringWheelAngleRateOfChangeMax");
    public static final Mapping steering_rate_mean = new Mapping("steering_rate_mean", "segSteeringWheelAngleRateOfChangeAvg");
    public static final Mapping steering_rate_median = new Mapping("steering_rate_median", "segSteeringWheelAngleRateOfChangeMed");
    public static final Mapping steering_rate_min = new Mapping("steering_rate_min", "segSteeringWheelAngleRateOfChangeMin");
    public static final Mapping steering_rate_stdev = new Mapping("steering_rate_stdev", "segSteeringWheelAngleRateOfChangeStdDev");
    public static final Mapping steering_rate_var = new Mapping("steering_rate_var", "segSteeringWheelAngleRateOfChangeVar");
    public static final Mapping surface_temp_iqr25 = new Mapping("surface_temp_iqr25", "segSurfaceTemperatureIQR25ESS");
    public static final Mapping surface_temp_iqr75 = new Mapping("surface_temp_iqr75", "segSurfaceTemperatureIQR75ESS");
    public static final Mapping surface_temp_max = new Mapping("surface_temp_max", "segSurfaceTemperatureMaxESS");
    public static final Mapping surface_temp_mean = new Mapping("surface_temp_mean", "segSurfaceTemperatureAvgESS");
    public static final Mapping surface_temp_median = new Mapping("surface_temp_median", "segSurfaceTemperatureMedESS");
    public static final Mapping surface_temp_min = new Mapping("surface_temp_min", "segSurfaceTemperatureMinESS");
    public static final Mapping surface_temp_stdev = new Mapping("surface_temp_stdev", "segSurfaceTemperatureStdDevESS");
    public static final Mapping surface_temp_var = new Mapping("surface_temp_var", "segSurfaceTemperatureVarESS");
    public static final Mapping total_num_msg = new Mapping("total_num_msg", "segTotalNum");
    public static final Mapping yaw_iqr25 = new Mapping("yaw_iqr25", "segYawIQR25");
    public static final Mapping yaw_iqr75 = new Mapping("yaw_iqr75", "segYawIQR75");
    public static final Mapping yaw_max = new Mapping("yaw_max", "segYawMax");
    public static final Mapping yaw_mean = new Mapping("yaw_mean", "segYawAvg");
    public static final Mapping yaw_median = new Mapping("yaw_median", "segYawMed");
    public static final Mapping yaw_min = new Mapping("yaw_min", "segYawMin");
    public static final Mapping yaw_stdev = new Mapping("yaw_stdev", "segYawStdDev");
    public static final Mapping yaw_var = new Mapping("yaw_var", "segYawVar");
    public static final Mapping all_hazards = new Mapping("all_hazards", "segAllHazards");
    public static final Mapping pavement_condition = new Mapping("pavement_condition", "segSurfaceStatus");
    public static final Mapping precipitation = new Mapping("precipitation", "segPrecipType");
    public static final Mapping visibility = new Mapping("visibility", "segVisibility");

}
