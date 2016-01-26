package wde.compute.algo;

import wde.compute.Inference;
import wde.compute.InferenceResult;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.Observation;
import wde.qchs.Radar;

import java.util.Set;

import static wde.compute.algo.PrecipitationType.NO_PRECIP;

public class PrecipitationIntensity extends Inference {

    // Precip Intensity enumeration
    //public static final int NO_PRECIP = 0;  // defined above
    public static final int LIGHT_PRECIP = 1;
    public static final int MODERATE_PRECIP = 2;
    public static final int HEAVY_PRECIP = 3;
    public static final int ROAD_SPLASH = 4;

    public final float precip_intensity_wgt = 0.30f;

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

    public final float radar_ref_wgt = 0.50f;
    public final float wipers_wgt = 0.30f;
    public final float speed_ratio_wgt = 0.10f;
    public final float lights_wgt = 0.10f;

    @Override
    public synchronized Set<InferenceResult> doInference(int obsTypeId, ISensor sensor, IObs obs) {
        Set<InferenceResult> resultSet = newInferenceResultSet();
        InferenceResult result = new InferenceResult();
        resultSet.add(result);

        result.setRan();
        result.setName("PrecipitationIntensity");
        result.setObsTypeId(obsTypeId);

        int precip_intensity = NO_PRECIP;
        float confidence = 0f;

        int precip_type = Integer.MAX_VALUE;
        IObs precipTypeObs = getRelatedObs(1000000, obs);
        if (precipTypeObs != null)
            precip_type = ((Double)precipTypeObs.getValue()).intValue();

        double radar_ref = Radar.getInstance().getReading(0, obs.getObsTimeLong(), obs.getLatitude(), obs.getLongitude());

        float radar_ref_input_conf = 1.0f;
        float wipers_input_conf = 1.0f;
        float speed_ratio_input_conf = 1.0f;
        float lights_input_conf = 1.0f;

        // Use precip-type to determine radar-composite-reflectivity (dBz) thresholds
        int RADAR_LIGHT_PRECIP;
        int RADAR_MODERATE_PRECIP;
        if (precip_type == PrecipitationType.MIX || precip_type == PrecipitationType.SNOW) {
            RADAR_LIGHT_PRECIP = WINTER_RADAR_LIGHT_PRECIP;
            RADAR_MODERATE_PRECIP = WINTER_RADAR_MODERATE_PRECIP;
        } else if (precip_type == PrecipitationType.RAIN) {
            RADAR_LIGHT_PRECIP = SUMMER_RADAR_LIGHT_PRECIP;
            RADAR_MODERATE_PRECIP = SUMMER_RADAR_MODERATE_PRECIP;
        } else {
            RADAR_LIGHT_PRECIP = DEF_RADAR_LIGHT_PRECIP;
            RADAR_MODERATE_PRECIP = DEF_RADAR_MODERATE_PRECIP;
        }

        // VDT case using just radar-composite-reflectivity
        if (!Double.isNaN(radar_ref)) {
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

//        // VDT case: modify intensity based on wipers or determine intensity if we just have wipers and no radar data
//        if (wipers_flag == 1) {
//            if (precip_intensity == FILL_VALUE) // This for the case with no radar data
//            {
//                if (wipers_off_flag)
//                    precip_intensity = NO_PRECIP;
//                else if (wipers_interm_flag)
//                    precip_intensity = LIGHT_PRECIP;
//                else // wipers_low_flag || wipers_high_flag
//                    precip_intensity = MODERATE_PRECIP;
//            } else if (precip_intensity == NO_PRECIP) {
//                if (wipers_on_flag)
//                    precip_intensity = ROAD_SPLASH;
//            } else if (precip_intensity == LIGHT_PRECIP) {
//                if (wipers_low_flag || wipers_high_flag)
//                    precip_intensity = MODERATE_PRECIP;
//            } else if (precip_intensity == MODERATE_PRECIP) {
//                if (wipers_high_flag)
//                    precip_intensity = HEAVY_PRECIP;
//                else if (wipers_off_flag)
//                    precip_intensity = LIGHT_PRECIP;
//            } else // precip_intensity == HEAVY_PRECIP
//            {
//                if (wipers_interm_flag)
//                    precip_intensity = MODERATE_PRECIP;
//                else if (wipers_off_flag)
//                    precip_intensity = LIGHT_PRECIP;
//            }
//            confidence = confidence + (wipers_input_conf * wipers_wgt);
//        }

//        // VDT case: modify intensity based on speed-ratio
//        if (speed_ratio != FILL_VALUE) {
//            if (precip_intensity == HEAVY_PRECIP) {
//                if (speed_ratio >= 0.7)
//                    precip_intensity = MODERATE_PRECIP;
//            }
//
//            confidence = confidence + (speed_ratio_input_conf * speed_ratio_wgt);
//        }

//        // VDT case: modify intensity based on headlights
//        if (percent_lights_off != FILL_VALUE) {
//            if (precip_intensity == MODERATE_PRECIP) {
//                if (percent_lights_off > 0.50)
//                    precip_intensity = LIGHT_PRECIP;
//            } else if (precip_intensity == HEAVY_PRECIP) {
//                if (percent_lights_off > 0.50)
//                    precip_intensity = MODERATE_PRECIP;
//            }
//            confidence = confidence + (lights_input_conf * lights_wgt);
//        }

        result.setConfidence(confidence);

        IObs pseudoObs = new Observation(
                1000001,
                obs.getSourceId(),
                obs.getSensorId(),
                obs.getObsTimeLong(),
                obs.getRecvTimeLong(),
                obs.getLatitude(),
                obs.getLongitude(),
                obs.getElevation(),
                precip_intensity);
        result.addObservation(pseudoObs);

        return resultSet;
    }
}
