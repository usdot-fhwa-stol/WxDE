// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file StationObs.java
 */
package clarus.qeds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import clarus.emc.IStation;
import clarus.qedc.IObs;

/**
 * Wraps observations with the station that recorded them.
 *
 * <p>
 * Implements {@code Comparator<IObs>} to enforce an ordering on observations,
 * based off sensor id.
 * </p>
 */
public class StationObs implements Comparator<IObs>
{
    /**
     * Latitude for grouped observations.
     */
	int m_nLat;
    /**
     * Longitude for grouped observations.
     */
	int m_nLon;
    /**
     * Elevation for grouped observations.
     */
	short m_tElev;
    /**
     * Flag showing whether or not this {@code StationObs} object contains
     * observations.
     */
	boolean m_bHasObs;
    /**
     * Station id.
     */
	int m_nId;
    /**
     * Distribution group this station belongs to.
     */
	int m_nDistGroup;
    /**
     * Timestamp corresponding to the last update.
     */
	long m_lLastUpdate;
    /**
     * Observations list.
     */
	final ArrayList<IObs> m_oObs = new ArrayList<IObs>();
    /**
     * Station corresponding to the observations.
     */
	IStation m_iStation;


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code StationObs}
	 * </p>
	 */
	StationObs()
	{
	}


    /**
     * <b> Constructor </b>
     * <p>
     * New instance of {@code StationObs} created with this constructor will
     * have the same station id as the provided station, and the station member
     * ({@code m_iStation}) will be a copy of the supplied station.
     * </p>
     * @param iStation  Object
     */
	StationObs(IStation iStation)
	{
		this(iStation, iStation.getLat(), iStation.getLon(), iStation.getElev());
	}


    /**
     * <b> Constructor </b>
     * <p>
     * New instance of {@code StationObs} created with this constructor will
     * have the same station id as the provided station, and geo-coordinates
	 * from mobile observations.
     * </p>
     * @param iStation  Object
     */
	StationObs(IStation iStation, int nLat, int nLon, short tElev)
	{
		m_nId = iStation.getId();
		m_iStation = iStation;
		m_nLat = nLat;
		m_nLon = nLon;
		m_tElev = tElev;
	}


    /**
     * Determines whether or not the observation list contains any observations.
     * @return true if the observation list contains observations, false
     * otherwise.
     */
	synchronized boolean hasObs()
	{
		return (m_oObs.size() > 0);
	}
	

    /**
     * Adds the supplied observation to the observation list if the observation
     * is new to the list, otherwise existing observations are replaced with
     * newer ones.
     * @param iObs observation to add to the list.
     * @return true if the observation is either added to the list, or replaces
     * an older observation.
     */
	synchronized boolean addObs(IObs iObs)
	{
		int nIndex = Collections.binarySearch(m_oObs, iObs, this);

		// add obs that are completely new
		if (nIndex < 0)
		{
			m_oObs.add(~nIndex, iObs);
			return true;
		}

		// replace existing obs with newer obs
		if (m_oObs.get(nIndex).getTimestamp() <= iObs.getTimestamp())
		{
			m_oObs.set(nIndex, iObs);
			return true;
		}
		
		return false;
	}


    /**
     * Compares the two observations by sensor id.
     * @param oLhs object to compare to {@code oRhs}
     * @param oRhs object to compare to {@code oLhs}
     * @return 0 if the sensor id's match. otherwise they don't.
     */
	public int compare(IObs oLhs, IObs oRhs)
	{
		return (oLhs.getSensorId() -  oRhs.getSensorId());
	}
}
