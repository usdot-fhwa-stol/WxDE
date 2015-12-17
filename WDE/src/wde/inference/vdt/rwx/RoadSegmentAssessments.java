package wde.inference.vdt.rwx;

import wde.inference.ObservationResolver;
import wde.inference.vdt.ValueHolder;
import wde.inference.vdt.VdtObservationResolver;
import wde.obs.IObs;

public class RoadSegmentAssessments implements ObservationResolver {

    public static final int FILL_VALUE = -9999;

    // Forecast precip-type
    public final int FCST_NONE = 0;
    public final int FCST_RAIN = 1;
    public final int FCST_SNOW = 2;
    public final int FCST_ICE = 5;

    // Forecast precip-intensity thresholds
    public final float FCST_LIGHT_PRECIP_WINTER = 0.254f; // <= 0.254 mm. (0.01 in.)
    public final float FCST_MODERATE_PRECIP_WINTER = 2.54f; // <= 2.54 mm. (0.10 in.)
    // FCST_HEAVY_PRECIP_WINTER is: > 2.54 mm (0.10 in.)
    public final float FCST_LIGHT_PRECIP_SUMMER = 2.54f; // <= 2.54 mm. (0.10 in.)
    public final float FCST_MODERATE_PRECIP_SUMMER = 7.62f; // <= 7.62 mm. (0.30 in.)
    // FCST_HEAVY_PRECIP_SUMMER is: > 7.62 mm (0.30 in.)

    // VDT radar reflectivity precip-intensity thresholds 
    public final int DEF_RADAR_LIGHT_PRECIP = 15; // <= 15 reflectivity (dBZ)
    public final int DEF_RADAR_MODERATE_PRECIP = 30; // <= 30 reflectivity (dBZ)
    // DEF_RADAR_HEAVY_PRECIP is: > 30 reflectivity (dBZ)
    public final int WINTER_RADAR_LIGHT_PRECIP = 10; // <= 10 reflectivity (dBZ)
    public final int WINTER_RADAR_MODERATE_PRECIP = 20; // <= 20 reflectivity (dBZ)
    // WINTER_RADAR_HEAVY_PRECIP is: > 20 reflectivity (dBZ)
    public final int SUMMER_RADAR_LIGHT_PRECIP = 20; // <= 20 reflectivity (dBZ)
    public final int SUMMER_RADAR_MODERATE_PRECIP = 40; // <= 40 reflectivity (dBZ)
    // SUMMER_RADAR_HEAVY_PRECIP is: > 40 reflectivity (dBZ)

    // Wind-speed threshold for blowing snow (m/s)
    public final int BLOWING_SNOW_WIND_SPEED = 10; // 10 m/s

    // Precip Type enumeration
    public final int NO_PRECIP = 0;
    public final int RAIN = 1;
    public final int MIX = 2;
    public final int SNOW = 3;

    // Precip Intensity enumeration
    //public final int NO_PRECIP = 0;  // defined above
    public final int LIGHT_PRECIP = 1;
    public final int MODERATE_PRECIP = 2;
    public final int HEAVY_PRECIP = 3;
    public final int ROAD_SPLASH = 4;

    // Pavement Condition enumeration
    public final int DRY_PAVEMENT = 0;
    public final int WET_PAVEMENT = 1;
    public final int SNOW_COVERED = 2;
    public final int ICE_COVERED = 3;
    public final int HYDROPLANE = 4;
    public final int BLACK_ICE = 5;
    public final int DRY_WET_PAVEMENT = 6;
    public final int DRY_FROZEN_PAVEMENT = 7;

    // Visibility enumeration 
    public final int VIS_NORMAL = 0;
    public final int VIS_LOW = 1;
    public final int VIS_HEAVY_RAIN = 2;
    public final int VIS_HEAVY_SNOW = 3;
    public final int VIS_BLOWING_SNOW = 4;
    public final int VIS_FOG = 5;
    public final int VIS_HAZE = 6;
    public final int VIS_DUST = 7;
    public final int VIS_SMOKE = 8;

    // Confidence field weights
    public final float fcst_wgt = 1.0f;
    public final float fcst_input_conf = 1.0f;

    public final float air_temp_wgt = 0.50f;

    public final float radar_ref_wgt = 0.50f;
    public final float wipers_wgt = 0.30f;
    public final float speed_ratio_wgt = 0.10f;
    public final float lights_wgt = 0.10f;

    public final float road_temp_wgt = 0.50f;
    public final float precip_type_wgt = 0.20f;
    public final float precip_intensity_wgt = 0.30f;

    public final float fog_wgt = 0.50f;
    public final float gen_vis_wgt = 0.30f;

//    /**
//     * @brief precipitation type
//     */
//    int precip_type;
//
//    /**
//     * @brief precipitation type confidence
//     */
//    float precip_type_confidence;
//
//    /**
//     * @brief precipitation intensity
//     */
//    int precip_intensity;
//
//    /**
//     * @brief precipitation intensity confidence
//     */
//    float precip_intensity_confidence;
//
//    /**
//     * @brief pavement condition
//     */
//    int pavement_condition;
//
//    /**
//     * @brief pavement condition confidence
//     */
//    float pavement_condition_confidence;
//
//    /**
//     * @brief pavement slickness
//     */
//    boolean pavement_slickness;
//
//    /**
//     * @brief visibility
//     */
//    int visibility;
//
//    /**
//     * @brief visibility confidence
//     */
//    float visibility_confidence;
//
//    /**
//     * @brief road_segment_id
//     */
//    int road_segment_id;

    VdtObservationResolver resolver;

    public RoadSegmentAssessments(VdtObservationResolver resolver) {
//        precip_type = FILL_VALUE;
//        pavement_condition = FILL_VALUE;
//        pavement_slickness = false; //FILL_VALUE;
//        visibility = FILL_VALUE;

        this.resolver = resolver;
    }

//    public void perform_assessment(vdt_probe_message_qc_statistics this.resolver)
//    {
//        road_segment_id = this.resolver.id;
//        precip_type = precip_type_assessment(this.resolver, precip_type_confidence);
//        precip_intensity = precip_intensity_assessment(this.resolver, precip_type, precip_intensity_confidence);
//        pavement_condition = pavement_condition_assessment(this.resolver, precip_type, precip_type_confidence, precip_intensity, precip_intensity_confidence, pavement_condition_confidence);
//        visibility = visibility_assessment(this.resolver, precip_type, precip_type_confidence, precip_intensity, precip_intensity_confidence, visibility_confidence);
//        pavement_slickness = pavement_slickness_assessment(this.resolver, precip_type, precip_intensity, pavement_condition);
//
//        // Check for dry pavement slickness if weather related pavement slickness is false
//        // Dry pavement slickness is based on abs, trac and stab
//        if(!pavement_slickness)
//            pavement_slickness = dry_pavement_slickness_assessment(this.resolver);
//
//    }


