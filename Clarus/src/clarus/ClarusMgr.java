/**
 * Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
 * 
 * Author: 	n/a
 * Date: 	n/a
 * 
 * Modification History:
 *		dd-Mmm-yyyy		iii		[Bug #]
 *			Change description.
 *
 * 		29-Jun-2012		das		
 * 			Removed logic that retrieved the datasources from the app context. This was removed because
 * 			datasources are now retrieved from the clarus.ClarusMgr configuration file. This eliminated
 * 			the need to configure the datasources in two locations.
 */

/**
 * @file ClarusMgr.java
 */

package clarus;

import java.util.ArrayList;
import java.util.Collections;
import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;
import javax.sql.DataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlConnectionPoolDataSource;
import clarus.qedc.IObs;
import clarus.qedc.IObsSet;
import clarus.qedc.Obs;
import util.Config;
import util.ConfigSvc;
import util.Log;
import util.threads.AsyncQ;

/**
 * {@code ClarusMgr} is the main entry point to the Clarus System.
 * <p>
 * This is a singleton class - there is one global instance accessed with the
 * {@code getInstance} method.
 * </p>
 * <p>
 * The purpose of this class is to hold the sequence of component processes
 * as specified by the root config file.
 * </p>
 */
public class ClarusMgr
{
    /**
     * The singleton instance of {@code ClarusMgr}.
     */
	private static ClarusMgr g_oInstance = new ClarusMgr();

    /**
     * Ordered list of names for the Obs set processes.
     */
	private String[] m_sSeqClassnames;

     /**
      * Asynchronous queue of Obs processors.
      */
	private ArrayList<AsyncQ<IObsSet>> m_oSequence =
            new ArrayList<AsyncQ<IObsSet>>();

    /**
     * Ordered list of services to initialize.
     */
	private ArrayList<Object> m_oComponents = new ArrayList<Object>();

    /**
     * List of database sources.
     */
	private ArrayList<DataSourceName> m_oDataSources =
            new ArrayList<DataSourceName>();

    /**
     * Points to the root configuration.
     */
	private Config m_oConfig;


    /**
     * Gets the global singleton instance of {@code ClarusMgr}.
     *
     * @return The singleton instance of {@code ClarusMgr}.
     */
    public static ClarusMgr getInstance()
	{
		return g_oInstance;
	}

