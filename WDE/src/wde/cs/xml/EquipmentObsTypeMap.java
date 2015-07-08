/************************************************************************
 * Source filename: EquipmentObsTypeMap.java
 * <p/>
 * Creation date: May 28, 2014
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.cs.xml;

import java.util.HashMap;

public class EquipmentObsTypeMap {
    /**
     * Lookup table to find obstypeid for a given equipmentid from WxTelematics
     */
    private static HashMap<String, ObsTypeUnit> equipmentObsTypeMap = null;

    static {
        equipmentObsTypeMap = new HashMap<>();
        equipmentObsTypeMap.put("208", new ObsTypeUnit(51139, "C", 1.0));
        equipmentObsTypeMap.put("209", new ObsTypeUnit(5733, "C", 1.0));
        equipmentObsTypeMap.put("210", new ObsTypeUnit(206, "", 1.0));
        equipmentObsTypeMap.put("211", new ObsTypeUnit(581, "%", 1.0));
        equipmentObsTypeMap.put("10100", new ObsTypeUnit(60000, "Lux", 1.0));
        equipmentObsTypeMap.put("10101", new ObsTypeUnit(554, "mbar", 1.0));
        equipmentObsTypeMap.put("10102", new ObsTypeUnit(5146, "ppb", 100.0));
        equipmentObsTypeMap.put("10106", new ObsTypeUnit(543, "km/h", 1.0));
    }

    static ObsTypeUnit getObsTypeUnit(String equipmentId) {
        return equipmentObsTypeMap.get(equipmentId);
    }
}
