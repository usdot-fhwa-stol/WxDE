// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Subscription.java
 */
package clarus.qeds;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import clarus.emc.Stations;

/**
 * Provides filtering parameters which can be set and used to gather
 * only the observations that meet these defined parameters.
 *
 * @author bryan.krueger
 */
public class Subscription
{
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
	public ArrayList<Integer> m_oStationIds = new ArrayList<Integer>();

	/**
	 *
	 */
	private boolean m_bWizardRunning = false;

	/**
	 * Radius of the region of interest.
	 */
	public PointRadius m_oRadius;

	public String m_sContactEmail = "";
	public String m_sContactName = "";

	/**
	* Creates a new default instance of {@code Subscription}.
	*/
	public Subscription()
	{
	}


	/**
	 * Initializes the attributes to their default values. Integers to zero,
	 * strings to null, min and max values to negative and positive infinity,
	 * output format to CSV, and the lists cleared.
	 */
	public void clearAll()
	{
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
		m_oStationIds.clear();
	}


	public void setContactEmail(String sContactEmail)
	{
		if (sContactEmail != null && sContactEmail.length() > 0)
			m_sContactEmail = sContactEmail;
	}


	public void setContactName(String sContactName)
	{
		if (sContactName != null && sContactName.length() > 0)
			m_sContactName = sContactName;
	}


