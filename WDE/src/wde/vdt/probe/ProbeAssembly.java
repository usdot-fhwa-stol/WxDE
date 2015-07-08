/************************************************************************
 * Source filename: ProbeAssembly.java
 * <p/>
 * Creation date: Oct 7, 2013
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
import org.apache.log4j.xml.DOMConfigurator;
import ucar.ma2.*;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.vdt.NetCdfArrayList;
import wde.vdt.VDTController;
import wde.vdt.probe.raw.ameritrak.*;
import wde.vdt.probe.raw.umtri.UmtriMessage;

import java.io.*;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.*;

public class ProbeAssembly {

    private static final Logger logger = Logger.getLogger(ProbeAssembly.class);
    public static boolean wdeMgrInstantiated = false;
    private static ProbeAssembly instance = null;
    private Properties prop = null;

    private String rawIMO;
    private String rawMN;
    private String rawMNPrefix;
    private String rawMNFileExtension;
    private String miUrl;
    private String rawMIFileExtension;
    private String miMonthFormatStr;
    private String miDateFormatStr;

    private String processedRoot;
    private String processedPrefix;
    private String processedFileExtension;
    private String separator;

    private SimpleDateFormat dateFormat = null;
    private SimpleDateFormat miMonthFormat = null;
    private SimpleDateFormat miDateFormat = null;
    private SimpleDateFormat tsFormat = null;
    private NetcdfFileWriteable ncFile = null;
    private InputProbeMessage inputProbeMessage = null;

    private ProbeAssembly() {
        prop = new Properties();
        loadPropertiesFile();
        separator = System.getProperty("file.separator");

        dateFormat = new SimpleDateFormat(VDTController.dateFormatStr);
        miMonthFormat = new SimpleDateFormat(miMonthFormatStr);
        miDateFormat = new SimpleDateFormat(miDateFormatStr);

        String df = VDTController.dateFormatStr + VDTController.fileDelimiter + VDTController.timeFormatStr;

        tsFormat = new SimpleDateFormat(df);
    }

    public static void main(String[] args) {
        DOMConfigurator.configure("config/vdt_log4j.xml");
        ProbeAssembly.getInstance();
    }

    public static ProbeAssembly getInstance() {
        if (instance == null)
            instance = new ProbeAssembly();

        return instance;
    }

    public String[] generateMNProbeMessage(GregorianCalendar now) {
        String[] filePaths = new String[2];

        Date oDate = now.getTime();
        String postfix = VDTController.fileDelimiter + tsFormat.format(now.getTime()) + VDTController.fileDelimiter;

        inputProbeMessage = new InputProbeMessage();

        try {
            BufferedReader fileReader = createFileReader(rawIMO, rawMN, rawMNPrefix, oDate, postfix + rawMNFileExtension);
            populateInputProbeMessageForMN(fileReader);
            fileReader.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        String ncFilePath = getFilePath(processedRoot, oDate, processedPrefix, postfix + processedFileExtension);
        filePaths[0] = ncFilePath;
        filePaths[1] = getFilePath(processedRoot + separator + "MN", oDate, processedPrefix, postfix + processedFileExtension);

        try {
            logger.info("Creating NetCDF file for MN: " + ncFilePath);
            createNcFile(ncFilePath);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return filePaths;
    }

    public String[] generateMIProbeMessage(GregorianCalendar now) {
        String[] filePaths = new String[2];

        Date oDate = now.getTime();
        String postfix = VDTController.fileDelimiter + tsFormat.format(now.getTime()) + VDTController.fileDelimiter;

        inputProbeMessage = new InputProbeMessage();

        try {
            String urlStr = miUrl + miMonthFormat.format(oDate) + "/" +
                    miDateFormat.format(oDate) + VDTController.fileDelimiter + rawMIFileExtension;

            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();
            BufferedReader fileReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

            logger.info("Invoking populateInputProbeMessageForMI for " + urlStr);

            populateInputProbeMessageForMI(fileReader);
            fileReader.close();
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        String ncFilePath = getFilePath(processedRoot, oDate, processedPrefix, postfix + processedFileExtension);
        filePaths[0] = ncFilePath;
        filePaths[1] = getFilePath(processedRoot + separator + "MI", oDate, processedPrefix, postfix + processedFileExtension);
        try {
            logger.info("Creating NetCDF file for MI: " + ncFilePath);
            createNcFile(ncFilePath);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        return filePaths;
    }

    private BufferedReader createFileReader(String rootFolder, String subFolder, String sPrefix, Date date, String postfix)
            throws Exception {
        BufferedReader br = null;
        String separator = System.getProperty("file.separator");

        StringBuilder filename = new StringBuilder(rootFolder);
        filename.append(subFolder);
        filename.append(sPrefix);
        filename.append(separator);
        filename.append(dateFormat.format(date));
        new File(filename.toString()).mkdirs();

        filename.append(separator);
        filename.append(sPrefix);
        filename.append(postfix);

        logger.info("Preparing to read: " + filename.toString());

        br = new BufferedReader(new FileReader(filename.toString()));

        return br;
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

            String separator = System.getProperty("file.separator");
            String path = System.getProperty("user.dir") + separator + "config" + separator + "vdt_config.properties";

            try {
                FileInputStream fis = new FileInputStream(path);
                prop.load(fis);
                fis.close();

                rawIMO = prop.getProperty("rawroot");
                rawMN = prop.getProperty("rawmn");
                rawMNPrefix = prop.getProperty("rawmnprefix");
                rawMNFileExtension = prop.getProperty("rawmnfileextension");
                miUrl = prop.getProperty("miurl");
                rawMIFileExtension = prop.getProperty("rawmifileextension");
                miMonthFormatStr = prop.getProperty("mimonthformat");
                miDateFormatStr = prop.getProperty("midateformat");

                processedRoot = prop.getProperty("processedroot");
                processedPrefix = prop.getProperty("processedprefix");
                processedFileExtension = prop.getProperty("processedfileextension");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            ConfigSvc configSvc = ConfigSvc.getInstance();
            Config config = configSvc.getConfig(this);

            rawIMO = config.getString("rawimo", "");
            rawMN = config.getString("rawmn", "");
            rawMNPrefix = config.getString("rawmnprefix", "");
            rawMNFileExtension = config.getString("rawmnfileextension", "");
            miUrl = config.getString("miurl", "");
            rawMIFileExtension = config.getString("rawmifileextension", "");
            miMonthFormatStr = config.getString("mimonthformat", "");
            miDateFormatStr = config.getString("midateformat", "");

            processedRoot = config.getString("processedroot", "");
            processedPrefix = config.getString("processedprefix", "");
            processedFileExtension = config.getString("processedfileextension", "");
        }
    }

    private void populateInputProbeMessageForMN(BufferedReader br) {
        String str = null;
        try {
            while ((str = br.readLine()) != null) {
                // Skip empty line and the title line
                if (str.length() == 0)
                    continue;

                // Remove the last semicolon
                str = str.substring(0, str.length() - 1);
                String str1 = str.substring(str.indexOf(">"));

                if (str1.startsWith(">ActX:")) {
                    ActX actX = (ActX) populateAmeritrakMessage(str, "ActX");
                    processActXMessage(actX);
                } else if (str1.startsWith(">CanX:")) {
                    CanX canX = (CanX) populateAmeritrakMessage(str, "CanX");
                    processCanXMessage(canX);
                } else if (str1.startsWith(">ObdY:")) {
                    ObdY obdY = (ObdY) populateAmeritrakMessage(str, "ObdY");
                    processObdYMessage(obdY);
                } else if (str1.startsWith(">VaiX:")) {
                    VaiX vaiX = (VaiX) populateAmeritrakMessage(str, "VaiX");
                    processVaiXMessage(vaiX);
                }
            }

            logger.info("populated probe messages for MN");

        } catch (Exception e) {
            logger.error("*=*= " + e.getMessage());
            e.printStackTrace();
        }
    }

    private AmeritrakMessage populateAmeritrakMessage(String str, String className) {
        int delimiterPosition = str.indexOf('>');
        String recvTime = str.substring(0, delimiterPosition);
        str = str.substring(delimiterPosition);

        AmeritrakMessage message = null;
        if (str.startsWith(">" + className + ":")) {
            str.replace(">" + className + ":", ">" + className + ",");
            String[] strArray = str.split("\\,");
            try {
                Class tempClass = Class.forName("wde.vdt.probe.raw.ameritrak." + className);
                Constructor<AmeritrakMessage> ctor = tempClass.getDeclaredConstructor(String.class, String[].class);
                message = ctor.newInstance(recvTime, (Object) strArray);
                inputProbeMessage.registerObsTimeVname(message.getAcqTime(), message.getvName());
                logger.debug("populated " + className + " and registered " + message.getAcqTime());
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
        return message;
    }

    private void processAmeritrakMessage(AmeritrakMessage msg) {
        String obsTimeStrVname = msg.getAcqTime() + "|" + msg.getvName();
        inputProbeMessage.updateValue("obs_time", obsTimeStrVname, msg.getObsTime());
        inputProbeMessage.updateValue("rec_time", obsTimeStrVname, msg.getRecTime());
        inputProbeMessage.updateValue("latitude", obsTimeStrVname, msg.getLat());
        inputProbeMessage.updateValue("longitude", obsTimeStrVname, msg.getLon());
        inputProbeMessage.updateValue("vehicle_id", obsTimeStrVname, msg.getvName());
        inputProbeMessage.updateValue("speed", obsTimeStrVname, msg.getVel());
        inputProbeMessage.updateValue("heading", obsTimeStrVname, msg.getCourse());
    }

    private void processActXMessage(ActX actX) {
        processAmeritrakMessage(actX);
        String obsTimeStrVname = actX.getAcqTime() + "|" + actX.getvName();
        if (!actX.getField(ActX.act1).equals("null"))
            inputProbeMessage.updateValue("lights", obsTimeStrVname, (short) Float.parseFloat(actX.getField(ActX.act1)));
        if (!actX.getField(ActX.act2).equals("null"))
            inputProbeMessage.updateValue("wiper_status", obsTimeStrVname, Short.parseShort(actX.getField(ActX.act2)));

        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_actx");
    }

    private void processCanXMessage(CanX canX) {
        processAmeritrakMessage(canX);
        String obsTimeStrVname = canX.getAcqTime() + "|" + canX.getvName();
        inputProbeMessage.updateValue("abs", obsTimeStrVname, canX.getAbs());

        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_canx");
    }

    private void processObdYMessage(ObdY obdY) {
        processAmeritrakMessage(obdY);
        String obsTimeStrVname = obdY.getAcqTime() + "|" + obdY.getvName();
        if (obdY.getField(ObdY.BP) != null && !obdY.getField(ObdY.BP).equals("null"))
            inputProbeMessage.updateValue("brake_status", obsTimeStrVname, Short.parseShort(obdY.getField(ObdY.BP)));
        if (obdY.getField(ObdY.AIRT) != null && !obdY.getField(ObdY.AIRT).equals("null")) {
            short tempValue = (short) ((Short.parseShort(obdY.getField(ObdY.AIRT)) - 32) / 1.8);
            inputProbeMessage.updateValue("air_temp", obsTimeStrVname, tempValue);
        }
        if (obdY.getField(ObdY.BPRS) != null && !obdY.getField(ObdY.BPRS).equals("null"))
            inputProbeMessage.updateValue("bar_pressure", obsTimeStrVname, Short.parseShort(obdY.getField(ObdY.BPRS)));
        if (obdY.getField(ObdY.TA) != null && !obdY.getField(ObdY.TA).equals("null"))
            inputProbeMessage.updateValue("trac", obsTimeStrVname, Short.parseShort(obdY.getField(ObdY.TA)));
        if (obdY.getField(ObdY.YAW) != null && !obdY.getField(ObdY.YAW).equals("null"))
            inputProbeMessage.updateValue("yaw_rate", obsTimeStrVname, Float.parseFloat(obdY.getField(ObdY.YAW)));
        if (obdY.getField(ObdY.LNAC) != null && !obdY.getField(ObdY.LNAC).equals("null"))
            inputProbeMessage.updateValue("hoz_accel_long", obsTimeStrVname, Float.parseFloat(obdY.getField(ObdY.LNAC)));
        if (obdY.getField(ObdY.LTAC) != null && !obdY.getField(ObdY.LTAC).equals("null"))
            inputProbeMessage.updateValue("hoz_accel_lat", obsTimeStrVname, Float.parseFloat(obdY.getField(ObdY.LTAC)));
        if (obdY.getField(ObdY.SA) != null && !obdY.getField(ObdY.SA).equals("null"))
            inputProbeMessage.updateValue("steering_angle", obsTimeStrVname, Float.parseFloat(obdY.getField(ObdY.SA)));

        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_obdy");
    }

    private void processVaiXMessage(VaiX vaiX) {
        processAmeritrakMessage(vaiX);
        String obsTimeStrVname = vaiX.getAcqTime() + "|" + vaiX.getvName();
        if (!vaiX.getField(VaiX.tempAir).equals("null")) {
            float tempValue = (float) ((Float.parseFloat(vaiX.getField(VaiX.tempAir)) - 32) / 1.8);
            inputProbeMessage.updateValue("air_temp2", obsTimeStrVname, tempValue);
        }
        if (!vaiX.getField(VaiX.dewPt).equals("null")) {
            float tempValue = (float) ((Float.parseFloat(vaiX.getField(VaiX.dewPt)) - 32) / 1.8);
            inputProbeMessage.updateValue("dew_temp", obsTimeStrVname, tempValue);
        }
        if (!vaiX.getField(VaiX.humid).equals("null"))
            inputProbeMessage.updateValue("humidity", obsTimeStrVname, Float.parseFloat(vaiX.getField(VaiX.humid)));
        if (!vaiX.getField(VaiX.tempRoad).equals("null")) {
            float tempValue = (float) ((Float.parseFloat(vaiX.getField(VaiX.tempRoad)) - 32) / 1.8);
            inputProbeMessage.updateValue("surface_temp", obsTimeStrVname, tempValue);
        }

        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_vaix");
    }

    private void populateInputProbeMessageForMI(BufferedReader br) {
        String str = null;
        Date recTime = Calendar.getInstance().getTime();
        try {
            while ((str = br.readLine()) != null) {
                // Skip empty line and the title line
                if (str.length() == 0)
                    continue;

                processUmtriMessage(recTime, str);
            }

            logger.info("populated probe messages for MI");

        } catch (Exception e) {
            logger.error("*=*= " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processUmtriMessage(Date recTime, String str) {
        logger.info("processUmtriMessage for " + str);

        UmtriMessage message = null;

        String[] strArray = str.split("\\,");
        message = new UmtriMessage(recTime, strArray);
        String currentTime = message.getCurrentTime();
        if (currentTime == null) {
            logger.error("encountered invalid current time in " + str);
            return;
        }
        inputProbeMessage.registerObsTimeVname(currentTime, message.getVin());
        logger.info("populated UMTRI message and registered " + currentTime + " " + message.getVin());

        String obsTimeStrVname = currentTime + "|" + message.getVin();
        inputProbeMessage.updateValue("vehicle_id", obsTimeStrVname, message.getVin());
        inputProbeMessage.updateValue("obs_time", obsTimeStrVname, message.getObsTime());
        inputProbeMessage.updateValue("rec_time", obsTimeStrVname, message.getRecTime());
        inputProbeMessage.updateValue("latitude", obsTimeStrVname, message.getLatitudePosition());
        inputProbeMessage.updateValue("longitude", obsTimeStrVname, message.getLongitudePosition());

        if (message.getAltitude().length() > 0)
            inputProbeMessage.updateValue("elevation", obsTimeStrVname, Float.parseFloat(message.getAltitude()));

        if (message.getSpeed().length() > 0)
            inputProbeMessage.updateValue("speed", obsTimeStrVname, Float.parseFloat(message.getSpeed()));

        if (message.getBrakes().length() > 0)
            inputProbeMessage.updateValue("brake_status", obsTimeStrVname, Short.parseShort(message.getBrakes()));

        if (message.getAirTemperatureFromCan().length() > 0) {
            short tempValue = (short) ((Float.parseFloat(message.getAirTemperatureFromCan()) - 32) / 1.8);
            inputProbeMessage.updateValue("air_temp", obsTimeStrVname, tempValue);
        }

        if (message.getBarometriPressureFromCan().length() > 0)
            inputProbeMessage.updateValue("bar_pressure", obsTimeStrVname, (short) Float.parseFloat(message.getBarometriPressureFromCan()));

        if (message.getTractionControlBrakingEvent().length() > 0)
            inputProbeMessage.updateValue("trac", obsTimeStrVname, Short.parseShort(message.getTractionControlBrakingEvent()));

        if (message.getElectronicStabilityControl().length() > 0)
            inputProbeMessage.updateValue("stab", obsTimeStrVname, Short.parseShort(message.getElectronicStabilityControl()));

        if (message.getAntilockBrakingSystem().length() > 0)
            inputProbeMessage.updateValue("abs", obsTimeStrVname, Short.parseShort(message.getAntilockBrakingSystem()));

        if (message.getCompassHeading().length() > 0)
            inputProbeMessage.updateValue("heading", obsTimeStrVname, Float.parseFloat(message.getCompassHeading()));

        if (message.getAmbientAirTemperature().length() > 0) {
            float tempValue = (float) ((Float.parseFloat(message.getAmbientAirTemperature()) - 32) / 1.8);
            inputProbeMessage.updateValue("air_temp2", obsTimeStrVname, tempValue);
        }

        if (message.getDewPoint().length() > 0) {
            float tempValue = (float) ((Float.parseFloat(message.getDewPoint()) - 32) / 1.8);
            inputProbeMessage.updateValue("dew_temp", obsTimeStrVname, tempValue);
        }

        if (message.getHumidity().length() > 0)
            inputProbeMessage.updateValue("humidity", obsTimeStrVname, Float.parseFloat(message.getHumidity()));

        if (message.getSurfaceTemp().length() > 0) {
            float tempValue = (float) ((Float.parseFloat(message.getSurfaceTemp()) - 32) / 1.8);
            inputProbeMessage.updateValue("surface_temp", obsTimeStrVname, tempValue);
        }
    }

    private void createNcFile(String filepath) throws IOException {

        ncFile = NetcdfFileWriteable.createNew(filepath, false);

        Dimension recNumDim = ncFile.addUnlimitedDimension("rec_num");
        Dimension vehicleIdLenDim = ncFile.addDimension("vehicle_id_len", 32);
        Dimension sourceIdLenDim = ncFile.addDimension("source_id_len", 32);
        String varName = null;

        varName = "vehicle_id";
        ArrayList<Dimension> dims = new ArrayList<>();
        dims.add(recNumDim);
        dims.add(vehicleIdLenDim);
        ncFile.addVariable(varName, DataType.CHAR, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Vehicle Identifier");

        varName = "source_id";
        dims = new ArrayList<>();
        dims.add(recNumDim);
        dims.add(sourceIdLenDim);
        ncFile.addVariable(varName, DataType.CHAR, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Source Identifier");

        varName = "speed";
        dims = new ArrayList<>();
        dims.add(recNumDim);
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Vehicle Speed");
        String[] speedRange = {"-327.65", "327.65"};
        Array data = Array.makeArray(DataType.DOUBLE, speedRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m/s");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "brake_status";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Brake Applied Status");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "all_off", 0);
        ncFile.addVariableAttribute(varName, "rr_active", 8);
        ncFile.addVariableAttribute(varName, "rf_active", 4);
        ncFile.addVariableAttribute(varName, "lr_active", 2);
        ncFile.addVariableAttribute(varName, "lf_active", 1);
        ncFile.addVariableAttribute(varName, "all_on", 15);

        varName = "brake_boost";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Brake Boost Applied Status");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "OFF", 1);
        ncFile.addVariableAttribute(varName, "ON", 2);

        varName = "wiper_status";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Front Wiper Status");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "intermittent", 2);
        ncFile.addVariableAttribute(varName, "low", 3);
        ncFile.addVariableAttribute(varName, "high", 4);
        ncFile.addVariableAttribute(varName, "washer", 5);
        ncFile.addVariableAttribute(varName, "automaticPresent", 255);

        varName = "air_temp";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Ambient Air Temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "Temperature");
        String[] tempRange = {"-40", "151"};
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "trac";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Traction Control State");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "on", 2);
        ncFile.addVariableAttribute(varName, "engaged", 3);

        varName = "stab";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Stability Control Status");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "on", 2);
        ncFile.addVariableAttribute(varName, "engaged", 3);

        varName = "abs";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Anti-lock Brake Status");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "on", 2);
        ncFile.addVariableAttribute(varName, "engaged", 3);

        varName = "lights";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Exterior Lights");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "parkingLightsOn", 128);
        ncFile.addVariableAttribute(varName, "fogLightOn", 64);
        ncFile.addVariableAttribute(varName, "daytimeRunningLightsOn", 32);
        ncFile.addVariableAttribute(varName, "automaticLightControlOn", 16);
        ncFile.addVariableAttribute(varName, "leftTurnSignalOn", 4);
        ncFile.addVariableAttribute(varName, "highBeamHeadlightsOn", 2);
        ncFile.addVariableAttribute(varName, "lowBeamHeadlightsOn", 1);
        ncFile.addVariableAttribute(varName, "hazardSignalOn", 24);
        ncFile.addVariableAttribute(varName, "allLightsOff", 0);

        varName = "psn";
        ncFile.addVariable(varName, DataType.INT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Probe Segment Number");
        String[] psnRange = {"1", "65535"};
        data = Array.makeArray(DataType.INT, psnRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.INT_FILL_VALUE);

        varName = "heading";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Heading");
        String[] headingRange = {"0", "360"};
        data = Array.makeArray(DataType.INT, headingRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "yaw_rate";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Yaw Rate");
        String[] yawRateRange = {"0.0", "655.35"};
        data = Array.makeArray(DataType.DOUBLE, yawRateRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees per second");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "hoz_accel_long";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Longitudinal Acceleration");
        String[] hozAccelRange = {"-20.0", "20.0"};
        data = Array.makeArray(DataType.DOUBLE, hozAccelRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m/s^2");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "hoz_accel_lat";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Lateral Acceleration");
        data = Array.makeArray(DataType.DOUBLE, hozAccelRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m/s^2");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "tire_pressure_lf";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Left Front Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "tire_pressure_rf";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Right Front Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "tire_pressure_lr";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Left Rear Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "tire_pressure_rr";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Right Rear Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "tire_pressure_sp";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Spare Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "steering_angle";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Steering Wheel Angle");
        String[] steeringAngleRange = {"-655.36", "655.36"};
        data = Array.makeArray(DataType.DOUBLE, steeringAngleRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "steering_rate";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Steering Wheel Angle Rate of Change");
        String[] steeringRateRange = {"-381", "381"};
        data = Array.makeArray(DataType.INT, steeringRateRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees/second");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);

        varName = "air_temp2";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "ambient air temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "air_temperature");
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "dew_temp";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "dew temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "dew_temperature");
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "humidity";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "humidity");
        ncFile.addVariableAttribute(varName, "standard_name", "humidity");
        String[] humidityRange = {"0", "100"};
        data = Array.makeArray(DataType.INT, humidityRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "percent");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "surface_temp";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "surface temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "surface_temperature");
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);

        varName = "obs_time";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "observation time");
        ncFile.addVariableAttribute(varName, "units", "seconds since 1970-1-1 00:00");
        ncFile.addVariableAttribute(varName, "_CoordinateAxisType", "Time");

        varName = "rec_time";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "received time");
        ncFile.addVariableAttribute(varName, "units", "seconds since 1970-1-1 00:00");
        ncFile.addVariableAttribute(varName, "_CoordinateAxisType", "Time");

        varName = "latitude";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "obs latitude");
        ncFile.addVariableAttribute(varName, "standard_name", "latitude");
        String[] latRange = {"-90.0", "90.0"};
        data = Array.makeArray(DataType.DOUBLE, latRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees_north");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.DOUBLE_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "_CoordinateAxisType", "Lat");

        varName = "longitude";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "obs longitude");
        ncFile.addVariableAttribute(varName, "standard_name", "longitude");
        String[] longRange = {"-180.0", "180.0"};
        data = Array.makeArray(DataType.DOUBLE, longRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees_east");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.DOUBLE_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "_CoordinateAxisType", "Lon");

        varName = "elevation";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Elevation");
        ncFile.addVariableAttribute(varName, "standard_name", "longitude");
        String[] elevRange = {"-1000", "10000"};
        data = Array.makeArray(DataType.INT, elevRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "_CoordinateAxisType", "Height");

        varName = "bar_pressure";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Barometric Pressure");
        String[] barPresRange = {"580", "1090"};
        data = Array.makeArray(DataType.INT, barPresRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "hPa");
        ncFile.addVariableAttribute(varName, "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "_CoordinateAxisType", "Pressure");

        ncFile.addGlobalAttribute("_CoordSysBuilder", "ucar.nc2.dataset.conv.DefaultConvention");

        // create the file
        try {
            // Create the file. At this point the (empty) file will be written to disk, 
            // and the metadata (Dimensions, Variables and Attributes) is fixed and cannot be changed or added.
            ncFile.create();
        } catch (IOException e) {
            // The ncfile.getLocation() method will return the filename. 
            logger.error("ERROR creating file " + ncFile.getLocation() + "\n" + e);
        }

        // Instead of list.size, should use rec_num.  Debug why none is populated
        int[] sortedFieldPositions = inputProbeMessage.getSortedFieldPositions();

        logger.info("rec_num: " + inputProbeMessage.getRecNum() + " positions: " + sortedFieldPositions.length);

        writeDoubleArray("obs_time", sortedFieldPositions);
        writeDoubleArray("rec_time", sortedFieldPositions);
        writeDoubleArray("latitude", sortedFieldPositions);
        writeDoubleArray("longitude", sortedFieldPositions);
        writeFloatArray("elevation", sortedFieldPositions);
        writeStringArray("vehicle_id", sortedFieldPositions);
        writeStringArray("source_id", sortedFieldPositions);
        writeFloatArray("speed", sortedFieldPositions);
        writeShortArray("brake_status", sortedFieldPositions);
        writeShortArray("brake_boost", sortedFieldPositions);
        writeShortArray("air_temp", sortedFieldPositions);
        writeShortArray("bar_pressure", sortedFieldPositions);
        writeShortArray("trac", sortedFieldPositions);
        writeShortArray("stab", sortedFieldPositions);
        writeShortArray("abs", sortedFieldPositions);
        writeShortArray("lights", sortedFieldPositions);
        writeIntArray("psn", sortedFieldPositions);
        writeFloatArray("heading", sortedFieldPositions);
        writeFloatArray("yaw_rate", sortedFieldPositions);
        writeFloatArray("hoz_accel_long", sortedFieldPositions);
        writeFloatArray("hoz_accel_lat", sortedFieldPositions);
        writeShortArray("tire_pressure_lf", sortedFieldPositions);
        writeShortArray("tire_pressure_rf", sortedFieldPositions);
        writeShortArray("tire_pressure_rf", sortedFieldPositions);
        writeShortArray("tire_pressure_rr", sortedFieldPositions);
        writeShortArray("tire_pressure_sp", sortedFieldPositions);
        writeFloatArray("steering_angle", sortedFieldPositions);
        writeShortArray("steering_rate", sortedFieldPositions);
        writeFloatArray("air_temp2", sortedFieldPositions);
        writeFloatArray("dew_temp", sortedFieldPositions);
        writeFloatArray("humidity", sortedFieldPositions);
        writeFloatArray("surface_temp", sortedFieldPositions);

        try {
            ncFile.close();
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private void writeDoubleArray(String varName, int[] positions) {
        int recNum = inputProbeMessage.getRecNum();
        logger.debug("Writing " + varName + " recNum: " + recNum);

        NetCdfArrayList<Double> doubleList = (NetCdfArrayList<Double>) inputProbeMessage.getList(varName);
        doubleList.ensureCapacity(recNum);
        ArrayDouble doubleArray = new ArrayDouble.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            doubleArray.setDouble(i, doubleList.get(positions[i]));

        try {
            ncFile.write(varName, doubleArray);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private void writeFloatArray(String varName, int[] positions) {
        int recNum = inputProbeMessage.getRecNum();
        logger.debug("Writing " + varName + " recNum: " + recNum);

        NetCdfArrayList<Float> floatList = (NetCdfArrayList<Float>) inputProbeMessage.getList(varName);
        floatList.ensureCapacity(recNum);
        ArrayFloat floatArray = new ArrayFloat.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            floatArray.setFloat(i, floatList.get(positions[i]));

        try {
            ncFile.write(varName, floatArray);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private void writeShortArray(String varName, int[] positions) {
        int recNum = inputProbeMessage.getRecNum();
        logger.debug("Writing " + varName + " recNum: " + recNum);

        NetCdfArrayList<Short> shortList = (NetCdfArrayList<Short>) inputProbeMessage.getList(varName);
        shortList.ensureCapacity(recNum);
        ArrayShort shortArray = new ArrayShort.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            shortArray.setShort(i, shortList.get(positions[i]));

        try {
            ncFile.write(varName, shortArray);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private void writeIntArray(String varName, int[] positions) {
        int recNum = inputProbeMessage.getRecNum();
        logger.debug("Writing " + varName + " recNum: " + recNum);

        NetCdfArrayList<Integer> intList = (NetCdfArrayList<Integer>) inputProbeMessage.getList(varName);
        intList.ensureCapacity(recNum);
        ArrayInt intArray = new ArrayInt.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            intArray.setInt(i, intList.get(positions[i]));

        try {
            ncFile.write(varName, intArray);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    private void writeStringArray(String varName, int[] positions) {
        int recNum = inputProbeMessage.getRecNum();
        logger.debug("Writing " + varName + " recNum: " + recNum);

        NetCdfArrayList<String> stringList = (NetCdfArrayList<String>) inputProbeMessage.getList(varName);
        stringList.ensureCapacity(recNum);
        ArrayChar charArray = new ArrayChar.D2(recNum, 32);
        for (int i = 0; i < positions.length; i++)
            charArray.setString(i, stringList.get(positions[i]));

        try {
            ncFile.write(varName, charArray);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
