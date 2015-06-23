// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file OutputCsv.java
 */
package clarus.qeds;

import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.TimeZone;

import util.Introsort;

/**
 * Provides methods to format and output observation data to .csv files to
 * a provided output stream.
 * <p>
 * Extends {@code OutputFormat} to enforce a general output-format interface.
 * </p>
 *
 * @see OutputCsv#fulfill(PrintWriter, ArrayList,
 *                          Subscription, String, int, long)
 */
public class OutputCsv extends OutputFormat
{
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
	protected String m_sHeader = "ObsTypeID,ObsTypeName," +
		"ClarusSensorID,ClarusSensorIndex,ClarusStationID,ClarusSiteID," +
		"Category,ClarusContribID,Contributor,StationCode,Timestamp," +
		"Latitude,Longitude,Elevation,Observation,Units," + 
		"EnglishValue,EnglishUnits,ConfValue," +
		"Complete,Manual,Sensor_Range,Climate_Range,Step,Like_Instrument," +
		"Persistence,IQR_Spatial,Barnes_Spatial,Dew_Point," +
		"Sea_Level_Pressure,Precip_Accum";

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
     * Decimal number format.
     */
    protected static DecimalFormat m_oDecimal = new DecimalFormat("0.000");


    /**
     * Initializes the file suffix. Initializes date format timezone to UTC.
     */
	OutputCsv()
	{
		int nQchLength = getQchLength();
		m_cRunFlags = new char[nQchLength];
		m_cPassFlags = new char[nQchLength];
		m_sSuffix = ".csv";
        m_oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
	}


    /**
     * Determines the number of quality check tests to be displayed
     */
	protected int getQchLength()
	{
		return 12;
	}


    /**
     * Prints {@code SubObs} data to provided output stream. First prints the
     * defined header, then traverses the provided {@code SubObs} list, printing
     * the contained data in a .csv comma-delimited manner, of the format:
     *
     * <blockquote>
     * observation-type id, observation-type name, sensor id, sensor index,
     * station id, site id, climate id, contributor id, contributor name,
     * station code, observation timestamp, latitude, longitude, elevation,
     * observation value, units, english-unit value, english-units, confidence
     * level, quality check
     * </blockquote>
     * followed by a timestamp footer.
     *
     * <p>
     * Required for extension of {@link OutputFormat}.
     * </p>
     *
     * @param oWriter output stream, connected, and ready to write data.
     * @param oSubObsList list of observations to print.
     * @param oSub subscription - used for filtering.
     * @param sFilename output filename to write in footer, can be specified as
     * null
     * @param nId subscription id.
     * @param lLimit timestamp lower bound. All observations with recieved or
     * completed date less than this limit will not be printed.
     */
	void fulfill(PrintWriter oWriter, ArrayList<SubObs> oSubObsList,
		Subscription oSub, String sFilename, int nId, long lLimit)
	{
		Introsort.usort(oSubObsList, this);

        try
        {
			// output the header information
			oWriter.println(m_sHeader);

			// output the obs details
			for (int nIndex = 0; nIndex < oSubObsList.size(); nIndex++)
			{
				SubObs oSubObs = oSubObsList.get(nIndex);
				// obs must match the time range and filter criteria
				if (oSubObs.m_lUpdated < lLimit || !oSub.matches(oSubObs))
					continue;

				oWriter.print(oSubObs.m_nObsTypeId);
				oWriter.print(",");
				oWriter.print(oSubObs.m_iObsType.getName());
				oWriter.print(",");
				oWriter.print(oSubObs.m_nSensorId);
				oWriter.print(",");
				oWriter.print(oSubObs.m_iSensor.getSensorIndex());
				oWriter.print(",");
				oWriter.print(oSubObs.m_iSensor.getStationId());
				oWriter.print(",");
				oWriter.print(oSubObs.m_iStation.getSiteId());
				oWriter.print(",");
				oWriter.print(oSubObs.m_iStation.getCat());
				oWriter.print(",");
				oWriter.print(oSubObs.m_oContrib.getId());
				oWriter.print(",");
				oWriter.print(oSubObs.m_oContrib.getName());
				oWriter.print(",");
				oWriter.print(oSubObs.m_iStation.getCode());
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
				oWriter.print(oSubObs.m_iObsType.getUnit());
				oWriter.print(",");
				oWriter.print(m_oDecimal.format(oSubObs.m_dEnglishValue));
				oWriter.print(",");
				oWriter.print(oSubObs.m_iObsType.getEnglishUnit());
				oWriter.print(",");
				oWriter.print(m_oDecimal.format(oSubObs.m_fConfidence));
				oWriter.print(",");
				outputQch(oWriter, oSubObs.m_nRunFlags, oSubObs.m_nPassedFlags);
				oWriter.println();
			}

			// output the end of file
			oWriter.print("END OF RECORDS");

			if (sFilename != null)
			{
				oWriter.print(" -- ");
				oWriter.print(nId);
				oWriter.print(":");
				oWriter.println(sFilename);
			}
        }
        catch(Exception oExp)
        {
            oExp.printStackTrace(System.out);
        }
	}


    /**
     * Updates the quality check flag buffers ({@code m_cRunFlags} and
     * {@code m_cPassFlags}) with the supplied run-flag, and pass-flag integer
     * values.
     * @param nRunFlags bit-field showing which quality checking algorithms
     * were ran.
     * @param nPassFlags bit-field showing whether the corresponding quality
     * check algoritm passed or failed.
     */
	protected void updateFlags(int nRunFlags, int nPassFlags)
	{
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
     * @param oWriter output stream to write quality checking data to. Connected
     * prior to the call to this method.
     * @param nRunFlags bit field corresponding to the quality checking
     * algorithms ran.
     * @param nPassFlags pass/fail bit field corresponding to the quality
     * checking algorithm ran.
     */
	protected void outputQch(PrintWriter oWriter, int nRunFlags, int nPassFlags)
	{
		updateFlags(nRunFlags, nPassFlags);

		// now generate the P,N,/,- output
		int nIndex = getQchLength();
		while (nIndex-- > 0)
		{
			if (m_cRunFlags[nIndex] == '0')
			{
				if (m_cPassFlags[nIndex] == '0')
					oWriter.print("/");
				else
					oWriter.print("-");
			}
			else
			{
				if (m_cPassFlags[nIndex] == '0')
					oWriter.print("N");
				else
					oWriter.print("P");
			}

			// output the trailing comma
			if (nIndex > 0)
				oWriter.print(",");
		}
	}


    /**
     * Compares the two {@code SubObs}.
     *
     * @param oSubObsL object to compare to {@code oSubObsR}.
     * @param oSubObsR object to compare to {@code oSubObsL}.
     * 
     * @return 0 if the {@code SubObs} match by contributor id, observation-type
     * id, and timestamp. 
     */
	public int compare(SubObs oSubObsL, SubObs oSubObsR)
	{
        // sort the observations for neat output by contrib, obstype, timestamp
		int nCompare = oSubObsL.m_oContrib.m_nId - oSubObsR.m_oContrib.m_nId;
		if (nCompare != 0)
			return nCompare;

		nCompare = oSubObsL.m_nObsTypeId - oSubObsR.m_nObsTypeId;
		if (nCompare != 0)
			return nCompare;

		return ((int)(oSubObsL.m_lTimestamp - oSubObsR.m_lTimestamp));
	}
}
