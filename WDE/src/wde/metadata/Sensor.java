/************************************************************************
 * Source filename: Sensor.java
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

import wde.dao.SensorDao;

import java.sql.Timestamp;

public class Sensor extends TimeVariantMetadata implements ISensor {

    private int sourceId;

    private int platformId;

    private int contribId;

    private int sensorIndex = 0;

    private int obsTypeId;

    private int qchparmId;

    private int distGroup;

    private float nsOffset;

    private float ewOffset;

    private float elevOffset;

    private float surfaceOffset;

    private Timestamp installDate;

    private Timestamp calibDate;

    private Timestamp maintDate;

    private Timestamp maintBegin;

    private Timestamp maintEnd;

    private String embeddedMaterial = null;

    private String sensorLocation = null;

    private int sensorTypeId;

    private double minRange;

    private double maxRange;

    private double ratePos;

    private double rateNeg;

    private double persistInterval;

    private double persistThreshold;

    private double likeThreshold;

    /**
     * @return the sourceId
     */
    public int getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId the sourceId to set
     */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the platformId
     */
    public int getPlatformId() {
        return platformId;
    }

    /**
     * @param platformId the platformId to set
     */
    public void setPlatformId(int platformId) {
        this.platformId = platformId;
    }

    /**
     * @return the contribId
     */
    public int getContribId() {
        return contribId;
    }

    /**
     * @param contribId the contribId to set
     */
    public void setContribId(int contribId) {
        this.contribId = contribId;
    }

    /**
     * @return the sensorIndex
     */
    public int getSensorIndex() {
        return sensorIndex;
    }

    /**
     * @param sensorIndex the sensorIndex to set
     */
    public void setSensorIndex(int sensorIndex) {
        this.sensorIndex = sensorIndex;
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
     * @return the qchparmId
     */
    public int getQchparmId() {
        return qchparmId;
    }

    /**
     * @param qchparmId the qchparmId to set
     */
    public void setQchparmId(int qchparmId) {
        this.qchparmId = qchparmId;
    }

    /**
     * @return the distGroup
     */
    public int getDistGroup() {
        return distGroup;
    }

    /**
     * @param distGroup the distGroup to set
     */
    public void setDistGroup(int distGroup) {
        this.distGroup = distGroup;
    }

    /**
     * @return the nsOffset
     */
    public float getNsOffset() {
        return nsOffset;
    }

    /**
     * @param nsOffset the nsOffset to set
     */
    public void setNsOffset(float nsOffset) {
        this.nsOffset = nsOffset;
    }

    /**
     * @return the ewOffset
     */
    public float getEwOffset() {
        return ewOffset;
    }

    /**
     * @param ewOffset the ewOffset to set
     */
    public void setEwOffset(float ewOffset) {
        this.ewOffset = ewOffset;
    }

    /**
     * @return the elevOffset
     */
    public float getElevOffset() {
        return elevOffset;
    }

    /**
     * @param elevOffset the elevOffset to set
     */
    public void setElevOffset(float elevOffset) {
        this.elevOffset = elevOffset;
    }

    /**
     * @return the surfaceOffset
     */
    public float getSurfaceOffset() {
        return surfaceOffset;
    }

    /**
     * @param surfaceOffset the surfaceOffset to set
     */
    public void setSurfaceOffset(float surfaceOffset) {
        this.surfaceOffset = surfaceOffset;
    }

    /**
     * @return the installDate
     */
    public Timestamp getInstallDate() {
        return installDate;
    }

    /**
     * @param installDate the installDate to set
     */
    public void setInstallDate(Timestamp installDate) {
        this.installDate = installDate;
    }

    /**
     * @return the calibDate
     */
    public Timestamp getCalibDate() {
        return calibDate;
    }

    /**
     * @param calibDate the calibDate to set
     */
    public void setCalibDate(Timestamp calibDate) {
        this.calibDate = calibDate;
    }

    /**
     * @return the maintDate
     */
    public Timestamp getMaintDate() {
        return maintDate;
    }

    /**
     * @param maintDate the maintDate to set
     */
    public void setMaintDate(Timestamp maintDate) {
        this.maintDate = maintDate;
    }

    /**
     * @return the maintBegin
     */
    public Timestamp getMaintBegin() {
        return maintBegin;
    }

    /**
     * @param maintBegin the maintBegin to set
     */
    public void setMaintBegin(Timestamp maintBegin) {
        this.maintBegin = maintBegin;
    }

    /**
     * @return the maintEnd
     */
    public Timestamp getMaintEnd() {
        return maintEnd;
    }

    /**
     * @param maintEnd the maintEnd to set
     */
    public void setMaintEnd(Timestamp maintEnd) {
        this.maintEnd = maintEnd;
    }

    /**
     * Determines whether the supplied time stamp occurs in a time of
     * maintenance.
     * @param lTimestamp time that is being check against maintenance time
     *      range.
     * @return true if the time stamp occurs in a time of maintenance.
     */
    public boolean underMaintenance(long time) {
        // this function must account for the possibility that 
        // either the begin or end time might be null
        if (maintBegin == null) {
            if (maintEnd == null)
                return false;

            if (time <= maintEnd.getTime())
                return true;
        } else {
            if (maintEnd == null)
                return (time >= maintBegin.getTime());
        }

        return (time >= maintBegin.getTime() && time <= maintEnd.getTime());
    }

    /**
     * @return the embeddedMaterial
     */
    public String getEmbeddedMaterial() {
        return embeddedMaterial;
    }

    /**
     * @param embeddedMaterial the embeddedMaterial to set
     */
    public void setEmbeddedMaterial(String embeddedMaterial) {
        this.embeddedMaterial = embeddedMaterial;
    }

    /**
     * @return the sensorLocation
     */
    public String getSensorLocation() {
        return sensorLocation;
    }

    /**
     * @param sensorLocation the sensorLocation to set
     */
    public void setSensorLocation(String sensorLocation) {
        this.sensorLocation = sensorLocation;
    }

    /**
     * @return the sensorTypeId
     */
    public int getSensorTypeId() {
        return sensorTypeId;
    }

    /**
     * @param sensorTypeId the sensorTypeId to set
     */
    public void setSensorTypeId(int sensorTypeId) {
        this.sensorTypeId = sensorTypeId;
    }

    /**
     * @return the minRange
     */
    public double getMinRange() {
        return minRange;
    }

    /**
     * @param minRange the minRange to set
     */
    public void setMinRange(double minRange) {
        this.minRange = minRange;
    }

    /**
     * @return the maxRange
     */
    public double getMaxRange() {
        return maxRange;
    }

    /**
     * @param maxRange the maxRange to set
     */
    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange;
    }

    /**
     * @return the ratePos
     */
    public double getRatePos() {
        return ratePos;
    }

    /**
     * @param ratePos the ratePos to set
     */
    public void setRatePos(double ratePos) {
        this.ratePos = ratePos;
    }

    /**
     * @return the rateNeg
     */
    public double getRateNeg() {
        return rateNeg;
    }

    /**
     * @param rateNeg the rateNeg to set
     */
    public void setRateNeg(double rateNeg) {
        this.rateNeg = rateNeg;
    }

    /**
     * @return the persistInterval
     */
    public double getPersistInterval() {
        return persistInterval;
    }

    /**
     * @param persistInterval the persistInterval to set
     */
    public void setPersistInterval(double persistInterval) {
        this.persistInterval = persistInterval;
    }

    /**
     * @return the persistThreshold
     */
    public double getPersistThreshold() {
        return persistThreshold;
    }

    /**
     * @param persistThreshold the persistThreshold to set
     */
    public void setPersistThreshold(double persistThreshold) {
        this.persistThreshold = persistThreshold;
    }

    /**
     * @return the likeThreshold
     */
    public double getLikeThreshold() {
        return likeThreshold;
    }

    /**
     * @param likeThreshold the likeThreshold to set
     */
    public void setLikeThreshold(double likeThreshold) {
        this.likeThreshold = likeThreshold;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        SensorDao.getInstance().updateSensor(this, atomic);
    }

    public void updateMap() {
        SensorDao.getInstance().updateSensorMap();
    }
}
