// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Persistence.java
 */
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.QCh;

import java.util.ArrayList;

/**
 * Checks observations against previous observations to ensure the difference
 * between the values for the same sensor is more than the configured
 * persistence threshold value for the sensor.
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class Persistence extends QCh {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Persistence}.
     * </p>
     */
    public Persistence() {
    }


    /**
     * Checks the provided observation against previous observations to ensure
     * the difference between the values for the same sensor is more than the
     * configured persistence threshold value for the provided sensor.
     *
     * @param nObsTypeId type of observation being tests.
     * @param iSensor    observing sensor.
     * @param iObs       observation be tested.
     * @param oResult    results of the test.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        long lTimestamp = iObs.getObsTimeLong();

        ArrayList<IObs> oObsSet = new ArrayList<IObs>();
        m_oObsMgr.getSensors(nObsTypeId, iObs.getSensorId(),
                lTimestamp - (long) (iSensor.getPersistInterval() * 1000.0),
                lTimestamp, oObsSet);

        // there needs to be at least one value for the comparison
        if (oObsSet.size() == 0)
            return;

        int nIndex = 0;
        boolean bPassed = false;
        while (nIndex < oObsSet.size() && !bPassed) {
            IObs iOtherObs = oObsSet.get(nIndex++);
            if (lTimestamp - iOtherObs.getObsTimeLong() > 0) {
                // the difference between the current obs and a previous obs
                // for the same sensor must be more than the threshold value
                double dValueDiff = iObs.getValue() - iOtherObs.getValue();
                double dThreshold = iSensor.getPersistThreshold();
                bPassed = dValueDiff > dThreshold || -dValueDiff > dThreshold;
            }
        }

        // indicate the test was run
        if (bPassed) {
            oResult.setPass(bPassed);
            oResult.setConfidence(1.0);
        }
        oResult.setRun();
    }
}
