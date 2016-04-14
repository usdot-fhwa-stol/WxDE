package wde.comp;

import wde.WDEMgr;
import wde.cs.ext.Radar;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
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
	private enum PRECIP_TYPE {NO_PRECIP, RAIN, MIX, SNOW};
	/**
	 * observation type identifier for precipitation presence
	 */
	private final int m_nPrecipYesNo;
	/**
	 * list of accepted observation types
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
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code VdtInference}, of which there should be 
	 * only one.
	 * </p>
	 */
	InferObs()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		String[] sObsTypes = oConfig.getString("accept", "5733,2001180").split(",");
		m_nObsTypes = new int[sObsTypes.length]; // convert and copy obs type ids
		for (int nIndex = 0; nIndex < sObsTypes.length; nIndex++)
			m_nObsTypes[nIndex] = Integer.parseInt(sObsTypes[nIndex]);	

		m_nPrecipYesNo = Integer.parseInt(oConfig.getString("essPrecipYesNo", "586"));
		int nWiperStatus = Integer.parseInt(oConfig.getString("canWiperStatus", "2000001"));
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
		boolean bFound = false;
		while (!bFound && nIndex-- > 0)
			bFound = (iObsSet.getObsType() == m_nObsTypes[nIndex]);
		
		if (bFound) // trigger on air temperature observations
		{
			// request inferred observation sets
			nIndex = iObsSet.size();
			while (nIndex-- > 0)
			{
				IObs iObs = iObsSet.get(nIndex);
				ISensor iSensor = m_oSensorDao.getSensor(iObs.getSensorId());
				IPlatform iPlatform = m_oPlatformDao.getPlatform(iSensor.getPlatformId());
				if (iPlatform.getCategory() == 'M') // only process mobile observations
					process(iObs);
			}
			// queue inferred observation sets to next stage
		}
		
		m_oWdeMgr.queue(iObsSet); // always requeue received observation set
	}

	private void process(IObs iObs)
	{
		double dRefl = m_oRadar.getReading(0, iObs.getObsTimeLong(),
			iObs.getLatitude(), iObs.getLongitude()); // obs type id is 0 for unknown
		if (Double.isNaN(dRefl))
			return; // no assessment without available radar value
	
		if (dRefl == 0.0)
			System.out.println();
	}
}
