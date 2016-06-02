/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import wde.util.Config;
import wde.util.ConfigSvc;

/**
 * This class represents the NDFD file that contains data on the cloud coverage 
 */
public class NDFDSky extends NDFDFile
{
	NDFDSky()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nObsTypes = new int[]{593};
		m_sObsTypes = new String[]{"Total_cloud_cover_surface"};
		//m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/NDFD/sky/";
		
		m_sBaseDir = oConfig.getString("dir", "/run/shm/ndfd/");
		m_sSrcFile = "ds.sky.bin";
		init();
	}
	
	/**
	 * Finds the NDFD model value for Cloud Coverage by time and location, then
	 * converts it to an "octal" value for METRo.
	 *
	 * @param nObsTypeId	the observation type to lookup.
	 * @param lTimestamp	the timestamp of the observation.
	 * @param nLat				the latitude of the requested data.
	 * @param nLon				the longitude of the requested data.
	 * 
	 * @return	the RTMAGrid model value for the requested observation type for the 
					specified time at the specified location.
	 */
	@Override
	public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon)
	{
		double dVal = super.getReading(nObsTypeId, lTimestamp, nLat, nLon);

		return Math.round(dVal/12.5); 
	}
	
	
	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDSky oNDFDSky = new NDFDSky();
		System.out.println(oNDFDSky.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}