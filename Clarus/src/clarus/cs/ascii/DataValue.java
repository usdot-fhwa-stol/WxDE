// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file DataValue.java
 */
package clarus.cs.ascii;

import clarus.UnitConv;
import clarus.Units;
import clarus.cs.CsMgr;
import clarus.emc.ObsTypes;
import clarus.emc.IObsType;
import util.Text;

/**
 * Provides a method to gather, convert, and compare observation data values.
 *
 * <p>
 * Implements {@code Comparable<DataValue>} interface to impose an ordering upon
 * {@code DataValue} object lists.
 * </p>
 */
public class DataValue implements Comparable<DataValue>
{
	/**
     * Column identifier.
     */
    protected int m_nColumnId;
    /**
     * Observation type identifier.
     */
	protected int m_nObsType;
    /**
     * Sensor index.
     */
	protected int m_nSensorIndex;
    /**
     * Column width.
     */
	protected int m_nColWidth;
    /**
     * Observation value. NaN implies there are no pending data values.
     */
	protected double m_dValue = Double.NaN;
    /**
     * Conversion Multiplier.
     */
	protected double m_dMultiplier = 1.0;
    /**
     * Values to ignore.
     */
	protected String[] m_sIgnoreValues;
    /**
     * Collector Service Manager Instance.
     */
	protected CsMgr m_oCsMgr;
    /**
     * CSV collector.
     */
	protected CsvCollector m_oCsvCollector;
    /**
     * Unit Converter.
     */
	protected UnitConv m_oUnitConv;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code DataValue}
	 * </p>
	 */
	DataValue()
	{
	}
	
	/**
     * Initializes attribute values. Gets conversion data based off the given
     * unit identifier string, and the observation type id.
     *
     * <p>
     * {@code init} should be called before {@code DataValue} objects are used.
     * </p>
     *
     * @param nColumnId column identifier.
     * @param nObsTypeId observation type identifier.
     * @param nSensorIndex sensor index.
     * @param nColWidth column width.
     * @param dMultiplier conversion multiplier.
     * @param sUnit unit type.
     * @param sIgnoreValues values to ignore.
     * @param oCsMgr collector service manager.
     * @param oCsvColl csv collector.
     */
	void init(int nColumnId, int nObsTypeId, int nSensorIndex, int nColWidth, 
		double dMultiplier, String sUnit, String sIgnoreValues, 
		CsMgr oCsMgr, CsvCollector oCsvColl)
	{
		m_oCsvCollector = oCsvColl;
		m_oCsMgr = oCsMgr;
		
		m_nColumnId = nColumnId;
		m_nObsType = nObsTypeId;
		m_nSensorIndex = nSensorIndex;
		m_nColWidth = nColWidth;
		
		if (dMultiplier != 0.0)
			m_dMultiplier = dMultiplier;
		
		// get the destination unit for the obs type
		IObsType iObsType = ObsTypes.getInstance().getObsType(nObsTypeId);
		// get a unit conversion based on the source and destination units
		if (iObsType != null)
			m_oUnitConv = Units.getInstance().
				getConversion(sUnit, iObsType.getUnit());
		
		if (sIgnoreValues != null && sIgnoreValues.length() > 0)
		{
			m_sIgnoreValues = sIgnoreValues.split(";");
			// trim whitespace
			int nIndex = m_sIgnoreValues.length;
			while (nIndex-- > 0)
				m_sIgnoreValues[nIndex] = m_sIgnoreValues[nIndex].trim();
		}
	}
	
	/**
     * Accessor for the column width attribute {@code m_nColWidth}.
     *
     * @return Column width member.
     */
	public int getWidth()
	{
		return m_nColWidth;
	}


	/**
     * Determines whether {@code sValue} is an ignore value (contained in the
     * ignore value list {@code m_sIgnoreValues}} or not.
     *
     * @param sValue string to check against ignore list.
     *
     * @return true if {@code sValue} is contained in the ignore value list.
     * <br /> false otherwise.
     */
	protected boolean ignore(CharSequence iCharSeq)
	{
		boolean bIgnore = false;

		if (m_sIgnoreValues != null)
		{
			int nIndex = m_sIgnoreValues.length;
			while (nIndex-- > 0 && !bIgnore)
				bIgnore = (Text.compare(m_sIgnoreValues[nIndex], iCharSeq) == 0);
		}

		return bIgnore;
	}
	
	/**
     * Calls {@link DataValue#reset() reset()} if sBuffer contains no data,
     * otherwise if the value is not one to be ignored, it applies the
     * configured multiplier, and conversion to the parsed value from the
     * given buffer, and assigns the converted value to {@code m_dValue}.
     * Finally, {@code m_bNull} is assigned a value of false to signify there
     * is a pending value.
     *
     * @param sBuffer The string buffer containing the value.
     */
	public void readData(StringBuilder sBuffer)
	{
		int nLength = sBuffer.length();
		if (nLength == 0 || ignore(sBuffer))
		{
			m_dValue = Double.NaN;
			return;
		}

		try
		{
			// remove potential units specifiers from right side
			while (nLength > 0 && !Character.isDigit(sBuffer.charAt(--nLength)));
			sBuffer.setLength(++nLength);

			// apply the configured multiplier and convert the units
			m_dValue = Double.parseDouble(sBuffer.toString()) * m_dMultiplier;
			m_dValue = m_oUnitConv.convert(m_dValue);
		}
		catch (Exception oException)
		{
		}
	}
	
	/**
     * If there's data pending, an observation is created and saved to the
     * list of observations contained in the {@code CsvCollector} object through
     * the {@link CsvCollector#createObs(int, int, double) createObs} method.
     * {@link DataValue#reset() reset} is then invoked to reset pending data
     * status, and clear the data value.
     */
	public void writeData()
	{
		if (Double.isNaN(m_dValue))
			return;
		
		m_oCsvCollector.createObs(m_nObsType, m_nSensorIndex, m_dValue);
		
		// reset the information for subsequent operations
		m_dValue = Double.NaN;
	}
	
	/**
     * Compares the column identifiers of <i> this </i> and
     * {@code oDataHandler}.
     *
     * <p>
     * Required for the implementation of the Comparable<DataValue> interface.
     * </p>
     *
     * @param oDataHandler the {@code DataValue} to compare to <i> this </i>.
     *
     * @return 0 if they're equivalent.
     */
	public int compareTo(DataValue oDataHandler)
	{
		return (m_nColumnId - oDataHandler.m_nColumnId);
	}
}
