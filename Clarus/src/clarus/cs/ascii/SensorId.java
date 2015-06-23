// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SensorId.java
 */

package clarus.cs.ascii;

/**
 * Provides a means of reading sensor index information from a string buffer
 * through the overriden method
 * {@link SensorId#readData(java.lang.StringBuilder)}
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare SensorId observation values.
 * </p>
 */
class SensorId extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code SensorId}
	 * </p>
	 */
	SensorId()
	{
	}
	

    /**
     * Sets the sensor index for the base classes instance of
     * {@code CsvCollector} to the integer value represented by the supplied
     * string buffer.
     * <p>
     * Wraps {@link CsvCollector#setSensorIndex(java.lang.StringBuilder)}
     * </p>
     * @param sBuffer The string buffer representing the sensor index integer
     * value.
     */
	@Override
	public void readData(StringBuilder sBuffer)
	{
		m_oCsvCollector.setSensorIndex(sBuffer);
	}
}
