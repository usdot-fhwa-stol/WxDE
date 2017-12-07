package wde.cs.ext;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.TimeZone;

import wde.util.Config;
import wde.util.ConfigSvc;


/**
 Real-Time Mesoscale Analysis. This singleton class downloads hourly RTMA 
 files from the National Weather Service and provides a lookup method to 
 retrieve the model value for the supported observation types that are then 
 used to quality check the measured observation.
 */
public final class RTMA extends RemoteGrid
{
	private static final RTMA g_oRTMA = new RTMA();
	private final SimpleDateFormat m_oSrcFile = new SimpleDateFormat(
		"'rtma2p5.'yyyyMMdd'/rtma2p5.t'HH'z.2dvarges_ndfd.grb2'");


	/**
	 * <b> Default package private constructor </b>
	 * <p>
	 * Creates a new instance of RTMA. It initializes NetCDF observation 
	 * type reference names to system observation type mapping, latitude and 
	 * longitude name mapping, and remote source and local storage directory.
	 * </p>
	 */
	private RTMA()
	{
		
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		
		m_nDelay = -300000; // collection five minutes after source file ready, file read at x-1:55
		m_nRange = 3900000; // RTMA forecast is hourly, good to use from x:00 to x+1:00
		m_nLimit = oConfig.getInt("limit", 12);  // keep up 12 hours of RTMA files
		m_nObsTypes = new int[]{575, 554, 5733, 5101, 56105, 56108, 56104, 593};
		m_sObsTypes = new String[]
		{
			"Dewpoint_temperature_height_above_ground", "Pressure_surface", 
			"Temperature_height_above_ground", "Visibility_surface", 
			"Wind_direction_from_which_blowing_height_above_ground", 
			"Wind_speed_gust_height_above_ground", "Wind_speed_height_above_ground",
			"Total_cloud_cover_entire_atmosphere_single_layer"
		};
		m_sHrz = "x";
		m_sVrt = "y";
		m_sTime = "time";
		//m_sBaseDir = "C:/Users/aaron.cherney/TestFiles/RTMA/";
		m_sBaseDir = oConfig.getString("dir", "/run/shm/rtma");
		m_sBaseURL = "ftp://ftp.ncep.noaa.gov/pub/data/nccf/com/rtma/prod/";
		m_oSrcFile.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		m_nOffset = 3300;
		m_nPeriod = 3600;
		m_nInitTime = oConfig.getInt("time", 3600 * 3);
		m_nRetryMax = oConfig.getInt("max", 4);
		m_nRetryInterval = oConfig.getInt("retry", 600000);
		m_oRetries = new ArrayList();
		init();
	}


	/**
	 * Returns a reference to singleton RTMA model data cache.
	 *
	 * @return reference to RTMA data lookup instance.
	 */
	public static RTMA getInstance()
	{
		return g_oRTMA;
	}


	/**
	 * RTMAGrid uses this method to determine remote filename based on current time.
	 *
	 * @param oNow	Calendar object used for time-based dynamic URLs
	 * 
	 * @return the name of the remote data file.
	 */
	@Override
	protected String getFilename(Calendar oNow)
	{
		if (oNow.get(Calendar.MINUTE) < 52)
			oNow.add(Calendar.HOUR, -1); // backup one hour when starting late

		oNow.set(Calendar.MILLISECOND, 0); // floor to the nearest minute
		oNow.set(Calendar.SECOND, 0);
		oNow.set(Calendar.MINUTE, 55); // set to normal scheduled time

		try
		{
			return m_oSrcFile.format(oNow.getTime());
		}
		catch (Exception oException)
		{
		}

		return null;
	}


	/**
	 * Finds the RTMAGrid model value for an observation type by time and location.
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
		if (Double.isNaN(dVal)) // pass through for no observation found
			return Double.NaN;

		if (nObsTypeId == 554) // convert pressure Pa to mbar
			return dVal / 100.0;

		if (nObsTypeId == 5733 || nObsTypeId == 575) // convert temperature K to C
			return dVal - 273.15;
		
		if (nObsTypeId == 593) // convert cloud coverage from percent to METRo "octal"
			return Math.round(dVal/12.5);

		return dVal; // no conversion necessary for other observation types
	}
	
	
	/**
	 * Used to determine the destination filename of the remote data
	 * 
	 * @param sSrcFile  the source file name
	 * @param oTime     the desired time for the time
	 * @return          the destination file name
	 */
	@Override
	protected String getDestFilename(String sSrcFile, Calendar oTime)
	{
		String sDestFile = m_sBaseDir; // ignore intervening directories in path
		int nSepIndex = sSrcFile.lastIndexOf("/");
		if (nSepIndex >= 0)
			return sDestFile + sSrcFile.substring(nSepIndex); // extract the file name
		else
			return sDestFile + sSrcFile; // local file name
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		RTMA oRTMA = RTMA.getInstance();
		long lTime = System.currentTimeMillis();
		for (int i = 0; i < 5; i++)
		{
			System.out.println(oRTMA.getReading(575, lTime - i * 3600000, 43000000, -94000000));
			System.out.println(oRTMA.getReading(554, lTime - i * 3600000, 43000000, -94000000));
			System.out.println(oRTMA.getReading(5733, lTime - i * 3600000, 43000000, -94000000));
			System.out.println(oRTMA.getReading(5101, lTime - i * 3600000, 43000000, -94000000));
			System.out.println(oRTMA.getReading(56105, lTime - i * 3600000, 43000000, -94000000));
			System.out.println(oRTMA.getReading(56108, lTime - i * 3600000, 43000000, -94000000));
			System.out.println(oRTMA.getReading(56104, lTime - i * 3600000, 43000000, -94000000));
		}
	//	System.out.println(oRTMA.getReading(5733, System.currentTimeMillis(), 43000000, -94000000));
	System.exit(0);
	}
}
