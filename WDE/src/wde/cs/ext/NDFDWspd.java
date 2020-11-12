/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import wde.util.Config;
import wde.util.ConfigSvc;

/**
 * This class represents the NDFD file that contains data on the wind speed 
 */
public class NDFDWspd extends NDFDFile
{
	NDFDWspd()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nObsTypes = new int[]{56104};
		m_sObsTypes = new String[]{"Wind_speed_surface_above_ground"};
		m_sBaseDir = oConfig.getString("dir", "/run/shm/ndfd/");
		m_sSrcFile = "ds.wspd.bin";
		init();
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		NDFDWspd oNDFDWspd = new NDFDWspd();
		System.out.println(oNDFDWspd.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}