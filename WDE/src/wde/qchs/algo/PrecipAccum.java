// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.Stage24Precip;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;

import java.util.ArrayList;

/**
 *
 */
public class PrecipAccum extends LikeInstrument {
    /**
     * Configuration item that determines how many hours in the past to
     * accumulate precipitation data, i.e. 3 hour precip is 3 hours
     */
    protected int m_nHours = -1;
    /**
     * The configuration object is persisted so the hour parameters
     * can be determined upon the first check execution
     */
    protected Config m_oConfig;
    /**
     * Service for requesting precipitation background fields
     */
    protected Stage24Precip m_oPrecip = Stage24Precip.getInstance();


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code PrecipAccum}
     * </p>
     */
    public PrecipAccum() {
        m_oConfig = ConfigSvc.getInstance().getConfig(this);
    }


    /**
     * Check the precipitation accumulation with the neighbor values within
     * 50 km around the test point. This algorithm uses Stage 2 and Stage 4
     * precipitation data--known as the stage neighbors--and values are in cm.
     * More than 75% of the stage neighbors must have a value to perform this
     * check. If the test value is less than 0.1 cm, then at least 3 stage
     * neighbors must have a value of less than 1.0 cm to pass. Otherwise, the
     * standard obs type neighbors are acquired and combined with the stage
     * neighbor values to determine a valie precipiation accumulation range
     * within which the test value must fall for the check to pass.
     *
     * @param nObsTypeId observation type.
     * @param iSensor    recording sensor.
     * @param iObs       observation in question.
     * @param oResult    results of the check, after returning from this method.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        // only set the number of hours to retrieve once
        if (m_nHours < 0)
            m_nHours = m_oConfig.getInt(Integer.toString(nObsTypeId), 0);

        if (m_nHours == 0)
            return;

        int nLat = iObs.getLatitude();
        int nLon = iObs.getLongitude();
        long lTimestamp = iObs.getObsTimeLong();
        // retrieve the background field in a 50km radious about the test point
        double[][] dValues = m_oPrecip.getPrecipAccum(MathUtil.fromMicro(nLat),
                MathUtil.fromMicro(nLon), 50000, m_nHours);

        if (dValues == null)
            return;

        double dMin = Double.MAX_VALUE;
        double dMax = -Double.MAX_VALUE;
        // determine the percentage of background values that have no data
        int nTotal = 0;
        int nNoData = 0;
        int n1CmCount = 0;
        for (int nY = 0; nY < dValues.length; nY++) {
            for (int nX = 0; nX < dValues[nY].length; nX++) {
                ++nTotal;
                double dValue = dValues[nY][nX];
                if (Double.isNaN(dValue))
                    ++nNoData;
                else {
                    // count how many stage neighbors are 1 cm or less
                    if (dValue <= 1.0)
                        ++n1CmCount;

                    // determine the min and max stage values
                    if (dValue > dMax)
                        dMax = dValue;

                    if (dValue < dMin)
                        dMin = dValue;
                }
            }
        }

        // the algorithm requires more than 3/4 valid background field values
        if (nTotal < nNoData * 4)
            return;

        // precipitation values are in cm
        double dValue = iObs.getValue();
        if (dValue > 0.1) {
            // retrieve a background field for the current obs type
            ArrayList<IObs> oObsSet = new ArrayList<IObs>();
            m_oObsMgr.getBackground(nObsTypeId, nLat - m_nGeoRadiusMax,
                    nLon - m_nGeoRadiusMax, nLat + m_nGeoRadiusMax,
                    nLon + m_nGeoRadiusMax, lTimestamp + m_lTimerangeMin,
                    lTimestamp + m_lTimerangeMax, oObsSet);

            // update the min and max neighbor values
            int nIndex = oObsSet.size();
            while (nIndex-- > 0) {
                double dNeighborValue = oObsSet.get(nIndex).getValue();
                if (dNeighborValue > dMax)
                    dMax = dNeighborValue;

                if (dNeighborValue < dMin)
                    dMin = dNeighborValue;
            }

            // create the comparison range
            double dTmin = dMin - 0.5;
            double dTmax = dMax * 2.0;

            if (dTmax < 0.8)
                dTmax = 0.8;
            if (dTmax > 5.0)
                dTmax = dMax + 2.5;

            // the test passes if the target value is within the range
            oResult.setPass(dTmin < dValue && dValue < dTmax);
        } else {
            // at least 3 stage neighbors must be <= 10mm to pass
            oResult.setPass(n1CmCount > 2);
        }

        if (oResult.getPass())
            oResult.setConfidence(1.0);
        oResult.setRun();
    }
}
