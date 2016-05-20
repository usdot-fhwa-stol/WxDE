/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import wde.util.Scheduler;



abstract class NDFD extends RemoteGrid
{
	protected NDFD()
	{
	}
	
	public static void scheduleNDFDFiles()
	{
		NDFDSky oNDFDSky = new NDFDSky();
		NDFDTd oNDFDTd = new NDFDTd();
		NDFDTemp oNDFDTemp = new NDFDTemp();
		NDFDWspd oNDFDWspd = new NDFDWspd();
		Scheduler.getInstance().schedule(oNDFDSky, 0, 120, true);
		Scheduler.getInstance().schedule(oNDFDTd, 30, 120, true);
		Scheduler.getInstance().schedule(oNDFDTemp, 60, 120, true);
		Scheduler.getInstance().schedule(oNDFDWspd, 90, 120, true);
	}
	
	public static void main(String sArgs[])
	{
		scheduleNDFDFiles();
	}
}