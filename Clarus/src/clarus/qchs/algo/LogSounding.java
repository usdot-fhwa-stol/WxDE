// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file LogSounding.java
 */
package clarus.qchs.algo;

import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.SimpleTimeZone;
import clarus.emc.ISensor;
import clarus.emc.Stations;
import clarus.qchs.ModObsSet;
import clarus.qchs.RAWINSONDE;
import clarus.qedc.IObs;
import util.threads.ILockFactory;
import util.threads.StripeLock;
import util.threads.ThreadPool;

/**
 * Performs spatial quality checking via {@code Barnes}. The difference being,
 * this quality check calculates the weighted mean of NWS-balloon adjusted
 * observations, and compares the provided observation to this value. If the
 * value being tested differs from the weighted mean by more than the configured
 * standard deviation the check fails
 * <p>
 * Extends {@code Barnes} to perform the quality check spatially, exept that
 * this algorithm checks observations against NWS-balloon observations.
 * </p>
 */
public class LogSounding extends Barnes implements Runnable
{
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
	 * Lock container on HeightTemp observation sets.
	 */
	private StripeLock<HeightTemp> m_oHeightTempLock =
		new StripeLock<HeightTemp>(new HeightTempFactory(), DEFAULT_LOCKS);
	/**
	 * Pointer to the RAWINSONDE singleton instance.
	 */
	private RAWINSONDE m_oRawinsonde = RAWINSONDE.getInstance();


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * New instances of {@code Sounding} are created with the default-inherited
	 * lock pointing to null.
	 * </p>
	 */
	public LogSounding()
	{		
		// free the default locks
		m_oLock = null;
		m_lCurrentHour = System.currentTimeMillis() / 3600000;
		m_oObsDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
		m_oObsDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
	}


	private synchronized void writeLog(int nObsTypeId, ISensor iSensor, 
		IObs iObs, HeightTemp oHeightTemp)
	{
		// only set the obs type id once
		if (m_nObsTypeId == 0)
		{
			m_nObsTypeId = nObsTypeId;
			// generate the file name pattern
			String sPattern = "'\\\\clarus5\\subscriptions\\1000000060\\'" +
				"yyyyMMdd_HH'00-RAWINSONDE" + Integer.toString(nObsTypeId) + ".csv'";
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

//		ClarusStationID, ClarusSensorID, Timestamp,Latitude,Longitude,Elevation,
//		StationPressure(Clarus),Elevation_at_700mb(sounding),Temperature_at_700mb(sounding),
//		SeaLevelPressure(output)
		m_sBuffer.append(iSensor.getStationId());
		m_sBuffer.append(",");
		m_sBuffer.append(iSensor.getSensorId());
		m_sBuffer.append(",");
		m_oObsDate.setTime(iObs.getTimestamp());
		m_sBuffer.append(m_oObsDateFormat.format(m_oObsDate));
		m_sBuffer.append(",");
		m_sBuffer.append(Stations.fromMicro(iObs.getLat()));
		m_sBuffer.append(",");
		m_sBuffer.append(Stations.fromMicro(iObs.getLon()));
		m_sBuffer.append(",");
		m_sBuffer.append(iObs.getElev());
		m_sBuffer.append(",");
		m_sBuffer.append(m_oDecimalFormat.format(iObs.getValue()));
		m_sBuffer.append(",");
		m_sBuffer.append(m_oDecimalFormat.format(oHeightTemp.m_dHeight));
		m_sBuffer.append(",");
		m_sBuffer.append(m_oDecimalFormat.format(oHeightTemp.m_dTemperature));
		m_sBuffer.append(",");
		m_sBuffer.append(m_oDecimalFormat.format(oHeightTemp.modifyValue(iObs)));
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
	 * Obtains a {@code HeightTemp} lock for use with the calling thread.
	 * @return the protected modified observation set.
	 */
	@Override
	protected ModObsSet readLock()
	{
		return m_oHeightTempLock.readLock();
	}


	/**
	 * Releases the {@code HeightTemp} lock assigned to the current thread.
	 */
	@Override
	protected void readUnlock()
	{
		m_oHeightTempLock.readUnlock();
	}


	/**
	 * If there's weather balloon data, the observation is checked against it
	 * using the {@code Barnes} spatial quality check.
	 *
	 * @param nObsTypeId type of observation in question.
	 * @param iSensor recording sensor.
	 * @param iObs observation being tested.
	 * @param oResult results of the quality check.
	 */
	@Override
	public void check(int nObsTypeId, ISensor iSensor, 
		IObs iObs, QChResult oResult)
	{
		// the lock is used directly to get the correct object type
		HeightTemp oHeightTemp = m_oHeightTempLock.readLock();

		if (m_oRawinsonde.getHeightTemp(oHeightTemp, iObs))
		{
			super.check(nObsTypeId, iSensor, iObs, oResult);
			writeLog(nObsTypeId, iSensor, iObs, oHeightTemp);
		}

		m_oHeightTempLock.readUnlock();
	}


	/**
	 * Allows {@code HeightTemp} objects to be added to a stripe lock.
	 * <p>
	 * Implements {@code ILockFactory<HeightTemp>} interface to allow
	 * {@code HeightTemp} objects to be modified in a mutually exclusive
	 * fashion through the use of {@link StripeLock} containers.
	 * </p>
	 */
	private class HeightTempFactory implements ILockFactory<HeightTemp>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code HeightTempFactory}
		 * </p>
         */
		public HeightTempFactory()
		{
		}


		/**
		 * Required for the implementation of the interface class
		 * {@code ILockFactory}.
		 * <p>
		 * This is used to add a container of lockable {@link HeightTemp}
		 * objects to the {@link StripeLock} Mutex.
		 * </p>
		 *
		 * @return A new instance of {@link HeightTemp}
		 *
		 * @see ILockFactory
		 * @see StripeLock
		 */
		public HeightTemp getLock()
		{
			return new HeightTemp();
		}
	}
}
