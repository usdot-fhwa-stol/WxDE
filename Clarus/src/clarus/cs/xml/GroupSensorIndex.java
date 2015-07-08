// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file GroupSensorIndex.java
 */
package clarus.cs.xml;

/**
 * Normal behavior for the XmlCollector is to reset the sensor index back to
 * the default sensor index after each value is read in.
 *
 * <p>
 * This class provides a means for one sensor index to be set for a whole group
 * of observations without it being reset to the default sensor index.
 *</p>
 *
 * <p>
 * Extends {@code DataValue} to provide a way to trigger the start of a group
 * of observations with the same sensor index
 * </p>
 */
public class GroupSensorIndex extends DataValue
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instance of {@code GroupSensorIndex}. Initialization done
	 * through base class {@link DataValue#init} method.
	 * </p>
	 */
	GroupSensorIndex()
	{
	}


	/**
	 * At the start of the group a flag in the XmlCollector is set
	 * to ignore the default sensor index so that a sensor index is set
	 * it stays until the end of this group.
	 */
	@Override
	public void start()
	{
		// tells the collector not to reset the current sensor index
		// to the default sensor index
		m_oXmlCollector.setIgnoreDefault(true);
	}


	/**
	 * This method is overriden so that nothing happens when it is called
	 * because this DataValue does not have an observation to read in.
	 */
	@Override
	public void characters(StringBuilder sBuffer)
	{
	}


	/**
	 * At the end of the group the flag in the XmlCollector is set to false
	 * so it will continue reseting the current sensor index to the default.
	 */
	@Override
	public void end()
	{
		// tells the collector to go back to reseting the
		// current sensor index to the default sensor index
		m_oXmlCollector.setIgnoreDefault(false);
	}
}

