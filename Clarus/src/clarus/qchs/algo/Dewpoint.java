// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Dewpoint.java
 */
package clarus.qchs.algo;

import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import clarus.emc.ISensor;
import clarus.qchs.ModObs;
import clarus.qchs.ModObsSet;
import clarus.qedc.IObs;
import util.Introsort;

/**
 * Performs spatial quality checking via {@code Barnes}. The difference being,
 * this quality check calculates the weighted mean dewpoint based off
 * surrounding sensor observations, and elevation. If the value being tested
 * differs from the weighted mean by more than the configured standard deviation
 * the check fails
 * <p>
 * Extends {@code Barnes} to perform the quality check spatially, exept that
 * this algorithm checks observations by dewpoint.
 * </p>
 * <p>
 * Implements {@code Comparator<ModObs>} to enforce an ordering based off 
 * observation location.
 * </p>
 */
public class Dewpoint extends Barnes implements Comparator<ModObs>
{
	/**
	 * Airtemp observation type identifier.
	 */
	private static final int OBSTYPE_AIRTEMP = 5733;

	/**
	 * IQR instance used to sort obs values for IQR algorithm.
	 */
	private IQR m_oIQR = new IQR();

	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Dewpoint}.
	 * </p>
	 */
	public Dewpoint()
	{
	}
	

	/**
	 * Calculates the gamma value for the given air temperature and relative
	 * humidity, used for calculation of dewpoint.
	 * @param dTa air temperature.
	 * @param dRh relative humidity.
	 * @return the calculated dewpoint-gamma value.
	 */
	private double gamma(double dTa, double dRh)
	{
				// calculate dew point from air temp and humidity
//				double dTd = Math.log((dRh / 100.0) * 
//					Math.exp((17.502 * dTa) / (240.97 + dTa)));
//				return 240.97 * dTd / (17.502 - dTd);
				
		double dTd = Math.log(dRh / 100.0) + 17.271 * dTa / (237.7 + dTa);
		return 237.7 * dTd / (17.271 - dTd);				
	}


	/**
	 * Computes and compares the dewpoint for the provided observation against
	 * the spatially weithed mean dewpoint. If the observations dewpoint falls
	 * within the configured standard deviation of the weighted mean, the
	 * check passes, else it fails.
	 * @param nObsTypeId observation type of interest.
	 * @param iSensor observing sensor.
	 * @param iObs observation to check.
	 * @param oResult results of the quality check.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		// retrieve the background field
		int nLat = iObs.getLat();
		int nLon = iObs.getLon();
		long lTimestamp = iObs.getTimestamp();
		
		// retrieve the relative humidity background field
		ArrayList<IObs> oRh = new ArrayList<IObs>();

		// bounding box based on latitude to compensate for the spherical earth
		int nLonRadius = sphereAdjust(m_nGeoRadiusMax, nLat);
		m_oObsMgr.getBackground(nObsTypeId, nLat - m_nGeoRadiusMax,
			nLon - nLonRadius, nLat + m_nGeoRadiusMax,
			nLon + nLonRadius, lTimestamp + m_lTimerangeMin,
			lTimestamp + m_lTimerangeMax, oRh);
		
		int nIndex = oRh.size();
		// cannot proceed without relative humidity obs
		if (nIndex == 0)
			return;
		
		// all obs inside the max radius are evaluated
		long lOuter = m_nGeoRadiusMax;
		lOuter *= lOuter;

		ModObsSet oModObsSet = readLock();
		oModObsSet.clear();
		oModObsSet.ensureCapacity(nIndex);

		// filter the relative humidity obs for the region
		ModObs oModObs = oModObsSet.getModObs();
		while (nIndex-- > 0)
		{
			oModObs.setObs(oRh.get(nIndex));
			oModObs.setDstSqr(nLat, nLon);

			// include the obs when the squared distance is within the region
			if (oModObs.m_lDstSqr < lOuter)
			{
				// save the obs information to be modified later
				oModObsSet.add(oModObs);
				// get a new working modified obs object
				oModObs = oModObsSet.getModObs();
			}
		}
		// return the working modified obs to the pool
		oModObsSet.putModObs(oModObs);
		
		if (oModObsSet.size() == 0)
		{
			readUnlock();
			return;
		}
		
		// sort the accepted relative humidity obs by location
		Introsort.usort(oModObsSet, this);

		// retrieve the air temperature background field
		ArrayList<IObs> oAirTemp =  new ArrayList<IObs>();
		
		m_oObsMgr.getBackground(OBSTYPE_AIRTEMP, nLat - m_nGeoRadiusMax,
			nLon - nLonRadius, nLat + m_nGeoRadiusMax,
			nLon + nLonRadius, lTimestamp + m_lTimerangeMin,
			lTimestamp + m_lTimerangeMax, oAirTemp);

		nIndex = oAirTemp.size();
		// at least one air temp is required for a valid comparison
		if (nIndex == 0)
		{
			readUnlock();
			return;
		}
		
		// get a modified obs object for searching
		oModObs = oModObsSet.getModObs();
		while (nIndex-- > 0)
		{
			// set up the search criteria
			IObs iAirTempObs = oAirTemp.get(nIndex);
			oModObs.m_nLat = iAirTempObs.getLat();
			oModObs.m_nLon = iAirTempObs.getLon();

			int nRhIndex = Collections.binarySearch(oModObsSet, oModObs, this);
			if (nRhIndex >= 0)
			{
				ModObs oModRh = oModObsSet.get(nRhIndex);
				double dTa = iAirTempObs.getValue();
				double dTd = gamma(dTa, oModRh.m_dValue);
				if (dTd > dTa)
					dTd = dTa;
				oModRh.m_dValue = dTd;
				oModRh.m_bModified = true;
			}						
		}

		// search for a modified obs that corresponds to the current obs
		oModObs.m_nLat = nLat;
		oModObs.m_nLon = nLon;
		nIndex = Collections.binarySearch(oModObsSet, oModObs, this);
		// return the search object to the pool
		oModObsSet.putModObs(oModObs);

		if (nIndex < 0)
		{
			readUnlock();
			return;
		}

		// remove the modified obs from the list
		oModObs = oModObsSet.remove(nIndex);
		double dValue = oModObs.m_dValue;
		oModObsSet.putModObs(oModObs);
		
		// return the remaining unmodified obs to the pool
		nIndex = oModObsSet.size();
		while (nIndex-- > 0)
		{
			if (!oModObsSet.get(nIndex).m_bModified)
				oModObsSet.putModObs(oModObsSet.remove(nIndex));
		}

		// complete the quality check for any remaining calculated obs
		nIndex = oModObsSet.size();
		if (nIndex < 5)
		{
			readUnlock();
			return;
		}

		IQR.runIQR(iSensor.getSensorId(), dValue, iObs, oResult, m_dSdMin,
			m_dSdMax, oModObsSet, m_oIQR);
		readUnlock();
	}


	/**
	 * Determines whether the observations occured at the same location, by
	 * latitude and longitude.
	 * @param oLhs value to compare to {@code oRhs}
	 * @param oRhs value to compare to {@code oLhs}
	 * @return 0 if the obvervation locations match. &lt 0 if {@code oLhs} is
	 * less than {@code oRhs}.
	 */
	public int compare(ModObs oLhs, ModObs oRhs)
	{
		int nCompare = oLhs.m_nLat - oRhs.m_nLat;
		if (nCompare == 0)
			return oLhs.m_nLon - oRhs.m_nLon;
		
		return nCompare;
	}
}
