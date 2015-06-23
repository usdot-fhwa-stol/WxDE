/************************************************************************
 * Source filename: ProbeAssembly.java
 * 
 * Creation date: Oct 7, 2013
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

package wde.vdt.probe;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;

import org.apache.log4j.Logger;

import org.apache.log4j.xml.DOMConfigurator;

import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.ArrayShort;
import ucar.ma2.DataType;

import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriteable;

import wde.vdt.NetCdfArrayList;


import wde.vdt.probe.raw.ActX;
import wde.vdt.probe.raw.CanX;
import wde.vdt.probe.raw.Message;
import wde.vdt.probe.raw.ObdY;
import wde.vdt.probe.raw.VaiX;

public class ProbeAssembly {
    
    private static final Logger logger = Logger.getLogger(ProbeAssembly.class);
    
    private static ProbeAssembly instance = null;

    private Properties prop = null;

    private String rawRoot;
    private String rawPrefix;
    private String rawFileExtension;

    private String processedRoot;
    private String processedPrefix;
    private String processedFileExtension;
    
    private String fileDelimiter;
    
    private String dateFormatStr;
    private String timeFormatStr;

    private SimpleDateFormat dateFormat = null;
    private SimpleDateFormat tsFormat = null;
    private NetcdfFileWriteable ncFile = null;
    private InputProbeMessage inputProbeMessage = null;
    
    public static void main(String[] args)
    {
        DOMConfigurator.configure("config/vdt_log4j.xml");
        ProbeAssembly.getInstance();
    }

    public static ProbeAssembly getInstance()
    {
        if (instance == null)
            instance = new ProbeAssembly();
        
        return instance;
    }
    
    public void generateProbeMessage(GregorianCalendar now) throws Exception
    {
        // Need to filter out outdated observations so we don't introduce duplicates
        
        Date oDate = now.getTime();
        String postfix = fileDelimiter + tsFormat.format(now.getTime()) + fileDelimiter;
        
        BufferedReader fileReader = createFileReader(rawPrefix, oDate, postfix + rawFileExtension);   
        populateInputProbeMessage(fileReader); 
        fileReader.close();
        
        String ncFilePath = getNcFilePath(processedRoot, processedPrefix, oDate, postfix + processedFileExtension);
        createNcFile(ncFilePath);
    }
    
    private ProbeAssembly()
    {
        prop = new Properties();
        loadPropertiesFile();
        
        dateFormat = new SimpleDateFormat(dateFormatStr);
        
        String df = dateFormatStr + fileDelimiter + timeFormatStr; 

        tsFormat = new SimpleDateFormat(df);

    }
    
    private BufferedReader createFileReader(String sPrefix, Date date, String postfix)
        throws Exception
    {
        BufferedReader fr = null;
        String separator = System.getProperty("file.separator");
        
        StringBuilder filename = new StringBuilder(rawRoot);
        filename.append(sPrefix);
        filename.append(separator);
        filename.append(dateFormat.format(date));
        new File(filename.toString()).mkdirs();
        
        filename.append(separator);
        filename.append(sPrefix);
        filename.append(postfix);

        logger.info("Preparing to read: " + filename.toString());
        fr = new BufferedReader(new FileReader(filename.toString()));
        
        return fr;
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
    
            rawRoot = prop.getProperty("rawroot");
            rawPrefix = prop.getProperty("rawprefix");
            rawFileExtension = prop.getProperty("rawfileextension");
            
            processedRoot = prop.getProperty("processedroot");
            processedPrefix = prop.getProperty("processedprefix");
            processedFileExtension = prop.getProperty("processedfileextension");

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

    private void populateInputProbeMessage(BufferedReader br)
    {
        inputProbeMessage = new InputProbeMessage();
        String str = null;
        try {
            while ((str = br.readLine()) != null) {
                // Skip empty line and the title line
                if (str.length() == 0)
                    continue;
                
                // Remove the last semicolon
                str = str.substring(0, str.length()-1);
                String str1 = str.substring(str.indexOf(">"));

                if (str1.startsWith(">ActX:")) {
                    ActX actX = (ActX) populateMessage(str, "ActX");
                    processActXMessage(actX);
                }
                else if (str1.startsWith(">CanX:")) {
                    CanX canX = (CanX) populateMessage(str, "CanX");
                    processCanXMessage(canX);
                }
                else if (str1.startsWith(">ObdY:")) {
                    ObdY obdY = (ObdY) populateMessage(str, "ObdY");
                    processObdYMessage(obdY);
                }
                else if (str1.startsWith(">VaiX:")) {
                    VaiX vaiX = (VaiX) populateMessage(str, "VaiX");
                    processVaiXMessage(vaiX);
                }
            }
            
            logger.info("populated probe message");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Message populateMessage(String str, String className)
    {
        int delimiterPosition = str.indexOf('>');
        String recvTime = str.substring(0, delimiterPosition);
        str = str.substring(delimiterPosition);

        Message message = null;
        if (str.startsWith(">" + className + ":")) {
            str.replace(">" + className + ":", ">" + className + ",");
            String[] strArray = str.split("\\,");
            try {
                Class tempClass = Class.forName("wde.vdt.probe.raw." + className);
                Constructor<Message> ctor = tempClass.getDeclaredConstructor(String.class, String[].class);
                message = ctor.newInstance(recvTime, (Object)strArray);
                inputProbeMessage.registerObsTimeVname(message.getAcqTime(), message.getvName());
                logger.info("populated " + className + " and registered " + message.getAcqTime());
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return message;
    }
    
    private void processMessage(Message msg)
    {
        String obsTimeStrVname = msg.getAcqTime() + "|" + msg.getvName();
        inputProbeMessage.updateValue("obs_time", obsTimeStrVname, msg.getObsTime());
        inputProbeMessage.updateValue("rec_time", obsTimeStrVname, msg.getRecTime());
        inputProbeMessage.updateValue("latitude", obsTimeStrVname, msg.getLat());
        inputProbeMessage.updateValue("longitude", obsTimeStrVname, msg.getLon());
        inputProbeMessage.updateValue("vehicle_id", obsTimeStrVname, msg.getvName());
        inputProbeMessage.updateValue("speed", obsTimeStrVname, msg.getVel());
        inputProbeMessage.updateValue("heading", obsTimeStrVname, msg.getCourse());
    }
    
    private void processActXMessage(ActX actX)
    {
        processMessage(actX);
        String obsTimeStrVname = actX.getAcqTime() + "|" + actX.getvName();
        if (!actX.getField(ActX.act1).equals("null"))
            inputProbeMessage.updateValue("lights", obsTimeStrVname, Short.parseShort(actX.getField(ActX.act1)));
        if (!actX.getField(ActX.act2).equals("null"))
            inputProbeMessage.updateValue("wiper_status", obsTimeStrVname, Short.parseShort(actX.getField(ActX.act2)));
        
        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_actx");
    }
    
    private void processCanXMessage(CanX canX)
    {
        processMessage(canX);
        String obsTimeStrVname = canX.getAcqTime() + "|" + canX.getvName();
        inputProbeMessage.updateValue("abs", obsTimeStrVname, canX.getAbs());
        
        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_canx");
    }
    
    private void processObdYMessage(ObdY obdY)
    {
        processMessage(obdY);
        String obsTimeStrVname = obdY.getAcqTime() + "|" + obdY.getvName();
        if (obdY.getField(ObdY.BP) != null && !obdY.getField(ObdY.BP).equals("null"))
            inputProbeMessage.updateValue("brake_status", obsTimeStrVname, Short.parseShort(obdY.getField(ObdY.BP)));
        if (obdY.getField(ObdY.AIRT) != null && !obdY.getField(ObdY.AIRT).equals("null"))
            inputProbeMessage.updateValue("air_temp", obsTimeStrVname, (short)(Short.parseShort(obdY.getField(ObdY.AIRT))-32));
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
    
    private void processVaiXMessage(VaiX vaiX)
    {
        processMessage(vaiX);
        String obsTimeStrVname = vaiX.getAcqTime() + "|" + vaiX.getvName();
        if (!vaiX.getField(VaiX.tempAir).equals("null"))
            inputProbeMessage.updateValue("air_temp2", obsTimeStrVname, (float)(Float.parseFloat(vaiX.getField(VaiX.tempAir))-32));
        if (!vaiX.getField(VaiX.dewPt).equals("null"))
            inputProbeMessage.updateValue("dew_temp", obsTimeStrVname, (float)(Float.parseFloat(vaiX.getField(VaiX.dewPt))-32));
        if (!vaiX.getField(VaiX.humid).equals("null"))
            inputProbeMessage.updateValue("humidity", obsTimeStrVname, Float.parseFloat(vaiX.getField(VaiX.humid)));
        if (!vaiX.getField(VaiX.tempRoad).equals("null"))
            inputProbeMessage.updateValue("surface_temp", obsTimeStrVname, (float)(Float.parseFloat(vaiX.getField(VaiX.tempRoad))-32));
        
        inputProbeMessage.updateValue("source_id", obsTimeStrVname, "ameritrak_vaix");
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
        ncFile.addVariable(varName,  DataType.CHAR, dims);
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
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "all_off", 0);
        ncFile.addVariableAttribute(varName, "rr_active", 8);
        ncFile.addVariableAttribute(varName, "rf_active", 4);
        ncFile.addVariableAttribute(varName, "lr_active", 2);
        ncFile.addVariableAttribute(varName, "lf_active", 1);
        ncFile.addVariableAttribute(varName, "all_on", 15);
        
        varName = "brake_boost";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Brake Boost Applied Status");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "OFF", 1);
        ncFile.addVariableAttribute(varName, "ON", 2);

        varName = "wiper_status";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Front Wiper Status");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
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
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        
        varName = "trac";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Traction Control State");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "on", 2);
        ncFile.addVariableAttribute(varName, "engaged", 3);
        
        varName = "stab";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Stability Control Status");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "on", 2);
        ncFile.addVariableAttribute(varName, "engaged", 3);
        
        varName = "abs";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Anti-lock Brake Status");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        ncFile.addVariableAttribute(varName, "notEquipped", 0);
        ncFile.addVariableAttribute(varName, "off", 1);
        ncFile.addVariableAttribute(varName, "on", 2);
        ncFile.addVariableAttribute(varName, "engaged", 3);
        
        varName = "lights";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Exterior Lights");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
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
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.INT_FILL_VALUE);

        varName = "heading";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Heading");
        String[] headingRange = {"0", "360"};
        data = Array.makeArray(DataType.INT, headingRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);
        
        varName = "yaw_rate";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Yaw Rate");
        String[] yawRateRange = {"0.0", "655.35"};
        data = Array.makeArray(DataType.DOUBLE, yawRateRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees per second");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);
        
        varName = "hoz_accel_long";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Longitudinal Acceleration");
        String[] hozAccelRange = {"-20.0", "20.0"};
        data = Array.makeArray(DataType.DOUBLE, hozAccelRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m/s^2");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);        
        
        varName = "hoz_accel_lat";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Lateral Acceleration");
        data = Array.makeArray(DataType.DOUBLE, hozAccelRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m/s^2");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);          
        
        varName = "tire_pressure_lf";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Left Front Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        
        varName = "tire_pressure_rf";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Right Front Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        
        varName = "tire_pressure_lr";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Left Rear Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        
        varName = "tire_pressure_rr";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Right Rear Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);        
        
        varName = "tire_pressure_sp";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Tire Pressure in PSI for the Spare Tire");
        ncFile.addVariableAttribute(varName, "units", "PSI");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        
        varName = "steering_angle";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Steering Wheel Angle");
        String[] steeringAngleRange = {"-655.36", "655.36"};
        data = Array.makeArray(DataType.DOUBLE, steeringAngleRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);           
        
        varName = "steering_rate";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Steering Wheel Angle Rate of Change");
        String[] steeringRateRange = {"-381", "381"};
        data = Array.makeArray(DataType.INT, steeringRateRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees/second");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE);
        
        varName = "air_temp2";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "ambient air temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "air_temperature");
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);
        
        varName = "dew_temp";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "dew temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "dew_temperature");
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);        
        
        varName = "humidity";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "humidity");
        ncFile.addVariableAttribute(varName, "standard_name", "humidity");
        String[] humidityRange = {"0", "100"};
        data = Array.makeArray(DataType.INT, humidityRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "percent");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);        
        
        varName = "surface_temp";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "surface temperature");
        ncFile.addVariableAttribute(varName, "standard_name", "surface_temperature");
        data = Array.makeArray(DataType.INT, tempRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "Celsius");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE);          
        
        varName = "obs_time";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "observation time");
        ncFile.addVariableAttribute(varName, "units", "seconds since 1970-1-1 00:00");
        ncFile.addVariableAttribute(varName,  "_CoordinateAxisType", "Time"); 
        
        varName = "rec_time";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "received time");
        ncFile.addVariableAttribute(varName, "units", "seconds since 1970-1-1 00:00");
        ncFile.addVariableAttribute(varName,  "_CoordinateAxisType", "Time");        
        
        varName = "latitude";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "obs latitude");
        ncFile.addVariableAttribute(varName, "standard_name", "latitude");
        String[] latRange = {"-90.0", "90.0"};
        data = Array.makeArray(DataType.DOUBLE, latRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees_north");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.DOUBLE_FILL_VALUE); 
        ncFile.addVariableAttribute(varName,  "_CoordinateAxisType", "Lat"); 
        
        varName = "longitude";
        ncFile.addVariable(varName, DataType.DOUBLE, dims);
        ncFile.addVariableAttribute(varName, "long_name", "obs longitude");
        ncFile.addVariableAttribute(varName, "standard_name", "longitude");
        String[] longRange = {"-180.0", "180.0"};
        data = Array.makeArray(DataType.DOUBLE, longRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "degrees_east");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.DOUBLE_FILL_VALUE); 
        ncFile.addVariableAttribute(varName,  "_CoordinateAxisType", "Lon");
        
        varName = "elevation";
        ncFile.addVariable(varName, DataType.FLOAT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Elevation");
        ncFile.addVariableAttribute(varName, "standard_name", "longitude");
        String[] elevRange = {"-1000", "10000"};
        data = Array.makeArray(DataType.INT, elevRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "m");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.FLOAT_FILL_VALUE); 
        ncFile.addVariableAttribute(varName,  "_CoordinateAxisType", "Height");        
        
        varName = "bar_pressure";
        ncFile.addVariable(varName, DataType.SHORT, dims);
        ncFile.addVariableAttribute(varName, "long_name", "Barometric Pressure");
        String[] barPresRange = {"580", "1090"};
        data = Array.makeArray(DataType.INT, barPresRange);
        ncFile.addVariableAttribute(varName, "valid_range", data);
        ncFile.addVariableAttribute(varName, "units", "hPa");
        ncFile.addVariableAttribute(varName,  "_FillValue", ProbeMessage.SHORT_FILL_VALUE); 
        ncFile.addVariableAttribute(varName,  "_CoordinateAxisType", "Pressure");
        
        ncFile.addGlobalAttribute("_CoordSysBuilder", "ucar.nc2.dataset.conv.DefaultConvention");

        // create the file
        try {
            // Create the file. At this point the (empty) file will be written to disk, 
            // and the metadata (Dimensions, Variables and Attributes) is fixed and cannot be changed or added.
            ncFile.create();
        }
        catch (IOException e) {
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
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void writeDoubleArray(String varName, int[] positions)
    {
        int recNum = inputProbeMessage.getRecNum();
        logger.info("Writing " + varName + " recNum: " + recNum);

        NetCdfArrayList<Double> doubleList = (NetCdfArrayList<Double>) inputProbeMessage.getList(varName);        
        ArrayDouble doubleArray = new ArrayDouble.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            doubleArray.setDouble(i, doubleList.get(positions[i])); 
        
        try {
            ncFile.write(varName, doubleArray);
        }        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void writeFloatArray(String varName, int[] positions)
    {
        int recNum = inputProbeMessage.getRecNum();
        logger.info("Writing " + varName + " recNum: " + recNum);
        
        NetCdfArrayList<Float> floatList = (NetCdfArrayList<Float>) inputProbeMessage.getList(varName);        
        ArrayFloat floatArray = new ArrayFloat.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            floatArray.setFloat(i, floatList.get(positions[i])); 
        
        try {
            ncFile.write(varName, floatArray);
        }        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void writeShortArray(String varName, int[] positions)
    {
        int recNum = inputProbeMessage.getRecNum();
        logger.info("Writing " + varName + " recNum: " + recNum);
        
        NetCdfArrayList<Short> shortList = (NetCdfArrayList<Short>) inputProbeMessage.getList(varName);        
        ArrayShort shortArray = new ArrayShort.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            shortArray.setShort(i, shortList.get(positions[i])); 
        
        try {
            ncFile.write(varName, shortArray);
        }        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void writeIntArray(String varName, int[] positions)
    {
        int recNum = inputProbeMessage.getRecNum();
        logger.info("Writing " + varName + " recNum: " + recNum);
        
        NetCdfArrayList<Integer> intList = (NetCdfArrayList<Integer>) inputProbeMessage.getList(varName);        
        ArrayInt intArray = new ArrayInt.D1(recNum);
        for (int i = 0; i < positions.length; i++)
            intArray.setInt(i, intList.get(positions[i])); 
        
        try {
            ncFile.write(varName, intArray);
        }        
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void writeStringArray(String varName, int[] positions)
    {
        int recNum = inputProbeMessage.getRecNum();
        logger.info("Writing " + varName + " recNum: " + recNum);
        
        NetCdfArrayList<String> stringList = (NetCdfArrayList<String>) inputProbeMessage.getList(varName);        
        ArrayChar charArray = new ArrayChar.D2(recNum, 32);
        for (int i = 0; i < positions.length; i++)
            charArray.setString(i, stringList.get(positions[i])); 
        
        try {
            ncFile.write(varName, charArray);
        }        
        catch (Exception e) {
            e.printStackTrace();
        }
    } 
}
