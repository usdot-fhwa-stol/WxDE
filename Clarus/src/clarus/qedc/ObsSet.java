// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ObsSet.java
 */
package clarus.qedc;

import clarus.ClarusMgr;
import java.util.ArrayList;

/**
 * List of observations that can be submitted to the {@code ClarusMgr} instance 
 * for processing.
 * <p>
 * Extends {@code ArrayList} to provide a list of {@code Obs} objects, which
 * can then be added to the {@link util.threads.AsyncQ} contained in
 * {@link ClarusMgr}
 * </p>
 * <p>
 * Implements interface {@code IObsSet} to provide a standard way of interacting
 * with the container.
 * </p>
 */
public class ObsSet extends ArrayList<Obs> implements IObsSet
{
    /**
     * Flag to keep {@code ObsSet} from being modified.
     */
	boolean m_bReadOnly;
    /**
     * Observation type contained in the {@code ObsSet}.
     */
	int m_nObsType;
    /**
     * Observation set serial identifier.
     */
	int m_nSerial;
    /**
     * Determines which {@code Obs} processor in the {@link ClarusMgr} queue
     * this observation set will be added to. This state in incremented each
     * time this observation set is enqueued.
     */
	int m_nState;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code ObsSet}
	 * </p>
	 */
	private ObsSet()
	{
	}
	

    /**
     * Initializes the corresponding attributes to the supplied values, when
     * a new instance of {@code ObsSet} is created with these parameters.
     * @param nObsType Observation type to be contained in this set.
     * @param nSerial observation set serial identifier.
     */
	ObsSet(int nObsType, int nSerial)
	{
		m_nObsType = nObsType;
		m_nSerial = nSerial;
	}
	

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation type contained in the {@code ObsSet}.
     */
	public int getObsType()
	{
		return m_nObsType;
	}


    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return queue state attribute.
     */
	public int getState()
	{
		return m_nState;
	}


    /**
     * <b> Mutator </b>
     * <p>
     * Sets the queue state attribute.
     * </p>
     * <p>
     * Interface method implementation.
     * </p>
     * @param nState queue state
     */
	public void setState(int nState)
	{
		m_nState = nState;
	}


    /**
     * If the observation set isn't in a read only state, a new {@link Obs}
     * instance with the supplied properties, and default values is created,
     * then added to the list.
     * <p>
     * Interface method implementation.
     * </p>
     * @param nSensorId id of the observation sensor.
     * @param lTimestamp timestamp corresponding to the time of observation.
     * @param nLat latitude of the observation sensor.
     * @param nLon longitude of the observation sensor.
     * @param dValue observation value.
     */
	public void addObs(int nSensorId, long lTimestamp, int nLat, int nLon, 
		double dValue)
	{
		addObs(nSensorId, lTimestamp, nLat, nLon, Short.MIN_VALUE, dValue);
	}


    /**
     * If the observation set isn't in a read only state, a new {@link Obs}
     * instance with the supplied properties, and default values is created,
     * then added to the list.
     * <p>
     * Interface method implementation.
     * </p>
     * @param nSensorId id of the observation sensor.
     * @param lTimestamp timestamp corresponding to the time of observation.
     * @param nLat latitude of the observation sensor.
     * @param nLon longitude of the observation sensor.
     * @param tElev elevation of the observing sensor.
     * @param dValue observation value.
     */
	public void addObs(int nSensorId, long lTimestamp, int nLat, int nLon, 
		short tElev, double dValue)
	{
		addObs(nSensorId, lTimestamp, nLat, nLon, tElev, dValue, 0, 0, 0.0F);
	}
	

    /**
     * If the observation set isn't in a read only state, a new {@link Obs}
     * instance with the supplied properties is created, and added to the list.
     * <p>
     * Interface method implementation.
     * </p>
     * @param nSensorId id of the observation sensor.
     * @param lTimestamp timestamp corresponding to the time of observation.
     * @param nLat latitude of the observation sensor.
     * @param nLon longitude of the observation sensor.
     * @param tElev elevation of the observing sensor.
     * @param dValue observation value.
     * @param nRun bit-field showing which quality checking algorithms to run
     * on this observation.
     * @param nFlags bit-field showing whether the corresponding quality check
     * algorithm passed or failed.
     * @param fConfidence quality confidence level.
     */
	public void addObs(int nSensorId, long lTimestamp, int nLat, int nLon, 
		short tElev, double dValue, int nRun, int nFlags, float fConfidence)
	{
		// no more obs can be added to a read only set
		if (m_bReadOnly)
			return;
		
		try
		{
			Obs oObs = new Obs(m_nObsType, nSensorId, lTimestamp,
				nLat, nLon, tElev, dValue, nRun, nFlags, fConfidence);
			add(oObs);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Observation set serial identifier.
     */
	public int serial()
	{
		return m_nSerial;
	}
}
