// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SensorRange.java
 */
package clarus.qchs.algo;

import clarus.emc.ISensor;
import clarus.qchs.QCh;
import clarus.qedc.IObs;

/**
 * Detects observations that fall outside the range of sensor-hardware or
 * theoretical limits.
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class SensorRange extends QCh
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code SensorRange}.
	 * </p>
	 */
	public SensorRange()
	{
	}


	/**
	 * Detects observations that fall outside the range of sensor-hardware or
	 * theoretical limits. Pass result is set to true if the observation value
	 * is greater than the min range, and less than the max range.
	 *
	 * @param nObsTypeId type of observation being tested.
	 * @param iSensor observing sensor.
	 * @param iObs observation being tested.
	 * @param oResult result of the test.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		if (iObs.getValue() >= iSensor.getRangeMin() &&
			iObs.getValue() <= iSensor.getRangeMax())
		{
			oResult.setPass(true);
			oResult.setConfidence(1.0);
		}
		oResult.setRun();
	}
}
