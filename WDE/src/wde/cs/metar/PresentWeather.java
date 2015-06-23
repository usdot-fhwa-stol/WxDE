package wde.cs.metar;

import java.io.FileWriter;

/**
 * The PresentWeather class holds the present values  from a METAR record.
 * It also contains the function to get the value out of the record
 *
 * @author scot.lange
 */
public class PresentWeather extends Report {
    int m_nArrayLength = 5;
    String[][] m_sWeather = new String[m_nArrayLength][2];
    int m_nPrecipitationSituation = nNO_DATA;
    int m_nVisibilitySituation = nNO_DATA;
    int m_nGroupCount = 0;

    PresentWeather() {
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        String[][] sNotation = new String[][]
                {{"MI", "PR", "BC", "DR", "BL", "SH", "TS", "FZ"}, //Descriptor
                        {"DZ", "RA", "SN", "SG", "IC", "PL", "GR", "GS", "UP"}, // Precipitation
                        {"BR", "FG", "FU", "VA", "DU", "SA", "HZ", "PY"}, // Visibility
                        {"PO", "SQ", "FC", "SS", "DS"}}; // Other

        int nIndex = nStartIndex;

        //Keep track of whether a value was found during the current iteration
        boolean bMatchFound = true;

        //Keep track of how many values have been found
        while (bMatchFound) {
            bMatchFound = false;

            //Check for an intensity value
            if (g_sBuffer.charAt(nIndex) == '+' || g_sBuffer.charAt(nIndex) == '-') {
                m_sWeather[m_nGroupCount][0] = super.substring(nIndex, ++nIndex);
                //take care of + HZ case
                if (g_sBuffer.charAt(nIndex) == ' ')
                    ++nIndex;
            }
            //Check for the "In the Vicinity" indicator
            else if (g_sBuffer.charAt(nIndex) == 'V'
                    && g_sBuffer.charAt(nIndex + 1) == 'C') {
                m_sWeather[m_nGroupCount][0] = "VC";
                nIndex += 2;
                if (g_sBuffer.charAt(nIndex) == ' ')
                    ++nIndex;
            }

            //Somtimes an errant space is between the descriptor and the value
            //itself, or if there are multiple conditions there may be spaces
            //between them.
            if (g_sBuffer.charAt(nIndex) == ' ')
                ++nIndex;

            //Lookp through checking for conditions
            for (int nI = 0; nI < sNotation.length; ++nI)
                for (int nJ = 0; nJ < sNotation[nI].length; ++nJ)
                    if (sNotation[nI][nJ].charAt(0) == g_sBuffer.charAt(nIndex)
                            && sNotation[nI][nJ].charAt(1)
                            == g_sBuffer.charAt(nIndex + 1)) {
                        m_sWeather[m_nGroupCount][1] += sNotation[nI][nJ];
                        nIndex += 2;
                        bMatchFound = true;
                        break;
                    }

            if (bMatchFound)
                ++m_nGroupCount;
        }

        if (m_nGroupCount > 0) {
            //Figure out whether there is a visibility condition,
            //precipitation condition,  or both

            //Look for precip
            boolean bValueFound = false;
            for (int nObsIndex = 0; nObsIndex < m_nGroupCount; ++nObsIndex) {
                for (int nJ = 0; nJ < sNotation[1].length; ++nJ) {
                    if (sNotation[1][nObsIndex].compareToIgnoreCase(m_sWeather[nObsIndex][1]) == 0) {
                        ++m_nObsCount;
                        bValueFound = true;
                        m_nPrecipitationSituation = numPrecip(m_sWeather[nObsIndex][1], m_sWeather[nObsIndex][0]);
                        break;
                    }
                }
                if (bValueFound)
                    break;
            }
            //Look for visibility
            bValueFound = false;
            for (int nObsIndex = 0; nObsIndex < m_nGroupCount; ++nObsIndex) {
                for (int nJ = 0; nJ < sNotation[2].length; ++nJ) {
                    if (sNotation[2][nObsIndex].compareToIgnoreCase(m_sWeather[nObsIndex][1]) == 0) {
                        ++m_nObsCount;
                        bValueFound = true;
                        m_nVisibilitySituation = numPrecip(m_sWeather[nObsIndex][1], m_sWeather[nObsIndex][0]);
                        break;
                    }
                }
                if (bValueFound)
                    break;
            }
            return nIndex;
        } else
            return
                    super.tryNextGroup(nStartIndex, nRecursiveCount, this);
    }


