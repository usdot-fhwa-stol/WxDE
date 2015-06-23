package clarus.qeds;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import clarus.emc.ISensor;
import clarus.emc.IStation;

/**
 *
 */
class ContribStations implements Comparable<Contrib>
{
	boolean m_bSendMail;
	boolean m_bSentMail;
	Contrib m_oContrib;
	private ArrayList<StationReport> m_oReports = new ArrayList<StationReport>();


	private ContribStations()
	{
	}


	ContribStations(Contrib oContrib)
	{
		m_oContrib = oContrib;
	}


	public void rotate()
	{
		int nCount = 0;
		int nIntervals = m_oContrib.m_nHours * 12; // hours to five-minute intervals

		int nIndex = m_oReports.size();
		if (nIndex == 0)
			return; // nothing to do

		while (nIndex-- > 0)
		{
			int nInner = nIntervals;
			Iterator<SensorReport> iIterator = m_oReports.get(nIndex).iterator();
			while (nCount == 0 && nInner-- > 0 && iIterator.hasNext())
			{
				if (iIterator.next().m_nId != 0)
					++nCount; // negative values indicate non-initialization
			}
			m_oReports.get(nIndex).rotate();
		}

		m_bSendMail = (nCount == 0); // no observations for period across stations
	}


	public void update(ISensor iSensor, IStation iStation)
	{
		StationReport oStationReport = null;

		int nIndex = Collections.binarySearch(m_oReports, iStation);
		if (nIndex < 0)
		{
			oStationReport = new StationReport(iStation);
			m_oReports.add(~nIndex, oStationReport);
		}
		else
			oStationReport = m_oReports.get(nIndex);

		oStationReport.update(iSensor);
	}


	public void printHTML(PrintWriter oWriter)
	{
		for (int nIndex = 0; nIndex < m_oReports.size(); nIndex++)
		{
			StationReport oStationReport = m_oReports.get(nIndex);

			oWriter.print("<tr>");
			oWriter.print("<td>");
			oWriter.print(m_oContrib.m_sName);
			oWriter.print("</td>");
			oWriter.print("<td>");
			oWriter.print(oStationReport.m_iStation.getCode());
			oWriter.print("</td>");

			Iterator<SensorReport> iIterator = oStationReport.iterator();
			while (iIterator.hasNext())
			{
				oWriter.print("<td>");
				int nValue = iIterator.next().m_nId;
				if (nValue > 0)
					oWriter.print(nValue);
				oWriter.print("</td>");
			}
			oWriter.println("</tr>");
		}
	}


	public void printCSV(PrintWriter oWriter)
	{
		for (int nIndex = 0; nIndex < m_oReports.size(); nIndex++)
		{
			StationReport oStationReport = m_oReports.get(nIndex);
			oWriter.print(m_oContrib.m_sName);
			oWriter.print(',');
			oWriter.print(oStationReport.m_iStation.getCode());

			Iterator<SensorReport> iIterator = oStationReport.iterator();
			while (iIterator.hasNext())
			{
				oWriter.print(',');
				oWriter.print(iIterator.next().m_nId);
			}
			oWriter.println();
		}
	}


	public int compareTo(Contrib oContrib)
	{
		return m_oContrib.m_sName.compareTo(oContrib.m_sName);
	}


	private class SensorReport implements Comparable<ISensor>
	{
		private int m_nId = -1;


		private SensorReport()
		{
		}


		public int compareTo(ISensor iSensor)
		{
			return (m_nId - iSensor.getSensorId());
		}
	}


	private class StationReport extends ArrayDeque<SensorReport>
		implements Comparable<IStation>
	{
		public static final int SLOTS = 288;


		private ArrayList<SensorReport> m_oSensorIds = new ArrayList<SensorReport>();
		private IStation m_iStation;


		private StationReport()
		{
		}


		private StationReport(IStation iStation)
		{
			super(SLOTS);

			m_iStation = iStation;

			int nIndex = SLOTS;
			while (nIndex-- > 0)
				add(new SensorReport());
		}


		public void update(ISensor iSensor)
		{
			int nIndex = Collections.binarySearch(m_oSensorIds, iSensor);
			if (nIndex < 0)
			{
				SensorReport oSensorReport = new SensorReport();
				oSensorReport.m_nId = iSensor.getSensorId();
				m_oSensorIds.add(~nIndex, oSensorReport);
			}

			peek().m_nId = m_oSensorIds.size(); // update report count
		}


		private void rotate()
		{
			m_oSensorIds.clear();

			// rotate old record out and prepend new record
			SensorReport oSensorReport = removeLast();
			oSensorReport.m_nId = 0; // reset update count
			this.push(oSensorReport);
		}


		public int compareTo(IStation iStation)
		{
			return m_iStation.getCode().compareTo(iStation.getCode());
		}
	}
}
