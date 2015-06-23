// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file LikeInstrument.java
 */
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;

import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * Tests observation values against the mean of the surrounding observations
 * recorded by the same platform.
 * <p/>
 * <p>
 * Extends {@code Step} to inherit timerange, query and the basic quality
 * checking attributes and methods.
 * </p>
 */
public class LikeInstrument extends Step {
    /**
     * Min configured radius from the observation location to consider.
     */
    protected int m_nGeoRadiusMin;
    /**
     * Max configured radius from the observation location to consider.
     */
    protected int m_nGeoRadiusMax;
    /**
     * Standard deviation min.
     */
    protected double m_dSdMin;
    /**
     * Standard deviation max.
     */
    protected double m_dSdMax;
    /**
     * Minimum number of observations to allow for mean computation.
     * Must be at least two.
     */
    protected int m_nObsCountMin;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code LikeInstrument}
     * </p>
     */
    public LikeInstrument() {
    }


    /**
     * Initializes <i> this </i> instance with data retrieved from the
     * result set. Configures the Like Instrument test.
     * <p>
     * This is called through the base class
     * {@link Step#init(int, int, int, double, int, java.sql.Connection) }
     * method.
     * </p>
     *
     * @param rs set containing the {@link Step#QCHCONFIG_QUERY} query.
     * @throws java.lang.Exception
     */
    @Override
    protected void init(ResultSet rs) throws Exception {
        super.init(rs);

        m_nGeoRadiusMin = rs.getInt(3);
        m_nGeoRadiusMax = rs.getInt(4);
        m_dSdMin = rs.getDouble(5);
        m_dSdMax = rs.getDouble(6);
        m_nObsCountMin = rs.getInt(7);
    }


    /**
     * Checks the observation by comparing the observation value to the mean of
     * all observation-values of the same type recorded on like-sensors from the
     * same platform, within the configured radius and timerange. If the value
     * falls within a like-threshold defined by the sensor it passes the check.
     *
     * @param nObsTypeId observation type.
     * @param iSensor    recording sensor.
     * @param iObs       observation in question.
     * @param oResult    results of the check, after returning from this method.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        // retrieve the background field
        int nLat = iObs.getLatitude();
        int nLon = iObs.getLongitude();
        long lTimestamp = iObs.getObsTimeLong();

        ArrayList<IObs> oObsSet = new ArrayList<IObs>();
        m_oObsMgr.getBackground(nObsTypeId, nLat - m_nGeoRadiusMax,
                nLon - m_nGeoRadiusMax, nLat + m_nGeoRadiusMax,
                nLon + m_nGeoRadiusMax, lTimestamp + m_lTimerangeMin,
                lTimestamp + m_lTimerangeMax, oObsSet);

        // this algorithm compares the current obs with the mean of
        // all the obs in the background inclusive
        int nPlatformId = iSensor.getPlatformId();
        int nObsCount = 0;
        double dSum = 0.0;

        int nIndex = oObsSet.size();
        while (nIndex-- > 0) {
            IObs iFieldObs = oObsSet.get(nIndex);
            // getting the platform id is computationally expensive
            ISensor iFieldSensor = sensorDao.getSensor(iFieldObs.getSensorId());
            if (iFieldSensor != null && iFieldSensor.getPlatformId() == nPlatformId) {
                // increment counter and accumulate sum for the other platforms
                ++nObsCount;
                dSum += iFieldObs.getValue();
            }
        }

        // there must be at least two obs for the mean comparison
        if (nObsCount < m_nObsCountMin)
            return;

        double dMean = dSum / nObsCount;
        double dValue = iObs.getValue();
        double dThreshold = iSensor.getLikeThreshold();

        // indicate the test was run
        if (dValue >= dMean - dThreshold && dValue <= dMean + dThreshold) {
            oResult.setPass(true);
            oResult.setConfidence(1.0);
        }
        oResult.setRun();
    }
}
