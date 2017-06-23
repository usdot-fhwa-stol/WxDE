/**
 * Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
 * <p/>
 * Author: 	n/a
 * Date: 	n/a
 * <p/>
 * Modification History:
 * dd-Mmm-yyyy		iii		[Bug #]
 * Change description.
 * <p/>
 * 29-Jun-2012		das
 * Quoted maxValue in SUBS_QUERY due to it being a reserved word
 * in newer releases of MySQL server.
 *
 * @file Subscriptions.java
 * @file Subscriptions.java
 */
/**
 * @file Subscriptions.java
 */
package wde.qeds;

import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.TimeZone;
import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.ObsTypeDao;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.dao.Units;
import wde.obs.IObs;
import wde.obs.ObsArchiveMgr;
import wde.security.AccessControl;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.util.Scheduler;

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
public class Subscriptions implements Runnable {
    private static final long NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000L;
    static Logger logger = Logger.getLogger(Subscriptions.class);
    /**
     * Pointer to the singleton instance of {@code Subscriptions}
     */
    private static Subscriptions g_oInstance = new Subscriptions();

    /**
     * Subscriptions query format.
     */
    private static String SUBS_QUERY = "SELECT id, lat1, lng1, lat2, lng2, " +
            "obsTypeId, minValue, maxValue, qchRun, qchFlags, format, cycle " +
            "FROM subs.subscription WHERE expires >= ? AND id>0 ORDER BY cycle, id";

    /**
     * Subscriptions cleanup query used in conjunction with lifetime attribute.
     */
    private static String SUBS_DELETE = "SELECT id FROM subs.subscription " +
            "WHERE expires < ?";

    /**
     * Cached obs cleanup query.
     */
//	private static final String OBS_DELETE =
//		"DELETE FROM obs.obs WHERE timestamp < ?";
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
    ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();
    /**
     * Pointer to {@code Sensors} cache.
     */
    SensorDao sensorDao = SensorDao.getInstance();
    /**
     * Pointer to {@code PlatformDao}.
     */
    PlatformDao platformDao = PlatformDao.getInstance();
    /**
     * Pointer to {@code Contribs} cache.
     */
    Contribs m_oContribs = Contribs.getInstance();
    /**
     * Pointer to {@code Units} conversions.
     */
    Units m_oUnits = Units.getInstance();

    NotificationEvaluator m_oNotificationEvaluator = new NotificationEvaluator();
    /**
     * Not currently used - length of time to keep {@code Subscription}
     * records.
     */
    private long m_lLifetime;
    /**
     * maximum rows of records to process for a query.
     */
    private int maxRows;
    /**
     * maximum time for executing a query.
     */
    private long maxTime;
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
     * <b> Default Constructor </b>
     * <p>
     * Configures {@code Subscriptions}. Gets storage directory, and
     * datasources. Configures the formatter Hash, and timezone. Schedules
     * itself to run on a 5-minute cycle.
     * </p>
     */
    public Subscriptions() {
        Config oConfig = ConfigSvc.getInstance().getConfig(this);
        m_sSubsDir = oConfig.getString("subsRoot", "./");

        // add a trailing slash if one was not set
        if (!m_sSubsDir.endsWith("/"))
            m_sSubsDir += "/";

        m_lLifetime = oConfig.getLong("lifetime", 604800L);
        // convert to milliseconds
        m_lLifetime *= 1000;

        maxRows = oConfig.getInt("maxrows", 100000);

        maxTime = oConfig.getLong("maxtime", 30000);

//		Added for Belle Isle
//	    maxRows = oConfig.getInt("maxrows", 10000000);
//	    maxTime = oConfig.getLong("maxtime",  600000);

        // save the datasource references
        WDEMgr wdeMgr = WDEMgr.getInstance();
        m_iDsObs = wdeMgr.getDataSource(oConfig.getString("obsDataSource", ""));
        m_iDsSubs = wdeMgr.getDataSource(oConfig.getString("subsDataSource", ""));

        m_oFormatters.put("CMML", new OutputCmml());
        m_oFormatters.put("CSV", new OutputCsv());
        m_oFormatters.put("KML", new OutputKml());
        m_oFormatters.put("XML", new OutputXml());

        m_oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        // set the five-minute subscription fulfillment interval
        Scheduler.getInstance().schedule(this, 0, 300, true);
    }

    /**
     * <b> Accessor </b>
     * @return the singleton instance of {@code Subscriptions}.
     */
    public static Subscriptions getInstance() {
        return g_oInstance;
    }

