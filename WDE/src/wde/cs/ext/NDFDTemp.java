/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import wde.util.Config;
import wde.util.ConfigSvc;

/**
 * This class represents the NDFD file that contains data on the temperature
 */
public class NDFDTemp extends NDFDFile
{	
	NDFDTemp()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nObsTypes = new int[]{5733};
		m_sObsTypes = new String[]{"Temperature_surface"};
		m_sBaseDir = oConfig.getString("dir", "/run/shm/ndfd/");
		m_sSrcFile = "ds.temp.bin";
		init();
	}
	
	@Override
	public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon)
	{
		return super.getReading(nObsTypeId, lTimestamp, nLat, nLon) - 273.15; 
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDTemp oNDFDTemp = new NDFDTemp();
		System.out.println(oNDFDTemp.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}