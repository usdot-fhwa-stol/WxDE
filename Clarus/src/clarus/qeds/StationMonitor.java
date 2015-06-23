// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file StationMonitor.java
 */
package clarus.qeds;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import clarus.ClarusMgr;
import clarus.emc.ISensor;
import clarus.emc.IStation;
import clarus.emc.Sensors;
import clarus.emc.Stations;
import clarus.qedc.IObs;
import clarus.qedc.IObsSet;
import util.Config;
import util.ConfigSvc;
import util.Introsort;
import util.Scheduler;
import util.threads.AsyncQ;

/**
 * Wraps all cached stations and their corresponding set of observations
 * together to be registered and processed by the Clarus manager.
 * <p>
 * Singleton class whose instance can be retrieved with the
 * {@link StationMonitor#getInstance()}.
 * </p>
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of the observations set
 * as it is enqueued.
 * </p>
 */
public class StationMonitor extends AsyncQ<IObsSet>
{
	/**
	 * Configured timeout, defaults to 14400000. Determines when stations
	 * should be flagged as having new observations (if it has been updated
	 * since the timeout).
	 */
	private static long TIMEOUT = 14400000;
	/**
	 * Pointer to station monitor instance.
	 */
	private static StationMonitor g_oInstance = new StationMonitor();

	/**
	 * List of stations, and their corresponding observations.
	 */
	private final ArrayList<StationObs> m_oStationList = new ArrayList<StationObs>();
	/**
	 * Pointer to {@code ClarusMgr} singleton instance.
	 */
	private ClarusMgr m_oClarusMgr = ClarusMgr.getInstance();
	/**
	 * Pointer to {@code Stations} cache.
	 */
	private Stations m_oStations = Stations.getInstance();
	/**
	 * Pointer to {@code Sensors} cache.
	 */
	private Sensors m_oSensors = Sensors.getInstance();
	/**
	 * Station-observation pair used for searching the stations list.
	 */
	private StationObs m_oSearchStation = new StationObs();
	/**
	 * Comparator enforces ordering by station-id.
	 */
	private SortByStation m_oSortByStation = new SortByStation();
	/**
	 * Object used to schedule removal time of expired obs and mobile stations.
	 */
	private Cleanup m_oCleanup = new Cleanup();
	

	/**
	 * <b> Accessor </b>
	 * @return singleton instance of {@code StationMonitor}.
	 */
	public static StationMonitor getInstance()
	{
		return g_oInstance;
	}
	

	/**
	 * Creates new instances of {@code StationMonitor}. Configures the station
	 * monitor. Populates the station list with cached stations. Updates the
	 * station distribution group with the corresponding cached sensor
	 * distribution group. Registers the station monitor with the system
	 * manager.
	 */
	private StationMonitor()
	{
		// apply the station monitor configuration
		ConfigSvc oConfigSvc = ConfigSvc.getInstance();
		Config oConfig = oConfigSvc.getConfig(this);

		// increase the queue depth for more thread concurrency
		TIMEOUT = oConfig.getLong("timeout", TIMEOUT);

		// initialize the station list
		ArrayList<IStation> oStations = new ArrayList<IStation>();
		m_oStations.getStations(oStations);

		synchronized(this)
		{
			int nIndex = oStations.size();
			m_oStationList.ensureCapacity(nIndex);
			while (nIndex-- > 0)
			{
				IStation iStation = oStations.get(nIndex);
				if (iStation.getCat() == 'M')
					continue; // only permanent stations are initialized here

				StationObs oStationObs = new StationObs(iStation);
				if (iStation.getContribId() == 4)
					oStationObs.m_nDistGroup = 1;
				m_oStationList.add(oStationObs);
			}

			// sort the newly acquired stations by id
			Introsort.usort(m_oStationList, m_oSortByStation);
		}

		// schedule cleanup process every five minutes
		Scheduler.getInstance().schedule(m_oCleanup, 13, 300);

        // register the station monitor with system manager
		m_oClarusMgr.register(getClass().getName(), this);
		System.out.println(getClass().getName());
	}