    public int precip_type_assessment(ValueHolder<Float> confidenceVal) {

        if (confidenceVal == null)
            confidenceVal = new ValueHolder<>(0f);

        int precip_type = FILL_VALUE;
        float confidence = 0f;

        // Get forecast fields
        float fcst_precip_type = this.resolver.getFcst_precip_type();
        float fcst_air_temp = this.resolver.getFcst_air_temp();

        // Forecast case
        if (fcst_precip_type != FILL_VALUE && fcst_air_temp != FILL_VALUE) {
            if (fcst_precip_type == FCST_NONE)
                precip_type = NO_PRECIP;
            else if (fcst_precip_type == FCST_RAIN) {
                if (fcst_air_temp < 1.5)
                    precip_type = MIX;
                else
                    precip_type = RAIN;
            } else if (fcst_precip_type == FCST_SNOW) {
                if (fcst_air_temp > 1.5)
                    precip_type = MIX;
                else
                    precip_type = SNOW;
            } else if (fcst_precip_type == FCST_ICE) {
                if (fcst_air_temp < -2)
                    precip_type = SNOW;
                else if (fcst_air_temp > 2)
                    precip_type = RAIN;
                else
                    precip_type = MIX;
            }

            confidence = confidence + (fcst_input_conf * fcst_wgt);
            confidenceVal.setValue(confidence);

            return precip_type;
        }

        // Determine what VDT air temp we should use
        // for vdt precip-type assessment
        //
        float mobile_air_temp = get_air_temp();
        float env_air_temp = get_env_air_temp();
        float air_temp = determine_temp(mobile_air_temp, env_air_temp);
        //printf("air_temp: %f\n", air_temp);

        // Hardwire the input field confidence for now
        // eventually this will be coming from the seg-stats file
        float air_temp_input_conf = 1.0f;

        // Do we want to use radar_precip_type first?

        // VDT case
        if (air_temp != FILL_VALUE) // vdt case
        {
            if (air_temp > 2)
                precip_type = RAIN;
            else if (air_temp < -2)
                precip_type = SNOW;
            else
                precip_type = MIX;

            confidence = confidence + (air_temp_input_conf * air_temp_wgt);
        }

        confidenceVal.setValue(confidence);

        return precip_type;
    }


    public int precip_intensity_assessment(int precip_type, ValueHolder<Float> confidenceVal) {

        if (confidenceVal == null)
            confidenceVal = new ValueHolder<>(0f);

        int precip_intensity = FILL_VALUE;
        float confidence = 0f;

        // Get forecast fields
        float fcst_precip_rate = this.resolver.getFcst_precip_rate();

        // Forecast case
        if (fcst_precip_rate != FILL_VALUE) {
            if (fcst_precip_rate <= 0)
                precip_intensity = NO_PRECIP;
            else if (fcst_precip_rate > 0 && fcst_precip_rate <= FCST_LIGHT_PRECIP_WINTER)
                precip_intensity = LIGHT_PRECIP;
            else if (fcst_precip_rate > FCST_LIGHT_PRECIP_WINTER && fcst_precip_rate <= FCST_MODERATE_PRECIP_WINTER)
                precip_intensity = MODERATE_PRECIP;
            else
                precip_intensity = HEAVY_PRECIP;

            confidence = confidence + (fcst_input_conf * fcst_wgt);
            confidenceVal.setValue(confidence);

            return precip_intensity;
        }

        // Get VDT fields
        float radar_ref = this.resolver.getRadarRef();
        float speed_ratio = this.resolver.getSpeed_ratio();

        int num_msg_valid_lights = this.resolver.getNum_msg_valid_lights();
        int num_lights_off = this.resolver.getNum_lights_off();
        float percent_lights_off = calc_percentage((float) num_lights_off, (float) num_msg_valid_lights);

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: num_msg_valid_lights: %d, num_lights_off: %d, percent_lights_off: %f\n",  num_msg_valid_lights, num_lights_off, percent_lights_off);

        // Determine if we have any wiper data
        int wipers_flag = 0;
        if (this.resolver.getNum_wipers_off() != FILL_VALUE ||
                this.resolver.getNum_wipers_intermittent() != FILL_VALUE ||
                this.resolver.getNum_wipers_low() != FILL_VALUE ||
                this.resolver.getNum_wipers_high() != FILL_VALUE)
            wipers_flag = 1;

        boolean wipers_off_flag = wipers_off();
        boolean wipers_on_flag = wipers_on();
        boolean wipers_interm_flag = wipers_intermittent();
        boolean wipers_low_flag = wipers_low();
        boolean wipers_high_flag = wipers_high();

        // Hardwire the input field confidence for now
        // eventually this will be coming from the seg-stats file
        float radar_ref_input_conf = 1.0f;
        float wipers_input_conf = 1.0f;
        float speed_ratio_input_conf = 1.0f;
        float lights_input_conf = 1.0f;

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: wipers_flag: %d, wipers_off_flag: %d, wipers_on_flag: %d, wipers_interm_flag: %d, wipers_low_flag: %d, wipers_high_flag: %d\n", wipers_flag, wipers_off_flag, wipers_on_flag,  wipers_interm_flag, wipers_low_flag, wipers_high_flag);

        // Use precip-type to determine radar-composite-reflectivity (dBz) thresholds
        int RADAR_LIGHT_PRECIP;
        int RADAR_MODERATE_PRECIP;
        if (precip_type == MIX || precip_type == SNOW) {
            RADAR_LIGHT_PRECIP = WINTER_RADAR_LIGHT_PRECIP;
            RADAR_MODERATE_PRECIP = WINTER_RADAR_MODERATE_PRECIP;
        } else if (precip_type == RAIN) {
            RADAR_LIGHT_PRECIP = SUMMER_RADAR_LIGHT_PRECIP;
            RADAR_MODERATE_PRECIP = SUMMER_RADAR_MODERATE_PRECIP;
        } else {
            RADAR_LIGHT_PRECIP = DEF_RADAR_LIGHT_PRECIP;
            RADAR_MODERATE_PRECIP = DEF_RADAR_MODERATE_PRECIP;
        }

        // VDT case using just radar-composite-reflectivity
        if (radar_ref != FILL_VALUE) {
            if (radar_ref <= 0)
                precip_intensity = NO_PRECIP;
            else if (radar_ref > 0 && radar_ref <= RADAR_LIGHT_PRECIP)
                precip_intensity = LIGHT_PRECIP;
            else if (radar_ref > RADAR_LIGHT_PRECIP && radar_ref <= RADAR_MODERATE_PRECIP)
                precip_intensity = MODERATE_PRECIP;
            else
                precip_intensity = HEAVY_PRECIP;

            confidence = confidence + (radar_ref_input_conf * radar_ref_wgt);
        }

        // VDT case: modify intensity based on wipers or determine intensity if we just have wipers and no radar data
        if (wipers_flag == 1) {
            if (precip_intensity == FILL_VALUE) // This for the case with no radar data
            {
                if (wipers_off_flag)
                    precip_intensity = NO_PRECIP;
                else if (wipers_interm_flag)
                    precip_intensity = LIGHT_PRECIP;
                else // wipers_low_flag || wipers_high_flag
                    precip_intensity = MODERATE_PRECIP;
            } else if (precip_intensity == NO_PRECIP) {
                if (wipers_on_flag)
                    precip_intensity = ROAD_SPLASH;
            } else if (precip_intensity == LIGHT_PRECIP) {
                if (wipers_low_flag || wipers_high_flag)
                    precip_intensity = MODERATE_PRECIP;
            } else if (precip_intensity == MODERATE_PRECIP) {
                if (wipers_high_flag)
                    precip_intensity = HEAVY_PRECIP;
                else if (wipers_off_flag)
                    precip_intensity = LIGHT_PRECIP;
            } else // precip_intensity == HEAVY_PRECIP
            {
                if (wipers_interm_flag)
                    precip_intensity = MODERATE_PRECIP;
                else if (wipers_off_flag)
                    precip_intensity = LIGHT_PRECIP;
            }
            confidence = confidence + (wipers_input_conf * wipers_wgt);
        }

        // VDT case: modify intensity based on speed-ratio
        if (speed_ratio != FILL_VALUE) {
            if (precip_intensity == HEAVY_PRECIP) {
                if (speed_ratio >= 0.7)
                    precip_intensity = MODERATE_PRECIP;
            }

            confidence = confidence + (speed_ratio_input_conf * speed_ratio_wgt);
        }

        // VDT case: modify intensity based on headlights
        if (percent_lights_off != FILL_VALUE) {
            if (precip_intensity == MODERATE_PRECIP) {
                if (percent_lights_off > 0.50)
                    precip_intensity = LIGHT_PRECIP;
            } else if (precip_intensity == HEAVY_PRECIP) {
                if (percent_lights_off > 0.50)
                    precip_intensity = MODERATE_PRECIP;
            }
            confidence = confidence + (lights_input_conf * lights_wgt);
        }

        confidenceVal.setValue(confidence);

        return precip_intensity;
    }

