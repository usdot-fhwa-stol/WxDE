// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Complete.java
 */
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.QCh;

/**
 * Sets the results of the check to a completed passed state.
 * <p/>
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class Complete extends QCh {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instance of {@code Complete}.
     * </p>
     */
    public Complete() {
    }


    /**
     * Sets the results to a completed, passed state, with a confidence of 1.
     * Parameters have no effect on this method.
     *
     * @param nObsTypeId type of observation being tested.
     * @param iSensor    observing sensor.
     * @param iObs       obsevation being tested.
     * @param oResult    results of the test after the method call returns.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        oResult.setPass(true);
        oResult.setConfidence(1.0);
        oResult.setRun();
    }
}
