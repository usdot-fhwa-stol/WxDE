/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;

/**
 * This class manages downloading RAP (Rapid Refresh) forecast files.
 * @author aaron.cherney
 */
public class RAP extends RemoteData implements Runnable
{

	public static final RAP g_oRAP = new RAP();
	private final SimpleDateFormat m_oSrcFile = new SimpleDateFormat();


	private RAP()
	{

		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nDelay = -300000; // collection five minutes after source file ready, file read at x-1:55
		m_nRange = 3900000; // RAP forecast is hourly, good to use from x:00 to x+1:00
		m_nLimit = oConfig.getInt("limit", 6);  // keep up to 6 hours of RAP files
		m_nObsTypes = new int[]{554, 587};
		m_sObsTypes = new String[]{"Pressure_surface", "Precipitation_rate_surface"};
		m_sHrz = "x";
		m_sVrt = "y";
		//m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/RAP/";
		m_sBaseDir = oConfig.getString("dir", "/run/shm/rap");
		m_sBaseURL = "http://nomads.ncep.noaa.gov/cgi-bin/filter_rap.pl?file=";
		m_oSrcFile.setTimeZone(Scheduler.UTC);
		m_nOffset = 3300;
		m_nPeriod = 3600;
		m_nInitTime = oConfig.getInt("time", 3600 * 3);
		init();
	}

	/**
	 * Return a reference to the singleton RAP model data cache
	 * @return  reference to RAP lookup instance
	 */
	public static RAP getInstance()
	{
		return g_oRAP;
	}

	
	/**
	 * This method returns the correct filename for the given time for RAP files
	 * 
	 * @param oNow  desired time of file
	 * @return formatted file name
	 */
	@Override
	protected String getFilename(Calendar oNow)
	{
		return m_oSrcFile.format(oNow.getTime());
	}

	
	/**
	 * This method returns the destination file name for RAP files.
	 * 
	 * @param sSrcFile   source file name
	 * @param oNow       desired time of file
	 * @return formatted destination file name
	 */
	@Override
	protected String getDestFilename(String sSrcFile, Calendar oNow)
	{
		String sDestFile = m_sBaseDir; // ignore intervening directories in path
		//remove non essential text from the end of the file name
		int nIndex = sSrcFile.indexOf("&");
		sSrcFile = sSrcFile.substring(0, nIndex);
		int nSepIndex = sSrcFile.lastIndexOf("/");
		if (nSepIndex >= 0)
		{
			return sDestFile + sSrcFile.substring(nSepIndex, nIndex); // extract the file name
		} 
		else
		{
			return sDestFile + sSrcFile; // local file name
		}
	}
	
	
	/**
	 * This method initializes data collection for RAP files by downloading the
	 * most recent files and scheduling future downloads.
	 */
	@Override
	protected final void init()
	{
		Calendar iCalendar = Scheduler.getNextPeriod(m_nOffset, m_nPeriod);
		iCalendar.add(Calendar.HOUR_OF_DAY, -1);  //the most recent file is one hour back
		Scheduler.getInstance().schedule(this, m_nOffset, m_nPeriod, true);
		//get all the RAP forecast files for the desired time range
		for (int i = 0; i < m_nInitTime / m_nPeriod; i++)
		{
			m_oSrcFile.applyPattern("'rap.t'HH'z.awp130pgrbf0'" + i + 
				"'.grib2&lev_surface=on&var_PRATE=on&var_PRES=on&leftlon=0&rightlon=360&toplat=90&bottomlat=-90&dir=%2Frap.'yyyyMMdd");
			loadFile(iCalendar);
			//update the time range by an hour
			m_nRange += 3600000;   
			m_nDelay += 3600000;
		}
		//set the time range back to the default range
		m_nRange -= 3600000 * 6;
		m_nDelay -= 3600000 * 6;
	}

	
	/**
	 * Downloads the most recent RAP files
	 */
	@Override
	public void run()
	{
		Calendar oTime = new GregorianCalendar(Scheduler.UTC);
		//get all the RAP forecast files for the desired time range
		for (int i = 0; i < m_nInitTime / m_nPeriod; i++)
		{
			m_oSrcFile.applyPattern("'rap.t'HH'z.awp130pgrbf0'" + i + 
				"'.grib2&lev_surface=on&var_PRATE=on&var_PRES=on&leftlon=0&rightlon=360&toplat=90&bottomlat=-90&dir=%2Frap.'yyyyMMdd");
			loadFile(oTime);
			//update the time range by an hour
			m_nRange += 3600000;
			m_nDelay += 3600000;
		}
		//set the time range back to the default range
		m_nRange -= 3600000 * 6;
		m_nDelay -= 3600000 * 6;
	}

	public static void main(String[] args)
	{
		RAP oRAP = RAP.getInstance();
		System.out.println(oRAP.getReading(587, System.currentTimeMillis() + 3600000 * 5, 24000000, -100000000));
	}
}