	/**
	 * Traverses the provided observation set, determining which station each
	 * observation belongs to. If the station is not already being monitored,
	 * the station is added to the station-obs list. Otherwise, the observation
	 * is added to the set of observations contained by the corresponding
	 * station. The station is then flagged showing that it has been updated
	 * recently. The observation set is then queued into the Clarus monitor
	 * for immediate processing.
	 * @param iObsSet set of observations to process.
	 */
    @Override
	public void run(IObsSet iObsSet)
	{
		StationObs oStation = null;
		
		// add the obs to the station and set the latest update timestamp
		int nObsIndex = iObsSet.size();
		while (nObsIndex-- > 0)
		{
			IObs iObs = iObsSet.get(nObsIndex);
			// find the sensor and then the station to which it belongs
			ISensor iSensor = m_oSensors.getSensor(iObs.getSensorId());
			if (iSensor == null)
				continue;
			
			IStation iStation = m_oStations.getStation(iSensor.getStationId());
			if (iStation == null)
				continue;

            synchronized(m_oStationList)
            {
                // search for the station in the managed list
                m_oSearchStation.m_nId = iStation.getId();
				m_oSearchStation.m_nLat = iObs.getLat();
				m_oSearchStation.m_nLon = iObs.getLon();

				int nStationIndex = Collections.binarySearch(m_oStationList,
                    m_oSearchStation, m_oSortByStation);

				// add new stations as they become available
                if (nStationIndex < 0)
                {
                    oStation = new StationObs(iStation);
					oStation.m_nLat = iObs.getLat();
					oStation.m_nLon = iObs.getLon();
                    m_oStationList.add(~nStationIndex, oStation);
                }
                else
                    oStation = m_oStationList.get(nStationIndex);
            }
			
			// update the station distribution group to the highest available
			if (iSensor.getDistGroup() > oStation.m_nDistGroup)
				oStation.m_nDistGroup = iSensor.getDistGroup();
			
			// set the latest update timestamp
			if (oStation.addObs(iObs) && 
				iObs.getTimestamp() > oStation.m_lLastUpdate)
				oStation.m_lLastUpdate = iObs.getTimestamp();
		}		
		
		// queue the obs set for the next process
		m_oClarusMgr.queue(iObsSet);
	}


	/**
	 * Retrieves the station from the list corresponding to the provided
	 * station id.
	 * @param nId id of the station of interest.
	 * @return the station corresponding to the supplied id if contained in the
	 * list, else null.
	 */
    StationObs getStation(int nId, int nLat, int nLon)
    {
        synchronized(m_oStationList)
        {
            m_oSearchStation.m_nId = nId;
			m_oSearchStation.m_nLat = nLat;
			m_oSearchStation.m_nLon = nLon;

			int nIndex = Collections.binarySearch(m_oStationList,
                    m_oSearchStation, m_oSortByStation);

            if (nIndex >= 0)
                return m_oStationList.get(nIndex);
        }

        return null;
    }
	

	/**
	 * <b> Accessor </b>
	 * @return a copy of the station-obs list.
	 */
	ArrayList<StationObs> getStations()
	{
        ArrayList<StationObs> oStations = new ArrayList<StationObs>();

        synchronized(m_oStationList)
        {
			int nIndex = m_oStationList.size();
			oStations.ensureCapacity(nIndex);
			while (nIndex-- > 0)
				oStations.add(m_oStationList.get(nIndex));
        }
		
		return oStations;
	}


	/**
	 * Implements {@code Comparator<StationObs>} to enforce an ordering based
	 * off station id.
	 */
	private class SortByStation implements Comparator<StationObs>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortByStationId}
		 * </p>
         */
		private SortByStation()
		{
		}


		/**
		 * Compares the provided {@code StationObs} by station id.
		 *
		 * @param oLhs object to compare to {@code oRhs}
		 * @param oRhs object to compare to {@code oLhs}
		 * @return 0 if the objects match by station id and geo-coordinates.
		 */
		public int compare(StationObs oLhs, StationObs oRhs)
		{
			int nDiff = oLhs.m_nId - oRhs.m_nId;
			if (nDiff != 0)
				return nDiff;

			nDiff = oLhs.m_nLat - oRhs.m_nLat;
			if (nDiff != 0)
				return nDiff;

			return (oLhs.m_nLon - oRhs.m_nLon);
		}
	}


	private class Cleanup implements Runnable
	{
		Cleanup()
		{
		}


		public void run()
		{
			long lNow = System.currentTimeMillis();
			long lExpired = lNow - TIMEOUT;
			long lExpiredMobile = lNow - 3600000; // one hour timeout

            synchronized(m_oStationList)
            {
				int nIndex = m_oStationList.size();
				while (nIndex-- > 0)
				{
					StationObs oStation = m_oStationList.get(nIndex);

					// flag all stations that have obs older than the timeout
					oStation.m_bHasObs = (oStation.m_lLastUpdate >= lExpired);

					if (oStation.m_iStation.getCat() == 'M')
					{
						// expired mobile stations get removed from the list
						if (oStation.m_lLastUpdate < lExpiredMobile)
							m_oStationList.remove(nIndex);
					}
				}
			}
		}
	}
}
