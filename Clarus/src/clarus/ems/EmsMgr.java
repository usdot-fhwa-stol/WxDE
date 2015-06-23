// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file EmsMgr.java
 */
package clarus.ems;

import java.io.FileWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import util.Config;
import util.ConfigSvc;
import util.Log;
import util.Scheduler;

/**
 * Keeps a log of metadata at the configured location for each database table
 * as specified by the configuration file.
 * <p>
 * This is a singleton class - there is one global instance accessed with the
 * {@link EmsMgr#getInstance} method.
 * </p>
 * <p>
 * {@code EmsMgr} implements {@see java.lang.Runnable} so instances of
 * {@code EmsMgr} can be the target of Thread instances.
 * </p>
 * @param <T> template type must be specified for new instances.
 */
public class EmsMgr<T> implements Runnable
{
	/**
	 * Configured default offset from midnight.
	 */
	protected static int DEFAULT_OFFSET = 0;
	/**
	 * Configured refresh rate.
	 */
	protected static int DEFAULT_REFRESH = 1200;
	/**
	 * Pointer to the meta-data log.
	 */
	private static EmsMgr g_oInstance = new EmsMgr();

    /**
     * <b> Accessor </b>
     * @return Singleton instance of {@code EmsMgr}.
     */
	public static EmsMgr getInstance()
	{
		return g_oInstance;
	}

    /**
     * Tracks the last time {@link EmsMgr#run() } was called to help with
     * refresh scheduling.
     */
	protected long m_lLastRunTime;
    /**
     * Pointer to configuration instance.
     */
	protected Config m_oConfig;
    /**
     * Pointer to log instance.
     */
	protected Log m_oLog;
		
	/**
     * <b> Default Constructor </b>
     * <p>
     * Calls {@link EmsMgr#run() }, and schedules further executions based off
     * the configured refresh interval.
     * </p>
     */
	private EmsMgr()
	{
		m_oConfig = ConfigSvc.getInstance().getConfig(this);
		m_oLog = Log.getInstance();

		// update the default refresh timing from the configuration
		DEFAULT_OFFSET = m_oConfig.getInt("offset", DEFAULT_OFFSET);
		DEFAULT_REFRESH = m_oConfig.getInt("refresh", DEFAULT_REFRESH);

		// write the metadata files upon initialization
		run();

		// schedule the refresh operation
		Scheduler.getInstance().schedule(this, DEFAULT_OFFSET, DEFAULT_REFRESH);
		m_oLog.write(this, "constructor");
	}
	
	/**
     * Creates meta-data .csv files at the configured location for every 
     * database table specified in the EmsMgr configuration file.
     * <p>
     * Prevents executions from happening more often than the default refresh.
     * Reconfigures based off any changes to the root config.
     * </p>
     * <p>
     * {@code EmsMgr.run()} required for implementation of interface
     * {@link Runnable}.
     * </p>
     */
	public synchronized void run()
	{
		// prevent successive iterations with an interval less than the default
		long lNow = System.currentTimeMillis();
		if (lNow - m_lLastRunTime < DEFAULT_REFRESH)
			return;
		
		m_lLastRunTime = lNow;

		// re-read the configuration to pick up any table or column changes
		m_oConfig.refresh();
		String sDir = m_oConfig.getString("metadata", null);
		String[] sTables = m_oConfig.getStringArray("table");

		if (sDir == null || sTables == null)
			return;
		
		m_oLog.write(this, "run");
		ClarusMgr oClarusMgr = ClarusMgr.getInstance();
		// append additional directory slash if one is not present
		if (!sDir.endsWith("/"))
			sDir += "/";

		// build one metadata output file for each specified table
		for (int nIndex = 0; nIndex < sTables.length; nIndex++)
		{
			String[] sParameters = m_oConfig.getStringArray(sTables[nIndex]);			
			String sDataSourceName = sParameters[0];
			String sQuery = sParameters[1];

			if (sDataSourceName == null || sQuery == null)
				continue;

			try
			{
				// open the metadata file
				FileWriter oFileWriter = 
					new FileWriter(sDir + sTables[nIndex] + ".csv");
				
				DataSource iDataSource =
					oClarusMgr.getDataSource(sDataSourceName);

				if (iDataSource == null)
					continue;

				Connection iConnection = iDataSource.getConnection();
				if (iConnection == null)
					continue;

				// get the result set and result set metadata
				ResultSet iResultSet = iConnection.createStatement().
					executeQuery(sQuery);
				ResultSetMetaData iMetaData = iResultSet.getMetaData();

				// write the Clarus metadata header
				int nCol = 0;
				for (nCol = 0; nCol < iMetaData.getColumnCount();)
				{
					if (nCol++ > 0)
						oFileWriter.write(",");

					oFileWriter.write("\"");
					oFileWriter.write(iMetaData.getColumnName(nCol));
					oFileWriter.write("\"");
				}
				oFileWriter.write("\n");

				// write the result set records
				while (iResultSet.next())
				{
					for (nCol = 0; nCol < iMetaData.getColumnCount();)
					{
						if (nCol++ > 0)
							oFileWriter.write(",");
						
						String sCol = iResultSet.getString(nCol);
						if (sCol != null && sCol.length() > 0
							&& !iResultSet.wasNull())
						{
							// write quotes for strings and not numbers
							int nColType = iMetaData.getColumnType(nCol);
							if
							(
								nColType == Types.CHAR ||
								nColType == Types.NCHAR ||
								nColType == Types.NVARCHAR ||
								nColType == Types.VARCHAR
							)
							{
								oFileWriter.write("\"");
								oFileWriter.write(sCol);
								oFileWriter.write("\"");
							}
							else
								oFileWriter.write(sCol);
						}
					}
					oFileWriter.write("\n");
				}
				
				oFileWriter.flush();
				oFileWriter.close();

				iResultSet.close();
				iConnection.close();
			}
			catch (Exception oException)
			{
				m_oLog.write(this, "run", sQuery);
				m_oLog.write(oException);
			}
		}
	}
}
