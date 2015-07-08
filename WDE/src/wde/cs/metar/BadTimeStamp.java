package wde.cs.metar;

import java.io.FileWriter;

/**
 * The BadTimeStamp class holds the less accurate timestamp that is included
 * in a METAR report.  Its primary purpose is to skip over to the following
 * group, but holds the characters that were skipped for debugging
 *
 * @author scot.lange
 */
public class BadTimeStamp extends Report {
    String m_sTime = sNO_DATA;

    BadTimeStamp() {
    }


    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = super.currentGroupEnd(nStartIndex);
        if (g_sBuffer.charAt(nGroupEnd - 1) == 'Z') {
            m_sTime = super.substring(nStartIndex, nGroupEnd - 1);
            return nGroupEnd + 1;
        } else
            return
                    super.tryNextGroup(nStartIndex, nRecursiveCount, this);
    }


    public void clearValues() {
        m_sTime = sNO_DATA;
    }


    public String toString() {
        return "";
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {

    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {

    }


}
