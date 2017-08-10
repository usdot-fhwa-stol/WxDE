package wde.comp;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import wde.WDEMgr;
import wde.cs.ext.NDFD;
import wde.cs.ext.RAP;
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
import wde.util.MathUtil;
import wde.util.Scheduler;
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
	private final int m_nSurfaceStatus;
	private final int m_nTimeLimit;  //number of minutes a RoadObs is kept
	//precip intensity constants from NCAR
	private final int m_nLIGHT_WINTER_PRECIP = 10;
	private final int m_nMODERATE_WINTER_PRECIP = 20;
	private final int m_nLIGHT_SUMMER_PRECIP = 20;
	private final int m_nMODERATE_SUMMER_PRECIP = 40;
	//forecast precip intensity constants from NCAR
	private final double m_dFORECAST_LIGHT_PRECIP_WINTER = .254; //0.254 mm. (0.01 in.)
	private final double m_dFORECAST_MODERATE_PRECIP_WINTER = 2.54; // 2.54 mm. (0.10 in.)
	private final double m_dFORECAST_LIGHT_PRECIP_SUMMER = 2.54; //2.54 mm. (0.10 in.)
	private final double m_dFORECAST_MODERATE_PRECIP_SUMMER = 7.62; //7.62 mm. (0.30 in.)
	//1204 precip situation constants
	final int m_nSNOW_SLIGHT = 7;
	final int m_nSNOW_MODERATE = 8;
	final int m_nSNOW_HEAVY = 9;
	final int m_nRAIN_SLIGHT = 10;
	final int m_nRAIN_MODERATE = 11;
	final int m_nRAIN_HEAVY = 12;
	final int m_nFROZEN_PRECIPITATION_SLIGHT = 13;
	final int m_nFROZEN_PRECIPITATION_MODERATE = 14;
	final int m_nFROZEN_PRECIPITATION_HEAVY = 15;
	//1204 mobile observation pavement constants
	final int m_nWET = 3;
	final int m_nDUSTING_FRESH_SNOW = 9;
	final int m_nMODERATE_FRESH_SNOW = 10;
	final int m_nDEEP_FRESH_SNOW = 11;
	final int m_nICE_PATCHES = 20;
	final int m_nMODERATELY_ICY = 21;
	final int m_nHEAVY_ICING = 22;
	//1204 visibility situation constants
	final int m_nBLOWING_SNOW = 6;
	//1204 surface status constants
	final int m_nWET_PAVEMENT = 5;
	final int m_nICE_WARNING = 7;
	final int m_nSNOW_WARNING = 9;
	
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
	private double m_dLatTop;
	private double m_dLatBot;
	private double m_dLonRight;
	private double m_dLonLeft;
	private int m_nForecastHrs;
	


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
		m_nSurfaceStatus = Integer.parseInt(oConfig.getString("essSurfaceStatus", "51137"));
