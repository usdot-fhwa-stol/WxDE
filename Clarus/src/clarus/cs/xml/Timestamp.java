// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Timestamp.java
 */
package clarus.cs.xml;

/**
 * Provides a means of reading timestamp information from a StringBuilder
 * through the overriden method
 * {@link Timestamp#characters(java.lang.StringBuilder)}
 *
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather and set
 * timestamp observation values
 * </p>
 */
class Timestamp extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Timestamp}. Initialization done through
	 * base class {@link DataValue#init} method.
	 * </p>
	 */
	Timestamp()
	{
	}


	/**
	 * Sets the formatted timestamp for the base class instance of
	 * {@code XmlCollector} to the timestamp represented by the StringBuilder
	 * 
	 * @param sBuffer StringBuilder representing the timestamp value
	 */
	@Override
	public void characters(StringBuilder sBuffer)
	{
		m_oXmlCollector.setTimestamp(sBuffer);
	}
}
