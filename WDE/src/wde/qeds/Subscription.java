// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Subscription.java
 */
package wde.qeds;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.MathUtil;
import wde.util.QualityCheckFlagUtil;
import wde.util.Region;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimeZone;
import wde.util.Scheduler;


/**
 * Provides filtering parameters which can be set and used to gather
 * only the observations that meet these defined parameters.
 *
 * @author bryan.krueger
 */
public class Subscription {
    private static final Logger logger = Logger.getLogger(Subscription.class);
	 public static NextId g_oNextId = new NextId();
    /**
     * Subscription id.
     */
    public int m_nId = 0;
    /**
     * How often the subscription data is gathered.
     */
    public int m_nCycle = 20;

    /**
     * Image security code.
     */
    public String m_sSecurityCode = "";
    /**
     * The password the user gave when creating the subscription.
     */
    public String m_sSecret = "";
    /**
     * The password the user gave when accessing a subscription.
     */
    public String m_sSecretAttempt = "";

    /**
     * Output file format.
     */
    public String m_sOutputFormat = "CSV";

    /**
     * Get observations no earlier than this.
     */
    public long m_lStartTime = Long.MIN_VALUE;
    /**
     * Get observations no older than this.
     */
    public long m_lEndTime = Long.MIN_VALUE;

    /**
     * Minimum region latitude pair.
     */
    public double m_dLat1 = -Double.MAX_VALUE;
    /**
     * Minimum region longitude pair.
     */
    public double m_dLng1 = -Double.MAX_VALUE;
    /**
     * Maximum region latitude pair.
     */
    public double m_dLat2 = Double.MAX_VALUE;
    /**
     * Maximum region longitude pair.
     */
    public double m_dLng2 = Double.MAX_VALUE;

    /**
     * Observation type of interest.
     */
    public int m_nObsType = 0;
    /**
     * Lower bound observation value.
     */
    public double m_dMin = Double.NEGATIVE_INFINITY;
    /**
     * Upper bound observation value.
     */
    public double m_dMax = Double.POSITIVE_INFINITY;

    /**
     * Bit field corresponding to quality checking algorithms ran.
     */
    public int m_nRun = 0;
    /**
     * Bit field corresponding to pass/fail result of qch algorithm.
     */
    public int m_nFlag = 0;

    /**
     * Comma-delimited string containing contributor id's for this subscription.
     */
    public String m_sContributors;

    /**
     * Contains the comma-delimited segments from the contributor string.
     */
    public ArrayList<Integer> m_oContribIds = new ArrayList<Integer>();
    /**
     * Contains station id's for this subscription.
     */
    public ArrayList<Integer> m_oPlatformIds = new ArrayList<Integer>();
    /**
     * Radius of the region of interest.
     */
    public PointRadius m_oRadius;
    public String m_sContactEmail = "";
    public String m_sContactName = "";
    /**
     *
     */
    private boolean m_bWizardRunning = false;
    private String name;
    private String description;
    private String subScope;

    /**
     * Creates a new default instance of {@code Subscription}.
     */
    public Subscription() {
    }


    /**
     * Initializes the attributes to their default values. Integers to zero,
     * strings to null, min and max values to negative and positive infinity,
     * output format to CSV, and the lists cleared.
     */
    public void clearAll() {
        m_nId = 0;
        m_sSecurityCode = "";
        m_sSecret = "";
        m_sSecretAttempt = "";

        m_sOutputFormat = "CSV";

        m_lStartTime = Long.MIN_VALUE;
        m_lEndTime = Long.MIN_VALUE;

        m_dLat1 = m_dLng1 = -Double.MAX_VALUE;
        m_dLat2 = m_dLng2 = Double.MAX_VALUE;

        m_nObsType = 0;
        m_dMin = Double.NEGATIVE_INFINITY;
        m_dMax = Double.POSITIVE_INFINITY;

        m_nRun = 0;
        m_nFlag = 0;

        m_sContributors = "";

        m_oContribIds.clear();
        m_oPlatformIds.clear();
    }


