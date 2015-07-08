/************************************************************************
 * Source filename: AmeritrakMessage.java
 * <p/>
 * Creation date: Mar 11, 2014
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

package wde.vdt.probe.raw.umtri;

import org.apache.log4j.Logger;
import wde.util.DateTimeHelper;

import java.text.ParseException;
import java.util.Date;

public class UmtriMessage {

    private static final Logger logger = Logger.getLogger(UmtriMessage.class);

    private String vin = null;

    private Date currentTime = null;

    private Date recTime = null;

    private double latitudePosition;

    private double longitudePosition;

    private String altitude = "";

    private String airTemperatureFromCan = "";

    private String barometriPressureFromCan = "";

    private String compassHeading = "";

    private String speed = "";

    private String brakes = "";

    private String antilockBrakingSystem = "";

    private String electronicStabilityControl = "";

    private String tractionControlBrakingEvent = "";

    private String surfaceTemp = "";

    private String dewPoint = "";

    private String ambientAirTemperature = "";

    private String humidity = "";


    public UmtriMessage(Date recvTime, String[] rawData) {
        recTime = recvTime;
        int len = rawData.length;
        int index = 0;

        if (index < len)
            vin = rawData[index++];

        if (index < len)
            try {
                currentTime = DateTimeHelper.getTimeFormatter2().parse(rawData[index++]);
            } catch (ParseException e) {
                e.printStackTrace();
            }

        if (index < len)
            latitudePosition = Double.parseDouble(rawData[index++]);

        if (index < len)
            longitudePosition = Double.parseDouble(rawData[index++]);

        if (index < len)
            altitude = rawData[index++];
        else return;

        if (index < len)
            airTemperatureFromCan = rawData[index++];
        else return;

        if (index < len)
            barometriPressureFromCan = rawData[index++];
        else return;

        if (index < len)
            compassHeading = rawData[index++];
        else return;

        // skip speed from GPS since the VDT does not process this
        index++;

        if (index < len)
            speed = rawData[index++];
        else return;

        if (index < len)
            brakes = rawData[index++];
        else return;

        if (index < len)
            antilockBrakingSystem = rawData[index++];
        else return;

        if (index < len)
            electronicStabilityControl = rawData[index++];
        else return;

        // skip traction control engin since the VDT does not process this
        index++;

        if (index < len)
            tractionControlBrakingEvent = rawData[index++];
        else return;

        if (index < len)
            surfaceTemp = rawData[index++];
        else return;

        if (index < len)
            dewPoint = rawData[index++];
        else return;

        if (index < len)
            ambientAirTemperature = rawData[index++];
        else return;

        if (index < len)
            humidity = rawData[index++];
        else return;
    }

    /**
     * @return the vin
     */
    public String getVin() {
        return vin;
    }

    /**
     * @return the currentTime
     */
    public String getCurrentTime() {
        if (currentTime == null)
            return null;

        return currentTime.toString();
    }

    /**
     * @return the currentTime in seconds
     */
    public double getObsTime() {
        return currentTime.getTime() / 1000;
    }

    /**
     * @return the recTime
     */
    public double getRecTime() {
        return recTime.getTime() / 1000;
    }

    /**
     * @return the latitudePosition
     */
    public double getLatitudePosition() {
        return latitudePosition;
    }

    /**
     * @return the longitudePosition
     */
    public double getLongitudePosition() {
        return longitudePosition;
    }

    /**
     * @return the altitude
     */
    public String getAltitude() {
        return altitude;
    }

    /**
     * @return the airTemperatureFromCan
     */
    public String getAirTemperatureFromCan() {
        return airTemperatureFromCan;
    }

    /**
     * @return the barometriPressureFromCan
     */
    public String getBarometriPressureFromCan() {
        return barometriPressureFromCan;
    }

    /**
     * @return the compassHeading
     */
    public String getCompassHeading() {
        return compassHeading;
    }

    /**
     * @return the speed
     */
    public String getSpeed() {
        return speed;
    }

    /**
     * @return the brakes
     */
    public String getBrakes() {
        return brakes;
    }

    /**
     * @return the antilockBrakingSystem
     */
    public String getAntilockBrakingSystem() {
        return antilockBrakingSystem;
    }

    /**
     * @return the electronicStabilityControl
     */
    public String getElectronicStabilityControl() {
        return electronicStabilityControl;
    }

    /**
     * @return the tractionControlBrakingEvent
     */
    public String getTractionControlBrakingEvent() {
        return tractionControlBrakingEvent;
    }

    /**
     * @return the surfaceTemp
     */
    public String getSurfaceTemp() {
        return surfaceTemp;
    }

    /**
     * @return the dewPoint
     */
    public String getDewPoint() {
        return dewPoint;
    }

    /**
     * @return the ambientAirTemperature
     */
    public String getAmbientAirTemperature() {
        return ambientAirTemperature;
    }

    /**
     * @return the humidity
     */
    public String getHumidity() {
        return humidity;
    }
}
