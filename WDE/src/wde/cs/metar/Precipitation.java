package wde.cs.metar;

import java.io.FileWriter;
import java.util.Calendar;
import java.util.TimeZone;

/**
 * the precipitation class holds the preciptation reading from a METAR record.
 * It also contains the function to get the reading out of the record
 *
 * @author scot.lange
 */
public class Precipitation extends Report {
    AWOS m_oAWOS;
    double m_dRainfall = dNO_DATA;
    boolean m_bThree = false;
    boolean m_bSix = false;
    boolean m_bTwentyFour = false;
    boolean m_bOne = false;
    Precipitation(AWOS oAWOS) {
        m_oAWOS = oAWOS;
    }

    @Override
    public int parse(int nStartIndex, int nRecursiveCount) {
        int nGroupEnd = super.currentGroupEnd(nStartIndex);
        //The precipation group will start with either a 7 indicating that
        //it is precipitation over the last 24 hours, a P indicating that
        //it is preciptation over the last hour, or a 6 indicating that it is
        //either precipitation over the last 3 hours or last 6 hours (determined
        //by whether this is a 3 hour or 6 hour report)
        if (g_sBuffer.charAt(nStartIndex) != '7'
                && g_sBuffer.charAt(nStartIndex) != 'P'
                && g_sBuffer.charAt(nStartIndex) != '6') {
            return
                    super.tryNextGroup(nStartIndex, nRecursiveCount, this);
        }


        try {
            //Try to get the value of the rest of the group
            m_dRainfall = Double.parseDouble(super.substring(
                    nStartIndex + 1, nGroupEnd)) / 100;
            ++m_nObsCount;

            //Once the value stored is succesfully parsed, it is known to be
            //valid, so store the type.
            switch (g_sBuffer.charAt(nStartIndex)) {
                case '7':
                    m_bTwentyFour = true;
                    break;
                case '6': {
                    //See if this is a 3 or 6 hour report.
                    Calendar oCalendar =
                            Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    oCalendar.setTimeInMillis(m_oAWOS.getTimeStamp());
                    if ((Math.round(oCalendar.get(Calendar.MINUTE) / 60.0)
                            + oCalendar.get(Calendar.HOUR)) % 6 == 0)
                        m_bSix = true;
                    else
                        m_bThree = true;
                    break;
                }
                case 'P':
                    m_bOne = true;
                    break;
            }
        } catch (Exception oException) {
            //If it can't be parsed, it's not a valid precipitation reading,
            //so let another report try to parse it
            return nStartIndex;
        }
        return nGroupEnd + 1;
    }


    @Override
    public String getObservationType(int nObservationIndex) {
        if (m_bThree)
            return "3HourPrecipitation";
        else if (m_bSix)
            return "6HourPrecipitation";
        else if (m_bTwentyFour)
            return "24HourPrecipitation";
        else if (m_bOne)
            return "1HourPreciptiation";
        else
            return sNO_DATA;
    }


    @Override
    public double getObservationValue(int nObservationIndex) {
        return m_dRainfall;
    }


    @Override
    public void clearValues() {
        m_dRainfall = dNO_DATA;
        m_bThree = false;
        m_bThree = false;
        m_bTwentyFour = false;
        m_bOne = false;
        m_nObsCount = 0;
    }


    @Override
    public String toString() {
        if (m_dRainfall != dNO_DATA)
            return "," + Double.toString(m_dRainfall);
        else
            return "";
    }

    @Override
    public void toCSV(FileWriter oOut, boolean bLast) throws Exception {
        if (m_dRainfall == dNO_DATA) {
            oOut.write(",,,");
        } else {
            if (this.m_bThree)
                oOut.write(Double.toString(m_dRainfall) + ",,,");
            else if (this.m_bSix)
                oOut.write("," + Double.toString(m_dRainfall) + ",,");
            else if (this.m_bTwentyFour)
                oOut.write(",," + Double.toString(m_dRainfall) + ",");
            else if (this.m_bOne)
                oOut.write(",,," + Double.toString(m_dRainfall));

        }
        if (!bLast)
            oOut.write(",");
    }

    @Override
    public void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception {
        oOut.write("3HourPrecipitation,6HourPrecipitation,24HourPrecipitation,1HourPreciptiation");

        if (!bLast)
            oOut.write(",");
    }
}
