package wde.cs.imo;


import java.io.File;
import java.io.FileWriter;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;


public class MiDot
{
	public static void main(String[] sArgs)
		throws Exception
	{
		String sPlatformId;
		String sDate = null;
		String sTime = null;
		String sAirTemp = null;
		String sPressure = null;
		String sLat = null;
		String sLon = null;
		String sAlt = null;
		SimpleDateFormat oDateFormat = new SimpleDateFormat("ddMMyyyy'.csv'");
		GregorianCalendar oCalendar = new GregorianCalendar();

		int nChar = sArgs.length; // fix missing path terminators
		while (nChar-- > 0)
		{
			if (!sArgs[nChar].endsWith("/"))
				sArgs[nChar] += "/";
		}


		URL oUrl = new URL(sArgs[0]); // get list of remote files
		InputStream iInputStream = oUrl.openConnection().getInputStream();

		StringBuilder sBuffer = new StringBuilder();
		while ((nChar = iInputStream.read()) >= 0)
			sBuffer.append((char)nChar);
		
		iInputStream.close();


		int nStart = 0;
		while ((nStart = sBuffer.indexOf("href=\"", nStart)) >= 0)
		{
			nStart += 6;
			int nEnd = sBuffer.indexOf("\"", nStart);
			String sFilename = sBuffer.substring(nStart, nEnd);
			nStart = nEnd;

			if (!sFilename.endsWith(".csv"))
				continue; // skip non-csv files, i.e. parent directory/jpeg
			
			int nStartDir = sFilename.indexOf("_"); // day-of-month directory
			sPlatformId = sFilename.substring(0, nStartDir); // set default id
			int nEndDir = sFilename.indexOf("_", ++nStartDir);
			String sDirMid = sFilename.substring(nStartDir, nEndDir);

			// year-month directory
			String sDirTop = sDirMid.substring(4, 8) + sDirMid.substring(2, 4);

			try
			{
				File oDir = new File(sArgs[1] + sDirTop + "/" + sDirMid);
				oDir.mkdirs(); // verify storage location exists

				File oFile = new File(oDir.getPath() + "/" + sFilename);
				if (oFile.exists())
					continue; // ignore previously downloaded files
				
				oUrl = new URL(sArgs[0] + sFilename);
				BufferedReader oReader = new BufferedReader(
					new InputStreamReader(oUrl.openConnection().getInputStream()));
				FileWriter oDstFile = new FileWriter(oFile);

				boolean bHasLoc = false; // default unknown location
				boolean bHasObs = false; // default unknown observations
				String[] sValues; // placeholder for columns
				String sLine;

				while ((sLine = oReader.readLine()) != null)
				{
					oDstFile.write(sLine); // copy remote file content
					oDstFile.write("\n"); // replace newline

					if (!bHasObs) // skip header processing when complete
					{
						if (sLine.startsWith("VIN:")) // update platform id
						{
							sValues = sLine.split(","); // VIN in last column
							if (!sValues[sValues.length - 1].contains("10001"))
								sPlatformId = sValues[sValues.length - 1].trim();
						}

						if (sLine.startsWith("Date:")) // save date information
						{
							sValues = sLine.split(","); // date in last column
							sDate = sValues[sValues.length - 1].trim();
						}

						if (sLine.startsWith("Air Temp:")) // save air temp
						{
							sValues = sLine.split(","); // use last column
							sAirTemp = sValues[sValues.length - 1].trim();
						}

						if (sLine.startsWith("Barometer:")) // save pressure
						{
							sValues = sLine.split(","); // use last column
							sPressure = sValues[sValues.length - 1].trim();
						}

						if (sLine.startsWith("TIME")) // change logic flags
							bHasObs = true;
					}
					else if (!bHasLoc)
					{
						// find first valid location by satellite count
						sValues = sLine.split(",");
						if (sValues != null && sValues.length > 4)
						{
							try // need at least 4 satellites, but not error
							{
								int nSat = Integer.parseInt(sValues[4]);
								if (nSat > 3 && nSat < 10001)
								{
									sTime = sValues[0].trim();
									sLat = sValues[1].trim();
									sLon = sValues[2].trim();
									sAlt = sValues[3].trim();
									bHasLoc = true; // finished processing
								}
							}
							catch (Exception oNaN)
							{
							}
						}
					}
				}

				oDstFile.close();
				oReader.close();

				// append observations to daily accumulation file
				// generate filename from clock so late files are collected
				String sObsFile = sArgs[1] + sDirTop + "/" + 
					oDateFormat.format(oCalendar.getTime());
				FileWriter oObsFile = new FileWriter(sObsFile, true);

				oObsFile.write(sPlatformId);
				oObsFile.write(",");
				oObsFile.write(sDate);
				oObsFile.write(" ");
				oObsFile.write(sTime);
				oObsFile.write(",");
				oObsFile.write(sLat);
				oObsFile.write(",");
				oObsFile.write(sLon);
				oObsFile.write(",");
				oObsFile.write(sAlt);
				oObsFile.write(",");
				oObsFile.write(sAirTemp);
				oObsFile.write(",");
				oObsFile.write(sPressure);
				oObsFile.write("\n");
				
				oObsFile.close();
			}
			catch (Exception oException)
			{
			}
		}
	}
}
