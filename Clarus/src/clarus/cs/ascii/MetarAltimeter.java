// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.cs.ascii;

import clarus.emc.IStation;

/**
 *
 */
public class MetarAltimeter extends DataValue
{
	static final double EXP = 5.2561;
	static final double MULT = 0.0065 / 288.0;


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code DataValue}
	 * </p>
	 */
	MetarAltimeter()
	{
	}
	

	/**
     * The base class data value method is overridden to provide a specialized
	 * conversion between a National Weather Service MetarAltimeter reading and the
	 * desired station pressure.
     *
     * @param sBuffer The string buffer containing the value.
     */
	@Override
	public void readData(StringBuilder sBuffer)
	{
		if (sBuffer.length() == 0)
		{
			m_dValue = Double.NaN;
			return;
		}

		try
		{
			IStation iStation = null;
			String sValue = sBuffer.toString();
			if (!ignore(sValue) && (iStation = m_oCsvCollector.getStation()) != null)
			{
				m_dValue = Double.parseDouble(sValue);
				// first adjust the altimeter inHg value to station pressure
				m_dValue *= Math.pow(1.0 - MULT * iStation.getElev(), EXP);

				// apply the configured multiplier and convert the units
				m_dValue *= m_dMultiplier;
				m_dValue = m_oUnitConv.convert(m_dValue);
			}
		}
		catch (Exception oException)
		{
		}
	}
}