    public void setContactEmail(String sContactEmail) {
        if (sContactEmail != null && sContactEmail.length() > 0)
            m_sContactEmail = sContactEmail;
    }


    public void setContactName(String sContactName) {
        if (sContactName != null && sContactName.length() > 0)
            m_sContactName = sContactName;
    }


    /**
     * Takes the provided comma-delimited string and parses the min and max
     * time range, sets the corresponding time bound attributes
     * ({@code m_lStartTime} and {@code m_lEndTime}).
     *
     * @param sTimeRange comma-delimited string containing two time bounds,
     *                   the min and the max.
     */
    public void setTimeRange(String sTimeRange) {
        logger.debug("setTimeRange(" + sTimeRange + ")");
        if (sTimeRange != null && sTimeRange.length() > 0) {
            String[] sTimes = sTimeRange.split(",");
            if (sTimes.length == 2) {
                long lStartTime = Long.parseLong(sTimes[0]);
                long lEndTime = Long.parseLong(sTimes[1]);
//				long lEndTime = lStartTime + 3600000;

                // If the Start and End Times are less than 0,
                // then interpret them as the number of seconds relative
                // to the current time.
                // Otherwise, interpret them as absolute timestamps.
                long lTimeNow = System.currentTimeMillis();
                if (lStartTime <= 0) {
                    m_lStartTime = lTimeNow + lStartTime;
                } else {
                    m_lStartTime = lStartTime;
                }

                if (lEndTime <= 0) {
                    m_lEndTime = lTimeNow + lEndTime;
                } else {
                    m_lEndTime = lEndTime;
                }

                // Make sure the Start Time is before the End Time.
                if (m_lStartTime > m_lEndTime) {
                    long lTemp = m_lStartTime;
                    m_lStartTime = m_lEndTime;
                    m_lEndTime = lTemp;
                }
            }
        }
    }

    /**
     * Splits the provided comma-delimited string and stores it in the provided
     * array.
     *
     * @param sList comma-delimited list of integers.
     * @param oList list to store the delimited segments, after a call to this
     *              method, the list will be sorted in ascending order.
     */
    private void stringsToArray(String sList, ArrayList<Integer> oList) {
        if (sList != null && sList.length() > 0) {
            String[] sIds = sList.split(",");
            int nIndex = sIds.length;
            while (nIndex-- > 0) {
                try {
                    oList.add(new Integer(Integer.parseInt(sIds[nIndex])));
                } catch (Exception ex) {
                    logger.debug("Unable to convert string to integer.", ex);
                }
            }

            Collections.sort(oList);
        }
    }

    /**
     * Gets the subscription id attribute ({@code m_nId}) from the provided
     * string, and retrieves the password from the database corresponding to
     * the provided subscription id, and stores it in the secret-attribute.
     *
     * @param sSubId string providing the subscription id.
     */
    public void setSubId(String sSubId) {
        logger.debug("setSubId(" + sSubId + ")");
        m_nId = Integer.parseInt(sSubId);

        Connection iConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            DataSource iDataSource =
                    WDEMgr.getInstance().getDataSource("java:comp/env/jdbc/wxde");

            if (iDataSource == null)
                return;

            iConnection = iDataSource.getConnection();
            if (iConnection == null)
                return;

            ps = iConnection.prepareStatement("SELECT password FROM subs.subscription WHERE id=?");
            ps.setInt(1, m_nId);
            rs = ps.executeQuery();
            if (rs.next())
                m_sSecret = rs.getString(1);
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
    }

    /**
     * Populates the station-id attribute array from the provided
     * comma-delimited string containing integer values corresponding to the
     * stations of interest.
     *
     * @param sStationList comma-delimited list of station identification
     *                     numbers.
     */
    public void setStations(String sStationList) {
        logger.debug("setStations(" + sStationList + ")");

        m_oPlatformIds.clear();
        stringsToArray(sStationList, m_oPlatformIds);
    }