//		int m_nWiperStatus = Integer.parseInt(oConfig.getString("canWiperStatus", "2000001"));
		String sRegion = oConfig.getString("region", "");
		String[] sBounds = sRegion.split(",");
		m_dLatTop = Double.parseDouble(sBounds[0]);
		m_dLonLeft = Double.parseDouble(sBounds[1]);
		m_dLatBot = Double.parseDouble(sBounds[2]);
		m_dLonRight = Double.parseDouble(sBounds[3]);	
		m_nForecastHrs = oConfig.getInt("hours", 5); //Metro gives a forecast of 5 hours and we need road temp from that
		m_oWdeMgr.register(getClass().getName(), this);
		Scheduler.getInstance().schedule(new ForecastInferObs(), 300, 3600, true);
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
					if (oRoadObs.m_dRefl <= m_nLIGHT_SUMMER_PRECIP) //light rain
						oRoadObs.m_nPrecipSit = m_nRAIN_SLIGHT;
					else if (oRoadObs.m_dRefl > m_nLIGHT_SUMMER_PRECIP && oRoadObs.m_dRefl <= m_nMODERATE_SUMMER_PRECIP) //moderate rain
						oRoadObs.m_nPrecipSit = m_nRAIN_MODERATE;
					else  //heavy rain
						oRoadObs.m_nPrecipSit = m_nRAIN_HEAVY;
				}
				else if (dAverageTemp < -2) //precip type snow
				{
					if (oRoadObs.m_dRefl <= m_nLIGHT_WINTER_PRECIP) //light snow
					{
						oRoadObs.m_nPrecipSit = m_nSNOW_SLIGHT;
						oRoadObs.m_nPavementSit = m_nDUSTING_FRESH_SNOW;
					}
					else if (oRoadObs.m_dRefl > m_nLIGHT_WINTER_PRECIP && oRoadObs.m_dRefl <= m_nMODERATE_WINTER_PRECIP) //moderate snow
					{
						oRoadObs.m_nPrecipSit = m_nSNOW_MODERATE;
						oRoadObs.m_nPavementSit = m_nMODERATE_FRESH_SNOW;
						oRoadObs.m_nVisibilitySit = m_nBLOWING_SNOW;
					}
					else //heavy snow
					{
						oRoadObs.m_nPrecipSit = m_nSNOW_HEAVY;
						oRoadObs.m_nPavementSit = m_nDEEP_FRESH_SNOW;
						oRoadObs.m_nVisibilitySit = m_nBLOWING_SNOW;
					}
				}
				else //precip type mix
				{
					if (oRoadObs.m_dRefl <= m_nLIGHT_WINTER_PRECIP) //light frozen
					{
						oRoadObs.m_nPrecipSit = m_nFROZEN_PRECIPITATION_SLIGHT;
						oRoadObs.m_nPavementSit = m_nICE_PATCHES;
					}
					else if (oRoadObs.m_dRefl > m_nLIGHT_WINTER_PRECIP && oRoadObs.m_dRefl <= m_nMODERATE_WINTER_PRECIP) //moderate frozen
					{
						oRoadObs.m_nPrecipSit = m_nFROZEN_PRECIPITATION_MODERATE;
						oRoadObs.m_nPavementSit = m_nMODERATELY_ICY;
					}
					else //heavy frozen
					{
						oRoadObs.m_nPrecipSit = m_nFROZEN_PRECIPITATION_HEAVY;
						oRoadObs.m_nPavementSit = m_nHEAVY_ICING;
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

	public class ForecastInferObs implements Runnable
	{
		@Override
		public void run()
		{
			Calendar oCal = Calendar.getInstance(Scheduler.UTC);
			oCal.set(Calendar.MILLISECOND, 0);
			oCal.set(Calendar.SECOND, 0);
			oCal.set(Calendar.MINUTE, 0);
			long lTimestamp = oCal.getTimeInMillis();
			RAP oRAP = RAP.getInstance();
			MetroResults oMR = MetroResults.getInstance();
			NDFD oNDFD = NDFD.getInstance();
			ArrayList<Road> oRoads = new ArrayList();
			m_oRoads.getLinks(oRoads, 1, MathUtil.toMicro(m_dLonLeft), MathUtil.toMicro(m_dLatBot), MathUtil.toMicro(m_dLonRight), MathUtil.toMicro(m_dLatTop));
			ObsSet oPavementSit = null;
			ObsSet oPrecipSit = null;
			ObsSet oVisibilitySit = null;
			boolean bNeedObsSet = true;
			for (int i = 1; i <= m_nForecastHrs; i++) //for each hour to be forecasted
			{
				for (Road oRoad : oRoads) //for every road make infer obs based off of forecasts
				{
					int nLat = oRoad.m_nYmid;
					int nLon = oRoad.m_nXmid;
					short tElev = oRoad.m_tElev;
					long lForecastTime = lTimestamp + (3600000 * i);
					int nPrecipType = (int)oRAP.getReading(207, lForecastTime, nLat, nLon);
					double dPrecipRate = oRAP.getReading(587, lForecastTime, nLat, nLon) * 3600; //convert from kg/(m^2 * sec) to mm in an hour
					double dRoadTemp = oMR.getReading(51138, lForecastTime, nLat, nLon);
					double dAirTemp = oNDFD.getReading(5733, lForecastTime, nLat, nLon);
					int nPavementSit = 0;
					int nPrecipSit = 0;
					int nVisibilitySit = 0;


					if (nPrecipType == 3 || dPrecipRate <= 0 || Double.isNaN(dRoadTemp)) //skip if the precip type is none, precip rate is less than or equal to zero, or Road Temp is NaN
						continue; 
					if (bNeedObsSet)
					{
						oPavementSit = m_oObsMgr.getObsSet(m_nSurfaceStatus);
						oPrecipSit = m_oObsMgr.getObsSet(m_nPrecipSit);
						oVisibilitySit = m_oObsMgr.getObsSet(m_nVisibilitySit);
						bNeedObsSet = false;
					}
					//correct precipitation type based off of air temp
					if (nPrecipType == 4) //rain
					{
						if (dAirTemp < 1.5)
							nPrecipType = 6; //mix
					}
					else if (nPrecipType == 5) //snow
					{
						if (dAirTemp > 1.5)
							nPrecipType = 6; //mix
					}
					else if (nPrecipType == 6) //mix
					{
						if (dAirTemp < -2)
							nPrecipType = 5; //snow
						else if (dAirTemp > 2)
							nPrecipType = 4; //rain
					}
					//create infer obs
					if(nPrecipType == 4) //rain
					{
						//infer precip situation
						if (dPrecipRate > 0 && dPrecipRate <= m_dFORECAST_LIGHT_PRECIP_WINTER)
							nPrecipSit = m_nRAIN_SLIGHT;
						else if (dPrecipRate > m_dFORECAST_LIGHT_PRECIP_WINTER && dPrecipRate <= m_dFORECAST_MODERATE_PRECIP_WINTER)
							nPrecipSit = m_nRAIN_MODERATE;
						else
							nPrecipSit = m_nRAIN_HEAVY;
						//infer pavement situation
						if (dRoadTemp <= 0)
							nPavementSit = m_nICE_WARNING;
						else
							nPavementSit = m_nWET_PAVEMENT;
					}
					else if (nPrecipType == 6) //mix
					{
						//infer precip situation
						if (dPrecipRate > 0 && dPrecipRate <= m_dFORECAST_LIGHT_PRECIP_WINTER)
							nPrecipSit = m_nFROZEN_PRECIPITATION_SLIGHT;
						else if (dPrecipRate > m_dFORECAST_LIGHT_PRECIP_WINTER && dPrecipRate <= m_dFORECAST_MODERATE_PRECIP_WINTER)
							nPrecipSit = m_nFROZEN_PRECIPITATION_MODERATE;
						else
							nPrecipSit = m_nFROZEN_PRECIPITATION_HEAVY;
						//infer pavement situation
						if (dRoadTemp <= 0)
							nPavementSit = m_nICE_WARNING;
						else
							nPavementSit = m_nWET_PAVEMENT;
					}
					else if (nPrecipType == 5) //snow
					{
						//infer precip situation and visibility situation
						if (dPrecipRate > 0 && dPrecipRate <= m_dFORECAST_LIGHT_PRECIP_WINTER)
							nPrecipSit = m_nSNOW_SLIGHT;
						else if (dPrecipRate > m_dFORECAST_LIGHT_PRECIP_WINTER && dPrecipRate <= m_dFORECAST_MODERATE_PRECIP_WINTER)
						{
							nVisibilitySit = m_nBLOWING_SNOW;
							nPrecipSit = m_nSNOW_MODERATE;
						}
						else
						{
							nVisibilitySit = m_nBLOWING_SNOW;
							nPrecipSit = m_nSNOW_HEAVY;
						}
						//infer pavement situation
						if (dRoadTemp > - 1 && dRoadTemp < 1)
							nPavementSit = m_nICE_WARNING;
						else if (dRoadTemp >= 1 && dPrecipRate <= m_dFORECAST_MODERATE_PRECIP_WINTER) 
							nPavementSit = m_nWET_PAVEMENT;
						else if (dRoadTemp > 2)
							nPavementSit = m_nWET_PAVEMENT;
						else
							nPavementSit = m_nSNOW_WARNING;
					}

					if (oPavementSit != null && nPavementSit != 0)
						oPavementSit.addObs(0, 0, lForecastTime, lTimestamp, nLat, nLon, tElev, nPavementSit);
					if (oPrecipSit != null && nPrecipSit != 0)
						oPrecipSit.addObs(0, 0, lForecastTime, lTimestamp, nLat, nLon, tElev, nPrecipSit);
					if (oVisibilitySit != null && nVisibilitySit != 0)
						oVisibilitySit.addObs(0, 0, lForecastTime, lTimestamp, nLat, nLon, tElev, nVisibilitySit);
				}
			}

			// queue inferred observation set to next stage
			if (oPavementSit != null && !oPavementSit.isEmpty())
				m_oObsMgr.queue(oPavementSit);
			if (oPrecipSit != null && !oPrecipSit.isEmpty())
				m_oObsMgr.queue(oPrecipSit);			
			if (oVisibilitySit != null && !oVisibilitySit.isEmpty())
				m_oObsMgr.queue(oVisibilitySit);
		}
	}
	
	private static double calcRh(double dDewTemp, double dAirTemp)
	{
		double dPrVapT = 6.112 * Math.exp((17.67 * dAirTemp) / (dAirTemp + 243.5));
		double dPrVapD = 6.112 * Math.exp((17.67 * dDewTemp) / (dDewTemp + 243.5));
		return dPrVapD / dPrVapT;
	}
}
