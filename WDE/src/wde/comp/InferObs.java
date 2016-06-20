package wde.comp;

import java.util.ArrayList;
import wde.WDEMgr;
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
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;

public class InferObs extends AsyncQ<IObsSet>
{
	private final int m_nAirTemp;
	private final int m_nDewTemp;
	private final int m_nRh;
	private final int m_nWindSpd;
	private final int m_nPrecipSit;
	private final int m_nVisibilitySit;
	private final int m_nPavementSit;
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
		String[] sObsTypes = oConfig.getString("accept", "5733,2001180").split(",");
		m_nObsTypes = new int[sObsTypes.length]; // convert and copy obs type ids
		for (int nIndex = 0; nIndex < sObsTypes.length; nIndex++)
			m_nObsTypes[nIndex] = Integer.parseInt(sObsTypes[nIndex]);	

		// input observation types
		m_nAirTemp = Integer.parseInt(oConfig.getString("essAirTemperature", "5733"));;
		m_nDewTemp = Integer.parseInt(oConfig.getString("essDewpointTemp", "575"));;
		m_nRh = Integer.parseInt(oConfig.getString("essPrecipSituation", "?"));;
		m_nWindSpd = Integer.parseInt(oConfig.getString("windSensorAvgSpeed", "56104"));;
		// output observation types
		m_nPrecipSit = Integer.parseInt(oConfig.getString("essPrecipSituation", "589"));
		m_nVisibilitySit = Integer.parseInt(oConfig.getString("essVisibilitySituation", "5102"));
		m_nPavementSit = Integer.parseInt(oConfig.getString("essMobileObservationPavement", "5123"));
//		int m_nWiperStatus = Integer.parseInt(oConfig.getString("canWiperStatus", "2000001"));
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
		int nIndex = m_nObsTypes.length;
		boolean bFound = false; // filter obs types other than air temperatures
		while (!bFound && nIndex-- > 0)
			bFound = (iObsSet.getObsType() == m_nObsTypes[nIndex]);
		
		if (bFound)
		{
			ArrayList<RoadObs> oRoads = new ArrayList();
			nIndex = iObsSet.size();
			while (nIndex-- > 0) // only process mobile observations
			{
				IObs iObs = iObsSet.get(nIndex);
				ISensor iSensor = m_oSensorDao.getSensor(iObs.getSensorId());
				IPlatform iPlatform = m_oPlatformDao.getPlatform(iSensor.getPlatformId());
				if (iPlatform.getCategory() == 'M')
				{
					Road oRoad = m_oRoads.getLink(1000, iObs.getLongitude(), 
						iObs.getLatitude());
					if (oRoad != null) // filter for existing roads and radar value
					{
						double dRefl = m_oRadar.getReading(0, iObs.getObsTimeLong(),
							iObs.getLatitude(), iObs.getLongitude()); // obs type id is 0 for unknown
						if (!Double.isNaN(dRefl) && !containsRoad(oRoads, oRoad))
							oRoads.add(new RoadObs(oRoad, dRefl, iObs.getObsTimeLong()));
					}
				}
			}

			for (RoadObs oRoadObs : oRoads)
			{
				if (m_nAirTemp > 2)
				{
					
				}
				else if (m_nAirTemp < -2)
				{
					
				}
				else
				{
					
				}
			}
			// request inferred observation sets
			// queue inferred observation sets to next stage
		}
		
		m_oWdeMgr.queue(iObsSet); // always requeue received observation set
	}


	private static boolean containsRoad(ArrayList<RoadObs> oRoads, Road oRoad)
	{
		boolean bFound = false;
		int nIndex = oRoads.size();
		while (!bFound && nIndex-- > 0) // matching object reference is okay here
			bFound = oRoads.get(nIndex).m_oRoad == oRoad;

		return bFound;
	}


	private static double calcRh(double dDewTemp, double dAirTemp)
	{
		double dPrVapT = 6.112 * Math.exp((17.67 * dAirTemp) / (dAirTemp + 243.5));
		double dPrVapD = 6.112 * Math.exp((17.67 * dDewTemp) / (dDewTemp + 243.5));
		return dPrVapD / dPrVapT;
	}


	private void process(RoadObs oRoadObs)
	{
	}
}
