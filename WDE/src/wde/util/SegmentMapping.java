package wde.util;


import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayInt;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;


public class SegmentMapping implements Comparable<SegmentMapping> {
    public static final String[] OBS_TYPES =
            {
                    "204 | icePercent",
                    "205 | precip10min",
                    "206 | precipIntensity",
                    "207 | precipType",
                    "541 | essLatitude",
                    "542 | essLongitude",
                    "543 | essVehicleSpeed",
                    "544 | essVehicleBearing",
                    "545 | essVehicleOdometer",
                    "551 | essReferenceHeight",
                    "554 | essAtmosphericPressure",
                    "574 | essWetBulbTemp",
                    "575 | essDewpointTemp",
                    "576 | essMaxTemp",
                    "577 | essMinTemp",
                    "581 | essRelativeHumidity",
                    "583 | essAdjacentSnowDepth",
                    "584 | essRoadwaySnowDepth",
                    "585 | essRoadwaySnowpackDepth",
                    "586 | essPrecipYesNo",
                    "587 | essPrecipRate",
                    "588 | essSnowfallAccumRate",
                    "589 | essPrecipSituation",
                    "592 | essTotalSun",
                    "593 | essCloudSituation",
                    "595 | essInstantaneousSolarRadiation",
                    "596 | essTotalRadiation",
                    "597 | essTotalRadiationPeriod",
                    "5101 | essVisibility",
                    "5102 | essVisibilitySituation",
                    "5121 | essMobileFriction",
                    "5122 | essMobileObservationGroundState",
                    "5123 | essMobileObservationPavement",
                    "5134 | essPaveTreatmentAmount",
                    "5135 | essPaveTreatmentWidth",
                    "5141 | essCO",
                    "5142 | essCO2",
                    "5143 | essNO",
                    "5144 | essNO2",
                    "5145 | essSO2",
                    "5146 | essO3",
                    "5733 | essAirTemperature",
                    "5810 | essIceThickness",
                    "5811 | essPrecipitationStartTime",
                    "5812 | essPrecipitationEndTime",
                    "5813 | essPrecipitationOneHour",
                    "5814 | essPrecipitationThreeHours",
                    "5815 | essPrecipitationSixHours",
                    "5816 | essPrecipitationTwelveHours",
                    "5817 | essPrecipitation24Hours",
                    "51137 | essSurfaceStatus",
                    "51138 | essSurfaceTemperature",
                    "51139 | essPavementTemperature",
                    "51165 | essSubSurfaceTemperature",
                    "51166 | essSubSurfaceMoisture",
                    "51167 | essSubSurfaceSensorError",
                    "51332 | essPaveTreatProductType",
                    "51333 | essPaveTreatProductForm",
                    "51334 | essPercentProductMix",
                    "56104 | windSensorAvgSpeed",
                    "56105 | windSensorAvgDirection",
                    "56106 | windSensorSpotSpeed",
                    "56107 | windSensorSpotDirection",
                    "56108 | windSensorGustSpeed",
                    "56109 | windSensorGustDirection",
                    "58212 | waterLevelSensorReading",
                    "511310 | essSurfaceWaterDepth",
                    "511311 | essSurfaceSalinity",
                    "511313 | essSurfaceFreezePoint",
                    "511314 | essSurfaceBlackIceSignal",
                    "511315 | essPavementSensorError",
                    "511316 | essSurfaceIceOrWaterDepth",
                    "511317 | essSurfaceConductivityV2",
                    "511319 | pavementSensorTemperatureDepth",
                    "511371 | essSurfaceStatus2",
                    "561010 | windSensorSituation",
                    "2000000 | canHeadlights",
                    "2000001 | canWiperStatus",
                    "2000002 | canAntiLockBrakeStatus",
                    "2000003 | canBrakeBoostApplied",
                    "2000004 | canBrakeAppliedStatus",
                    "2000005 | canHeading",
                    "2000006 | canLatAcceleration",
                    "2000007 | canLongAcceleration",
                    "2000008 | canSpeed",
                    "2000009 | canStabilityControlStatus",
                    "2000010 | canSteeringWheelAngle",
                    "2000011 | canSteeringWheelAngleRateOfChange",
                    "2000012 | canTractionControlState",
                    "2000013 | canYawRate",
                    "2001000 | segTotalNum",
                    "2001001 | vdtAirTemperature",
                    "2001002 | vdtAtmosphericPressure",
                    "2001003 | vdtAirTemperatureAvg",
                    "2001004 | vdtAtmosphericPressureAvg",
                    "2001005 | vdtDewpointTempAvg",
                    "2001006 | vdtPrecipitationOneHourAvg",
                    "2001007 | vdtVisibilityAvg",
                    "2001008 | vdtWindSensorAvgDirection",
                    "2001009 | vdtWindSensorAvgSpeed",
                    "2001010 | radar_cref",
                    "2001011 | radar_precip_flag",
                    "2001012 | radar_precip_type",
                    "2001013 | cloud_mask",
                    "2001014 | segValidAirTemperatureCANNum",
                    "2001015 | segAirTemperatureIQR25CAN",
                    "2001016 | segAirTemperatureIQR75CAN",
                    "2001017 | segAirTemperatureMaxCAN",
                    "2001018 | segAirTemperatureAvgCAN",
                    "2001019 | segAirTemperatureMedCAN",
                    "2001020 | segAirTemperatureMinCAN",
                    "2001021 | segAirTemperatureStdDevCAN",
                    "2001022 | segAirTemperatureVarCAN",
                    "2001023 | segValidAirTemperatureESSNum",
                    "2001024 | segAirTemperatureIQR25AvgESS",
                    "2001025 | segAirTemperatureIQR75AvgESS",
                    "2001026 | segAirTemperatureMaxESS",
                    "2001027 | segAirTemperatureAvgESS",
                    "2001028 | segAirTemperatureMedESS",
                    "2001029 | segAirTemperatureMinESS",
                    "2001030 | segAirTemperatureStdDevESS",
                    "2001031 | segAirTemperatureVarESS",
                    "2001032 | segValidAtmosphericPressureCANNum",
                    "2001033 | segAtmosphericPressureIQR25CAN",
                    "2001034 | segAtmosphericPressureIQR75CAN",
                    "2001035 | segAtmosphericPressureMaxCAN",
                    "2001036 | segAtmosphericPressureAvgCAN",
                    "2001037 | segAtmosphericPressureMedCAN",
                    "2001038 | segAtmosphericPressureMinCAN",
                    "2001039 | segAtmosphericPressureStdDevCAN",
                    "2001040 | segAtmosphericPressureVarCAN",
                    "2001041 | segValidDewpointTempNum",
                    "2001042 | segDewpointTempIQR25ESS",
                    "2001043 | segDewpointTempIQR75ESS",
                    "2001044 | segDewpointTempMaxESS",
                    "2001045 | segDewpointTempAvgESS",
                    "2001046 | segDewpointTempMedESS",
                    "2001047 | segDewpointTempMinESS",
                    "2001048 | segDewpointTempStdDevESS",
                    "2001049 | segDewpointTempVarESS",
                    "2001050 | segValidHeadingNum",
                    "2001051 | segHeadingIQR25",
                    "2001052 | segHeadingIQR75",
                    "2001053 | segHeadingMax",
                    "2001054 | segHeadingAvg",
                    "2001055 | segHeadingMed",
                    "2001056 | segHeadingMin",
                    "2001057 | segHeadingStdDev",
                    "2001058 | segHeadingVar",
                    "2001059 | segValidLatAccelerationNum",
                    "2001060 | segLatAccelerationIQR25",
                    "2001061 | segLatAccelerationIQR75",
                    "2001062 | segLatAccelerationMax",
                    "2001063 | segLatAccelerationAvg",
                    "2001064 | segLatAccelerationMed",
                    "2001065 | segLatAccelerationMin",
                    "2001066 | segLatAccelerationStdDev",
                    "2001067 | segLatAccelerationVar",
                    "2001068 | segValidLongAccelerationNum",
                    "2001069 | segLongAccelerationIQR25",
                    "2001070 | segLongAccelerationIQR75",
                    "2001071 | segLongAccelerationMax",
                    "2001072 | segLongAccelerationAvg",
                    "2001073 | segLongAccelerationMed",
                    "2001074 | segLongAccelerationMin",
                    "2001075 | segLongAccelerationStdDev",
                    "2001076 | segLongAccelerationVar",
                    "2001077 | segValidAntiLockBrakeNum",
                    "2001078 | segAntiLockBrakeStatusEngagedNum",
                    "2001079 | segAntiLockBrakeStatusUnavailableNum",
                    "2001080 | segAntiLockBrakeStatusOffNum",
                    "2001081 | segAntiLockBrakeStatusOnNum",
                    "2001082 | segValidBrakeAppliedStatusNum",
                    "2001083 | segBrakeAppliedStatusOffNum",
                    "2001084 | segBrakeAppliedStatusOnNum",
                    "2001085 | segBrakeAppliedStatusLeftFrontNum",
                    "2001086 | segBrakeAppliedStatusLeftRearNum",
                    "2001087 | segBrakeAppliedStatusRightFrontNum",
                    "2001088 | segBrakeAppliedStatusRightRearNum",
                    "2001089 | segValidBrakeBoostAppliedNum",
                    "2001090 | segBrakeBoostAppliedUnavailableNum",
                    "2001091 | segBrakeBoostAppliedOffNum",
                    "2001092 | segBrakeBoostAppliedOnNum",
                    "2001093 | segValidLightsNum",
                    "2001094 | segAutomaticLightControlOnNum",
                    "2001095 | segDaytimeRunningLightsOnNum",
                    "2001096 | segFogLightOnNum",
                    "2001097 | segHazardSignalOnNum",
                    "2001098 | segHighBeamLightsOnNum",
                    "2001099 | segLeftTurnSignalOnNum",
                    "2001100 | segLowBeamLightsOnNum",
                    "2001101 | segAllLightsOffNum",
                    "2001102 | segParkingLightsOnNum",
                    "2001103 | segRightTurnSignalOnNum",
                    "2001104 | segValidStabilityControlStatusNum",
                    "2001105 | segStabilityControlStatusEngagedNum",
                    "2001106 | segStabilityControlStatusUnavailableNum",
                    "2001107 | segStabilityControlStatusOffNum",
                    "2001108 | segStabilityControlStatusOnNum",
                    "2001109 | segValidTractionControlStateNum",
                    "2001110 | segTractionControlStateEngagedNum",
                    "2001111 | segTractionControlStateUnavailableNum",
                    "2001112 | segTractionControlStateOffNum",
                    "2001113 | segTractionControlStateOnNum",
                    "2001114 | segValidWiperStatusNum",
                    "2001115 | segWiperStatusAutomaticPresentNum",
                    "2001116 | segWiperStatusHighNum",
                    "2001117 | segWiperStatusIntermittentNum",
                    "2001118 | segWiperStatusLowNum",
                    "2001119 | segWiperStatusUnavailableNum",
                    "2001120 | segWiperStatusOffNum",
                    "2001121 | segWiperStatusWasherInUserNum",
                    "2001122 | segValidSpeedNum",
                    "2001123 | segSpeedIQR25CAN",
                    "2001124 | segSpeedIQR75CAN",
                    "2001125 | segSpeedMaxCAN",
                    "2001126 | segSpeedAvgCAN",
                    "2001127 | segSpeedMedCAN",
                    "2001128 | segSpeedMinCAN",
                    "2001129 | segSpeedRatioCAN",
                    "2001130 | segSpeedStdDevCAN",
                    "2001131 | segSpeedVarCAN",
                    "2001132 | segValidSteeringWheelAngleNum",
                    "2001133 | segSteeringWheelAngleIQR25",
                    "2001134 | segSteeringWheelAngleIQR75",
                    "2001135 | segSteeringWheelAngleMax",
                    "2001136 | segSteeringWheelAngleAvg",
                    "2001137 | segSteeringWheelAngleMed",
                    "2001138 | segSteeringWheelAngleMin",
                    "2001139 | segSteeringWheelAngleStdDev",
                    "2001140 | segSteeringWheelAngleVar",
                    "2001141 | segValidSteeringWheelAngleRateOfChangeNum",
                    "2001142 | segSteeringWheelAngleRateOfChangeMax",
                    "2001143 | segSteeringWheelAngleRateOfChangeAvg",
                    "2001144 | segSteeringWheelAngleRateOfChangeMed",
                    "2001145 | segSteeringWheelAngleRateOfChangeMin",
                    "2001146 | segSteeringWheelAngleRateOfChangeStdDev",
                    "2001147 | segSteeringWheelAngleRateOfChangeVar",
                    "2001148 | segValidSurfaceTemperatureNum",
                    "2001149 | segSurfaceTemperatureIQR25ESS",
                    "2001150 | segSurfaceTemperatureIQR75ESS",
                    "2001151 | segSurfaceTemperatureMaxESS",
                    "2001152 | segSurfaceTemperatureAvgESS",
                    "2001153 | segSurfaceTemperatureMedESS",
                    "2001154 | segSurfaceTemperatureMinESS",
                    "2001155 | segSurfaceTemperatureStdDevESS",
                    "2001156 | segSurfaceTemperatureVarESS",
                    "2001157 | segValidYawNum",
                    "2001158 | segYawIQR25",
                    "2001159 | segYawIQR75",
                    "2001160 | segYawMax",
                    "2001161 | segYawAvg",
                    "2001162 | segYawMed",
                    "2001163 | segYawMin",
                    "2001164 | segYawStdDev",
                    "2001165 | segYawVar",
                    "2001166 | segAllHazards",
                    "2001167 | segSurfaceStatus",
                    "2001168 | segPrecipType",
                    "2001169 | segVisibility"

                    // Additional ones added later are not included here since this program has already been executed
            };

