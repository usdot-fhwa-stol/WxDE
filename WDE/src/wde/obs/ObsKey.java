/************************************************************************
 * Source filename: ObsKey.java
 * <p/>
 * Creation date: Oct 25, 2013
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

package wde.obs;

public class ObsKey implements Comparable<ObsKey> {

    private int sourceId;

    private int sensorId;

    private int obsTypeId;

    private long obsTime;

    public ObsKey(int sourceId, int sensorId, int obsTypeId, long obsTime) {
        this.sourceId = sourceId;
        this.sensorId = sensorId;
        this.obsTypeId = obsTypeId;
        this.obsTime = obsTime;
    }

    public int compareTo(ObsKey obsKey) {
        int result = sourceId - obsKey.sourceId;
        if (result != 0)
            return result;

        result = sensorId - obsKey.sensorId;
        if (result != 0)
            return result;

        result = obsTypeId - obsKey.obsTypeId;
        if (result != 0)
            return result;

        long obsDiff = obsTime - obsKey.obsTime;
        if (obsDiff == 0)
            return 0;

        if (obsDiff > 0)
            return 1;
        else
            return -1;
    }

    /**
     * @return the obsTime
     */
    public long getObsTime() {
        return obsTime;
    }
}
