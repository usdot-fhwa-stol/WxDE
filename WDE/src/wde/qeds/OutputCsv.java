// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file OutputCsv.java
 */
package wde.qeds;

import wde.dao.QualityFlagDao;
import wde.util.Introsort;
import wde.util.QualityCheckFlagUtil;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

/**
 * Provides methods to format and output observation data to .csv files to
 * a provided output stream.
 * <p>
 * Extends {@code OutputFormat} to enforce a general output-format interface.
 * </p>
 *
 * @see OutputCsv#fulfill(PrintWriter, ArrayList,
 * Subscription, String, int, long)
 */
public class OutputCsv extends OutputFormat {
    /**
     * Decimal number format.
     */
    protected static DecimalFormat m_oDecimal = new DecimalFormat("0.000");
    /**
     * File header information format.
     * <blockquote>
     * SELECT <br />
     * ot.id, ot.obsType, se.id, se.sensorIndex, st.id, si.id, <br />
     * si.climateId, c.id, c.name, st.stationCode, o.timestamp, <br />
     * o.latitude, o.longitude, o.elevation, o.value, ot.obsInternalUnits,<br />
     * ot.obsEnglishUnits, o.confidence, o.runFlags, o.passedFlags <br />
     * </blockquote>
     * <blockquote>
     * FROM <br />
     * clarus_qedc.obs o, clarus_meta.obsType ot, clarus_meta.contrib, <br />
     * clarus_meta.sensor se, clarus_meta.station st, clarus_meta.site si <br />
     * </blockquote>
     * <blockquote>
     * WHERE (o.receivedmd > ? OR o.qchcompletems > ?)
     * </blockquote>
     * <blockquote>
     * AND o.obsType IN (,,,) <br />
     * AND o.longitude >= ? <br />
     * AND o.longitude <= ? <br />
     * AND o.latitude >= ? <br />
     * AND o.latitude <= ? <br />
     * AND c.id IN (,,,) <br />
     * AND st.id IN (,,,) <br />
     * AND ot.id = o.obsType <br />
     * AND se.id = o.sensorId <br />
     * AND se.distGroup = 2 <br />
     * AND st.id = se.stationId <br />
     * AND si.id = st.siteId <br />
     * ORDER BY c.name, st.stationCode, ot.obsType
     * </blockquote>
     */
    protected String m_sHeader = "SourceId,ObsTypeID,ObsTypeName," +
            "SensorID,SensorIndex,StationID,SiteID," +
            "Category,ContribID,Contributor,StationCode,Timestamp," +
            "Latitude,Longitude,Elevation,Observation,Units," +
            "EnglishValue,EnglishUnits,ConfValue," +
            "<Flags - see list below>";
    /**
     * Quality checking algorithm run flag buffer.
     */
    protected char[] m_cRunFlags;
    /**
     * Quality checking algorithm pass/fail flag buffer.
     */
    protected char[] m_cPassFlags;
    /**
     * Timestamp format.
     */
    protected SimpleDateFormat m_oDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");


