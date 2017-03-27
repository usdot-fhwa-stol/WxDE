package wde.qeds;

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.ObsTypeDao;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.dao.Units;
import wde.security.AccessControl;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.util.Scheduler;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.File;
import java.io.PrintWriter;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.TimeZone;
import org.postgresql.util.PSQLException;
import wde.cs.ext.NDFD;
import wde.cs.ext.RAP;
import wde.cs.ext.RTMA;
import wde.data.osm.Road;
import wde.data.osm.Roads;

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
public class FcstSubscriptions implements Runnable, Comparator<Road> {
    private static final long NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000L;
    static Logger logger = Logger.getLogger(FcstSubscriptions.class);
    /**
     * Pointer to the singleton instance of {@code Subscriptions}
     */
    private static final FcstSubscriptions g_oInstance = new FcstSubscriptions();

    /**
     * Subscriptions query format.
     */
    private static String SUBS_QUERY = "SELECT id, lat1, lng1, lat2, lng2, " +
            "obsTypeId, minValue, maxValue, qchRun, qchFlags, format, cycle " +
            "FROM subs.subscription WHERE expires >= ? AND id<0 ORDER BY cycle, id";
	 /**
	  * Obs types for NDFD
	  */
	 private final int[] m_nNdfdObsTypes = new int[]{575, 593, 5733, 56104};
	 /**
	  * Obs types for RAP
	  */
	 private final int[] m_nRapObsTypes = new int[]{207, 554, 587, 2074, 2075, 2076, 2077};
	 /**
	  * Obs types for RTMA
	  */
	 private final int[] m_nRtmaObsTypes = new int[]{554, 575, 593, 5101, 5733, 56104, 56105, 56108};
	 /**
	  * Obs types for alerts, warnings, and forecasted infer obs that come from the database
	  */
	 private final int[] m_nDbObsTypes = new int[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 589, 5102, 51137};

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
    public FcstSubscriptions() {
        Config oConfig = ConfigSvc.getInstance().getConfig("wde.qeds.Subscriptions");
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
		  //ensure all of the obs type arrays are sorted because binary search is used with them
		  Arrays.sort(m_nNdfdObsTypes);
		  Arrays.sort(m_nRapObsTypes);
		  Arrays.sort(m_nRtmaObsTypes);
		  Arrays.sort(m_nDbObsTypes);
        // set the five-minute subscription fulfillment interval
        Scheduler.getInstance().schedule(this, 0, 3600, true);
    }

    /**
     * <b> Accessor </b>
     * @return the singleton instance of {@code Subscriptions}.
     */
    public static FcstSubscriptions getInstance() {
        return g_oInstance;
    }

