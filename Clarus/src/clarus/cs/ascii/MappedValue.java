// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file MappedValue.java
 */
package clarus.cs.ascii;

import clarus.cs.MappedValues;

/**
 * Provides the ability to retrieve a numerical data value from a mapped string,
 * using the {@link MappedValue#readData(java.lang.StringBuilder)} method.
 *
 * <p>
 * Extension of {@link DataValue}. 
 * </p>
 */
class MappedValue extends DataValue
{
    /**
     * The global instance of {@code MappedValues}
     */
	private MappedValues m_oMappedValues = MappedValues.getInstance();
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code MappedValue}
	 * </p>
	 */
	MappedValue()
	{
	}
	
    /**
     * Retrieves the numerical value from the member csv collector id, and
     * observation type that the string buffer was mapped to. This numerical
     * value is then assigned to {@code m_dValue}. {@code m_bNull} is then
     * updated to true if the retrieved value is
     * {@link Double#NEGATIVE_INFINITY}, false otherwise.
     *
     * @param sBuffer The string buffer containing the mapped label.
     */
	@Override
	public void readData(StringBuilder sBuffer)
	{
		if (sBuffer.length() == 0 || ignore(sBuffer))
			return;

		// attempt to convert the assumed text label to a numeric value
		m_dValue = m_oMappedValues.
			getValue(m_oCsvCollector.m_nId, m_nObsType, sBuffer);
	}
}