    /**
     * Executes database observation query, gathering observations that fall
     * within the given time bounds, and populates the provided
     * {@code SubObs} list with the results.
     * @param oSubObsList
     * @param lMinTime lower time-bound.
     * @param lMaxTime upper time-bound.
     * @return 0 if the query is fully executed, 1 if the query is not executed
     * 2 if the row number exceeds configured limit, 3 if time exceeds configured limit
     */
    private int getObs(ArrayList<SubObs> oSubObsList, Subscription sub, boolean isSuperUser) {
        int status = 1;

        Connection dbConn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            // attempt to get the needed obs cache connection
            if (m_iDsObs == null)
                return status;

            dbConn = m_iDsObs.getConnection();
            if (dbConn == null)
                return status;

            String contribListStr = sub.getContributors();
            String platformListStr = null;

            if (contribListStr != null && contribListStr.length() > 0) {
                contribListStr = "(" + contribListStr + ")";

                platformListStr = "(";
                for (Integer pi : sub.m_oPlatformIds) {
                    platformListStr += pi.intValue() + ",";
                }
                platformListStr = platformListStr.substring(0, platformListStr.length() - 1) + ")";
            }

            int rowCount = 0;
            long startQueryTime = System.currentTimeMillis();
            Timestamp beginTime = new Timestamp(sub.m_lStartTime);
            Timestamp endTime = new Timestamp(sub.m_lEndTime);
            Timestamp runningTime = new Timestamp(sub.m_lStartTime);

            logger.info("Statistics on-demand query obsType: " + sub.m_nObsTypes
                    + " Start time: " + beginTime + " End time: " + endTime
                    + " Contributors: " + contribListStr + " Platforms: " + platformListStr);

            while (runningTime.getTime() < (sub.m_lEndTime + NUM_OF_MILLI_SECONDS_IN_A_DAY)) {
                String tableName = "obs_" + runningTime.toString().substring(0, 10);
                runningTime.setTime(runningTime.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                DatabaseMetaData dbm = dbConn.getMetaData();
                ResultSet tables = dbm.getTables(null, null, tableName, null);
                if (!tables.next()) {
                    tables.close();
                    logger.debug(tableName + " does not exist");
                    continue;
                }
                tables.close();

                String queryStr = null;
                String obsTypeStr1 = "";
                String obsTypeStr2 = "";
                if (sub.m_nObsTypes != null)
                {
                  StringBuilder conditionBuilder = new StringBuilder(50);
                  for(int obstype : sub.m_nObsTypes)
                    conditionBuilder.append(",").append(obstype);

                  String obstypeList = conditionBuilder.substring(1);

                  conditionBuilder.setLength(0);
                  obsTypeStr1 =conditionBuilder.append(" and o.obsTypeId IN(").append(obstypeList).append(")").toString();
                  conditionBuilder.setLength(0);
                  obsTypeStr2 = conditionBuilder.append(" and obsTypeId IN(").append(obstypeList).append(")").toString();
                }

                if (contribListStr != null && contribListStr.length() > 2)
                    queryStr = "SELECT o.* FROM obs.\"" + tableName + "\" o, meta.sensor s " +
                            "WHERE obsTime >= ? AND obsTime < ? AND o.sensorId=s.id " +
                            "AND s.contribId IN " + contribListStr + " AND s.platformId in " + platformListStr + obsTypeStr1 +
                            " ORDER BY obsTypeId, sourceId, sensorId limit " + (maxRows - rowCount);
                else
                    queryStr = "SELECT * FROM obs.\"" + tableName + "\" " +
                            "WHERE obsTime >= ? AND obsTime < ?" +
                            " AND latitude > " + MathUtil.toMicro(sub.m_dLat1) + " AND latitude < " + MathUtil.toMicro(sub.m_dLat2) +
                            " AND longitude > " + MathUtil.toMicro(sub.m_dLng1) + " AND longitude < " + MathUtil.toMicro(sub.m_dLng2) + obsTypeStr2 +
                            " ORDER BY obsTypeId, sourceId, sensorId limit " + (maxRows - rowCount);

                // now that we have database connections, get the obs
                ps = dbConn.prepareStatement(queryStr);

                ps.setTimestamp(1, beginTime);
                ps.setTimestamp(2, endTime);

                // cache obs records as objects with resolved contribs and stations
                rs = ps.executeQuery();

                while (rs.next()) {

                    SubObs oSubObs = new SubObs(m_oContribs, platformDao,
                            sensorDao, m_oUnits, obsTypeDao, rs);

                    // only add obs that have valid metadata and can be distributed
                    if
                            (
                            oSubObs.m_iObsType != null &&
                                    oSubObs.m_iSensor != null &&
                                    (oSubObs.m_iSensor.getDistGroup() == 2 || oSubObs.m_iSensor.getDistGroup() == 0 && isSuperUser) &&
                                    oSubObs.m_oContrib != null &&
                                    oSubObs.m_iPlatform != null
                            )
                        oSubObsList.add(oSubObs);
                    rowCount++;
                }

                if (rowCount >= maxRows) {
                    status = 2;
                    break;
                }

                long now = System.currentTimeMillis();
                if (now - startQueryTime >= maxTime) {
                    status = 3;
                    break;
                }
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            }
            dbConn.close();
            dbConn = null;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        return status;
    }


    /**
     * Wraps {@link Subscriptions#getObs}, providing the time-bounds from the
     * supplied {@code Subscription}. The data gathered from the call to
     * {@code getObs} is then output in the format specified by the provided
     * {@code Subscription} to the provided output stream.
     * @param oWriter stream to write the subscription-observation data to.
     * @param oSubs subscription matching to observations to output.
     */
    public void getResults(HttpServletRequest request, PrintWriter writer, Subscription subs) {
        ArrayList<SubObs> oSubObsList = new ArrayList<SubObs>();
        boolean isSuperUser = AccessControl.isSuperUser(request);

        logger.debug("getResults() -- begin calling getObs()");

        GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();

        logger.info("Statistics on-demand query requester username: " + principal.getName());

        int status = getObs(oSubObsList, subs, isSuperUser);

        logger.debug("getResults() -- end calling getObs()");

        OutputFormat oOutputFormat =
                m_oFormatters.get(subs.m_sOutputFormat);

        if (status == 2 && oSubObsList.size() > 0)
            writer.println(String.format(OutputFormat.warning1, maxRows));

        if (status == 3 && oSubObsList.size() > 0)
            writer.println(String.format(OutputFormat.warning2, (maxTime / 1000)));

        if (oOutputFormat != null) {
            oOutputFormat.fulfill(writer, oSubObsList,
                    subs, null, 0, subs.m_lStartTime, false);
        }
        logger.debug("getResults() -- end of method");
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
    public synchronized void run() {
        logger.info("run() invoked");

        // process subscriptions here
        GregorianCalendar oNow = new GregorianCalendar();
        Timestamp oNowTs = new Timestamp(oNow.getTimeInMillis());

        long lNotificationObsTimeCutoff = oNow.getTimeInMillis() - 1000L * 60 * 5;

        // only one object of each is needed to deserialize database records
        ArrayList<SubObs> oSubObsList = new ArrayList<SubObs>();
        ArrayList<SubObs> oNotificationObsList = new ArrayList<SubObs>();
        Subscription oSubs = new Subscription();

        // a 30-minute window is always queried
        ArrayList<IObs> oRawObsList = new ArrayList<IObs>();
        ObsArchiveMgr.getInstance().getObs(oRawObsList,
                oNow.getTimeInMillis() - 3900000); // 65 minutes
        int nIndex = oRawObsList.size();
        while (nIndex-- > 0) {
            SubObs oSubObs = new SubObs(m_oContribs, platformDao,
                    sensorDao, m_oUnits, obsTypeDao, oRawObsList.get(nIndex));

            // only add obs that have valid metadata
            if
                    (
                    oSubObs.m_iObsType != null &&
                            oSubObs.m_iSensor != null &&
                            oSubObs.m_oContrib != null &&
                            oSubObs.m_iPlatform != null
                    )
            {
              //only add obs since the last time notifications were evaluated
              if(oSubObs.recvTime >= lNotificationObsTimeCutoff)
                oNotificationObsList.add(oSubObs);

              //only add obs that can be distributed
              if(oSubObs.m_iSensor.getDistGroup() == 2)
                oSubObsList.add(oSubObs);
            }
        }

        try {
            // get the subscription information
            if (m_iDsSubs == null)
                return;

            Connection iSubsDb = m_iDsSubs.getConnection();
            if (iSubsDb == null)
                return;

            // prepare the subscription detail queries
            PreparedStatement iGetRadius = iSubsDb.prepareStatement("SELECT lat, lng, radius FROM subs.subRadius WHERE subId = ?");
            PreparedStatement iGetContrib = iSubsDb.prepareStatement("SELECT contribId FROM subs.subContrib WHERE subId = ?");
            PreparedStatement iGetStation = iSubsDb.prepareStatement("SELECT stationId FROM subs.subStation WHERE subId = ?");
            PreparedStatement iGetSubObs = iSubsDb.prepareStatement("SELECT obstypeid FROM subs.subobs WHERE subId = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            // the result sets are stored in memory and can be passed to
            // other methods repeatedly
            PreparedStatement iSubsQuery = iSubsDb.prepareStatement(SUBS_QUERY);
            iSubsQuery.setTimestamp(1, oNowTs);
            ResultSet iSubsResults = iSubsQuery.executeQuery();
            while (iSubsResults.next()) {
                // process all subscriptions for the current cycle
                int nCycle = iSubsResults.getInt(12);
                if (oNow.get(GregorianCalendar.MINUTE) % nCycle == 0) {
                    oSubs.deserialize(iSubsResults, iGetRadius, iGetContrib, iGetStation, iGetSubObs);

                    // ensure the subscription destination exists
                    String sPath = m_sSubsDir + oSubs.m_nId;
                    File oFile = new File(sPath);
                    if (!oFile.exists() && !oFile.mkdirs())
                        continue;

                    OutputFormat oOutputFormat =
                            m_oFormatters.get(oSubs.m_sOutputFormat);

                    if (oOutputFormat != null) {
                        String sFilename = m_oDateFormat.format(
                                oNow.getTime()) + oOutputFormat.getSuffix();
                        oFile = new File(sPath + "/" + sFilename);

                        PrintWriter oPrintWriter = new PrintWriter(oFile);
                        oOutputFormat.fulfill(oPrintWriter, oSubObsList,
                                oSubs, sFilename, oSubs.m_nId,
                                oNow.getTimeInMillis() - (nCycle * 60000), true);

                        // finish writing the output
                        oPrintWriter.flush();
                        oPrintWriter.close();
                    }
                }
            }
            // free most of the subscription database resources
            iSubsResults.close();
            iGetRadius.close();
            iGetSubObs.close();
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
            iSubsQuery = iSubsDb.prepareStatement("DELETE FROM subs.subscription WHERE id = ?");
            iGetRadius = iSubsDb.prepareStatement("DELETE FROM subs.subRadius WHERE subId = ?");
            iGetContrib = iSubsDb.prepareStatement("DELETE FROM subs.subContrib WHERE subId = ?");
            iGetStation = iSubsDb.prepareStatement("DELETE FROM subs.subStation WHERE subId = ?");
    		iGetSubObs = iSubsDb.prepareStatement("DELETE FROM subs.subobs WHERE subId = ?");

            nIndex = oSubIds.size();
            while (nIndex-- > 0) {
                int nSubId = oSubIds.get(nIndex).intValue();

                iGetRadius.setInt(1, nSubId);
                iGetRadius.executeUpdate();

                iGetContrib.setInt(1, nSubId);
                iGetContrib.executeUpdate();

                iGetStation.setInt(1, nSubId);
                iGetStation.executeUpdate();

                iSubsQuery.setInt(1, nSubId);
                iSubsQuery.executeUpdate();

              iGetSubObs.setInt(1, nSubId);
              iGetSubObs.executeUpdate();
            }

            // free the remainder of the subscription database resources
            iGetRadius.close();
            iGetContrib.close();
            iGetStation.close();
            iSubsQuery.close();
            iGetSubObs.close();
            iSubsDb.close();

            // remove old files and directories
            long lRemoveTime = oNow.getTimeInMillis() - m_lLifetime;

            // iterate through each subscription directory
            File[] oSubscriptions = new File(m_sSubsDir).listFiles();
            nIndex = oSubscriptions.length;
            while (nIndex-- > 0) {
                File oSubsDir = oSubscriptions[nIndex];
                File[] oSubFiles = oSubsDir.listFiles();

                if (oSubsDir.lastModified() < lRemoveTime) {
                    // remove all files when the directory has expired
                    int nFileIndex = oSubFiles.length;
                    while (nFileIndex-- > 0)
                        oSubFiles[nFileIndex].delete();

                    // now remove the top-level subscription directory
                    oSubsDir.delete();
                } else {
                    int nFileIndex = oSubFiles.length;
                    while (nFileIndex-- > 0) {
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

            m_oNotificationEvaluator.processNotifications(oNotificationObsList);

//			if (m_iDsObs != null)
//			{
//				// delete cached observations older than 7 days
//				Connection iObsDb = m_iDsObs.getConnection();
//				PreparedStatement iDeleteQuery =
//					iObsDb.prepareStatement(OBS_DELETE);
//				oNowTs.setTime(oNow.getTimeInMillis() - 604800000L);
//				iDeleteQuery.setTimestamp(1, oNowTs);
//				iDeleteQuery.executeUpdate();
//				iDeleteQuery.close();
//				iObsDb.close();
//			}
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

        logger.info("run() returning");
    }


	/**
	 * Gets and increments the next unique subscription id
	 * @return subscription id
	 */
	 public int getNextId()
	{
		return FcstSubscriptions.getInstance().getNextId();
	}
}
