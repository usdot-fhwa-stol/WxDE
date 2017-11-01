package wde.qeds;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Notification;
import wde.util.Scheduler;
import wde.util.threads.AsyncQ;

import javax.mail.internet.InternetAddress;
import javax.sql.DataSource;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Inet4Address;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;

/**
 *
 */
public class ContribMonitor extends AsyncQ<IObsSet> {
    static Logger logger = Logger.getLogger(ContribMonitor.class);

    private static ContribMonitor g_oInstance = new ContribMonitor();
    ArrayList<ContribPlatforms> m_oContribPlatforms = new ArrayList<ContribPlatforms>();
    private WDEMgr wdeMgr = WDEMgr.getInstance();
    private Contribs m_oContribs = Contribs.getInstance();
    private PlatformDao platformDao = PlatformDao.getInstance();
    private SensorDao sensorDao = SensorDao.getInstance();
    private Config m_oConfig;


    private ContribMonitor() {
        // initialize list of platforms
        ArrayList<IPlatform> platforms = platformDao.getActivePlatforms();
        int nIndex = platforms.size();
        while (nIndex-- > 0)
            getContribPlatforms(platforms.get(nIndex));

        // schedule monitor to shift time cells every five minutes
        Scheduler.getInstance().schedule(new Rotate(), 0, 300, false);
        wdeMgr.register(getClass().getName(), this);
    }

    public static ContribMonitor getInstance() {
        return g_oInstance;
    }

    @Override
    public void run(IObsSet iObsSet) {
        int nIndex = iObsSet.size();
        logger.info("run for iObsSet.size() = " + nIndex);

        while (nIndex-- > 0) {
            IObs iObs = iObsSet.get(nIndex);

            ISensor iSensor = sensorDao.getSensor(iObs.getSensorId());
            if (iSensor == null)
                continue;

            IPlatform iPlatform = platformDao.getPlatform(iSensor.getPlatformId());
            if (iPlatform == null)
                continue;

            ContribPlatforms oContribPlatforms = getContribPlatforms(iPlatform);
            if (oContribPlatforms != null)
                oContribPlatforms.update(iSensor, iPlatform);
        }
        // queue the obs set for the next process
        wdeMgr.queue(iObsSet);
    }


    private ContribPlatforms getContribPlatforms(IPlatform platform) {
        ContribPlatforms oContribPlatforms = null;
        Contrib oContrib = m_oContribs.getContrib(platform.getContribId());

        if (oContrib == null)
            return oContribPlatforms;

        synchronized (this) {
            int nContribIndex = Collections.binarySearch(m_oContribPlatforms, oContrib);
            if (nContribIndex < 0) {
                oContribPlatforms = new ContribPlatforms(oContrib);
                m_oContribPlatforms.add(~nContribIndex, oContribPlatforms);
            } else
                oContribPlatforms = m_oContribPlatforms.get(nContribIndex);
        }

        return oContribPlatforms;
    }


    public synchronized void print(PrintWriter oWriter) {
        for (int nIndex = 0; nIndex < m_oContribPlatforms.size(); nIndex++)
            m_oContribPlatforms.get(nIndex).printCSV(oWriter);
    }


    private class Rotate implements Runnable {
        private Rotate() {
        }


