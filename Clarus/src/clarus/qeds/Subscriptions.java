/**
 * Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
 * 
 * Author: 	n/a
 * Date: 	n/a
 * 
 * Modification History:
 *		dd-Mmm-yyyy		iii		[Bug #]
 *			Change description.
 *
 * 		29-Jun-2012		das		
 * 			Quoted maxValue in SUBS_QUERY due to it being a reserved word
 *			in newer releases of MySQL server.
 */
/**
 * @file Subscriptions.java
 */
package clarus.qeds;

import java.io.File;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.TimeZone;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import clarus.Units;
import clarus.emc.ObsTypes;
import clarus.emc.Sensors;
import clarus.emc.Stations;
import clarus.qedc.IObs;
import clarus.qedc.ObsMgr;
import util.Config;
import util.ConfigSvc;
import util.Scheduler;

/**
 * Provides an interface and control class that gathers and processes
 * subscription data on a scheduled basis.
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link Subscriptions#getInstance()}
 * </p>
 * <p>
 * Implements {@code Runnable} to allow {@code Subscriptions} to be the target
 * of, and allow execution by threads.
 * </p>
 */
public class Subscriptions implements Runnable
{
	/**
	 * Pointer to the singleton instance of {@code Subscriptions}
	 */
	private static Subscriptions g_oInstance = new Subscriptions();

	/**
	 * Observation query format.
	 */
	private static String OBS_QUERY = "SELECT obsType, sensorId, timestamp, " +
		"latitude, longitude, elevation, value, confidence, runFlags, " +
		"passedFlags FROM obs WHERE updated >= ? AND updated < ? " +
		"ORDER BY obsType, sensorId";

	/**
	 * Subscriptions query format.
	 */
	private static String SUBS_QUERY = "SELECT id, lat1, lng1, lat2, lng2, " +
		"obsTypeId, minValue, `maxValue`, qchRun, qchFlags, format, cycle " +
		"FROM subscription WHERE expires >= ? ORDER BY cycle, id";

	/**
	 * Subscriptions cleanup query used in conjunction with lifetime attribute.
	 */
	private static String SUBS_DELETE = "SELECT id FROM subscription " +
		"WHERE expires < ?";

    /**
     * Cached obs cleanup query.
     */
	private static final String OBS_DELETE =
		"DELETE FROM obs WHERE timestamp < ?";

	/**
	 * <b> Accessor </b>
	 * @return the singleton instance of {@code Subscriptions}.
	 */
	public static Subscriptions getInstance()
	{
		return g_oInstance;
	}

