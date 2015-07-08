// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Sensor.java
 */
package clarus.emc;

/**
 * Provides storage and retrieval of data about observation sensors.
 *
 * <p>
 * Implements {@code ISensor} to provide a standard interface for data access.
 * </p>
 */
public class Sensor implements ISensor
{
    /**
     * Sensor identifier.
     */
	int m_nId;
    /**
     * Observation type identifier.
     */
	int m_nObsType;
    /**
     * Sensor type identifier.
     */
	int m_nSensorType;
    /**
     * Index of {@code Sensor}. Distinguishes sensors at the same
     * {@code Station}.
     */
	int m_nSensorIndex;
    /**
     * {@code Sensor} distribution group.
     */
	int m_nDistGroup;
    /**
     * Identifier of the {@code Station} where the {@code Sensor} is held.
     */
	int m_nStationId;
    /**
     * Minimum scalar value the {@code Sensor} should be able to report.
     */
	double m_dMinRange;
    /**
     * Maximum scalar value the {@code Sensor} should be able to report.
     */
	double m_dMaxRange;
    /**
     * Maximum positive rate of change the {@code Sensor} should be able to
     * report.
     */
	double m_dRatePos;
    /**
     * Maximum negative rate of change the {@code Sensor} should be able to
     * report.
     */
	double m_dRateNeg;
    /**
     * Length of time used for qchs persistence test.
     */
	double m_dPersistInterval;
    /**
     * Minimum change between values that must occur over
     * {@code m_dPersisInterval} for the observation to pass qchs persistence
     * test.
     */
	double m_dPersistThreshold;
    /**
     * Maximum variance between this {@code Sensor} and like sensors
     * observations to pass qchs "Like Instrument Test."
     */
	double m_dLikeThreshold;
    /**
     * Begin timestamp for maintenance.
     */
	long m_lMaintBegin;
    /**
     * End timestamp for maintenance.
     */
	long m_lMaintEnd;

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Sensor}
	 * </p>
	 */
	Sensor()
	{
	}
	
	/**
     * <b> Copy Constructor </b>
     * <p>
     * Copies the supplied {@code Sensor} to a new instance of {@code Station}.
     * </p>
     * @param oSensor The {@code Sensor} to copy to the new instance.
     */
	Sensor(Sensor oSensor)
	{
		m_nId = oSensor.m_nId;
		m_nObsType = oSensor.m_nObsType;
		m_nSensorType = oSensor.m_nSensorType;
		m_nSensorIndex = oSensor.m_nSensorIndex;
		m_nDistGroup = oSensor.m_nDistGroup;
		m_nStationId = oSensor.m_nStationId;
		m_dMinRange = oSensor.m_dMinRange;
		m_dMaxRange = oSensor.m_dMaxRange;
		m_dRatePos = oSensor.m_dRatePos;
		m_dRateNeg = oSensor.m_dRateNeg;
		m_dPersistInterval = oSensor.m_dPersistInterval;
		m_dPersistThreshold = oSensor.m_dPersistThreshold;
		m_dLikeThreshold = oSensor.m_dLikeThreshold;
		m_lMaintBegin = oSensor.m_lMaintBegin;
		m_lMaintEnd = oSensor.m_lMaintEnd;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return sensor identifier.
     */
	public int getSensorId()
	{
		return m_nId;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return identifier of the {@code Station} that the {@code Sensor} is
     *      contained.
     */
	public int getStationId()
	{
		return m_nStationId;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return {@code Sensor} type identifier.
     */
	public int getSensorType()
	{
		return m_nSensorType;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation type identifier.
     */
	public int getObsTypeId()
	{
		return m_nObsType;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Index of Sensor. Distinguishes sensors at the same Station.
     */
	public int getSensorIndex()
	{
		return m_nSensorIndex;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return sensor distribution group.
     */
	public int getDistGroup()
	{
		return m_nDistGroup;
	}


    /**
     * Determines whether the supplied timestamp occurs in a time of
     * maintenance.
     * @param lTimestamp time that is being check against maintenance time
     *      range.
     * @return true if the timestamp occurs in a time of maintenance.
     */
	public boolean underMaintenance(long lTimestamp)
	{
		// this function must account for the possibility that 
		// either the begin or end time might be null
		if (m_lMaintBegin == 0)
		{
			if (m_lMaintEnd == 0)
				return false;

			if (lTimestamp <= m_lMaintEnd)
				return true;
			}
			else
			{
				if (m_lMaintEnd == 0)
					return (lTimestamp >= m_lMaintBegin);
		}

		return (lTimestamp >= m_lMaintBegin && lTimestamp <= m_lMaintEnd);
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Minimum scalar value the Sensor should be able to report.
     */
	public double getRangeMin()
	{
		return m_dMinRange;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Maximum scalar value the Sensor should be able to report.
     */
	public double getRangeMax()
	{
		return m_dMaxRange;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Maximum positive rate of change the Sensor should be able to
     *      report.
     */
	public double getRatePos()
	{
		return m_dRatePos;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Maximum negative rate of change the Sensor should be able to
     *      report.
     */
	public double getRateNeg()
	{
		return m_dRateNeg;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Length of time used for qchs persistence test.
     */
	public double getPersistInterval()
	{
		return m_dPersistInterval;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Minimum change between values that must occur over
     *      m_dPersisInterval for the observation to pass qchs persistence test.
     */
	public double getPersistThreshold()
	{
		return m_dPersistThreshold;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Maximum variance between this Sensor and like sensors
     *      observations to pass qchs "Like Instrument Test."
     */
	public double getLikeThreshold()
	{
		return m_dLikeThreshold;
	}
}
