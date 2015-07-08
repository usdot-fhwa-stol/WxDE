package wde.vdt.wde;

import wde.util.Text;
import wde.util.io.CharTokenizer;

import java.text.SimpleDateFormat;
import java.util.ArrayList;


class PlatformObs extends ArrayList<ObsLabel> implements Comparable<PlatformObs> {
    static final int OBS_TYPE_NAME = 2;
    static final int PLATFORM_ID = 5;
    static final int PLATFORM_CODE = 10;
    static final int TIMESTAMP = 11;
    static final int LATITUDE = 12;
    static final int LONGITUDE = 13;
    static final int ELEVATION = 14;
    static final int OBS_VALUE = 15;

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    long m_lTimestamp;
    int m_nId;
    String m_sCode;
    String m_sObsType;
    float[] m_fValues;


    PlatformObs() {
    }


    PlatformObs(PlatformObs oPlatform) {
        m_lTimestamp = oPlatform.m_lTimestamp;
        m_nId = oPlatform.m_nId;
        m_sCode = oPlatform.m_sCode;
        m_sObsType = oPlatform.m_sObsType;

        add(new ObsLabel("latitude", oPlatform.m_fValues[LATITUDE]));
        add(new ObsLabel("longitude", oPlatform.m_fValues[LONGITUDE]));
        add(new ObsLabel("elevation", oPlatform.m_fValues[ELEVATION]));
        add(new ObsLabel(m_sObsType, oPlatform.m_fValues[OBS_VALUE]));
    }


    boolean readRecord(CharTokenizer oTokenizer, StringBuilder sCol)
            throws Exception {
        if (m_fValues == null)
            m_fValues = new float[OBS_VALUE + 1];

        int nCol = -1; // reset column index counter
        while (oTokenizer.hasTokens()) {
            ++nCol;
            oTokenizer.nextToken(sCol);
            Text.removeWhitespace(sCol);
            if (sCol.indexOf("---END OF RECORDS") >= 0)
                return false;

            switch (nCol) {
                case OBS_TYPE_NAME:
                    m_sObsType = sCol.toString().intern();
                    break;

                case PLATFORM_CODE:
                    m_sCode = sCol.toString().intern();
                    break;

                case PLATFORM_ID:
                    m_nId = Text.parseInt(sCol);
                    break;

                case TIMESTAMP:
                    m_lTimestamp = DATE_FORMAT.parse(sCol.toString()).getTime();
                    break;

                case LATITUDE:
                case LONGITUDE:
                case ELEVATION:
                case OBS_VALUE: {
                    m_fValues[nCol] = (float) Text.parseDouble(sCol);
                }
            }
        }
        return true;
    }


    float getFloat(String sLabel) {
        int nIndex = size();
        while (nIndex-- > 0) {
            ObsLabel oObsLabel = get(nIndex);
            if (oObsLabel.m_sTypeName.compareTo(sLabel) == 0)
                return oObsLabel.m_fValue;
        }
        return Float.MAX_VALUE;
    }


    short getShort(String sLabel) {
        float fValue = getFloat(sLabel);
        if (fValue != Float.MAX_VALUE)
            return (short) fValue;

        return Short.MIN_VALUE;
    }


    @Override
    public int compareTo(PlatformObs oPlatform) {
        if (m_lTimestamp == oPlatform.m_lTimestamp)
            return (m_nId - oPlatform.m_nId);

        if (m_lTimestamp > oPlatform.m_lTimestamp)
            return 1;

        return -1;
    }
}
