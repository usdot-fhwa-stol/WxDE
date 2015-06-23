// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ClimateRange.java
 */
package clarus.qchs.algo;

import java.util.Calendar;
import java.util.GregorianCalendar;
import clarus.emc.ClimateRecords;
import clarus.emc.Stations;
import clarus.emc.IClimateRecord;
import clarus.emc.ISensor;
import clarus.emc.IStation;
import clarus.qchs.QCh;
import clarus.qedc.IObs;

/**
 * Determines whether or not the observation being tested falls within the
 * observing station's specific climatological ranges, that vary by date to
 * account for seasonal changes.
 *
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class ClimateRange extends QCh
{
	/**
	 * Observation timestamp.
	 */
	private final GregorianCalendar m_oCalendar = new GregorianCalendar();

	/**
	 * Pointer to the {@code ClimateRecords} cache singleton instance.
	 */
	private ClimateRecords m_oClimateRecords = 
		ClimateRecords.getInstance();

	/**
	 * Pointer to the {@code Stations} cache singleton instance.
	 */
	private Stations m_oStations = Stations.getInstance();
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates instances of {@code ClimateRange}. Initialization done through
	 * base class {@code init} methods.
	 * </p>
	 */
	public ClimateRange()
	{
	}


	/**
	 * Determines whether or not the provided observation falls within the
	 * station specific climatological ranges, that vary by date to account for
	 * seasonal changes. Observation passes if the value is greater than the
	 * climatological min, and less than the max.
	 *
	 * @param nObsTypeId type of observation being tested.
	 * @param iSensor recording sensor, used to determine the station.
	 * @param iObs observation being tested.
	 * @param oResult contains the results of the test after returning from the
	 * method call.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		// find the station to which this sensor belongs
		IStation iStation = m_oStations.getStation(iSensor.getStationId());
		if (iStation != null)
		{
			// determine the current period
			// synchronized to reuse the single Gregorian calendar instance
			int nPeriod = 0;
			synchronized(m_oCalendar)
			{
				m_oCalendar.setTimeInMillis(iObs.getTimestamp());
				nPeriod = m_oCalendar.get(Calendar.MONTH);
			}
			
			// attempt to get the climate record for the current station
			IClimateRecord iClimateRecord = m_oClimateRecords.
				getClimateRecord(nObsTypeId, nPeriod, iObs.getLat(), iObs.getLon());

			// check for both min and max being set to zero, which is the
			// result when a climate record exists but the values are null
			if (iClimateRecord != null && 
				iClimateRecord.getMin() != iClimateRecord.getMax())
			{
				if (iObs.getValue() >= iClimateRecord.getMin() &&
					iObs.getValue() <= iClimateRecord.getMax())
				{
					oResult.setPass(true);
					oResult.setConfidence(1.0);
				}
				oResult.setRun();
			}
		}
	}
}
