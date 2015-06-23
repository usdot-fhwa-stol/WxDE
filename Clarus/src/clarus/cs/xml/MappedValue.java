// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file MappedValue.java
 */
package clarus.cs.xml;

import clarus.cs.MappedValues;

/**
 * Provides the ability to retrieve a numerical data value from a mapped string,
 * using the {@link MappedValues#getValue(int, int, java.lang.CharSequence)}
 * method
 *
 * <p>
 * Extension of {@link DataValue}
 * </p>
 */
class MappedValue extends DataValue
{
	/**
	 * The global instance of {@code MappedValues}.
	 */
	private MappedValues m_oMappedValues = MappedValues.getInstance();


	/**
     * <b> Default Constructor </b>
     *
     * <p>
     * Creates new instances of {@code MappedValue}. Initialization done through
	 * base class {@link DataValue#init} method.
     * </p>
     */
	MappedValue()
	{
	}
	
	/**
	 * If {@code sBuffer} does not contain a value that is supposed to be
	 * ignored then MappedValues attempts to find a numerical value that
	 * corresponds to {@code sBuffer}. If it returns {@link Double#NaN} then it
	 * did not find a numerical value. Otherwise the value and ObsType are
	 * passed back to XmlCollector to be stored and later printed.
	 *
	 * @param sBuffer The StringBuilder containing the mapped label
	 */
	@Override
	public void characters(StringBuilder sBuffer)
	{
		if (ignore(sBuffer))
			return;

		// attempt to convert the assumed text label to a numeric value
		m_dValue = m_oMappedValues.
			getValue(m_oXmlCollector.m_nId, m_nObsType, sBuffer);

		if (!Double.isNaN(m_dValue))
			m_oXmlCollector.addValueHolder(m_nObsType, m_dValue);
	}
}
