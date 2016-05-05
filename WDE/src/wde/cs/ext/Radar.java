package wde.cs.ext;

import java.io.BufferedInputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import wde.util.Scheduler;


public class Radar extends RemoteGrid
{
	private static final Radar g_oRadar = new Radar();

	private final Matcher m_oMatcher = Pattern.compile(
		"(MRMS_MergedBaseReflectivityQC_00\\.00_[0-9]{8}-[0-9]{6}\\.grib2\\.gz)")
		.matcher("");

	private Radar()
	{
		m_nDelay = 90000; // collection 90 seconds after source file ready
		m_nRange = 120000; // radar files are updated every two minutes
		m_nLimit = 3; // keep up to thirty radar files
		m_nObsTypes = new int[]{0};
		m_sObsTypes = new String[]{"MergedBaseReflectivityQC_altitude_above_msl"};
		m_sHrz = "lon";
		m_sVrt = "lat";
		m_sBaseURL = "http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/";

		Scheduler.getInstance().schedule(this, 90, 120, true);
	}


	public static Radar getInstance()
	{
		return g_oRadar;
	}


	/**
	 * This method is used to determine the remote filename. It first downloads 
	 * the base URL as the file index and then iterates to find the latest name.
	 *
	 * @param oNow	Calendar object used for time-based dynamic URLs
	 * 
	 * @return the name of the remote data file.
	 */
	@Override
	protected String getFilename(Calendar oNow)
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


	public static void main(String[] sArgs)
		throws Exception
	{
		Radar oRadar = Radar.getInstance();
		Thread.sleep(600000);
		System.out.println(oRadar.getReading(0, System.currentTimeMillis(), 43000000, -94000000));
	}
}