package wde.cs.metar;

import java.io.FileWriter;

/**
 * The Altimiter class holds the altimeter reading from a METAR record.
 * It also contains the function to get the reading out of the record
 *
 * @author scot.lange
 */
public class Altimeter extends Report {
    double m_dPressure = dNO_DATA;

    Altimeter() {
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = super.currentGroupEnd(nStartIndex);
        //Check that the group starts with an A, and check to make sure it
        //is longer than the automated station type code (A--)
        if (g_sBuffer.charAt(nStartIndex) != 'A' && nGroupEnd - nStartIndex > 2)
            return
                    super.tryNextGroup(nStartIndex, nRecursiveCount, this);

        int nCodedPressure =
                super.parseInt(super.substring(nStartIndex + 1, nGroupEnd));
        if (nCodedPressure != nNO_DATA) {
            ++m_nObsCount;
            m_dPressure = (double) nCodedPressure;
            m_dPressure /= 100;
        }

        return nGroupEnd + 1;
    }

    @Override
    public double getObservationValue(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return dNO_DATA;

        return m_dPressure;
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return super.getObservationType(nObservationIndex);
        return "Pressure";
    }


    @Override
    public void clearValues() {
        m_dPressure = dNO_DATA;
        m_nObsCount = 0;
    }


    @Override
    public String toString() {
        if (m_dPressure == dNO_DATA)
            return "";
        else
            return "," + Double.toString(m_dPressure);
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_dPressure != dNO_DATA)
            oOut.write(Double.toString(m_dPressure));
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("Pressure");
        if (!bLast)
            oOut.write(",");
    }


}

