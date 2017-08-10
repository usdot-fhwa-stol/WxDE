package wde.cs.imo;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.TimeZone;


public class MnDot implements Runnable
{
	private final int m_nDelay;
	private final int m_nTz;
	private final String m_sRoot;
	private final SimpleDateFormat m_oUrlFormat;
	private final SimpleDateFormat m_oDateFormat = new SimpleDateFormat("yyyyMMdd");
	private final SimpleDateFormat m_oDayFormat = new SimpleDateFormat("'.'yyyyMMdd'.'HHmm'.txt'");
	private final HashMap<String, FileWriter> m_oFiles = new HashMap<>();


	public MnDot(String[] sArgs)
		throws Exception
	{
		m_nTz = Integer.parseInt(sArgs[0]); // timezone offset in hours
		m_nDelay = Integer.parseInt(sArgs[1]); // collection delay in minutes
		m_sRoot = sArgs[2]; // root directory to save files
		m_oUrlFormat = new SimpleDateFormat(sArgs[3]);
	}


	private FileWriter getFileWriter(String sPrefix, Calendar oNow)
		throws Exception
	{
		FileWriter oWriter = m_oFiles.get(sPrefix);
		if (oWriter == null)
		{
			StringBuilder sFilename = new StringBuilder(m_sRoot);
			if (!m_sRoot.endsWith("/"))
				sFilename.append("/"); // correct missing path separator
			sFilename.append(sPrefix);
			sFilename.append("/");
			sFilename.append(m_oDateFormat.format(oNow.getTime()));
			new File(sFilename.toString()).mkdirs();

			sFilename.append("/");
			sFilename.append(sPrefix);
			sFilename.append(m_oDayFormat.format(oNow.getTime()));

			oWriter = new FileWriter(sFilename.toString());
			m_oFiles.put(sPrefix, oWriter);
		}
		return oWriter;
	}


	@Override
	public void run()
	{
		Calendar oNow = new GregorianCalendar();
		oNow.add(Calendar.MINUTE, m_nDelay); // adjust for collection delay
		oNow.add(Calendar.HOUR, m_nTz); // and time zone offset
		try
		{
			URL oUrl = new URL(m_oUrlFormat.format(oNow.getTime()));
			BufferedReader oReader = new BufferedReader(
				new InputStreamReader(oUrl.openStream()));

			oNow.add(Calendar.HOUR, -m_nTz); // adjust the time zone back to UTC
			String sLine;
			while ((sLine = oReader.readLine()) != null)
			{
				if (sLine.length() > 0 && sLine.charAt(0) == '>')
				{
					String sPrefix = sLine.substring(1, sLine.indexOf(':'));
					FileWriter oWriter = getFileWriter(sPrefix, oNow);
					oWriter.write(sLine);
					oWriter.write("\n"); // don't forget the newline
				}
			}

			Iterator<FileWriter> oIt = m_oFiles.values().iterator();
			while (oIt.hasNext()) // close file writers
				oIt.next().close();		
		}
		catch (Exception oEx)
		{
			oEx.printStackTrace();
		}
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		new MnDot(sArgs).run();
	}
}
