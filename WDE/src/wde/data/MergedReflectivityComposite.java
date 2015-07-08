/************************************************************************
 * Source filename: MergedReflectivityComposite.java
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

public class MergedReflectivityComposite extends Grib2DataHolder {

    /**
     * Keeps a map of grib2 files to memory objects
     */
    protected static HashMap<String, Grib2DataHolder> g2dMap = new HashMap<>();
    private float altitudeAboveMsl;
    private Array lat = null; // float
    private Array lon = null; // float
    private double refTime;
    private String refTimeISO = null;
    private double time;
    private Array var209 = null; // float

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
     * @return the altitudeAboveMsl
     */
    public float getAltitudeAboveMsl() {
        return altitudeAboveMsl;
    }

    /**
     * @return Array of latitude in float
     */
    public Array getLat() {
        return lat;
    }

    /**
     * @return Array of longitude in float
     */
    public Array getLon() {
        return lon;
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
     * @return the time
     */
    public double getTime() {
        return time;
    }

    /**
     * @return Array of var209 values in float
     */
    public Array getVar209() {
        return var209;
    }

    protected void populate(Group root) throws IOException {
        lat = root.findVariable("lat").read();
        lon = root.findVariable("lon").read();
        refTime = root.findVariable("reftime").read().getDouble(0);
        refTimeISO = (String) root.findVariable("reftime_ISO").read().getObject(0);
        time = root.findVariable("time").read().getDouble(0);
        var209 = root.findVariable("VAR209-11-1_FROM_161-0--1_altitude_above_msl").read();
    }
}
