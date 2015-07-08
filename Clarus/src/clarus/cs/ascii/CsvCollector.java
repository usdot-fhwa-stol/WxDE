// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsvCollector.java
 */

package clarus.cs.ascii;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import clarus.cs.CsMgr;
import clarus.cs.ICollector;
import clarus.emc.Sensors;
import clarus.emc.Stations;
import clarus.emc.ISensor;
import clarus.emc.IStation;
import clarus.qedc.IObsSet;
import util.Text;
import util.io.CharTokenizer;
import util.net.NetConn;

/**
 * Provides a means of collecting data from CSV files, and converting them to
 * observation objects, which are then added to the Obs-cache.
 *
 * <p>
 * Implements {@code ICollector} to perform the collection and processing of
 * csv observations from contributing agencies.
 * </p>
 */
public class CsvCollector implements ICollector
{
    /**
     * Collector service Id.
     */
	int m_nId;
    /**
     * Time to wait, in seconds, after collection attempt occurs.
     */
	protected int m_nDelay;
    /**
     * Latitude read from input for mobile data
     */
	protected int m_nLat;
    /**
     * Longitude read from input for mobile data
     */
	protected int m_nLon;
    /**
     * Elevation read from input for mobile data
     */
	protected short m_tElev;
    /**
     * Sensor index, retrieved from database.
     */
	protected int m_nSensorIndex;
    /**
     * Contributor Identifier. Tracks which contributor data is gathered from.
     */
	protected int m_nContribId;
    /**
     * Number of lines to skip at beginning of file.
     */
	protected int m_nSkipLines;
    /**
     * Retry collection attempt flag.
     */
	protected boolean m_bRetry;
    /**
     * Default station code used when source does not contain this information
     */
	protected String m_sDefaultStationCode;

    /**
     * Query format string:
     * <pre>    {columnId}, {observation type id}, {sensor index}, </pre>
     * <pre>    {column width}, {conversion multiplier}, </pre>
     * <pre>    {collector name}, {units}, {values to ignore} </pre>
     */
	private String m_sQuery = "SELECT columnId, obsTypeId, sensorIndex, " +
		"colWidth, multiplier, className, unit, ignoreValues " + 
		"FROM csvcoldef WHERE collectorId = ?";

    /**
     * Timestamp accumulates pieces of the timestamp that tracks the 
	 * data collection time.
     */
	protected StringBuilder m_sTimestamp = new StringBuilder();
    /**
     * Station code id
     */
	protected StringBuilder m_sStationCode = new StringBuilder();
    /**
     * Last date/time of collection. Millisecond precision.
     */
	protected Date m_oLastCollection = new Date();
    /**
     * Timestamp format.
     */
	protected DateFormat m_oTsFormat;
    /**
     * Timestamp to use for filename of input file.
     */
	protected DateFormat m_oFilepath;
    /**
     * List of {@link DataValue} used to wrap collection of different csv
     * data-types through the use of
     * {@link DataValue#readData(java.lang.StringBuilder) } on extensions of
     * {@code DataValue}
     */
	protected ArrayList<DataValue> m_oColumns = new ArrayList<DataValue>();

    /**
     * Parent Collector service object.
     */
	protected CsvSvc m_oCsvSvc;
    /**
     * Used for processing objects with delimiter and newline strings.
     */
	protected CharTokenizer m_oTokenizer;

