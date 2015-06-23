// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Pressure.java
 */
package clarus.qchs.algo;

import clarus.qchs.ModObsSet;
import util.threads.ILockFactory;
import util.threads.StripeLock;

/**
 * Performs spatial quality checking via {@code Barnes}. The difference being,
 * this quality check calculates the weighted mean sea-level pressure based off
 * surrounding sensor observations, and elevation. If the value being tested
 * differs from the weighted mean by more than the configured standard deviation
 * the check fails.
 * <p>
 * Extends {@code Barnes} to perform the quality check spatially, exept that
 * this quality check analyzes sea level pressure derived from pressure
 * observations and elevation.
 * </p>
 */
public class Pressure extends Barnes
{
	/**
	 * Lock container on Pressure observation sets.
	 */
	protected StripeLock<PressureObsSet> m_oPressureLock =
		new StripeLock<PressureObsSet>(new PressureObsSets(), DEFAULT_LOCKS);


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * New instances of {@code Pressure} are created with the default-inherited
	 * lock pointing to null.
	 * </p>
	 */
	public Pressure()
	{
		// free the default locks
		m_oLock = null;
	}


	/**
	 * Obtains a {@code PressureObsSet} lock for use with the calling thread.
	 * @return the protected modified observation set.
	 */
	@Override
	protected ModObsSet readLock()
	{
		return m_oPressureLock.readLock();
	}


	/**
	 * Releases the {@code PressureObsSet} lock assigned to the current thread.
	 */
	@Override
	protected void readUnlock()
	{
		m_oPressureLock.readUnlock();
	}


	/**
	 * Allows {@code PressureObsSet} objects to be added to a stripe lock.
	 * <p>
	 * Implements {@code ILockFactory<PressureObsSet>} interface to allow
	 * {@code PressureObsSet} objects to be modified in a mutually exclusive
	 * fashion through the use of {@link StripeLock} containers.
	 * </p>
	 */
	private class PressureObsSets implements ILockFactory<PressureObsSet>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code PressureObsSets}
		 * </p>
         */
		public PressureObsSets()
		{
		}


		/**
		 * Required for the implementation of the interface class
		 * {@code ILockFactory}.
		 * <p>
		 * This is used to add a container of lockable {@link PressureObsSet}
		 * objects to the {@link StripeLock} Mutex.
		 * </p>
		 *
		 * @return A new instance of {@link PressureObsSet}
		 *
		 * @see ILockFactory
		 * @see StripeLock
		 */
		public PressureObsSet getLock()
		{
			return new PressureObsSet();
		}
	}
}
