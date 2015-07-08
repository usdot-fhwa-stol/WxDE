// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file GetStationsForRegion.java
 */
package clarus.qeds;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.text.DecimalFormat;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import clarus.UnitConv;
import clarus.Units;
import clarus.emc.IObsType;
import clarus.emc.ObsTypes;
import clarus.emc.ISensor;
import clarus.emc.Sensors;
import clarus.emc.Stations;
import clarus.qedc.IObs;

/**
 * Provides a means of requesting data across an http connection. Observation
 * data can be retrieved by observation type, and station. Stations are used
 * if they have a public distribution group.
 * <p>
 * Extends {@code HttpServlet} to enforce a standard interface for requesting
 * data, and responding to these data requests.
 * </p>
 */
public class GetStationsForRegion extends HttpServlet
{
	private boolean m_bDebug;
    /**
     * Observation timestamp.
     */
	private Date m_oDate = new Date();
    /**
     * Timestamp format.
     */
	private SimpleDateFormat m_oDateFormat =
			new SimpleDateFormat("yyyy-MM-dd HH:mm");

    /**
     * Reference to {@code Units} instance.
     */
	private Units m_oUnits = Units.getInstance();
    /**
     * Reference to {@code ObsTypes} instance.
     */
	private ObsTypes m_oObsTypes = ObsTypes.getInstance();
    /**
     * Reference to {@code Sensors} instance.
     */
	private Sensors m_oSensors = Sensors.getInstance();
    /**
     * Reference to {@code StationMonitor} instance.
     */
	private StationMonitor m_oStationMonitor = StationMonitor.getInstance();

	
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code GetStationsForRegion}
	 * </p>
	 */
    public GetStationsForRegion()
    {
    }


	@Override
	public void init(ServletConfig iConfig)
	{
		String sShowAll = iConfig.getInitParameter("debug");
		if (sShowAll != null && sShowAll.length() > 0)
			m_bDebug = Boolean.parseBoolean(sShowAll);
	}


    /**
     * Wraps
     * {@link GetStationsForRegion#doPost(HttpServletRequest, HttpServletResponse)}
     * @param oRequest Requested data to be sent to {@code oResponse}. Connected
     * prior to a call to this method.
     * @param oResponse Connected response servlet, to write the requested
     * data to.
     */
	@Override
	public void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
        doPost(oRequest, oResponse);
    }
    

    /**
     * Gets all stations with a public distribution group. If the request is for
     * observation type and/or station id, the corresponding values are printed,
     * otherwise the stations are printed to the response servlet.
     *
     * @param oRequest Requested data to be sent to {@code oResponse}. Connected
     * prior to a call to this method.
     * @param oResponse Connected response servlet, to write the requested
     * data to.
     */
	@Override
    public void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse)
    {
		ArrayList<StationObs> oStations = m_oStationMonitor.getStations();
        String sShowAsos = oRequest.getParameter("showasos");
		boolean bDebug = (sShowAsos != null && sShowAsos.length() > 0);
		
		// build the output to include all stations 
		// except those without a public distribution group
		// debug mode will ignore the distribution group
		if (!m_bDebug || !bDebug)
		{
			int nIndex = oStations.size();
			while (nIndex-- > 0)
			{
				if (oStations.get(nIndex).m_nDistGroup == 1)
					oStations.remove(nIndex);
			}
		}
		
		try
		{
			oResponse.setContentType("application/json");
			PrintWriter oPrintWriter = oResponse.getWriter();
			
			String sObsType = oRequest.getParameter("obsType");
			if (sObsType != null)
				getObsByType(oPrintWriter, oStations, Integer.parseInt(sObsType));
			
			String sStationId = oRequest.getParameter("stationId");
			String sLat = oRequest.getParameter("lat");
			String sLon = oRequest.getParameter("lon");
			if (sStationId != null && sLat != null && sLon != null)
			{
				getStationObs(oPrintWriter, Integer.parseInt(sStationId),
					Stations.toMicro(Double.parseDouble(sLat)),
					Stations.toMicro(Double.parseDouble(sLon)));
			}

			if (sObsType == null && sStationId == null)
				getStations(oPrintWriter, oStations);
			
			oPrintWriter.flush();
			oPrintWriter.close();
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
    }
	

    /**
     * Prints observations of the supplied type, from each of the stations
     * in the provided stations list to the provided output stream in a
     * formatted manner:
     * <blockquote>
     * [{station-id, [observation values (comma delimited)], [english-unit
     * observation values (comma delimited)]}]
     * </blockquote>
     * @param oPrintWriter output stream - connected prior to a call to this
     * method.
     * @param oStations list of stations of interest.
     * @param nObsTypeId observation-type of interest.
     */
	private void getObsByType(PrintWriter oPrintWriter, 
		ArrayList<StationObs> oStations, int nObsTypeId)
	{
        try
        {
            // see if we are getting a particular observation type
			IObsType iObsType = m_oObsTypes.getObsType(nObsTypeId);
			if (iObsType == null)
				return;
			
			UnitConv oUnitConv = m_oUnits.
				getConversion(iObsType.getUnit(), iObsType.getEnglishUnit());

            StringBuilder sObsValue = new StringBuilder();
            StringBuilder sEnglishValue = new StringBuilder();
            DecimalFormat oFormatter = new DecimalFormat("0.00");
            
            // get the set of observations for each station
			int nObsCount = 0;
			oPrintWriter.print('[');
			for (int nIndex = 0; nIndex < oStations.size(); nIndex++)
            {
				sObsValue.setLength(0);
				sEnglishValue.setLength(0);

				// get the obs for each sensor
                StationObs oStation = oStations.get(nIndex);
				ArrayList<IObs> oObs = oStation.m_oObs;
				synchronized(oObs)
				{
					for (int nObsIndex = 0; nObsIndex < oObs.size(); 
						nObsIndex++)
					{
						IObs iObs = oObs.get(nObsIndex);

						// show obs that match the requested type and that
						// can be distributed
                        if (iObs.getTypeId() != nObsTypeId || 
							(iObs.getRun() == 0 && iObs.getFlags() == 0))
                            continue;
						
						ISensor iSensor = m_oSensors.getSensor(iObs.getSensorId());
						if (iSensor == null || iSensor.getDistGroup() == 1)
							continue; // skip sensors not for distribution

						// insert a comma if there is more than one value
						if (sObsValue.length() > 0)
						{
							sObsValue.append(",");
							sEnglishValue.append(",");
						}

						sObsValue.append(oFormatter.format(iObs.getValue()));
						sEnglishValue.append(oFormatter.format(
							oUnitConv.convert(iObs.getValue())));
					}
				}
					
				// only write out the record if there are obs values
				if (sObsValue.length() > 0)
				{
					if (nObsCount > 0)
						oPrintWriter.print(",");

					oPrintWriter.print("\n\t{id:");
					oPrintWriter.print(oStation.m_nId);

					oPrintWriter.print(",lt:");
					oPrintWriter.print(Stations.fromMicro(oStation.m_nLat));

					oPrintWriter.print(",ln:");
					oPrintWriter.print(Stations.fromMicro(oStation.m_nLon));

					oPrintWriter.print(",mv:[");
					for (int i = 0; i < sObsValue.length(); i++)
						oPrintWriter.print(sObsValue.charAt(i));

					oPrintWriter.print("],ev:[");
					for (int i = 0; i < sEnglishValue.length(); i++)
						oPrintWriter.print(sEnglishValue.charAt(i));

					oPrintWriter.print("]}");
					++nObsCount;
				}
			}
            oPrintWriter.print("\n]");
        }
        catch (Exception oException)
        {
            oException.printStackTrace();
        }
    }
    

    /**
     * Prints observations from the station corresponding to the supplied
     * station-id to the provided output stream in a formatted manner:
     * <blockquote>
     * station-description, station elevation, timestamp, observation type,
     * timestamp, value, units, english-unit value, english units,
     * quality check bit field, pass/fail bit field.
     * </blockquote>
     * @param oPrintWriter output stream - connected prior to a call to this
     * method.
     * @param nStationId station whose observations are of interest.
     */
	private synchronized void getStationObs(PrintWriter oPrintWriter,
		int nStationId, int nLat, int nLon)
	{
        StationObs oStation = m_oStationMonitor.getStation(nStationId, nLat, nLon);
        if (oStation == null)
            return;

        ArrayList<IObs> oObs = oStation.m_oObs;

        oPrintWriter.print("{\n\tnm:\"");
        oPrintWriter.print(oStation.m_iStation.getDesc());
        oPrintWriter.print("\",\n\tel:");
        oPrintWriter.print(oStation.m_tElev);
        oPrintWriter.print(",\n\tob:\n\t[");

        if (oObs != null && oObs.size() > 0)
        {
			int nObsCount = 0;
            DecimalFormat oFormatter = new DecimalFormat("0.00");
			DecimalFormat oConfFormat = new DecimalFormat("##0");
            for (int nIndex = 0; nIndex < oObs.size(); nIndex++)
            {
				IObs iObs = oObs.get(nIndex);
				// obs not to be distributed never have any quality flags
				if (iObs.getRun() == 0 && iObs.getFlags() == 0)
					continue;

				m_oDate.setTime(iObs.getTimestamp());
				
				IObsType iObsType = m_oObsTypes.getObsType(iObs.getTypeId());
				if (iObsType == null)
					continue;

				ISensor iSensor = m_oSensors.getSensor(iObs.getSensorId());
				if (iSensor == null || iSensor.getDistGroup() < 2)
					continue; // only send distributable obs values

				UnitConv oUnitConv = m_oUnits.
					getConversion(iObsType.getUnit(), iObsType.getEnglishUnit());

				if (nObsCount > 0)
					oPrintWriter.print(",");

				oPrintWriter.print("\n\t\t{ot:\"");
				oPrintWriter.print(iObsType.getName());
				oPrintWriter.print("\",si:\"");
				oPrintWriter.print(iSensor.getSensorIndex());
				oPrintWriter.print("\",ts:\"");
				oPrintWriter.print(m_oDateFormat.format(m_oDate));
				oPrintWriter.print("\",mv:\"");
				oPrintWriter.print(oFormatter.format(iObs.getValue()));
				oPrintWriter.print("\",mu:\"");
				oPrintWriter.print(iObsType.getUnit());
				oPrintWriter.print("\",ev:\"");
				oPrintWriter.print(oFormatter.format(oUnitConv.convert(iObs.getValue())));
				oPrintWriter.print("\",eu:\"");
				oPrintWriter.print(iObsType.getEnglishUnit());
				oPrintWriter.print("\",cv:");
				oPrintWriter.print(oConfFormat.format(iObs.getConfidence() * 100.0));
				oPrintWriter.print(",rf:");
				oPrintWriter.print(iObs.getRun());
				oPrintWriter.print(",pf:");
				oPrintWriter.print(iObs.getFlags());
				oPrintWriter.print("}");

				++nObsCount;
            }
		}

        oPrintWriter.print("\n\t]\n}");
	}
    

    /**
     * Prints the stations contained in the supplied list to the supplied
     * {@code PrintWriter} in a formatted manner:
     * <blockquote>
     * station-id, contributor-id, station-code, latitude, longitude, obs-flag
     * </blockquote>
     * where the obs-flag = 1 indicates the station has observations older than
     * the timeout.
     * @param oPrintWriter output stream, connected prior to the call to this
     * method.
     * @param oStations list of stations to print.
     */
	private void getStations(PrintWriter oPrintWriter, 
		ArrayList<StationObs> oStations)
	{
        try
        {
			boolean bPrinted = false;
			oPrintWriter.print('[');
			for (int nIndex = 0; nIndex < oStations.size(); nIndex++)
            {
                StationObs oStation = oStations.get(nIndex);

				if (bPrinted)
					oPrintWriter.print(",");

				oPrintWriter.print("\n\t{id:");
				oPrintWriter.print(oStation.m_nId);

				oPrintWriter.print(",cn:");
				oPrintWriter.print(oStation.m_iStation.getContribId());

				oPrintWriter.print(",st:\"");
				oPrintWriter.print(oStation.m_iStation.getCode());

				oPrintWriter.print("\",ca:\"");
				oPrintWriter.print(oStation.m_iStation.getCat());

				oPrintWriter.print("\",lt:");
				oPrintWriter.print(Stations.
					fromMicro(oStation.m_nLat));

				oPrintWriter.print(",ln:");
				oPrintWriter.print(Stations.
					fromMicro(oStation.m_nLon));

				oPrintWriter.print(",ho:");
				if (oStation.m_bHasObs)
					oPrintWriter.print(1);
				else
					oPrintWriter.print(0);

				oPrintWriter.print('}');
				bPrinted = true;
            }
            oPrintWriter.print("\n]");
        }
        catch (Exception oException)
        {
            oException.printStackTrace();
        }
	}
}
