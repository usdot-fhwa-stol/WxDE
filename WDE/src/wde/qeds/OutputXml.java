// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file OutputXml.java
 */
package wde.qeds;

import wde.dao.QualityFlagDao;
import wde.util.Introsort;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;

/**
 * Provides methods to output .xml formatted observation data to a provided
 * output stream.
 * <p/>
 * <p>
 * Extends {@code OutputCsv} to get common header data, as well as non-format
 * specific methods. Overrides all format specific methods.
 * </p>
 *
 * @see OutputXml#fulfill(PrintWriter, ArrayList,
 * Subscription, String, int, long)
 */
public class OutputXml extends OutputCsv {
    /**
     * Tracks current indentation level.
     */
    private int m_nIndentLevel;

    /**
     * Contains the header string after comma delimiters have been removed.
     */
    private String[] m_sHeaders;
    /**
     * Used as a stack to keep the order in which tag sections are started, to
     * ensure they are closed in the correct order.
     */
    private ArrayDeque<String> m_oSections = new ArrayDeque<String>();


    /**
     * Sets the file extension to .xml, and splits the header around the comma
     * delimiter.
     */
    OutputXml() {
        m_sSuffix = ".xml";
        m_sHeaders = m_sHeader.split(",");
    }


    /**
     * Prints {@code SubObs} data to the provided output stream. First prints
     * the defined header, then traverses the provided {@code SubObs} list,
     * printing the contained data in .xml format.
     * <p/>
     * <p>
     * Overrides base class implementation.
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
    @Override
    void fulfill(PrintWriter oWriter, ArrayList<SubObs> oSubObsList,
                 Subscription oSub, String sFilename, int nId, long lLimit, boolean matchCheck) {
        Introsort.usort(oSubObsList, this);

        try {
            if (oSubObsList.size() == 0) {
                oWriter.println("No records found");
                return;
            }

            // output the header information
            oWriter.println("SourceId, Quality Flags");
            ArrayList<String> qualityFlags = QualityFlagDao.getInstance().getQualityFlagStringArray();
            for (String qf : qualityFlags) {
                oWriter.println(qf);
            }

            oWriter.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");

            startTag(oWriter, "wxde");
            if (sFilename != null) {
                startTag(oWriter, "observations", "subscriptionId",
                        Integer.toString(nId), "filename", sFilename);
            } else
                startTag(oWriter, "observations");

            for (int nIndex = 0; nIndex < oSubObsList.size(); nIndex++) {
                SubObs oSubObs = oSubObsList.get(nIndex);
                // obs must match the time range and filter criteria
                if (oSubObs.recvTime < lLimit || !oSub.matches(oSubObs, matchCheck))
                    continue;

                doIndent(oWriter);
                oWriter.print("<obs");

                oWriter.print(" SourceId=\"");
                oWriter.print(oSubObs.sourceId);
                oWriter.print("\"");

                oWriter.print(" ObsTypeID=\"");
                oWriter.print(oSubObs.m_nObsTypeId);
                oWriter.print("\"");

                oWriter.print(" ObsTypeName=\"");
                oWriter.print(oSubObs.m_iObsType.getObsType());
                oWriter.print("\"");

                oWriter.print(" SensorID=\"");
                oWriter.print(oSubObs.m_nSensorId);
                oWriter.print("\"");

                oWriter.print(" SensorIndex=\"");
                oWriter.print(oSubObs.m_iSensor.getSensorIndex());
                oWriter.print("\"");

                oWriter.print(" StationID=\"");
                oWriter.print(oSubObs.m_iSensor.getPlatformId());
                oWriter.print("\"");

                oWriter.print(" SiteID=\"");
                oWriter.print(oSubObs.m_iPlatform.getSiteId());
                oWriter.print("\"");

                oWriter.print(" Category=\"");
                oWriter.print(oSubObs.m_iPlatform.getCategory());
                oWriter.print("\"");

                oWriter.print(" ContribID=\"");
                oWriter.print(oSubObs.m_oContrib.getId());
                oWriter.print("\"");

                oWriter.print(" Contributor=\"");
                oWriter.print(oSubObs.m_oContrib.getName());
                oWriter.print("\"");

                oWriter.print(" StationCode=\"");
                oWriter.print(oSubObs.m_iPlatform.getPlatformCode());
                oWriter.print("\"");

                oWriter.print(" Timestamp=\"");
                oWriter.print(m_oDateFormat.format(oSubObs.m_lTimestamp));
                oWriter.print("\"");

                oWriter.print(" Latitude=\"");
                oWriter.print(oSubObs.m_dLat);
                oWriter.print("\"");

                oWriter.print(" Longitude=\"");
                oWriter.print(oSubObs.m_dLon);
                oWriter.print("\"");

                oWriter.print(" Elevation=\"");
                oWriter.print(oSubObs.m_nElev);
                oWriter.print("\"");

                oWriter.print(" Observation=\"");
                oWriter.print(m_oDecimal.format(oSubObs.m_dValue));
                oWriter.print("\"");

                oWriter.print(" Units=\"");
                oWriter.print(oSubObs.m_iObsType.getObsInternalUnit());
                oWriter.print("\"");

                oWriter.print(" EnglishValue=\"");
                oWriter.print(m_oDecimal.format(oSubObs.m_dEnglishValue));
                oWriter.print("\"");

                oWriter.print(" EnglishUnits=\"");
                oWriter.print(oSubObs.m_iObsType.getObsEnglishUnit());
                oWriter.print("\"");

                oWriter.print(" ConfValue=\"");
                oWriter.print(m_oDecimal.format(oSubObs.m_fConfidence));
                oWriter.print("\"");

                oWriter.print(" Flags=\"");
                outputQch(oWriter, oSubObs.m_nFlags);

                oWriter.println("\">");
            }

            endTag(oWriter);
            endTag(oWriter);
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }


    /**
     * Prints quality checking data to the supplied output stream in xml
     * format:
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
    @Override
    protected void outputQch(PrintWriter oWriter, char[] nFlags) {
        // now generate the P,N,/,- output
        if (nFlags == null) return;

        // now generate the P,N,/,- output
        for (int i = 0; i < nFlags.length; i++) {
            oWriter.print(nFlags[i]);

            // output the trailing comma
            if (i < nFlags.length - 1) oWriter.print(",");
        }
    }


    /**
     * Prints a start tag at the current indent level, marking the beginning of
     * a new section. The section tag name is then pushed onto the Deque
     * ({@code m_oSections}).
     * <p/>
     * <p>
     * This method must be given at least one string representing the start
     * tag identifier. Any given pairs of strings thereafter will be used as
     * attributes.
     * </p>
     * <p>
     * Tags will be of the form:
     * <blockquote>
     * &lt sSection sAttr[0]=sAttr[1] ... sAttr[n-2]=sAttr[n-1]>
     * </blockquote>
     * </p>
     *
     * @param oWriter  output stream, ready to write data prior to a call to this
     *                 method.
     * @param sSection start tag name.
     * @param sAttr    attribute-value pairs, can define any number of these, as
     *                 long as they come in pairs.
     */
    protected void startTag(PrintWriter oWriter, String sSection,
                            String... sAttr) {
        doIndent(oWriter);
        m_nIndentLevel++;

        oWriter.print("<");
        oWriter.print(sSection);

        for (int nIndex = 0; nIndex < sAttr.length; ) {
            oWriter.print(" ");
            oWriter.print(sAttr[nIndex++]);
            oWriter.print("=\"");
            oWriter.print(sAttr[nIndex++]);
            oWriter.print("\"");
        }
        oWriter.println(">");

        m_oSections.push(sSection);
    }


    /**
     * Removes one indent level, and prints the end tag corresponding to the
     * last start tag written, and the end of that section.
     *
     * @param oWriter the output stream, connected prior to a call to this
     *                method.
     */
    protected void endTag(PrintWriter oWriter) {
        m_nIndentLevel--;
        doIndent(oWriter);

        oWriter.print("</");
        oWriter.print(m_oSections.pop());
        oWriter.println(">");
    }


    /**
     * Writes tabs to the supplied output stream corresponding to the current
     * indent level.
     *
     * @param oWriter output stream, connected and ready for input prior to a
     *                call to this method.
     */
    protected void doIndent(PrintWriter oWriter) {
        int nIndentLevel = m_nIndentLevel;
        while (nIndentLevel-- > 0)
            oWriter.print("\t");
    }
}
