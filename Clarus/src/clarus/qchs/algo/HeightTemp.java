// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file HeightTemp.java
 */
package clarus.qchs.algo;

import clarus.qchs.ModObsSet;
import clarus.qedc.IObs;

/**
 * The HeightTemp class is a simple structure used to hold a height
 * and temperature value, and return a modified temperature value, based
 * off location and NWS balloon observations.
 * @author scot.lange
 */
public class HeightTemp extends ModObsSet
{
	/**
	 * Standard atmosphere lapse rate, K m-1
	 */
	private static final double GAMMA = 0.0065;
	/**
	 * Exponent constants based on ideal gas law coefficient for dry air
	 * and the mean gravitational acceleration
	 */
	private static final double EXP = 9.80665 / (287.053072047065 * GAMMA);

	/**
	 * Balloon height value.
	 */
	double m_dHeight;
	/**
	 * Balloon temperature value, in Kelvin.
	 */
	double m_dTemperature;


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code HeightTemp}.
	 * </p>
	 */
	public HeightTemp()
	{
	}


	public void setHeightTemp(double dHeight, double dTemp)
	{
		m_dHeight = dHeight;
		// adjust from C to K
		m_dTemperature = dTemp + 273.15;
	}


	/**
	 * Calculates an adjusted temperature value, based off the recorded value,
	 * its elevation, and NWS balloon recorded values.
	 * @param iObs observation to modify.
	 * @return the newly adjusted temperature value.
	 */
	@Override
	public double modifyValue(IObs iObs)
	{
		double dTempSta = m_dTemperature + (m_dHeight - iObs.getElev()) * GAMMA;
		double dTempSL = m_dTemperature + m_dHeight * GAMMA;

		return (iObs.getValue() * Math.pow(dTempSL / dTempSta, EXP));
	}
}
