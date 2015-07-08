/************************************************************************
 * Source filename: VDTDataIngester.java
 * <p/>
 * Creation date: Oct 24, 2013
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

package wde.vdt;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import wde.dao.*;
import wde.metadata.IPlatform;
import wde.metadata.Segment;
import wde.metadata.TimeInvariantMetadata;
import wde.obs.ObsKey;
import wde.obs.Observation;
import wde.qeds.PlatformMonitor;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.vdt.probe.ObsQcMap;
import wde.vdt.probe.QcProbeMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class VDTDataIngester {

    private static final Logger logger = Logger.getLogger(VDTDataIngester.class);
    // Indices of quality checks
    private static final int QC_FLAGS = 13;
    private static final int CAT = 0;
    private static final int CRT = 1;
    private static final int MAT = 2;
    private static final int NST = 3;
    private static final int NVT = 4;
    private static final int PERSIST = 5;
    private static final int RANGE = 6;
    private static final int SDT = 7;
    private static final int SPATIAL_BARNES = 8;
    private static final int SPATIAL_IQR = 9;
    private static final int TIME_STEP = 10;
    private static final int OVERALL = 11;
    private static final int FILTERING = 12;
    public static boolean wdeMgrInstantiated = false;
    private static VDTDataIngester instance = null;
    private Properties prop = null;

    private HashMap<String, ObsQcMap> probeTypeMap = null;

    private HashMap<String, String> segTypeMap = null;

    private String outputRoot;
    private String qcProbePrefix;
    private String segmentStatisticsPrefix;
    private String segmentAssessmentPrefix;
    private String outputFileExtension;
    private String separator;

    private SimpleDateFormat dateFormat = null;
    private SimpleDateFormat tsFormat = null;
    private QcProbeMessage qcProbeMessage = null;
    private SegmentStatistics stat = null;
    private SegmentAssessment segAssessment = null;

    private PlatformMonitor platformMonitor = null;
    private ObsTypeDao obsTypeDao = null;
    private SensorDao sensorDao = null;
    private ObservationDao observationDao = null;
    private TreeMap<ObsKey, Observation> obsMap = null;
    private HashMap<String, Integer> stateContribMap = null;

    public VDTDataIngester() {
        prop = new Properties();
        separator = System.getProperty("file.separator");
        loadPropertiesFile();

        populateObsQcMaps();
        populateSegTypeMap();
        platformMonitor = PlatformMonitor.getInstance();
        obsTypeDao = ObsTypeDao.getInstance();
        sensorDao = SensorDao.getInstance();
        observationDao = ObservationDao.getInstance();

        dateFormat = new SimpleDateFormat(VDTController.dateFormatStr);

        String df = VDTController.dateFormatStr + VDTController.fileDelimiter + VDTController.timeFormatStr;

        tsFormat = new SimpleDateFormat(df);
        obsMap = new TreeMap<>();
        stateContribMap = new HashMap<>();
        stateContribMap.put("MN", new Integer(2));
        stateContribMap.put("MI", new Integer(26));
    }

    public static void main(String[] args) {
        DOMConfigurator.configure("config/vdt_log4j.xml");
        VDTDataIngester.getInstance();
    }

    public static VDTDataIngester getInstance() {
        if (instance == null)
            instance = new VDTDataIngester();

        return instance;
    }

    public void processData(GregorianCalendar now, String stateName) throws Exception {
        Integer contribIdInt = stateContribMap.get(stateName);
        if (contribIdInt == null) {
            logger.error("Unexpected stateName encountered: " + stateName);
            return;
        }

        int contribId = contribIdInt.intValue();

        Date oDate = now.getTime();
        String postfix = VDTController.fileDelimiter + tsFormat.format(now.getTime()) + VDTController.fileDelimiter;
        String ncFilePath = getFilePath(outputRoot, oDate, qcProbePrefix, postfix + outputFileExtension);
        String targetFilePath = getFilePath(outputRoot + separator + stateName, oDate, qcProbePrefix, postfix + outputFileExtension);

        File file = new File(ncFilePath);
        if (file.exists()) {
            qcProbeMessage = new QcProbeMessage();
            populateNcMem(ncFilePath, qcProbeMessage);
            persistQcProbe(contribId);
            moveFile(ncFilePath, targetFilePath);
        }

        ncFilePath = getFilePath(outputRoot, oDate, segmentStatisticsPrefix, postfix + outputFileExtension);
        targetFilePath = getFilePath(outputRoot + separator + stateName, oDate, segmentStatisticsPrefix, postfix + outputFileExtension);
        file = new File(ncFilePath);
        if (file.exists()) {
            stat = new SegmentStatistics();
            populateNcMem(ncFilePath, stat);
            persistSegmentStatistics(contribId);
            moveFile(ncFilePath, targetFilePath);
        }

        ncFilePath = getFilePath(outputRoot, oDate, segmentAssessmentPrefix, postfix + outputFileExtension);
        targetFilePath = getFilePath(outputRoot + separator + stateName, oDate, segmentAssessmentPrefix, postfix + outputFileExtension);
        file = new File(ncFilePath);
        if (file.exists()) {
            segAssessment = new SegmentAssessment();
            populateNcMem(ncFilePath, segAssessment);
            persistSegmentAssessment(contribId);
            moveFile(ncFilePath, targetFilePath);
        }
    }

    public void cleanup(long expirationTime) {
        int count = 0;
        synchronized (obsMap) {
            for (ObsKey key : obsMap.keySet()) {
                if (key.getObsTime() <= expirationTime) {
                    obsMap.put(key, null);
                    count++;
                }
            }
        }
        logger.info("Cleaned up " + count + " old observations");
    }

    private void moveFile(String sourcePath, String targetPath) {
        try {
            File f = new File(sourcePath);
            if (!f.renameTo(new File(targetPath)))
                logger.error("Failed to move " + sourcePath + " to " + targetPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getFilePath(String root, Date date, String sPrefix, String postfix) {
        StringBuilder filename = new StringBuilder(root);
        filename.append(separator);
        filename.append(dateFormat.format(date));
        new File(filename.toString()).mkdirs();

        filename.append(separator);
        filename.append(sPrefix);
        filename.append(postfix);

        return filename.toString();
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        if (!wdeMgrInstantiated) {
            String path = System.getProperty("user.dir") + separator + "config" + separator + "vdt_config.properties";

            try {
                FileInputStream fis = new FileInputStream(path);
                prop.load(fis);
                fis.close();

                outputRoot = prop.getProperty("outputroot");
                qcProbePrefix = prop.getProperty("qcprobeprefix");
                segmentStatisticsPrefix = prop.getProperty("segstatprefix");
                segmentAssessmentPrefix = prop.getProperty("segassessprefix");
                outputFileExtension = prop.getProperty("outputfileextension");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            ConfigSvc configSvc = ConfigSvc.getInstance();
            Config config = configSvc.getConfig(this);

            outputRoot = config.getString("outputroot", "");
            qcProbePrefix = config.getString("qcprobeprefix", "");
            segmentStatisticsPrefix = config.getString("segstatprefix", "");
            segmentAssessmentPrefix = config.getString("segassessprefix", "");
            outputFileExtension = config.getString("outputfileextension", "");
        }
    }

    private void populateNcMem(String filePath, NcMem ncMem) throws IOException {
        NetcdfFile ncfile = null;

        ncfile = NetcdfFile.open(filePath);

        populateLists(ncfile, ncMem, ncMem.getAttributeMap());
        populateLists(ncfile, ncMem, ncMem.getVariableMap());

        ncfile.close();
    }

    private void populateLists(NetcdfFile ncfile, NcMem ncMem, HashMap<String, Class> map) {
        for (String varName : map.keySet()) {
            logger.debug(varName);
            Variable v = ncfile.findVariable(varName);
            if (v == null) {
                logger.error(varName + " not found");
                continue;
            }
            try {
                Array data = v.read();
                String className = map.get(varName).toString();
                if (className.contains("Double"))
                    ncMem.populateDoubleArrayVariable(varName, data);
                else if (className.contains("Float"))
                    ncMem.populateFloatArrayVariable(varName, data);
                else if (className.contains("Int"))
                    ncMem.populateIntArrayVariable(varName, data);
                else if (className.contains("Short"))
                    ncMem.populateShortArrayVariable(varName, data);
                else if (className.contains("String"))
                    ncMem.populateStringArrayVariable(varName, data);
            } catch (IOException ioe) {
                logger.error("trying to read " + varName, ioe);
            }
        }
    }

    private void populateObsQcMaps() {

        probeTypeMap = new HashMap<>();
        HashMap<String, Integer> qcMap = null;
        ObsQcMap obsQcMap = null;

        String vdtObsType = "air_temp";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_cat_passed", new Integer(CAT));
        qcMap.put(vdtObsType + "_crt_passed", new Integer(CRT));
        qcMap.put(vdtObsType + "_mat_passed", new Integer(MAT));
        qcMap.put(vdtObsType + "_nst_passed", new Integer(NST));
        qcMap.put(vdtObsType + "_nvt_passed", new Integer(NVT));
        qcMap.put(vdtObsType + "_persist_passed", new Integer(PERSIST));
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        qcMap.put(vdtObsType + "_sdt_passed", new Integer(SDT));
        qcMap.put(vdtObsType + "_spatial_barnes_passed", new Integer(SPATIAL_BARNES));
        qcMap.put(vdtObsType + "_spatial_iqr_passed", new Integer(SPATIAL_IQR));
        qcMap.put(vdtObsType + "_step_passed", new Integer(TIME_STEP));
        obsQcMap = new ObsQcMap("essAirTemperature", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "air_temp2";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_cat_passed", new Integer(CAT));
        qcMap.put(vdtObsType + "_crt_passed", new Integer(CRT));
        qcMap.put(vdtObsType + "_mat_passed", new Integer(MAT));
        qcMap.put(vdtObsType + "_nst_passed", new Integer(NST));
        qcMap.put(vdtObsType + "_nvt_passed", new Integer(NVT));
        qcMap.put(vdtObsType + "_persist_passed", new Integer(PERSIST));
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        qcMap.put(vdtObsType + "_sdt_passed", new Integer(SDT));
        qcMap.put(vdtObsType + "_spatial_barnes_passed", new Integer(SPATIAL_BARNES));
        qcMap.put(vdtObsType + "_spatial_iqr_passed", new Integer(SPATIAL_IQR));
        qcMap.put(vdtObsType + "_step_passed", new Integer(TIME_STEP));
        obsQcMap = new ObsQcMap("essAirTemperature", "float", 1, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "bar_press";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_cat_passed", new Integer(CAT));
        qcMap.put(vdtObsType + "_crt_passed", new Integer(CRT));
        qcMap.put(vdtObsType + "_mat_passed", new Integer(MAT));
        qcMap.put(vdtObsType + "_nst_passed", new Integer(NST));
        qcMap.put(vdtObsType + "_nvt_passed", new Integer(NVT));
        qcMap.put(vdtObsType + "_persist_passed", new Integer(PERSIST));
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        qcMap.put(vdtObsType + "_sdt_passed", new Integer(SDT));
        qcMap.put(vdtObsType + "_spatial_barnes_passed", new Integer(SPATIAL_BARNES));
        qcMap.put(vdtObsType + "_spatial_iqr_passed", new Integer(SPATIAL_IQR));
        qcMap.put(vdtObsType + "_step_passed", new Integer(TIME_STEP));
        obsQcMap = new ObsQcMap("canAtmosphericPressure", "short", 0, qcMap);
        probeTypeMap.put("bar_pressure", obsQcMap);

        vdtObsType = "dew_temp";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_crt_passed", new Integer(CRT));
        qcMap.put(vdtObsType + "_nst_passed", new Integer(NST));
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        qcMap.put(vdtObsType + "_qc_passed", new Integer(OVERALL));
        obsQcMap = new ObsQcMap("essDewpointTemp", "float", 1, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "surface_temp";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_cat_passed", new Integer(CAT));
        qcMap.put(vdtObsType + "_crt_passed", new Integer(CRT));
        qcMap.put(vdtObsType + "_nvt_passed", new Integer(NVT));
        qcMap.put(vdtObsType + "_persist_passed", new Integer(PERSIST));
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        qcMap.put(vdtObsType + "_spatial_barnes_passed", new Integer(SPATIAL_BARNES));
        qcMap.put(vdtObsType + "_spatial_iqr_passed", new Integer(SPATIAL_IQR));
        qcMap.put(vdtObsType + "_step_passed", new Integer(TIME_STEP));
        obsQcMap = new ObsQcMap("essSurfaceTemperature", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "wiper_status";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canWiperStatus", "short", 1, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "lights";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canHeadlights", "short", 1, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "abs";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canAntiLockBrakeStatus", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "brake_boost";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canBrakeBoostApplied", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "brake_status";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canBrakeAppliedStatus", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "heading";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canHeading", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "hoz_accel_lat";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canLatAcceleration", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "hoz_accel_long";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canLongAcceleration", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "speed";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canSpeed", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "stab";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canStabilityControlStatus", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "steering_angle";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canSteeringWheelAngle", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "steering_rate";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canSteeringWheelAngleRateOfChange", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "trac";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canTractionControlState", "short", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);

        vdtObsType = "yaw_rate";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("canYawRate", "float", 0, qcMap);
        probeTypeMap.put(vdtObsType, obsQcMap);
    }

    private void persistQcProbe(int contribId) {
        Observation obs = null;
        ObsKey obsKey = null;
        char[] qchCharFlag = null;
        ArrayList<Observation> obsList = new ArrayList<>();
        int recNum = qcProbeMessage.getRecNum();

        for (int i = 0; i < recNum; i++) {

            // First collect all the basic fields obs_time, recv_time, vehicle_id, etc.
            long obsTime = (long) getDoubleValue(qcProbeMessage, "obs_time", i) * 1000;
            long recvTime = (long) getDoubleValue(qcProbeMessage, "rec_time", i) * 1000;
            int latitude = MathUtil.toMicro(getDoubleValue(qcProbeMessage, "latitude", i));
            short filtering = getShortValue(qcProbeMessage, "latitude_dft_passed", i); // assume longitude has the same value
            int longitude = MathUtil.toMicro(getDoubleValue(qcProbeMessage, "longitude", i));
            float elevation = getFloatValue(qcProbeMessage, "elevation", i);
            if (elevation == NcMem.FLOAT_FILL_VALUE)
                elevation = 0;
            String platformCode = getStringValue(qcProbeMessage, "vehicle_id", i);

            // look for individual obs types and create corresponding observations
            for (String vdtObsType : probeTypeMap.keySet()) {
                logger.debug(vdtObsType);
                ObsQcMap obsQcMap = probeTypeMap.get(vdtObsType);
                String valueType = obsQcMap.getValueType();
                double value = 0;

                if (valueType.equals("short")) {
                    short shortValue = getShortValue(qcProbeMessage, vdtObsType, i);
                    if (shortValue == NcMem.SHORT_FILL_VALUE)
                        continue;

                    value = shortValue;
                } else if (valueType.equals("float")) {
                    float floatValue = getFloatValue(qcProbeMessage, vdtObsType, i);
                    if (floatValue == NcMem.FLOAT_FILL_VALUE)
                        continue;

                    value = floatValue;
                } else {
                    logger.error("Unexpected data type encountered");
                }

                qchCharFlag = getQcFlags(vdtObsType, i, filtering);

                int obsTypeId = obsTypeDao.getObsTypeId(obsQcMap.getObsType());
                int sourceId = 2; // for VDT

                // same sensor is shared by both WxDE and VDT
                int sensorId = sensorDao.getSensorId(1, platformCode, obsTypeId, obsQcMap.getSensorIndex());

                if (sensorId != -1) {
                    obsKey = new ObsKey(sourceId, sensorId, obsTypeId, obsTime);
                    synchronized (obsMap) {
                        if (obsMap.get(obsKey) == null) {
                            obs = new Observation(obsTypeId, sourceId, sensorId, obsTime, recvTime, latitude, longitude,
                                    (short) elevation, value, qchCharFlag, (float) 1.0);  // confValue is set to 1 for now

                            obsMap.put(obsKey, obs);

                            obsList.add(obs);

                            // Need to trigger WxDE to load these observations into memory
                            platformMonitor.updatePlatform(obs);
                        }
                    }
                } else {
                    String qchCharFlagStr = "";
                    for (char aChar : qchCharFlag)
                        qchCharFlagStr += "\"" + aChar + "\",";
                    qchCharFlagStr = "{" + qchCharFlagStr.substring(0, qchCharFlagStr.length() - 1) + "}";

                    // Add the obs in invalidObs table
                    observationDao.insertInvalidObservation(sourceId, contribId, platformCode, obsTypeId, obsQcMap.getSensorIndex(),
                            new Timestamp(obsTime), new Timestamp(recvTime), latitude, longitude, value, qchCharFlagStr);
                }
            }
        }
        observationDao.insertObservations(obsList);
    }

    private void populateSegTypeMap() {
        segTypeMap = new HashMap<>();
        segTypeMap.put("air_temp_iqr25", "segAirTemperatureIQR25CAN");
        segTypeMap.put("air_temp_iqr75", "segAirTemperatureIQR75CAN");
        segTypeMap.put("air_temp_max", "segAirTemperatureMaxCAN");
        segTypeMap.put("air_temp_mean", "segAirTemperatureAvgCAN");
        segTypeMap.put("air_temp_median", "segAirTemperatureMedCAN");
        segTypeMap.put("air_temp_min", "segAirTemperatureMinCAN");
        segTypeMap.put("air_temp_stdev", "segAirTemperatureStdDevCAN");
        segTypeMap.put("air_temp_var", "segAirTemperatureVarCAN");
        segTypeMap.put("air_temp2_iqr25", "segAirTemperatureIQR25AvgESS");
        segTypeMap.put("air_temp2_iqr75", "segAirTemperatureIQR75AvgESS");
        segTypeMap.put("air_temp2_max", "segAirTemperatureMaxESS");
        segTypeMap.put("air_temp2_mean", "segAirTemperatureAvgESS");
        segTypeMap.put("air_temp2_median", "segAirTemperatureMedESS");
        segTypeMap.put("air_temp2_min", "segAirTemperatureMinESS");
        segTypeMap.put("air_temp2_stdev", "segAirTemperatureStdDevESS");
        segTypeMap.put("air_temp2_var", "segAirTemperatureVarESS");
        segTypeMap.put("bar_press_iqr25", "segAtmosphericPressureIQR25CAN");
        segTypeMap.put("bar_press_iqr75", "segAtmosphericPressureIQR75CAN");
        segTypeMap.put("bar_press_max", "segAtmosphericPressureMaxCAN");
        segTypeMap.put("bar_press_mean", "segAtmosphericPressureAvgCAN");
        segTypeMap.put("bar_press_median", "segAtmosphericPressureMedCAN");
        segTypeMap.put("bar_press_min", "segAtmosphericPressureMinCAN");
        segTypeMap.put("bar_press_stdev", "segAtmosphericPressureStdDevCAN");
        segTypeMap.put("bar_press_var", "segAtmosphericPressureVarCAN");
        segTypeMap.put("dew_temp_iqr25", "segDewpointTempIQR25ESS");
        segTypeMap.put("dew_temp_iqr75", "segDewpointTempIQR75ESS");
        segTypeMap.put("dew_temp_max", "segDewpointTempMaxESS");
        segTypeMap.put("dew_temp_mean", "segDewpointTempAvgESS");
        segTypeMap.put("dew_temp_median", "segDewpointTempMedESS");
        segTypeMap.put("dew_temp_min", "segDewpointTempMinESS");
        segTypeMap.put("dew_temp_stdev", "segDewpointTempStdDevESS ");
        segTypeMap.put("dew_temp_var", "segDewpointTempVarESS");
        segTypeMap.put("heading_iqr25", "segHeadingIQR25");
        segTypeMap.put("heading_iqr75", "segHeadingIQR75");
        segTypeMap.put("heading_max", "segHeadingMax");
        segTypeMap.put("heading_mean", "segHeadingAvg");
        segTypeMap.put("heading_median", "segHeadingMed");
        segTypeMap.put("heading_min", "segHeadingMin");
        segTypeMap.put("heading_stdev", "segHeadingStdDev");
        segTypeMap.put("heading_var", "segHeadingVar");
        segTypeMap.put("hoz_accel_lat_iqr25", "segLatAccelerationIQR25");
        segTypeMap.put("hoz_accel_lat_iqr75", "segLatAccelerationIQR75");
        segTypeMap.put("hoz_accel_lat_max", "segLatAccelerationMax");
        segTypeMap.put("hoz_accel_lat_mean", "segLatAccelerationAvg");
        segTypeMap.put("hoz_accel_lat_median", "segLatAccelerationMed");
        segTypeMap.put("hoz_accel_lat_min", "segLatAccelerationMin");
        segTypeMap.put("hoz_accel_lat_stdev", "segLatAccelerationStdDev");
        segTypeMap.put("hoz_accel_lat_var", "segLatAccelerationVar");
        segTypeMap.put("hoz_accel_lon_iqr25", "segLongAccelerationIQR25");
        segTypeMap.put("hoz_accel_lon_iqr75", "segLongAccelerationIQR75");
        segTypeMap.put("hoz_accel_lon_max", "segLongAccelerationMax");
        segTypeMap.put("hoz_accel_lon_mean", "segLongAccelerationAvg");
        segTypeMap.put("hoz_accel_lon_median", "segLongAccelerationMed");
        segTypeMap.put("hoz_accel_lon_min", "segLongAccelerationMin");
        segTypeMap.put("hoz_accel_lon_stdev", "segLongAccelerationStdDev");
        segTypeMap.put("hoz_accel_lon_var", "segLongAccelerationVar");

        // Need to create corresponding types
        segTypeMap.put("model_air_temp", "segAirTemperatureModel");
        segTypeMap.put("model_bar_press", "segAtmosphericPressureModel");
        segTypeMap.put("nss_air_temp_mean", "segAirTemperatureAvgNearbyESS");
        segTypeMap.put("nss_bar_press_mean", "segAtmosphericPressureAvgNearbyESS");
        segTypeMap.put("nss_dew_temp_mean", "segDewpointTempAvgNearbyESS");
        segTypeMap.put("nss_hourly_precip_mean", "segPrecipitationOneHourAvgNearbyESS");
        segTypeMap.put("nss_prevail_vis_mean", "segVisibilityAvgNearbyESS");
        segTypeMap.put("nss_wind_dir_mean", "segWindSensorAvgDirectionNearbyESS");
        segTypeMap.put("nss_wind_speed_mean", "segWindSensorAvgSpeedNearbyESS");

        segTypeMap.put("num_abs_engaged", "segAntiLockBrakeStatusEngagedNum");
        segTypeMap.put("num_abs_not_equipped", "segAntiLockBrakeStatusUnavailableNum");
        segTypeMap.put("num_abs_off", "segAntiLockBrakeStatusOffNum");
        segTypeMap.put("num_abs_on", "segAntiLockBrakeStatusOnNum");
        segTypeMap.put("num_brakes_all_off", "segBrakeAppliedStatusOffNum");
        segTypeMap.put("num_brakes_all_on", "segBrakeAppliedStatusOnNum");
        segTypeMap.put("num_brakes_boost_not_equipped", "segBrakeBoostAppliedUnavailableNum");
        segTypeMap.put("num_brakes_boost_off", "segBrakeBoostAppliedOffNum");
        segTypeMap.put("num_brakes_boost_on", "segBrakeBoostAppliedOnNum");
        segTypeMap.put("num_brakes_lf_active", "segBrakeAppliedStatusLeftFrontNum");
        segTypeMap.put("num_brakes_lr_active", "segBrakeAppliedStatusLeftRearNum");
        segTypeMap.put("num_brakes_rf_active", "segBrakeAppliedStatusRightFrontNum");
        segTypeMap.put("num_brakes_rr_active", "segBrakeAppliedStatusRightRearNum");
        segTypeMap.put("num_lights_automatic_control", "segAutomaticLightControlOnNum");
        segTypeMap.put("num_lights_drl", "segDaytimeRunningLightsOnNum");
        segTypeMap.put("num_lights_fog", "segFogLightOnNum");
        segTypeMap.put("num_lights_hazard", "segHazardSignalOnNum");
        segTypeMap.put("num_lights_high_beam", "segHighBeamLightsOnNum");
        segTypeMap.put("num_lights_left_turn", "segLeftTurnSignalOnNum");
        segTypeMap.put("num_lights_low_beam", "segLowBeamLightsOnNum");
        segTypeMap.put("num_lights_off", "segAllLightsOffNum");
        segTypeMap.put("num_lights_parking", "segParkingLightsOnNum");
        segTypeMap.put("num_lights_right_turn", "segRightTurnSignalOnNum");
        segTypeMap.put("num_msg_valid_abs", "segValidAntiLockBrakeNum");
        segTypeMap.put("num_msg_valid_air_temp", "segValidAirTemperatureCANNum");
        segTypeMap.put("num_msg_valid_air_temp2", "segValidAirTemperatureESSNum");
        segTypeMap.put("num_msg_valid_bar_press", "segValidAtmosphericPressureCANNum");
        segTypeMap.put("num_msg_valid_brakes", "segValidBrakeAppliedStatusNum");
        segTypeMap.put("num_msg_valid_brakes_boost", "segValidBrakeBoostAppliedNum");
        segTypeMap.put("num_msg_valid_dew_temp", "segValidDewpointTempNum");
        segTypeMap.put("num_msg_valid_heading", "segValidHeadingNum");
        segTypeMap.put("num_msg_valid_hoz_accel_lat", "segValidLatAccelerationNum");
        segTypeMap.put("num_msg_valid_hoz_accel_lon", "segValidLongAccelerationNum");
        segTypeMap.put("num_msg_valid_lights", "segValidLightsNum");
        segTypeMap.put("num_msg_valid_speed", "segValidSpeedNum");
        segTypeMap.put("num_msg_valid_stab", "segValidStabilityControlStatusNum");
        segTypeMap.put("num_msg_valid_steering_angle", "segValidSteeringWheelAngleNum");
        segTypeMap.put("num_msg_valid_steering_rate", "segValidSteeringWheelAngleRateOfChangeNum");
        segTypeMap.put("num_msg_valid_surface_temp", "segValidSurfaceTemperatureNum");
        segTypeMap.put("num_msg_valid_trac", "segValidTractionControlStateNum");
        segTypeMap.put("num_msg_valid_wipers", "segValidWiperStatusNum");
        segTypeMap.put("num_msg_valid_yaw", "segValidYawNum");
        segTypeMap.put("num_stab_engaged", "segStabilityControlStatusEngagedNum");
        segTypeMap.put("num_stab_not_equipped", "segStabilityControlStatusUnavailableNum");
        segTypeMap.put("num_stab_off", "segStabilityControlStatusOffNum");
        segTypeMap.put("num_stab_on", "segStabilityControlStatusOnNum");
        segTypeMap.put("num_trac_engaged", "segTractionControlStateEngagedNum");
        segTypeMap.put("num_trac_not_equipped", "segTractionControlStateUnavailableNum");
        segTypeMap.put("num_trac_off", "segTractionControlStateOffNum");
        segTypeMap.put("num_trac_on", "segTractionControlStateOnNum");
        segTypeMap.put("num_wipers_automatic", "segWiperStatusAutomaticPresentNum");
        segTypeMap.put("num_wipers_high", "segWiperStatusHighNum");
        segTypeMap.put("num_wipers_intermittent", "segWiperStatusIntermittentNum");
        segTypeMap.put("num_wipers_low", "segWiperStatusLowNum");
        segTypeMap.put("num_wipers_not_equipped", "segWiperStatusUnavailableNum");
        segTypeMap.put("num_wipers_off", "segWiperStatusOffNum");
        segTypeMap.put("num_wipers_washer", "segWiperStatusWasherInUserNum");
        segTypeMap.put("radar_cref", "radar_cref");
        segTypeMap.put("radar_precip_flag", "radar_precip_flag");
        segTypeMap.put("radar_precip_type", "radar_precip_type");
        segTypeMap.put("speed_iqr25", "segSpeedIQR25CAN");
        segTypeMap.put("speed_iqr75", "segSpeedIQR75CAN");
        segTypeMap.put("speed_max", "segSpeedMaxCAN");
        segTypeMap.put("speed_mean", "segSpeedAvgCAN");
        segTypeMap.put("speed_median", "segSpeedMedCAN");
        segTypeMap.put("speed_min", "segSpeedMinCAN");
        segTypeMap.put("speed_ratio", "segSpeedRatioCAN");
        segTypeMap.put("speed_stdev", "segSpeedStdDevCAN");
        segTypeMap.put("speed_var", "segSpeedVarCAN");
        segTypeMap.put("steering_angle_iqr25", "segSteeringWheelAngleIQR25");
        segTypeMap.put("steering_angle_iqr75", "segSteeringWheelAngleIQR75");
        segTypeMap.put("steering_angle_max", "segSteeringWheelAngleMax");
        segTypeMap.put("steering_angle_mean", "segSteeringWheelAngleAvg");
        segTypeMap.put("steering_angle_median", "segSteeringWheelAngleMed");
        segTypeMap.put("steering_angle_min", "segSteeringWheelAngleMin");
        segTypeMap.put("steering_angle_stdev", "segSteeringWheelAngleStdDev");
        segTypeMap.put("steering_angle_var", "segSteeringWheelAngleVar");
        segTypeMap.put("steering_rate_max", "segSteeringWheelAngleRateOfChangeMax");
        segTypeMap.put("steering_rate_mean", "segSteeringWheelAngleRateOfChangeAvg");
        segTypeMap.put("steering_rate_median", "segSteeringWheelAngleRateOfChangeMed");
        segTypeMap.put("steering_rate_min", "segSteeringWheelAngleRateOfChangeMin");
        segTypeMap.put("steering_rate_stdev", "segSteeringWheelAngleRateOfChangeStdDev");
        segTypeMap.put("steering_rate_var", "segSteeringWheelAngleRateOfChangeVar");
        segTypeMap.put("surface_temp_iqr25", "segSurfaceTemperatureIQR25ESS");
        segTypeMap.put("surface_temp_iqr75", "segSurfaceTemperatureIQR75ESS");
        segTypeMap.put("surface_temp_max", "segSurfaceTemperatureMaxESS");
        segTypeMap.put("surface_temp_mean", "segSurfaceTemperatureAvgESS");
        segTypeMap.put("surface_temp_median", "segSurfaceTemperatureMedESS");
        segTypeMap.put("surface_temp_min", "segSurfaceTemperatureMinESS");
        segTypeMap.put("surface_temp_stdev", "segSurfaceTemperatureStdDevESS");
        segTypeMap.put("surface_temp_var", "segSurfaceTemperatureVarESS");
        segTypeMap.put("total_num_msg", "segTotalNum");
        segTypeMap.put("yaw_iqr25", "segYawIQR25");
        segTypeMap.put("yaw_iqr75", "segYawIQR75");
        segTypeMap.put("yaw_max", "segYawMax");
        segTypeMap.put("yaw_mean", "segYawAvg");
        segTypeMap.put("yaw_median", "segYawMed");
        segTypeMap.put("yaw_min", "segYawMin");
        segTypeMap.put("yaw_stdev", "segYawStdDev");
        segTypeMap.put("yaw_var", "segYawVar");
        segTypeMap.put("all_hazards", "segAllHazards");
        segTypeMap.put("pavement_condition", "segSurfaceStatus");
        segTypeMap.put("precipitation", "segPrecipType");
        segTypeMap.put("visibility", "segVisibility");
    }

    private void persistSegmentStatistics(int contribId) {
        Observation obs = null;
        ObsKey obsKey = null;
        ArrayList<Observation> obsList = new ArrayList<>();
        int recNum = stat.getRecNum();
        HashMap<String, TimeInvariantMetadata> segmentMap = SegmentDao.getInstance().getSegmentMap();

        for (int i = 0; i < recNum; i++) {

            // First collect all the basic fields obs_time, recv_time, vehicle_id, etc.
            long obsTime = (long) (getDoubleValue(stat, "begin_time", i) + getDoubleValue(stat, "end_time", i)) * 500; // get the average
            long recvTime = System.currentTimeMillis();
            short elevation = 0; // no elevation information for segment
            String segId = String.valueOf(getIntegerValue(stat, "id", i));

            // Need to map segId to platformCode
            String key = contribId + "-" + segId;
            String platformCode = ((Segment) segmentMap.get(key)).getSegmentName();
            IPlatform platform = PlatformDao.getInstance().getPlatformForContribId(contribId, platformCode);
            int latitude = MathUtil.toMicro(platform.getLocBaseLat());
            int longitude = MathUtil.toMicro(platform.getLocBaseLong());

            // look for individual obs types and create corresponding observations
            HashMap<String, Class> statObsTypes = stat.getVariableMap();
            for (String statObsType : statObsTypes.keySet()) {
                logger.debug(statObsType);

                String valueType = statObsTypes.get(statObsType).getName();
                double value = 0;

                if (valueType.equals("java.lang.Integer")) {
                    int intValue = getIntegerValue(stat, statObsType, i);
                    if (intValue == NcMem.INT_FILL_VALUE)
                        continue;

                    value = intValue;
                } else if (valueType.equals("java.lang.Short")) {
                    short shortValue = getShortValue(stat, statObsType, i);
                    if (shortValue == NcMem.SHORT_FILL_VALUE)
                        continue;

                    value = shortValue;
                } else if (valueType.equals("java.lang.Float")) {
                    float floatValue = getFloatValue(stat, statObsType, i);
                    if (floatValue == NcMem.FLOAT_FILL_VALUE)
                        continue;

                    value = floatValue;
                } else if (valueType.equals("java.lang.Double")) {
                    double doubleValue = getDoubleValue(stat, statObsType, i);
                    if (doubleValue == NcMem.DOUBLE_FILL_VALUE)
                        continue;

                    value = doubleValue;
                } else {
                    logger.error("Unexpected data type encountered");
                }

                int obsTypeId = obsTypeDao.getObsTypeId(segTypeMap.get(statObsType));
                int sourceId = 2; // from VDT
                int sensorId = sensorDao.getSensorId(sourceId, platformCode, obsTypeId, 0);

                if (sensorId != -1) {
                    obsKey = new ObsKey(sourceId, sensorId, obsTypeId, obsTime);
                    synchronized (obsMap) {
                        if (obsMap.get(obsKey) == null) {
                            obs = new Observation(obsTypeId, sourceId, sensorId, obsTime, recvTime, latitude, longitude,
                                    elevation, value, null, (float) 1.0);  // confValue is set to 1 for now

                            obsMap.put(obsKey, obs);

                            obsList.add(obs);

                            // Need to trigger WxDE to load these observations into memory
                            platformMonitor.updatePlatform(obs);
                        }
                    }
                }
            }
        }
        observationDao.insertObservations(obsList);
    }

    private void persistSegmentAssessment(int contribId) {
        Observation obs = null;
        ObsKey obsKey = null;
        ArrayList<Observation> obsList = new ArrayList<>();
        int recNum = segAssessment.getRecNum();
        HashMap<String, TimeInvariantMetadata> segmentMap = SegmentDao.getInstance().getSegmentMap();

        for (int i = 0; i < recNum; i++) {

            // First collect all the basic fields obs_time, recv_time, vehicle_id, etc.
            long obsTime = System.currentTimeMillis();
            long recvTime = obsTime;
            short elevation = 0; // no elevation information for segment
            String segId = String.valueOf(getIntegerValue(segAssessment, "road_segment_id", i));

            String key = contribId + "-" + segId;
            String platformCode = ((Segment) segmentMap.get(key)).getSegmentName();
            IPlatform platform = PlatformDao.getInstance().getPlatformForContribId(contribId, platformCode);
            int latitude = MathUtil.toMicro(platform.getLocBaseLat());
            int longitude = MathUtil.toMicro(platform.getLocBaseLong());

            // look for individual obs types and create corresponding observations
            HashMap<String, Class> statObsTypes = segAssessment.getVariableMap();
            for (String statObsType : statObsTypes.keySet()) {
                logger.debug(statObsType);

                String valueType = statObsTypes.get(statObsType).getName();
                double value = 0;

                if (valueType.equals("java.lang.Short")) {
                    short shortValue = getShortValue(segAssessment, statObsType, i);
                    if (shortValue == NcMem.SHORT_FILL_VALUE)
                        continue;

                    value = shortValue;
                } else {
                    logger.error("Unexpected data type encountered");
                }

                int obsTypeId = obsTypeDao.getObsTypeId(segTypeMap.get(statObsType));
                int sourceId = 2; // from VDT
                int sensorId = sensorDao.getSensorId(sourceId, platformCode, obsTypeId, 0);

                if (sensorId != -1) {
                    obsKey = new ObsKey(sourceId, sensorId, obsTypeId, obsTime);
                    synchronized (obsMap) {
                        if (obsMap.get(obsKey) == null) {
                            obs = new Observation(obsTypeId, sourceId, sensorId, obsTime, recvTime, latitude, longitude,
                                    elevation, value, null, (float) 1.0);  // confValue is set to 1 for now

                            obsMap.put(obsKey, obs);

                            obsList.add(obs);

                            // Need to trigger WxDE to load these observations into memory
                            platformMonitor.updatePlatform(obs);
                        }
                    }
                }
            }
        }
        observationDao.insertObservations(obsList);
    }

    private char[] getQcFlags(String vdtObsType, int index, short filtering) {

        // create airTemp observation
        ObsQcMap obsQcMap = probeTypeMap.get(vdtObsType);
        char[] qchCharFlag = getDefaultQcFlags();
        HashMap<String, Integer> qcMap = obsQcMap.getQcMap();
        for (String key : qcMap.keySet()) {
            short qcValue = getShortValue(qcProbeMessage, key, index);
            int position = qcMap.get(key).intValue();
            qchCharFlag[position] = getQcFlag(qcValue);
        }
        qchCharFlag[FILTERING] = getQcFlag(filtering);

        return qchCharFlag;
    }

    private char getQcFlag(short qcValue) {
        char flag = '/';
        switch (qcValue) {
            case NcMem.QC_FILL_VALUE:
                flag = '/';
                break;
            case 0:
                flag = 'N';
                break;
            case 1:
                flag = 'P';
                break;
            default:
                logger.error("Invalid QC value encountered: " + qcValue);
                break;
        }

        return flag;
    }

    private char[] getDefaultQcFlags() {
        char[] qchCharFlag = new char[QC_FLAGS];
        for (int i = 0; i < QC_FLAGS; i++)
            qchCharFlag[i] = '/';

        return qchCharFlag;
    }

    private double getDoubleValue(NcMem ncMem, String varName, int index) {
        logger.debug("varName: " + varName + " index: " + index);
        NetCdfArrayList<Double> valueList = (NetCdfArrayList<Double>) ncMem.getList(varName);
        return valueList.get(index).doubleValue();
    }

    private float getFloatValue(NcMem ncMem, String varName, int index) {
        logger.debug("varName: " + varName + " index: " + index);
        NetCdfArrayList<Float> valueList = (NetCdfArrayList<Float>) ncMem.getList(varName);
        return valueList.get(index).floatValue();
    }

    private int getIntegerValue(NcMem ncMem, String varName, int index) {
        logger.debug("varName: " + varName + " index: " + index);
        NetCdfArrayList<Integer> valueList = (NetCdfArrayList<Integer>) ncMem.getList(varName);
        return valueList.get(index).intValue();
    }

    private short getShortValue(NcMem ncMem, String varName, int index) {
        logger.debug("varName: " + varName + " index: " + index);
        NetCdfArrayList<Short> valueList = (NetCdfArrayList<Short>) ncMem.getList(varName);
        return valueList.get(index).shortValue();
    }

    private String getStringValue(NcMem ncMem, String varName, int index) {
        logger.debug("varName: " + varName + " index: " + index);
        NetCdfArrayList<String> valueList = (NetCdfArrayList<String>) ncMem.getList(varName);
        return valueList.get(index);
    }
}
