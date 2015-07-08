package wde.cs.metar;

import java.io.FileWriter;

/**
 * The ReportModifier gets the the value indicating if the report is an
 * automatic or correctional report.  Its primary puprose is to skip over
 * to the next group for the next Report subclass that parses the stream, but
 * is stored for debugging and in case it is used later on.
 *
 * @author scot.lange
 */
public class ReportModifier extends Report {
    String m_sModifier = sNO_DATA;

    ReportModifier() {
    }


    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        // search for, and ignore, AUTO and COR modifier labels
        // this will only adjust the index for the start of the next report
        int nIndex;

        if ((nIndex = g_sBuffer.indexOf("AUTO", nStartIndex)) > -1) {
            nIndex += 5;
            m_sModifier = super.substring(nStartIndex, nIndex);
        } else if ((nIndex = g_sBuffer.indexOf("COR", nStartIndex)) > -1) {
            nIndex += 4;
            m_sModifier = super.substring(nStartIndex, nIndex);
        } else
            return nStartIndex;

        return nIndex;
    }


    @Override
    public void clearValues() {
        m_sModifier = sNO_DATA;
    }


    @Override
    public String toString() {
        return "";
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {

    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {

    }


}
