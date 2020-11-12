// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsMgr.java
 */

package wde.cs;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimerTask;
import javax.sql.DataSource;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;

/**
 * Manages the list of {@code CollectorSvc} which it creates as it is
 * instantiated. When the thread is scheduled to run, it traverses the list
 * of collection services, calling {@see CollectorSvc#retry} which will attempt
 * to collect on that service until the service either runs out of collection
 * attempts, or finishes its collection.
 * <p>
 * Singleton class whose instance can be accessed by the
 * {@see CsMgr#getInstance} method.
 * </p>
 * <p>
 * {@code CsMgr} implements {@see java.lang.Runnable} so instances can be the
 * target of Thread instances.
 * </p>
 */
public class CsMgr implements Runnable {
    private static final Logger logger = Logger.getLogger(CsMgr.class);

    /**
     * Interval with which to attempt observation collections.
     */
    private static int RETRY_INTERVAL = 300;
    /**
     * Formatted database query of the form:
     * id, contribId, offset, interval, name, classname,
     * endpoint, username, password
     */
//	private static String CSVC_QUERY = "SELECT id, contribId, " +
//		"midnightOffset, collectionInterval, instanceName, className, " +
//		"endpoint, username, password FROM conf.csvc WHERE id=33 AND active = 1 ORDER BY id";
//    private static String CSVC_QUERY = "SELECT id, contribId, " +
//        "midnightOffset, collectionInterval, instanceName, className, " +
//        "endpoint, username, password FROM conf.csvc WHERE contribid=16 AND active = 1 ORDER BY id";
    private static String CSVC_QUERY = "SELECT id, contribId, " +
            "midnightOffset, collectionInterval, instanceName, className, " +
            "endpoint, username, password FROM conf.csvc WHERE active = 1 ORDER BY id";
    /**
     * The instance of the manager.
     */
    private static CsMgr g_oInstance = new CsMgr();
    /**
     * URL used to verify altitude information
     */
    private String m_sAltUrl;
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
     * Timestamps from the location of data collection.
     */
    private DateFormatFactory m_oTimestamps;
    /**
     * Timezone information from the location of data collection.
     */
    private TimezoneFactory m_oTimezones;

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
    public CsMgr() {


            Config oConfig = ConfigSvc.getInstance().getConfig(this);
						m_sAltUrl = oConfig.getString("url", "http://otile1.data-env.com/elev/");

            // set up the retry interval
            RETRY_INTERVAL = oConfig.getInt("retry", RETRY_INTERVAL);

            // get the database connection pool
            WDEMgr wdeMgr = WDEMgr.getInstance();
            DataSource iDataSource = wdeMgr.
                    getDataSource(oConfig.getString("datasource", null));

            if (iDataSource == null)
                return;

        try(Connection iConnection = iDataSource.getConnection();
            PreparedStatement ps = iConnection.prepareStatement(CSVC_QUERY);
            ResultSet rs = ps.executeQuery();) {


            // load the timezone information
            m_oTimezones = new TimezoneFactory(iConnection);

            // load the timestamp format information
            m_oTimestamps = new DateFormatFactory(iConnection);


            while (rs.next()) {
                try {
                    // create an instance of the specified class that
                    // inherits from the collector service base class
                    CollectorSvc oSvc = (CollectorSvc) Class.
                            forName(rs.getString(6)).newInstance();

                    oSvc.init(rs.getInt(1), rs.getString(2),
                            rs.getInt(3), rs.getInt(4),
                            rs.getString(5), rs.getString(7),
                            rs.getString(8), rs.getString(9),
                            this, iConnection, RETRY_INTERVAL);

                    m_oSvcs.add(oSvc);
                } catch (Exception e) {
                    // suppress class instantiation errors
                    e.printStackTrace();
                    logger.error(e.getMessage());
                }
            }

            // create the schedule used to retry failed collection attempts
            m_oTask = Scheduler.getInstance().schedule(this, 0, RETRY_INTERVAL, true);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        logger.info("Completing constructor");
    }

    /**
     * Get the singleton instance of CsMgr.
     *
     * @return The instance of CsMgr.
     */
    public static CsMgr getInstance() {
        return g_oInstance;
    }

    /**
     * Attempts to collect data from each element in the {@code CollectionSvc}
     * list, until the corresponding {@code CollectionSvc} either succeeds at
     * collecting the observation data, or it runs out of retry attempts.
     */
    public void run() {
        // global default interval to retry any collections that were missed
        for (int nIndex = 0; nIndex < m_oSvcs.size(); nIndex++)
            m_oSvcs.get(nIndex).retry();
    }


    /**
     * Looks up altitude information from a web service if provided altitude
		 * is missing.
     *
     * @param nLat	the latitude used to check the altitude
     * @param nLon	the longitude used to check the altitude
     * @param tElev	the altitude value to check
		 *
     * @return	the altitude for the given location
     */
    public short checkElev(int nLat, int nLon, short tElev)
		{
			if (tElev == Short.MIN_VALUE)
			{
				try // lookup altitude when unknown for provided location
				{
					URL oUrl = new URL(m_sAltUrl + nLat + "/" + nLon);
					URLConnection oUrlConn = oUrl.openConnection();
					oUrlConn.setConnectTimeout(500); // half second timeout

					oUrlConn.connect();
					int nLen = oUrlConn.getContentLength();
					try (InputStream oIn = oUrlConn.getInputStream())
					{
						StringBuilder sBuf = new StringBuilder();
						while (nLen-- > 0)
							sBuf.append((char)oIn.read());

						double dVal = Double.parseDouble(sBuf.toString());
						if (!Double.isNaN(dVal))
							return (short)Math.round(dVal);
					}
				}
				catch (Exception oException)
				{
				}
			}
			return tElev; // altitude is known or lookup failed
    }


    /**
     * Creates the date format for the locale of the running JVM.
     *
     * @param nIndex Index of the {@code CollectorSvc} that corresponds to the
     *               timestamp.
     * @return The date format for the locale of the JVM.
     * @see DateFormatFactory
     * @see DateFormat
     */
    public DateFormat createDateFormat(int nIndex) {
        return m_oTimestamps.createDateFormat(nIndex);
    }

    /**
     * Creates timezone for the locale of collected data.
     *
     * @param nIndex Index of the {@code CollectorSvc} that corresponds to the
     *               timezone.
     * @return The timezone as described above.
     * @see TimezoneFactory
     * @see SimpleTimeZone
     */
    public SimpleTimeZone createSimpleTimeZone(int nIndex) {
        return m_oTimezones.createSimpleTimeZone(nIndex);
    }
}
