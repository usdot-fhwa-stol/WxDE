// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SurfaceRange.java
 */
package clarus.qchs.algo;

import java.util.Calendar;
import java.util.GregorianCalendar;
import clarus.emc.ISensor;
import clarus.qchs.QCh;
import clarus.qchs.Surface;
import clarus.qchs.SurfaceRecord;
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
public class SurfaceRange extends QCh
{
	/**
	 * Calendar object used to determine month for climate range values.
	 */
	private final GregorianCalendar m_oCalendar = new GregorianCalendar();

	/**
	 * Pointer to the {@code Stations} cache singleton instance.
	 */
	private Surface m_oSurface = Surface.getInstance();
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates instances of {@code SurfaceRange}. Initialization done through
	 * base class {@code init} methods.
	 * </p>
	 */
	public SurfaceRange()
	{
	}


	/**
	 * Determines whether or not the provided observation falls within the
	 * gridded surface temperature range, that vary by date to account for
	 * seasonal changes. Observation passes if the value is greater than the
	 * minimum, and less than the maximum.
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
		// determine the current calendar period
		// syncronized to reuse the single Gregorian calendar instance
		int nPeriod = 0;
		synchronized(m_oCalendar)
		{
			m_oCalendar.setTimeInMillis(iObs.getTimestamp());
			nPeriod = m_oCalendar.get(Calendar.MONTH);
		}

		// get the surface climate record
		SurfaceRecord oSurfaceRecord =
			m_oSurface.getSurfaceRecord(iObs.getLat(), iObs.getLon(), nPeriod);

		if (oSurfaceRecord != null)
		{
			oResult.setPass(oSurfaceRecord.inRange(iObs.getValue()));
			oResult.setConfidence(1.0);
			oResult.setRun();
		}
	}
}