    /**
     * Splits the provided comma delimited string into the region bounds:
     * min-max latitude-longitude pairs. String should be of the form:
     * <blockquote>
     * min-lat, min-long, max-lat, max-long
     * </blockquote>
     * <pre>		or		</pre>
     * <blockquote>
     * lat, long, radius
     * </blockquote>
     *
     * @param sRegion comma-delimited string containing the coordinates of the
     *                region of interest, either of the form: min-lat, min-long, max-lat,
     *                max-long or lat, long, radius.
     */
    public void setRegion(String sRegion) {
        logger.debug("setRegion(" + sRegion + ")");
        double[] coordinates = Region.convert(sRegion);
        m_dLat1 = coordinates[0];
        m_dLng1 = coordinates[1];
        m_dLat2 = coordinates[2];
        m_dLng2 = coordinates[3];
    }

    /**
     * Sets the observation-type, and observation-bound filters. The string
     * should be comma-delimited of the form:
     * <blockquote>
     * observation-type, observation-min-value, observation-max-value
     * </blockquote>
     *
     * @param sObs comma-delimited string containing observation filters.
     */
    public void setObs(String sObs) {
        logger.debug("setObs(" + sObs + ")");
        m_nObsType = 0;
        m_dMin = Double.NEGATIVE_INFINITY;
        m_dMax = Double.POSITIVE_INFINITY;

        String[] sObsFilters = sObs.split(",");

        if (sObsFilters.length > 0 && sObsFilters[0] != null && sObsFilters[0].length() > 0)
            m_nObsType = Integer.parseInt(sObsFilters[0]);

        if (sObsFilters.length > 1 && sObsFilters[1] != null && sObsFilters[1].length() > 0)
            m_dMin = Double.parseDouble(sObsFilters[1]);

        if (sObsFilters.length > 2 && sObsFilters[2] != null && sObsFilters[2].length() > 0)
            m_dMax = Double.parseDouble(sObsFilters[2]);
    }

    /**
     * Sets the run, and pass-fail bit fields to the values contained in the
     * provided string. The string should contain 1's, 0's, and -'s.
     * <blockquote>
     * 1 => pass <br />
     * 0 => fail <br />
     * - => didn't run <br />
     * </blockquote>
     *
     * @param sFlags string formatted as indicated above, representing the
     *               pass-fail-run quality checking algorithm bit fields.
     */
    public void setFlags(String sFlags) {
        logger.debug("setFlags(" + sFlags + ")");
        m_nRun = m_nFlag = 0;

        if (sFlags != null) {
            int nRun = 0;
            int nFlag = 0;

            for (int nIndex = 0; nIndex < sFlags.length(); nIndex++) {
                // shift the bits over one position
                nRun <<= 1;
                nFlag <<= 1;

                // if there is a flag state then the run bit is always set to 1
                if (sFlags.charAt(nIndex) != '-')
                    nRun += 1;

                if (sFlags.charAt(nIndex) == '1')
                    nFlag += 1;
            }

            m_nRun = nRun;
            m_nFlag = nFlag;
        }
    }

    /**
     * <b> Mutator </b>
     *
     * @param sSecret new value to set the secret attribute to.
     */
    public void setSecret(String sSecret) {
        m_sSecret = sSecret;
    }

    /******
     public void setCode(String sCode)
     {
     m_nCode = Integer.parseInt(sCode);
     }

     public void setKey(String sKey)
     {
     m_nKey = 0;
     String[] sKeys = sKey.split(",");

     int nMultiplier = 100000;
     int nIndex = sKeys.length;
     while (nIndex-- > 0)
     {
     boolean bContinue = true;
     int nKeyIndex = g_sKeys.length;
     while (nKeyIndex-- > 0 && bContinue)
     {
     if (sKeys[nIndex].compareTo(g_sKeys[nKeyIndex]) == 0)
     {
     m_nKey += nMultiplier * (nKeyIndex % 10);
     nMultiplier /= 10;
     bContinue = false;
     }
     }
     }
     }
     *****/

