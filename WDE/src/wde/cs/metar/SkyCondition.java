package wde.cs.metar;

import java.io.FileWriter;

/**
 * The SkyCondition class gets and holds the list of sky conditions from a
 * METAR report
 *
 * @author scot.lange
 */
public class SkyCondition extends Report {
    int m_nArrayLength = 20;
    String[] m_sConditions = new String[m_nArrayLength];
    int[] m_nHeights = new int[m_nArrayLength];
    String[] m_sClouds = new String[m_nArrayLength];

    SkyCondition() {
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        String[] sContractions = new String[]
                {"SKC", "CLR", "FEW", "SCT", "BKN", "OVC"};

        int nIndex = nStartIndex;

        //Keep track of how many groups have been found
        int nGroupCount = 0;

        //Keep track of whether or not a condition has been found on this
        //iteration
        boolean bMatchFound = true;
        while (bMatchFound) {
            bMatchFound = false;
            //Check for "Vertical Visibilyt" value
            if (g_sBuffer.charAt(nIndex) == 'V'
                    && g_sBuffer.charAt(nIndex + 1) == 'V') {
                m_sConditions[nGroupCount] = "VV";
                nIndex += 6;
                bMatchFound = true;
            } else // Check for all the 3 character values
                for (int nI = 0; nI < sContractions.length; ++nI) {
                    if (sContractions[nI].charAt(0) == g_sBuffer.charAt(nIndex)
                            && sContractions[nI].charAt(1)
                            == g_sBuffer.charAt(nIndex + 1)
                            && sContractions[nI].charAt(2)
                            == g_sBuffer.charAt(nIndex + 2)) {
                        m_sConditions[nGroupCount] = sContractions[nI];

                        if (nI > 1)
                            nIndex += 7;
                        else
                            nIndex += 4;
                        bMatchFound = true;
                        break;
                    }
                }
            if (bMatchFound) {
                //Get the height value
                m_nHeights[nGroupCount] =
                        super.parseInt(super.substring(nIndex - 4, nIndex - 1));
                //Check for cloud values
                if (g_sBuffer.charAt(nIndex - 1) == 'C'
                        && g_sBuffer.charAt(nIndex) == 'B') {
                    m_sClouds[nGroupCount] = "CB";
                    nIndex += 2;
                } else if (g_sBuffer.charAt(nIndex - 1) == 'T'
                        && g_sBuffer.charAt(nIndex) == 'C'
                        && g_sBuffer.charAt(nIndex + 1) == 'U') {
                    m_sClouds[nGroupCount] = "TCU";
                    nIndex += 3;
                } else
                    m_sClouds[nGroupCount] = sNO_DATA;

                ++nGroupCount;
                nIndex = super.nearestGroupStart(nIndex);
            }
        }

        if (nGroupCount > 0)
            return nIndex;
        else return super.tryNextGroup(nStartIndex, nRecursiveCount, this);
    }


    @Override
    public void clearValues() {
        for (int nI = 0; nI < m_nArrayLength; ++nI) {
            m_sConditions[nI] = sNO_DATA;
            m_sClouds[nI] = sNO_DATA;
            m_nHeights[nI] = nNO_DATA;
        }
    }


    @Override
    public double getObservationValue(int nObservationIndex) {
        return super.getObservationValue(nObservationIndex);
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        return super.getObservationType(nObservationIndex);
    }


    @Override
    public String toString() {
        String sReturnString = "";

        for (int nI = 0; nI < m_nArrayLength; ++nI) {
            if (m_sConditions[nI].compareTo(sNO_DATA) == 0)
                break;
            sReturnString += "," + m_sConditions[nI];
            if (m_nHeights[nI] != nNO_DATA)
                sReturnString += "," + m_nHeights[nI];
            if (m_sClouds[nI].compareTo(sNO_DATA) != 0)
                sReturnString += "," + m_sClouds[nI];
        }
        return sReturnString;
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {

    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {

    }


}