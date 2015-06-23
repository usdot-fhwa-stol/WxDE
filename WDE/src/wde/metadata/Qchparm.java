/************************************************************************
 * Source filename: Qchparm.java
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

import wde.dao.QchparmDao;

public class Qchparm extends TimeInvariantMetadata {

    private int sensorTypeId;

    private int obsTypeId;

    private boolean isDefault = true;

    private float minRange;

    private float maxRange;

    private float resolution;

    private float accuracy;

    private double ratePos;

    private double rateNeg;

    private double rateInterval;

    private double persistInterval;

    private double persistThreshold;

    private double likeThreshold;

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
     * @return the isDefault
     */
    public boolean isDefault() {
        return isDefault;
    }

    /**
     * @param isDefault the isDefault to set
     */
    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    /**
     * @return the minRange
     */
    public float getMinRange() {
        return minRange;
    }

    /**
     * @param minRange the minRange to set
     */
    public void setMinRange(float minRange) {
        this.minRange = minRange;
    }

    /**
     * @return the maxRange
     */
    public float getMaxRange() {
        return maxRange;
    }

    /**
     * @param maxRange the maxRange to set
     */
    public void setMaxRange(float maxRange) {
        this.maxRange = maxRange;
    }

    /**
     * @return the resolution
     */
    public float getResolution() {
        return resolution;
    }

    /**
     * @param resolution the resolution to set
     */
    public void setResolution(float resolution) {
        this.resolution = resolution;
    }

    /**
     * @return the accuracy
     */
    public float getAccuracy() {
        return accuracy;
    }

    /**
     * @param accuracy the accuracy to set
     */
    public void setAccuracy(float accuracy) {
        this.accuracy = accuracy;
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
     * @return the rateInterval
     */
    public double getRateInterval() {
        return rateInterval;
    }

    /**
     * @param rateInterval the rateInterval to set
     */
    public void setRateInterval(double rateInterval) {
        this.rateInterval = rateInterval;
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
    public void insertDbRecord(boolean atomic) {
        QchparmDao.getInstance().insertQchparm(this, atomic);
    }

    public void updateMap() {
        QchparmDao.getInstance().updateQchparmMap();
    }
}