    public int pavement_condition_assessment(int precip_type, float precip_type_input_conf, int precip_intensity, float precip_intensity_input_conf, ValueHolder<Float> confidenceVal) {

        if (confidenceVal == null)
            confidenceVal = new ValueHolder<>(0f);

        int pavement_condition = FILL_VALUE;
        float confidence = 0f;

        // Get forecast fields
        float fcst_road_temp = this.resolver.getFcst_road_temp();
        float fcst_road_water_phase_type = this.resolver.getFcst_road_water_phase_type(); // Not sure if we can use this as it shows wet-pavement for NO_PRECIP
        //printf("fcst_road_temp: %f, fcst_road_water_phase_type: %f\n", fcst_road_temp, fcst_road_water_phase_type);

        // Forecast case
        if (fcst_road_temp != FILL_VALUE && precip_type != FILL_VALUE && precip_intensity != FILL_VALUE) {
            pavement_condition = get_pavement_condition_basic(precip_type, precip_intensity, fcst_road_temp);
            confidence = confidence + (fcst_input_conf * fcst_wgt);
            confidenceVal.setValue(confidence);

            return pavement_condition;
        }

        // Get VDT fields
        float road_temp = this.resolver.getSurface_temp_mean();
        boolean black_ice_possible = false;
        boolean hydroplane_possible = false;

        // Hardwire the input field confidence for now
        // eventually this will be coming from the seg-stats file
        float road_temp_input_conf = 1.0f;

        // VDT case with road-temp, precip-type and precip-intensity
        if (road_temp != FILL_VALUE && precip_type != FILL_VALUE && precip_intensity != FILL_VALUE) {
            //printf("pavement condition assessment: with road_temp, precip_type and precip_intensity\n");
            //printf("road_temp_input_conf: %f, road_temp_wgt: %f, precip_type_input_conf: %f, precip_type_wgt: %f, precip_intensity_input_conf: %f, precip_intensity_wgt: %f\n", road_temp_input_conf, road_temp_wgt, precip_type_input_conf, precip_type_wgt, precip_intensity_input_conf, precip_intensity_wgt);
            pavement_condition = get_pavement_condition_basic(precip_type, precip_intensity, road_temp);
            if (pavement_condition == DRY_PAVEMENT && road_temp < 1)
                black_ice_possible = true;
            else if (pavement_condition == WET_PAVEMENT)
                hydroplane_possible = true;

            confidence = confidence + (road_temp_input_conf * road_temp_wgt) + (precip_type_input_conf * precip_type_wgt) + (precip_intensity_input_conf * precip_intensity_wgt);
        } else if (precip_type != FILL_VALUE && precip_intensity != FILL_VALUE) // VDT case with just precip-type and precip-intensity
        {
            //printf("pavement condition assessment: with precip_type and precip_intensity\n");
            //printf("precip_type_input_conf: %f, precip_type_wgt: %f, precip_intensity_input_conf: %f, precip_intensity_wgt: %f\n", precip_type_input_conf, precip_type_wgt, precip_intensity_input_conf, precip_intensity_wgt);
            if (precip_intensity == NO_PRECIP)
                pavement_condition = DRY_PAVEMENT;
            else if (precip_type == RAIN) {
                pavement_condition = WET_PAVEMENT;
                hydroplane_possible = true;
            } else if (precip_type == MIX && precip_intensity != ROAD_SPLASH)
                pavement_condition = ICE_COVERED;
            else if (precip_type == MIX && precip_intensity == ROAD_SPLASH) {
                pavement_condition = WET_PAVEMENT;
                hydroplane_possible = true;
            } else
                pavement_condition = SNOW_COVERED;

            confidence = confidence + (precip_type_input_conf * precip_type_wgt) + (precip_intensity_input_conf * precip_intensity_wgt);
        } else if (road_temp != FILL_VALUE) // VDT case with just road-temp
        {
            //printf("pavement condition assessment: with road_temp\n");
            //printf("road_temp_input_conf: %f, road_temp_wgt: %f\n", road_temp_input_conf, road_temp_wgt);
            if (road_temp <= 0) {
                pavement_condition = DRY_FROZEN_PAVEMENT;
                black_ice_possible = true;
            } else {
                pavement_condition = DRY_WET_PAVEMENT;
                hydroplane_possible = true;
            }
            confidence = confidence + (road_temp_input_conf * road_temp_wgt);
        } else if (precip_type != FILL_VALUE) // VDT case with just precip-type
        {
            //printf("pavement condition assessment: with precip-type\n");
            //printf("precip_type_input_conf: %f, precip_type_wgt: %f\n", precip_type_input_conf, precip_type_wgt);
            if (precip_type == RAIN) {
                pavement_condition = DRY_WET_PAVEMENT;
                hydroplane_possible = true;
            } else if (precip_type == MIX || precip_type == SNOW) {
                pavement_condition = DRY_FROZEN_PAVEMENT;
                black_ice_possible = true;
            }
            confidence = confidence + (precip_type_input_conf * precip_type_wgt);
        }

        // Determine if there is black-ice or hydroplane hazard
        boolean pavement_slickness = pavement_slickness_assessment(precip_type, precip_intensity, pavement_condition);
        if (pavement_slickness) {
            if (black_ice_possible)
                pavement_condition = BLACK_ICE;
            else if (hydroplane_possible)
                pavement_condition = HYDROPLANE;
        }

        confidenceVal.setValue(confidence);

        return pavement_condition;
    }


