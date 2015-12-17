package wde.qchs;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ucar.nc2.dt.GridDatatype;


public class Radar extends RemoteGrid
{
	private static final Radar g_oRadar = new Radar();

	private final Matcher m_oMatcher = Pattern.compile(
		"(MRMS_MergedBaseReflectivityQC_00\\.00_[0-9]{8}-[0-9]{6}\\.grib2\\.gz)")
		.matcher("");

	private Radar()
	{
		m_nObsTypes = new int[]{0};
		m_sObsTypes = new String[]{"MergedBaseReflectivityQC_altitude_above_msl"};
		m_sHrz = "lon";
		m_sVrt = "lat";
		m_sBaseDir = "C:/Users/bryan.krueger/"; // should be configured
		m_sBaseURL = "http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/";

		run(); // manually initialize first run, then set schedule
//		Scheduler.getInstance().schedule(this, 60 * 55, 3600, true);
	}


	public static Radar getInstance()
	{
		return g_oRadar;
	}


	/**
	 * This method is used to determine the remote filename. It first downloads 
	 * the base URL as the file index and then iterates to find the latest name.
	 *
	 * @param oNow	timestamp Date object used for time-based dynamic URLs
	 * 
	 * @return the name of the remote data file.
	 */
	@Override
	protected String getFilename(Date oNow)
	{
		String sFilename = null;
		StringBuilder sIndex = new StringBuilder();
		try
		{
			BufferedInputStream oIn = new BufferedInputStream(
				new URL(m_sBaseURL).openStream());
			int nByte; // copy remote file index to buffer
			while ((nByte = oIn.read()) >= 0)
				sIndex.append((char)nByte);
			oIn.close();

			m_oMatcher.reset(sIndex);
			while (m_oMatcher.find()) // desired file name should be last match
				sFilename = m_oMatcher.group(1);
		}
		catch (Exception oException)
		{
		}

		return sFilename;
	}


	/**
	 * Retrieves the current radar grid data.
	 *
	 * @param nObsTypeId	the observation type identifier used to find grid data.
	 * 
	 * @return the grid data for the variable specified by observation type.
	 */
	@Override
	protected GridDatatype getGridByObs(int nObsTypeId)
	{
		return super.getGridByObs(0); // observation type is ignored for radar
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		Radar oRadar = Radar.getInstance();
		System.out.println(oRadar.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}