    /**
     * Instance of Station observation resolving service.
     */
	protected Stations m_oStations = Stations.getInstance();
    /**
     * Instance of Sensor observation resolving service.
     */
	protected Sensors m_oSensors = Sensors.getInstance();

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code CsvCollector}
	 * </p>
	 */
	protected CsvCollector()
	{
	}
	
	/**
     * Initializes {@code CsvCollector} attributes. Sets up the timezone,
     * and timestamp formats. Initializes the character tokenizer with the
     * supplied delimiter, and newline characters. Prepares and executes
     * the query ({@code m_sQuery}), and creates the corresponding
     * {@code DataValue} handler for each column, which is then added to the
     * list of {@code DataValue} handlers ({@code m_oColumns}), then sorts
     * the list in ascending order, based off the column identifier.
     *
     * @param nId collector service ID
     * @param nDelay time to wait, in milliseconds, after collection time
     *          occurs.
     * @param bRetry retry collection attempt flag.
     * @param nCollectTzId collector timezone id.
     * @param nContentTzId content timezone id.
     * @param nTimestampId timestamp format id.
     * @param nSkipLines configured lines to skip at the beginning of the file.
     * @param sDelimiter configured delimiter marker.
     * @param sNewline configured line terminator.
     * @param sFilepath string describing timestamp formatted filename.
     * @param oCsMgr main {@code CsMgr} instance. Ready to manage collector
     *          services.
     * @param oCsvSvc parent CSV collector service, ready for query.
     * @param iConnection connection to SQL, assumes it is connected and ready
     *          to read.
     */
	protected CsvCollector(int nId, int nDelay, boolean bRetry, 
		int nCollectTzId, int nContentTzId, int nTimestampId, int nSkipLines, 
		String sDelimiter, String sNewline, String sStationCode,
		String sFilepath, CsMgr oCsMgr, CsvSvc oCsvSvc, Connection iConnection)
	{
		m_nId = nId;
		m_sDefaultStationCode = sStationCode;
		
		// set the number of seconds to wait after the collection time occurs 
		// and if this information collector should retry on failure
		m_nDelay = nDelay * 1000; // convert seconds to milliseconds
		m_bRetry = bRetry;

		// set up the file content timestamp reader
		m_oTsFormat = oCsMgr.createDateFormat(nTimestampId);
		m_oTsFormat.setTimeZone(oCsMgr.createSimpleTimeZone(nContentTzId));
		
		// set up the filename timestamp generator
		m_oFilepath = new SimpleDateFormat(sFilepath);
		m_oFilepath.setTimeZone(oCsMgr.createSimpleTimeZone(nCollectTzId));		
		
		// save the number of lines to skip, usually skips over file headers
		m_nSkipLines = nSkipLines;
		
		// set up the csv processing objects with delimiter and newline strings
		m_oTokenizer = new CharTokenizer(sDelimiter, sNewline);
		
		// save the parent collector service object
		m_oCsvSvc = oCsvSvc;

		// create the handler objects that interpret column information 
		// configuration information is stored in a configuration database
		try
		{
			PreparedStatement iPreparedStatement = 
				iConnection.prepareStatement(m_sQuery);
			iPreparedStatement.setInt(1, nId);
			ResultSet iResultSet = iPreparedStatement.executeQuery();
			
			while (iResultSet.next())
			{
				DataValue oDataHandler = (DataValue)Class.
					forName(iResultSet.getString(6)).newInstance();
				
				int nSensorIndex = iResultSet.getInt(3);
				if (iResultSet.wasNull())
					nSensorIndex = -1;
				
				oDataHandler.init(iResultSet.getInt(1), iResultSet.getInt(2), 
					nSensorIndex, iResultSet.getInt(4), 
					iResultSet.getDouble(5), iResultSet.getString(7), 
					iResultSet.getString(8), oCsMgr, this);
				
				m_oColumns.add(oDataHandler);
			}
			
			iResultSet.close();
			iPreparedStatement.close();
			
			Collections.sort(m_oColumns);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	
	/**
     * Reads the file {@code m_oFilepath} by column, adding the observations
     * to the parent Collector Service's ({@code m_oCsvSvc}) Obs-cache.
     *
     * @param nContribId The contributor id we're interested in collecting from.
     * @param oNetConn Input stream, previously connected.
     * @param lTimestamp Collection time, not delay-adjusted.
     */
	public void collect(int nContribId, NetConn oNetConn, long lTimestamp)
	{
		m_nContribId = nContribId;
		
		// adjust the task execution time by the delay to derive 
		// the actual collection time that generates filenames
		lTimestamp -= m_nDelay;
		
		// verify that this is a unique collection request
		if (m_oLastCollection.getTime() == lTimestamp)
			return;
		
		// collect column data here
		m_oLastCollection.setTime(lTimestamp);
		
		try
		{
			if (oNetConn.open(m_oFilepath.format(m_oLastCollection)))
			{
				// skip the configured number of header lines
				m_oTokenizer.setInput(oNetConn);
				int nIndex = m_nSkipLines;
				while (nIndex-- > 0 && m_oTokenizer.nextSet());

				// process each record
				int nColumn = 0;
				StringBuilder sColumnBuffer = new StringBuilder();
				while (m_oTokenizer.nextSet())
				{
					// clear the required station information
					m_sTimestamp.setLength(0);
					m_sStationCode.setLength(0);
					m_nLat = Integer.MIN_VALUE;
					m_nLon = Integer.MIN_VALUE;
					m_tElev = Short.MIN_VALUE;
					
					// process each column for the record
					nColumn = 0;
					while (nColumn < m_oColumns.size() &&
						m_oTokenizer.hasTokens())
					{
						m_oTokenizer.nextToken(sColumnBuffer);
						// remove whitespace characters as necessary
						Text.removeWhitespace(sColumnBuffer);
							
						// the column is set to null when there is no data
						m_oColumns.get(nColumn++).readData(sColumnBuffer);
					}
					
					// process all of the observations for the current record
					for (nColumn = 0; nColumn < m_oColumns.size(); nColumn++)
						m_oColumns.get(nColumn).writeData();
				}

				oNetConn.close();
			}
			else
				m_oLastCollection.setTime(0);
		}
		catch (Exception oException)
		{
			try
			{
				oNetConn.close();
			}
			catch (Exception oIOException)
			{
			}
			
			m_oLastCollection.setTime(0);
		}
	}


	/**
     * Attempts to convert the observation timing information to a timestamp.
	 * A single string is parsed, but may have been used to accumulate
	 * pieces of a timestamp.
     *
     * @return millisecond value of timestamp
     */
	protected long getTimestamp()
	{
		long lTimestamp = 0L;
		if (m_sTimestamp.length() > 0)
		{
			try
			{
				// attempt to parse the contents of the timestamp
				lTimestamp = m_oTsFormat.parse(m_sTimestamp.toString()).getTime();
			}
			catch (Exception oException)
			{
			}
		}
		return lTimestamp;
	}


	/**
     * Attempts to get the station based on the station code from the cache of
     * {@code Stations}. This method is used to find the station needed to
	 * create a valid observation.
     *
     * @return IStation
     */
	IStation getStation()
	{
		String sStationCode = m_sDefaultStationCode;
		if (m_sStationCode.length() > 0)
			sStationCode = m_sStationCode.toString();

		// attempt to get the station for this observation
		return m_oStations.getStation(m_nContribId, sStationCode);
	}

	
	/**
     * If the station is found for the observation, the corresponding sensor
     * is retrieved from the {@code Sensors} cache. Finally, the Obs set for the
     * supplied observation type id is retrieved, and the observation is added.
     *
     * @param nObsTypeId
     * @param nSensorIndex
     * @param dValue
     */
	void createObs(int nObsTypeId, int nSensorIndex, double dValue)
	{
		// an observation must have at least a timestamp and station 
		// the sensor index is optional as it defaults to zero
		long lTimestamp = getTimestamp();
		if (lTimestamp == 0)
			return;

		IStation iStation = getStation();
		if (iStation != null)
		{
			// if a station is available, attempt to find the correct sensor
			ISensor iSensor = null;
			if (iStation != null)
			{
				// override the default sensor index as needed
				if (nSensorIndex < 0)
					nSensorIndex = m_nSensorIndex;

				// override default geo-coordiates if not read from file
				if (m_nLat < -90000000 && m_nLon < -180000000)
				{
					m_nLat = iStation.getLat();
					m_nLon = iStation.getLon();
					m_tElev = iStation.getElev();
				}
					
				iSensor = m_oSensors.getSensor(iStation.getId(), 
					nObsTypeId, nSensorIndex);
			}
			
			// if both a station and sensor are found then get the obs set
			IObsSet iObsSet = null;
			if (iSensor != null)
				iObsSet = m_oCsvSvc.getObsSet(nObsTypeId);
			
			// now that we have all three components, save the observation
			if (iObsSet != null)
			{
				iObsSet.addObs
				(
					iSensor.getSensorId(), 
					lTimestamp, m_nLat, m_nLon, m_tElev, dValue
				);
			}
		}
	}
	

	/**
     * Convert the latitude attribute {@code m_nLat} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
	void setLat(StringBuilder sBuffer)
	{
		try
		{
			m_nLat = Stations.toMicro(Double.parseDouble(sBuffer.toString()));
		}
		catch (Exception oException)
		{
			m_nLat = Integer.MIN_VALUE;
		}
	}


	/**
     * Convert the longitude attribute {@code m_nLon} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
	void setLon(StringBuilder sBuffer)
	{
		try
		{
			m_nLon = Stations.toMicro(Double.parseDouble(sBuffer.toString()));
		}
		catch (Exception oException)
		{
			m_nLon = Integer.MIN_VALUE;
		}
	}


	/**
     * Convert the elevation attribute {@code m_tElev} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
	void setElev(StringBuilder sBuffer)
	{
		try
		{
			m_tElev = (short)Double.parseDouble(sBuffer.toString());
		}
		catch (Exception oException)
		{
			m_tElev = Short.MIN_VALUE;
		}
	}


	/**
     * Set the sensor index attribute {@code m_nSensorIndex} to the integer
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the sensor index integer.
     */
	void setSensorIndex(StringBuilder sBuffer)
	{
		try
		{
			m_nSensorIndex = Integer.parseInt(sBuffer.toString());
		}
		catch (Exception oException)
		{
			m_nSensorIndex = 0;
		}
	}


    /**
     * Set the timestamp attribute {@code m_lTimestamp} to the time
     * value counter contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the time value counter.
     */
	public void setTimestamp(StringBuilder sBuffer)
	{
		// insert configured delimiter to recombine timestamp pieces
		if (m_sTimestamp.length() > 0)
			m_sTimestamp.append(" ");

		m_sTimestamp.append(sBuffer);
	}

    /**
     * Set the station code attribute {@code m_sStationCode}. Hyphenates the
     * value contained in the supplied string buffer, and appends it to the
     * station code attribute.
     *
     * @param sBuffer The string containing the non-hyphenated station code.
     */
	void setStationCode(StringBuilder sBuffer)
	{
		// insert hyphen for combination system and station codes
		if (m_sStationCode.length() > 0)
			m_sStationCode.append("-");
		
		m_sStationCode.append(sBuffer);
	}
}
