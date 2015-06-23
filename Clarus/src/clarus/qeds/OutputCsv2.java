// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qeds;

/**
 *
 */
public class OutputCsv2 extends OutputCsv
{
	OutputCsv2()
	{
		m_sHeader = "ObsTypeID,ObsTypeName," +
		"ClarusSensorID,ClarusSensorIndex,ClarusStationID,ClarusSiteID," +
		"Category,ClarusContribID,Contributor,StationCode,Timestamp," +
		"Latitude,Longitude,Elevation,Observation,Units," +
		"EnglishValue,EnglishUnits,ConfValue,QchsSequenceComplete," +
		"QchsManualFlag,QchsServiceSensorRange,QchsServiceClimateRange," +
		"QchsServiceStep,QchsServiceLike,QchsServicePersist," +
		"QchsServiceIQR,QchsServiceBarnes,QchServiceDewpoint," +
		"QchsServicePressure,QchsServicePrecipAccum";
	}


	@Override
	protected int getQchLength()
	{
		return 12;
	}
}
