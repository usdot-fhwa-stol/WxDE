package wde.comp;

import java.util.ArrayList;
import java.util.Collections;
import wde.WDEMgr;
import wde.cs.ext.RTMA;
import wde.cs.ext.Radar;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.data.osm.Road;
import wde.data.osm.Roads;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.obs.ObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;

/**
 * This class is responsible for making Inferred Observations on pavement, 
 * visibility, and precipitation situations based off of temperature readings 
 * from mobile stations and radar reflectivity.
 * @author aaron.cherney
 */
public class InferObs extends AsyncQ<IObsSet>
{
	private final int m_nAirTemp;
	private final int m_nDewTemp;
	private final int m_nRh;
	private final int m_nWindSpd;
	private final int m_nPrecipSit;
	private final int m_nVisibilitySit;
	private final int m_nPavementSit;
	private final int m_nTimeLimit;  //number of minutes a RoadObs is kept
	//precip intensity constants from NCAR
	private final int m_nLIGHTWINTERPRECIP = 10;
	private final int m_nMODERATEWINTERPRECIP = 20;
	private final int m_nLIGHTSUMMERPRECIP = 20;
	private final int m_nMODERATESUMMERPRECIP = 40;
	//1204 precip situation constants
	final int m_nSNOWSLIGHT = 7;
	final int m_nSNOWMODERATE = 8;
	final int m_nSNOWHEAVY = 9;
	final int m_nRAINSLIGHT = 10;
	final int m_nRAINMODERATE = 11;
	final int m_nRAINHEAVY = 12;
	final int m_nFROZENPRECIPITATIONSLIGHT = 13;
	final int m_nFROZENPRECIPITATIONMODERATE = 14;
	final int m_nFROZENPRECIPITATIONHEAVY = 15;
	//1204 mobile observation pavement constants
	final int m_nWET = 3;
	final int m_nDUSTINGFRESHSNOW = 9;
	final int m_nMODERATEFRESHSNOW = 10;
	final int m_nDEEPFRESHSNOW = 11;
	final int m_nICEPATCHES = 20;
	final int m_nMODERATELYICY = 21;
	final int m_nHEAVYICING = 22;
	//1204 visibility situation constants
	final int m_nBLOWINGSNOW = 6;
	
	/**
	 * list of accepted temperature observation types
	 */
	private final int[] m_nObsTypes;
	/**
	 * Reference to the singleton instance of {@link WDEMgr}.
	 */
	private final WDEMgr m_oWdeMgr = WDEMgr.getInstance();
	/**
	 * Instance of platform observation resolving service.
	 */
	protected final PlatformDao m_oPlatformDao = PlatformDao.getInstance();
	/**
	 * Instance of road resolving service.
	 */
	protected final Roads m_oRoads = Roads.getInstance();
	/**
	 * Instance of Sensor observation resolving service.
	 */
	protected final SensorDao m_oSensorDao = SensorDao.getInstance();
	/**
	 * Manages inferred observations.
	 */
	private final ObsMgr m_oObsMgr = ObsMgr.getInstance();
	/**
	 * Reference to the singleton instance of radar reflectivity.
	 */
	private final Radar m_oRadar = Radar.getInstance();
	/**
	 * Reference to the singleton instance of RTMA
	 */
	private final RTMA m_oRtma = RTMA.getInstance();
	/**
	 * List that contains the most recent RoadObs
	 */
	private final ArrayList<RoadObs> m_oRoadObs = new ArrayList();
	/**
	 * Reference to the singleton instance of this class InferObs.
	 */
	private static final InferObs g_oInferObs = new InferObs();


	/**
	 * Returns a reference to singleton InferObs processor object.
	 *
	 * @return reference to InferObs instance.
	 */
	public static InferObs getInstance()
	{
		return g_oInferObs;
	}


