// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qchs.algo;

import java.io.FileWriter;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.SimpleTimeZone;
import clarus.emc.ISensor;
import clarus.emc.Stations;
import clarus.qedc.IObs;
import util.Introsort;
import util.threads.ThreadPool;

/**
 * Class provides a Quality checking method to check whether the value is within
 * a tollerable amount of one standard deviation away from other nearby
 * observation values.
 */
public class LogIQR extends LikeInstrument implements Runnable, Comparator<IObs>
{
	/**
	 * Minimum Tolerance Bound
	 */
	protected double m_dMinToleranceBound;
	/**
	 * Error Limit
	 */
	protected double m_dErrorLimit;

	private int m_nObsTypeId;
	private long m_lCurrentHour;
	private StringBuilder m_sBuffer = new StringBuilder(2000000);
	private StringBuilder m_sWriteBuffer = new StringBuilder(2000000);
	private Date m_oFileDate = new Date();
	private Date m_oObsDate = new Date();
	private DecimalFormat m_oDecimalFormat = new DecimalFormat("#0.00");
	private SimpleDateFormat m_oFilenameFormat;
	private SimpleDateFormat m_oObsDateFormat;


	/**
	 * <b> Default Constructor </b>
	 *
	 * <p>
	 * Creates new instances of {@code LogIQR}. Initialization done through
	 * {@link LogIQR#init} method.
	 * </p>
	 */
	public LogIQR()
	{
		m_lCurrentHour = System.currentTimeMillis() / 3600000;
		m_oObsDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		m_oObsDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
	}


	/**
	 * Calls the init method of its super class and sets the
	 * {@code MinToleranceBound} and {@code ErrorLimit} based on values
	 * read in from the super class init method's query.
	 *
	 * @param iResultSet
	 *
	 * @throws java.lang.Exception
	 */
	@Override
	protected void init(ResultSet iResultSet) throws Exception
	{
		super.init(iResultSet);

		m_dMinToleranceBound = m_dSdMin;
		m_dErrorLimit = m_dSdMax;
	}


	private synchronized void writeLog(int nObsTypeId, IObs iObs,
		boolean bPass, ArrayList<IObs> oObsSet)
	{
		// only set the obs type id once
		if (m_nObsTypeId == 0)
		{
			m_nObsTypeId = nObsTypeId;
			// generate the file name pattern
			String sPattern = "'\\\\clarus5\\subscriptions\\1000000060\\'" +
				"yyyyMMdd_HH'00-IQR" + Integer.toString(nObsTypeId) + ".csv'";
			m_oFilenameFormat = new SimpleDateFormat(sPattern);
			m_oFilenameFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
		}

		// determine if it is time to write out the log information
		long lNow = System.currentTimeMillis() / 3600000;
		if (lNow > m_lCurrentHour)
		{
			// truncate time to the most current hour
			m_lCurrentHour = lNow;
			m_oFileDate.setTime(m_lCurrentHour * 3600000);

			// swap string builders and launch thread to write data
			StringBuilder sTempBuffer = m_sWriteBuffer;
			sTempBuffer.setLength(0);
			m_sWriteBuffer = m_sBuffer;
			m_sBuffer = sTempBuffer;
			ThreadPool.getInstance().execute(this);
		}

		m_oObsDate.setTime(iObs.getTimestamp());
		m_sBuffer.append(m_oObsDateFormat.format(m_oObsDate));
		m_sBuffer.append(",");
		m_sBuffer.append(nObsTypeId);
		m_sBuffer.append(",");
		m_sBuffer.append(iObs.getSensorId());
		m_sBuffer.append(",");
		m_sBuffer.append(Stations.fromMicro(iObs.getLat()));
		m_sBuffer.append(",");
		m_sBuffer.append(Stations.fromMicro(iObs.getLon()));
		m_sBuffer.append(",");
		m_sBuffer.append(iObs.getElev());
		m_sBuffer.append(",");
		m_sBuffer.append(m_oDecimalFormat.format(iObs.getValue()));
		m_sBuffer.append(",");
		if (bPass)
			m_sBuffer.append("P");
		else
			m_sBuffer.append("N");
		for (int nIndex = 0; nIndex < oObsSet.size(); nIndex++)
		{
			m_sBuffer.append(",");
			m_sBuffer.append(m_oDecimalFormat.format(oObsSet.get(nIndex).getValue()));
		}
		m_sBuffer.append("\n");
	}


