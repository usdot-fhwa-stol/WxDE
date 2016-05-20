/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import wde.util.Scheduler;
/**
 *
 * @author aaron.cherney
 */
abstract class NDFDFile extends RemoteData implements Runnable
{
	SimpleDateFormat m_oDateForFile = new SimpleDateFormat("yyyyMMdd'-'HHmm");
	protected String m_sSrcFile;
	
	
	protected NDFDFile()
	{
		m_nDelay = -300000; // collection five minutes after source file ready, file read at x-1:55
		m_nRange = 3900000; // NDFD forecast is hourly, good to use from x:00 to x+1:00
		m_nLimit = 12; // keep up to NDFD files
		m_sHrz = "lon";
		m_sVrt = "lat";
		m_nObsTypes = new int[]{0};
		m_sBaseURL = "http://weather.noaa.gov/pub/SL.us008001/ST.opnl/DF.gr2/DC.ndfd/AR.conus/VP.001-003/";
		m_nOffset = 3300;
		m_nPeriod = 3600;
		m_oDateForFile.setTimeZone(Scheduler.UTC);
		m_nSecsBack = 3600;
	}
	
	@Override
	protected final void init()
	{
		Calendar iCalendar = Scheduler.getNextPeriod(m_nOffset, m_nPeriod);
		iCalendar.add(Calendar.SECOND, -m_nSecsBack);
		for (int i = 0; i < m_nSecsBack / m_nPeriod; i++)
		{
		  loadFile(iCalendar);
		  iCalendar.add(Calendar.SECOND, m_nPeriod); 
		}
	}
	
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
	
	
	@Override
	public void run()
	{
		loadFile(new GregorianCalendar(Scheduler.UTC));
	}
}