    /**
     * Initializes the file suffix. Initializes date format timezone to UTC.
     */
    OutputCsv() {
        int nQchLength = getQchLength();
        m_cRunFlags = new char[nQchLength];
        m_cPassFlags = new char[nQchLength];
        m_sSuffix = ".csv";
        m_oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Determines the number of quality check tests to be displayed
     *
     * @return
     */
    protected int getQchLength() {
        // Assume the sourceId is always for WxDE
        return QualityCheckFlagUtil.getQcLength(1);
    }


    /**
     * Prints {@code SubObs} data to provided output stream. First prints the
     * defined header, then traverses the provided {@code SubObs} list, printing
     * the contained data in a .csv comma-delimited manner, of the format:
     * <p/>
     * <blockquote>
     * observation-type id, observation-type name, sensor id, sensor index,
     * station id, site id, climate id, contributor id, contributor name,
     * station code, observation timestamp, latitude, longitude, elevation,
     * observation value, units, english-unit value, english-units, confidence
     * level, quality check
     * </blockquote>
     * followed by a timestamp footer.
     * <p/>
     * <p>
     * Required for extension of {@link OutputFormat}.
     * </p>
     *
     * @param oWriter     output stream, connected, and ready to write data.
     * @param oSubObsList list of observations to print.
     * @param oSub        subscription - used for filtering.
     * @param sFilename   output filename to write in footer, can be specified as
     *                    null
     * @param nId         subscription id.
     * @param lLimit      timestamp lower bound. All observations with recieved or
     *                    completed date less than this limit will not be printed.
     */
    void fulfill(PrintWriter oWriter, ArrayList<SubObs> oSubObsList,
                 Subscription oSub, String sFilename, int nId, long lLimit, boolean matchCheck) {
        Introsort.usort(oSubObsList, this);

        try {
            if (oSubObsList.size() == 0) {
                oWriter.println("No records found");
                return;
            }

            // output the header information
            oWriter.println(m_sHeader);
            ArrayList<String> qualityFlags = QualityFlagDao.getInstance().getQualityFlagStringArray();
            for (String qf : qualityFlags) {
                oWriter.println(qf);
            }

            //oWriter.println("---BEGIN OF RECORDS---");

            // output the obs details
            for (int nIndex = 0; nIndex < oSubObsList.size(); nIndex++) {
                SubObs oSubObs = oSubObsList.get(nIndex);
                // obs must match the time range and filter criteria
                if (oSubObs.recvTime < lLimit || !oSub.matches(oSubObs, matchCheck))
                    continue;

                oWriter.print(oSubObs.sourceId);
                oWriter.print(",");
                oWriter.print(oSubObs.m_nObsTypeId);
                oWriter.print(",");
                oWriter.print(oSubObs.m_iObsType.getObsType());
                oWriter.print(",");
                oWriter.print(oSubObs.m_nSensorId);
                oWriter.print(",");
                oWriter.print(oSubObs.m_iSensor.getSensorIndex());
                oWriter.print(",");
                oWriter.print(oSubObs.m_iSensor.getPlatformId());
                oWriter.print(",");
                oWriter.print(oSubObs.m_iPlatform.getSiteId());
                oWriter.print(",");
                oWriter.print(oSubObs.m_iPlatform.getCategory());
                oWriter.print(",");
                oWriter.print(oSubObs.m_oContrib.getId());
                oWriter.print(",");
                oWriter.print(oSubObs.m_oContrib.getName());
                oWriter.print(",");
                oWriter.print(oSubObs.m_iPlatform.getPlatformCode());
                oWriter.print(",");
                oWriter.print(m_oDateFormat.format(oSubObs.m_lTimestamp));
                oWriter.print(",");
                oWriter.print(oSubObs.m_dLat);
                oWriter.print(",");
                oWriter.print(oSubObs.m_dLon);
                oWriter.print(",");
                oWriter.print(oSubObs.m_nElev);
                oWriter.print(",");
                oWriter.print(m_oDecimal.format(oSubObs.m_dValue));
                oWriter.print(",");
                oWriter.print(oSubObs.m_iObsType.getObsInternalUnit());
                oWriter.print(",");
                oWriter.print(m_oDecimal.format(oSubObs.m_dEnglishValue));
                oWriter.print(",");
                oWriter.print(oSubObs.m_iObsType.getObsEnglishUnit());
                oWriter.print(",");
                oWriter.print(m_oDecimal.format(oSubObs.m_fConfidence));
                oWriter.print(",");
                outputQch(oWriter, oSubObs.m_nFlags);
                oWriter.println();
            }

            // output the end of file
            oWriter.print("---END OF RECORDS---");

            if (sFilename != null) {
                oWriter.println();
                oWriter.print(" -- ");
                oWriter.print(nId);
                oWriter.print(":");
                oWriter.println(sFilename);
            }
        } catch (Exception oExp) {
            oExp.printStackTrace(System.out);
        }
    }


    /**
     * Updates the quality check flag buffers ({@code m_cRunFlags} and
     * {@code m_cPassFlags}) with the supplied run-flag, and pass-flag integer
     * values.
     *
     * @param nRunFlags  bit-field showing which quality checking algorithms
     *                   were ran.
     * @param nPassFlags bit-field showing whether the corresponding quality
     *                   check algoritm passed or failed.
     */
    protected void updateFlags(int nRunFlags, int nPassFlags) {
        // clear quality check flag buffers
        int nIndex = getQchLength();
        while (nIndex-- > 0)
            m_cRunFlags[nIndex] = m_cPassFlags[nIndex] = '0';

        // copy the binary character values to the flag arrays
        // first populate the run flag array
        String sFlags = Integer.toBinaryString(nRunFlags);
        int nDestIndex = getQchLength();
        int nSrcIndex = sFlags.length();
        nIndex = Math.min(nDestIndex, nSrcIndex);
        while (nIndex-- > 0)
            m_cRunFlags[--nDestIndex] = sFlags.charAt(--nSrcIndex);

        // then populate the pass flag array
        sFlags = Integer.toBinaryString(nPassFlags);
        nDestIndex = getQchLength();
        nSrcIndex = sFlags.length();
        nIndex = Math.min(nDestIndex, nSrcIndex);
        while (nIndex-- > 0)
            m_cPassFlags[--nDestIndex] = sFlags.charAt(--nSrcIndex);
    }


    /**
     * Prints quality checking data to the supplied output stream:
     * <blockquote>
     * / ... indicates Qch algorithm not applicable. <br />
     * - ... indicates Qch algorithm did not run. <br />
     * N ... indicates Qch algorithm did not pass. <br />
     * P ... indicates Qch algorithm passed. <br />
     * </blockquote>
     *
     * @param oWriter output stream to write quality checking data to. Connected
     *                prior to the call to this method.
     * @param nFlags  pass/fail field corresponding to the quality
     *                checking algorithm ran.
     */
    protected void outputQch(PrintWriter oWriter, char[] nFlags) {
        if (nFlags == null)
            return;

        // now generate the P,N,/,- output
        for (int i = 0; i < nFlags.length; i++) {
            oWriter.print(nFlags[i]);

            // output the trailing comma
            if (i < nFlags.length - 1)
                oWriter.print(",");
        }
    }


    /**
     * Compares the two {@code SubObs}.
     *
     * @param oSubObsL object to compare to {@code oSubObsR}.
     * @param oSubObsR object to compare to {@code oSubObsL}.
     * @return 0 if the {@code SubObs} match by contributor id, observation-type
     * id, and timestamp.
     */
    public int compare(SubObs oSubObsL, SubObs oSubObsR) {
        // sort the observations for neat output by contrib, obstype, timestamp
        int nCompare = oSubObsL.m_oContrib.m_nId - oSubObsR.m_oContrib.m_nId;
        if (nCompare != 0)
            return nCompare;

        nCompare = oSubObsL.m_nObsTypeId - oSubObsR.m_nObsTypeId;
        if (nCompare != 0)
            return nCompare;

        return ((int) (oSubObsL.m_lTimestamp - oSubObsR.m_lTimestamp));
    }
}
