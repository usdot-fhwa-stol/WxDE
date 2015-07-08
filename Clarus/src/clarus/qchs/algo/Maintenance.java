// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Maintenance.java
 */
package clarus.qchs.algo;

import clarus.emc.ISensor;
import clarus.qchs.QCh;
import clarus.qedc.IObs;

/**
 * Determines whether or not the observation was recorded from a sensor that was
 * under maintenance at the time of observation.
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class Maintenance extends QCh
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * creates new instances of {@code Maintenance}. Relies on base class
	 * {@code init} methods to initialize attributes.
	 * </p>
	 */
	public Maintenance()
	{
	}


	/**
	 * Sets pass to false if the sensor was under maintenance when the
	 * observation was being recorded.
	 * 
	 * @param nObsTypeId type of observation being tested.
	 * @param iSensor observing sensor.
	 * @param iObs observation being tested.
	 * @param oResult the results of the test.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		// the maintenance check is odd in that it can only set the
		// flag to false as there is no maintenance passing state
		if (iSensor.underMaintenance(iObs.getTimestamp()))
			oResult.setRun();
	}
}