    public int visibility_assessment(int precip_type, float precip_type_input_conf, int precip_intensity, float precip_intensity_input_conf, ValueHolder<Float> confidenceVal) {

        if (confidenceVal == null)
            confidenceVal = new ValueHolder<>(0f);

        int visibility = FILL_VALUE;
        float confidence = 0;

        // Get forecast fields
        float fcst_visibility = this.resolver.getFcst_visibility();
        float fcst_wind_speed = this.resolver.getFcst_wind_speed();
        float fcst_prob_fog = this.resolver.getFcst_prob_fog();

        // Forecast case
        if (fcst_visibility != FILL_VALUE && precip_type != FILL_VALUE && precip_intensity != FILL_VALUE) {
            if (fcst_visibility < 10) // May need to adjust this threshold? Do we even want to use forecast visibility?
            {
                if (precip_type == RAIN && precip_intensity == HEAVY_PRECIP)
                    visibility = VIS_HEAVY_RAIN;
                else if (precip_type == SNOW && precip_intensity != NO_PRECIP) {
                    if (fcst_wind_speed != FILL_VALUE && fcst_wind_speed > BLOWING_SNOW_WIND_SPEED)
                        visibility = VIS_BLOWING_SNOW;
                    else if (precip_intensity == HEAVY_PRECIP)
                        visibility = VIS_HEAVY_SNOW;
                }
                // If the visibility is still missing look for fog
                // NOTE: We currently do not have prob_fog values from logicast rdwx files
                //       so this is a placeholder for now
                // We could use RH to calculate fog?
                if (visibility == FILL_VALUE && fcst_prob_fog != FILL_VALUE && fcst_prob_fog > 0.5)
                    visibility = VIS_FOG;
            } else {
                visibility = VIS_NORMAL;
            }
            confidence = confidence + (fcst_input_conf * fcst_wgt);
            confidenceVal.setValue(confidence);

            return visibility;
        }

        // Determine what VDT air temp and dewpt we should use
        // to calculate rh for fog calculation and generic visibility calculations
        //
        float mobile_air_temp = get_air_temp();
        float env_air_temp = get_env_air_temp();
        float air_temp = determine_temp(mobile_air_temp, env_air_temp);

        float mobile_dew_temp = this.resolver.getDew_temp_mean();
        float env_dew_temp = this.resolver.getNss_dew_temp_mean();
        float dew_temp = determine_temp(mobile_dew_temp, env_dew_temp);

        float rh = calc_rh(air_temp, dew_temp);
        //printf("air_temp: %f, dew_temp: %f, rh: %f\n", air_temp, dew_temp, rh);

        // Get addtional VDT variables
        float wind_speed = this.resolver.getNss_wind_speed_mean();

        float speed_ratio = this.resolver.getSpeed_ratio();
        float env_vis = this.resolver.getNss_prevail_vis_mean();

        int num_msg_valid_lights = this.resolver.getNum_msg_valid_lights();
        int num_lights_off = this.resolver.getNum_lights_off();
        int num_lights_fog = this.resolver.getNum_lights_fog();
        int num_lights_high_beam = this.resolver.getNum_lights_high_beam();
        float percent_lights_off = calc_percentage((float) num_lights_off, (float) num_msg_valid_lights);
        float percent_lights_fog = calc_percentage((float) num_lights_fog, (float) num_msg_valid_lights);
        float percent_lights_high_beam = calc_percentage((float) num_lights_high_beam, (float) num_msg_valid_lights);

        // Determine percent lights on from percent lights off
        float percent_lights_on;
        if (percent_lights_off != FILL_VALUE)
            percent_lights_on = 1 - percent_lights_off;
        else
            percent_lights_on = FILL_VALUE;

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: rh: %f, speed_ratio: %f, env_vis: %f, percent_lights_off: %f, percent_lights_on: %f, percent_lights_fog: %f, percent_lights_high_beam: %f\n", rh, speed_ratio, env_vis, percent_lights_off, percent_lights_on, percent_lights_fog, percent_lights_high_beam);

        // Calculate fog interest 
        float fog_interest = calc_fog_interest(rh, speed_ratio, env_vis, percent_lights_fog, percent_lights_high_beam);
        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: fog_interest: %f\n", fog_interest);

        // Calculate generic visibility interest
        float gen_vis_interest = calc_generic_vis_interest(rh, speed_ratio, env_vis, percent_lights_on, percent_lights_fog, percent_lights_high_beam);
        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: gen_vis_interest: %f\n", gen_vis_interest);

        // Hardwire the "input" confidence for now
        // Do we want to calculate this in the calc_fog and cal_generic_vis interest functions?
        float fog_input_conf = 1.0f;
        float gen_vis_input_conf = 1.0f;

        // VDT case precip-type and precip-intensity
        if (precip_type != FILL_VALUE && precip_intensity != FILL_VALUE) {
            if (precip_type == RAIN && precip_intensity == HEAVY_PRECIP)
                visibility = VIS_HEAVY_RAIN;
            else if (precip_type == SNOW && precip_intensity != NO_PRECIP) {
                if (wind_speed != FILL_VALUE && wind_speed > BLOWING_SNOW_WIND_SPEED)
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
        } else if (precip_intensity != FILL_VALUE) // VDT case with just precip-intensity
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
        if (visibility == FILL_VALUE || visibility == VIS_NORMAL) {
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

        confidenceVal.setValue(confidence);

        return visibility;
    }


    public boolean pavement_slickness_assessment(int precip_type, int precip_intensity, int pavement_condition) {
        boolean pavement_slickness = false;

        // Determine precipitation interest
        float precip_intrst;
        if (precip_intensity != FILL_VALUE && precip_type != FILL_VALUE) {
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
            precip_intrst = FILL_VALUE;

        // Determine pavement condition interest
        float pav_cond_intrst;
        if (pavement_condition != FILL_VALUE) {
            if (pavement_condition == DRY_PAVEMENT)
                pav_cond_intrst = -1;
            else if (pavement_condition == WET_PAVEMENT || pavement_condition == DRY_WET_PAVEMENT || pavement_condition == HYDROPLANE)
                pav_cond_intrst = 0;
            else if (pavement_condition == DRY_FROZEN_PAVEMENT)
                pav_cond_intrst = 0.5f;
            else // pavement_condition == SNOW_COVERED || pavement_condition == ICE_COVERED || pavement_condition == BLACK_ICE
                pav_cond_intrst = 1;
        } else
            pav_cond_intrst = FILL_VALUE;

        // For stability-interest, yaw-iqr-interest, yaw-median-interest:
        // If input to each interest component is missing
        // set the interest to 0.
        // Since we are doing a weighted sum, interest values of 0
        // will contribute nothing to the sum.
        // Note that even if inputs are not missing, interest values
        // can still be 0.

        // Determine if we have any abs, trac or stab data
        boolean stab_fields_flag = false;
        int num_abs_engaged = this.resolver.getNum_abs_engaged();
        int num_trac_engaged = this.resolver.getNum_trac_engaged();
        int num_stab_engaged = this.resolver.getNum_stab_engaged();

        if (num_abs_engaged != FILL_VALUE || num_trac_engaged != FILL_VALUE ||
                num_stab_engaged != FILL_VALUE)
            stab_fields_flag = true;

        // Determine stability interest 
        float stab_intrst;
        if (stab_fields_flag) {
            boolean abs_engaged_flag = abs_engaged();
            boolean trac_engaged_flag = trac_engaged();
            boolean stab_engaged_flag = stab_engaged();
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
        float yaw_iqr25 = this.resolver.getYaw_iqr25();
        float yaw_iqr75 = this.resolver.getYaw_iqr75();
        if (yaw_iqr25 != FILL_VALUE && yaw_iqr75 != FILL_VALUE) {
            float yaw_iqr_diff = yaw_iqr75 - yaw_iqr25;
            if (yaw_iqr_diff <= 1)
                yaw_iqr_intrst = yaw_iqr_diff;
            else // > 1
                yaw_iqr_intrst = 1;
        } else
            yaw_iqr_intrst = 0;

        // Determine yaw-median interest
        float yaw_median_intrst;
        float yaw_min = this.resolver.getYaw_min();
        float yaw_max = this.resolver.getYaw_max();
        float yaw_median = this.resolver.getYaw_median();

        if (yaw_min != FILL_VALUE && yaw_max != FILL_VALUE &&
                yaw_median != FILL_VALUE) {
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
        if (precip_intrst != FILL_VALUE && pav_cond_intrst != FILL_VALUE) {
            slickness_intrst = (precip_wgt * precip_intrst) + (pav_cond_wgt * pav_cond_intrst) + (stab_wgt * stab_intrst) +
                    (yaw_iqr_wgt * yaw_iqr_intrst) + (yaw_median_wgt * yaw_median_intrst);
        } else
            slickness_intrst = FILL_VALUE;

        if (slickness_intrst != FILL_VALUE && slickness_intrst >= 0.44)
            pavement_slickness = true;

        return pavement_slickness;
    }


    public boolean dry_pavement_slickness_assessment() {
        boolean pavement_slickness = false;

        // Get abs, trac and stab fields
        int num_abs_on = this.resolver.getNum_abs_on();
        int num_abs_engaged = this.resolver.getNum_abs_engaged();
        int num_abs_off = this.resolver.getNum_abs_off();

        //printf("num_abs_on: %d, num_abs_engaged: %d, num_abs_off: %d\n", num_abs_on, num_abs_engaged, num_abs_off);

        int num_trac_on = this.resolver.getNum_trac_on();
        int num_trac_engaged = this.resolver.getNum_trac_engaged();
        int num_trac_off = this.resolver.getNum_trac_off();

        int num_stab_on = this.resolver.getNum_stab_on();
        int num_stab_engaged = this.resolver.getNum_stab_engaged();
        int num_stab_off = this.resolver.getNum_stab_off();

        float abs_percent;
        if (num_abs_on != FILL_VALUE && num_abs_engaged != FILL_VALUE && num_abs_off != FILL_VALUE)
            abs_percent = (float) (num_abs_on + num_abs_engaged) / (float) (num_abs_on + num_abs_engaged + num_abs_off);
        else
            abs_percent = 0;

        float trac_percent;
        if (num_trac_on != FILL_VALUE && num_trac_engaged != FILL_VALUE && num_trac_off != FILL_VALUE)
            trac_percent = (float) (num_trac_on + num_trac_engaged) / (float) (num_trac_on + num_trac_engaged + num_trac_off);
        else
            trac_percent = 0;

        float stab_percent;
        if (num_stab_on != FILL_VALUE && num_stab_engaged != FILL_VALUE && num_stab_off != FILL_VALUE)
            stab_percent = (float) (num_stab_on + num_stab_engaged) / (float) (num_stab_on + num_stab_engaged + num_stab_off);
        else
            stab_percent = 0;

        if (abs_percent >= 0.25 || trac_percent >= 0.25 || stab_percent >= 0.25)
            pavement_slickness = true;

        return pavement_slickness;
    }


    public int get_pavement_condition_basic(float precip_type, float precip_intensity, float road_temp) {
        int pavement_condition = FILL_VALUE;

        if (road_temp != FILL_VALUE && precip_type != FILL_VALUE && precip_intensity != FILL_VALUE) {
            if (precip_intensity == NO_PRECIP)
                pavement_condition = DRY_PAVEMENT;
            else if (precip_type == RAIN) {
                if (road_temp <= 0 && precip_intensity != ROAD_SPLASH)
                    pavement_condition = ICE_COVERED;
                else
                    pavement_condition = WET_PAVEMENT;
            } else if (precip_type == MIX) {
                if (road_temp <= 0 && precip_intensity != ROAD_SPLASH)
                    pavement_condition = ICE_COVERED;
                else
                    pavement_condition = WET_PAVEMENT;
            } else if (precip_type == SNOW) {
                if (road_temp > -1 && road_temp < 1 && precip_intensity != ROAD_SPLASH)
                    pavement_condition = ICE_COVERED;
                else if (road_temp >= 1 && precip_intensity != HEAVY_PRECIP)
                    pavement_condition = WET_PAVEMENT;
                else if (road_temp > 2)
                    pavement_condition = WET_PAVEMENT;
                else
                    pavement_condition = SNOW_COVERED;
            }
        }

        return pavement_condition;
    }


    public int get_wipers_on() {
        int num_wipers_on = 0;

        if (this.resolver.getNum_wipers_intermittent() != FILL_VALUE) {
            num_wipers_on += this.resolver.getNum_wipers_intermittent();
        }

        if (this.resolver.getNum_wipers_low() != FILL_VALUE) {
            num_wipers_on += this.resolver.getNum_wipers_low();
        }

        if (this.resolver.getNum_wipers_high() != FILL_VALUE) {
            num_wipers_on += this.resolver.getNum_wipers_high();
        }

        if (this.resolver.getNum_wipers_automatic() != FILL_VALUE) {
            num_wipers_on += this.resolver.getNum_wipers_automatic();
        }

        return num_wipers_on;
    }


    public float get_air_temp() {
        if (this.resolver.getAir_temp_mean() == FILL_VALUE &&
                this.resolver.getAir_temp2_mean() == FILL_VALUE) {
            return FILL_VALUE;
        }

        // Use air_temp_mean if it is there, if not fall back on air_temp2_mean
        float air_temp;
        if (this.resolver.getAir_temp_mean() != FILL_VALUE)
            air_temp = this.resolver.getAir_temp_mean();
        else
            air_temp = this.resolver.getAir_temp2_mean();

        return air_temp;
    }

    public float get_env_air_temp() {
        if (this.resolver.getNss_air_temp_mean() == FILL_VALUE &&
                this.resolver.getModel_air_temp() == FILL_VALUE) {
            return FILL_VALUE;
        }

        // Use nss_air_temp_mean if it is there, if not fall back on model_air_temp
        float env_air_temp;
        if (this.resolver.getNss_air_temp_mean() != FILL_VALUE)
            env_air_temp = this.resolver.getNss_air_temp_mean();
        else
            env_air_temp = this.resolver.getModel_air_temp();

        return env_air_temp;
    }

    public boolean wipers_on() {
        int num_wipers_on = get_wipers_on();
        return (num_wipers_on > 0);
    }

    public boolean wipers_off() {
        return (this.resolver.getNum_wipers_off() > this.resolver.getNum_wipers_intermittent() &&
                this.resolver.getNum_wipers_off() > this.resolver.getNum_wipers_high() &&
                this.resolver.getNum_wipers_off() > this.resolver.getNum_wipers_low());

    }

    public boolean wipers_high() {
        return (this.resolver.getNum_wipers_high() >= this.resolver.getNum_wipers_intermittent() &&
                this.resolver.getNum_wipers_high() >= this.resolver.getNum_wipers_off() &&
                this.resolver.getNum_wipers_high() >= this.resolver.getNum_wipers_low());

    }

    public boolean wipers_intermittent() {
        return (this.resolver.getNum_wipers_intermittent() >= this.resolver.getNum_wipers_low() &&
                this.resolver.getNum_wipers_intermittent() > this.resolver.getNum_wipers_high() &&
                this.resolver.getNum_wipers_intermittent() >= this.resolver.getNum_wipers_off());

    }


    public boolean wipers_low() {
        return (this.resolver.getNum_wipers_low() > this.resolver.getNum_wipers_intermittent() &&
                this.resolver.getNum_wipers_low() > this.resolver.getNum_wipers_high() &&
                this.resolver.getNum_wipers_low() >= this.resolver.getNum_wipers_off());

    }


    public boolean abs_engaged() {
        return (this.resolver.getNum_msg_valid_abs() > 0 &&
                this.resolver.getNum_abs_engaged() > (this.resolver.getNum_abs_on() + this.resolver.getNum_abs_off()));
    }


    public boolean abs_not_engaged() {
        return (this.resolver.getNum_msg_valid_abs() > 0 &&
                (this.resolver.getNum_abs_on() + this.resolver.getNum_abs_off()) > this.resolver.getNum_abs_engaged());
    }


    public boolean trac_engaged() {
        return (this.resolver.getNum_msg_valid_trac() > 0 &&
                this.resolver.getNum_trac_engaged() > (this.resolver.getNum_trac_on() + this.resolver.getNum_trac_off()));
    }


    public boolean trac_not_engaged() {
        return (this.resolver.getNum_msg_valid_trac() > 0 &&
                (this.resolver.getNum_trac_on() + this.resolver.getNum_trac_off()) > this.resolver.getNum_trac_engaged());
    }


    public boolean stab_engaged() {
        return (this.resolver.getNum_msg_valid_stab() > 0 &&
                this.resolver.getNum_stab_engaged() > (this.resolver.getNum_stab_on() + this.resolver.getNum_stab_off()));
    }


    public boolean stab_not_engaged() {
        return (this.resolver.getNum_msg_valid_stab() > 0 &&
                (this.resolver.getNum_stab_on() + this.resolver.getNum_stab_off()) > this.resolver.getNum_stab_engaged());
    }


    public boolean wipers_missing() {
        return (this.resolver.getNum_wipers_off() == FILL_VALUE &&
                this.resolver.getNum_wipers_intermittent() == FILL_VALUE &&
                this.resolver.getNum_wipers_low() == FILL_VALUE &&
                this.resolver.getNum_wipers_high() == FILL_VALUE);
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


    public double get_dry_pavement() {
        double interest = 0.0;
        if (this.resolver.getSpeed_ratio() != FILL_VALUE) {
            interest += this.resolver.getSpeed_ratio();
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


        interest += get_interest(this.resolver.getLat_accel_mean(), 0, 0.2, 1);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0.2, 0.24, -25, 6);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0, 0.5, 1);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0.5, 1, -2, 2);
        interest += get_interest(this.resolver.getYaw_mean(), 0, 1, 1);
        interest += get_interest(this.resolver.getYaw_mean(), 1, 1.7, -1.428, 2.428);
        interest += get_interest(this.resolver.getSteering_angle_mean(), 0, 4, -0.25, 1);

        return interest;
    }


    public double get_wet_pavement() {
        double interest = 0.0;

        interest += get_interest(this.resolver.getSpeed_ratio(), 0, 0.8, 1.25, 0);
        interest += get_interest(this.resolver.getSpeed_ratio(), 0.8, 1.0, -1.5, 2.2);

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

        interest += get_abs_stab_trac_interest(0, 1);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0, 0.2, 1);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0.2, 0.3, -10, 3);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0, 0.5, 1);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0.5, 1, -2, 2);
        interest += get_interest(this.resolver.getYaw_mean(), 0, 1, 1);
        interest += get_interest(this.resolver.getYaw_mean(), 1, 1.7, -1.428, 2.428);
        interest += get_interest(this.resolver.getSteering_angle_mean(), 0, 4, -0.25, 1);

        return interest;
    }


