package wde.cs.metar;

import java.io.FileWriter;

/**
 * Visibility gets and holds the visibility group from a METAR report
 */
public class Visibility extends Report {
    double m_dVisible = dNO_DATA;

    Visibility() {
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = super.currentGroupEnd(nStartIndex);

        if (g_sBuffer.charAt(nGroupEnd - 2) != 'S' || g_sBuffer.charAt(nGroupEnd - 1) != 'M')
            return super.tryNextGroup(nStartIndex, nRecursiveCount, this);

        int nIndex = nStartIndex;
        int nSlashIndex = g_sBuffer.indexOf("/", nStartIndex);

        //See if this is a whole number or fraction.
        if (nSlashIndex > -1 && nSlashIndex < nGroupEnd) {
            //numerator, denominator, and, if there is one, the whole number
            double dWholeNumber = 0;
            double dNumerator =
                    super.parseDouble(super.substring(nIndex, nIndex + 1));
            double dDenominator = super.parseDouble(super.substring(
                    nSlashIndex + 1, nGroupEnd - 2));
            if (nSlashIndex - nIndex == 2)
                dWholeNumber =
                        super.parseInt(super.substring(nIndex, ++nIndex));
            m_dVisible = dWholeNumber + dNumerator / dDenominator;
        } else // Just get the whole number
            m_dVisible =
                    super.parseInt(super.substring(nIndex, nGroupEnd - 2));
        if (m_dVisible != dNO_DATA)
            ++m_nObsCount;
        return nGroupEnd + 1;
    }


    @Override
    public double getObservationValue(int nObservationIndex) {
        if (m_nObsCount > 0 && nObservationIndex == 1)
            return m_dVisible;
        else
            return super.getObservationValue(nObservationIndex);
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        if (m_nObsCount > 0 && nObservationIndex == 1)
            return "Visibility";
        else
            return super.getObservationType(nObservationIndex);
    }


    @Override
    public void clearValues() {
        m_dVisible = dNO_DATA;
        m_nObsCount = 0;
    }


    @Override
    public String toString() {
        if (m_dVisible == dNO_DATA)
            return "";
        else
            return "," + Double.toString(m_dVisible);
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("Visibility");
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_dVisible != dNO_DATA)
            oOut.write(Double.toString(m_dVisible));
        if (!bLast)
            oOut.write(",");
    }
}

