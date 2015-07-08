package wde.cs.metar;

import java.io.FileWriter;

/**
 * The TemperatureDewPoint classgets and holds the Temperature/Dewpoint value
 * from a METAR report
 *
 * @author scot.lange
 */
public class TemperatureDewPoint extends Report {
    int m_nTemp = nNO_DATA;
    int m_nDewpoint = nNO_DATA;
    TemperatureDewPoint() {
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = super.currentGroupEnd(nStartIndex);
        //Look for the slash between values
        int nSlashIndex = g_sBuffer.indexOf("/", nStartIndex);

        if (nSlashIndex > -1 && nSlashIndex < nGroupEnd) {
            //We've got a slash, just get the two values.
            m_nTemp =
                    getTemperatureValue(nStartIndex, nSlashIndex);
            if (m_nTemp != nNO_DATA)
                ++m_nObsCount;
            m_nDewpoint =
                    getTemperatureValue(nSlashIndex + 1, nGroupEnd);
            if (m_nDewpoint != nNO_DATA)
                ++m_nObsCount;
            return nGroupEnd + 1;
        } else
            return
                    super.tryNextGroup(nStartIndex, nRecursiveCount, this);
    }


    @Override
    public double getObservationValue(int nObservationIndex) {
        if (nObservationIndex == 1) {
            if (m_nTemp != nNO_DATA)
                return m_nTemp;
            else
                return m_nDewpoint;
        } else if (nObservationIndex == 2)
            return m_nDewpoint;
        else
            return super.getObservationValue(nObservationIndex);
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return super.getObservationType(nObservationIndex);
        if (nObservationIndex == 1) {
            if (m_nTemp != nNO_DATA)
                return "Temperature";
            else
                return "DewpointTemperature";

        } else if (nObservationIndex == 2)
            return "DewPointTemperature";
        else
            return super.getObservationType(nObservationIndex);
    }

    private int getTemperatureValue(int nStart, int nEnd) {
        int nMultiplier;
        if (g_sBuffer.charAt(nStart) == 'M') {
            nMultiplier = -1;
            ++nStart;
        } else nMultiplier = 1;

        return nMultiplier * super.parseInt(super.substring(nStart, nEnd));

    }


    @Override
    public void clearValues() {
        m_nTemp = nNO_DATA;
        m_nDewpoint = nNO_DATA;
        m_nObsCount = 0;
    }


    @Override
    public String toString() {
        String sReturnString = "";
        if (m_nTemp != nNO_DATA)
            sReturnString += "," + Integer.toString(m_nTemp);

        if (m_nDewpoint != nNO_DATA)
            sReturnString += "," + Integer.toString(m_nDewpoint);
        return sReturnString;
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("Temp,DewPoint");
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_nTemp != nNO_DATA)
            oOut.write(Integer.toString(m_nTemp));
        oOut.write(",");
        if (m_nDewpoint != nNO_DATA)
            oOut.write(Integer.toString(m_nDewpoint));
        if (!bLast)
            oOut.write(",");
    }
}
