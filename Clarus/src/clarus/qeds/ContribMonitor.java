package clarus.qeds;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import clarus.emc.ISensor;
import clarus.emc.IStation;
import clarus.emc.Sensors;
import clarus.emc.Stations;
import clarus.qedc.IObs;
import clarus.qedc.IObsSet;
import util.Config;
import util.ConfigSvc;
import util.Scheduler;
import util.threads.AsyncQ;

/**
 *
 */
public class ContribMonitor extends AsyncQ<IObsSet>
{
	private static ContribMonitor g_oInstance = new ContribMonitor();
	private ClarusMgr m_oClarusMgr = ClarusMgr.getInstance();
	private Contribs m_oContribs = Contribs.getInstance();
	private Stations m_oStations = Stations.getInstance();
	private Sensors m_oSensors = Sensors.getInstance();
	private Config m_oConfig;
	ArrayList<ContribStations> m_oContribStations =
		new ArrayList<ContribStations>();


	public static ContribMonitor getInstance()
	{
		return g_oInstance;
	}


	private ContribMonitor()
	{
		// initialize list of stations
		ArrayList<IStation> oStations = new ArrayList<IStation>();
		m_oStations.getStations(oStations);
		int nIndex = oStations.size();
		while (nIndex-- > 0)
			getContribStations(oStations.get(nIndex));

		// schedule monitor to shift time cells every five minutes
		Scheduler.getInstance().schedule(new Rotate(), 0, 300);
		m_oClarusMgr.register(getClass().getName(), this);
	}


	@Override
	public void run(IObsSet iObsSet)
	{
		int nIndex = iObsSet.size();
		while (nIndex-- > 0)
		{
			IObs iObs = iObsSet.get(nIndex);

			ISensor iSensor = m_oSensors.getSensor(iObs.getSensorId());
			if (iSensor == null)
				continue;

			IStation iStation = m_oStations.getStation(iSensor.getStationId());
			if (iStation == null)
				continue;

			ContribStations oContribStations = getContribStations(iStation);
			if (oContribStations != null)
				oContribStations.update(iSensor, iStation);
		}
		// queue the obs set for the next process
		m_oClarusMgr.queue(iObsSet);
	}


	private ContribStations getContribStations(IStation iStation)
	{
		ContribStations oContribStations = null;
		Contrib oContrib = m_oContribs.getContrib(iStation.getContribId());

		if (oContrib == null)
			return oContribStations;

		synchronized(this)
		{
			int nContribIndex = Collections.binarySearch(m_oContribStations, oContrib);
			if (nContribIndex < 0)
			{
				oContribStations = new ContribStations(oContrib);
				m_oContribStations.add(~nContribIndex, oContribStations);
			}
			else
				oContribStations = m_oContribStations.get(nContribIndex);
		}

		return oContribStations;
	}


	public synchronized void print(PrintWriter oWriter)
	{
		for (int nIndex = 0; nIndex < m_oContribStations.size(); nIndex++)
			m_oContribStations.get(nIndex).printCSV(oWriter);
	}


	private class Rotate implements Runnable
	{
		private Rotate()
		{
		}


		private void writeHeader(PrintWriter oPrintWriter)
		{
			oPrintWriter.println("<html>");
			oPrintWriter.println("<body>");
			oPrintWriter.println("<table border='1' cellpadding='0' cellspacing='0'>");
			oPrintWriter.print("<tr>");
			oPrintWriter.print("<td>Contributor</td><td>Station</td>");

			GregorianCalendar oNow = new GregorianCalendar();
			int nHour = oNow.get(GregorianCalendar.HOUR_OF_DAY);
			int nMinute = oNow.get(GregorianCalendar.MINUTE);

			int nColSpan = nMinute / 5 + 1;
			oPrintWriter.print("<td colspan='");
			oPrintWriter.print(nColSpan);
			oPrintWriter.print("'>");
			if (nHour < 10)
				oPrintWriter.print('0');
			oPrintWriter.print(nHour);
			oPrintWriter.print("</td>");

			nColSpan = 288 - nColSpan;
			while (nColSpan >= 12)
			{
				if (--nHour < 0)
					nHour = 23;

				oPrintWriter.print("<td colspan='12'>");
				if (nHour < 10)
					oPrintWriter.print('0');
				oPrintWriter.print(nHour);
				oPrintWriter.print("</td>");

				nColSpan -= 12;
			}

			if (nColSpan > 0)
			{
				if (--nHour < 0)
					nHour = 23;

				oPrintWriter.print("<td colspan='");
				oPrintWriter.print(nColSpan);
				oPrintWriter.print("'>");
				if (nHour < 10)
					oPrintWriter.print('0');
				oPrintWriter.print(nHour);
				oPrintWriter.print("</td>");
			}

			oPrintWriter.println("</tr>");


			oPrintWriter.print("<tr>");
			oPrintWriter.print("<td>Name</td><td>Name</td>");
			nColSpan = 288;
			while (nColSpan-- > 0)
			{
				oPrintWriter.print("<td>");
				if (nMinute < 10)
					oPrintWriter.print('0');
				oPrintWriter.print(nMinute);

				nMinute -= 5;
				if (nMinute < 0)
					nMinute = 55;

				oPrintWriter.print("</td>");
			}
			oPrintWriter.println("</tr>");
		}


		private void writeFooter(PrintWriter oPrintWriter)
		{
			oPrintWriter.println("</table>");
			oPrintWriter.println("</body>");
			oPrintWriter.println("</html>");

			oPrintWriter.flush();
		}


