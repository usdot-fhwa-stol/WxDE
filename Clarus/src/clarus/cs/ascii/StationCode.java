// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.cs.ascii;

/**
 * Provides a means of reading station code information from a string buffer
 * through the overriden method
 * {@link StationCode#readData(java.lang.StringBuilder)}
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare station code observation values.
 * </p>
 */
class StationCode extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code StationCode}
	 * </p>
	 */
	StationCode()
	{
	}
	
    /**
     * Sets the sensor station code for the base classes instance of
     * {@code CsvCollector} to the integer value represented by the supplied
     * string buffer.
     *
     * @param sBuffer The string buffer representing the station code integer
     * value.
     */
	@Override
	public void readData(StringBuilder sBuffer)
	{
		m_oCsvCollector.setStationCode(sBuffer);
	}
}
