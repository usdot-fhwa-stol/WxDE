// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SensorIndex.java
 */
package clarus.cs.xml;

/**
 * Overrides a method from {@link DataValue} to provide a means of reading the
 * sensor index from the file and setting the XmlCollector's sensor index to
 * the value read in.
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather and set Sensor
 * Index values.
 * </p>
 */
class SensorIndex extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code SensorIndex}. Initialization done through
	 * base class {@link DataValue#init} method.
	 * </p>
	 */
	SensorIndex()
	{
	}

	/**
	 * Sets the sensor index for the base class instance of {@code XmlCollecotr}
	 * to the integer value represented by the StringBuilder {@code sBuffer}.
	 *
	 * <p>
	 * Wraps {@link XmlCollector#setSensorIndex(java.lang.StringBuilder)}
	 * </p>
	 *
	 * @param sBuffer the StringBuilder representing the sensor index integer
	 * value.
	 */
	@Override
	public void characters(StringBuilder sBuffer)
	{
		m_oXmlCollector.setSensorIndex(sBuffer);
	}
}
