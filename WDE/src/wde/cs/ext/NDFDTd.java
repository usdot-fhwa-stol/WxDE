/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import wde.util.Config;
import wde.util.ConfigSvc;

/**
 * This class represents the NDFD file that contains data on the dew point 
 */
public class NDFDTd extends NDFDFile
{
	NDFDTd()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nObsTypes = new int[]{575};
		m_sObsTypes = new String[]{"Dewpoint_temperature_surface"};
		m_sBaseDir = oConfig.getString("dir", "/run/shm/ndfd/");
		m_sSrcFile = "ds.td.bin";
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
		NDFDTd oNDFDTd = new NDFDTd();
		System.out.println(oNDFDTd.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}