    /**
     * <b> Mutator </b>
     *
     * @param sSecretAttempt new value to set the secret attempt attribute to.
     */
    public void setSecretAttempt(String sSecretAttempt) {
        m_sSecretAttempt = sSecretAttempt;
    }

    /**
     * Determines whether the provided id is one contained in the list.
     *
     * @param nId   id in question.
     * @param oList list containing the id's of interest.
     * @return true if either the id is contained in the list, or if the list
     * is empty (if the list is empty, the subscription is only monitoring one
     * id). false otherwise.
     */
    private boolean inList(int nId, ArrayList<Integer> oList) {
        if (oList.size() == 0)
            return true;

        Integer oId = new Integer(nId);
        return (Collections.binarySearch(oList, oId) >= 0);
    }

    /**
     * Determines whether the observation-value is within the filter range or
     * not.
     *
     * @param dValue The value in question.
     * @return true if either no observation type is set, or the provided value
     * falls within the observation-value range. false otherwise.
     */
    public boolean inRange(double dValue) {
        if (m_nObsType == 0)
            return true;

        return (dValue >= m_dMin && dValue <= m_dMax);
    }

    /**
     * Determines whether the provided run and pass-fail bit fields match those
     * provided contained by the {@code Subscription} filter or not.
     *
     * @param nRun  quality checking algorithm run bit-field.
     * @param nFlag pass-fail qch algorithm bit-field.
     * @return true if the quality checking algorithms represented by the
     * supplied bit-fields match those contained by this {@code Subscription}.
     * false if either the qch algorithm was not run, or the pass-fail flags
     * don't match.
     */
    public boolean wasChecked(int sourceId, char[] _nFlag) {
        if (m_nObsType == 0)
            return true;

        if (m_nRun == 0)
            return true;

        char[] nFlag = QualityCheckFlagUtil.getQcCharFlags(sourceId, m_nRun, m_nFlag);

        for (int i = 0; i < nFlag.length; i++)
            if (nFlag[i] != '/' && _nFlag != null && nFlag[i] != _nFlag[i])
                return false;

        return true;
    }

    /**
     * Determines whether or not the supplied coordinates fall within the
     * region of interest.
     *
     * @param dLat latitude coordinate.
     * @param dLng longitude coordinate.
     * @return true if the supplied coordinates fall within the bounds of the
     * region of interest. false otherwise.
     */
    public boolean inRegion(double dLat, double dLng) {
        return (dLat >= m_dLat1 && dLat <= m_dLat2 && dLng >= m_dLng1 && dLng <= m_dLng2);
    }

    /**
     * Determines whether the supplied observation type is the type of interest
     * for this {@code Subscription}.
     *
     * @param nObsType observation type in question.
     * @return true if either an observation type hasn't been set for this
     * {@code Subscription} or if the provided obs-type matches the obs-type of
     * interest.
     */
    public boolean isObs(int nObsType) {
        if (m_nObsType == 0)
            return true;

        return (m_nObsType == nObsType);
    }

