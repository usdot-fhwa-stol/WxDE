/************************************************************************
 * Source filename: ObsQcMap.java
 * <p/>
 * Creation date: Oct 22, 2013
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

import java.util.HashMap;

public class ObsQcMap {

    private String obsType = null;

    private String valueType = null;

    private int sensorIndex;

    private HashMap<String, Integer> qcMap = null;

    public ObsQcMap(String obsType, String valueType, int sensorIndex, HashMap<String, Integer> qcMap) {
        this.obsType = obsType;
        this.valueType = valueType;
        this.sensorIndex = sensorIndex;
        this.qcMap = qcMap;
    }

    /**
     * @return the obsType
     */
    public String getObsType() {
        return obsType;
    }

    /**
     * @return the valueType
     */
    public String getValueType() {
        return valueType;
    }

    /**
     * @return the sensorIndex
     */
    public int getSensorIndex() {
        return sensorIndex;
    }

    /**
     * @return the qcMap
     */
    public HashMap<String, Integer> getQcMap() {
        return qcMap;
    }

}
