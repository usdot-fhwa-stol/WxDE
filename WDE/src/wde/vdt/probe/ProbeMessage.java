/************************************************************************
 * Source filename: ProbeMessage.java
 * <p/>
 * Creation date: Sep 26, 2013
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
import wde.vdt.NcMem;
import wde.vdt.NetCdfArrayList;

import java.util.Collection;
import java.util.TreeMap;


public class ProbeMessage extends NcMem {

    private static final Logger logger = Logger.getLogger(ProbeMessage.class);

    private TreeMap<String, Integer> vehObsTimeLookupMap = null; // Key is acqTime|vName

    public ProbeMessage() {

        super();
        vehObsTimeLookupMap = new TreeMap<>();
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
        logger.debug("updating value for key: " + key + " obsTimeStrVname: " + obsTimeStrVname + " value: " + value.getClass());
        NetCdfArrayList<T> list = (NetCdfArrayList<T>) content.get(key);

        int position = getPosition(obsTimeStrVname);
        if (position == -1) {
            logger.info(obsTimeStrVname + " not yet registered.");
            return;
        }
        list.ensureCapacity(position + 1);
        list.update(position, value);
    }

    public void print() {
        for (String key : vehObsTimeLookupMap.keySet())
            System.out.println(key);
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