    /**
     * Extracts the data associated with the subscription id specified by the
     * provided subscription result set. This subscription id is used to execute
     * the queries associated with the provided prepared statements.
     *
     * @param oSubscription result set of a subscription query.
     * @param oGetRadius    prepared radius database query, ready to execute.
     * @param oGetContrib   prepared contributor db query, ready to execute.
     * @param oGetStation   prepared station database query, ready to execute.
     * @throws java.lang.Exception
     */
    public void deserialize(ResultSet oSubscription,
                            PreparedStatement oGetRadius, PreparedStatement oGetContrib,
                            PreparedStatement oGetStation) throws Exception {
        m_nId = oSubscription.getInt(1);

        // set the subscription parameters
        m_dLat1 = MathUtil.fromMicro(oSubscription.getInt(2));
        if (oSubscription.wasNull())
            m_dLat1 = -Double.MAX_VALUE;

        m_dLng1 = MathUtil.fromMicro(oSubscription.getInt(3));
        if (oSubscription.wasNull())
            m_dLng1 = -Double.MAX_VALUE;

        m_dLat2 = MathUtil.fromMicro(oSubscription.getInt(4));
        if (oSubscription.wasNull())
            m_dLat2 = Double.MAX_VALUE;

        m_dLng2 = MathUtil.fromMicro(oSubscription.getInt(5));
        if (oSubscription.wasNull())
            m_dLng2 = Double.MAX_VALUE;

        m_nObsType = oSubscription.getInt(6);

        m_dMin = oSubscription.getDouble(7);
        if (oSubscription.wasNull())
            m_dMin = Double.NEGATIVE_INFINITY;

        m_dMax = oSubscription.getDouble(8);
        if (oSubscription.wasNull())
            m_dMax = Double.POSITIVE_INFINITY;

        m_nRun = oSubscription.getInt(9);
        m_nFlag = oSubscription.getInt(10);

        m_sOutputFormat = oSubscription.getString(11);

        m_oRadius = null;
        oGetRadius.setInt(1, m_nId);
        ResultSet rs = oGetRadius.executeQuery();
        if (rs.next())
            m_oRadius = new PointRadius(rs.getInt(1),
                    rs.getInt(2),
                    rs.getInt(3));
        rs.close();

        m_oContribIds.clear();
        oGetContrib.setInt(1, m_nId);
        rs = oGetContrib.executeQuery();
        while (rs.next())
            m_oContribIds.add(new Integer(rs.getInt(1)));
        rs.close();

        m_oPlatformIds.clear();
        oGetStation.setInt(1, m_nId);
        rs = oGetStation.executeQuery();
        while (rs.next())
            m_oPlatformIds.add(new Integer(rs.getInt(1)));
        rs.close();
    }

    /**
     * <b> Accessor </b>
     *
     * @return contributor string attribute.
     */
    public String getContributors() {
        return (m_sContributors);
    }

    /**
     * Populates the contributor-id attribute array from the provided
     * comma-delimited string containing integer values corresponding to the
     * contributors of interest. Also keeps a record of the supplied string in
     * the contributor string attribute.
     *
     * @param sContribList comma-delimited list of contributor identification
     *                     numbers.
     */
    public void setContributors(String sContribList) {
        logger.debug("setContributors(" + sContribList + ")");
        m_oContribIds.clear();
        stringsToArray(sContribList, m_oContribIds);

        m_sContributors = StringUtils.join(m_oContribIds, ",");
    }

    /**
     * <b> Accessor </b>
     *
     * @return true if the wizard is running, false otherwise.
     */
    public boolean isWizardRunning() {
        return (m_bWizardRunning);
    }

    /**
     * <b> Mutator </b>
     *
     * @param bValue new value for the wizard running flag.
     */
    public void setWizardRunning(boolean bValue) {
        m_bWizardRunning = bValue;
    }

    /**
     * <b> Mutator </b>
     *
     * @param sCycle string containing the cycle value, to assign to the
     *               cycle attribute.
     */
    public void setCycle(String sCycle) {
        m_nCycle = Integer.parseInt(sCycle);
    }

    /**
     * Checks the password attempt against the subscription password.
     *
     * @return true if there is no subscription password, or if the attempt
     * matches the subscription password. false otherwise.
     */
    public boolean checkSecurity() {
        // Always allow access if the subscription password is blank.
        // Otherwise, check the password against what was supplied.
        return
                (
                        m_sSecret.length() == 0 ||
                                m_sSecret.compareTo(m_sSecretAttempt) == 0
                );
    }

