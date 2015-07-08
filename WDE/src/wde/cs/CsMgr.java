// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsMgr.java
 */

package wde.cs;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.SimpleTimeZone;
import java.util.TimerTask;

/**
 * Manages the list of {@code CollectorSvc} which it creates as it is
 * instantiated. When the thread is scheduled to run, it traverses the list
 * of collection services, calling {@see CollectorSvc#retry} which will attempt
 * to collect on that service until the service either runs out of collection
 * attempts, or finishes its collection.
 * <p/>
 * <p>
 * Singleton class whose instance can be accessed by the
 * {@see CsMgr#getInstance} method.
 * </p>
 * <p/>
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
        Connection iConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            Config oConfig = ConfigSvc.getInstance().getConfig(this);

            // set up the retry interval
            RETRY_INTERVAL = oConfig.getInt("retry", RETRY_INTERVAL);

            // get the database connection pool
            WDEMgr wdeMgr = WDEMgr.getInstance();
            DataSource iDataSource = wdeMgr.
                    getDataSource(oConfig.getString("datasource", null));

            if (iDataSource == null)
                return;

            iConnection = iDataSource.getConnection();
            if (iConnection == null)
                return;

            // load the timezone information
            m_oTimezones = new TimezoneFactory(iConnection);

            // load the timestamp format information
            m_oTimestamps = new DateFormatFactory(iConnection);

            ps = iConnection.prepareStatement(CSVC_QUERY);
            rs = ps.executeQuery();

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
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
                iConnection.close();
                iConnection = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        logger.info("Completing constructor");
    }

    /**
     * Get the singleton instance of CsMgr.
     *
     * @return The instance of CsMgr.
     */
    public CsMgr getInstance() {
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