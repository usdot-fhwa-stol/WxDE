package wde.cs.ext;

import java.io.BufferedInputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;


/**
 * This singleton class downloads Radar files every 2 minutes from the 
 * National Oceanic and Atmospheric Administration
 */
public class Radar extends RemoteGrid 
{
	private static final Radar g_oRadar = new Radar();
	SimpleDateFormat m_oNeededFile = new SimpleDateFormat("yyyyMMdd'-'HHmm");
	

	private Radar()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		
		m_nDelay = 60000; // collection 180 seconds after source file ready, file is good for use for the next 2 minute interval
		m_nRange = 180000; // radar files are updated every two minutes (ex. file for 3:10 is read at 3:13 and is good for 3:12 - 3:14)
		m_nLimit = oConfig.getInt("limit", 360);  // keep up 3 hours of NDFD files
		m_nObsTypes = new int[]{0};
		m_sObsTypes = new String[]{"MergedBaseReflectivityQC_altitude_above_msl"};
		m_sHrz = "lon";
		m_sVrt = "lat";
		m_sBaseDir = oConfig.getString("dir", "/run/shm/radar/");
		m_sBaseURL = "http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/";
		m_nOffset = 60;
		m_nPeriod = 120;
		m_oNeededFile.setTimeZone(Scheduler.UTC);
		m_nSecsBack = oConfig.getInt("SecsBack", 3600 * 3);
		init();
	}

	/**
	 * Returns a reference to singleton Radar model data cache.
	 *
	 * @return reference to Radar data lookup instance.
	 */
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
		StringBuilder sIndex = new StringBuilder();
		try
		{
			BufferedInputStream oIn = new BufferedInputStream(
				new URL(m_sBaseURL).openStream());
			int nByte; // copy remote file index to buffer
			while ((nByte = oIn.read()) >= 0)
				sIndex.append((char)nByte);
			oIn.close();

			oNow.add(Calendar.SECOND, (-(m_nPeriod + m_nOffset)));
			int nTimeStampIndex = sIndex.indexOf(m_oNeededFile.format(oNow.getTime()));
			oNow.add(Calendar.SECOND, (m_nPeriod + m_nOffset));
			if (nTimeStampIndex < 0)
			{
				oNow.add(Calendar.SECOND, (-(m_nPeriod + m_nOffset + 60)));
				nTimeStampIndex = sIndex.indexOf(m_oNeededFile.format(oNow.getTime()));
				oNow.add(Calendar.SECOND, (m_nPeriod + m_nOffset + 60));
				if(nTimeStampIndex < 0)
					return null;
			}
			int nFileExtIndex = sIndex.indexOf(".gz", nTimeStampIndex);
			int nFileIndex = sIndex.lastIndexOf("\"", nTimeStampIndex);
			return sIndex.substring(++nFileIndex, nFileExtIndex + ".gz".length());
		}
		catch (Exception oException)
		{
		}
		return null;
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
		String sDestFile = m_sBaseDir; // ignore intervening directories in path
		int nSepIndex = sScrFile.lastIndexOf("/");
		if (nSepIndex >= 0)
			return sDestFile + sScrFile.substring(nSepIndex); // extract the file name
		else
			return sDestFile + sScrFile; // local file name
	}
	
	
	public static void main(String[] sArgs)
		throws Exception
	{
		Radar oRadar = Radar.getInstance();
		for(int i = 2281; i < 2285; i++)
			for(int j = 1010; j < 1020; j++)
				System.out.println(oRadar.getReading(0, System.currentTimeMillis(), -10000 * i + 54995000, 10000 * j - 129995000));
	}
}