	 /**
	  * Executes forecast getReadings and database queries to populate the given 
	  * list with forecast observations that meet the specifications of the given 
	  * Forecast Subscription
	  * 
	  * 
	  * @param oSubObsList empty list to be populated with observations
	  * @param oSub the Forecast Subscription
	  */
    private void getObs(ArrayList<SubObs> oSubObsList, FcstSubscription oSub) 
	 {
		Connection iObsDb = null;
		ResultSet iRs = null;
		NDFD oNDFD = NDFD.getInstance();
		RAP oRAP = RAP.getInstance();
		RTMA oRTMA = RTMA.getInstance();
		int nMaxLat = (int)Math.max(MathUtil.toMicro(oSub.m_dLat1), MathUtil.toMicro(oSub.m_dLat2));
		int nMinLat = (int)Math.min(MathUtil.toMicro(oSub.m_dLat1), MathUtil.toMicro(oSub.m_dLat2));
		int nMaxLon = (int)Math.max(MathUtil.toMicro(oSub.m_dLng1), MathUtil.toMicro(oSub.m_dLng2));
		int nMinLon = (int)Math.min(MathUtil.toMicro(oSub.m_dLng1), MathUtil.toMicro(oSub.m_dLng2));
		ArrayList<SubObs> oTempList = new ArrayList();
		ArrayList<Road> oRoadList = new ArrayList();
		Roads oRoads = Roads.getInstance();
		oRoads.getLinks(oRoadList, 1, MathUtil.toMicro(oSub.m_dLng1), MathUtil.toMicro(oSub.m_dLat1), MathUtil.toMicro(oSub.m_dLng2), MathUtil.toMicro(oSub.m_dLat2)); //get all roads in the bounding box
		int nIndex = oRoadList.size();
		while (nIndex-- > 0) //remove roads that have a midpoint outside of the bounding box of the subscription
		{
			Road oTempRoad = oRoadList.get(nIndex);
			if (oTempRoad.m_nYmid > nMaxLat || oTempRoad.m_nYmid < nMinLat ||
				 oTempRoad.m_nXmid > nMaxLon || oTempRoad.m_nXmid < nMinLon)
				oRoadList.remove(nIndex);
		}
		Calendar oNow = new GregorianCalendar();
		//set the time to the beginning of the hour
		oNow.set(Calendar.MILLISECOND, 0);
		oNow.set(Calendar.SECOND, 0);
		oNow.set(Calendar.MINUTE, 0);
		try
		{
			//get the data source for the obs tables
			if (m_iDsObs != null)
				iObsDb = m_iDsObs.getConnection();
			
			Arrays.sort(oSub.m_nObsTypes); //sort obstypeids in ascending order
			oRoadList.sort(this); //sort the roads by latitude then longitute
			for (int nObsType : oSub.m_nObsTypes) //for each obstype
			{
				oTempList.clear(); //clear the temporary list
				boolean bNdfdObs = false;
				boolean bDbObs = false;
				boolean bRtmaObs = false;
				boolean bRapObs = false;
				boolean bCaught = false;
				//determine which type of obs it is
				if (Arrays.binarySearch(m_nNdfdObsTypes, nObsType) >= 0)
					bNdfdObs = true;
				if (Arrays.binarySearch(m_nDbObsTypes, nObsType) >= 0)
					bDbObs = true;
				if (Arrays.binarySearch(m_nRtmaObsTypes, nObsType) >= 0)
					bRtmaObs = true;
				if (Arrays.binarySearch(m_nRapObsTypes, nObsType) >= 0)
					bRapObs = true;
				if (iObsDb != null)
				{
					Timestamp oNowTs = new Timestamp(oNow.getTimeInMillis());
					String sTableName = String.format("obs_%d-%02d-%02d", oNow.get(GregorianCalendar.YEAR),
						oNow.get(GregorianCalendar.MONTH) + 1, oNow.get(GregorianCalendar.DAY_OF_MONTH));
					Statement iStatement = iObsDb.createStatement();
					Calendar oTomorrow = new GregorianCalendar();
					oTomorrow.setTimeInMillis(oNow.getTimeInMillis());
					oTomorrow.set(GregorianCalendar.DAY_OF_MONTH, oTomorrow.get(GregorianCalendar.DAY_OF_MONTH) + 1);
					String sTomTableName = String.format("obs_%d-%02d-%02d", oTomorrow.get(GregorianCalendar.YEAR),
						oTomorrow.get(GregorianCalendar.MONTH) + 1, oTomorrow.get(GregorianCalendar.DAY_OF_MONTH));
					try //try getting obs from today and tomorrow
					{
						iRs = iStatement.executeQuery("SELECT * FROM("
							+ "SELECT obstypeid, obstime, recvtime, latitude, longitude, value "
							+ "FROM obs.\"" + sTableName + "\" "
							+ "WHERE obstypeid = " + nObsType + " AND obstime >= '" + oNowTs.toString().substring(0,19) + "' AND obstime >= recvtime "
							+ "UNION "
							+ "SELECT obstypeid, obstime, recvtime, latitude, longitude, value "
							+ "FROM obs.\"" + sTomTableName + "\" "
							+ "WHERE obstypeid = " + nObsType + " AND obstime >= '" + oNowTs.toString().substring(0, 19) + "' AND obstime >= recvtime) AS a "
							+ "ORDER BY latitude, longitude, obstime, recvtime");
					}
					catch (PSQLException oException) //tomorrow's table might not exist yet, if it doesn't just get results from today's table
					{
						bCaught = true;
					}
					finally
					{
						if (bCaught) //the query didn't run so run it for today's table only
						{
							iRs = iStatement.executeQuery("SELECT obstypeid, obstime, recvtime, latitude, longitude, value FROM obs.\"" + sTableName + 
							"\" WHERE obstypeid = " + nObsType + " AND obstime >= '" + oNowTs.toString().substring(0,19) + "' AND obstime>=recvtime ORDER BY latitude, longitude, obstime, recvtime");
						}
						if (iRs != null)
						{
							while (iRs.next()) //put the result set in memory so we can filter out noncurrent forecasts
								oTempList.add(new SubObs(iRs.getInt(1), iRs.getTimestamp(2).getTime(), 
									iRs.getTimestamp(3).getTime(), iRs.getInt(4), iRs.getInt(5), 0, iRs.getDouble(6), obsTypeDao));
							iRs.close();
						}
					}
				}

				nIndex = oTempList.size() - 1; //start at the next to last obs in the list
				while (nIndex-- > 0)
				{
					SubObs oTempSubObs = oTempList.get(nIndex);
					//remove noncurrent forecasts from the list (the list is ordered by latitude, then longitude, then obstime, then received time)
					if (oTempSubObs.m_dLat < nMinLat || oTempSubObs.m_dLat > nMaxLat ||
						 oTempSubObs.m_dLon < nMinLon || oTempSubObs.m_dLon > nMaxLon ||
						(oTempSubObs.m_dLat == oTempList.get(nIndex + 1).m_dLat && 
						 oTempSubObs.m_dLon == oTempList.get(nIndex + 1).m_dLon && 
						 oTempSubObs.m_lTimestamp == oTempList.get(nIndex + 1).m_lTimestamp))
						oTempList.remove(nIndex);
				}
				//the last object never got checked in the other loop so check it now
				if (!oTempList.isEmpty())
					if(oTempList.get(oTempList.size() - 1).m_dLat < nMinLat || 
							oTempList.get(oTempList.size() - 1).m_dLat > nMaxLat ||
							oTempList.get(oTempList.size() - 1).m_dLon < nMinLon || 
							oTempList.get(oTempList.size() - 1).m_dLon > nMaxLon)
						oTempList.remove(oTempList.size() -1);
				for (Road oRoad : oRoadList)
				{
					for (int i = 0; i <= 6; i++) //for each road and each forecast hour get the desired observation
					{
						long lFcstTime = oNow.getTimeInMillis() + (i * 3600000);
						double dVal = Double.NaN;
						if (bRapObs)
						{
							dVal = oRAP.getReading(nObsType, lFcstTime, oRoad.m_nYmid, oRoad.m_nXmid);
						}
						else if (bDbObs && !oTempList.isEmpty())
						{
							SubObs oTemp = oTempList.get(0); //only need to check the first obs since the list is sorted by lat, then lon, then obstime
							if (oTemp.m_dLat == oRoad.m_nYmid && oTemp.m_dLon == oRoad.m_nXmid && oTemp.m_lTimestamp >= lFcstTime && oTemp.m_lTimestamp < lFcstTime + 3600000) //check if the obs is the correct lat, lon, and time
							{
								dVal = oTemp.m_dValue;
								oTempList.remove(0);
							}
						}
						else if (i == 0 && bRtmaObs)
						{
							dVal = oRTMA.getReading(nObsType, lFcstTime, oRoad.m_nYmid, oRoad.m_nXmid);
						}
						else if(bNdfdObs)
						{
							dVal = oNDFD.getReading(nObsType, lFcstTime, oRoad.m_nYmid, oRoad.m_nXmid);
						}
						if (!Double.isNaN(dVal)) //if the value got set, add the obs to the list
						{
							oSubObsList.add(new SubObs(nObsType, lFcstTime, oNow.getTimeInMillis(), oRoad.m_nYmid, oRoad.m_nXmid, oRoad.m_tElev, dVal, obsTypeDao));
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
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
    public void getResults(HttpServletRequest request, PrintWriter writer, FcstSubscription subs) 
	 {
        ArrayList<SubObs> oSubObsList = new ArrayList<SubObs>();
        boolean isSuperUser = AccessControl.isSuperUser(request);

        logger.debug("getResults() -- begin calling getObs()");

        GenericPrincipal principal = (GenericPrincipal) request.getUserPrincipal();

        logger.info("Statistics on-demand query requester username: " + principal.getName());

        getObs(oSubObsList, subs);

        logger.debug("getResults() -- end calling getObs()");

        OutputFormat oOutputFormat =
                m_oFormatters.get(subs.m_sOutputFormat);

        if (oOutputFormat != null) {
            oOutputFormat.fulfill(writer, oSubObsList,
                    subs, null, 0, subs.m_lStartTime, false);
        }
        logger.debug("getResults() -- end of method");
    }


    /**
     * Connects to the data-source to perform the subscription query. For each
     * subscription found, the observations are gathered, processed, and
     * output. Runs on a 60 minute cycle, as configured in the default
     * constructor.
     * <p>
     * Defines a required method for the implementation of {@link Runnable}.
     * </p>
     */
    public synchronized void run() {
        logger.info("run() invoked");

        // process subscriptions here
        GregorianCalendar oNow = new GregorianCalendar();
		  oNow.set(GregorianCalendar.MILLISECOND, 0);
		  oNow.set(GregorianCalendar.SECOND, 0);
		  oNow.set(GregorianCalendar.MINUTE, 0);
        Timestamp oNowTs = new Timestamp(oNow.getTimeInMillis());

        // only one object of each is needed to deserialize database records
        ArrayList<SubObs> oSubObsList = new ArrayList<SubObs>();
        FcstSubscription oSubs = new FcstSubscription();
        try {
            // get the subscription information
            if (m_iDsSubs == null)
                return;

            Connection iSubsDb = m_iDsSubs.getConnection();
            if (iSubsDb == null)
                return;
            // prepare the subscription detail queries
				PreparedStatement iGetSubObs = iSubsDb.prepareStatement("SELECT obstypeid FROM subs.subobs WHERE subId = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            // the result sets are stored in memory and can be passed to
            // other methods repeatedly
            PreparedStatement iSubsQuery = iSubsDb.prepareStatement(SUBS_QUERY);
            iSubsQuery.setTimestamp(1, oNowTs);
            ResultSet iSubsResults = iSubsQuery.executeQuery();
            while (iSubsResults.next()) 
				{
					oSubObsList.clear();
                // process all subscriptions for the current cycle
                int nCycle = iSubsResults.getInt(12);
                if (oNow.get(GregorianCalendar.MINUTE) % nCycle == 0) 
					 {
                    oSubs.deserialize(iSubsResults, iGetSubObs);
						  getObs(oSubObsList, oSubs);
                    // ensure the subscription destination exists
                    String sPath = m_sSubsDir + -oSubs.m_nId;
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
                                oNow.getTimeInMillis() - (nCycle * 60000), false);

                        // finish writing the output
                        oPrintWriter.flush();
                        oPrintWriter.close();
                    }
                }
            }
            // free most of the subscription database resources
				iGetSubObs.close();
            iSubsResults.close();
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
				iGetSubObs = iSubsDb.prepareStatement("DELETE FROM subs.subobs WHERE subId = ?");

            int nIndex = oSubIds.size();
            while (nIndex-- > 0) {
                int nSubId = oSubIds.get(nIndex).intValue();

					 iGetSubObs.setInt(1, nSubId);
					 iGetSubObs.executeUpdate();
					 
                iSubsQuery.setInt(1, nSubId);
                iSubsQuery.executeUpdate();
					 

            }

            // free the remainder of the subscription database resources
				iGetSubObs.close();
            iSubsQuery.close();
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
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        logger.info("run() returning");
    }
	 
	 
	/**
	 * Gets and increments the next unique subscription id 
	 * @return subscription id
	 */ 
	public int getNextId()
	{
		synchronized(Subscription.g_oNextId)
		{
			return Subscription.g_oNextId.m_nNextId++;
		}
	}

	
	/**
	 * Compares two roads by latitude then longitude
	 * @param o1 road 1
	 * @param o2 road 2
	 * @return 
	 */
	@Override
	public int compare(Road o1, Road o2)
	{
		int nReturn = o1.m_nYmid - o2.m_nYmid;
		if (nReturn == 0)
			nReturn = o1.m_nXmid - o2.m_nXmid;
		
		return nReturn;
	}
}
