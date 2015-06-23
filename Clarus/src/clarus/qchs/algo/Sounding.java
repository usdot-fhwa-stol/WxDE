// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Sounding.java
 */
package clarus.qchs.algo;

import clarus.emc.ISensor;
import clarus.qchs.ModObsSet;
import clarus.qchs.RAWINSONDE;
import clarus.qedc.IObs;
import util.threads.ILockFactory;
import util.threads.StripeLock;

/**
 * Performs spatial quality checking via {@code Barnes}. The difference being,
 * this quality check calculates the weighted mean of NWS-balloon adjusted
 * observations, and compares the provided observation to this value. If the
 * value being tested differs from the weighted mean by more than the configured
 * standard deviation the check fails
 * <p>
 * Extends {@code Barnes} to perform the quality check spatially, exept that
 * this algorithm checks observations against NWS-balloon observations.
 * </p>
 */
public class Sounding extends Barnes
{
	/**
	 * Lock container on HeightTemp observation sets.
	 */
	private StripeLock<HeightTemp> m_oHeightTempLock =
		new StripeLock<HeightTemp>(new HeightTempFactory(), DEFAULT_LOCKS);
	/**
	 * Pointer to the RAWINSONDE singleton instance.
	 */
	private RAWINSONDE m_oRawinsonde = RAWINSONDE.getInstance();


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * New instances of {@code Sounding} are created with the default-inherited
	 * lock pointing to null.
	 * </p>
	 */
	public Sounding()
	{		
		// free the default locks
		m_oLock = null;
		// ignore IQR test
		m_bIgnoreIQR = true;
	}


	/**
	 * Obtains a {@code HeightTemp} lock for use with the calling thread.
	 * @return the protected modified observation set.
	 */
	@Override
	protected ModObsSet readLock()
	{
		return m_oHeightTempLock.readLock();
	}


	/**
	 * Releases the {@code HeightTemp} lock assigned to the current thread.
	 */
	@Override
	protected void readUnlock()
	{
		m_oHeightTempLock.readUnlock();
	}


	/**
	 * If there's weather balloon data, the observation is checked against it
	 * using the {@code Barnes} spatial quality check.
	 *
	 * @param nObsTypeId type of observation in question.
	 * @param iSensor recording sensor.
	 * @param iObs observation being tested.
	 * @param oResult results of the quality check.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		// the lock is used directly to get the correct object type
		HeightTemp oHeightTemp = m_oHeightTempLock.readLock();

		if (m_oRawinsonde.getHeightTemp(oHeightTemp, iObs))
			super.check(nObsTypeId, iSensor, iObs, oResult);

		m_oHeightTempLock.readUnlock();
	}


	/**
	 * Allows {@code HeightTemp} objects to be added to a stripe lock.
	 * <p>
	 * Implements {@code ILockFactory<HeightTemp>} interface to allow
	 * {@code HeightTemp} objects to be modified in a mutually exclusive
	 * fashion through the use of {@link StripeLock} containers.
	 * </p>
	 */
	private class HeightTempFactory implements ILockFactory<HeightTemp>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code HeightTempFactory}
		 * </p>
         */
		public HeightTempFactory()
		{
		}


		/**
		 * Required for the implementation of the interface class
		 * {@code ILockFactory}.
		 * <p>
		 * This is used to add a container of lockable {@link HeightTemp}
		 * objects to the {@link StripeLock} Mutex.
		 * </p>
		 *
		 * @return A new instance of {@link HeightTemp}
		 *
		 * @see ILockFactory
		 * @see StripeLock
		 */
		public HeightTemp getLock()
		{
			return new HeightTemp();
		}
	}
}
