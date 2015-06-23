/************************************************************************
 * Source filename: ObsType.java
 * <p/>
 * Creation date: Feb 25, 2013
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

import wde.dao.ObsTypeDao;

public class ObsType extends TimeInvariantMetadata implements Comparable<ObsType> {

    private String obsType = null;

    private String obs1204Unit = null;

    private String obsDesc = null;

    private String obsInternalUnit = null;

    private boolean active = false;

    private String obsEnglishUnit = null;

    /**
     * @return the obsType
     */
    public String getObsType() {
        return obsType;
    }

    /**
     * @param obsType the obsType to set
     */
    public void setObsType(String obsType) {
        this.obsType = obsType;
    }

    /**
     * @return the obs1204Units
     */
    public String getObs1204Unit() {
        return obs1204Unit;
    }

    /**
     * @param obs1204Units the obs1204Units to set
     */
    public void setObs1204Unit(String obs1204Unit) {
        this.obs1204Unit = obs1204Unit;
    }

    /**
     * @return the obsDesc
     */
    public String getObsDesc() {
        return obsDesc;
    }

    /**
     * @param obsDesc the obsDesc to set
     */
    public void setObsDesc(String obsDesc) {
        this.obsDesc = obsDesc;
    }

    /**
     * @return the obsInternalUnits
     */
    public String getObsInternalUnit() {
        return obsInternalUnit;
    }

    /**
     * @param obsInternalUnits the obsInternalUnits to set
     */
    public void setObsInternalUnit(String obsInternalUnit) {
        this.obsInternalUnit = obsInternalUnit;
    }

    /**
     * @return the active
     */
    public boolean isActive() {
        return active;
    }

    /**
     * @param active the active to set
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * @return the obsEnglishUnits
     */
    public String getObsEnglishUnit() {
        return obsEnglishUnit;
    }

    /**
     * @param obsEnglishUnits the obsEnglishUnits to set
     */
    public void setObsEnglishUnit(String obsEnglishUnits) {
        this.obsEnglishUnit = obsEnglishUnits;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void insertDbRecord(boolean atomic) {
        ObsTypeDao.getInstance().insertObsType(this, atomic);
    }

    public void updateMap() {
        ObsTypeDao.getInstance().updateObsTypeMap();
    }

    public int compareTo(ObsType ot) {
        return this.obsType.compareTo(ot.obsType);
    }
}
