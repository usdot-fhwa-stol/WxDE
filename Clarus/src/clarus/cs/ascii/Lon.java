// Copyright (c) 2011 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Lon.java
 */

package clarus.cs.ascii;

/**
 * Provides a means of reading longitude information from a string buffer
 * through the overriden method
 * {@link Lon#readData(java.lang.StringBuilder)}
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare geo-coordinate positioning.
 * </p>
 */
class Lon extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Lon}
	 * </p>
	 */
	Lon()
	{
	}
	

    /**
     * Sets the longitude for the base classes instance of
     * {@code CsvCollector} to the integer value represented by the supplied
     * string buffer.
     * <p>
     * Wraps {@link CsvCollector#setLon(java.lang.StringBuilder)}
     * </p>
     * @param sBuffer The string buffer representing the longitude position
     * value.
     */
	@Override
	public void readData(StringBuilder sBuffer)
	{
		m_oCsvCollector.setLon(sBuffer);
	}
}