	/**
	 * Not currently used - length of time to keep {@code Subscription}
	 * records.
	 */
	private long m_lLifetime;
	/**
	 * Directory to store subscription files in.
	 */
	private String m_sSubsDir;
	/**
	 * Configured observation data source.
	 */
	private DataSource m_iDsObs;
	/**
	 * Configured subscriptions data source.
	 */
	private DataSource m_iDsSubs;
	/**
	 * Output formats. Hash binds file extension to a corresponding output
	 * format instance.
	 * <blockquote>
	 *  "CSV" => OutputCsv formatter. <br />
	 *  "CMML" => OutputCmml formatter. <br />
	 *  "XML" => OutputXml formatter. <br />
	 * </blockquote>
	 *
	 * @see OutputCsv
	 * @see OutputCmml
	 * @see OutputXml
	 */
	Hashtable<String, OutputFormat> m_oFormatters =
		new Hashtable<String, OutputFormat>();
	/**
	 * Timestamp format
	 */
	SimpleDateFormat m_oDateFormat = new SimpleDateFormat("yyyyMMdd_HHmm");
	/**
	 * Pointer to {@code ObsTypes} cache.
	 */
	ObsTypes m_oObsTypes = ObsTypes.getInstance();
	/**
	 * Pointer to {@code Sensors} cache.
	 */
	Sensors m_oSensors = Sensors.getInstance();
	/**
	 * Pointer to {@code Stations} cache.
	 */
	Stations m_oStations = Stations.getInstance();
	/**
	 * Pointer to {@code Contribs} cache.
	 */
	Contribs m_oContribs = Contribs.getInstance();
	/**
	 * Pointer to {@code Units} conversions.
	 */
	Units m_oUnits = Units.getInstance();


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Configures {@code Subscriptions}. Gets storage directory, and
	 * datasources. Configures the formatter Hash, and timezone. Schedules
	 * itself to run on a 5-minute cycle.
	 * </p>
	 */
    public Subscriptions()
    {
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_sSubsDir = oConfig.getString("subsRoot", "./");

		// add a trailing slash if one was not set
		if (!m_sSubsDir.endsWith("/"))
			m_sSubsDir += "/";

		m_lLifetime = oConfig.getLong("lifetime", 604800L);
		// convert to milliseconds
		m_lLifetime *= 1000;

		// save the datasource references
		ClarusMgr oClarusMgr = ClarusMgr.getInstance();
		m_iDsObs = oClarusMgr.getDataSource(oConfig.getString("obsDataSource", ""));
		m_iDsSubs = oClarusMgr.getDataSource(oConfig.getString("subsDataSource", ""));

		m_oFormatters.put("CMML", new OutputCmml());
		m_oFormatters.put("CSV", new OutputCsv());
		m_oFormatters.put("CSV2", new OutputCsv2());
		m_oFormatters.put("KML", new OutputKml());
		m_oFormatters.put("XML", new OutputXml());

		m_oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		
		// set the five-minute subscription fulfillment interval
		Scheduler.getInstance().schedule(this, 0, 300);
	}


