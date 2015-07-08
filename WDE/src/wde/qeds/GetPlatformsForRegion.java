// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file GetPlatformsForRegion.java
 */
package wde.qeds;

import wde.dao.ObsTypeDao;
import wde.dao.SensorDao;
import wde.dao.UnitConv;
import wde.dao.Units;
import wde.metadata.ISensor;
import wde.metadata.ObsType;
import wde.obs.IObs;
import wde.security.AccessControl;
import wde.util.MathUtil;
import wde.util.QualityCheckFlagUtil;
import wde.util.QualityCheckFlags;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;

/**
 * Provides a means of requesting data across an HTTP connection. Observation
 * data can be retrieved by observation type, and platform. Platforms are used
 * if they have a public distribution group.
 * <p>
 * Extends {@code HttpServlet} to enforce a standard interface for requesting
 * data, and responding to these data requests.
 * </p>
 */
public class GetPlatformsForRegion extends HttpServlet {
    private ArrayList<String> segStatisticsObsTypeDisplayList = new ArrayList<>();

    private boolean m_bDebug;
    /**
     * Observation timestamp.
     */
    private Date m_oDate = new Date();
    /**
     * Timestamp format.
     */
    private SimpleDateFormat m_oDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Reference to {@code Units} instance.
     */
    private Units m_oUnits = Units.getInstance();
    /**
     * Reference to {@code ObsTypes} instance.
     */
    private ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();
    /**
     * Reference to {@code Sensors} instance.
     */
    private SensorDao sensorDao = SensorDao.getInstance();
    /**
     * Reference to {@code PlatformMonitor} instance.
     */
    private PlatformMonitor m_oPlatformMonitor = PlatformMonitor.getInstance();

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code GetPlatformsForRegion}
     * </p>
     */
    public GetPlatformsForRegion() {
        m_oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    public void init(ServletConfig iConfig) {
        String sShowAll = iConfig.getInitParameter("debug");
        if (sShowAll != null && sShowAll.length() > 0)
            m_bDebug = Boolean.parseBoolean(sShowAll);

        segStatisticsObsTypeDisplayList.add("segAirTemperatureAvgCAN");
        segStatisticsObsTypeDisplayList.add("segAirTemperatureAvgESS");
        segStatisticsObsTypeDisplayList.add("segAtmosphericPressureAvgCAN");
        segStatisticsObsTypeDisplayList.add("segDewpointTempAvgESS");
        segStatisticsObsTypeDisplayList.add("segAtmosphericPressureAvgNearbyESS");
        segStatisticsObsTypeDisplayList.add("segPrecipitationOneHourAvgNearbyESS");
        segStatisticsObsTypeDisplayList.add("segVisibilityAvgNearbyESS");
        segStatisticsObsTypeDisplayList.add("segWindSensorAvgDirectionNearbyESS");
        segStatisticsObsTypeDisplayList.add("segWindSensorAvgSpeedNearbyESS");
        segStatisticsObsTypeDisplayList.add("segAntiLockBrakeStatusOnNum");
        segStatisticsObsTypeDisplayList.add("segBrakeAppliedStatusOnNum");
        segStatisticsObsTypeDisplayList.add("segFogLightOnNum");
        segStatisticsObsTypeDisplayList.add("segHazardSignalOnNum");
        segStatisticsObsTypeDisplayList.add("segHighBeamLightsOnNum");
        segStatisticsObsTypeDisplayList.add("segLowBeamLightsOnNum");
        segStatisticsObsTypeDisplayList.add("segSpeedAvgCAN");
        segStatisticsObsTypeDisplayList.add("segSurfaceTemperatureAvgESS");
    }

    /**
     * Wraps
     * {@link GetPlatformsForRegion#doPost(HttpServletRequest, HttpServletResponse)}
     *
     * @param oRequest  Requested data to be sent to {@code oResponse}. Connected
     *                  prior to a call to this method.
     * @param oResponse Connected response servlet, to write the requested
     *                  data to.
     */
    @Override
    public void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse) {
        doPost(oRequest, oResponse);
    }

    /**
     * Gets all platforms with a public distribution group. If the request is for
     * observation type and/or platform id, the corresponding values are printed,
     * otherwise the platforms are printed to the response servlet.
     *
     * @param oRequest  Requested data to be sent to {@code oResponse}. Connected
     *                  prior to a call to this method.
     * @param oResponse Connected response servlet, to write the requested
     *                  data to.
     */
    @Override
    public void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse) {
        ArrayList<PlatformObs> oPlatforms = m_oPlatformMonitor.getPlatforms();
        String sShowAsos = oRequest.getParameter("showasos");
        boolean isSuperUser = AccessControl.isSuperUser(oRequest);
        boolean bDebug = (sShowAsos != null && sShowAsos.length() > 0);

        // build the output to include all platforms
        // except those without a public distribution group
        // debug mode will ignore the distribution group
        if (!m_bDebug || !bDebug) {
            int nIndex = oPlatforms.size();
            while (nIndex-- > 0) {
                if (oPlatforms.get(nIndex).m_nDistGroup == 1)
                    oPlatforms.remove(nIndex);
            }
        }

        try {
            oResponse.setContentType("application/json");
            PrintWriter oPrintWriter = oResponse.getWriter();

            String sObsType = oRequest.getParameter("obsType");
            if (sObsType != null)
                getObsByType(oPrintWriter, oPlatforms, Integer.parseInt(sObsType), isSuperUser);

            String sSourceId = oRequest.getParameter("sourceId");
            String sPlatformId = oRequest.getParameter("stationId");
            String sLat = oRequest.getParameter("lat");
            String sLon = oRequest.getParameter("lon");
            if (sSourceId != null && sPlatformId != null && sLat != null && sLon != null) {
                getPlatformObs(oPrintWriter, Integer.parseInt(sSourceId),
                        Integer.parseInt(sPlatformId),
                        MathUtil.toMicro(Double.parseDouble(sLat)),
                        MathUtil.toMicro(Double.parseDouble(sLon)), isSuperUser);
            }

            if (sObsType == null && sPlatformId == null)
                getPlatforms(oPrintWriter, oPlatforms, isSuperUser);

            oPrintWriter.flush();
            oPrintWriter.close();
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }

    /**
     * Prints observations of the supplied type, from each of the platforms
     * in the provided platform list to the provided output stream in a
     * formatted manner:
     * <blockquote>
     * [{platform-id, [observation values (comma delimited)], [english-unit
     * observation values (comma delimited)]}]
     * </blockquote>
     *
     * @param oPrintWriter output stream - connected prior to a call to this
     *                     method.
     * @param oPlatforms   list of platforms of interest.
     * @param nObsTypeId   observation-type of interest.
     */
    private void getObsByType(PrintWriter oPrintWriter,
                              ArrayList<PlatformObs> oPlatforms, int nObsTypeId, boolean isSuperUser) {
        try {
            // see if we are getting a particular observation type
            ObsType obsType = obsTypeDao.getObsType(nObsTypeId);
            if (obsType == null)
                return;

            UnitConv oUnitConv = m_oUnits.getConversion(obsType.getObsInternalUnit(), obsType.getObsEnglishUnit());

            StringBuilder sObsValue = new StringBuilder();
            StringBuilder sEnglishValue = new StringBuilder();
            String mValue = null;
            String eValue = null;
            DecimalFormat oFormatter = new DecimalFormat("0.00");

            // get the set of observations for each platform
            int nObsCount = 0;
            oPrintWriter.print('[');
            for (int nIndex = 0; nIndex < oPlatforms.size(); nIndex++) {
                sObsValue.setLength(0);
                sEnglishValue.setLength(0);

                PlatformObs oPlatform = oPlatforms.get(nIndex);
                IObs[] iObsArray = {
                        oPlatform.getLatestObs(nObsTypeId, 1),
                        oPlatform.getLatestObs(nObsTypeId, 2)
                };

                ISensor iSensor = null;
                for (int i = 0; i < 2; i++) {
                    if (iObsArray[i] != null) {

                        iSensor = sensorDao.getSensor(iObsArray[i].getSensorId());
                        if (iSensor == null)
                            iObsArray[i] = null;

                        int distGroup = iSensor.getDistGroup();
                        if (distGroup == 1 || distGroup == 0 && !isSuperUser)
                            iObsArray[i] = null;
                    }

                    if (iObsArray[i] != null) {
                        double value = iObsArray[i].getValue();
                        mValue = oFormatter.format(value);
                        eValue = oFormatter.format(oUnitConv.convert(value));
                    }
                }

                for (int source = 1; source <= 2; source++) {
                    IObs iObs = iObsArray[source - 1];
                    if (iObs != null) {
                        if (oPlatform.m_cCategory != 'S' && iObs.getQchCharFlag() == null)
                            continue;

                        if (nObsCount > 0)
                            oPrintWriter.print(",");

                        oPrintWriter.print("\n\t{id:");
                        oPrintWriter.print(oPlatform.m_nId);

                        oPrintWriter.print(",si:");
                        oPrintWriter.print(source);

                        oPrintWriter.print(",lt:");
                        oPrintWriter.print(MathUtil.fromMicro(oPlatform.m_nLat));

                        oPrintWriter.print(",ln:");
                        oPrintWriter.print(MathUtil.fromMicro(oPlatform.m_nLon));

                        oPrintWriter.print(",mv:");
                        oPrintWriter.print(mValue);

                        oPrintWriter.print(",ev:");
                        oPrintWriter.print(eValue);

                        oPrintWriter.print("}");
                        ++nObsCount;
                    }
                }
            }
            oPrintWriter.print("\n]");
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }

    /**
     * Prints observations from the platform corresponding to the supplied
     * platform-id to the provided output stream in a formatted manner:
     * <blockquote>
     * platform-description, platform elevation, timestamp, observation type,
     * timestamp, value, units, english-unit value, english units,
     * quality check bit field, pass/fail bit field.
     * </blockquote>
     *
     * @param oPrintWriter output stream - connected prior to a call to this
     *                     method.
     * @param nPlatformId  platform whose observations are of interest.
     */
    private synchronized void getPlatformObs(PrintWriter oPrintWriter,
                                             int nSourceId, int nPlatformId, int nLat, int nLon, boolean isSuperUser) {
        PlatformObs oPlatform = m_oPlatformMonitor.getPlatform(nPlatformId, nLat, nLon);
        if (oPlatform == null)
            return;

        ArrayList<IObs> oObs = oPlatform.m_oObs;

        oPrintWriter.print("{\n\tnm:\"");
        oPrintWriter.print(oPlatform.m_iPlatform.getDescription());
        oPrintWriter.print("\",\n\tel:");
        oPrintWriter.print(oPlatform.m_tElev);
        oPrintWriter.print(",\n\tob:\n\t[");

        if (oObs != null && oObs.size() > 0) {
            int nObsCount = 0;
            DecimalFormat oFormatter = new DecimalFormat("0.00");
            DecimalFormat oConfFormat = new DecimalFormat("##0");
            for (int nIndex = 0; nIndex < oObs.size(); nIndex++) {
                IObs iObs = oObs.get(nIndex);

                // skip invalid observations - these need to be cleaned up later
                int lat = iObs.getLatitude();
                int lon = iObs.getLongitude();
                if (lat == 0 && lon == 0)
                    continue;

                ObsType obsType = obsTypeDao.getObsType(iObs.getObsTypeId());
                if (obsType == null)
                    continue;

                if (oPlatform.m_iPlatform.getCategory() != 'S') {
                    // WxDE obs not to be distributed never have any quality flags.  Segments don't have quality checks
                    if (iObs.getQchCharFlag() == null)
                        continue;
                } else {
                    // For segments, display only observation types in the list
                    if (!segStatisticsObsTypeDisplayList.contains(obsType.getObsType()))
                        continue;
                }

                // obs not from the same source
                if (iObs.getSourceId() != nSourceId)
                    continue;

                m_oDate.setTime(iObs.getObsTimeLong());

                ISensor iSensor = sensorDao.getSensor(iObs.getSensorId());
                if (iSensor == null)
                    continue; // only send distributable obs values

                // Allow super user to see all data
                int distGroup = iSensor.getDistGroup();
                if (distGroup != 2 && !isSuperUser)
                    continue;

                UnitConv oUnitConv = m_oUnits.
                        getConversion(obsType.getObsInternalUnit(), obsType.getObsEnglishUnit());

                if (nObsCount > 0)
                    oPrintWriter.print(",");

                oPrintWriter.print("\n\t\t{ot:\"");
                oPrintWriter.print(obsType.getObsType());
                oPrintWriter.print("\",si:\"");
                oPrintWriter.print(iSensor.getSensorIndex());
                oPrintWriter.print("\",ts:\"");
                oPrintWriter.print(m_oDateFormat.format(m_oDate));
                oPrintWriter.print("\",mv:\"");
                oPrintWriter.print(oFormatter.format(iObs.getValue()));
                oPrintWriter.print("\",mu:\"");
                oPrintWriter.print(obsType.getObsInternalUnit());
                oPrintWriter.print("\",ev:\"");
                oPrintWriter.print(oFormatter.format(oUnitConv.convert(iObs.getValue())));
                oPrintWriter.print("\",eu:\"");
                oPrintWriter.print(obsType.getObsEnglishUnit());
                oPrintWriter.print("\",cv:");
                oPrintWriter.print(oConfFormat.format(iObs.getConfValue() * 100.0));
                char cat = oPlatform.m_iPlatform.getCategory();
                if (cat != 'S') {
                    if (cat == 'M') {
                        oPrintWriter.print(",lt:");
                        oPrintWriter.print(MathUtil.fromMicro(iObs.getLatitude()));

                        oPrintWriter.print(",ln:");
                        oPrintWriter.print(MathUtil.fromMicro(iObs.getLongitude()));
                    }

                    QualityCheckFlags qcf = QualityCheckFlagUtil.getFlags(nSourceId, iObs.getQchCharFlag());
                    oPrintWriter.print(",rf:");
                    oPrintWriter.print(qcf.getRunFlags());
                    oPrintWriter.print(",pf:");
                    oPrintWriter.print(qcf.getPassFlags());
                } else {
                    oPrintWriter.print(",rf:0");
                    oPrintWriter.print(",pf:0");
                }
                oPrintWriter.print("}");

                ++nObsCount;
            }
        }

        oPrintWriter.print("\n\t]\n}");
    }

    /**
     * Prints the platforms contained in the supplied list to the supplied
     * {@code PrintWriter} in a formatted manner:
     * <blockquote>
     * platform-id, contributor-id, platform-code, latitude, longitude, obs-flag
     * </blockquote>
     * where the obs-flag = 1 indicates the platform has observations older than
     * the timeout.
     *
     * @param oPrintWriter output stream, connected prior to the call to this
     *                     method.
     * @param oPlatforms   list of platforms to print.
     */
    private void getPlatforms(PrintWriter oPrintWriter, ArrayList<PlatformObs> oPlatforms, boolean isSuperUser) {
        try {
            boolean bPrinted = false;
            oPrintWriter.print('[');
            for (int nIndex = 0; nIndex < oPlatforms.size(); nIndex++) {
                PlatformObs oPlatform = oPlatforms.get(nIndex);

                if (oPlatform.m_nLat == 0 && oPlatform.m_nLon == 0)
                    continue;

                if (bPrinted)
                    oPrintWriter.print(",");

                oPrintWriter.print("\n\t{id:");
                oPrintWriter.print(oPlatform.m_nId);

                oPrintWriter.print(",cn:");
                oPrintWriter.print(oPlatform.m_iPlatform.getContribId());

                oPrintWriter.print(",st:\"");
                oPrintWriter.print(oPlatform.m_sCode);

                oPrintWriter.print("\",ca:\"");
                oPrintWriter.print(oPlatform.m_iPlatform.getCategory());

                oPrintWriter.print("\",lt:");
                oPrintWriter.print(MathUtil.fromMicro(oPlatform.m_nLat));

                oPrintWriter.print(",ln:");
                oPrintWriter.print(MathUtil.fromMicro(oPlatform.m_nLon));

                // 0 - no obs, 1 - obs from WxDE, 2 - obs from VDT, 3 - obs from WxDE and VDT
                oPrintWriter.print(",ho:");
                int wxdeObs = 0;
                int vdtObs = 0;
                if (oPlatform.m_nDistGroup == 2 || isSuperUser) {
                    wxdeObs = (oPlatform.m_bHasWxDEObs) ? 1 : 0;
                    vdtObs = (oPlatform.m_bHasVDTObs) ? 2 : 0;
                    oPrintWriter.print(wxdeObs + vdtObs);
                } else {
                    oPrintWriter.print(0);
                    if (oPlatform.m_nDistGroup == 1) {
                        oPrintWriter.print('}');
                        continue;
                    }
                }

                // include polyline points for road segment platforms
                if (oPlatform.m_iPlatform.getCategory() == 'S') {
                    ArrayList<Double> oLat = oPlatform.m_oLat;
                    ArrayList<Double> oLon = oPlatform.m_oLon;

                    oPrintWriter.print(",sg:[");
                    if (oLat != null && oLon != null &&
                            oLat.size() > 1 && oLat.size() == oLon.size()) {
                        // must have at least two points to avoid empty array

                        for (int nPoint = 0; nPoint < oLat.size(); nPoint++) {
                            if (nPoint > 0)
                                oPrintWriter.print(",");

                            oPrintWriter.print(String.format("%06f", oLat.get(nPoint)));
                            oPrintWriter.print(",");
                            oPrintWriter.print(String.format("%06f", oLon.get(nPoint)));
                        }
                    }
                    oPrintWriter.print(']');
                } else if (oPlatform.m_iPlatform.getCategory() == 'M') {
                    ArrayList<IObs> oObs = oPlatform.m_oObs;
                    if (oObs != null && oObs.size() > 0) {
                        int wxdeCount = 0;
                        int vdtCount = 0;
                        StringBuffer wxde = new StringBuffer();
                        StringBuffer vdt = new StringBuffer();
                        ;

                        wxde.append("wxde:[");
                        vdt.append("vdt:[");
                        for (int nIndex2 = 0; nIndex2 < oObs.size(); nIndex2++) {
                            IObs iObs = oObs.get(nIndex2);

                            if (iObs.getQchCharFlag() == null)
                                continue;

                            ISensor iSensor = sensorDao.getSensor(iObs.getSensorId());
                            if (iSensor == null)
                                continue;

                            // Allow super user to see all data
                            int distGroup = iSensor.getDistGroup();
                            if (distGroup != 2 && !isSuperUser)
                                continue;

                            // skip invalid observations - these need to be cleaned up later
                            int lat = iObs.getLatitude();
                            int lon = iObs.getLongitude();
                            if (lat == 0 && lon == 0)
                                continue;

                            String latLon = MathUtil.fromMicro(lat) + "," + MathUtil.fromMicro(lon);

                            if (iObs.getSourceId() == 1) {
                                if (wxdeCount > 0)
                                    wxde.append(",");

                                wxde.append(latLon);
                                wxdeCount++;
                            } else {
                                if (vdtCount > 0)
                                    vdt.append(",");

                                vdt.append(latLon);
                                vdtCount++;
                            }
                        }
                        wxde.append("]");
                        vdt.append("]");

                        if (wxdeObs > 0)
                            oPrintWriter.print("," + wxde.toString());

                        if (vdtObs > 0)
                            oPrintWriter.print("," + vdt.toString());
                    }
                }

                oPrintWriter.print('}');
                bPrinted = true;
            }
            oPrintWriter.print("\n]");
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }
}
