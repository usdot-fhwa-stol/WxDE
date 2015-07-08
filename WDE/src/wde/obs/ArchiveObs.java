/************************************************************************
 * Source filename: ArchiveObs.java
 * <p/>
 * Creation date: Feb 26, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.obs;

import wde.util.DateRange;

public class ArchiveObs {

    DateRange duration;

    private String gridId;

    private int obsTypeId;

    private ObsValue[] values;

    /**
     * @return the duration
     */
    public DateRange getDuration() {
        return duration;
    }

    /**
     * @param duration the duration to set
     */
    public void setDuration(DateRange duration) {
        this.duration = duration;
    }

    /**
     * @return the gridId
     */
    public String getGridId() {
        return gridId;
    }

    /**
     * @param gridId the gridId to set
     */
    public void setGridId(String gridId) {
        this.gridId = gridId;
    }

    /**
     * @return the obsTypeId
     */
    public int getObsTypeId() {
        return obsTypeId;
    }

    /**
     * @param obsTypeId the obsTypeId to set
     */
    public void setObsTypeId(int obsTypeId) {
        this.obsTypeId = obsTypeId;
    }

    /**
     * @return the values
     */
    public ObsValue[] getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(ObsValue[] values) {
        this.values = values;
    }
}
