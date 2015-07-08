package wde.cs.metar;

import java.io.FileWriter;

/**
 * The Wind class gets and holds the Wind values from a METAR report
 *
 * @author scot.lange
 */
public class Wind extends Report {
    int m_nWindSpeed = nNO_DATA;
    int m_nWindDirection = nNO_DATA;
    int m_nWindGustSpeed = nNO_DATA;
    int[] m_nVariableWindDirection = new int[]{nNO_DATA, nNO_DATA};
    Wind() {
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = g_sBuffer.indexOf(" ", nStartIndex);

        if (g_sBuffer.charAt(nGroupEnd - 2) != 'K'
                || g_sBuffer.charAt(nGroupEnd - 1) != 'T')
            return super.tryNextGroup(nStartIndex, nRecursiveCount, this);
        int nIndex = nStartIndex;
        int nGIndex = g_sBuffer.indexOf("G", nIndex);
        int nWindEnd;
        int nNextEnd = super.nextEnd(nStartIndex);
        int nVIndex = g_sBuffer.indexOf("V", nGroupEnd);

        //if there's a gust component, the avg speed/direction will be right
        //before it, otherwise it will be right before the KT
        if (nGIndex > -1 && nGIndex < nGroupEnd)
            nWindEnd = nGIndex;
        else
            nWindEnd = nGroupEnd - 2;

        m_nWindDirection = super.parseInt(super.substring(nIndex, nIndex + 3));
        if (m_nWindDirection != nNO_DATA)
            ++m_nObsCount;

        m_nWindSpeed = super.parseInt(super.substring(nIndex + 3, nWindEnd));
        if (m_nWindSpeed != nNO_DATA)
            ++m_nObsCount;

        if (nGIndex > -1 && nGIndex < nGroupEnd) {
            m_nWindGustSpeed = super.parseInt(super.substring(
                    nGIndex + 1, nGroupEnd - 2));
            if (m_nWindGustSpeed != nNO_DATA)
                ++m_nObsCount;
        }

        //See if there's a variable direction component
        if (nVIndex > -1 && nNextEnd - (nGroupEnd + 1) == 7) {
            //Both the left and right sides of the V can be parsed as ints,
            //store the values and return the index to the group 2 ahead of
            //the current, otherwise return the index to the next group.
            try {
                m_nVariableWindDirection[0] = Integer.parseInt(
                        super.substring(nGroupEnd + 1, nVIndex));
                m_nVariableWindDirection[1] = Integer.parseInt(
                        super.substring(nVIndex + 1, nNextEnd));
                return nNextEnd + 1;
            } catch (Exception oException) {
                m_nVariableWindDirection[0] = nNO_DATA;
                m_nVariableWindDirection[1] = nNO_DATA;
                return nGroupEnd + 1;
            }
        } else
            return nGroupEnd + 1;
    }


    @Override
    public double getObservationValue(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return dNO_DATA;
        switch (nObservationIndex) {
            case 1:
                if (m_nWindSpeed != nNO_DATA)
                    return m_nWindSpeed;
                else if (m_nWindDirection != nNO_DATA)
                    return m_nWindDirection;
                else if (m_nWindGustSpeed != nNO_DATA)
                    return m_nWindGustSpeed;
                break;
            case 2:
                if (m_nWindDirection != nNO_DATA)
                    return m_nWindDirection;
                else if (m_nWindGustSpeed != nNO_DATA)
                    return m_nWindGustSpeed;
                break;
            case 3:
                return m_nWindGustSpeed;
        }

        return dNO_DATA;
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return super.getObservationType(nObservationIndex);
        switch (nObservationIndex) {
            case 1:
                if (m_nWindSpeed != nNO_DATA)
                    return "WindAvgSpeed";
                else if (m_nWindDirection != nNO_DATA)
                    return "WindDirection";
                else if (m_nWindGustSpeed != nNO_DATA)
                    return "WindGustSpeed";
                break;
            case 2:
                if (m_nWindDirection != nNO_DATA)
                    return "WindDirection";
                else if (m_nWindGustSpeed != nNO_DATA)
                    return "WindGustSpeed";
                break;
            case 3:
                return "WindGustSpeed";
        }
        return super.getObservationType(nObservationIndex);
    }


    @Override
    public void clearValues() {
        m_nWindSpeed = nNO_DATA;
        m_nWindDirection = nNO_DATA;
        m_nWindGustSpeed = nNO_DATA;
        m_nVariableWindDirection[0] = nNO_DATA;
        m_nVariableWindDirection[1] = nNO_DATA;
        m_nObsCount = 0;
    }


    @Override
    public String toString() {
        String sReturnString = "";
        if (m_nWindDirection != nNO_DATA)
            sReturnString += "," + m_nWindDirection;

        if (m_nWindSpeed != nNO_DATA)
            sReturnString += "," + m_nWindSpeed;

        if (m_nWindGustSpeed != nNO_DATA)
            sReturnString += "," + m_nWindGustSpeed;

        if (m_nVariableWindDirection[0] != nNO_DATA)
            sReturnString += "," + m_nVariableWindDirection[0];

        if (m_nVariableWindDirection[1] != nNO_DATA)
            sReturnString += "," + m_nVariableWindDirection[1];
        return sReturnString;
    }


    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("WindDirection,WindSpeed,WindGustSpeed");
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_nWindDirection != nNO_DATA)
            oOut.write(Integer.toString(m_nWindDirection));
        oOut.write(",");
        if (m_nWindSpeed != nNO_DATA)
            oOut.write(Integer.toString(m_nWindSpeed));
        oOut.write(",");
        if (m_nWindGustSpeed != nNO_DATA)
            oOut.write(Integer.toString(m_nWindGustSpeed));
        if (!bLast)
            oOut.write(",");
    }
}


