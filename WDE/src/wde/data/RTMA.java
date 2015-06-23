/************************************************************************
 * Source filename: RTMA.java
 * <p/>
 * Creation date: Mar 4, 2015
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

package wde.data;

import ucar.ma2.Array;
import ucar.nc2.Group;

import java.io.IOException;
import java.util.HashMap;

/**
 * Holds an one hour slice of the RTMA data feed.
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class RTMA extends Grib2DataHolder {

    /**
     * Keeps a map of grib2 files to memory objects
     */
    protected static HashMap<String, Grib2DataHolder> g2dMap = new HashMap<>();
    private Array dewPointTemperatureHeightAboveGround;  // float
    private Array geoPotentialHeightSurface; // float
    private float heightAboveGround;
    private float heightAboveGround1;
    private int lambertConformalProject;
    private Array pressureSurface; // float
    private double refTime;
    private String refTimeISO = null;
    private Array specificHumidityHeightAboveGround; // float
    private Array temperatureHeightAboveGround; // float
    private double time;
    private Array uComponentOfWindHeightAboveGround; // float
    private Array vComponentOfWindHeightAboveGround; // float
    private Array visibilitySurface; // float
    private Array windDirectionFromWhichBlowingHeightAboveGround; // float
    private Array windSpeedGustHeightAboveGround; // float
    private Array windSpeedHeightAboveGround; // float
    private Array x; // float
    private Array y; // float

    /* (non-Javadoc)
     * @see wde.data.Grib2DataHolder#getMap()
     */
    public HashMap<String, Grib2DataHolder> getMap() {
        return g2dMap;
    }

    /**
     * This removes the object from the map.  The object will be garbage collected 
     * if no other references to it exist.
     *
     * @param filepath
     */
    public void remove(String filepath) {
        g2dMap.put(filepath, null);
    }

    /**
     * @return Array of dewPointTemperatureHeightAboveGround in float
     */
    public Array getDewPointTemperatureHeightAboveGround() {
        return dewPointTemperatureHeightAboveGround;
    }

    /**
     * @return Array of geoPotentialHeightSurface in float
     */
    public Array getGeoPotentialHeightSurface() {
        return geoPotentialHeightSurface;
    }

    /**
     * @return the heightAboveGround
     */
    public float getHeightAboveGround() {
        return heightAboveGround;
    }

    /**
     * @return the heightAboveGround1
     */
    public float getHeightAboveGround1() {
        return heightAboveGround1;
    }

    /**
     * @return the lambertConformalProject
     */
    public int getLambertConformalProject() {
        return lambertConformalProject;
    }

    /**
     * @return Array of pressureSurface in float
     */
    public Array getPressureSurface() {
        return pressureSurface;
    }

    /**
     * @return the refTime
     */
    public double getRefTime() {
        return refTime;
    }

    /**
     * @return the refTimeISO
     */
    public String getRefTimeISO() {
        return refTimeISO;
    }

    /**
     * @return Array of specificHumidityHeightAboveGround in float
     */
    public Array getSpecificHumidityHeightAboveGround() {
        return specificHumidityHeightAboveGround;
    }

    /**
     * @return Array of temperatureHeightAboveGround in float
     */
    public Array getTemperatureHeightAboveGround() {
        return temperatureHeightAboveGround;
    }

    /**
     * @return the time
     */
    public double getTime() {
        return time;
    }

    /**
     * @return Array of uComponentOfWindHeightAboveGround in float
     */
    public Array getuComponentOfWindHeightAboveGround() {
        return uComponentOfWindHeightAboveGround;
    }

    /**
     * @return Array of vComponentOfWindHeightAboveGround in float
     */
    public Array getvComponentOfWindHeightAboveGround() {
        return vComponentOfWindHeightAboveGround;
    }

    /**
     * @return Array of visibilitySurface in float
     */
    public Array getVisibilitySurface() {
        return visibilitySurface;
    }

    /**
     * @return Array of windDirectionFromWhichBlowingHeightAboveGround in float
     */
    public Array getWindDirectionFromWhichBlowingHeightAboveGround() {
        return windDirectionFromWhichBlowingHeightAboveGround;
    }

    /**
     * @return Array of windSpeedGustHeightAboveGround in float
     */
    public Array getWindSpeedGustHeightAboveGround() {
        return windSpeedGustHeightAboveGround;
    }

    /**
     * @return Array of windSpeedHeightAboveGround in float
     */
    public Array getWindSpeedHeightAboveGround() {
        return windSpeedHeightAboveGround;
    }

    /**
     * @return Array of x in float
     */
    public Array getX() {
        return x;
    }

    /**
     * @return Array of y in float
     */
    public Array getY() {
        return y;
    }

    protected void populate(Group root) throws IOException {
        dewPointTemperatureHeightAboveGround = root.findVariable("Dewpoint_temperature_height_above_ground").read();
        geoPotentialHeightSurface = root.findVariable("Geopotential_height_surface").read();
        heightAboveGround = root.findVariable("height_above_ground").read().getFloat(0);
        heightAboveGround1 = root.findVariable("height_above_ground1").read().getFloat(0);
        lambertConformalProject = root.findVariable("LambertConformal_Projection").read().getInt(0);
        pressureSurface = root.findVariable("Pressure_surface").read();
        refTime = root.findVariable("reftime").read().getDouble(0);
        refTimeISO = (String) root.findVariable("reftime_ISO").read().getObject(0);
        specificHumidityHeightAboveGround = root.findVariable("Specific_humidity_height_above_ground").read();
        temperatureHeightAboveGround = root.findVariable("Temperature_height_above_ground").read();
        time = root.findVariable("time").read().getDouble(0);
        uComponentOfWindHeightAboveGround = root.findVariable("u-component_of_wind_height_above_ground").read();
        vComponentOfWindHeightAboveGround = root.findVariable("v-component_of_wind_height_above_ground").read();
        visibilitySurface = root.findVariable("Visibility_surface").read();
        windDirectionFromWhichBlowingHeightAboveGround = root.findVariable("Wind_direction_from_which_blowing_height_above_ground").read();
        windSpeedGustHeightAboveGround = root.findVariable("Wind_speed_gust_height_above_ground").read();
        windSpeedHeightAboveGround = root.findVariable("Wind_speed_height_above_ground").read();
        x = root.findVariable("x").read();
        y = root.findVariable("y").read();
    }

}
