// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QChSeqMgr.java
 */
package clarus.qchs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import clarus.emc.ISensor;
import clarus.emc.IStation;
import clarus.emc.Sensors;
import clarus.emc.Stations;
import clarus.qchs.algo.QChResult;
import clarus.qedc.IObs;
import clarus.qedc.IObsSet;
import util.threads.AsyncQ;
import util.threads.ILockFactory;
import util.threads.StripeLock;

/**
 * Recieves observation sets to quality check, and filters them into the correct
 * quality check sequence for its climate-region id through the
 * {@link QChSeqMgr#run(clarus.qedc.IObsSet)} method.
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of observation sets as
 * they are enqueued.
 * </p>
 * <p>
 * Implements {@code Comparable<QChSeqMgr>} to enforce an ordering on sequence
 * managers by observation type.
 * </p>
 * <p>
 * Implements {@code ILockFactory<QChSeq>} mutually exclusive access of quality
 * check sequences through the use of {@link StripeLock} containers.
 * </p>
 */
public class QChSeqMgr extends AsyncQ<IObsSet> 
	implements Comparable<QChSeqMgr>, ILockFactory<QChSeq>
{
	/**
	 * Qch Sequence Manager database query format string. 
	 * Quality checks are based on the climate region.
	 */
	private static String QCHSEQMGR_QUERY = "SELECT id, climateId " + 
		"FROM qchseqmgr WHERE obsTypeId = ? AND active = 1";

	/**
	 * Observation type being quality checked by this manager.
	 */
	private int m_nObsTypeId;
	/**
	 * List of quality check sequences ordered by climate id.
	 */
	private ArrayList<QChSeq> m_oSeq;
	/**
	 * Provides mutually exclusive access to quality checking sequences.
	 */
	private StripeLock<QChSeq> m_oSeqLock;
	/**
	 * Provides mutually exclusive access to qualtity check results.
	 */
	private StripeLock<QChResult> m_oResultLock;
	/**
	 * Pointer to the sensors cache singleton instance.
	 */
	private Sensors m_oSensors;
	/**
	 * Pointer to the stations cache singleton instance.
	 */
	private Stations m_oStations;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code QChSeqMgr}
	 * </p>
	 */
	QChSeqMgr()
	{
	}
	

	/**
	 * Initializes new instances of {@code QChSeqMgr} with the provided values.
	 * Queries the database to populate the sorted list of quality check
	 * sequences, setting their climate-region id..
	 * @param nObsTypeId observation type id for this manager, used for the
	 * database query.
	 * @param nMaxThreads max threads to allocate to processing the quality
	 * check sequence and result {@link StripeLock} containers.
	 * @param iConnection connection to the datasource, ready for queries prior
	 * to this method call.
	 */
	QChSeqMgr(int nObsTypeId, int nMaxThreads, Connection iConnection)
	{
		setObsTypeId(nObsTypeId);
		setMaxThreads(nMaxThreads);
		
		m_oSeq = new ArrayList<QChSeq>();
		m_oSeqLock = new StripeLock<QChSeq>(this, nMaxThreads);
		m_oResultLock = 
			new StripeLock<QChResult>(new ResultFactory(), nMaxThreads);
		m_oSensors = Sensors.getInstance();
		m_oStations = Stations.getInstance();

		try
		{
			PreparedStatement iPreparedStatement = 
				iConnection.prepareStatement(QCHSEQMGR_QUERY);
			iPreparedStatement.setInt(1, nObsTypeId);
			
			ResultSet iResultSet = iPreparedStatement.executeQuery();
			while (iResultSet.next())
				m_oSeq.add(new QChSeq(iResultSet.getInt(1), 
					iResultSet.getInt(2), iConnection));

			iPreparedStatement.close();
			Collections.sort(m_oSeq);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	

	/**
	 * <b> Mutator </b>
	 * @param nObsTypeId observation type id value for this sequence manager.
	 */
	void setObsTypeId(int nObsTypeId)
	{
		m_nObsTypeId = nObsTypeId;
	}
	

	/**
	 * Quality checks the observations in the set that haven't been checked, and
	 * that have sensors with distribution group 2.
	 * <p>
	 * Uses the default-region quality check sequence if the sequence isn't
	 * available for the given observations climate-region.
	 * </p>
	 * <p>
	 * Overrides {@code AsyncQ} default run method.
	 * </p>
	 * @param iObsSet set of observations to quality check.
	 */
	@Override
	public void run(IObsSet iObsSet)
	{
		// an appropriate sequence must be found for each obs in the set
		for (int nObsIndex = 0; nObsIndex < iObsSet.size(); nObsIndex++)
		{
			IObs iObs = iObsSet.get(nObsIndex);

			// only quality check obs without existing quality check flags
			if (iObs.getRun() != 0)
				continue;
			
			// only sensors with dist group 2 are checked
			ISensor iSensor = m_oSensors.getSensor(iObs.getSensorId());
			if (iSensor == null || iSensor.getDistGroup() < 2)
				continue;
			
			QChSeq oSeq = null;
			// always search for the default climate id sequence
			QChSeq oSearchSeq = m_oSeqLock.readLock();
			oSearchSeq.setClimateId(0);
			int nSeqIndex = Collections.binarySearch(m_oSeq, oSearchSeq);
			if (nSeqIndex >= 0)
				oSeq = m_oSeq.get(nSeqIndex);

			// search for a specific climate region sequence when no default
			// seq is found or when there is more than one seq available
			if (oSeq == null || m_oSeq.size() > 1)
			{
				// finding the climate id based the current obs is costly
				IStation iStation = m_oStations.
					getStation(iSensor.getStationId());

				if (iStation != null)
				{
					oSearchSeq.setClimateId(iStation.getClimateId());
					nSeqIndex =
						Collections.binarySearch(m_oSeq, oSearchSeq);
					// the default is used when a climate seq is not found
					if (nSeqIndex >= 0)
						oSeq = m_oSeq.get(nSeqIndex);
				}
			}
			m_oSeqLock.readUnlock();

			// quality check the obs with the selected sequence, if any
			if (oSeq != null)
			{
				QChResult oResult = m_oResultLock.readLock();
				oSeq.check(m_nObsTypeId, iSensor, iObs, oResult);
				m_oResultLock.readUnlock();
			}
		}
	}


	/**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link QChSeq} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link QChSeq}
     *
     * @see ILockFactory
     * @see StripeLock
     */
	@Override
	public QChSeq getLock()
	{
		return new QChSeq();
	}
	

	/**
	 * Compares <i> this </i> sequence manager with the provided sequence
	 * manager by observation type.
	 *
	 * <p>
	 * Required for the implementation of {@link Comparable}.
	 * </p>
	 * @param oQChSeqMgr manager to compare with <i> this </i>
	 * @return 0 if they match by observation type. &lt 0 if <i> this </i>
	 * is the less than the provided manager.
	 */
	public int compareTo(QChSeqMgr oQChSeqMgr)
	{
		return (m_nObsTypeId - oQChSeqMgr.m_nObsTypeId);
	}
	

	/**
	 * Provides mutually exclusive access of results.
	 *
	 * <p>
	 * Implements {@code ILockFactory<QChResult>} to provide a means of
	 * accessing results in a mutually exclusive manner, through the use
	 * of {@link StripeLock}.
	 * </p>
	 */
	private class ResultFactory implements ILockFactory<QChResult>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code ResultFactory}
		 * </p>
         */
		ResultFactory()
		{
		}


		/**
		 * Required for the implementation of the interface class
		 * {@code ILockFactory}.
		 * <p>
		 * This is used to add a container of lockable {@link QChResult} objects
		 * to the {@link StripeLock} Mutex.
		 * </p>
		 *
		 * @return A new instance of {@link QChResult}
		 *
		 * @see ILockFactory
		 * @see StripeLock
		 */
		@Override
		public QChResult getLock()
		{
			return new QChResult();
		}
	}
}
