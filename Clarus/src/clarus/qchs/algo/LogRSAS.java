// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qchs.algo;

import java.io.FileWriter;
import java.sql.ResultSet;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.SimpleTimeZone;
import clarus.emc.ISensor;
import clarus.emc.Stations;
import clarus.qedc.IObs;
import clarus.qchs.RSAS;
import util.threads.ThreadPool;

/**
 * Class provides a Quality checking method to check whether the value is within
 * a tollerable amount of one standard deviation away from other nearby
 * observation values.
 */
public class LogRSAS extends LikeInstrument implements Runnable
{
	/**
	 * Minimum Tolerance Bound
	 */
	protected double m_dMinToleranceBound;
	/**
	 * Error Limit
	 */
	protected double m_dErrorLimit;
	/**
	 * Service for requesting RSAS background fields
	 */
	protected RSAS m_oRSAS = RSAS.getInstance();

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
	 * Creates new instances of {@code LogRSAS}. Initialization done through
	 * the {@link LogRSAS#init} method.
	 * </p>
	 */
	public LogRSAS()
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
		boolean bPass, double[] dRSAS)
	{
		// only set the obs type id once
		if (m_nObsTypeId == 0)
		{
			m_nObsTypeId = nObsTypeId;
			// generate the file name pattern
			String sPattern = "'\\\\clarus5\\subscriptions\\1000000060\\'" + 
				"yyyyMMdd_HH'00-RSAS" + Integer.toString(nObsTypeId) + ".csv'";
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
		for (int nIndex = 0; nIndex < dRSAS.length; nIndex++)
		{
			m_sBuffer.append(",");
			m_sBuffer.append(m_oDecimalFormat.format(dRSAS[nIndex]));
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
	 * Gets an array of all the readings within a given radius from RSAS and
	 * uses it to test the sensor reading. Records the
	 * test as a pass only if the specified observation value is within the
	 * tolerance bound of a standard deviation of the values around it
	 * geographically.
	 *
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
		double[] dValues = m_oRSAS.getReadings(nObsTypeId,
				iObs.getLat(),iObs.getLon());

		// If we have enough neighbors continue, otherwise return early
		if (dValues == null || dValues.length < m_nObsCountMin)
			return;

		Arrays.sort(dValues);

		// Calc median:
		double dPredictValue = calcQuantile(0.5, dValues);

		// Calc IQR = interquartile range = (75th percentile) - (25th percentile)
		double dIQR = calcQuantile(0.75, dValues)
		  - calcQuantile(0.25, dValues);

		// In the normal distribution, the 0.25 and 0.75 quantiles
		// are at -/+ 0.67448 * sigma respectively.
		// So the iqr = 2 * 0.67448 * sigma = 1.348960 * sigma,
		// and stddev = 0.74131 * iqr
		double dStdDev = 0.74131 * dIQR;

		double dAbsError = Math.abs(iObs.getValue() - dPredictValue);

		if (dAbsError <= Math.max(m_dMinToleranceBound,	m_dErrorLimit * dStdDev))
		{
			oResult.setPass(true);
			oResult.setConfidence(1.0);
		}
		oResult.setRun();

		// write log entry for algorithm evaluation
		writeLog(nObsTypeId, iObs, oResult.getPass(), dValues);
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
	private double calcQuantile(double dQuantile, double[] dNeighborValues)
	{
		// finds the whole number index with which to reference the array
		int nIndex = (int)(dQuantile * dNeighborValues.length);

		// finds the fraction that was stripped off when nIndex was calculated
		double dFrac = dQuantile * dNeighborValues.length - nIndex;

		// return the value at nIndex plus dFrac times the difference between
		// the value and the next largest value
		return dNeighborValues[nIndex] + dFrac *
				(dNeighborValues[nIndex+1] - dNeighborValues[nIndex]);
	}
}