        private void writeHeader(PrintWriter oPrintWriter) {
            oPrintWriter.println("<html>");
            oPrintWriter.println("<body>");
            oPrintWriter.println("<table border='1' cellpadding='0' cellspacing='0'>");
            oPrintWriter.print("<tr>");
            oPrintWriter.print("<td>Contributor</td><td>Platform</td>");

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
            while (nColSpan >= 12) {
                if (--nHour < 0)
                    nHour = 23;

                oPrintWriter.print("<td colspan='12'>");
                if (nHour < 10)
                    oPrintWriter.print('0');
                oPrintWriter.print(nHour);
                oPrintWriter.print("</td>");

                nColSpan -= 12;
            }

            if (nColSpan > 0) {
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
            while (nColSpan-- > 0) {
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


        private void writeFooter(PrintWriter oPrintWriter) {
            oPrintWriter.println("</table>");
            oPrintWriter.println("</body>");
            oPrintWriter.println("</html>");

            oPrintWriter.flush();
        }

        private void sendMail(ContribPlatforms oContribPlatforms)
                throws Exception {
            boolean bSendMail = oContribPlatforms.m_bSendMail;
            boolean bSentMail = oContribPlatforms.m_bSentMail;
            if ((bSendMail && bSentMail) || (!bSendMail && !bSentMail))
                return; // only send email when status changes

            DataSource iDataSource = wdeMgr.getDataSource(m_oConfig.getString("datasource", null));
            if (iDataSource == null)
                return;

            ArrayList<InternetAddress> oRecipients = new ArrayList<InternetAddress>();

            try(Connection iConnection = iDataSource.getConnection();
                PreparedStatement statement = iConnection.prepareStatement("SELECT email FROM conf.monitorContact WHERE contribId = ?");)
            {
              statement.setInt(1, oContribPlatforms.m_oContrib.getId());

              try(ResultSet rs = statement.executeQuery())
              {
                while (rs.next())
                    oRecipients.add(new InternetAddress(rs.getString(1)));
              }
              //testing phase
              oRecipients.add(new InternetAddress("bryan.krueger@synesis-partners.com"));
            }

            if (oRecipients.isEmpty())
                return; // there must be at least one recipient address

            // test only
            String hostname = Inet4Address.getLocalHost().getHostName();

            String sSubject = "WxDE (" + hostname + ") - " + oContribPlatforms.m_oContrib.getName() + " observations ";
            String msgText = "";
            if (!bSendMail && bSentMail) {
                sSubject += "have resumed";
                msgText = sSubject; // copy subject into body
            } else {
                sSubject += "not received";

                msgText =
                        "Greetings:\n\r" +
                                "WxDE has not received any new observations for " +
                                oContribPlatforms.m_oContrib.m_nHours + " hours. " +
                                "Attached is a report of the number of observations " +
                                "received per station over the past 24 hours. Time is " +
                                "measured from UTC with the most recent five minutes on " +
                                "the left side and progressing backward in time toward " +
                                "the right side.\n\r";
            }

            String filePath = m_oConfig.getString("dir", null) + oContribPlatforms.m_oContrib.getId() + ".html";
            String fileDisplayName = oContribPlatforms.m_oContrib.getName() + ".html";

            logger.info("Sending status email ...");
            Notification.getInstance().sendEmail(oRecipients, sSubject, msgText, filePath, fileDisplayName);

            oContribPlatforms.m_bSentMail = !oContribPlatforms.m_bSentMail;
        }

        public void run() {
            logger.info("calling Rotate.run()");

            // read configuration at every interval
            m_oConfig = ConfigSvc.getInstance().getConfig(ContribMonitor.this);

            StringWriter oMainStringWriter = new StringWriter(5000000);
            PrintWriter oMainPrintWriter = new PrintWriter(oMainStringWriter);

            StringWriter oSubStringWriter = new StringWriter(1000000);
            PrintWriter oSubPrintWriter = new PrintWriter(oSubStringWriter);
            StringBuffer sSubBuffer = oSubStringWriter.getBuffer();

            writeHeader(oMainPrintWriter);

            synchronized (ContribMonitor.this) {
                for (int nContribIndex = 0; nContribIndex < m_oContribPlatforms.size(); nContribIndex++) {
                    ContribPlatforms oContribPlatforms = m_oContribPlatforms.get(nContribIndex);

                    sSubBuffer.setLength(0);
                    writeHeader(oSubPrintWriter);
                    oContribPlatforms.printHTML(oSubPrintWriter);
                    writeFooter(oSubPrintWriter);

                    oContribPlatforms.printHTML(oMainPrintWriter);
                    oContribPlatforms.rotate(); // rotate is always the last step

                    try {
                        FileWriter oSubFileWriter = new FileWriter(
                                m_oConfig.getString("dir", null) +
                                        oContribPlatforms.m_oContrib.m_nId + ".html");

                        // copy buffer contents to file
                        for (int nIndex = 0; nIndex < sSubBuffer.length(); nIndex++)
                            oSubFileWriter.write(sSubBuffer.charAt(nIndex));

                        oSubFileWriter.close();

                        sendMail(oContribPlatforms);
                    } catch (Exception e) {
                        e.printStackTrace();
                        logger.error(e.getMessage());
                    }
                }
            }

            writeFooter(oMainPrintWriter);
            StringBuffer sMainBuffer = oMainStringWriter.getBuffer();
            try {
                FileWriter oFileWriter =
                        new FileWriter(m_oConfig.getString("dir", null) + "0.html");

                // copy buffer contents to file
                for (int nIndex = 0; nIndex < sMainBuffer.length(); nIndex++)
                    oFileWriter.write(sMainBuffer.charAt(nIndex));

                oFileWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }
        }
    }
}
