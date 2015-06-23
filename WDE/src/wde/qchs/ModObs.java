// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ModObs
 */
package wde.qchs;

import wde.obs.IObs;
import wde.util.MathUtil;

/**
 * Wraps observation data with a modification flag indicating when the
 * observation has been modified.
 */
public class ModObs {
    /**
     * Flag to show the record has been modified, false by default.
     */
    public boolean m_bModified;
    /**
     * The identifier of the sensor that recorded the observation value.
     */
    public int m_nSensorId;
    /**
     * Observation latitude, 0 by default.
     */
    public int m_nLat;
    /**
     * Observation longitude, 0 by default.
     */
    public int m_nLon;
    /**
     * Observation elevation based on the sensor location.
     */
    public int m_tElev;
    /**
     * Distance between the latitude-longitude pairs supplied in
     * {@link ModObs#setDstSqr(int, int, int, int)}, 0 by default.
     */
    public long m_lDstSqr;
    /**
     * Observation value, 0 by default.
     */
    public double m_dValue;


    /**
     * Sets all attributes to default values.
     */
    ModObs() {
        clear();
    }

    public static double sphere(int nLat) {
        return Math.cos(MathUtil.fromMicro(nLat) * Math.PI / 180.0);
    }

    /**
     * Sets all attributes to default values.
     */
    void clear() {
        m_bModified = false;
        m_nLat = m_nLon = Integer.MIN_VALUE;
        m_lDstSqr = 0L;
        m_dValue = 0.0;
    }

    /**
     * Copies the values from an IObs to this modified obs.
     *
     * @param iObs The interface view of an observation.
     */
    public void setObs(IObs iObs) {
        m_nSensorId = iObs.getSensorId();
        m_nLat = iObs.getLatitude();
        m_nLon = iObs.getLongitude();
        m_tElev = iObs.getElevation();
        m_dValue = iObs.getValue();
    }

    /**
     * Calculates the distance squared between the two latitude-longitude pairs.
     *
     * @param nLat1 latitude of latitude-longitude pair 1.
     * @param nLon1 longitude of latitude-longitude pair 1.
     * @param nLat2 latitude of latitude-longitude pair 2.
     * @param nLon2 longitude of latitude-longitude pair 2.
     */
    public void setDstSqr(int nLat, int nLon) {
        long lDeltaLat = nLat - m_nLat;
        // adjust longitude to compensate for a spherical earth
        // this essentialy moves the point closer to or farther from the target
        long lDeltaLon = MathUtil.toMicro(MathUtil.fromMicro(nLon - m_nLon) * sphere(nLat));
        m_lDstSqr = lDeltaLat * lDeltaLat + lDeltaLon * lDeltaLon;
    }
}
