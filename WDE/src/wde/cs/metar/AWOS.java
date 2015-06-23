package wde.cs.metar;

import java.io.FileWriter;

/**
 * The AWOS class holds an array of Report subclasses in the order that they
 * should appear in the report.  it is used to parse METAR reports by calling
 * the parse functions of each of the reports in the array.
 *
 * @author scot.lange
 */
public class AWOS extends Report {
    public String m_sSourceString = "";

    public Report[] m_oReports =
            {
                    new GoodTimeStamp(),
                    new StationIdentifier(),
                    new BadTimeStamp(),
                    new ReportModifier(),
                    new Wind(),
                    new Precipitation(this), // precipitation needs access to timestamp
                    //to determine if it is a 3 or 6 hour report
                    new Visibility(),
                    new PresentWeather(),
                    new SkyCondition(),
                    new TemperatureDewPoint(),
                    new Altimeter()
            };

    public AWOS() {
        clearValues();
    }


    public void setBuffer(StringBuilder sBuffer) {
        g_sBuffer = sBuffer;
        g_nBufferLength = sBuffer.length();
        m_sSourceString = sBuffer.toString();
    }


    public long getTimeStamp() {
        return ((GoodTimeStamp) m_oReports[0]).m_oTimeStamp.getTime();
    }


    public String getStationCode() {
        return ((StationIdentifier) m_oReports[1]).m_sStationId;
    }


    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        clearValues();

        try {
            for (int nIndex = 1; nIndex < 3; nIndex++)
                nStartIndex = super.nearestGroupStart(
                        m_oReports[nIndex].parse(nStartIndex, 0));

            for (int nI = 3; nI < m_oReports.length; ++nI) {
                nStartIndex = super.nearestGroupStart(
                        m_oReports[nI].parse(nStartIndex, 1));
                if (nStartIndex > g_nBufferLength - 3)
                    break;
            }
        }
        //If there's an error, it is at the end of the line and all the
        //valid values have already been read in, so just return as if
        //it had been parsed normally.
        catch (Exception oException) {
            int nX = 0;
        }// just for a break point

        return nStartIndex;
    }


    @Override
    public void clearValues() {
        int nI = m_oReports.length;
        while (--nI > 0)
            m_oReports[nI].clearValues();
    }


    @Override
    public String toString() {
        String sReturnString = m_sSourceString + "\n";
        for (int nI = 0; nI < m_oReports.length; ++nI)
            sReturnString += m_oReports[nI].toString();
        return sReturnString;
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        int nI;
        for (nI = 0; nI < m_oReports.length - 1; ++nI)
            m_oReports[nI].toCSV(oOut, false);
        m_oReports[nI].toCSV(oOut, true);
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        int nI;
        for (nI = 0; nI < m_oReports.length - 1; ++nI)
            m_oReports[nI].printCSVHeader(oOut, false);
        m_oReports[nI].printCSVHeader(oOut, true);
    }

}
