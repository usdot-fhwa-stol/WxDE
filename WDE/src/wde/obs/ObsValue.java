/************************************************************************
 * Source filename: ObsValue.java
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

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ObsValue {

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private int sourceId;

    private int sensorId;

    private Timestamp obsTime;

    private Timestamp recvTime;

    private int latitude;

    private int longitude;

    private int elevation;

    private double value;

    private float confValue;

    private String qchCharFlags;

    public ObsValue(Observation obs) {
        sourceId = obs.getSourceId();
        sensorId = obs.getSensorId();
        obsTime = obs.getObsTime();
        recvTime = obs.getRecvTime();
        latitude = obs.getLatitude();
        longitude = obs.getLongitude();
        elevation = obs.getElevation();
        value = obs.getValue();
        confValue = obs.getConfValue();
        qchCharFlags = obs.getQchCharFlags();
    }

    public ObsValue(String s) {
        // First strip all double quotes
        s = s.replace("\\\"", "");
        String[] sArr = s.split(",");
        sourceId = Integer.valueOf(sArr[0]);
        sensorId = Integer.valueOf(sArr[1]);
        try {
            Date d;
            d = sdf.parse(sArr[2]);
            obsTime = new Timestamp(d.getTime());
            d = sdf.parse(sArr[3]);
            recvTime = new Timestamp(d.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }
        latitude = Integer.valueOf(sArr[4]);
        longitude = Integer.valueOf(sArr[5]);
        elevation = Integer.valueOf(sArr[6]);
        value = Double.valueOf(sArr[7]);
        confValue = Float.valueOf(sArr[8]);
        qchCharFlags = sArr[9];
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
     * @return the qchCharFlags
     */
    public String getQchCharFlags() {
        return qchCharFlags;
    }

    /**
     * @param qchCharFlags the qchCharFlag to set
     */
    public void setQchCharFlag(String qchCharFlags) {
        this.qchCharFlags = qchCharFlags;
    }
}
