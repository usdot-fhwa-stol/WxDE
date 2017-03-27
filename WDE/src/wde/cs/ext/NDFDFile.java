/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;


/**
 *  Abstract base class for all of the NDFD Files that are downloaded
 */
abstract class NDFDFile extends RemoteData
{
	SimpleDateFormat m_oDateForFile = new SimpleDateFormat("yyyyMMdd'-'HHmm");
	protected String m_sSrcFile;
	
	
	protected NDFDFile()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nDelay = -3900000; // collection five minutes after source file ready, file read at x-1:55
		m_nRange = 28800000; // NDFD forecast is produced hourly but used for up to 8 hours, good to use from x+1:00 to x+9:00
		m_nLimit = oConfig.getInt("limit", 1);  
		m_sHrz = "x";
		m_sVrt = "y";
		m_sTime = "time";
		//m_sBaseURL = "http://weather.noaa.gov/pub/SL.us008001/ST.opnl/DF.gr2/DC.ndfd/AR.conus/VP.001-003/";
		m_sBaseURL = "ftp://tgftp.nws.noaa.gov/SL.us008001/ST.opnl/DF.gr2/DC.ndfd/AR.conus/VP.001-003/";
		m_nOffset = 3300;
		m_nPeriod = 3600;
		m_oDateForFile.setTimeZone(Scheduler.UTC);
		m_nInitTime = oConfig.getInt("time", 3600);
		
	}
	
	/**
	 * Initialize the data files when the program is started by downloading the
	 * previous hour of NDFD files
	 */
	@Override
	protected final void init()
	{
		Calendar iCalendar = Scheduler.getNextPeriod(m_nOffset, m_nPeriod);
		iCalendar.add(Calendar.SECOND, -m_nInitTime);
		for (int i = 0; i < m_nInitTime / m_nPeriod; i++)
		{
		  loadFile(iCalendar);
		  iCalendar.add(Calendar.SECOND, m_nPeriod); 
		}
	}
	
	/**
	 * This method returns the source file name in the correct format for the 
	 * given time. In the case of NDFD files, time does not matter.
	 * 
	 * @param oTime    the requested time for the file
	 * @return         the formatted source file name
	 */
	@Override
	protected String getFilename(Calendar oTime)
	{
		return m_sSrcFile;
	}
	
	
	/**
	 * Used to determine the destination filename of the remote data
	 * 
	 * @param sScrFile  the source file name
	 * @param oTime     the desired time for the time
	 * @return          the destination file name
	 */
	@Override
	protected String getDestFilename(String sScrFile, Calendar oTime)
	{
		return m_sBaseDir +  m_oDateForFile.format(oTime.getTime()) + sScrFile;
	}
}
