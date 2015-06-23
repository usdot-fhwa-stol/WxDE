// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SubObs.java
 */
package wde.qeds;

import wde.dao.*;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.metadata.ObsType;
import wde.obs.IObs;
import wde.util.MathUtil;

import java.sql.Array;
import java.sql.ResultSet;

/**
 * Wraps all observation data relevant to subscribers into one class.
 */
public class SubObs {
    /**
     * Observation type id.
     */
    int m_nObsTypeId;

    int sourceId;

    /**
     * Observation sensor id.
     */
    int m_nSensorId;
    /**
     * Observation timestamp.
     */
    long m_lTimestamp;
    /**
     * Timestamp indicating when the observation was most recently received.
     */
    long recvTime;
    /**
     * Sensor latitude.
     */
    double m_dLat;
    /**
     * Sensor longitude.
     */
    double m_dLon;
    /**
     * Sensor elevation.
     */
    int m_nElev;

    /**
     * Observation value.
     */
    double m_dValue;
    /**
     * Quality confidence level.
     */
    float m_fConfidence;
    /**
     * Bit field - Quality checking alogrithm pass/fail.
     */
    char[] m_nFlags;
    /**
     * Corresponding english value.
     */
    double m_dEnglishValue;

    /**
     * Observation type.
     */
    ObsType m_iObsType;
    /**
     * Observation sensor.
     */
    ISensor m_iSensor;
    /**
     * Observation station.
     */
    IPlatform m_iPlatform;
    /**
     * Observation contributor.
     */
    Contrib m_oContrib;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code SubObs}
     * </p>
     */
    SubObs() {
    }


    /**
     * <b> Constructor </b>
     * <p>
     * Initializes {@code SubObs} attributes of new instances of {@code SubObs}
     * from provided data, and data gathered from the provided database result
     * set.
     * </p>
     *
     * @param oContribs   used to determine the contributor corresponding to the
     *                    sensor/station retrieved from the result set. Should be a pointer to the
     *                    singleton instance of the {@code Contribs} {@code DbCache}.
     * @param oStations   used to determine the station containing the sensor
     *                    retrieved from the resultant set. Should be a pointer to the
     *                    singleton instance of {@code PlatformDao}.
     * @param oSensors    used to retrieve the sensor found in the result set. Should
     *                    be a pointer to the singleton instance of the {@code Sensors}
     *                    {@code DbCache}.
     * @param oUnits      used to find the conversion from the units corresponding
     *                    to the resultant set observation type id. Should be a pointer to the
     *                    singleton instance of {@code Units}.
     * @param oObsTypes   used to retrieve observation type from the resultant
     *                    set observation-type id. Should be a pointer to the
     *                    singleton instance of the {@code ObsTypes} {@code DbCache}.
     * @param iObsResults set containing the observation data of interest.
     * @throws java.lang.Exception
     */
    SubObs(Contribs oContribs, PlatformDao platforms, SensorDao sensorDao,
           Units oUnits, ObsTypeDao obsTypeDao, ResultSet iObsResults)
            throws Exception {
        m_nObsTypeId = iObsResults.getInt(1);
        sourceId = iObsResults.getInt(2);
        m_nSensorId = iObsResults.getInt(3);
        m_lTimestamp = iObsResults.getTimestamp(4).getTime();
        recvTime = iObsResults.getTimestamp(5).getTime();
        m_dLat = MathUtil.fromMicro(iObsResults.getInt(6));
        m_dLon = MathUtil.fromMicro(iObsResults.getInt(7));
        m_nElev = iObsResults.getInt(8);
        m_dValue = iObsResults.getDouble(9);
        m_fConfidence = iObsResults.getFloat(10);

        Array charArr = iObsResults.getArray("qchCharFlag");
        if (charArr != null) {
            String[] strArray = (String[]) charArr.getArray();
            char[] charArray = new char[strArray.length];
            for (int i = 0; i < strArray.length; i++)
                charArray[i] = strArray[i].charAt(0);
            m_nFlags = charArray;
        }

        resolveMetadata(oContribs, platforms, sensorDao, oUnits, obsTypeDao);
    }


    SubObs(Contribs oContribs, PlatformDao platforms, SensorDao sensorDao,
           Units oUnits, ObsTypeDao obsTypeDao, IObs iObs) {
        m_nObsTypeId = iObs.getObsTypeId();
        sourceId = iObs.getSourceId();
        m_nSensorId = iObs.getSensorId();
        m_lTimestamp = iObs.getObsTimeLong();
        m_dLat = MathUtil.fromMicro(iObs.getLatitude());
        m_dLon = MathUtil.fromMicro(iObs.getLongitude());
        m_nElev = iObs.getElevation();
        m_dValue = iObs.getValue();
        m_fConfidence = iObs.getConfValue();
        m_nFlags = iObs.getQchCharFlag();
        recvTime = iObs.getRecvTimeLong();

        resolveMetadata(oContribs, platforms, sensorDao, oUnits, obsTypeDao);
    }


    private void resolveMetadata(Contribs oContribs, PlatformDao platforms,
                                 SensorDao sensorDao, Units oUnits, ObsTypeDao obsTypeDao) {
        // resolve obs metadata
        m_iObsType = obsTypeDao.getObsType(m_nObsTypeId);
        if (m_iObsType == null)
            return;

        UnitConv oUnitConv = oUnits.
                getConversion(m_iObsType.getObsInternalUnit(), m_iObsType.getObsEnglishUnit());
        m_dEnglishValue = oUnitConv.convert(m_dValue);

        m_iSensor = sensorDao.getSensor(m_nSensorId);
        if (m_iSensor == null)
            return;

        m_iPlatform = platforms.getPlatform(m_iSensor.getPlatformId());
        if (m_iPlatform == null)
            return;

        m_oContrib = oContribs.getContrib(m_iPlatform.getContribId());
        if (m_oContrib != null && m_oContrib.m_nDisplay == 0)
            m_oContrib = null;
    }
}
