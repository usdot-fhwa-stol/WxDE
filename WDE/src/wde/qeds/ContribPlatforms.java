package wde.qeds;

import wde.metadata.IPlatform;
import wde.metadata.ISensor;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/**
 *
 */
class ContribPlatforms implements Comparable<Contrib> {
    boolean m_bSendMail;
    boolean m_bSentMail;
    Contrib m_oContrib;
    private ArrayList<PlatformReport> m_oReports = new ArrayList<PlatformReport>();

    ContribPlatforms(Contrib oContrib) {
        m_oContrib = oContrib;
    }


    public void rotate() {
        int nCount = 0;
        int nIntervals = m_oContrib.m_nHours * 12; // hours to five-minute intervals

        int nIndex = m_oReports.size();
        if (nIndex == 0)
            return; // nothing to do

        while (nIndex-- > 0) {
            int nInner = nIntervals;
            Iterator<SensorReport> iIterator = m_oReports.get(nIndex).iterator();
            while (nCount == 0 && nInner-- > 0 && iIterator.hasNext()) {
                if (iIterator.next().m_nId != 0)
                    ++nCount; // negative values indicate non-initialization
            }
            m_oReports.get(nIndex).rotate();
        }

        m_bSendMail = (nCount == 0); // no observations for period across platforms
    }


    public void update(ISensor iSensor, IPlatform iPlatform) {
        PlatformReport oPlatformReport = null;

        int nIndex = Collections.binarySearch(m_oReports, iPlatform);
        if (nIndex < 0) {
            oPlatformReport = new PlatformReport(iPlatform);
            m_oReports.add(~nIndex, oPlatformReport);
        } else
            oPlatformReport = m_oReports.get(nIndex);

        oPlatformReport.update(iSensor);
    }


    public void printHTML(PrintWriter oWriter) {
        for (int nIndex = 0; nIndex < m_oReports.size(); nIndex++) {
            PlatformReport oPlatformReport = m_oReports.get(nIndex);

            oWriter.print("<tr>");
            oWriter.print("<td>");
            oWriter.print(m_oContrib.m_sName);
            oWriter.print("</td>");
            oWriter.print("<td>");
            oWriter.print(oPlatformReport.m_iPlatform.getPlatformCode());
            oWriter.print("</td>");

            Iterator<SensorReport> iIterator = oPlatformReport.iterator();
            while (iIterator.hasNext()) {
                oWriter.print("<td>");
                int nValue = iIterator.next().m_nId;
                if (nValue > 0)
                    oWriter.print(nValue);
                oWriter.print("</td>");
            }
            oWriter.println("</tr>");
        }
    }


    public void printCSV(PrintWriter oWriter) {
        for (int nIndex = 0; nIndex < m_oReports.size(); nIndex++) {
            PlatformReport oPlatformReport = m_oReports.get(nIndex);
            oWriter.print(m_oContrib.m_sName);
            oWriter.print(',');
            oWriter.print(oPlatformReport.m_iPlatform.getPlatformCode());

            Iterator<SensorReport> iIterator = oPlatformReport.iterator();
            while (iIterator.hasNext()) {
                oWriter.print(',');
                oWriter.print(iIterator.next().m_nId);
            }
            oWriter.println();
        }
    }


    public int compareTo(Contrib oContrib) {
        return m_oContrib.m_sName.compareTo(oContrib.m_sName);
    }


    private class SensorReport implements Comparable<ISensor> {
        private int m_nId = -1;


        private SensorReport() {
        }


        public int compareTo(ISensor iSensor) {
            return (m_nId - iSensor.getId());
        }
    }


    private class PlatformReport extends ArrayDeque<SensorReport>
            implements Comparable<IPlatform> {
        public static final int SLOTS = 288;


        private ArrayList<SensorReport> m_oSensorIds = new ArrayList<SensorReport>();
        private IPlatform m_iPlatform;


        private PlatformReport() {
        }


        private PlatformReport(IPlatform iPlatform) {
            super(SLOTS);

            m_iPlatform = iPlatform;

            int nIndex = SLOTS;
            while (nIndex-- > 0)
                add(new SensorReport());
        }


        public void update(ISensor iSensor) {
            int nIndex = Collections.binarySearch(m_oSensorIds, iSensor);
            if (nIndex < 0) {
                SensorReport oSensorReport = new SensorReport();
                oSensorReport.m_nId = iSensor.getId();
                m_oSensorIds.add(~nIndex, oSensorReport);
            }

            peek().m_nId = m_oSensorIds.size(); // update report count
        }


        private void rotate() {
            m_oSensorIds.clear();

            // rotate old record out and prepend new record
            SensorReport oSensorReport = removeLast();
            oSensorReport.m_nId = 0; // reset update count
            this.push(oSensorReport);
        }


        public int compareTo(IPlatform iPlatform) {
            return m_iPlatform.getPlatformCode().compareTo(iPlatform.getPlatformCode());
        }
    }
}
