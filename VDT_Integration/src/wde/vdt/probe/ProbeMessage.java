/************************************************************************
 * Source filename: ProbeMessage.java
 * 
 * Creation date: Sep 26, 2013
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

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ucar.ma2.Array;
import wde.vdt.NetCdfArrayList;


public class ProbeMessage {
    
    private static final Logger logger = Logger.getLogger(ProbeMessage.class);
    
    public static final double DOUBLE_FILL_VALUE = -9999.0;
    
    public static final float FLOAT_FILL_VALUE = -9999.0f;
    
    public static final short SHORT_FILL_VALUE = -9999;
    
    public static final short QC_FILL_VALUE = 255;
    
    public static final int INT_FILL_VALUE = -9999;
    
    private TreeMap<String, Integer> vehObsTimeLookupMap = null; // Key is acqTime|vName
    
    private int recNum;
      
    protected HashMap<String, Class> variableMap = null;
    
    // The key is the variable name
    protected HashMap<String, Object> content = null;

    public ProbeMessage() {
        
        vehObsTimeLookupMap = new TreeMap<>();
        recNum = 0;
        content = new HashMap<>();
        variableMap = new HashMap<>();
        loadVariableList();
    }
    
    /**
     * @return the variableMap
     */
    public HashMap<String, Class> getVariableMap() {
        return variableMap;
    }

    public void registerObsTimeVname(String vehObsTimeStr, String vName) {
        String key = vehObsTimeStr + "|" + vName;
        if (vehObsTimeLookupMap.get(key) == null) {
            vehObsTimeLookupMap.put(key, new Integer(recNum++));
        }
    }
    
    public int[] getSortedFieldPositions() {
        Collection<Integer> positionIntegers = vehObsTimeLookupMap.values();
        int[] positions = new int[positionIntegers.size()];
        int i = 0;
        for (Integer pos : positionIntegers)
            positions[i++] = pos.intValue();
        
        return positions;
    }
    
    public <T> void updateValue(String key, String obsTimeStrVname, T value) {
        logger.info("updating value for " + key);
        NetCdfArrayList<T> list = (NetCdfArrayList<T>)content.get(key);
      
        int position = getPosition(obsTimeStrVname);
        if (position == -1) {
            logger.info(obsTimeStrVname + " not yet registered.");
            return;
        }

        list.update(position, value);
    }
    
    public void populateDoubleArrayVariable(String key, Array data) {
        NetCdfArrayList<Double> list = (NetCdfArrayList<Double>)content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getDouble(i));
    }
    
    public void populateFloatArrayVariable(String key, Array data) {
        NetCdfArrayList<Float> list = (NetCdfArrayList<Float>)content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getFloat(i)); 
    }
    
    public void populateIntArrayVariable(String key, Array data) {
        logger.info("populatingIntArray for " + key);
        NetCdfArrayList<Integer> list = (NetCdfArrayList<Integer>)content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getInt(i)); 
    }  
    
    public void populateShortArrayVariable(String key, Array data) {
        NetCdfArrayList<Short> list = (NetCdfArrayList<Short>)content.get(key);
        if (recNum == 0)
            recNum = (int) data.getSize();
        else if (recNum != data.getSize())
            logger.error("Different array size encountered");
        for (int i = 0; i < data.getSize(); i++)
            list.update(i, data.getShort(i)); 
    }  
    
    public void populateStringArrayVariable(String key, Array data) {
        int strCount = data.getShape()[0];
        int strLen = data.getShape()[1];
        char[] elementChars = new char[strLen];
   
        NetCdfArrayList<String> list = (NetCdfArrayList<String>)content.get(key);
        if (recNum == 0)
            recNum = strCount;
        else if (recNum != strCount)
            logger.error("Different array size encountered");
        
        for (int i = 0; i < strCount; i++) {

            for (int j = 0; j < strLen; j++)
                elementChars[j] = data.getChar(i*strLen + j);

            String element = new String(elementChars);
            list.update(i, element.trim()); 
        }
    }    
    
    public Object getList(String key) {
        return content.get(key);
    }
    
    /**
     * @return the recNum
     */
    public int getRecNum() {
        return recNum;
    }

    public void print() {
        for (String key: vehObsTimeLookupMap.keySet())
            System.out.println(key);
    }

    protected <T> void addVariable(String varName, T type) {
        NetCdfArrayList<T> list = new NetCdfArrayList<>();
        String typeName = type.toString();
        if (typeName.contains("Double"))
            ((NetCdfArrayList<Double>)list).init(DOUBLE_FILL_VALUE);
        else if (typeName.contains("Float"))
            ((NetCdfArrayList<Float>)list).init(FLOAT_FILL_VALUE);
        else if (typeName.contains("Short"))
            ((NetCdfArrayList<Short>)list).init(SHORT_FILL_VALUE);
        else if (typeName.contains("Integer"))
            ((NetCdfArrayList<Integer>)list).init(INT_FILL_VALUE);
        else if (typeName.contains("String"))
            ((NetCdfArrayList<String>)list).init("");
    
        content.put(varName, list);
    }

    protected void loadVariableList() {
        
        logger.info("calling loadVariableList");
        
        variableMap.put("abs", Short.class);
        variableMap.put("air_temp", Short.class);
        variableMap.put("air_temp2", Float.class); 
        variableMap.put("bar_pressure", Short.class);
        variableMap.put("brake_boost", Short.class);
        variableMap.put("brake_status", Short.class);
        variableMap.put("dew_temp", Float.class);
        variableMap.put("elevation", Float.class);
        variableMap.put("heading", Float.class);
        variableMap.put("hoz_accel_lat", Float.class);    
        variableMap.put("hoz_accel_long", Float.class);
        variableMap.put("humidity", Float.class);
        variableMap.put("latitude", Double.class);
        variableMap.put("lights", Short.class);
        variableMap.put("longitude", Double.class);        
        variableMap.put("obs_time", Double.class);

        variableMap.put("rec_time", Double.class);

        variableMap.put("speed", Float.class);
        variableMap.put("stab", Short.class);
        variableMap.put("steering_angle", Float.class);
        variableMap.put("steering_rate", Short.class);
        variableMap.put("surface_temp", Float.class);

        variableMap.put("trac", Short.class);
        variableMap.put("vehicle_id", String.class);
        variableMap.put("wiper_status", Short.class);
        variableMap.put("yaw_rate", Float.class);
    }

    private int getPosition(String obsTimeStrVname) {
        
        Integer position = vehObsTimeLookupMap.get(obsTimeStrVname);
        
        if (position != null)
            return position.intValue();

        return -1;
    }
}
