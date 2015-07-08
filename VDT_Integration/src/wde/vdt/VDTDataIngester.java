/************************************************************************
 * Source filename: VDTDataIngester.java
 * 
 * Creation date: Oct 24, 2013
 * 
 * Author: zhengg
 * 
 * Project: VDT Integration
 * 
 * Objective:
 * 
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Properties;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import org.apache.log4j.xml.DOMConfigurator;

import ucar.ma2.Array;

import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

import wde.dao.ObsTypeDao;
import wde.dao.ObservationDao;
import wde.dao.SensorDao;
import wde.obs.ObsKey;
import wde.obs.Observation;
import wde.util.MathUtil;
import wde.vdt.NetCdfArrayList;

import wde.vdt.probe.ObsQcMap;
import wde.vdt.probe.ProbeMessage;
import wde.vdt.probe.QcProbeMessage;

public class VDTDataIngester {
    
    private static final Logger logger = Logger.getLogger(VDTDataIngester.class);
    
    private static VDTDataIngester instance = null;
    
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

    private Properties prop = null;
    
    private HashMap<String, ObsQcMap> typeMap = null;
    
    private String outputRoot;
    private String outputPrefix;
    private String outputFileExtension;
    
    private String fileDelimiter;
    
    private String dateFormatStr;
    private String timeFormatStr;

    private SimpleDateFormat dateFormat = null;
    private SimpleDateFormat tsFormat = null;
    private QcProbeMessage qcProbeMessage = null;
    
    private ObsTypeDao obsTypeDao = null;
    private SensorDao sensorDao = null;
    private ObservationDao observationDao = null;
    private TreeMap<ObsKey, Observation> obsMap = null;
    
    public static void main(String[] args)
    {
        DOMConfigurator.configure("config/vdt_log4j.xml");
        VDTDataIngester.getInstance();
    }

    public static VDTDataIngester getInstance()
    {
        if (instance == null)
            instance = new VDTDataIngester();
        
        return instance;
    }
    
    public VDTDataIngester()
    {
        prop = new Properties();
        loadPropertiesFile();
        
        populateObsQcMaps();
        obsTypeDao = ObsTypeDao.getInstance();
        sensorDao = SensorDao.getInstance();
        observationDao = ObservationDao.getInstance();
        
        dateFormat = new SimpleDateFormat(dateFormatStr);
        
        String df = dateFormatStr + fileDelimiter + timeFormatStr; 

        tsFormat = new SimpleDateFormat(df);
        obsMap = new TreeMap<>();
    }
    
    public void processData(GregorianCalendar now) throws Exception
    {     
        Date oDate = now.getTime();
        String postfix = fileDelimiter + tsFormat.format(now.getTime()) + fileDelimiter;
        String ncFilePath = getNcFilePath(outputRoot, outputPrefix, oDate, postfix + outputFileExtension);
        populateQcProbeMessage(ncFilePath);
        populateDatabase();
    }
    
    public void cleanup(long expirationTime)
    {
        int count = 0;
        synchronized (obsMap) {
            for (ObsKey key : obsMap.keySet()) {
                if (key.getObsTime() <= expirationTime) {
                    obsMap.put(key,  null);
                    count++;
                }
            }
        }
        logger.info("Cleaned up " + count + " old observations");
    }

    private String getNcFilePath(String root, String sPrefix, Date date, String postfix)
    {
        String separator = System.getProperty("file.separator");
        
        StringBuilder filename = new StringBuilder(root);
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
    private void loadPropertiesFile()
    {
        logger.info("Loading properties file");
        
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "vdt_config.properties";
    
        try
        {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();
            
            outputRoot = prop.getProperty("outputroot");
            outputPrefix = prop.getProperty("outputprefix");
            outputFileExtension = prop.getProperty("outputfileextension");
            
            fileDelimiter = prop.getProperty("filedelimiter");
            dateFormatStr = prop.getProperty("dateformat");
            timeFormatStr = prop.getProperty("timeformat");
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private void populateQcProbeMessage(String filePath) throws IOException 
    {
        qcProbeMessage = new QcProbeMessage();

        NetcdfFile ncfile = null;

        ncfile = NetcdfFile.open(filePath);

        HashMap<String, Class> variableMap = qcProbeMessage.getVariableMap();
        for (String varName : variableMap.keySet()) {
            logger.info(varName);
            Variable v = ncfile.findVariable(varName);
            if (v == null) {
                logger.error(varName + " not found");
                ncfile.close();
                return;
            }
            try {
                Array data = v.read();
                String className = qcProbeMessage.getVariableMap().get(varName).toString();
                if (className.contains("Double"))
                    qcProbeMessage.populateDoubleArrayVariable(varName, data);
                else if (className.contains("Float"))
                    qcProbeMessage.populateFloatArrayVariable(varName, data);
                else if (className.contains("Int"))
                    qcProbeMessage.populateIntArrayVariable(varName,  data);
                else if (className.contains("Short"))
                    qcProbeMessage.populateShortArrayVariable(varName,  data);
                else if (className.contains("String"))
                    qcProbeMessage.populateStringArrayVariable(varName,  data);
            }
            catch (IOException ioe) {
                logger.error("trying to read " + varName, ioe);
            }
        }
        ncfile.close();
    }
    
    private void populateObsQcMaps() {
        
        typeMap = new HashMap<>();
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
        typeMap.put(vdtObsType, obsQcMap);
        
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
        typeMap.put(vdtObsType, obsQcMap);
        
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
        obsQcMap = new ObsQcMap("essAtmosphericPressure", "short", 0, qcMap);
        typeMap.put("bar_pressure", obsQcMap);        
        
        vdtObsType = "dew_temp";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_crt_passed", new Integer(CRT));
        qcMap.put(vdtObsType + "_nst_passed", new Integer(NST));
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        qcMap.put(vdtObsType + "_qc_passed", new Integer(OVERALL));   
        obsQcMap = new ObsQcMap("essDewpointTemp", "float", 1, qcMap);
        typeMap.put(vdtObsType, obsQcMap); 
        
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
        typeMap.put(vdtObsType, obsQcMap); 
        
        vdtObsType = "wiper_status";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("mobileWipers", "short", 1, qcMap);
        typeMap.put(vdtObsType, obsQcMap); 
        
        vdtObsType = "lights";
        qcMap = new HashMap<>();
        qcMap.put(vdtObsType + "_range_qc_passed", new Integer(RANGE));
        obsQcMap = new ObsQcMap("mobileHeadlights", "short", 1, qcMap);
        typeMap.put(vdtObsType, obsQcMap); 
        
        vdtObsType = "humidity";
        qcMap = new HashMap<>();
        obsQcMap = new ObsQcMap("essRelativeHumidity", "float", 1, qcMap);
        typeMap.put(vdtObsType, obsQcMap); 
    }

    private void populateDatabase() {
        Observation obs = null;
        ObsKey obsKey = null;
        char[] qchCharFlag = null;
        ArrayList<Observation> obsList = new ArrayList<>();
        int recNum = qcProbeMessage.getRecNum();
        for (int i = 0; i < recNum; i++) {
            
            // First collect all the basic fields obs_time, recv_time, vehicle_id, etc.
            long obsTime = (long) getDoubleValue("obs_time", i) * 1000;
            long recvTime = (long) getDoubleValue("rec_time", i) * 1000;
            int latitude = MathUtil.toMicro(getDoubleValue("latitude", i));
            short filtering = getShortValue("latitude_dft_passed", i); // assume longitude has the same value
            int longitude = MathUtil.toMicro(getDoubleValue("longitude", i));
            short elevation = (short)getFloatValue("elevation", i);
            String platformCode = getStringValue("vehicle_id", i);
            
            // look for individual obs types and create corresponding observations
            for (String vdtObsType : typeMap.keySet()) {
                logger.info(vdtObsType);
                ObsQcMap obsQcMap = typeMap.get(vdtObsType);
                String valueType = obsQcMap.getValueType();
                double value = 0;
                
                if (valueType.equals("short")) {
                    short shortValue = getShortValue(vdtObsType, i);
                    if (shortValue == ProbeMessage.SHORT_FILL_VALUE)
                        continue;
                    
                    value = shortValue;
                }
                else if (valueType.equals("float")) {
                    float floatValue = getFloatValue(vdtObsType, i);
                    if (floatValue == ProbeMessage.FLOAT_FILL_VALUE)
                        continue;
                    
                    value = floatValue;
                }
                else {
                    logger.error("Unexpected data type encountered");
                }

                qchCharFlag = getQcFlags(vdtObsType, i, filtering);

                int obsTypeId = obsTypeDao.getObsTypeId(obsQcMap.getObsType());
                int sourceId = 2;
                int sensorId = sensorDao.getSensorId(platformCode, obsTypeId, obsQcMap.getSensorIndex());
                
                obsKey = new ObsKey(sourceId, sensorId, obsTypeId, obsTime); 
                
                synchronized (obsMap) {
                    if (obsMap.get(obsKey) == null) {
                        obs = new Observation(obsTypeId, sourceId, sensorId, obsTime, recvTime, latitude, longitude,
                            elevation, value, qchCharFlag, (float) 1.0);  // confValue is set to 1 for now
//                        obs = new Observation(obsTypeId, sourceId, sensorId, now.getTime(), now.getTime(), latitude, longitude,
//                            elevation, value, qchCharFlag, (float) 1.0);  // confValue is set to 1 for now
                    
                        obsMap.put(obsKey, obs);
                        
                        obsList.add(obs);
                    }
                }
            }
        }  
        observationDao.insertObservations(obsList);
        
        // Need to trigger WxDE to load these observations into memory
        // Call StationMonitor updatePlatform(IObs)
    }
    
    private char[] getQcFlags(String vdtObsType, int index, short filtering) {
        
        // create airTemp observation
        ObsQcMap obsQcMap = typeMap.get(vdtObsType);
        char[] qchCharFlag = getDefaultQcFlags();
        HashMap<String, Integer> qcMap = obsQcMap.getQcMap();
        for (String key : qcMap.keySet()) {
            short qcValue = getShortValue(key, index);
            int position = qcMap.get(key).intValue();
            qchCharFlag[position] = getQcFlag(qcValue);
        }
        qchCharFlag[FILTERING] = getQcFlag(filtering);
        
        return qchCharFlag;
    }
    
    private char getQcFlag(short qcValue) {
        char flag = '/';
        switch (qcValue) {
        case ProbeMessage.QC_FILL_VALUE: 
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
    
    private double getDoubleValue(String varName, int index) {
        logger.info("varName: " + varName + " index: " + index);
        NetCdfArrayList<Double> valueList = (NetCdfArrayList<Double>) qcProbeMessage.getList(varName); 
        return valueList.get(index).doubleValue();
    }
    
    private float getFloatValue(String varName, int index) {
        logger.info("varName: " + varName + " index: " + index);
        NetCdfArrayList<Float> valueList = (NetCdfArrayList<Float>) qcProbeMessage.getList(varName); 
        return valueList.get(index).floatValue();
    }
    
    private short getShortValue(String varName, int index) {
        logger.info("varName: " + varName + " index: " + index);
        NetCdfArrayList<Short> valueList = (NetCdfArrayList<Short>) qcProbeMessage.getList(varName); 
        return valueList.get(index).shortValue();
    }
    
    private String getStringValue(String varName, int index) {
        logger.info("varName: " + varName + " index: " + index);
        NetCdfArrayList<String> valueList = (NetCdfArrayList<String>) qcProbeMessage.getList(varName); 
        return valueList.get(index);
    }
}
