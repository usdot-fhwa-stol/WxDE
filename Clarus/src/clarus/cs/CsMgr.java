// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsMgr.java
 */

package clarus.cs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimerTask;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import util.Config;
import util.ConfigSvc;
import util.Scheduler;

/**
 * Manages the list of {@code CollectorSvc} which it creates as it is
 * instantiated. When the thread is scheduled to run, it traverses the list
 * of collection services, calling {@see CollectorSvc#retry} which will attempt
 * to collect on that service until the service either runs out of collection
 * attempts, or finishes its collection.
 *
 * <p>
 * Singleton class whose instance can be accessed by the
 * {@see CsMgr#getInstance} method.
 * </p>
 *
 * <p>
 * {@code CsMgr} implements {@see java.lang.Runnable} so instances can be the
 * target of Thread instances.
 * </p>
 */
public class CsMgr implements Runnable
{
    /**
     * Interval with which to attempt observation collections.
     */
	private static int RETRY_INTERVAL = 300;
    /**
     * Formatted database query of the form:
     * id, contribId, offset, interval, name, classname,
     * endpoint, username, password
     */
	private static String CSVC_QUERY = "SELECT id, contribId, " + 
		"midnightOffset, collectionInterval, instanceName, className, " + 
		"endpoint, username, password FROM csvc WHERE active = 1 ORDER BY id";

    /**
     * The instance of the manager.
     */
	private static CsMgr g_oInstance = new CsMgr();

    /**
     * Each element is a collector service which is created in the constructor,
     * and who's type is based off database queries.
     */
	private ArrayList<CollectorSvc> m_oSvcs = new ArrayList<CollectorSvc>();
    /**
     * Timer to schedule tasks.
     */
    private TimerTask m_oTask;
    /**
     *  Timestamps from the location of data collection.
     */
	private DateFormatFactory m_oTimestamps;
    /**
     *  Timezone information from the location of data collection.
     */
	private TimezoneFactory m_oTimezones;
	
	/**
     * Get the singleton instance of CsMgr.
     * @return The instance of CsMgr.
     */
	public CsMgr getInstance()
	{
		return g_oInstance;
	}
	
	/**
     * Default Constructor.
     * <p>
     * Reads the configuration file to determine the retry interval, and
     * location of the contributor database. Loads timestamp information for the
     * corresponding data. Creates new instances of {@see CollectorSvc}
     * containing the contributor data, and stores them in the
     * {@code CollectorSvc} list ({@see CsMgr#m_oSvcs}). Finally the task is
     * scheduled based off {@code RETRY_INTERVAL}.
     * </p>
     */
	public CsMgr()
	{
		try
		{
			Config oConfig = ConfigSvc.getInstance().getConfig(this);
			
			// set up the retry interval
			RETRY_INTERVAL = oConfig.getInt("retry", RETRY_INTERVAL);
			
			// get the database connection pool
			ClarusMgr oClarusMgr = ClarusMgr.getInstance();
			DataSource iDataSource = oClarusMgr.
				getDataSource(oConfig.getString("datasource", null));
			
			if (iDataSource == null)
				return;

			Connection iConnection = iDataSource.getConnection();
			if (iConnection == null)
				return;
			
			// load the timezone information
			m_oTimezones = new TimezoneFactory(iConnection);
			
			// load the timestamp format information
			m_oTimestamps = new DateFormatFactory(iConnection);
			
			ResultSet iResultSet = 
				iConnection.createStatement().executeQuery(CSVC_QUERY);

			while (iResultSet.next())
			{
				try
				{
					// create an instance of the specified class that
					// inherits from the collector service base class
					CollectorSvc oSvc = (CollectorSvc)Class.
						forName(iResultSet.getString(6)).newInstance();					
					
					oSvc.init(iResultSet.getInt(1), iResultSet.getInt(2), 
						iResultSet.getInt(3), iResultSet.getInt(4), 
						iResultSet.getString(5), iResultSet.getString(7), 
						iResultSet.getString(8), iResultSet.getString(9), 
						this, iConnection, RETRY_INTERVAL);
					
					m_oSvcs.add(oSvc);
				}
				catch (Exception oException)
				{
					// supress class intantiation errors
					oException.printStackTrace();
				}
			}
			iResultSet.close();
			iConnection.close();
			
			// create the schedule used to retry failed collection attempts
			m_oTask = Scheduler.getInstance().schedule(this, 0, RETRY_INTERVAL);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
			
		System.out.println(getClass().getName());
	}
	
	/**
     * Attempts to collect data from each element in the {@code CollectionSvc} 
     * list, until the corresponding {@code CollectionSvc} either succeeds at
     * collecting the observation data, or it runs out of retry attempts.
     */
	public void run()
	{
		// global default interval to retry any collections that were missed
		for (int nIndex = 0; nIndex < m_oSvcs.size(); nIndex++)
			m_oSvcs.get(nIndex).retry();
	}
	
	/**
     * Creates the date format for the locale of the running JVM.
     *
     * @param nIndex Index of the {@code CollectorSvc} that corresponds to the
     * timestamp.
     *
     * @return The date format for the locale of the JVM.
     *
     * @see DateFormatFactory
     * @see DateFormat
     */
	public DateFormat createDateFormat(int nIndex)
	{
		return m_oTimestamps.createDateFormat(nIndex);
	}
	
	/**
     * Creates timezone for the locale of collected data.
     *
     * @param nIndex Index of the {@code CollectorSvc} that corresponds to the
     * timezone.
     *
     * @return The timezone as described above.
     *
     * @see TimezoneFactory
     * @see SimpleTimeZone
     */
	public SimpleTimeZone createSimpleTimeZone(int nIndex)
	{
		return m_oTimezones.createSimpleTimeZone(nIndex);
	}
}
