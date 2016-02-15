package wde.compute.algo;

public class ObservationTypes {

    public static final Mapping ess_relative_humidity = new Mapping("essRelativeHumidity");
    public static final Mapping air_temp_mean = new Mapping("essAirTemperature", "canAirTemperature");

    public static final Mapping air_temp2_mean = new Mapping("segAirTemperatureAvgESS");
    public static final Mapping dew_temp_mean = new Mapping("segDewpointTempAvgESS");
    public static final Mapping model_air_temp = new Mapping("segAirTemperatureModel");
    public static final Mapping nss_air_temp_mean = new Mapping("segAirTemperatureAvgNearbyESS");
    public static final Mapping nss_dew_temp_mean = new Mapping("segDewpointTempAvgNearbyESS");
    public static final Mapping nss_prevail_vis_mean = new Mapping("segVisibilityAvgNearbyESS");
    public static final Mapping nss_wind_speed_mean = new Mapping("segWindSensorAvgSpeedNearbyESS");
    public static final Mapping num_abs_engaged = new Mapping("segAntiLockBrakeStatusEngagedNum");
    public static final Mapping num_abs_off = new Mapping("segAntiLockBrakeStatusOffNum");
    public static final Mapping num_abs_on = new Mapping("segAntiLockBrakeStatusOnNum");
    public static final Mapping num_lights_fog = new Mapping("segFogLightOnNum");
    public static final Mapping num_lights_high_beam = new Mapping("segHighBeamLightsOnNum");
    public static final Mapping num_lights_off = new Mapping("segAllLightsOffNum");
    public static final Mapping num_msg_valid_abs = new Mapping("segValidAntiLockBrakeNum");
    public static final Mapping num_msg_valid_lights = new Mapping("segValidLightsNum");
    public static final Mapping num_msg_valid_stab = new Mapping("segValidStabilityControlStatusNum");
    public static final Mapping num_msg_valid_trac = new Mapping("segValidTractionControlStateNum");
    public static final Mapping num_stab_engaged = new Mapping("segStabilityControlStatusEngagedNum");
    public static final Mapping num_stab_off = new Mapping("segStabilityControlStatusOffNum");
    public static final Mapping num_stab_on = new Mapping("segStabilityControlStatusOnNum");
    public static final Mapping num_trac_engaged = new Mapping("segTractionControlStateEngagedNum");
    public static final Mapping num_trac_off = new Mapping("segTractionControlStateOffNum");
    public static final Mapping num_trac_on = new Mapping("segTractionControlStateOnNum");
    public static final Mapping num_wipers_automatic = new Mapping("segWiperStatusAutomaticPresentNum");
    public static final Mapping num_wipers_high = new Mapping("segWiperStatusHighNum");
    public static final Mapping num_wipers_intermittent = new Mapping("segWiperStatusIntermittentNum");
    public static final Mapping num_wipers_low = new Mapping("segWiperStatusLowNum");
    public static final Mapping num_wipers_off = new Mapping("segWiperStatusOffNum");
    public static final Mapping speed_ratio = new Mapping("segSpeedRatioCAN");
    public static final Mapping steering_angle_mean = new Mapping("segSteeringWheelAngleAvg");
    public static final Mapping yaw_iqr25 = new Mapping("segYawIQR25");
    public static final Mapping yaw_iqr75 = new Mapping("segYawIQR75");
    public static final Mapping yaw_max = new Mapping("segYawMax");
    public static final Mapping yaw_mean = new Mapping("segYawAvg");
    public static final Mapping yaw_median = new Mapping("segYawMed");
    public static final Mapping yaw_min = new Mapping("segYawMin");
    public static final Mapping visibility = new Mapping("segVisibility");

    private ObservationTypes() {
    }

    public static class Mapping {

        private final String[] names;
        private Integer id = null;

        public Mapping(String... names) {
            this.names = names;
        }

        public Mapping(int id, String name) {
            this.id = id;
            this.names = new String[] { name };
        }

        public String[] getNames() {
            return names;
        }

        public Integer getId() {
            return id;
        }
    }
}