    /**
     * Determines whether the provided observation passes the subscription
     * filter.
     *
     * @param oSubObs    observation in question.
     * @param matchCheck additional matchCheck
     * @return true if the observation matches the parameters set by the
     * subscription, false otherwise.
     */
    boolean matches(SubObs oSubObs, boolean matchCheck) {
        // exposed variables for debugging filtering algorithms
//		boolean bIsObs = isObs(oSubObs.m_nObsTypeId);

        // The following check has been incorporated in the query
        boolean bIsObs = true;

        boolean bInRange = inRange(oSubObs.m_dValue);
        boolean bWasChecked = wasChecked(oSubObs.sourceId, oSubObs.m_nFlags);

        // The following checks have been incorporated in the query

        boolean bInRegion = true;
        boolean bInContribList = true;
        boolean bInStationList = true;
        if (matchCheck) {
            bInRegion =
                    (
                            m_oContribIds.size() > 0 ||
                                    inRegion(oSubObs.m_dLat, oSubObs.m_dLon)
                    );
            bInContribList =
                    (
                            oSubObs.m_oContrib != null &&
                                    inList(oSubObs.m_oContrib.getId(), m_oContribIds)
                    );
            bInStationList =
                    (
                            oSubObs.m_iPlatform != null &&
                                    inList(oSubObs.m_iPlatform.getId(), m_oPlatformIds)
                    );
        }

        return
                (
                        bIsObs && bInRange && bWasChecked
                                && bInRegion && bInContribList && bInStationList
                );
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSubScope() {
        return subScope;
    }

    public void setSubScope(String subScope) {
        this.subScope = subScope;
    }

    public String getFormat() {
        return this.m_sOutputFormat;
    }

    /**
     * @param sFormat
     */
    public void setFormat(String sFormat) {
        m_sOutputFormat = sFormat.toUpperCase();
    }
	 
	 public static class NextId implements Runnable
	 {
		 public int m_nNextId = 0;
		 
		 NextId()
		 {
			 run();
			 Scheduler.getInstance().schedule(this, 0, 86400, true);
		 }
		 @Override
		 public final void run()
		 {
			boolean bFoundMax = false;
			boolean bFoundMin = false;
			java.util.Date oToday = new java.util.Date();
			SimpleDateFormat oFormat = new SimpleDateFormat("yyyyMMdd");
			oFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
			String sSearch = "'%" + oFormat.format(oToday) + "%'";
			if (m_nNextId != 0)
			{
				m_nNextId = Integer.parseInt(oFormat.format(oToday)) * 100;
			}
			Connection iConnection = null;
			try 
			{
            DataSource iDataSource = WDEMgr.getInstance().getDataSource("java:comp/env/jdbc/wxde");
            if (iDataSource == null)
                return;

            iConnection = iDataSource.getConnection();
            if (iConnection == null)
                return;

				int nMax = 0;
				int nMin = 0;
				Statement iStatementMax = iConnection.createStatement();
				Statement iStatementMin = iConnection.createStatement();
				ResultSet oRsMax = iStatementMax.executeQuery("SELECT MAX(id) FROM subs.subscription WHERE CAST(id AS TEXT) LIKE " + sSearch + " AND id>0");
				ResultSet oRsMin = iStatementMin.executeQuery("SELECT MIN(id) FROM subs.subscription WHERE CAST(id AS TEXT) LIKE " + sSearch + " AND id<0");
				if (oRsMax.next())
				{
					nMax = oRsMax.getInt(1);
					if (nMax != 0)
						bFoundMax = true;
				}
				oRsMax.close();
				if (oRsMin.next())
				{
					nMin = oRsMin.getInt(1);
					if (nMin != 0)
						bFoundMin = true;
				}
				oRsMin.close();
				if (bFoundMax && bFoundMin)
					m_nNextId = Math.max(nMax, -nMin) + 1;
				else if (bFoundMax)
					m_nNextId = nMax + 1;
				else if (bFoundMin)
					m_nNextId = -nMin + 1;
				else
					m_nNextId = Integer.parseInt(oFormat.format(oToday)) * 100;	
			}
			catch (Exception oException)
			{

			}
		 }
	 }
}