	public void run()
	{
		// the file name should be stable long enough to get the data written
		// and the string builder should remain untouched for the same period
		try
		{
			// generate the filename with UNC directories
			String sFilename = m_oFilenameFormat.format(m_oFileDate);
			FileWriter oWriter = new FileWriter(sFilename);

			// write the buffered qch data to the file
			for (int nIndex = 0; nIndex < m_sWriteBuffer.length(); nIndex++)
				oWriter.write(m_sWriteBuffer.charAt(nIndex));

			// finish up the file
			oWriter.flush();
			oWriter.close();
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}


	/**
	 * Gets an ArrayList of all the stations within a given radius and removes
	 * the stations that aren't within a given elevation range. Records the
	 * test as a pass only if the specified observation value is within the
	 * tolerance bound of a standard deviation of the values around it
	 * geographically.
	 *
	 * @see LogIQR#calcQuantile(double, java.util.ArrayList)
	 *
	 * @param nObsTypeId Observation Type Id
	 * @param iSensor Observation Sensor
	 * @param iObs Observation
	 * @param oResult Result of the Quality Check
	 */
	@Override
	public void check (int nObsTypeId, ISensor iSensor, IObs iObs, QChResult oResult)
	{
		// retrieve the background field
		int nLat = iObs.getLat();
		int nLon = iObs.getLon();
		long lTimestamp = iObs.getTimestamp();

		ArrayList<IObs> oObsSet = new ArrayList<IObs>();

		m_oObsMgr.getBackground(nObsTypeId,	nLat - m_nGeoRadiusMax,
			nLon - m_nGeoRadiusMax, nLat + m_nGeoRadiusMax,
			nLon + m_nGeoRadiusMax, lTimestamp + m_lTimerangeMin,
			lTimestamp + m_lTimerangeMax, oObsSet);

		// filter obs outside of elevation range
		int nMinElev = iObs.getElev() - 350;
		int nMaxElev = iObs.getElev() + 350;
		int nIndex = oObsSet.size();
		while (nIndex-- > 0)
		{
			IObs iTempObs = oObsSet.get(nIndex);
			int nElev = iTempObs.getElev();
			// remove the target obs from the background field too
			if (nElev < nMinElev || nElev > nMaxElev || iTempObs == iObs)
				oObsSet.remove(nIndex);
		}

		// If we have enough neighbors continue, otherwise return early
		if (oObsSet.size() < m_nObsCountMin)
			return;

		double dTestValue = iObs.getValue();

		Introsort.usort(oObsSet, this);

		// Calc median:
		double dPredictValue = calcQuantile(0.5, oObsSet);

		// Calc LogIQR = interquartile range = (75th percentile) - (25th percentile)
		double dLogIQR = calcQuantile(0.75, oObsSet)
		  - calcQuantile(0.25, oObsSet);

		// In the normal distribution, the 0.25 and 0.75 quantiles
		// are at -/+ 0.67448 * sigma respectively.
		// So the LogIQR = 2 * 0.67448 * sigma = 1.348960 * sigma,
		// and stddev = 0.74131 * LogIQR
		double dStdDev = 0.74131 * dLogIQR;

		double dAbsError = Math.abs(dTestValue - dPredictValue);

		if (dAbsError <= Math.max(m_dMinToleranceBound, m_dErrorLimit * dStdDev))
		{
			oResult.setPass(true);
			oResult.setConfidence(1.0);
		}
		oResult.setRun();

		// write log entry for algorithm evaluation
		writeLog(nObsTypeId, iObs, oResult.getPass(), oObsSet);
	}


	/**
	 * Calculate the specified quantile. A quantile is a percentile but with
	 * a 0 to 1 domain instead of 0 to 100.  For example, the 75th percentile
	 * is the 0.75 quantile.
	 *
	 * @param dQuantile Quantile to be calculated
	 * @param nNumNeighborVals Number of Neighbor Values
	 * @param dNeighborValues Array of Neighboring Values
	 *
	 * @return the value of the specified quantile
	 */
	private double calcQuantile(double dQuantile,
		ArrayList<IObs> oNeighborValues)
	{
		// finds the whole number index with which to reference the array
		int nIndex = (int)(dQuantile * oNeighborValues.size());

		// finds the fraction that was stripped off when nIndex was calculated
		double dFrac = dQuantile * oNeighborValues.size() - nIndex;

		// return the value at nIndex plus dFrac times the difference between
		// the value and the next largest value
		return (oNeighborValues.get(nIndex).getValue() + dFrac *
				(oNeighborValues.get(nIndex+1).getValue() -
				oNeighborValues.get(nIndex).getValue()));
	}


	/**
	 * Compares the values of two Observations.
	 *
	 * @param iObs1 first observation to be compared.
	 * @param iObs2 second observation to be compared.
	 *
	 * @return 0 if the observations have the same value.
	 * <br /> -1 if the first observation is less than the second.
	 * <br /> 1 if the first observation is greater than the second.
	 */
	public int compare(IObs iObs1, IObs iObs2)
	{
		if (iObs1.getValue() < iObs2.getValue())
			return -1;

		if (iObs1.getValue() > iObs2.getValue())
			return 1;

		return 0;
	}
}
