// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file StationCode.java
 */
package clarus.cs.xml;

/**
 * Provides a means of reading station code information from a StringBuilder
 * through the overriden method
 * {@link StationCode#characters(java.lang.StringBuilder)}
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather and set Station
 * Code values.
 * </p>
 */
class StationCode extends DataValue
{
	/**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code StationCode}. Initialization done through
	 * base class {@link DataValue#init} method.
     * </p>
     */
	StationCode()
	{
	}

	/**
	 * Sets the station code for the base class instance of {@code XmlCollecotr}
	 * to the integer value represented by the StringBuilder {@code sBuffer}.
	 *
	 * @param sBuffer the StringBuilder representing the sensor index integer
	 * value.
	 */
	@Override
	public void characters(StringBuilder sBuffer)
	{
		m_oXmlCollector.setStationCode(sBuffer);
	}
}
