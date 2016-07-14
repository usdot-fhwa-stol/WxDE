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
			oDMW.fillArrays(this);
			if (!oDMW.m_bFail)
			{
				oDMW.run();
				oDMW.saveRoadcast(this);
			}
			oMetroMgr.createAlerts(this);
			System.out.println(((m_dLatTop + m_dLatBot) / 2) + "_" + ((m_dLonRight + m_dLonLeft) / 2) + " " + MetroResults.getInstance().getReading(51137, oMetroMgr.m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2)+ " " + MetroResults.getInstance().getReading(51138, oMetroMgr.m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2));
			if (oMetroMgr.m_nRunning.decrementAndGet() == 0)
			{
				long lNow = System.currentTimeMillis();
				System.out.println("Finished METRo: Ran from " + m_oTimestamp.format(oMetroMgr.m_lNow) + " to " + m_oTimestamp.format(lNow));
				//if it takes METRo more than an hour to run, update the MetroResults List for each hour
				if ((lNow - oMetroMgr.m_lNow) >= 3600000)
				{
					for (int i = 1; i <= (lNow - oMetroMgr.m_lNow) / 3600000; i++)
						MetroResults.getInstance().initArrayList(oMetroMgr.m_lNow + (3600000 * i), oMetroMgr.m_nObservationHours, oMetroMgr.m_nForecastHours);
				}
			}
		}
	}