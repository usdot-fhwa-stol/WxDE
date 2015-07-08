// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ClimateRecord.java
 */
package clarus.emc;

/**
 * Provides a means of copying {@code ClimateRecords}, and accessing
 * {@code ClimateRecord} attributes.
 *
 * <p>
 * A {@code ClimateRecord} represents a row from the climate record database.
 * Records are monthly min, max, and avg values for a particular type of
 * observation, and are arranged by month and a 2.5 degree "rectangular" grid.
 * </p>
 *
 * <p>
 * Implements {@code IClimateRecord} which forces an accessor interface on
 * implementations.
 * </p>
 */
public class ClimateRecord implements IClimateRecord
{
    /**
     * Observation type.
     */
	int m_nObsTypeId;
    /**
     * The month to which the record applies, from 1 to 12.
     */
	int m_nMonth;
    /**
     * The latitude of the grid cell to which this historical record applies.
     */
	int m_nLat;
    /**
     * The longitude of the grid cell to which this historical record applies.
     */
	int m_nLon;
    /**
     * Min scalar value of the observation type for the month.
     */
	double m_dMin;
    /**
     * Max scalar value of the observation type for the month.
     */
	double m_dMax;
    /**
     * Average scalar value of the observation type for the month.
     */
	double m_dAvg;

    
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code ClimateRecord}
	 * </p>
	 */
	ClimateRecord()
	{
	}
	

    /**
     * <b> Copy Constructor </b>
     * <p>
     * New instances of {@code ClimateRecord} have the same attribute values
     * as the supplied {@code oClimateRecord}.
     * </p>
     * @param oClimateRecord The {@code ClimateRecord} to copy to a new
     *      instance.
     */
	ClimateRecord(ClimateRecord oClimateRecord)
	{
		m_nObsTypeId = oClimateRecord.m_nObsTypeId;
		m_nMonth = oClimateRecord.m_nMonth;
		m_nLat = oClimateRecord.m_nLat;
		m_nLon = oClimateRecord.m_nLon;
		m_dMin = oClimateRecord.m_dMin;
		m_dMax = oClimateRecord.m_dMax;
		m_dAvg = oClimateRecord.m_dAvg;
	}
	

    /**
     * <b> Accessor </b>
     *
     * @return Min value attribute corresponding to the min scalar value of the
     *  observation type for the month.
     */
	public double getMin()
	{
		return m_dMin;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Implements interface method.
     * </p>
     * @return Max value attribute corresponding to the max scalar value of the
     *  observation type for the month
     */
	public double getMax()
	{
		return m_dMax;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Implements interface method.
     * </p>
     * @return Average value attribute corresponding to the normal scalar value
     * of the observation type for the month
     */
	public double getAvg()
	{
		return m_dAvg;
	}
}