    @Override
    public double getObservationValue(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return super.getObservationValue(nObservationIndex);
        else {
            if (nObservationIndex == 1) {
                if (m_nPrecipitationSituation != nNO_DATA)
                    return m_nPrecipitationSituation;
                else
                    return m_nVisibilitySituation;
            } else if (nObservationIndex == 2)
                return m_nVisibilitySituation;
        }
        return super.getObservationValue(nObservationIndex);
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        if (nObservationIndex > m_nObsCount || nObservationIndex < 0)
            return super.getObservationType(nObservationIndex);
        else {
            if (nObservationIndex == 1) {
                if (m_nPrecipitationSituation != nNO_DATA)
                    return "PresentPrecipitation";
                else
                    return "PresentVisibility";
            } else if (nObservationIndex == 2)
                return "PresentVisibility";
        }
        return super.getObservationType(nObservationIndex);
    }


    //Find the range to return based ont the precip reading
    public int numPrecip(String sPrecip, String sIntensity) {
        int nRange = 0;
        if (sPrecip.compareToIgnoreCase("UP") == 0) nRange = 5;
        else if (sPrecip.compareToIgnoreCase("SN") == 0 || sPrecip.compareToIgnoreCase("SG") == 0) nRange = 8;
        else if (sPrecip.compareToIgnoreCase("RA") == 0 || sPrecip.compareToIgnoreCase("DZ") == 0) nRange = 11;
        else if (sPrecip.compareToIgnoreCase("IC") == 0 || sPrecip.compareToIgnoreCase("PL") == 0 || sPrecip.compareToIgnoreCase("GR") == 0 ||
                sPrecip.compareToIgnoreCase("GR") == 0 || sPrecip.compareToIgnoreCase("GS") == 0 || sPrecip.compareToIgnoreCase("FZ") == 0)
            nRange = 14;
        if (sIntensity.compareTo("+") == 0)
            ++nRange;
        if (sIntensity.compareTo("-") == 0)
            --nRange;
        return nRange;
    }

    //Find the range to return based ont the visible reading
    public int numVisible(String sVisible, String sIntensity) {
        int nRange = 0;
        if (sVisible.compareToIgnoreCase("BR") == 0 || sVisible.compareToIgnoreCase("VA") == 0 || sVisible.compareToIgnoreCase("HZ") == 0 ||
                sVisible.compareToIgnoreCase("PY") == 0) nRange = 1;
        else if (sVisible.compareToIgnoreCase("FG") == 0) nRange = 4;
        else if (sVisible.compareToIgnoreCase("BL") == 0 || sVisible.compareToIgnoreCase("SN") == 0) nRange = 6;
        else if (sVisible.compareToIgnoreCase("FU") == 0) nRange = 7;
        else if (sVisible.compareToIgnoreCase("SS") == 0 || sVisible.compareToIgnoreCase("DS") == 0 || sVisible.compareToIgnoreCase("PO") == 0 ||
                sVisible.compareToIgnoreCase("DU") == 0 || sVisible.compareToIgnoreCase("SA") == 0) nRange = 10;

        if (sIntensity.compareTo("+") == 0)
            ++nRange;
        if (sIntensity.compareTo("-") == 0)
            --nRange;

        return nRange;
    }


    @Override
    public void clearValues() {
        for (int nI = 0; nI < m_nArrayLength; ++nI) {
            m_sWeather[nI][0] = sNO_DATA;
            m_sWeather[nI][1] = sNO_DATA;
        }
        m_nPrecipitationSituation = nNO_DATA;
        m_nVisibilitySituation = nNO_DATA;
        m_nGroupCount = 0;
        m_nObsCount = 0;
    }


    @Override
    public String toString() {
        String sReturnString = "";
        for (int nIndex = 0; nIndex < m_nArrayLength; ++nIndex) {
            if (m_sWeather[nIndex][1].compareTo(sNO_DATA) == 0)
                break;
            if (m_sWeather[nIndex][0].compareTo("") != 0)
                sReturnString += "," + m_sWeather[nIndex][0] + m_sWeather[nIndex][1];
            else
                sReturnString += "," + m_sWeather[nIndex][1];
        }
        return sReturnString;
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("PrecipitationSituation,VisibilitySituation");
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_nPrecipitationSituation != nNO_DATA)
            oOut.write(Integer.toString(m_nPrecipitationSituation));
        oOut.write(",");
        if (m_nVisibilitySituation != nNO_DATA)
            oOut.write(Integer.toString(m_nVisibilitySituation));
        if (!bLast)
            oOut.write(",");
    }


}

