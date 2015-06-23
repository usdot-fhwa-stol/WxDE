// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SubObs.java
 */
package clarus.qeds;

import java.sql.ResultSet;
import clarus.UnitConv;
import clarus.Units;
import clarus.emc.IObsType;
import clarus.emc.ObsTypes;
import clarus.emc.ISensor;
import clarus.emc.Sensors;
import clarus.emc.IStation;
import clarus.emc.Stations;
import clarus.qedc.IObs;

/**
 * Wraps all observation data relevant to subscribers into one class.
 */
public class SubObs
{
    /**
     * Observation type id.
     */
	int m_nObsTypeId;
    /**
     * Observation sensor id.
     */
	int m_nSensorId;
    /**
     * Observation timestamp.
     */
	long m_lTimestamp;
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
     * Timestamp indicating when the observation was most recently updated.
     */
	long m_lUpdated;
    /**
     * Observation value.
     */
	double m_dValue;
    /**
     * Quality confidence level.
     */
	float m_fConfidence;
    /**
     * Bit field - Quality checking algorithms ran.
     */
	int m_nRunFlags;
    /**
     * Bit field - Quality checking alogrithm pass/fail.
     */
	int m_nPassedFlags;
    /**
     * Corresponding english value.
     */
	double m_dEnglishValue;

    /**
     * Observation type.
     */
	IObsType m_iObsType;
    /**
     * Observation sensor.
     */
	ISensor m_iSensor;
    /**
     * Observation station.
     */
	IStation m_iStation;
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
	SubObs()
	{
	}


    /**
     * <b> Constructor </b>
     * <p>
     * Initializes {@code SubObs} attributes of new instances of {@code SubObs}
     * from provided data, and data gathered from the provided database result
     * set.
     * </p>
     * @param oContribs used to determine the contributor corresponding to the
     * sensor/station retrieved from the result set. Should be a pointer to the
     * singleton instance of the {@code Contribs} {@code DbCache}.
     * @param oStations used to determine the station containing the sensor
     * retrieved from the resultant set. Should be a pointer to the
     * singleton instance of the {@code Stations} {@code DbCache}.
     * @param oSensors used to retrieve the sensor found in the result set. Should
     * be a pointer to the singleton instance of the {@code Sensors}
     * {@code DbCache}.
     * @param oUnits used to find the conversion from the units corresponding
     * to the resultant set observation type id. Should be a pointer to the
     * singleton instance of {@code Units}.
     * @param oObsTypes used to retrieve observation type from the resultant
     * set observation-type id. Should be a pointer to the
     * singleton instance of the {@code ObsTypes} {@code DbCache}.
     * @param iObsResults set containing the observation data of interest.
     * @throws java.lang.Exception
     */
	SubObs(Contribs oContribs, Stations oStations, Sensors oSensors, 
		Units oUnits, ObsTypes oObsTypes, ResultSet iObsResults)
		throws Exception
	{
		m_nObsTypeId = iObsResults.getInt(1);
		m_nSensorId = iObsResults.getInt(2);
		m_lTimestamp = iObsResults.getTimestamp(3).getTime();
		m_dLat = Stations.fromMicro(iObsResults.getInt(4));
		m_dLon = Stations.fromMicro(iObsResults.getInt(5));
		m_nElev = iObsResults.getInt(6);
		m_dValue = iObsResults.getDouble(7);
		m_fConfidence = iObsResults.getFloat(8);
		m_nRunFlags = iObsResults.getInt(9);
		m_nPassedFlags = iObsResults.getInt(10);
		m_lUpdated = m_lTimestamp;

		resolveMetadata(oContribs, oStations, oSensors, oUnits, oObsTypes);
	}


	SubObs(Contribs oContribs, Stations oStations, Sensors oSensors,
		Units oUnits, ObsTypes oObsTypes, IObs iObs)
	{
		m_nObsTypeId = iObs.getTypeId();
		m_nSensorId = iObs.getSensorId();
		m_lTimestamp = iObs.getTimestamp();
		m_dLat = Stations.fromMicro(iObs.getLat());
		m_dLon = Stations.fromMicro(iObs.getLon());
		m_nElev = iObs.getElev();
		m_dValue = iObs.getValue();
		m_fConfidence = iObs.getConfidence();
		m_nRunFlags = iObs.getRun();
		m_nPassedFlags = iObs.getFlags();
		m_lUpdated = iObs.getUpdate();

		resolveMetadata(oContribs, oStations, oSensors, oUnits, oObsTypes);
	}


	private void resolveMetadata(Contribs oContribs, Stations oStations,
		Sensors oSensors, Units oUnits, ObsTypes oObsTypes)
	{
		// resolve obs metadata
		m_iObsType = oObsTypes.getObsType(m_nObsTypeId);
		if (m_iObsType == null)
			return;

		UnitConv oUnitConv = oUnits.
			getConversion(m_iObsType.getUnit(), m_iObsType.getEnglishUnit());
		m_dEnglishValue = oUnitConv.convert(m_dValue);

		m_iSensor = oSensors.getSensor(m_nSensorId);
		if (m_iSensor == null)
			return;

		m_iStation = oStations.getStation(m_iSensor.getStationId());
		if (m_iStation == null)
			return;

		m_oContrib = oContribs.getContrib(m_iStation.getContribId());
		if (m_oContrib != null && m_oContrib.m_nDisplay == 0)
			m_oContrib = null;
	}
}
