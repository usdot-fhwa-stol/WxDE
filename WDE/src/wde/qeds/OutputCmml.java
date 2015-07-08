// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file OutputCmml.java
 */
package wde.qeds;

import wde.dao.QualityFlagDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.metadata.QualityFlag;
import wde.util.Introsort;

import java.io.PrintWriter;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimeZone;

/**
 * Provides methods to output .cmml formatted observation data to a provided
 * output stream.
 * <p/>
 * <p>
 * Extends {@code OutputXml} to get common header data, as well as common .xml,
 * .cmml formatting methods. {@code OutputCmml} adds, and overrides some format
 * specific methods.
 * </p>
 *
 * @see OutputCmml#fulfill(PrintWriter, ArrayList,
 * Subscription, String, int, long)
 */
public class OutputCmml extends OutputXml {
    /**
     * {@code CmmlType} map list.
     */
    private ArrayList<CmmlType> m_oCmmlMapping = new ArrayList<CmmlType>();


    /**
     * Sets file suffix to .cmml, and initializes both the timestamp format, and
     * timezone. Also populates the sorted cmml map with {@code CmmlType}
     * objects.
     */
    OutputCmml() {
        m_sSuffix = ".cmml";
        m_oDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'-00:00'");
        m_oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        m_oCmmlMapping.add(new CmmlType(204, "ice-percent", "pavement"));
        m_oCmmlMapping.add(new CmmlType(205, "precip-10-min", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(206, "precip-intensity", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(207, "precip-type", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(541, "ess-latitude", "extension"));
        m_oCmmlMapping.add(new CmmlType(576, "ess-max-temp", "temperature"));
        m_oCmmlMapping.add(new CmmlType(577, "ess-min-temp", "temperature"));
        m_oCmmlMapping.add(new CmmlType(542, "ess-longitude", "extension"));
        m_oCmmlMapping.add(new CmmlType(592, "ess-total-sun", "radiation"));
        m_oCmmlMapping.add(new CmmlType(587, "ess-precip-rate", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(543, "ess-vehicle-speed", "extension"));
        m_oCmmlMapping.add(new CmmlType(574, "ess-wet-bulb-temp", "temperature"));
        m_oCmmlMapping.add(new CmmlType(575, "ess-dewpoint-temp", "temperature"));
        m_oCmmlMapping.add(new CmmlType(586, "ess-precip-yes-no", "weather"));
        m_oCmmlMapping.add(new CmmlType(5141, "ess-co", "air-quality"));
        m_oCmmlMapping.add(new CmmlType(5143, "ess-no", "air-quality"));
        m_oCmmlMapping.add(new CmmlType(5146, "ess-o3", "air-quality"));
        m_oCmmlMapping.add(new CmmlType(5142, "ess-co2", "air-quality"));
        m_oCmmlMapping.add(new CmmlType(5144, "ess-no2", "air-quality"));
        m_oCmmlMapping.add(new CmmlType(5145, "ess-so2", "air-quality"));
        m_oCmmlMapping.add(new CmmlType(5101, "ess-visibility", "visibility"));
        m_oCmmlMapping.add(new CmmlType(5810, "ess-ice-thickness", "pavement"));
        m_oCmmlMapping.add(new CmmlType(51137, "ess-surface-status", "pavement"));
        m_oCmmlMapping.add(new CmmlType(544, "ess-vehicle-bearing", "extension"));
        m_oCmmlMapping.add(new CmmlType(593, "ess-cloud-situation", "extension"));
        m_oCmmlMapping.add(new CmmlType(596, "ess-total-radiation", "radiation"));
        m_oCmmlMapping.add(new CmmlType(5121, "ess-mobile-friction", "pavement"));
        m_oCmmlMapping.add(new CmmlType(5733, "ess-air-temperature", "temperature"));
        m_oCmmlMapping.add(new CmmlType(545, "ess-vehicle-odometer", "extension"));
        m_oCmmlMapping.add(new CmmlType(551, "ess-reference-height", "extension"));
        m_oCmmlMapping.add(new CmmlType(589, "ess-precip-situation", "weather"));
        m_oCmmlMapping.add(new CmmlType(511311, "ess-surface-salinity", "pavement"));
        m_oCmmlMapping.add(new CmmlType(581, "ess-relative-humidity", "humidity"));
        m_oCmmlMapping.add(new CmmlType(56104, "wind-sensor-avg-speed", "wind"));
        m_oCmmlMapping.add(new CmmlType(561010, "wind-sensor-situation", "wind"));
        m_oCmmlMapping.add(new CmmlType(584, "ess-roadway-snow-depth", "snow"));
        m_oCmmlMapping.add(new CmmlType(56106, "wind-sensor-spot-speed", "wind"));
        m_oCmmlMapping.add(new CmmlType(56108, "wind-sensor-gust-speed", "wind"));
        m_oCmmlMapping.add(new CmmlType(583, "ess-adjacent-snow-depth", "snow"));
        m_oCmmlMapping.add(new CmmlType(588, "ess-snowfall-accum-rate", "snow"));
        m_oCmmlMapping.add(new CmmlType(51138, "ess-surface-temperature", "temperature"));
        m_oCmmlMapping.add(new CmmlType(51334, "ess-percent-product-mix", "pavement"));
        m_oCmmlMapping.add(new CmmlType(554, "ess-atmospheric-pressure", "pressure"));
        m_oCmmlMapping.add(new CmmlType(5102, "ess-visibility-situation", "visibility"));
        m_oCmmlMapping.add(new CmmlType(5135, "ess-pave-treatment-width", "pavement"));
        m_oCmmlMapping.add(new CmmlType(51139, "ess-pavement-temperature", "pavement"));
        m_oCmmlMapping.add(new CmmlType(51166, "ess-sub-surface-moisture", "subsurface"));
        m_oCmmlMapping.add(new CmmlType(511313, "ess-surface-freeze-point", "pavement"));
        m_oCmmlMapping.add(new CmmlType(5134, "ess-pave-treatment-amount", "pavement"));
        m_oCmmlMapping.add(new CmmlType(56105, "wind-sensor-avg-direction", "wind"));
        m_oCmmlMapping.add(new CmmlType(511315, "ess-pavement-sensor-error", "pavement"));
        m_oCmmlMapping.add(new CmmlType(585, "ess-roadway-snowpack-depth", "snow"));
        m_oCmmlMapping.add(new CmmlType(597, "ess-total-radiation-period", "radiation"));
        m_oCmmlMapping.add(new CmmlType(5812, "ess-precipitation-end-time", "weather"));
        m_oCmmlMapping.add(new CmmlType(5813, "ess-precipitation-one-hour", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(5817, "ess-precipitation-24-hours", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(56107, "wind-sensor-spot-direction", "wind"));
        m_oCmmlMapping.add(new CmmlType(56109, "wind-sensor-gust-direction", "wind"));
        m_oCmmlMapping.add(new CmmlType(58212, "water-level-sensor-reading", "extension"));
        m_oCmmlMapping.add(new CmmlType(5815, "ess-precipitation-six-hours", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(51165, "ess-sub-surface-temperature", "subsurface"));
        m_oCmmlMapping.add(new CmmlType(51332, "ess-pave-treat-product-type", "pavement"));
        m_oCmmlMapping.add(new CmmlType(51333, "ess-pave-treat-product-form", "pavement"));
        m_oCmmlMapping.add(new CmmlType(511317, "ess-surface-conductivity-v2", "pavement"));
        m_oCmmlMapping.add(new CmmlType(5811, "ess-precipitation-start-time", "weather"));
        m_oCmmlMapping.add(new CmmlType(51167, "ess-sub-surface-sensor-error", "subsurface"));
        m_oCmmlMapping.add(new CmmlType(511314, "ess-surface-black-ice-signal", "pavement"));
        m_oCmmlMapping.add(new CmmlType(5814, "ess-precipitation-three-hours", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(5816, "ess-precipitation-twelve-hours", "precipitation"));
        m_oCmmlMapping.add(new CmmlType(511316, "ess-surface-ice-or-water-depth", "pavement"));
        m_oCmmlMapping.add(new CmmlType(5123, "ess-mobile-observation-pavement", "pavement"));
        m_oCmmlMapping.add(new CmmlType(595, "ess-instantaneous-solar-radiation", "radiation"));
        m_oCmmlMapping.add(new CmmlType(511319, "pavement-sensor-temperature-depth", "pavement"));
        m_oCmmlMapping.add(new CmmlType(5122, "ess-mobile-observation-ground-state", "weather"));

        Collections.sort(m_oCmmlMapping);
    }


    /**
     * Prints {@code SubObs} data to the provided output stream. First prints
     * the defined header, then traverses the provided {@code SubObs} list,
     * printing the contained data in .cmml format.
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
        Introsort.usort(oSubObsList, this);
        CmmlType oCmmlTypeSearch = new CmmlType();

        try {
            if (oSubObsList.size() == 0) {
                oWriter.println("No records found");
                return;
            }

            // output the header information
            oWriter.println("SourceId, Quality Flags");
            ArrayList<String> qualityFlagStrs = QualityFlagDao.getInstance().getQualityFlagStringArray();
            for (String qf : qualityFlagStrs) {
                oWriter.println(qf);
            }
            ArrayList<QualityFlag> qualityFlags = QualityFlagDao.getInstance().getQualityFlags();

            oWriter.println("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>");
            startTag(oWriter, "cmml", "version", "2.01");

            // writer header section
            startTag(oWriter, "head");

            startTag(oWriter, "product");

            if (sFilename != null) {
                addElement(oWriter, "title",
                        "WxDE System Observations" + " -- " +
                                nId + ":" + sFilename);
            } else
                addElement(oWriter, "title", "WxDE System Observations");

            addElement(oWriter, "field", "meteorological");
            addElement(oWriter, "category", "observation");
            addElement(oWriter, "creation-date", m_oDateFormat.format(System.currentTimeMillis()));
            endTag(oWriter); // product

            startTag(oWriter, "source");
            addElement(oWriter, "production-center", "Leidos");
            endTag(oWriter); // source

            endTag(oWriter); // head

            startTag(oWriter, "data");

            int nObsCount = 0;
            int nPlatformId = 0;
            for (int nIndex = 0; nIndex < oSubObsList.size(); nIndex++) {
                SubObs oSubObs = oSubObsList.get(nIndex);
                // obs must match the time range and filter criteria
                if (oSubObs.recvTime < lLimit || !oSub.matches(oSubObs, matchCheck))
                    continue;

                // only add the obs information if a valid cmml mapping exists
                oCmmlTypeSearch.m_nObsTypeId = oSubObs.m_nObsTypeId;
                int nCmmlIndex = Collections.binarySearch(m_oCmmlMapping, oCmmlTypeSearch);

                if (nCmmlIndex < 0)
                    continue;

                CmmlType oCmmlType = m_oCmmlMapping.get(nCmmlIndex);
                IPlatform iPlatform = oSubObs.m_iPlatform;

                if (iPlatform.getId() != nPlatformId) {
                    if (nPlatformId > 0)
                        endTag(oWriter); // observation-series

                    nPlatformId = iPlatform.getId();

                    // start a new observation-series for this contributor
                    startTag(oWriter, "observation-series");

                    startTag(oWriter, "origin", "type", "station");
                    addElement(oWriter, "id",
                            Integer.toString(oSubObs.m_oContrib.m_nId),
                            "type", "contributor-id");

                    addElement(oWriter, "id", Integer.toString(nPlatformId),
                            "type", "station-id");

                    addElement(oWriter, "id", iPlatform.getPlatformCode(),
                            "type", "station-code");

                    addElement(oWriter, "id",
                            Integer.toString(iPlatform.getSiteId()),
                            "type", "site-id");

                    addElement(oWriter, "id",
                            iPlatform.getPlatformCode(),
                            "type", "category");

                    endTag(oWriter); // origin

                    startTag(oWriter, "location");
                    addElement(oWriter, "latitude",
                            Double.toString(oSubObs.m_dLat));

                    addElement(oWriter, "longitude",
                            Double.toString(oSubObs.m_dLon));

                    addElement(oWriter, "elevation",
                            Integer.toString(oSubObs.m_nElev), "units", "m");

                    endTag(oWriter); // location
                }

                // add obs information and track how many obs are formatted
                ++nObsCount;
                startTag(oWriter, "observation", "valid-time",
                        m_oDateFormat.format(oSubObs.m_lTimestamp));

                ISensor iSensor = oSubObs.m_iSensor;
                startTag(oWriter, oCmmlType.m_sCategory,
                        "type", oCmmlType.m_sObsTypeName,
                        "index", Integer.toString(iSensor.getSensorIndex()));

                addElement(oWriter, "value",
                        m_oDecimal.format(oSubObs.m_dValue),
                        "units", oSubObs.m_iObsType.getObsInternalUnit());

                addElement(oWriter, "qualifier",
                        Integer.toString(oSubObs.sourceId),
                        "type", "source-id");

                addElement(oWriter, "qualifier",
                        Integer.toString(oSubObs.m_nObsTypeId),
                        "type", "observation-type");

                addElement(oWriter, "qualifier",
                        Integer.toString(iSensor.getId()),
                        "type", "sensor-id");

                addElement(oWriter, "qualifier",
                        m_oDecimal.format(oSubObs.m_dEnglishValue),
                        "type", "english-value",
                        "units", oSubObs.m_iObsType.getObsEnglishUnit());

                addElement(oWriter, "qualifier",
                        m_oDecimal.format(oSubObs.m_fConfidence),
                        "type", "confidence-value");

                outputQch2(oWriter, oSubObs, qualityFlags);

                endTag(oWriter); // oCmmlType.m_sCategory
                endTag(oWriter); // observation
            }

            if (nObsCount > 0)
                endTag(oWriter); // final observation-series tag

            endTag(oWriter); // data
            endTag(oWriter); // cmml
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }

    /**
     * Outputs quality check data in .cmml format to the supplied output stream.
     * Converts the provided bit-fields to .cmml element strings, that are
     * printed within a start-tag end-tag section.
     *
     * @param oWriter    output stream. Connected prior to a call to this method.
     * @param nRunFlags  bit field corresponding to the quality checking
     *                   algorithms ran.
     * @param nPassFlags pass/fail bit field corresponding to the quality
     *                   checking algorithm ran.
     */
    protected void outputQch2(PrintWriter oWriter, SubObs subObs, ArrayList<QualityFlag> qualityFlagList) {
        // output "Passed", "Not passed", "Not run", "Not applicable" states
        int nQchIndex = 0;
        char[] nFlags = subObs.m_nFlags;
        String[] qualityFlags = getQualityCheckFlags(subObs, qualityFlagList);
        for (int i = 0; i < nFlags.length; i++) {
            startTag(oWriter, "qch", "performer", qualityFlags[nQchIndex++]);

            switch (nFlags[i]) {
                case '/':
                    addElement(oWriter, "summary", "Not applicable");
                    break;
                case '-':
                    addElement(oWriter, "summary", "Not run");
                    break;
                case 'N':
                    addElement(oWriter, "summary", "Not passed");
                    break;
                case 'P':
                    addElement(oWriter, "summary", "Passed");
                    break;
            }

            endTag(oWriter); // qch		    
        }
    }

    private String[] getQualityCheckFlags(SubObs subObs, ArrayList<QualityFlag> qualityFlags) {
        long obsTime = subObs.m_lTimestamp;
        int sourceId = subObs.sourceId;

        for (QualityFlag qf : qualityFlags) {
            if (sourceId != qf.getSourceId())
                continue;

            if (obsTime < qf.getUpdateTime().getTime())
                continue;

            Timestamp toTime = qf.getToTime();

            if (toTime == null || obsTime < toTime.getTime())
                return qf.getQchFlagLabel();
        }

        return null;
    }

    /**
     * Prints an element that occurs between a start tag, and end tag in .cmml
     * files.
     *
     * @param oWriter  output stream, ready to write data prior to a call to this
     *                 method.
     * @param sTag     element tag name.
     * @param sContent element value.
     * @param sAttr    attribute-value pairs, can define any number of these, as
     *                 long as they come in pairs. Describes the content value.
     */
    protected void addElement(PrintWriter oWriter, String sTag,
                              String sContent, String... sAttr) {
        doIndent(oWriter);

        oWriter.print("<");
        oWriter.print(sTag);

        for (int nIndex = 0; nIndex < sAttr.length; ) {
            oWriter.print(" ");
            oWriter.print(sAttr[nIndex++]);
            oWriter.print("=\"");
            oWriter.print(sAttr[nIndex++]);
            oWriter.print("\"");
        }
        oWriter.print(">");

        oWriter.print(sContent);
        oWriter.print("</");
        oWriter.print(sTag);
        oWriter.println(">");
    }


    /**
     * Wraps the observation type (id and name) with the observation category.
     * Provides a means of comparing and ordering the objects based off the
     * observation type id.
     * <p/>
     * <p>
     * Implements {@code Comparable} to enforce an ordering on {@code CmmlType}
     * objects.
     * </p>
     */
    private class CmmlType implements Comparable<CmmlType> {
        /**
         * Observation type id.
         */
        int m_nObsTypeId;
        /**
         * Observation type name.
         */
        String m_sObsTypeName;
        /**
         * Observation category.
         */
        String m_sCategory;


        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code CmmlType}
         * </p>
         */
        protected CmmlType() {
        }


        /**
         * @param nObsTypeId
         * @param sObsTypeName
         * @param sCategory
         */
        public CmmlType(int nObsTypeId, String sObsTypeName, String sCategory) {
            m_nObsTypeId = nObsTypeId;
            m_sObsTypeName = sObsTypeName;
            m_sCategory = sCategory;
        }


        /**
         * Compares <i> this </i> to the provided {@code CmmlType} object by
         * observation type id.
         *
         * @param oCmmlTypeR {@code CmmlType} to compare to <i> this </i>
         * @return 0 if the records match by observation type id.
         */
        public int compareTo(CmmlType oCmmlTypeR) {
            return (m_nObsTypeId - oCmmlTypeR.m_nObsTypeId);
        }
    }
}