	/**
	 * Executes database observation query, gathering observations that fall
	 * within the given time bounds, and populates the provided
	 * {@code SubObs} list with the results.
	 * @param oSubObsList
	 * @param lMinTime lower time-bound.
	 * @param lMaxTime upper time-bound.
	 */
	private void getObs(ArrayList<SubObs> oSubObsList, 
		long lMinTime, long lMaxTime)
	{
		try
		{
			// attempt to get the needed obs cache connection
			if (m_iDsObs == null)
				return;

			Connection iObsDb = m_iDsObs.getConnection();
			if (iObsDb == null)
				return;

			// now that we have database connections, get the obs
			PreparedStatement iObsQuery = iObsDb.prepareStatement(OBS_QUERY);
//			Timestamp oTsRange = new Timestamp(lMinTime);
//			iObsQuery.setTimestamp(1, oTsRange);
//			oTsRange.setTime(lMaxTime);
//			iObsQuery.setTimestamp(2, oTsRange);
			iObsQuery.setLong(1, lMinTime);
			iObsQuery.setLong(2, lMaxTime);

			// cache obs records as objects with resolved contribs and stations
			ResultSet iObsResults = iObsQuery.executeQuery();
			while (iObsResults.next())
			{
				SubObs oSubObs = new SubObs(m_oContribs, m_oStations,
					m_oSensors, m_oUnits, m_oObsTypes, iObsResults);

				// only add obs that have valid metadata and can be distributed
				if
				(
					oSubObs.m_iObsType != null &&
					oSubObs.m_iSensor != null &&
					oSubObs.m_iSensor.getDistGroup() > 1 &&
					oSubObs.m_oContrib != null &&
					oSubObs.m_iStation != null
				)
					oSubObsList.add(oSubObs);
			}
			// free obs database resources
			iObsResults.close();
			iObsQuery.close();
			iObsDb.close();
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}


	/**
	 * Wraps {@link Subscriptions#getObs}, providing the time-bounds from the
	 * supplied {@code Subscription}. The data gathered from the call to
	 * {@code getObs} is then output in the format specified by the provided
	 * {@code Subscription} to the provided output stream.
	 * @param oWriter stream to write the subscription-observation data to.
	 * @param oSubs subscription matching to observations to output.
	 */
	public void getResults(PrintWriter oWriter, Subscription oSubs)
	{
		ArrayList<SubObs> oSubObsList = new ArrayList<SubObs>();
		getObs(oSubObsList, oSubs.m_lStartTime, oSubs.m_lEndTime);

		OutputFormat oOutputFormat =
			m_oFormatters.get(oSubs.m_sOutputFormat);

		if (oOutputFormat != null)
		{
			oOutputFormat.fulfill(oWriter, oSubObsList,
				oSubs, null, 0, oSubs.m_lStartTime);
		}
	}


	/**
	 * Connects to the data-source to perform the subscription query. For each
	 * subscription found, the observations are gathered, processed, and
	 * output. Runs on a five minute cycle, as configured in the default
	 * constructor.
	 * <p>
	 * Defines a required method for the implementation of {@link Runnable}.
	 * </p>
	 */
	public synchronized void run()
    {
		// process subscriptions here
		GregorianCalendar oNow = new GregorianCalendar();
		Timestamp oNowTs = new Timestamp(oNow.getTimeInMillis());

		// only one object of each is needed to deserialize database records
		ArrayList<SubObs> oSubObsList = new ArrayList<SubObs>();
		Subscription oSubs = new Subscription();

		// a 30-minute window is always queried
		ArrayList<IObs> oRawObsList = new ArrayList<IObs>();
		ObsMgr.getInstance().getObs(oRawObsList,
			oNow.getTimeInMillis() - 3900000); // 1800000
		int nIndex = oRawObsList.size();
		while (nIndex-- > 0)
		{
			SubObs oSubObs = new SubObs(m_oContribs, m_oStations,
				m_oSensors, m_oUnits, m_oObsTypes, oRawObsList.get(nIndex));

			// only add obs that have valid metadata and can be distributed
			if
			(
				oSubObs.m_iObsType != null &&
				oSubObs.m_iSensor != null &&
				oSubObs.m_iSensor.getDistGroup() > 1 &&
				oSubObs.m_oContrib != null &&
				oSubObs.m_iStation != null
			)
				oSubObsList.add(oSubObs);
		}

		try
		{
			// get the subscription information
			if (m_iDsSubs == null)
				return;

			Connection iSubsDb = m_iDsSubs.getConnection();
			if (iSubsDb == null)
				return;

			// prepare the subscription detail queries
            PreparedStatement iGetRadius = iSubsDb.prepareStatement
				("SELECT lat, lng, radius FROM subRadius WHERE subId = ?");

            PreparedStatement iGetContrib = iSubsDb.prepareStatement
				("SELECT contribId FROM subContrib WHERE subId = ?");
			
            PreparedStatement iGetStation = iSubsDb.prepareStatement
				("SELECT stationId FROM subStation WHERE subId = ?");

			// the result sets are stored in memory and can be passed to
			// other methods repeatedly
			PreparedStatement iSubsQuery = iSubsDb.prepareStatement(SUBS_QUERY);
			iSubsQuery.setTimestamp(1, oNowTs);
			ResultSet iSubsResults = iSubsQuery.executeQuery();
			while (iSubsResults.next())
			{
				// process all subscriptions for the current cycle
				int nCycle = iSubsResults.getInt(12);
				if (oNow.get(GregorianCalendar.MINUTE) % nCycle == 0)
				{
					oSubs.deserialize(iSubsResults,
						iGetRadius, iGetContrib, iGetStation);

					// ensure the subscription destination exists
					String sPath = m_sSubsDir + oSubs.m_nId;
					File oFile = new File(sPath);
					if (!oFile.exists() && !oFile.mkdirs())
						continue;
					
					OutputFormat oOutputFormat =
						m_oFormatters.get(oSubs.m_sOutputFormat);

					if (oOutputFormat != null)
					{
						String sFilename = m_oDateFormat.format(
							oNow.getTime()) + oOutputFormat.getSuffix();
						oFile = new File(sPath + "/" + sFilename);

						PrintWriter oPrintWriter = new PrintWriter(oFile);
						oOutputFormat.fulfill(oPrintWriter, oSubObsList,
							oSubs, sFilename, oSubs.m_nId,
							oNow.getTimeInMillis() - (nCycle * 60000));

						// finish writing the output
						oPrintWriter.flush();
						oPrintWriter.close();
					}
				}
			}
			// free most of the subscription database resources
			iSubsResults.close();
			iGetRadius.close();
			iGetContrib.close();
			iGetStation.close();
			iSubsQuery.close();

			// clean up expired subscriptions
			ArrayList<Integer> oSubIds = new ArrayList<Integer>();
			iSubsQuery = iSubsDb.prepareStatement(SUBS_DELETE);
			iSubsQuery.setTimestamp(1, oNowTs);
			iSubsResults = iSubsQuery.executeQuery();
			while (iSubsResults.next())
				oSubIds.add(new Integer(iSubsResults.getInt(1)));

			iSubsResults.close();
			iSubsQuery.close();

			// prepare the subscription delete queries
			iSubsQuery = iSubsDb.prepareStatement
				("DELETE FROM subscription WHERE id = ?");

			iGetRadius = iSubsDb.prepareStatement
				("DELETE FROM subRadius WHERE subId = ?");

            iGetContrib = iSubsDb.prepareStatement
				("DELETE FROM subContrib WHERE subId = ?");

            iGetStation = iSubsDb.prepareStatement
				("DELETE FROM subStation WHERE subId = ?");

			nIndex = oSubIds.size();
			while (nIndex-- > 0)
			{
				int m_nSubId = oSubIds.get(nIndex).intValue();

				iGetRadius.setInt(1, m_nSubId);
				iGetRadius.executeUpdate();

				iGetContrib.setInt(1, m_nSubId);
				iGetContrib.executeUpdate();

				iGetStation.setInt(1, m_nSubId);
				iGetStation.executeUpdate();

				iSubsQuery.setInt(1, m_nSubId);
				iSubsQuery.executeUpdate();
			}

			// free the remainder of the subscription database resources
			iGetRadius.close();
			iGetContrib.close();
			iGetStation.close();
			iSubsQuery.close();			
			iSubsDb.close();

			// remove old files and directories
			long lRemoveTime = oNow.getTimeInMillis() - m_lLifetime;

			// iterate through each subscription directory
			File[] oSubscriptions = new File(m_sSubsDir).listFiles();
			nIndex = oSubscriptions.length;
			while (nIndex-- > 0)
			{
				File oSubsDir = oSubscriptions[nIndex];
				File[] oSubFiles = oSubsDir.listFiles();

				if (oSubsDir.lastModified() < lRemoveTime)
				{
					// remove all files when the directory has expired
					int nFileIndex = oSubFiles.length;
					while (nFileIndex-- > 0)
						oSubFiles[nFileIndex].delete();

					// now remove the top-level subscription directory
					oSubsDir.delete();
				}
				else
				{
					int nFileIndex = oSubFiles.length;
					while (nFileIndex-- > 0)
					{
						File oFile = oSubFiles[nFileIndex];
						// remove files older than the cutoff except README.txt
						if
						(
							oFile.lastModified() < lRemoveTime &&
							oFile.getName().compareTo("README.txt") != 0
						)
							oFile.delete();
					}
				}
			}

			if (m_iDsObs != null)
			{
				// delete cached observations older than 7 days
				Connection iObsDb = m_iDsObs.getConnection();
				PreparedStatement iDeleteQuery =
					iObsDb.prepareStatement(OBS_DELETE);
				oNowTs.setTime(oNow.getTimeInMillis() - 604800000L);
				iDeleteQuery.setTimestamp(1, oNowTs);
				iDeleteQuery.executeUpdate();
				iDeleteQuery.close();
				iObsDb.close();
			}
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
    }
}
