package wde.cs.metar;

import java.io.FileWriter;

/**
 * The StationIdentifier class gets and holds the StationID from a METAR report
 *
 * @author scot.lange
 */
public class StationIdentifier extends Report {
    public String m_sStationId = sNO_DATA;

    StationIdentifier() {

    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = super.currentGroupEnd(nStartIndex);
        if (nGroupEnd - nStartIndex == 4) {
            m_sStationId = super.substring(nStartIndex, nGroupEnd);
            return nGroupEnd + 1;
        } else
            return super.tryNextGroup(nStartIndex, nRecursiveCount, this);
    }


    @Override
    public void clearValues() {
        m_sStationId = sNO_DATA;
    }


    @Override
    public String toString() {
        return "," + m_sStationId;
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("StationId");
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_sStationId.compareTo(sNO_DATA) != 0)
            oOut.write(m_sStationId);

        if (!bLast)
            oOut.write(",");
    }


}