    public double get_snow_covered_pavement() {
        double interest = 0.0;
        interest += get_interest(this.resolver.getSpeed_ratio(), 0, 0.6, 0.833, 0.502);
        interest += get_interest(this.resolver.getSpeed_ratio(), 0.6, 1, -2, 2.2);

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

        interest += get_abs_stab_trac_interest(1, 1);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0, 0.2, 1);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0.2, 0.4, -5, 2);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0, 0.5, 1);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0.5, 1, -2, 2);
        interest += get_interest(this.resolver.getYaw_mean(), 0, 1, 1);
        interest += get_interest(this.resolver.getYaw_mean(), 1, 2, -1, 2);
        interest += get_interest(this.resolver.getSteering_angle_mean(), 0, 4, -0.25, 1);

        return interest;
    }


    public double get_slick_pavement() {
        double interest = 0.0;
        interest += get_interest(this.resolver.getSpeed_ratio(), 0, 0.8, -1.25, 1);

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

        interest += get_abs_stab_trac_interest(2, 1);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0, 0.2, 5, 0);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0.2, 0.4, 1);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0.3, 0.5, 5, -1.5);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0.5, 1, 1);
        interest += get_interest(this.resolver.getYaw_mean(), 0, 1, 1, 0);
        interest += get_interest(this.resolver.getYaw_mean(), 1, 2, 1);
        interest += get_interest(this.resolver.getSteering_angle_mean(), 2, 3, 1, -2);
        interest += get_interest(this.resolver.getSteering_angle_mean(), 3, 4, 1);

        return interest;
    }


    public double get_road_splash_pavement() {
        double interest = 0.0;

        interest += get_interest(this.resolver.getSpeed_ratio(), 0, 0.8, 1.25, 0);
        interest += get_interest(this.resolver.getSpeed_ratio(), 0.8, 1, -1.5, 2.2);

  /*
  interest += get_interest(this.resolver.air_temp_mean,-10,0,0.1,1);
  interest += get_interest(this.resolver.air_temp_mean,0,5,1);

  interest += get_interest(this.resolver.radar_ref, 0, 25, 1);

  if (wipers_low(this.resolver) || wipers_high(this.resolver))
    {
      interest += 1;
    }
  */

        interest += get_abs_stab_trac_interest(0.5, 1.0);
        interest += get_interest(this.resolver.getLat_accel_mean(), 0.2, 0.4, -5, 2);
        interest += get_interest(this.resolver.getLon_accel_mean(), 0.5, 1, -2, 2);
        interest += get_interest(this.resolver.getYaw_mean(), 1, 1.7, -1.428, 2.428);
        interest += get_interest(this.resolver.getSteering_angle_mean(), 0, 4, 0.25, 1);

        return interest;
    }

    public double get_abs_stab_trac_interest(double engaged, double not_engaged) {
        double interest = 0.0;
        if (abs_not_engaged()) {
            interest += not_engaged;
        }
        if (trac_not_engaged()) {
            interest += not_engaged;
        }
        if (stab_not_engaged()) {
            interest += not_engaged;
        }
        if (abs_engaged()) {
            interest += engaged;
        }
        if (trac_engaged()) {
            interest += engaged;
        }
        if (stab_engaged()) {
            interest += engaged;
        }
        return interest;
    }

    public double get_interest(double val, double slope_min, double slope_max, double m, double b) {
        if (val == FILL_VALUE) {
            return 0;
        }

        if (val >= slope_min && val < slope_max) {
            return m * val + b;
        }

        return 0;
    }


    public double get_interest(double val, double min, double max, double flat_val) {
        if (val == FILL_VALUE) {
            return 0;
        }

        if (val >= min && val < max) {
            return flat_val;
        }

        return 0;
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
        if (rh != FILL_VALUE) {
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
        if (speed_ratio != FILL_VALUE) {
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
        if (station_vis != FILL_VALUE) {
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
        if (percent_fog_lights != FILL_VALUE) {
            if (percent_fog_lights > 0 && percent_fog_lights <= 1)
                fog_lights_intrst = percent_fog_lights;
            else
                fog_lights_intrst = 0;
        } else
            fog_lights_intrst = 0;

        // Determine high beams interest
        float high_beams_intrst;
        if (percent_high_beams != FILL_VALUE) {
            if (percent_high_beams > 0 && percent_high_beams < 1)
                high_beams_intrst = percent_high_beams;
            else
                high_beams_intrst = 0;
        } else
            high_beams_intrst = 0;

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: rh_intrst: %f, speed_ratio_intrst: %f, station_vis_intrst: %f, fog_lights_intrst: %f, high_beams_intrst: %f\n", rh_intrst, speed_ratio_intrst, station_vis_intrst, fog_lights_intrst, high_beams_intrst);  

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

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: fog_intrst: %f\n", fog_intrst);

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
        if (rh != FILL_VALUE) {
            // Convert rh to a percentage
            float rh_pct = rh * 100;
            //printf("rh_pct: %f\n", rh_pct);

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
        if (speed_ratio != FILL_VALUE) {
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
        if (station_vis != FILL_VALUE) {
            // Convert visibility from km to miles
            float station_vis_mi = station_vis * 0.621371f;
            //printf("station_vis(km): %f, station_vis_mi: %f\n", station_vis, station_vis_mi);

            if (station_vis_mi >= 0 && station_vis_mi <= 10)
                station_vis_intrst = (-2 * station_vis_mi / 10) + 1;
            else
                station_vis_intrst = 0;
        } else
            station_vis_intrst = 0;

        // Determine lights on interest
        float lights_on_intrst;
        if (percent_lights_on != FILL_VALUE) {
            if (percent_lights_on > 0 && percent_lights_on <= 1)
                lights_on_intrst = percent_lights_on;
            else
                lights_on_intrst = 0;
        } else
            lights_on_intrst = 0;

        // Determine fog lights interest
        float fog_lights_intrst;
        if (percent_fog_lights != FILL_VALUE) {
            if (percent_fog_lights > 0 && percent_fog_lights <= 1)
                fog_lights_intrst = percent_fog_lights;
            else
                fog_lights_intrst = 0;
        } else
            fog_lights_intrst = 0;

        // Determine high beams interest
        float high_beams_intrst;
        if (percent_high_beams != FILL_VALUE) {
            if (percent_high_beams > 0 && percent_high_beams < 1)
                high_beams_intrst = percent_high_beams;
            else
                high_beams_intrst = 0;
        } else
            high_beams_intrst = 0;

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: rh_intrst: %f, speed_ratio_intrst: %f, station_vis_intrst: %f, lights_on_intrst: %f, fog_lights_intrst: %f, high_beams_intrst: %f\n", rh_intrst, speed_ratio_intrst, station_vis_intrst, lights_on_intrst, fog_lights_intrst, high_beams_intrst);  

        // Set weights of fields for light interest 
        float lights_on_wgt = 0.375f;
        float fog_lights_wgt = 0.625f;
        float high_beams_wgt = -0.125f;

        // Determine light interest
        float lights_intrst = (lights_on_wgt * lights_on_intrst) + (fog_lights_wgt * fog_lights_intrst) + (high_beams_wgt * high_beams_intrst);

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: lights_intrst: %f\n", lights_intrst);

        // Set weights of fields for generic visibility interest
        float rh_wgt = 0.3f;
        float speed_ratio_wgt = 0.25f;
        float lights_wgt = 0.25f;
        float station_vis_wgt = 0.2f;

        // Determine generic visibility interest
        float vis_intrst = (rh_wgt * rh_intrst) + (speed_ratio_wgt * speed_ratio_intrst) + (lights_wgt * lights_intrst) + (station_vis_wgt * station_vis_intrst);

        //printf("RWX_ROAD_SEGMENT_ASSESSMENT.CC: generic vis_intrst: %f\n", vis_intrst);

        return vis_intrst;
    }


    public float determine_temp(float mobile_temp, float env_temp) {
        float temp;

        if (mobile_temp != FILL_VALUE)
            temp = mobile_temp;
        else if (env_temp != FILL_VALUE)
            temp = env_temp;
        else
            temp = FILL_VALUE;
  
  /*
  if(mobile_temp != FILL_VALUE && env_temp != FILL_VALUE)
    {
      float temp_abs_diff = fabs(mobile_temp - env_temp);
      if(temp_abs_diff < 1)
	temp = env_temp;
      else
	temp = mobile_temp;
    }
  else if(env_temp != FILL_VALUE)
    temp = env_temp;
  else if(mobile_temp != FILL_VALUE)
    temp = mobile_temp;
  else
    temp = FILL_VALUE;
  */

        return temp;
    }

    public float calc_rh(float temp, float dewpt) {
        float rh;

        if (temp == FILL_VALUE || dewpt == FILL_VALUE)
            return FILL_VALUE;

        rh = calc_pr_vapr(dewpt) / calc_pr_vapr(temp);

        return rh;
    }

    public float calc_pr_vapr(float temp) {
        float pr_vap = FILL_VALUE;

        if (temp != FILL_VALUE)
            pr_vap = (float) (6.112 * StrictMath.exp((17.67 * temp) / (temp + 243.5)));

        return (pr_vap);
    }

    public float calc_percentage(float num_values, float total_values) {
        float percentage;

        if (num_values == FILL_VALUE || total_values == FILL_VALUE)
            return FILL_VALUE;

        percentage = num_values / total_values;
        return percentage;
    }

    String get_precip_type_str(int precip_type) {
        String precip_type_str = "";

        if (precip_type == NO_PRECIP)
            precip_type_str = "NO_PRECIP";
        else if (precip_type == RAIN)
            precip_type_str = "RAIN";
        else if (precip_type == MIX)
            precip_type_str = "MIX";
        else if (precip_type == SNOW)
            precip_type_str = "SNOW";
        else
            precip_type_str = "MISSING";

        return precip_type_str;
    }

    String get_precip_intensity_str(int precip_intensity) {
        String precip_intensity_str = "";

        if (precip_intensity == NO_PRECIP)
            precip_intensity_str = "NO_PRECIP";
        else if (precip_intensity == LIGHT_PRECIP)
            precip_intensity_str = "LIGHT_PRECIP";
        else if (precip_intensity == MODERATE_PRECIP)
            precip_intensity_str = "MODERATE_PRECIP";
        else if (precip_intensity == HEAVY_PRECIP)
            precip_intensity_str = "HEAVY_PRECIP";
        else if (precip_intensity == ROAD_SPLASH)
            precip_intensity_str = "ROAD_SPLASH";
        else
            precip_intensity_str = "MISSING";

        return precip_intensity_str;
    }


    String get_pavement_condition_str(int pavement_condition) {
        String pavement_condtion_str = "";

        if (pavement_condition == DRY_PAVEMENT)
            pavement_condtion_str = "DRY_PAVEMENT";
        else if (pavement_condition == WET_PAVEMENT)
            pavement_condtion_str = "WET_PAVEMENT";
        else if (pavement_condition == SNOW_COVERED)
            pavement_condtion_str = "SNOW_COVERED";
        else if (pavement_condition == ICE_COVERED)
            pavement_condtion_str = "ICE_COVERED";
        else if (pavement_condition == HYDROPLANE)
            pavement_condtion_str = "HYDROPLANE";
        else if (pavement_condition == BLACK_ICE)
            pavement_condtion_str = "BLACK_ICE";
        else if (pavement_condition == DRY_WET_PAVEMENT)
            pavement_condtion_str = "DRY_WET_PAVEMENT";
        else if (pavement_condition == DRY_FROZEN_PAVEMENT)
            pavement_condtion_str = "DRY_FROZEN_PAVEMENT";
        else
            pavement_condtion_str = "MISSING";

        return pavement_condtion_str;
    }

    String get_visibility_str(int visibility) {
        String visibility_str = "";

        if (visibility == VIS_NORMAL)
            visibility_str = "VIS_NORMAL";
        else if (visibility == VIS_LOW)
            visibility_str = "VIS_LOW";
        else if (visibility == VIS_HEAVY_RAIN)
            visibility_str = "VIS_HEAVY_RAIN";
        else if (visibility == VIS_HEAVY_SNOW)
            visibility_str = "VIS_HEAVY_SNOW";
        else if (visibility == VIS_BLOWING_SNOW)
            visibility_str = "VIS_BLOWING_SNOW";
        else if (visibility == VIS_FOG)
            visibility_str = "VIS_FOG";
        else if (visibility == VIS_HAZE)
            visibility_str = "VIS_HAZE";
        else if (visibility == VIS_DUST)
            visibility_str = "VIS_DUST";
        else if (visibility == VIS_SMOKE)
            visibility_str = "VIS_SMOKE";
        else
            visibility_str = "MISSING";

        return visibility_str;
    }

//    rwx_road_segment_assessment & operator=(rwx_road_segment_assessment &assessment)
//    {
//        if (this == &assessment)
//        {
//            return *this;
//        }
//
//        operator= (assessment);
//
//        precip_type = assessment.precip_type;
//        precip_intensity = assessment.precip_intensity;
//        pavement_condition = assessment.pavement_condition;
//        pavement_slickness = assessment.pavement_slickness;
//        visibility = assessment.visibility;
//
//        road_segment_id = assessment.road_segment_id;
//
//        return *this;
//    }

    /**
     * combine StrictMath.floor and StrictMath.abs to implement c++ fabs
     */
    public static float fabs(double x) {

        return (float) StrictMath.floor(StrictMath.abs(x));
    }

    @Override
    public IObs resolve(String obsTypeName) {
        return this.resolver.resolve(obsTypeName);
    }
}
