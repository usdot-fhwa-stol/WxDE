// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.RSAS;

import java.sql.ResultSet;
import java.util.Arrays;

/**
 * Class provides a Quality checking method to check whether the value is within
 * a tollerable amount of one standard deviation away from other nearby
 * observation values.
 */
public class IQRRSAS extends LikeInstrument {
    /**
     * Minimum Tolerance Bound
     */
    protected double m_dMinToleranceBound;
    /**
     * Error Limit
     */
    protected double m_dErrorLimit;
    /**
     * Service for requesting RSAS background fields
     */
    protected RSAS m_oRSAS = RSAS.getInstance();


    /**
     * <b> Default Constructor </b>
     * <p/>
     * <p>
     * Creates new instances of {@code IQRRSAS}. Initialization done through
     * the {@link IQRRSAS#init} method.
     * </p>
     */
    public IQRRSAS() {
    }


    /**
     * Calls the init method of its super class and sets the
     * {@code MinToleranceBound} and {@code ErrorLimit} based on values
     * read in from the super class init method's query.
     *
     * @param rs
     * @throws java.lang.Exception
     */
    @Override
    protected void init(ResultSet rs) throws Exception {
        super.init(rs);

        m_dMinToleranceBound = m_dSdMin;
        m_dErrorLimit = m_dSdMax;
    }


    /**
     * Gets an array of all the readings within a given radius from RSAS and
     * uses it to test the sensor reading. Records the
     * test as a pass only if the specified observation value is within the
     * tolerance bound of a standard deviation of the values around it
     * geographically.
     *
     * @param nObsTypeId Observation Type Id
     * @param iSensor    Observation Sensor
     * @param iObs       Observation
     * @param oResult    Result of the Quality Check
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor, IObs iObs, QChResult oResult) {
        // retrieve the background field
        double[] dValues = m_oRSAS.getReadings(nObsTypeId,
                iObs.getLatitude(), iObs.getLongitude());

        // If we have enough neighbors continue, otherwise return early
        if (dValues == null || dValues.length < m_nObsCountMin)
            return;

        Arrays.sort(dValues);

        // Calc median:
        double dPredictValue = calcQuantile(0.5, dValues);

        // Calc IQR = interquartile range = (75th percentile) - (25th percentile)
        double dIQR = calcQuantile(0.75, dValues)
                - calcQuantile(0.25, dValues);

        // In the normal distribution, the 0.25 and 0.75 quantiles
        // are at -/+ 0.67448 * sigma respectively.
        // So the iqr = 2 * 0.67448 * sigma = 1.348960 * sigma,
        // and stddev = 0.74131 * iqr
        double dStdDev = 0.74131 * dIQR;

        double dAbsError = Math.abs(iObs.getValue() - dPredictValue);

        if (dAbsError <= Math.max(m_dMinToleranceBound, m_dErrorLimit * dStdDev)) {
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
    private double calcQuantile(double dQuantile, double[] dNeighborValues) {
        // finds the whole number index with which to reference the array
        int nIndex = (int) (dQuantile * dNeighborValues.length);

        // finds the fraction that was stripped off when nIndex was calculated
        double dFrac = dQuantile * dNeighborValues.length - nIndex;

        // return the value at nIndex plus dFrac times the difference between
        // the value and the next largest value
        return dNeighborValues[nIndex] + dFrac *
                (dNeighborValues[nIndex + 1] - dNeighborValues[nIndex]);
    }
}
