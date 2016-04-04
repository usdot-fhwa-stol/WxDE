package wde.cs.ext;


import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;


/**
 * Real-Time Mesoscale Analysis. This singleton class downloads hourly RTMA 
 * files from the National Weather Service and provides a lookup method to 
 * retrieve the model value for the supported observation types that are then 
 * used to quality check the measured observation.
 */
public final class RTMA extends RemoteGrid
{
	private static final RTMA g_oRTMA = new RTMA();

	private final SimpleDateFormat m_oSrcFile = new SimpleDateFormat(
		"'rtma2p5.'yyyyMMdd'/rtma2p5.t'HH'z.2dvarges_ndfd.grb2'");


	/**
	 * <b> Default Private Constructor </b>
	 * <p>
	 * Creates a new instance of RTMA upon class loading. Client components 
	 * obtain a singleton reference through the getInstance method.
	 * </p>
	 */
	private RTMA()
	{
		m_nObsTypes = new int[]{575, 554, 5733, 5101, 56105, 56108, 56104};
		m_sObsTypes = new String[]
		{
			"Dewpoint_temperature_height_above_ground", "Pressure_surface", 
			"Temperature_height_above_ground", "Visibility_surface", 
			"Wind_direction_from_which_blowing_height_above_ground", 
			"Wind_speed_gust_height_above_ground", "Wind_speed_height_above_ground"
		};
		m_sHrz = "x";
		m_sVrt = "y";
		m_sBaseDir = "C:/Users/bryan.krueger/"; // should be configured
		m_sBaseURL = "ftp://ftp.ncep.noaa.gov/pub/data/nccf/com/rtma/prod/";

		m_oSrcFile.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		run(); // manually initialize first run, then set schedule
//		Scheduler.getInstance().schedule(this, 60 * 55, 3600, true);
	}


	/**
	 * Returns a reference to singleton RTMA model data cache.
	 *
	 * @return reference to RTMA instance.
	 */
	public static RTMA getInstance()
	{
		return g_oRTMA;
	}


	/**
	 * RTMA uses this method to determine remote filename based on current time.
	 *
	 * @param oNow	timestamp Date object used for time-based dynamic URLs
	 * 
	 * @return the name of the remote data file.
	 */
	@Override
	protected String getFilename(Date oNow)
	{
		try
		{
			return m_oSrcFile.format(oNow);
		}
		catch (Exception oException)
		{
		}

		return null;
	}


	/**
	 * Finds the RTMA model value for an observation type by time and location.
	 *
	 * @param nObsTypeId	the observation type to lookup.
	 * @param lTimestamp	the timestamp of the observation.
	 * @param nLat				the latitude of the requested data.
	 * @param nLon				the longitude of the requested data.
	 * 
	 * @return	the RTMA model value for the requested observation type for the 
	 *					specified time at the specified location.
	 */
	@Override
	public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon)
	{
		double dVal = super.getReading(nObsTypeId, lTimestamp, nLat, nLon);
		if (Double.isNaN(dVal)) // pass through for no observation found
			return Double.NaN;

		if (nObsTypeId == 554) // convert pressure Pa to mbar
			return dVal / 100.0;

		if (nObsTypeId == 5733) // convert temperature K to C
			return dVal - 273.15;

		return dVal; // no conversion necessary for other observation types
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		RTMA oRTMA = RTMA.getInstance();
		System.out.println(oRTMA.getReading(5733, System.currentTimeMillis(), 43000000, -94000000));
	}
}
