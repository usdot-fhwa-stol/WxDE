/************************************************************************
 * Source filename: SensorRecoveryObj.java
 * <p/>
 * Creation date: Jul 24, 2013
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

package wde.util;

public class SensorRecoveryObj {

    private int contribId;

    private String platformCode = null;

    private int obsTypeId;

    private int sensorIndex;

    public SensorRecoveryObj(int contribId, String platformCode, int obsTypeId, int sensorIndex) {
        this.contribId = contribId;

        this.platformCode = platformCode.trim();

        this.obsTypeId = obsTypeId;

        this.sensorIndex = sensorIndex;
    }

    /**
     * @return the contribId
     */
    public int getContribId() {
        return contribId;
    }

    /**
     * @return the platformCode
     */
    public String getPlatformCode() {
        return platformCode;
    }

    /**
     * @return the obsTypeId
     */
    public int getObsTypeId() {
        return obsTypeId;
    }

    /**
     * @return the sensorIndex
     */
    public int getSensorIndex() {
        return sensorIndex;
    }
}
