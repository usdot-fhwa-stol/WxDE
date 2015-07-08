// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Obs.java
 */
package clarus.qedc;

/**
 * Provides a means of accessing, modifying, and quality checking observation
 * data.
 * <p>
 * Implements {@code IObs} to provide an interface for accessing and modifying
 * {@code Obs} attributes.
 * </p>
 */
public class Obs implements IObs
{
    /**
     * Observation type identifier.
     */
    int m_nTypeId;
    /**
     * Identifier of the sensor making the observation.
     */
	int m_nSensorId;
    /**
     * Timestamp indicating when the observation is valid.
     */
	long m_lTimestamp;
    /**
     * Observation latitude, based on the sensor location.
     */
	int m_nLat;
    /**
     * Observation longitude, based on the sensor location.
     */
	int m_nLon;
    /**
     * Observation elevation based on the sensor location.
     */
	short m_tElev;
    /**
     * Hash value.
     */
    short m_tHash;
    /**
     * Quality confidence level.
     */
	float m_fConfidence;
    /**
     * Observation scalar value.
     */
	double m_dValue;
    /**
     * Bit-field showing which quality checking algorithms were ran on
     * this observation.
     */
	int m_nQChRun;
    /**
     * Bit-field showing whether the corresponding quality check algorithm
     * passed or failed.
     */
	int m_nQChFlags;
    /**
     * Timestamp indicating when the observation was most recently updated.
     */
	long m_lUpdated;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Obs}
	 * </p>
	 */
	protected Obs()
	{
	}
	
	/**
     * Initializes attributes of new instance of {@code Obs} to the
     * corresponding supplied values.
     * @param nTypeId observation type id.
     * @param nSensorId sensor id.
     * @param lTimestamp timestamp indicating when the observation was made.
     * @param nLat latitude of the observing sensor.
     * @param nLon longitude of the observing sensor.
     * @param tElev elevation of the observing sensor.
     *      made.
     * @param dValue observation scalar value.
     */
	Obs(int nTypeId, int nSensorId, long lTimestamp, int nLat, int nLon,
		short tElev, double dValue)
	{
        m_nTypeId = nTypeId;
		m_nSensorId = nSensorId;
		m_lTimestamp = lTimestamp;
		m_nLat = nLat;
		m_nLon = nLon;
		m_tElev = tElev;
		m_dValue = dValue;
	}
	
    /**
     * Initializes attributes of new instance of {@code Obs} to the
     * corresponding supplied values.
     * @param nTypeId observation type id.
     * @param nSensorId sensor id.
     * @param lTimestamp timestamp indicating when the observation was made.
     * @param nLat latitude of the observing sensor.
     * @param nLon longitude of the observing sensor.
     * @param tElev elevation of the observing sensor.
     *      made.
     * @param dValue observation scalar value.
     * @param nRun bit-field indicating which quality checking algorithm to run.
     * @param nFlags bit-field indicating results of the corresponding
     *      algorithm.
     * @param fConfidence quality confidence level .
     */
	Obs(int nTypeId, int nSensorId, long lTimestamp, int nLat, int nLon,
		short tElev, double dValue, int nRun, int nFlags, float fConfidence)
	{
		this(nTypeId, nSensorId, lTimestamp, nLat, nLon, tElev, dValue);
		
		m_nQChRun = nRun;
		m_nQChFlags = nFlags;
		m_fConfidence = fConfidence;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation type identifier.
     */
    public int getTypeId()
    {
        return m_nTypeId;
    }


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Identifier of the sensor making the observation.
     */
	public int getSensorId()
	{
		return m_nSensorId;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Timestamp indicating when the observation was made.
     */
	public long getTimestamp()
	{
		return m_lTimestamp;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation latitude, based on the sensor location.
     */
	public int getLat()
	{
		return m_nLat;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation longitude, based on the sensor location.
     */
	public int getLon()
	{
		return m_nLon;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation elevation based on the sensor location.
     */
	public short getElev()
	{
		return m_tElev;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation scalar value.
     */
	public double getValue()
	{
		return m_dValue;
	}
	

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Bit-field showing which quality checking algorithms were ran on
     *      this observation.
     */
	public int getRun()
	{
		return m_nQChRun;
	}
	

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Bit-field showing whether the corresponding quality check
     *      algorithm passed or failed.
     */
	public int getFlags()
	{
		return m_nQChFlags;
	}
	

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Quality confidence level.
     */
	public float getConfidence()
	{
		return m_fConfidence;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return the timestamp when the obs wast most recently updated.
     */
	public long getUpdate()
	{
		return m_lUpdated;
	}


	/**
     * <b> Mutator </b>
     * <p>
     * Sets the run, pass, and confidence for the quality checking algorithm
     * used.
     * </p>
     * <p>
     * Interface method implementation.
     * </p>
     * @param nRun the bit field id of the quality checking algorithm used.
     * @param nFlags corresponds to the run-bit-field, but indicates whether the
     *  the quality checking algorithm passed.
     * @param fConfidence quality confidence level.
     */
	public void setFlags(int nRun, int nFlags, float fConfidence)
	{
		m_nQChRun = nRun;
		m_nQChFlags = nFlags;
		m_fConfidence = fConfidence;
	}
}
