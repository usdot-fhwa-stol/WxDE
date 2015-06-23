// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file OutputXml.java
 */
package wde.qeds;

import wde.dao.QualityFlagDao;
import wde.metadata.IPlatform;
import wde.util.Introsort;

import java.io.PrintWriter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Provides methods to output .kml formatted observation data to a provided
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
public class OutputKml extends OutputCsv {
    /**
     * Tracks current indention level.
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
    OutputKml() {
        m_sSuffix = ".kml";
        m_sHeaders = m_sHeader.split(",");
        m_sHeader = "SourceId,ObsTypeID,ObsTypeName,SensorIndex,Timestamp,Observation,Units," +
                "EnglishValue,EnglishUnits,ConfValue,Complete,Manual,Sensor_Range," +
                "Climate_Range,Step,Like_Instrument,Persistence,IQR_Spatial," +
                "Barnes_Spatial,Dew_Point,Sea_Level_Pressure,Precip_Accum";
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
     * @param lLimit      timestamp lower bound. All observations with received or
     *                    completed date less than this limit will not be printed.
     */
    @Override
    void fulfill(PrintWriter oWriter, ArrayList<SubObs> oSubObsList,
                 Subscription oSub, String sFilename, int nId, long lLimit, boolean matchCheck) {
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

        Introsort.usort(oSubObsList, this);
        // group observations back to owning stations
        ArrayList<PlatformGroup> oPlatformGroups = new ArrayList<PlatformGroup>();
        for (int nIndex = 0; nIndex < oSubObsList.size(); nIndex++) {
            SubObs oSubObs = oSubObsList.get(nIndex);
            // obs must match the time range and filter criteria
            if (oSubObs.recvTime < lLimit || !oSub.matches(oSubObs, matchCheck))
                continue;

            // attempt to find station in list
            int nStationIndex = Collections.
                    binarySearch(oPlatformGroups, oSubObs.m_iPlatform);

            PlatformGroup oPlatformGroup = null;
            // create new station group when one is not found
            if (nStationIndex < 0) {
                oPlatformGroup = new PlatformGroup(oSubObs.m_iPlatform);
                oPlatformGroups.add(~nStationIndex, oPlatformGroup);
            } else
                oPlatformGroup = oPlatformGroups.get(nStationIndex);

            oPlatformGroup.add(oSubObs); // save the observation to the station
            if (oPlatformGroup.m_sContribName == null)
                oPlatformGroup.m_sContribName = oSubObs.m_oContrib.m_sName;
        }

        try {
            oWriter.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            oWriter.println("<kml xmlns=\"http://www.opengis.net/kml/2.2\">");

            startTag(oWriter, "Document");

            for (int nIndex = 0; nIndex < oPlatformGroups.size(); nIndex++) {
                PlatformGroup oPlatformGroup = oPlatformGroups.get(nIndex);

                startTag(oWriter, "Placemark");

                startTag(oWriter, "name");
                doIndent(oWriter);
                oWriter.println(oPlatformGroup.m_iPlatform.getPlatformCode());
                endTag(oWriter); // name

                writeDescription(oWriter, oPlatformGroup);

                startTag(oWriter, "Point");
                startTag(oWriter, "coordinates");

                doIndent(oWriter);
                oWriter.print(oPlatformGroup.m_iPlatform.getLocBaseLong());
                oWriter.print(",");
                oWriter.print(oPlatformGroup.m_iPlatform.getLocBaseLat());
                oWriter.print(",");
                oWriter.println(oPlatformGroup.m_iPlatform.getLocBaseElev());

                endTag(oWriter); // coordinates
                endTag(oWriter); // Point

                endTag(oWriter); // Placemark
            }
            endTag(oWriter); // Document
            oWriter.println("</kml>");
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }


    /**
     * Formats station observations into output compatible with KML
     *
     * @param oWriter       the output stream, connected prior to a call to this
     *                      method.
     * @param oStationGroup the set of observations to format.
     */
    private void writeDescription(PrintWriter oWriter, PlatformGroup oPlatformGroup) {
        startTag(oWriter, "description");

        doIndent(oWriter); // write output to a CDATA section
        oWriter.println("<![CDATA[");

        oWriter.print(oPlatformGroup.m_sContribName); // write station info
        oWriter.println("<br/>");
        oWriter.print(oPlatformGroup.m_iPlatform.getDescription());
        oWriter.println("<br/>");
        oWriter.print(oPlatformGroup.m_iPlatform.getLocBaseLat());
        oWriter.print(",");
        oWriter.print(oPlatformGroup.m_iPlatform.getLocBaseLong());
        oWriter.print(",");
        oWriter.print(oPlatformGroup.m_iPlatform.getLocBaseElev());
        oWriter.println("<br/>");
        oWriter.println("<br/>");

        oWriter.print(m_sHeader); // write csv header line
        oWriter.println("<br/>");
        oWriter.println("<br/>");
        for (int nIndex = 0; nIndex < oPlatformGroup.size(); nIndex++) {
            SubObs oSubObs = oPlatformGroup.get(nIndex);
            writeObs(oWriter, oSubObs);
        }
        doIndent(oWriter); // end CDATA
        oWriter.println("]]>");

        endTag(oWriter); // description
    }


    protected void writeObs(PrintWriter oWriter, SubObs oSubObs) {
        oWriter.print(oSubObs.sourceId);
        oWriter.print(",");
        oWriter.print(oSubObs.m_nObsTypeId);
        oWriter.print(",");
        oWriter.print(oSubObs.m_iObsType.getObsType());
        oWriter.print(",");
        oWriter.print(oSubObs.m_iSensor.getSensorIndex());
        oWriter.print(",");
        oWriter.print(m_oDateFormat.format(oSubObs.m_lTimestamp));
        oWriter.print(",");
        oWriter.print("<font color=\"#FF0000\">");
        oWriter.print(m_oDecimal.format(oSubObs.m_dValue));
        oWriter.print("</font>");
        oWriter.print(",");
        oWriter.print(oSubObs.m_iObsType.getObsInternalUnit());
        oWriter.print(",");
        oWriter.print("<font color=\"#FF0000\">");
        oWriter.print(m_oDecimal.format(oSubObs.m_dEnglishValue));
        oWriter.print("</font>");
        oWriter.print(",");
        oWriter.print(oSubObs.m_iObsType.getObsEnglishUnit());
        oWriter.print(",");
        oWriter.print(m_oDecimal.format(oSubObs.m_fConfidence));
        oWriter.print(",");
        outputQch(oWriter, oSubObs.m_nFlags);
        oWriter.println("<br/>");
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


    /**
     * Accumulates subscription observations back to a station to support
     * the KML hierarchy for data display
     */
    private class PlatformGroup extends ArrayList<SubObs>
            implements Comparable<IPlatform> {
        private String m_sContribName;
        private IPlatform m_iPlatform;


        private PlatformGroup() {
        }

        PlatformGroup(IPlatform iPlatform) {
            m_iPlatform = iPlatform;
        }


        public int compareTo(IPlatform oRhs) {
            return (m_iPlatform.getId() - oRhs.getId());
        }
    }
}
