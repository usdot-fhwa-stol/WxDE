/************************************************************************
 * Source filename: Observation.java
 * <p/>
 * Creation date: Feb 26, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * Captures the business object for a sensor observation
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.obs;

import java.sql.Timestamp;


public class Observation implements Comparable<Observation>, IObs {

    private int obsTypeId;

    private int sourceId;

    private int sensorId;

    private Timestamp obsTime;

    private Timestamp recvTime;

    private int latitude;

    private int longitude;

    private int elevation;

    private double value;

    private float confValue;

    private char[] qchCharFlag;

    private short hashValue;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Obs}
     * </p>
     */
    public Observation() {
    }

    /**
     * Initializes attributes of new instance of {@code Obs} to the
     * corresponding supplied values.
     * @param nTypeId observation type id.
     * @param nSensorId sensor id.
     * @param lTimestamp timestamp indicating when the observation was made.
     * @param nLat latitude of the observing sensor.
     * @param nLon longitude of the observing sensor.
     * @param tElev elevation of the observing sensor.
     *      made.
     * @param dValue observation scalar value.
     */
    public Observation(int obsTypeId, int sourceId, int sensorId, long obsTime, long recvTime, int latitude, int longitude,
                       int elevation, double value) {
        this.obsTypeId = obsTypeId;
        this.sourceId = sourceId;
        this.sensorId = sensorId;
        this.obsTime = new Timestamp(obsTime);
        this.recvTime = new Timestamp(recvTime);
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevation = elevation;
        this.value = value;
    }

    /**
     * Initializes attributes of new instance of {@code Obs} to the
     * corresponding supplied values.
     * @param nTypeId observation type id.
     * @param nSensorId sensor id.
     * @param lTimestamp timestamp indicating when the observation was made.
     * @param nLat latitude of the observing sensor.
     * @param nLon longitude of the observing sensor.
     * @param tElev elevation of the observing sensor.
     *      made.
     * @param dValue observation scalar value.
     * @param nRun bit-field indicating which quality checking algorithm to run.
     * @param nFlags bit-field indicating results of the corresponding
     *      algorithm.
     * @param fConfidence quality confidence level.
     */
    public Observation(int obsTypeId, int sourceId, int sensorId, long obsTime, long recvTime, int latitude, int longitude,
                       short elevation, double value, char[] qchCharFlag, float confValue) {
        this(obsTypeId, sourceId, sensorId, obsTime, recvTime, latitude, longitude, elevation, value);

        this.qchCharFlag = qchCharFlag;
        this.confValue = confValue;
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
     * @return the sensorId
     */
    public int getSensorId() {
        return sensorId;
    }

    /**
     * @param sensorId the sensorId to set
     */
    public void setSensorId(int sensorId) {
        this.sensorId = sensorId;
    }

    /**
     * @return the obsTime
     */
    public Timestamp getObsTime() {
        return obsTime;
    }

    /**
     * @param obsTime the obsTime to set
     */
    public void setObsTime(Timestamp obsTime) {
        this.obsTime = obsTime;
    }

    /**
     * @return the obsTime long value
     */
    public long getObsTimeLong() {
        return obsTime.getTime();
    }

    /**
     * @param obsTime the obsTime to set
     */
    public void setObsTimeLong(long obsTime) {
        this.obsTime = new Timestamp(obsTime);
    }

    /**
     * @return the recvTime
     */
    public Timestamp getRecvTime() {
        return recvTime;
    }

    /**
     * @param recvTime the recvTime to set
     */
    public void setRecvTime(Timestamp recvTime) {
        this.recvTime = recvTime;
    }

    /**
     * @return the recvTime long value
     */
    public long getRecvTimeLong() {
        return recvTime.getTime();
    }

    /**
     * @param recvTime the recvTime to set
     */
    public void setRecvTimeLong(long recvTime) {
        this.recvTime = new Timestamp(recvTime);
    }

    /**
     * @return the latitude
     */
    public int getLatitude() {
        return latitude;
    }

    /**
     * @param latitude the latitude to set
     */
    public void setLatitude(int latitude) {
        this.latitude = latitude;
    }

    /**
     * @return the longitude
     */
    public int getLongitude() {
        return longitude;
    }

    /**
     * @param longitude the longitude to set
     */
    public void setLongitude(int longitude) {
        this.longitude = longitude;
    }

    /**
     * @return the elevation
     */
    public int getElevation() {
        return elevation;
    }

    /**
     * @param elevation the elevation to set
     */
    public void setElevation(int elevation) {
        this.elevation = elevation;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

    /**
     * @param value the value to set
     */
    public void setValue(double value) {
        this.value = value;
    }

    /**
     * @return the confValue
     */
    public float getConfValue() {
        return confValue;
    }

    /**
     * @param confValue the confValue to set
     */
    public void setConfValue(float confValue) {
        this.confValue = confValue;
    }

    /**
     * @return the qchCharFlag
     */
    public char[] getQchCharFlag() {
        return qchCharFlag;
    }

    /**
     * @param qchCharFlag the qchCharFlag to set
     */
    public void setQchCharFlag(char[] qchCharFlag) {
        this.qchCharFlag = qchCharFlag;
    }

    /**
     * @return
     */
    public String getQchCharFlags() {
        if (qchCharFlag == null)
            return null;

        String flags = "";
        for (char f : qchCharFlag) {
            flags += f;
        }
        return flags;
    }

    /**
     * @return the hashValue
     */
    public short getHashValue() {
        return hashValue;
    }

    /**
     * @param hashValue the hashValue to set
     */
    public void setHashValue(short hashValue) {
        this.hashValue = hashValue;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(Observation obs) {
        return this.obsTime.compareTo(obs.obsTime);
    }

    /**
     *
     */
    void clear() {
        sensorId = latitude = longitude = 0;
        qchCharFlag = null;
        elevation = 0;
        obsTime = new Timestamp(0L);
        confValue = 0.0F;
        value = 0.0;
    }
}
