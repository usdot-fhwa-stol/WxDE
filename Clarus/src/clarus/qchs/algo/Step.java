// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Step.java
 */
package clarus.qchs.algo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import clarus.emc.ISensor;
import clarus.qchs.QCh;
import clarus.qedc.IObs;

/**
 * Tests observations to determine whether or not the recorded value falls
 * within the observing stations maximum rate of change over a configured
 * period of time.
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class Step extends QCh
{
	/**
	 * Quality check configuration query format string.
	 */
	private static String QCHCONFIG_QUERY = "SELECT timerangeMin, " + 
		"timerangeMax, geoRadiusMin, geoRadiusMax, sdMin, sdMax, " + 
		"obsCountMin, testName FROM qchconfig WHERE id = ?";

	/**
	 * Timerange min within which to gather the set of observations, in ms.
	 */
	protected long m_lTimerangeMin;
	/**
	 * Timerange max within which to gather the set of observations, in ms.
	 */
	protected long m_lTimerangeMax;
	/**
	 * Name of the test to perform.
	 */
	protected String m_sName;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instance of {@code Step}.
	 * </p>
	 */
	public Step()
	{
	}


	/**
	 * Initializes <i> this </i> instance with the provided values. Queries
	 * the database for qch configuration data, with the supplied id and
	 * connection. Calls {@link Step#init(java.sql.ResultSet) } on the result
	 * of this query, to set the timerange.
	 *
	 * <p>
	 * Overrides base class {@code init} method.
	 * </p>
	 *
     * @param nSeq sequence id.
     * @param nBitPosition bit position for the qch algorithm.
     * @param nRunAlways a value of two or three will set the run-always flag
	 * to signal a stop on a failed test.
     * @param dWeight quality check influence.
     * @param nConfigId qch configuration id.
     * @param iConnection connected to the datasource before call to this
	 * method.
	 */
	@Override
	public void init(int nSeq, int nBitPosition, int nRunAlways,
		double dWeight, int nConfigId, Connection iConnection)
	{
		super.init(nSeq, nBitPosition, nRunAlways,
			dWeight, nConfigId, iConnection);
		
		try
		{
			PreparedStatement iStatement = 
				iConnection.prepareStatement(QCHCONFIG_QUERY);
			
			iStatement.setInt(1, nConfigId);			
			ResultSet iResultSet = iStatement.executeQuery();
			if (iResultSet.next())
				init(iResultSet);

			iResultSet.close();
			iStatement.close();			
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	

	/**
	 * Sets the configured timerange min and max from the provided result set.
	 * @param iResultSet results table containing the timerange data.
	 * @throws java.lang.Exception
	 */
	protected void init(ResultSet iResultSet) throws Exception
	{
		// convert the seconds in the configuration to milliseconds
		m_lTimerangeMin = iResultSet.getLong(1) * 1000;
		m_lTimerangeMax = iResultSet.getLong(2) * 1000;
		
		// the time range minimum must always be negative
		if (m_lTimerangeMin > 0)
			m_lTimerangeMin = -m_lTimerangeMin;
		
		m_sName = iResultSet.getString(8);
	}


	/**
     * Tests the provided observation by comparing its value to similar values 
	 * over the configured time range, to ensure that it does not fall outside
	 * of the sensors maximum rate of change in either direction (positive or 
	 * negative), and then passes the results to the provided result object.
	 *
	 * @param nObsTypeId observation type
	 * @param iSensor recording sensor.
	 * @param iObs observation to be tested.
	 * @param oResult contains the results of the test after returning.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		long lTimestamp = iObs.getTimestamp();
		ArrayList<IObs> oObsSet = new ArrayList<IObs>();
		m_oObsMgr.getSensors(nObsTypeId, iObs.getSensorId(),
			lTimestamp + m_lTimerangeMin,
			lTimestamp + m_lTimerangeMax, oObsSet);
		
		// at least two values are required for the comparison 
		// one of the values is the current obs being quality checked
		if (oObsSet.size() < 2)
			return;
		
		boolean bPassed = false;
		int nIndex = 0;
		while (nIndex < oObsSet.size() && !bPassed)
		{
			IObs iOtherObs = oObsSet.get(nIndex++);
			long lTimeDiff = lTimestamp - iOtherObs.getTimestamp();
			// ignore the value when the timestamp is the same 
			// that means it is the obs being evaluated
			if (lTimeDiff > 0)
			{
				// convert from milliseconds to seconds
				double dRate = (iObs.getValue() - iOtherObs.getValue()) * 
					1000.0 / lTimeDiff;

				bPassed = dRate >= iSensor.getRateNeg() && 
					dRate <= iSensor.getRatePos();
			}
		}

		// indicate the test was run
		oResult.setPass(bPassed);
		oResult.setRun();

		if (bPassed)
			oResult.setConfidence(1.0);
	}
}
