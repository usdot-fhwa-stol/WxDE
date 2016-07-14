
package wde.comp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;


import wde.cs.ext.NDFD;
import wde.cs.ext.RTMA;
import wde.cs.ext.Radar;
import wde.cs.ext.RAP;
import wde.data.osm.Road;
import wde.data.osm.Roads;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.util.Scheduler;

/**
 * A singleton class that manages the use and scheduling of METRo.  It contains
 * a method that determines which MapCells need to be processed by METRo, 
 * utility methods, and a method that create alerts for the MapCells. When its 
 * run() is called, it places all of the MapCells with roads in them into a 
 * thread pool where they are then processed by METRo.
 * 
 * @author aaron.cherney
 */
public class MetroMgr implements Runnable, Comparator<RoadAlert>
{
	private static final MetroMgr g_oMetroMgr = new MetroMgr();
	private MetroResults m_oMetroResults = MetroResults.getInstance();
	private ArrayList<MapCell> m_oRoadMapCells;  //stores all of the MapCells that contain roads
	private ArrayList<MapCell> m_oBounds;        //stores the bounds of all of the regions the program is ran on
	private ArrayList<RoadAlert> m_oAlerts = new ArrayList();
	private final SimpleDateFormat m_oTimestamp = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm'Z'");
	public final int m_nForecastHours;
	public final int m_nObservationHours;
	private final int m_nThreads;
	private final int m_nOffset;
	private final int m_nPeriod;
	private final int m_nRadarFilesPerHour = 15;
	private double m_dLatTop;
	private double m_dLonLeft;
	private double m_dLatBot;
	private double m_dLonRight;
	private final double m_dStepX = 2.5385189277;
	private final double m_dStepY = 2.5378585548;
	public long m_lNow;
	public AtomicInteger m_nRunning = new AtomicInteger();
	private ExecutorService m_oThreadPool;
	