		private void sendMail(ContribStations oContribStations)
			throws Exception
		{
			boolean bSendMail = oContribStations.m_bSendMail;
			boolean bSentMail = oContribStations.m_bSentMail;
			if ((bSendMail && bSentMail) || (!bSendMail && !bSentMail))
				return; // only send email when status changes

			DataSource iDataSource = m_oClarusMgr.
				getDataSource(m_oConfig.getString("datasource", null));
			if (iDataSource == null)
				return;

			Connection iConnection = iDataSource.getConnection();
			if (iConnection == null)
				return;

			ResultSet iResultSet = iConnection.createStatement().
				executeQuery("SELECT email FROM monitorContact " +
				"WHERE contribId=" + oContribStations.m_oContrib.getId());

			ArrayList<InternetAddress> oRecipients = new ArrayList<InternetAddress>();
			while (iResultSet.next())
				oRecipients.add(new InternetAddress(iResultSet.getString(1)));

			iConnection.close();
			if (oRecipients.isEmpty())
				return; // there must be at least one recipient address

			Properties oProperties = new Properties();
			oProperties.put("mail.smtp.host", m_oConfig.getString("mail.smtp.host", null));
			oProperties.put("mail.from", m_oConfig.getString("mail.from", null));

			Session oSession = Session.getInstance(oProperties, null);

			// send one message when status changes to unacceptable
			// and a secod message when normal operation resumes
			MimeMessage oMessage = new MimeMessage(oSession);
			oMessage.setSentDate(new Date());

			for (int nIndex = 0; nIndex < oRecipients.size(); nIndex++)
				oMessage.setRecipient(Message.RecipientType.TO, oRecipients.get(nIndex));

			oMessage.setRecipient(Message.RecipientType.BCC,
				new InternetAddress(m_oConfig.getString("bcc", null)));

			oMessage.setFrom(); // automatically populated from properties

			String sSubject = "Clarus - " + oContribStations.m_oContrib.getName() + " observations ";
			if (!bSendMail && bSentMail)
			{
				sSubject += "have resumed";
				oMessage.setText(sSubject); // copy subject into body
			}
			else
			{
				sSubject += "not received";

				 // create and fill the first message part
				MimeBodyPart oMsgText = new MimeBodyPart();
				oMsgText.setText
				(
					"Greetings:\n\r" +
					"Clarus has not received any new observations for " +
					oContribStations.m_oContrib.m_nHours + " hours. " +
					"Attached is a report of the number of observations " +
					"received per station over the past 24 hours. Time is " +
					"measured from UTC with the most recent five minutes on " +
					"the left side and progressing backward in time toward " +
					"the right side.\n\r"
				);

				// create the second message part
				MimeBodyPart oMsgFile = new MimeBodyPart();
			    oMsgFile.attachFile(m_oConfig.getString("dir", null) +
					oContribStations.m_oContrib.getId() + ".html");
				oMsgFile.setFileName(oContribStations.m_oContrib.getName() + ".html");

				// create the Multipart message and add the parts to it
				Multipart oMultiplart = new MimeMultipart();
				oMultiplart.addBodyPart(oMsgText);
				oMultiplart.addBodyPart(oMsgFile);

				// add the Multipart to the message
				oMessage.setContent(oMultiplart);
			}

			oMessage.setSubject(sSubject);
			Transport.send(oMessage); // very last step
			oContribStations.m_bSentMail = !oContribStations.m_bSentMail;
		}


		public void run()
		{
			// read configuration at every interval
			m_oConfig = ConfigSvc.getInstance().getConfig(ContribMonitor.this);

			StringWriter oMainStringWriter = new StringWriter(5000000);
			PrintWriter oMainPrintWriter = new PrintWriter(oMainStringWriter);

			StringWriter oSubStringWriter = new StringWriter(1000000);
			PrintWriter oSubPrintWriter = new PrintWriter(oSubStringWriter);
			StringBuffer sSubBuffer = oSubStringWriter.getBuffer();

			writeHeader(oMainPrintWriter);

			synchronized(ContribMonitor.this)
			{
				for (int nContribIndex = 0; nContribIndex < m_oContribStations.size(); nContribIndex++)
				{
					ContribStations oContribStations = m_oContribStations.get(nContribIndex);

					sSubBuffer.setLength(0);
					writeHeader(oSubPrintWriter);
					oContribStations.printHTML(oSubPrintWriter);
					writeFooter(oSubPrintWriter);

					oContribStations.printHTML(oMainPrintWriter);
					oContribStations.rotate(); // rotate is always the last step

					try
					{
						FileWriter oSubFileWriter = new FileWriter(
							m_oConfig.getString("dir", null) +
							oContribStations.m_oContrib.m_nId + ".html");

						// copy buffer contents to file
						for (int nIndex = 0; nIndex < sSubBuffer.length(); nIndex++)
							oSubFileWriter.write(sSubBuffer.charAt(nIndex));

						oSubFileWriter.close();

						sendMail(oContribStations);
					}
					catch (Exception oException)
					{
						oException.printStackTrace();
					}
				}
			}

			writeFooter(oMainPrintWriter);
			StringBuffer sMainBuffer = oMainStringWriter.getBuffer();
			try
			{
				FileWriter oFileWriter =
					new FileWriter(m_oConfig.getString("dir", null) + "0.html");

				// copy buffer contents to file
				for (int nIndex = 0; nIndex < sMainBuffer.length(); nIndex++)
					oFileWriter.write(sMainBuffer.charAt(nIndex));
				
				oFileWriter.close();
			}
			catch (Exception oException)
			{
				oException.printStackTrace();
			}
		}
	}
}
