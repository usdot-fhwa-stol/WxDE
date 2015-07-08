// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file PressureObsSet.java
 */
package wde.qchs.algo;

import wde.obs.IObs;
import wde.qchs.ModObsSet;

/**
 * Converts a provided pressure observation to its corresponding sea-level
 * pressure.
 * <p>
 * Extends {@code ModObsSet} to provide standard mod-obs interface, and
 * properties but overrides the modifyValue method to perform sea-level
 * pressure calcuation.
 * </p>
 */
public class PressureObsSet extends ModObsSet {
    //	Source: Bluestein 1997
    /**
     * Lapse rate for standard atmosphere.
     */
    private static final double kGamma = 6.5;
    /**
     * Mean surface temperature for standard atmosphere.
     */
    private static final double kMTAIR = 288.0;
    /**
     * Mean surface pressure for standard atmosphere.
     */
    private static final double kMPSL = 1013.25;
    /**
     * C1 = gamma Rd/g.
     */
    private static final double kC1 = 0.1901631;
    /**
     * C2 = g/gamma Rd.
     */
    private static final double kC2 = 5.258643;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code PressureObsSet}.
     * </p>
     */
    PressureObsSet() {
    }


    /**
     * Calculates the sea-level pressure based off the observation value.
     *
     * @param iObs observation to perform pressure calculation on.
     * @return the calculated pressure value.
     */
    @Override
    public double modifyValue(IObs iObs) {
        double dValue = iObs.getValue();
        return dValue * Math.pow(1.0 + Math.pow((kMPSL / dValue), kC1) *
                (kGamma * iObs.getElevation() / kMTAIR), kC2);
    }
}
