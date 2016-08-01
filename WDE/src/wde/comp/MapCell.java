package wde.comp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


/**
*  This class represents a single cell of a grid map. The cell is defined by a 
*  bounding box of latitudes and longitudes. It contains a list of roads that 
*  are in the region. It implements Runnable so that MetroMgr can run METRo for
*  every MapCell that contains a road.
*/
	public class MapCell implements Runnable
	{
		public final double m_dLatTop;
		public final double m_dLonLeft;
		public final double m_dLatBot;
		public final double m_dLonRight;
		public final String m_sRegion;
		public ArrayList<Integer> m_oRoads = new ArrayList();
		private final SimpleDateFormat m_oTimestamp = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm':'ss'Z'");
		
		
		MapCell(double dLatTop, double dLonLeft, double dLatBot, double dLonRight, String sRegion)
		{
			m_dLatTop = dLatTop;
			m_dLonLeft = dLonLeft;
			m_dLatBot = dLatBot;
			m_dLonRight = dLonRight;
			m_sRegion = sRegion;
		}
		
		
		/**
		 * This method runs METRo for the given MapCell by filling the input arrays
		 * running the DoMetroWrapper, and saving the output arrays from METRo. It
		 * also creates alerts for any Road in the MapCell with a road condition
		 * other than a dry road.
		 */
		@Override
		public void run()
		{
			MetroMgr oMetroMgr = MetroMgr.getInstance();
			DoMetroWrapper oDMW = new DoMetroWrapper();
			if (DoMetroWrapper.g_bLibraryLoaded)
			{
				oDMW.fillArrays(this);
				if (!oDMW.m_bFail)
				{
					oDMW.run();
					oDMW.saveRoadcast(this);
				}
				oMetroMgr.createAlerts(this);
			}
//			if (oMetroMgr.m_nRunning.get() % 1000 == 0)
//				System.out.println(((m_dLatTop + m_dLatBot) / 2) + "_" + ((m_dLonRight + m_dLonLeft) / 2) + " " + MetroResults.getInstance().getReading(51137, oMetroMgr.m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2)+ " " + MetroResults.getInstance().getReading(51138, oMetroMgr.m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2));
			if (oMetroMgr.m_nRunning.decrementAndGet() == 0)
			{
				long lNow = System.currentTimeMillis();
				System.out.println("Finished METRo: Ran from " + m_oTimestamp.format(oMetroMgr.m_lNow) + " to " + m_oTimestamp.format(lNow));
				oMetroMgr.writeNetcdfFile();
				//if it takes METRo more than an hour to run, update the MetroResults List for each hour
				if ((lNow - oMetroMgr.m_lNow) >= 3600000)
				{
					for (int i = 1; i <= (lNow - oMetroMgr.m_lNow) / 3600000; i++)
						MetroResults.getInstance().initArrayList(oMetroMgr.m_lNow + (3600000 * i), oMetroMgr.m_nObservationHours, oMetroMgr.m_nForecastHours);
				}
			}
		}
		
		
		/**
		 * This method runs Metro by calling the Python code for a given MapCell.
		 */
		public void runPython()
		{
			MetroMgr oMM = MetroMgr.getInstance();
			oMM.createObsXML(this);
			oMM.createForecastXML(this);
			oMM.createStationXML(this);
			try
			{
				Process oProcess = Runtime.getRuntime().exec("python /usr/local/metro/usr/bin/metro"
					+ " --input-forecast " + oMM.m_sBaseDir + "forecast" + Thread.currentThread().getId() + ".xml"
					+ " --input-station " + oMM.m_sBaseDir + "station" + Thread.currentThread().getId() + ".xml" 
					+ " --input-observation " + oMM.m_sBaseDir + "observation" + Thread.currentThread().getId() + ".xml" 
					+ " --output-roadcast " + oMM.m_sBaseDir + "roadcast" + Thread.currentThread().getId() + ".xml");
				oProcess.waitFor();
			}
			catch(Exception e)
			{
			}
			oMM.readRoadcastFile(oMM.m_sBaseDir + "roadcast" + Thread.currentThread().getId() + ".xml", this);
			System.out.println(oMM.m_nRunning.get() + " " + ((m_dLatTop + m_dLatBot) / 2) + "_" + ((m_dLonRight + m_dLonLeft) / 2) + " " + MetroResults.getInstance().getReading(51138, oMM.m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2));
			//delete old XML files
			oMM.cleanupFiles();
			//create alerts for all the roads in the MapCell
			oMM.createAlerts(this);
			if (oMM.m_nRunning.decrementAndGet() == 0)
			{
				long lNow = System.currentTimeMillis();
				System.out.println("Finished Metro: Ran from " + m_oTimestamp.format(oMM.m_lNow) + " to " + m_oTimestamp.format(lNow));
				if ((lNow - oMM.m_lNow) >= 3600000)
				{
					for (int i = 1; i <= (lNow - oMM.m_lNow) / 3600000; i++)
						MetroResults.getInstance().initArrayList(oMM.m_lNow + (3600000 * i), oMM.m_nObservationHours, oMM.m_nForecastHours);
				}
			}
		}
	}