	/**
	 * <b> Default Constructor </b>
	 * <p>
     * Initializes newly created instances of {@code ClarusMgr} so that it is
     * ready to connect to the database located as specified in the root
     * configuration file.
	 * </p>
     */
	private ClarusMgr()
	{
		// get the root configuration
		m_oConfig = ConfigSvc.getInstance().getConfig(this);

		try
		{
			// generate the list of datasources
			InitialContext oInitCtx = new InitialContext();
			String[] sDataSources = m_oConfig.getStringArray("datasource");
			for (int nIndex = 0; nIndex < sDataSources.length; nIndex++)
			{
				// parse the datasource configuration string
				String[] sParms = sDataSources[nIndex].split(",");

				// search the context for an existing datasource
				DataSource iDataSource = null;

				// Commented out because the Clarus web apps retrieve connections 
				// through the conf files.
//				try
//				{
//					iDataSource = (DataSource)oInitCtx.lookup(sParms[0]);
//				}
//				catch (NoInitialContextException oNoInitialContextException)
//				{
//					// supress initial context exceptions
//				}
//				catch (Exception oException)
//				{
//					// typical exceptions are no default context,
//					// or JNDI name not found
//					oException.printStackTrace();
//				}

				// create a new datasource when one is not found
				if (iDataSource == null)
				{
					MysqlConnectionPoolDataSource oMysqlSource =
						new MysqlConnectionPoolDataSource();

					// set the required parameters
					oMysqlSource.setServerName(sParms[1]);
					oMysqlSource.setDatabaseName(sParms[2]);
					oMysqlSource.setUser(sParms[3]);
					oMysqlSource.setPassword(sParms[4]);

					// override the default network port
					if (sParms.length == 6)
						oMysqlSource.setPort(Integer.parseInt(sParms[5]));

					// save the instance as the datasource interface
					iDataSource = oMysqlSource;
				}

				m_oDataSources.add(new DataSourceName(sParms[0], iDataSource));
			}

			// reserve index locations for AsyncQ registration
			m_sSeqClassnames = m_oConfig.getStringArray("order");
			m_oSequence.ensureCapacity(m_sSeqClassnames.length);
			int nIndex = m_sSeqClassnames.length;
			while (nIndex-- > 0)
				m_oSequence.add(null);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}


    /**
     * Orders the intialization of services based off the root config file.
     *
     */
    public void startup()
	{
		try
		{
			// maintain references to started components so other components
			// have an opportunity to grab them before garbage collection
			String[] sComponents = m_oConfig.getStringArray("start");
			for (int nIndex = 0; nIndex < sComponents.length; nIndex++)
				m_oComponents.add(Class.forName(sComponents[nIndex]));
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}

		Log.getInstance().write(this, "startup");
	}


    /**
     * Empties the components list {@code m_oComponents}.
     */
    public void shutdown()
	{
		m_oComponents.clear();
	}


    /**
     * @param sName The name of the data source to retrieve.
     * @return The data source of name {@code sName}.
     *         <p>
     *         null - if the name is invalid, or doesn't exist in the list
     *         of sources.
     *         </p>
     */
    public synchronized DataSource getDataSource(String sName)
	{
		if (sName == null || sName.length() == 0)
			return null;

		int nIndex = Collections.binarySearch(m_oDataSources, sName);
		if (nIndex < 0)
			return null;

		return m_oDataSources.get(nIndex).m_iDataSource;
	}


    /**
     * Registers {@code oAsyncQ} in the list of AsyncQ's ({@code m_oSequence})
     * at the index of the current location of {@code sClassname} in the
     * ordered list of process names ({@code m_sSeqClassnames}), whose order is
     * determined by the root config file.
     *
     * @param sClassname The name of the process to be registered in the
     *                   sequence list.
     * @param oAyncQ The AsyncQ to be added to the sequence list.
     */
    public void register(String sClassname, AsyncQ<IObsSet> oAyncQ)
	{
		// search for a classname match
		int nIndex = m_sSeqClassnames.length;
		while (nIndex-- > 0)
		{
			// set the index location to the supplied object for each match
			if (m_sSeqClassnames[nIndex].compareTo(sClassname) == 0)
				m_oSequence.set(nIndex, oAyncQ);
		}
	}


    /**
     * Adds {@code iObsSet} to an asynchronous queue to be processed as
     * the object is queued.
     *
     * @param iObsSet The objects to be added to the asynchronous queue.
     * @see AsyncQ
     */
    public void queue(IObsSet iObsSet)
	{
		int nState = iObsSet.getState();
		if (nState < m_oSequence.size())
		{
			AsyncQ<IObsSet> oAsyncQ = m_oSequence.get(nState);
    		iObsSet.setState(++nState);

			if (oAsyncQ != null)
				oAsyncQ.queue(iObsSet);
		}
	}

	/**
     * Associates a {@code String} with a {@code DataSource} that can be used as
     * a name for {@code String} comparisons with the {@code compareTo} method.
     *
     * @see DataSource
     */
	private class DataSourceName implements Comparable<String>
	{
		private String m_sName;
		private DataSource m_iDataSource;

		private DataSourceName()
		{
		}

		/**
         * Initializes the name and data source for the {@code DataSourceName}
         * object.
         *
         * @param sName The name to be used with the new object.
         * @param iDataSource The {@code DataSource} to be used with the new
         *        object.
         */
		private DataSourceName(String sName, DataSource iDataSource)
		{
			m_sName = sName;
			m_iDataSource = iDataSource;
		}

        /**
         * Compares {@code sName} to the name of the {@code DataSourceName}
         * object, ala java.lang.String.compareTo(String).
         * @param sName
         * @return 0 if the names ({@code String}s) are equal
         *
         * @see String#compareTo(java.lang.String)
         */
		public int compareTo(String sName)
		{
			return m_sName.compareTo(sName);
		}
	}


    /**
     * Prints the data items contained in {@code iObsSet} in the form:
     * <p><blockquote>
     *     {object type}, {Sensor ID}, {Timestamp}, {Latitude}, {Longitude},
     *     {Elevation}, {Value}, {Run}, {Flags}, {Confidence Level}
     * </blockquote></p>
     *
     * @param iObsSet The object set to be printed.
     * @see Obs
     */
    public static void displayObsSet(IObsSet iObsSet)
	{
		for (int nIndex = 0; nIndex < iObsSet.size(); nIndex++)
		{
			System.out.print(iObsSet.getObsType());
			System.out.print(",");

			IObs iObs = iObsSet.get(nIndex);

			System.out.print(iObs.getSensorId());
			System.out.print(",");
			System.out.print(iObs.getTimestamp());
			System.out.print(",");
			System.out.print(iObs.getLat());
			System.out.print(",");
			System.out.print(iObs.getLon());
			System.out.print(",");
			System.out.print(iObs.getElev());
			System.out.print(",");
			System.out.print(iObs.getValue());
			System.out.print(",");
			System.out.print(iObs.getRun());
			System.out.print(",");
			System.out.print(iObs.getFlags());
			System.out.print(",");
			System.out.println(iObs.getConfidence());
		}
	}


	/**
     * Catches the instance of {@code ClarusMgr} and instantaites it.
     * @param sArgs Not used
     * @throws Exception Not used
     */
	public static void main2(String[] sArgs) throws Exception
	{
		ClarusMgr oClarusMgr = ClarusMgr.getInstance();
		oClarusMgr.startup();

		Log.getInstance().write(oClarusMgr, "system started");

//		Stations oStations = Stations.getInstance();
//		oStations.countNeighbors(1000000L);

//		for (int nOuter = 1; nOuter < 69; nOuter++)
//		{
//			System.out.print(nOuter);

//			for (int nInner = 2; nInner < 6; nInner++)
//			{
//				System.out.print(',');
//				System.out.print(oStations.countNeighbors(1000000L, nOuter, nInner));
//			}

//			System.out.println();
//		}

//		oClarusMgr.shutdown();

//		clarus.emc.CacheClimateRecords.getInstance().toString(new java.io.PrintWriter(System.out, true));

		// join the thread pool
	}
}