    String m_sName;
    String m_sId;
    ArrayList<Double> m_oLat = null;
    ArrayList<Double> m_oLon = null;


    SegmentMapping() {
    }


    SegmentMapping(String sName, String sId) {
        m_sName = sName;
        m_sId = sId;
        m_oLat = new ArrayList();
        m_oLon = new ArrayList();
    }

    public static void main(String[] sArgs) {
        try {
            String sNcFile = sArgs[0];
            String sSqlFile = sArgs[1];
            String sUpdateDate = sArgs[2];
            int nPlatformId = Integer.parseInt(sArgs[3]);
            int nPlatformStaticId = Integer.parseInt(sArgs[4]);
            int nContribId = Integer.parseInt(sArgs[5]);
            int nSiteId = Integer.parseInt(sArgs[6]);

            FileWriter oWriter = new FileWriter(sSqlFile);

            StringBuilder sBuffer1 = new StringBuilder();
            StringBuilder sBuffer2 = new StringBuilder();
            ArrayList<SegmentMapping> oSegMaps = new ArrayList();
            SegmentMapping oSearch = new SegmentMapping();

            NetcdfFile oNcFile = NetcdfFile.open(sNcFile);
            Variable oSegName = oNcFile.findVariable("seg_name");
            Variable oSegId = oNcFile.findVariable("seg_id");
            Variable oLat = oNcFile.findVariable("latitude");
            Variable oLon = oNcFile.findVariable("longitude");

            ArrayChar.D2 oSegs1 = (ArrayChar.D2) oSegName.read();
            ArrayInt.D1 oSegs2 = (ArrayInt.D1) oSegId.read();
            ArrayDouble.D1 oLats = (ArrayDouble.D1) oLat.read();
            ArrayDouble.D1 oLons = (ArrayDouble.D1) oLon.read();

            // accumulate lat lon points by segment name
            int[] nShape1 = oSegs1.getShape();
            for (int nRow = 0; nRow < nShape1[0]; nRow++) {
                sBuffer1.setLength(0);
                sBuffer2.setLength(0);
                for (int nCol = 0; nCol < nShape1[1]; nCol++) {
                    if (oSegs1.get(nRow, nCol) != 0)
                        sBuffer1.append(oSegs1.get(nRow, nCol));
                }
                if (oSegs2.get(nRow) != 0)
                    sBuffer2.append(oSegs2.get(nRow));

                oSearch.m_sName = sBuffer1.toString();
                oSearch.m_sId = sBuffer2.toString();
                int nIndex = Collections.binarySearch(oSegMaps, oSearch);
                if (nIndex < 0) {
                    nIndex = ~nIndex;
                    oSegMaps.add(nIndex, new SegmentMapping(oSearch.m_sName, oSearch.m_sId));
                }
                SegmentMapping oSegMap = oSegMaps.get(nIndex);

                oSegMap.m_oLat.add(new Double(oLats.get(nRow)));
                oSegMap.m_oLon.add(new Double(oLons.get(nRow)));
            }

            // create platform SQL insert statements
            oWriter.write(String.format("alter sequence meta.platform_id_seq " +
                    "restart with %d;\r\n", nPlatformId));

            for (int nIndex = 0; nIndex < oSegMaps.size(); nIndex++) {
                SegmentMapping oSegMap = oSegMaps.get(nIndex);
                int nMid = oSegMap.m_oLat.size() / 2;

                oWriter.write(String.format
                                (
                                        "INSERT INTO meta.platform (staticid, updatetime, " +
                                                "platformcode, category, description, " +
                                                "contribid, siteid, locbaselat, locbaselong) " +
                                                "VALUES (%d, '%s', '%s', 'S', " +
                                                "'MDOT VDT road segment %s', %d, %d, %f, %f);\r\n",
                                        nIndex + nPlatformStaticId, sUpdateDate, oSegMap.m_sName,
                                        oSegMap.m_sName, nContribId, nSiteId,
                                        oSegMap.m_oLat.get(nMid), oSegMap.m_oLon.get(nMid))
                );
            }
            oNcFile.close();


            // create sensor SQL insert statements while platforms are in memory
            int nSensorId = Integer.parseInt(sArgs[7]);
            int nQchparmId = Integer.parseInt(sArgs[8]);

            oWriter.write(String.format("alter sequence meta.sensor_id_seq " +
                    "restart with %d;\r\n", nSensorId));

            for (int nSegIndex = 0; nSegIndex < oSegMaps.size(); nSegIndex++) {
                for (int nIndex = 0; nIndex < OBS_TYPES.length; nIndex++) {
                    oWriter.write(String.format
                                    (
                                            "INSERT INTO meta.sensor " +
                                                    "(sourceid, staticid, updatetime, platformid, " +
                                                    "contribid, sensorindex, obstypeid, qchparmid, " +
                                                    "distgroup) VALUES(2, %d, '%s', %d, %d, 0, %s, %d, 2);\r\n",
                                            nIndex + (nPlatformStaticId + nSegIndex) * 1000,
                                            sUpdateDate, nPlatformId + nSegIndex, nContribId,
                                            OBS_TYPES[nIndex].split(" | ")[0], nIndex + nQchparmId)
                    );
                }
            }
            oWriter.close();
        } catch (Exception oException) {
            System.out.println
                    (
                            "usage:\r\n\r\n" +
                                    "First create a new site for the contrib segments and determine " +
                                    "the site id, contrib id, and starting qchparm id; then derive " +
                                    "the starting platform id and sensor id from the database.\r\n\r\n" +
                                    "There are nine required command line parameters:\r\n\r\n" +
                                    "<input NC file path and file name> contains the road segments\r\n" +
                                    "<output SQL file path and file name> SQL statements are stored here\r\n" +
                                    "<update date> use the format YYYY-MM-DD i.e. 2014-01-01\r\n" +
                                    "<starting platform id> determined from first step\r\n" +
                                    "<starting platform static id> contrib id * 1000 + 1 works well\r\n" +
                                    "<contrib id> determined in the first step\r\n" +
                                    "<site id> created in the first step\r\n" +
                                    "<starting sensor id> determined from first step\r\n" +
                                    "<starting qchparm id> determined from firs step\r\n"
                    );
            oException.printStackTrace();
        }
    }

    @Override
    public int compareTo(SegmentMapping oRhs) {
        return m_sName.compareTo(oRhs.m_sName);
    }
}
