// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Date.java
 */
package clarus.cs.xml;

/**
 * In one of the Xml files the date and time are expressed separately.
 * This class provides a means of reading only date information as a
 * StringBuilder and setting it so that the time can be set later to complete
 * the Timestamp.
 *
 * <p>
 * Extends {@code Timestamp} to provide a way to gather and set only the date
 * and for the time to be appended later
 * </p>
 */
class Date extends Timestamp
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instance of {@code Date}. Initialization done through base
	 * class {@link DataValue#init} method.
	 * </p>
	 */
	Date()
	{
	}

	/**
	 * Appends the date contained in {@code sBuffer} to the timestamp
	 * StringBuilder in the base class instance of {@code XmlCollector}
	 *
	 * @param sBuffer StringBuilder representing the date
	 */
	@Override
	public void characters(StringBuilder sBuffer)
	{
		m_oXmlCollector.setDate(sBuffer.toString());
	}
}
