/************************************************************************
 * Source filename: SensorType.java
 * <p/>
 * Creation date: Feb 24, 2013
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

package wde.metadata;

import wde.dao.SensorTypeDao;

public class SensorType extends TimeVariantMetadata {

    private String mfr = null;

    private String model = null;

    /**
     * @return the mfr
     */
    public String getMfr() {
        return mfr;
    }

    /**
     * @param mfr the mfr to set
     */
    public void setMfr(String mfr) {
        this.mfr = mfr;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        SensorTypeDao.getInstance().updateSensorType(this, atomic);
    }

    public void updateMap() {
        SensorTypeDao.getInstance().updateSensorTypeMap();
    }
}
