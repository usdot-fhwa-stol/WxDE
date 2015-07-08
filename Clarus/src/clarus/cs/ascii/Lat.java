// Copyright (c) 2011 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Lat.java
 */

package clarus.cs.ascii;

/**
 * Provides a means of reading latitude information from a string buffer
 * through the overriden method
 * {@link Lat#readData(java.lang.StringBuilder)}
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare geo-coordinate positioning.
 * </p>
 */
class Lat extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Lat}
	 * </p>
	 */
	Lat()
	{
	}
	

    /**
     * Sets the latitude for the base classes instance of
     * {@code CsvCollector} to the integer value represented by the supplied
     * string buffer.
     * <p>
     * Wraps {@link CsvCollector#setLat(java.lang.StringBuilder)}
     * </p>
     * @param sBuffer The string buffer representing the latitude position
     * value.
     */
	@Override
	public void readData(StringBuilder sBuffer)
	{
		m_oCsvCollector.setLat(sBuffer);
	}
}