	private MetroMgr()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nThreads = oConfig.getInt("threads", 1);
		m_oThreadPool = Executors.newFixedThreadPool(m_nThreads);
		m_nForecastHours = oConfig.getInt("fhours", 3);
		m_nObservationHours = oConfig.getInt("ohours", 12);
		m_nOffset = oConfig.getInt("offset", 0);
		m_nPeriod = oConfig.getInt("period", 3600);
		String[] sRegionArray = oConfig.getStringArray("region");
		m_oBounds = new ArrayList(sRegionArray.length);
		//initialize all of the regions bounding boxes
		for (String sRegion : sRegionArray)
		{
			String sRegionBounds = oConfig.getString(sRegion, "");
			String[] sBounds = sRegionBounds.split(",");
			m_dLatTop = Double.parseDouble(sBounds[0]);
			m_dLonLeft = Double.parseDouble(sBounds[1]);
			m_dLatBot = Double.parseDouble(sBounds[2]);
			m_dLonRight = Double.parseDouble(sBounds[3]);
			m_oBounds.add(new MapCell(m_dLatTop, m_dLonLeft, m_dLatBot, m_dLonRight, sRegion));
		}		
		getMapCells(); //initialize list of MapCells that contain roads
		Scheduler.getInstance().schedule(this, m_nOffset, m_nPeriod, true);
	}

	
	/**
	 * Returns a reference to singleton MetroMgr
	 * 
	 * @return Singleton MetroMgr reference
	 */	
	public static MetroMgr getInstance()
	{
		return g_oMetroMgr;
	}
	
	
	/**
	 * A method that fills the RoadMapCells Array List with every cell that
	 * contains a road.
	 */
	public final void getMapCells()
	{
		ArrayList<Road> oRoadList = new ArrayList();
		Roads oRoads = Roads.getInstance();
		double[][] dLatLonTR;   //[0][0] is lat
		double[][] dLatLonBL;   //[1][0] is lon
		double[][] dProjTR;     //[0][0] is x
		double[][] dProjBL;     //[1][0] is y
		m_oRoadMapCells = new ArrayList();

		//for each region defined in the configuration file
		for (MapCell oRegion : m_oBounds)
		{
			//convert lat and lon to lambert conformal projection x and y, need to do this because the grid we are basing our map off of is in lambert conformal
			dProjTR = MetroResults.latLonToLambert(oRegion.m_dLatTop, oRegion.m_dLonRight); //top right
			dProjBL = MetroResults.latLonToLambert(oRegion.m_dLatBot, oRegion.m_dLonLeft);	//bottom left
			for (double x = dProjBL[0][0]; x < dProjTR[0][0]; x += m_dStepX)
			{
				for (double y = dProjBL[1][0]; y < dProjTR[1][0]; y += m_dStepY)
				{
					//convert projection back to lat lon
					dLatLonBL = MetroResults.lambertToLatLon(x, y);
					dLatLonTR = MetroResults.lambertToLatLon(x + m_dStepX, y + m_dStepY);
					//reset the road list
					oRoadList.clear();
					//find roads in the bounding box
					oRoads.getLinks(oRoadList, 1, MathUtil.toMicro(dLatLonBL[1][0]), MathUtil.toMicro(dLatLonBL[0][0]), 
						MathUtil.toMicro(dLatLonTR[1][0]), MathUtil.toMicro(dLatLonTR[0][0]));
					//if there are roads in the list, add the MapCell to the list of cells with roads
					if (!oRoadList.isEmpty())
					{
						m_oRoadMapCells.add(new MapCell(dLatLonTR[0][0], dLatLonBL[1][0],
							dLatLonBL[0][0], dLatLonTR[1][0], oRegion.m_sRegion));
						//add the road IDs to the MapCell's array of roads
						for (Road oRoad : oRoadList)
						{
							m_oRoadMapCells.get(m_oRoadMapCells.size() - 1).m_oRoads.add(oRoad.m_nId);
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * This method returns whether or not there is a presence of precipitation
	 * in the last hour for the given coordinates and time. 1 means there is 
	 * precipitation, 0 means there is no precipitation
	 * 
	 * @param nLat         latitude in micro degrees
	 * @param nLon         longitude in micro degrees
	 * @param lTimestamp   time to look an hour back from
	 * @return   1 if there is precipitation. 0 if there is no precipitation
	 */
	public int getPresenceOfPrecip(int nLat, int nLon, long lTimestamp)
	{
		//check radar files every 4 minutes for the last hour. if reflectivity is greater than 0, there is precipitation
		for (int i = 0; i < m_nRadarFilesPerHour ; i++)
		{
			if (Radar.getInstance().getReading(0, lTimestamp - i * 120000, nLat, nLon) > 0)
				return 1;
		}
		
		return 0;
	}
	
	
	/**
	 * This method is ran every hour to set up and execute the process to run 
	 * METRo for every MapCell that contains a road.
	 */
	@Override
	public void run()
	{
		//check if the previous process is still running, if it is skip the next one
		if (m_nRunning.compareAndSet(0, m_oRoadMapCells.size()))
		{
			//reset Alerts
			m_oAlerts.clear();
			//set the time for the process
			m_lNow = System.currentTimeMillis();
			
			//initialize/update the MetroResults List
			m_oMetroResults.initArrayList(m_lNow, m_nObservationHours, m_nForecastHours);
			
			System.out.println("Started METRo at " + m_oTimestamp.format(m_lNow));
			//queue all of the MapCells into the work queue for the ThreadPool
			for (MapCell oCell : m_oRoadMapCells)
			{
				m_oThreadPool.execute(oCell);
			}
		}
	}
	
	
	/**
	 * This method creates alerts and warnings for all of the roads in the given 
	 * map cell.
	 * 
	 * @param oMapCell  the region to create alerts for
	 */
	public void createAlerts(MapCell oMapCell)
	{
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		
		//create alerts/warnings for each hour of roadcasts
		for (int i = 0; i < m_nForecastHours - 1; i++)  //number of roadcast hours is 1 less than forecast hours input
		{
			//read the road condition from MetroResults
			int nRoadCondition = (int) m_oMetroResults.getReading(51137, oTime.getTimeInMillis(), (oMapCell.m_dLatBot + oMapCell.m_dLatTop) / 2, (oMapCell.m_dLonLeft + oMapCell.m_dLonRight) / 2);
			//convert METRo road conditions into 1204 ess standard numbers
			switch (nRoadCondition)
			{
				case 2:	
					nRoadCondition = 13; //pavement wet alert
					break;
				case 3:
					nRoadCondition = 14; //pavement snow alert
					break;
				case 4:
					nRoadCondition = 15; //pavement slick alert
					break;
				case 8:
					nRoadCondition = 17; //pavement ice alert
					break;
				default:
					nRoadCondition = 1;  //no alert

			}
			if (nRoadCondition > 1)
			{
				synchronized(m_oAlerts)
				{
					for (int nRoadID : oMapCell.m_oRoads)
					{
						//check to see if an alert for the road exists
						RoadAlert oAlert = new RoadAlert(nRoadID, nRoadCondition);
						int nAlertIndex = Collections.binarySearch(m_oAlerts, oAlert, this);
						//if the road does not have an existing alert, add the alert to the list
						if (nAlertIndex < 0)
							m_oAlerts.add(~nAlertIndex, oAlert);
					}
				}
			}
			oTime.add(Calendar.HOUR_OF_DAY, 1);
		}
	}
	
	
	/**
	 *  This function prints all of the roads that have an alert and the kind of
	 *  alert for that road
	 */
	public void printAlerts()
	{
		if (m_oAlerts.isEmpty())
			System.out.println("No alerts");
		else
			for (RoadAlert oAlert : m_oAlerts)
				System.out.println(oAlert.m_nRoadID + ": " + oAlert.getRoadAlertMessage());
	}
	
	
	/**
	 * This method returns the RoadAlert message for the given road.
	 * @param oRoad  the desired road to get an alert for
	 * @return the alert message for that road
	 */
	public String getRoadAlert(Road oRoad)
	{
		for (RoadAlert oAlert : m_oAlerts)
			if (oRoad.m_nId == oAlert.m_nRoadID)
				return oAlert.getRoadAlertMessage();
		
		return "No alert";
	}
	
		
/**
 * Allows RoadAlert objects to be compared by their RoadID
 * @param oLhs  left hand side
 * @param oRhs  right hand side
 * @return 
 */
	@Override
	public int compare(RoadAlert oLhs, RoadAlert oRhs)
	{
		return oLhs.m_nRoadID - oRhs.m_nRoadID;
	}

	
	public static void main(String[] args)
	{
		MetroMgr oMetroMgr = MetroMgr.getInstance();
		NDFD.getInstance();
		RAP.getInstance();
		Radar.getInstance();
		RTMA.getInstance();
		oMetroMgr.run();
	}
}