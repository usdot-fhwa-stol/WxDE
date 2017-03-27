/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import javax.servlet.http.HttpServlet;
import wde.WDEMgr;
import wde.comp.MetroMgr;


/**
 *
 * @author aaron.cherney
 */
public class ExtStarter extends HttpServlet implements Runnable
{
	public ExtStarter()
	{
	}
	
	
	/**
	 * This method initializes the external data collection services
	 */
	@Override
	public void init()
	{
		WDEMgr.getInstance();
		new Thread(this).start();

	}
	
	/**
	 * This method gets the singleton instance of external data collection 
	 * services as well as Metro Manager and Roads
	 */
	@Override
	public void run()
	{		
		WDEMgr.getInstance().startup();
		MetroMgr.getInstance(); // Roads are also loaded by MetroMgr
		NDFD.getInstance();
		Radar.getInstance();
		RAP.getInstance();
		RTMA.getInstance();
	}
}
