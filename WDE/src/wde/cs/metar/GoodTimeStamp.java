package wde.cs.metar;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * The GoodTimeStamp class gets and holds the more accurate from a METAR record
 *
 * @author scot.lange
 */
public class GoodTimeStamp extends Report {
    String m_sTimestamp;
    Date m_oTimeStamp;
    SimpleDateFormat oFormatter = null;

    GoodTimeStamp() {
        oFormatter = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        oFormatter.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    public int parse(int nStartIndex, int nRecursiveCount) {
        try {
            m_sTimestamp = g_sBuffer.toString().trim();
            m_oTimeStamp = oFormatter.parse(m_sTimestamp);
        } catch (Exception oException) {
            int aoue = 0;
        }
        return (g_nBufferLength);
    }


    public void clearValues() {
        m_oTimeStamp = null;
    }


    public String toString() {
        if (m_oTimeStamp == null)
            return "";
        else
            return "," + m_oTimeStamp.toString();
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_oTimeStamp != null)
            oOut.write(m_sTimestamp);
//            oOut.write(m_oTimeStamp.toString());
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("Time");
        if (!bLast)
            oOut.write(",");
    }
}
