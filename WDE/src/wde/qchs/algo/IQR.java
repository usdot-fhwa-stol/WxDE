// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IQR.java
 */
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.ModObs;
import wde.qchs.ModObsSet;
import wde.util.Introsort;

import java.util.ArrayList;
import java.util.Comparator;

/**
 * Class provides a Quality checking method to check whether the value is within
 * a tolerable amount of one standard deviation away from other nearby
 * observation values.
 */
public class IQR extends Barnes implements Comparator<ModObs> {
    /**
     * <b> Default Constructor </b>
     * <p/>
     * <p>
     * Creates new instances of {@code IQR}. Initialization done through
     * {@link IQR#init} method.
     * </p>
     */
    public IQR() {
    }

    static void runIQR(int nSensorId, double dTestValue, IObs iObs,
                       QChResult oResult, double dMinToleranceBound, double dErrorLimit,
                       ModObsSet oModObsSet, Comparator<ModObs> oSort) {
        // filter obs outside of elevation range
        int nMinElev = iObs.getElevation() - 350;
        int nMaxElev = iObs.getElevation() + 350;
        int nIndex = oModObsSet.size();
        while (nIndex-- > 0) {
            ModObs oTempObs = oModObsSet.get(nIndex);
            int nElev = oTempObs.m_tElev;
            // remove the target obs from the background field too
            if (nElev < nMinElev || nElev > nMaxElev ||
                    oTempObs.m_nSensorId == nSensorId)
                oModObsSet.remove(nIndex);
        }

        // If we have enough neighbors continue, otherwise return early
        if (oModObsSet.size() < 5)
            return;

        Introsort.usort(oModObsSet, oSort);

        // Calc median:
        double dPredictValue = calcQuantile(0.5, oModObsSet);

        // Calc IQR = interquartile range = (75th percentile) - (25th percentile)
        double dIQR = calcQuantile(0.75, oModObsSet)
                - calcQuantile(0.25, oModObsSet);

        // In the normal distribution, the 0.25 and 0.75 quantiles
        // are at -/+ 0.67448 * sigma respectively.
        // So the iqr = 2 * 0.67448 * sigma = 1.348960 * sigma,
        // and stddev = 0.74131 * iqr
        double dStdDev = 0.74131 * dIQR;

        double dAbsError = Math.abs(dTestValue - dPredictValue);

        if (dAbsError <= Math.max(dMinToleranceBound, dErrorLimit * dStdDev)) {
            oResult.setPass(true);
            oResult.setConfidence(1.0);
        }
        oResult.setRun();
    }

    /**
     * Calculate the specified quantile. A quantile is a percentile but with
     * a 0 to 1 domain instead of 0 to 100.  For example, the 75th percentile
     * is the 0.75 quantile.
     *
     * @param dQuantile        Quantile to be calculated
     * @param nNumNeighborVals Number of Neighbor Values
     * @param dNeighborValues  Array of Neighboring Values
     * @return the value of the specified quantile
     */
    static double calcQuantile(double dQuantile,
                               ModObsSet oNeighborValues) {
        // finds the whole number index with which to reference the array
        int nIndex = (int) (dQuantile * oNeighborValues.size());

        // finds the fraction that was stripped off when nIndex was calculated
        double dFrac = dQuantile * oNeighborValues.size() - nIndex;

        // return the value at nIndex plus dFrac times the difference between
        // the value and the next largest value
        return (oNeighborValues.get(nIndex).m_dValue + dFrac *
                (oNeighborValues.get(nIndex + 1).m_dValue -
                        oNeighborValues.get(nIndex).m_dValue));
    }

    /**
     * Gets an ArrayList of all the platforms within a given radius and removes
     * the platforms that aren't within a given elevation range. Records the
     * test as a pass only if the specified observation value is within the
     * tolerance bound of a standard deviation of the values around it
     * geographically.
     *
     * @param nObsTypeId Observation Type Id
     * @param iSensor    Observation Sensor
     * @param iObs       Observation
     * @param oResult    Result of the Quality Check
     * @see IQR#calcQuantile(double, java.util.ArrayList)
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor, 
			IObs iObs, QChResult oResult)
		{
        // retrieve the background field
        int nLat = iObs.getLatitude();
        int nLon = iObs.getLongitude();
        long lTimestamp = iObs.getObsTimeLong();

        ArrayList<IObs> oObsSet = new ArrayList<IObs>();

        // bounding box based on latitude to compensate for the spherical earth
        int nLonRadius = sphereAdjust(m_nGeoRadiusMax, nLat);
        m_oObsMgr.getBackground(nObsTypeId, nLat - m_nGeoRadiusMax,
                nLon - nLonRadius, nLat + m_nGeoRadiusMax,
                nLon + nLonRadius, lTimestamp + m_lTimerangeMin,
                lTimestamp + m_lTimerangeMax, oObsSet);

        // obs need to be copied into a modified obs set to interface it with
        // the IQR algorithm that is also used by the dewpoint spatial test
        ModObsSet oModObsSet = m_oLock.readLock();
        estimateValue(nLat, nLon, iObs, oObsSet, oModObsSet);
        runIQR(iSensor.getId(), oModObsSet.modifyValue(iObs), iObs,
                oResult, m_dSdMin, m_dSdMax, oModObsSet, this);
        m_oLock.readUnlock();
    }

    /**
     * Compares the values of two Observations.
     *
     * @param iObs1 first observation to be compared.
     * @param iObs2 second observation to be compared.
     * @return 0 if the observations have the same value.
     * <br /> -1 if the first observation is less than the second.
     * <br /> 1 if the first observation is greater than the second.
     */
    public int compare(ModObs oModObsL, ModObs oModObsR) {
        if (oModObsL.m_dValue < oModObsR.m_dValue)
            return -1;

        if (oModObsL.m_dValue > oModObsR.m_dValue)
            return 1;

        return 0;
    }
}