	/**
	 * <b> Default Private Constructor </b>
	 * <p>
	 * Creates new instances of {@code InferObs}, of which there should be 
	 * only one.
	 * </p>
	 */
	private InferObs()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nTimeLimit = oConfig.getInt("limit", 20);
		String[] sObsTypes = oConfig.getString("accept", "5733,2001180").split(",");
		m_nObsTypes = new int[sObsTypes.length]; // convert and copy obs type ids
		for (int nIndex = 0; nIndex < sObsTypes.length; nIndex++)
			m_nObsTypes[nIndex] = Integer.parseInt(sObsTypes[nIndex]);	

//		 input observation types
		m_nAirTemp = Integer.parseInt(oConfig.getString("essAirTemperature", "5733"));
		m_nDewTemp = Integer.parseInt(oConfig.getString("essDewpointTemp", "575"));
		m_nRh = Integer.parseInt(oConfig.getString("essPrecipSituation", "589"));
		m_nWindSpd = Integer.parseInt(oConfig.getString("windSensorAvgSpeed", "56104"));
		// output observation types
		m_nPrecipSit = Integer.parseInt(oConfig.getString("essPrecipSituation", "589"));
		m_nVisibilitySit = Integer.parseInt(oConfig.getString("essVisibilitySituation", "5102"));
		m_nPavementSit = Integer.parseInt(oConfig.getString("essMobileObservationPavement", "5123"));
//		int m_nWiperStatus = Integer.parseInt(oConfig.getString("canWiperStatus", "2000001"));
		m_oWdeMgr.register(getClass().getName(), this);
	}


	/**
	 * Processes each observation within the set into inferred 
	 * pseudo-observations. The supplied observation set must be an air 
	 * temperature and from a mobile platform. Other observations are ignored.
	 * .
	 * <p>
	 * Overrides base class method {@link AsyncQ#run()}.
	 * </p>
	 *
	 * @param iObsSet observation set to process and queue.
	 */
	@Override
	public void run(IObsSet iObsSet)
	{
		long lTimestamp = System.currentTimeMillis();
		
		synchronized(m_oRoadObs)  //remove old RoadObs
		{
			int nSize = m_oRoadObs.size();
			while (nSize-- > 0)
			{
				if (m_oRoadObs.get(nSize).m_lTimestamp + (m_nTimeLimit * 60 * 1000) <= lTimestamp)
					m_oRoadObs.remove(nSize);
			}
		}

		ObsSet oPrecipSit = null;  // request inferred observation sets
		ObsSet oVisibilitySit = null;
		ObsSet oPavementSit = null;
		
		int nIndex = m_nObsTypes.length;
		boolean bFound = false; // filter obs types other than air temperatures
		while (!bFound && nIndex-- > 0)
			bFound = (iObsSet.getObsType() == m_nObsTypes[nIndex]);
		
		if (bFound)
		{
			nIndex = iObsSet.size();
			while (nIndex-- > 0) // only process mobile observations
			{
				IObs iObs = iObsSet.get(nIndex);
				int nElev = iObs.getElevation();
				double dAirTemp = iObs.getValue();
				double dRefl = m_oRadar.getReading(0, iObs.getObsTimeLong(),
							iObs.getLatitude(), iObs.getLongitude()); // obs type id is 0 for unknown
				if (Double.isNaN(dRefl))  //skip if no reflectivity data
					continue;
				ISensor iSensor = m_oSensorDao.getSensor(iObs.getSensorId());
				IPlatform iPlatform = m_oPlatformDao.getPlatform(iSensor.getPlatformId());
				if (iPlatform.getCategory() == 'M')
				{
					Road oRoad = m_oRoads.getLink(1000, iObs.getLongitude(), 
						iObs.getLatitude());
					if (oRoad != null) // filter for existing roads
					{
						synchronized(m_oRoadObs)
						{
							RoadObs oRoadObs;
							int nRoad = Collections.binarySearch(m_oRoadObs, oRoad);  //check if there is already a RoadObs for the road
							if (nRoad < 0)
							{
								oRoadObs = new RoadObs(oRoad, dRefl, iObs.getObsTimeLong(), nElev);  //create a new RoadObs
								m_oRoadObs.add(~nRoad, oRoadObs);
							}
							else
								oRoadObs = m_oRoadObs.get(nRoad);   //if the RoadObs exists already get that one
						
							oRoadObs.m_dAirTemp.add(dAirTemp);  //add the temp to the RoadObs temperature list
							oRoadObs.m_nPrevPavement = oRoadObs.m_nPavementSit;  //set prev situations to check against later
							oRoadObs.m_nPrevPrecip = oRoadObs.m_nPrecipSit;
							oRoadObs.m_nPrevVisibility = oRoadObs.m_nVisibilitySit;
						}
					}
				}
			}
			
			boolean bNeedObsSet = true;
			for (RoadObs oRoadObs : m_oRoadObs)  //create inferred obs for each RoadObs
			{
				double dRtmaTemp = m_oRtma.getReading(5733, oRoadObs.m_lTimestamp, oRoadObs.m_oRoad.m_nYmid, oRoadObs.m_oRoad.m_nXmid);
				if (Double.isNaN(dRtmaTemp) || oRoadObs.m_dRefl <= 0)  //skip if there is no RTMA reading or there is no precipitation (refl <= 0)
					continue;
				else if (bNeedObsSet)
				{
					oPrecipSit = m_oObsMgr.getObsSet(m_nPrecipSit);
					oVisibilitySit = m_oObsMgr.getObsSet(m_nVisibilitySit);
					oPavementSit = m_oObsMgr.getObsSet(m_nPavementSit);
					bNeedObsSet = false;
				}
				
				double dAverageTemp = 0;
				for (double dTemp : oRoadObs.m_dAirTemp)   //calculate the average temp from all mobile observations and RTMA
					dAverageTemp += dTemp;
				
				dAverageTemp = (dAverageTemp + dRtmaTemp) / (oRoadObs.m_dAirTemp.size() + 1);
				
				if (dAverageTemp > 2) //precip type rain
				{
					oRoadObs.m_nPavementSit = m_nWET;    
					if (oRoadObs.m_dRefl <= m_nLIGHTSUMMERPRECIP) //light rain
						oRoadObs.m_nPrecipSit = m_nRAINSLIGHT;
					else if (oRoadObs.m_dRefl > m_nLIGHTSUMMERPRECIP && oRoadObs.m_dRefl <= m_nMODERATESUMMERPRECIP) //moderate rain
						oRoadObs.m_nPrecipSit = m_nRAINMODERATE;
					else  //heavy rain
						oRoadObs.m_nPrecipSit = m_nRAINHEAVY;
				}
				else if (dAverageTemp < -2) //precip type snow
				{
					if (oRoadObs.m_dRefl <= m_nLIGHTWINTERPRECIP) //light snow
					{
						oRoadObs.m_nPrecipSit = m_nSNOWSLIGHT;
						oRoadObs.m_nPavementSit = m_nDUSTINGFRESHSNOW;
					}
					else if (oRoadObs.m_dRefl > m_nLIGHTWINTERPRECIP && oRoadObs.m_dRefl <= m_nMODERATEWINTERPRECIP) //moderate snow
					{
						oRoadObs.m_nPrecipSit = m_nSNOWMODERATE;
						oRoadObs.m_nPavementSit = m_nMODERATEFRESHSNOW;
						oRoadObs.m_nVisibilitySit = m_nBLOWINGSNOW;
					}
					else //heavy snow
					{
						oRoadObs.m_nPrecipSit = m_nSNOWHEAVY;
						oRoadObs.m_nPavementSit = m_nDEEPFRESHSNOW;
						oRoadObs.m_nVisibilitySit = m_nBLOWINGSNOW;
					}
				}
				else //precip type mix
				{
					if (oRoadObs.m_dRefl <= m_nLIGHTWINTERPRECIP) //light frozen
					{
						oRoadObs.m_nPrecipSit = m_nFROZENPRECIPITATIONSLIGHT;
						oRoadObs.m_nPavementSit = m_nICEPATCHES;
					}
					else if (oRoadObs.m_dRefl > m_nLIGHTWINTERPRECIP && oRoadObs.m_dRefl <= m_nMODERATEWINTERPRECIP) //moderate frozen
					{
						oRoadObs.m_nPrecipSit = m_nFROZENPRECIPITATIONMODERATE;
						oRoadObs.m_nPavementSit = m_nMODERATELYICY;
					}
					else //heavy frozen
					{
						oRoadObs.m_nPrecipSit = m_nFROZENPRECIPITATIONHEAVY;
						oRoadObs.m_nPavementSit = m_nHEAVYICING;
					}
				}
				//check is situations have changed for the RoadObs, if they have add a new Obs to the ObsSet
				if (oPrecipSit != null && (oRoadObs.m_nPrecipSit != oRoadObs.m_nPrevPrecip))
					oPrecipSit.addObs(0, 0, lTimestamp, lTimestamp, oRoadObs.m_oRoad.m_nYmid, oRoadObs.m_oRoad.m_nXmid, (short) oRoadObs.m_nElev, oRoadObs.m_nPrecipSit);
				if (oPavementSit != null && oRoadObs.m_nPavementSit != oRoadObs.m_nPrevPavement)
					oPavementSit.addObs(0, 0, lTimestamp, lTimestamp, oRoadObs.m_oRoad.m_nYmid, oRoadObs.m_oRoad.m_nXmid, (short) oRoadObs.m_nElev, oRoadObs.m_nPavementSit);
				if (oVisibilitySit != null && oRoadObs.m_nVisibilitySit != oRoadObs.m_nPrevVisibility)
					oVisibilitySit.addObs(0, 0, lTimestamp, lTimestamp, oRoadObs.m_oRoad.m_nYmid, oRoadObs.m_oRoad.m_nXmid, (short) oRoadObs.m_nElev, oRoadObs.m_nVisibilitySit);
			}
		}
		
		// queue inferred observation sets to next stage
		if (oPrecipSit != null && !oPrecipSit.isEmpty())
			m_oObsMgr.queue(oPrecipSit);
		if (oPavementSit != null && !oPavementSit.isEmpty())
			m_oObsMgr.queue(oPavementSit);
		if (oVisibilitySit != null && !oVisibilitySit.isEmpty())
			m_oObsMgr.queue(oVisibilitySit);
		m_oWdeMgr.queue(iObsSet); // always requeue received observation set
	}


	private static double calcRh(double dDewTemp, double dAirTemp)
	{
		double dPrVapT = 6.112 * Math.exp((17.67 * dAirTemp) / (dAirTemp + 243.5));
		double dPrVapD = 6.112 * Math.exp((17.67 * dDewTemp) / (dDewTemp + 243.5));
		return dPrVapD / dPrVapT;
	}
}