	/**
	 * Takes the provided comma-delimited string and parses the min and max
	 * time range, sets the corresponding time bound attributes
	 * ({@code m_lStartTime} and {@code m_lEndTime}).
	 * @param sTimeRange comma-delimited string containing two time bounds,
	 * the min and the max.
	 */
	public void setTimeRange(String sTimeRange)
	{
		// System.out.println("Debug Subscription: setTimeRange(" + sTimeRange + ")");
		if (sTimeRange != null && sTimeRange.length() > 0)
		{
			String[] sTimes = sTimeRange.split(",");
			if (sTimes.length == 2)
			{
				long lStartTime = Long.parseLong(sTimes[0]);
//				long lEndTime = Long.parseLong(sTimes[1]);
				long lEndTime = lStartTime + 3600000;

				// If the Start and End Times are less than 0,
				// then interpret them as the number of seconds relative
				// to the current time.
				// Otherwise, interpret them as absolute timestamps.
				long lTimeNow = System.currentTimeMillis();
				if (lStartTime <= 0)
				{
					m_lStartTime = lTimeNow + lStartTime;
				}
				else
				{
					m_lStartTime = lStartTime;
				}

				if (lEndTime <= 0)
				{
					m_lEndTime = lTimeNow + lEndTime;
				}
				else
				{
					m_lEndTime = lEndTime;
				}

				// Make sure the Start Time is before the End Time.
				if (m_lStartTime > m_lEndTime)
				{
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
	 * method, the list will be sorted in ascending order.
	 */
	private void stringsToArray(String sList, ArrayList<Integer> oList)
	{
		if (sList != null && sList.length() > 0)
		{
			String[] sIds = sList.split(",");
			int nIndex = sIds.length;
			while (nIndex-- > 0)
				oList.add(new Integer(Integer.parseInt(sIds[nIndex])));

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
	public void setSubId(String sSubId)
	{
		// System.out.println("Debug Subscription: setSubId(" + sSubId + ")");
		m_nId = Integer.parseInt(sSubId);
		try
		{
			DataSource iDataSource =
			ClarusMgr.getInstance().getDataSource("java:comp/env/jdbc/clarus_subs");

			if (iDataSource == null)
				return;

			Connection iConnection = iDataSource.getConnection();
			if (iConnection == null)
				return;

			PreparedStatement oGetSubscription = iConnection.prepareStatement("SELECT password FROM subscription WHERE id=?");
			oGetSubscription.setInt(1, m_nId);
			ResultSet oResultSet = oGetSubscription.executeQuery();
			if (oResultSet.next())
				m_sSecret = oResultSet.getString(1);

			oResultSet.close();
			oGetSubscription.close();
			iConnection.close();
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}

	/**
	 * Populates the contributor-id attribute array from the provided
	 * comma-delimited string containing integer values corresponding to the
	 * contributors of interest. Also keeps a record of the supplied string in
	 * the contributor string attribute.
	 * 
	 * @param sContribList comma-delimited list of contributor identification
	 * numbers.
	 */
	public void setContributors(String sContribList)
	{
		// System.out.println("Debug Subscription: setContributors(" + sContribList + ")");
		m_oContribIds.clear();
		stringsToArray(sContribList, m_oContribIds);
		m_sContributors = sContribList;
	}

	/**
	 * Populates the station-id attribute array from the provided
	 * comma-delimited string containing integer values corresponding to the
	 * stations of interest.
	 * 
	 * @param sStationList comma-delimited list of station identification
	 * numbers.
	 */
	public void setStations(String sStationList)
	{
		// System.out.println("Debug Subscription: setStations(" + sStationList + ")");
		m_oStationIds.clear();
		stringsToArray(sStationList, m_oStationIds);
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
	 * @param sRegion comma-delimited string containing the coordinates of the
	 * region of interest, either of the form: min-lat, min-long, max-lat,
	 * max-long or lat, long, radius.
	 */
	public void setRegion(String sRegion)
	{
		// System.out.println("Debug Subscription: setRegion(" + sRegion + ")");
		m_dLat1 = m_dLng1 = -Double.MAX_VALUE;
		m_dLat2 = m_dLng2 = Double.MAX_VALUE;
		m_oRadius = null;

		String[] sCoordinates = sRegion.split(",");

		if (sCoordinates.length == 4)
		{
			m_dLat1 = Double.parseDouble(sCoordinates[0]);
			m_dLng1 = Double.parseDouble(sCoordinates[1]);
			m_dLat2 = Double.parseDouble(sCoordinates[2]);
			m_dLng2 = Double.parseDouble(sCoordinates[3]);

			// swap coordinates if the minimum and maximum overlap
			double dTemp = 0.0;
			if (m_dLat1 > m_dLat2)
			{
				dTemp = m_dLat1;
				m_dLat1 = m_dLat2;
				m_dLat2 = dTemp;
			}
			if (m_dLng1 > m_dLng2)
			{
				dTemp = m_dLng1;
				m_dLng1 = m_dLng2;
				m_dLng2 = dTemp;
			}
		}

		if (sCoordinates.length == 3)
		{
			m_oRadius = new PointRadius(sCoordinates);

			// the radius causes both coordinate pairs to be recalculated
			// using mean Earth circumference of 40,076 Km
			double dAdjustment = m_oRadius.m_dRadius * 360.0 / 40076.0;
			m_dLat1 = m_oRadius.m_dLat - dAdjustment;
			m_dLng1 = m_oRadius.m_dLng - dAdjustment;

			m_dLat2 = m_oRadius.m_dLat + dAdjustment;
			m_dLng2 = m_oRadius.m_dLng + dAdjustment;

			// must check for coordinate boundary conditions
			if (m_dLat1 < -90.0)
				m_dLat1 += 180.0;

			if (m_dLat1 > 90.0)
				m_dLat1 -= 180.0;

			if (m_dLat2 < -90.0)
				m_dLat2 += 180.0;

			if (m_dLat2 > 90.0)
				m_dLat2 -= 180.0;

			if (m_dLng1 <= -180.0)
				m_dLng1 += 360.0;

			if (m_dLng1 > 180.0)
				m_dLng1 -= 360.0;

			if (m_dLng2 <= -180.0)
				m_dLng2 += 360.0;

			if (m_dLng2 > 180.0)
				m_dLng2 -= 360.0;
		}
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
	public void setObs(String sObs)
	{
		// System.out.println("Debug Subscription: setObs(" + sObs + ")");
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
	 * @param sFlags string formatted as indicated above, representing the
	 * pass-fail-run quality checking algorithm bit fields.
	 */
	public void setFlags(String sFlags)
	{
		// System.out.println("Debug Subscription: setFlags(" + sFlags + ")");
		m_nRun = m_nFlag = 0;

		if (sFlags != null)
		{
			int nRun = 0;
			int nFlag = 0;

			for (int nIndex = 0; nIndex < sFlags.length(); nIndex++)
			{
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
	 * @param sSecret new value to set the secret attribute to.
	 */
	public void setSecret(String sSecret)
	{
		m_sSecret = sSecret;
	}

	/**
	 * <b> Mutator </b>
	 * @param sSecretAttempt new value to set the secret attempt attribute to.
	 */
	public void setSecretAttempt(String sSecretAttempt)
	{
		m_sSecretAttempt = sSecretAttempt;
	}


	/**
	 * Determines whether the provided id is one contained in the list.
	 * @param nId id in question.
	 * @param oList list containing the id's of interest.
	 * @return true if either the id is contained in the list, or if the list
	 * is empty (if the list is empty, the subscription is only monitoring one
	 * id). false otherwise.
	 */
	private boolean inList(int nId, ArrayList<Integer> oList)
	{
		if (oList.size() == 0)
			return true;

		Integer oId = new Integer(nId);
		return (Collections.binarySearch(oList, oId) >= 0);
	}


	/**
	 * Determines whether the observation-value is within the filter range or
	 * not.
	 * @param dValue The value in question.
	 * @return true if either no observation type is set, or the provided value
	 * falls within the observation-value range. false otherwise.
	 */
	public boolean inRange(double dValue)
	{
		if (m_nObsType == 0)
			return true;

		return (dValue >= m_dMin && dValue <= m_dMax);
	}

	/**
	 * Determines whether the provided run and pass-fail bit fields match those
	 * provided contained by the {@code Subscription} filter or not.
	 * @param nRun quality checking algorithm run bit-field.
	 * @param nFlag pass-fail qch algorithm bit-field.
	 * @return true if the quality checking algorithms represented by the
	 * supplied bit-fields match those contained by this {@code Subscription}.
	 * false if either the qch algorithm was not run, or the pass-fail flags
	 * don't match.
	 */
	public boolean wasChecked(int nRun, int nFlag)
	{
		if (m_nObsType == 0)
			return true;

		return ((nRun & m_nRun) == m_nRun && (nFlag & m_nRun) == m_nFlag);
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
	public boolean inRegion(double dLat, double dLng)
	{
		return (dLat >= m_dLat1 && dLat <= m_dLat2 && dLng >= m_dLng1 && dLng <= m_dLng2);
	}

	/**
	 * Determines whether the supplied observation type is the type of interest
	 * for this {@code Subscription}.
	 * @param nObsType observation type in question.
	 * @return true if either an observation type hasn't been set for this
	 * {@code Subscription} or if the provided obs-type matches the obs-type of
	 * interest.
	 */
	public boolean isObs(int nObsType)
	{
		if (m_nObsType == 0)
			return true;

		return (m_nObsType == nObsType);
	}

	/**
	 * Extracts the data associated with the subscription id specified by the
	 * provided subscription result set. This subscription id is used to execute
	 * the queries associated with the provided prepared statements.
	 * @param oSubscription result set of a subscription query.
	 * @param oGetRadius prepared radius database query, ready to execute.
	 * @param oGetContrib prepared contributor db query, ready to execute.
	 * @param oGetStation prepared station database query, ready to execute.
	 * @throws java.lang.Exception
	 */
	public void deserialize(ResultSet oSubscription, 
			PreparedStatement oGetRadius, PreparedStatement oGetContrib,
			PreparedStatement oGetStation) throws Exception
	{
		m_nId = oSubscription.getInt(1);

		// set the subscription parameters
		m_dLat1 = Stations.fromMicro(oSubscription.getInt(2));
		if (oSubscription.wasNull())
			m_dLat1 = -Double.MAX_VALUE;

		m_dLng1 = Stations.fromMicro(oSubscription.getInt(3));
		if (oSubscription.wasNull())
			m_dLng1 = -Double.MAX_VALUE;

		m_dLat2 = Stations.fromMicro(oSubscription.getInt(4));
		if (oSubscription.wasNull())
			m_dLat2 = Double.MAX_VALUE;

		m_dLng2 = Stations.fromMicro(oSubscription.getInt(5));
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
		ResultSet oResultSet = oGetRadius.executeQuery();
		if (oResultSet.next())
			m_oRadius = new PointRadius(oResultSet.getInt(1), 
										oResultSet.getInt(2),
										oResultSet.getInt(3));
		oResultSet.close();

		m_oContribIds.clear();
		oGetContrib.setInt(1, m_nId);
		oResultSet = oGetContrib.executeQuery();
		while (oResultSet.next())
			m_oContribIds.add(new Integer(oResultSet.getInt(1)));
		oResultSet.close();

		m_oStationIds.clear();
		oGetStation.setInt(1, m_nId);
		oResultSet = oGetStation.executeQuery();
		while (oResultSet.next())
			m_oStationIds.add(new Integer(oResultSet.getInt(1)));
		oResultSet.close();
	}


	/**
	 * <b> Accessor </b>
	 * @return contributor string attribute.
	 */
	public String getContributors()
	{
		return(m_sContributors);
	}

	/**
	 * <b> Accessor </b>
	 * @return true if the wizard is running, false otherwise.
	 */
	public boolean isWizardRunning()
	{
		return(m_bWizardRunning);
	}

	/**
	 * <b> Mutator </b>
	 * @param bValue new value for the wizard running flag.
	 */
	public void setWizardRunning(boolean bValue)
	{
		m_bWizardRunning = bValue;
	}

	/**
	 *
	 * @param sFormat
	 */
	public void setFormat(String sFormat)
	{
		m_sOutputFormat = sFormat.toUpperCase();
	}

	/**
	 * <b> Mutator </b>
	 * @param sCycle string containing the cycle value, to assign to the
	 * cycle attribute.
	 */
	public void setCycle(String sCycle)
	{
		m_nCycle = Integer.parseInt(sCycle);
	}

	/**
	 * Checks the password attempt against the subscription password.
	 * @return true if there is no subscription password, or if the attempt
	 * matches the subscription password. false otherwise.
	 */
	public boolean checkSecurity()
	{
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
	 * @param oSubObs observation in question.
	 * @return true if the observation matches the parameters set by the
	 * subscription, false otherwise.
	 */
	boolean matches(SubObs oSubObs)
	{
		// exposed variables for debugging filtering algorithms
		boolean bIsObs = isObs(oSubObs.m_nObsTypeId);
		boolean bInRange = inRange(oSubObs.m_dValue);
		boolean bWasChecked =
			wasChecked(oSubObs.m_nRunFlags, oSubObs.m_nPassedFlags);
		boolean bInRegion =
		(
			m_oContribIds.size() > 0 ||
			inRegion(oSubObs.m_dLat, oSubObs.m_dLon)
		);
		boolean bInContribList =
		(
			oSubObs.m_oContrib != null &&
			inList(oSubObs.m_oContrib.getId(), m_oContribIds)
		);
		boolean bInStationList =
		(
			oSubObs.m_iStation != null &&
			inList(oSubObs.m_iStation.getId(), m_oStationIds)
		);

		return
		(
			bIsObs && bInRange && bWasChecked &&
			bInRegion && bInContribList && bInStationList
		);
	}
}
