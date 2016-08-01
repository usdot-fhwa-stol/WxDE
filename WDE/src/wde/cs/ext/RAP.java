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
		m_nObsTypes = new int[]{554, 587, 207, 2076, 2077, 2074, 2075};
		m_sObsTypes = new String[]{"Pressure_surface", "Precipitation_rate_surface", "precipType", "Categorical_Freezing_Rain_surface",
											"Categorical_Ice_Pellets_surface", "Categorical_Rain_surface", "Categorical_snow_surface"};
		m_sHrz = "x";
		m_sVrt = "y";
		m_sTime = "time";
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
	 * Finds the RAPGrid model value for an observation type by time and location.
	 *
	 * @param nObsTypeId	the observation type to lookup.
	 * @param lTimestamp	the timestamp of the observation.
	 * @param nLat				the latitude of the requested data.
	 * @param nLon				the longitude of the requested data.
	 * 
	 * @return	the RAPGrid model value for the requested observation type for the 
					specified time at the specified location.
	 */
	@Override
	public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon)
	{
		if (nObsTypeId == 207)
		{
			if (super.getReading(2076, lTimestamp, nLat, nLon) == 1)  //freezing rain
				return 6;
			else if (super.getReading(2077, lTimestamp, nLat, nLon) == 1) //ice pellets
				return 6;
			else if (super.getReading(2075, lTimestamp, nLat, nLon) == 1) //snow
				return 5;
			else if (super.getReading(2074, lTimestamp, nLat, nLon) == 1) //rain
				return 4;
			else //no precip
				return 3;
		}
		else
		{
			double dVal = super.getReading(nObsTypeId, lTimestamp, nLat, nLon);

			if (nObsTypeId == 554) // convert pressure Pa to mbar
				return dVal / 100.0;

			return dVal;
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
				"'.grib2&lev_surface=on&var_CFRZR=on&var_CICEP=on&var_CRAIN=on&var_CSNOW=on&var_PRATE=on&var_PRES=on&leftlon=0&rightlon=360&toplat=90&bottomlat=-90&dir=%2Frap.'yyyyMMdd");
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
				"'.grib2&lev_surface=on&var_CFRZR=on&var_CICEP=on&var_CRAIN=on&var_CSNOW=on&var_PRATE=on&var_PRES=on&leftlon=0&rightlon=360&toplat=90&bottomlat=-90&dir=%2Frap.'yyyyMMdd");
			loadFile(oTime);
			//update the time range by an hour
			m_nRange += 3600000;
			m_nDelay += 3600000;
		}
		//set the time range back to the default range
		m_nRange -= (3600000 * (m_nInitTime /3600));   //init time is in secs, want it in hours here
		m_nDelay -= (3600000 * (m_nInitTime /3600));
	}

	public static void main(String[] args)
	{
		RAP oRAP = RAP.getInstance();
		System.out.println(oRAP.getReading(207, System.currentTimeMillis() + 3600000 * 5, 24000000, -100000000));
	}
}
