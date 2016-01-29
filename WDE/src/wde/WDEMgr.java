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
 * Removed logic that retrieved the datasources from the app context. This was removed because
 * datasources are now retrieved from the wde.WDEMgr configuration file. This eliminated
 * the need to configure the datasources in two locations.
 * <p/>
 * 01-Apr-2013     George Zheng (SAIC)
 * Renamed the class to WDEMgr
 * <p/>
 * 12-Apr-2013     Bryan Krueger (Synesis Partners)
 * Removed references to MySQL database connections
 *
 * @file WDEMgr.java
 */

/**
 * @file WDEMgr.java
 */

package wde;

import org.apache.log4j.Logger;
import wde.compute.InferenceMgr;
import wde.dao.DatabaseManager;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.CollectionReport;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;

import javax.naming.InitialContext;
import javax.naming.NoInitialContextException;
import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;

//import wde.vdt.VDTController;

/**
 * {@code WDEMgr} is the main entry point to the WDE System.
 * <p>
 * This is a singleton class - there is one global instance accessed with the
 * {@code getInstance} method.
 * </p>
 * <p>
 * The purpose of this class is to hold the sequence of component processes
 * as specified by the root config file.
 * </p>
 */
public class WDEMgr extends TimerTask {
    private static final Logger logger = Logger.getLogger(WDEMgr.class);

    private static final long NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000L;

    /**
     * The singleton instance of {@code WDEMgr}.
     */
    private static WDEMgr g_oInstance = new WDEMgr();

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
     * timer for global tasks
     */
    private Timer timer = null;


    /**
     * Collection Report file path
     */
    private String collectionReportPath = null;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Initializes newly created instances of {@code WDEMgr} so that it is
     * ready to connect to the database located as specified in the root
     * configuration file.
     * </p>
     */
    private WDEMgr() {
        // get the root configuration
        m_oConfig = ConfigSvc.getInstance().getConfig(this);
        System.out.println("FILE NAME: " + m_oConfig.m_sFilename);

        try {
            // generate the list of datasources
            InitialContext oInitCtx = new InitialContext();
            String[] sDataSources = m_oConfig.getStringArray("datasource");
            collectionReportPath = m_oConfig.getString("collectionreportpath", "");
            for (int nIndex = 0; nIndex < sDataSources.length; nIndex++) {
                // parse the datasource configuration string
                String[] sParms = sDataSources[nIndex].split(",");

                // search the context for an existing datasource
                DataSource iDataSource = null;

                try {
                    iDataSource = (DataSource) oInitCtx.lookup(sParms[0]);
                    m_oDataSources.add(new DataSourceName(sParms[0], iDataSource));
                } catch (NoInitialContextException oNoInitialContextException) {
                    // suppress initial context exceptions
                } catch (Exception oException) {
                    // typical exceptions are no default context,
                    // or JNDI name not found
                    oException.printStackTrace();
                }
            }

            // reserve index locations for AsyncQ registration
            m_sSeqClassnames = m_oConfig.getStringArray("order");
            m_oSequence.ensureCapacity(m_sSeqClassnames.length);
            int nIndex = m_sSeqClassnames.length;
            while (nIndex-- > 0) {
                m_oSequence.add(null);
            }

            DatabaseManager.wdeMgrInstantiated = true;
//            VDTController.wdeMgrInstantiated = true;
            CollectionReport.wdeMgrInstantiated = true;
        } catch (Exception oException) {
            oException.printStackTrace();
        }

        timer = new Timer();

        long currentTime = System.currentTimeMillis();

        // 4 hours pass mid-night
        long fourHours = NUM_OF_MILLI_SECONDS_IN_A_DAY / 6;
        long extra = currentTime % NUM_OF_MILLI_SECONDS_IN_A_DAY;

        long delay = fourHours - extra;
        if (extra >= fourHours)
            delay += NUM_OF_MILLI_SECONDS_IN_A_DAY;

        logger.info("wait for " + delay / 1000 + " seconds before the first run");

        // schedule the task to run every mid-night
        timer.scheduleAtFixedRate(this, delay, NUM_OF_MILLI_SECONDS_IN_A_DAY);

        //InferenceMgr.getInstance();
    }

    /**
     * Gets the global singleton instance of {@code WDEMgr}.
     *
     * @return The singleton instance of {@code WDEMgr}.
     */
    public static WDEMgr getInstance() {
        return g_oInstance;
    }

    /**
     * Prints the data items contained in {@code iObsSet} in the form:
     * <p><blockquote>
     *     {object type}, {Sensor ID}, {Timestamp}, {Latitude}, {Longitude},
     *     {Elevation}, {Value}, {Run}, {Flags}, {Confidence Level}
     * </blockquote></p>
     *
     * @param iObsSet The object set to be printed.
     * @see Observation
     */
    public static void displayObsSet(IObsSet iObsSet) {
        for (int nIndex = 0; nIndex < iObsSet.size(); nIndex++) {
            System.out.print(iObsSet.getObsType());
            System.out.print(",");

            IObs iObs = iObsSet.get(nIndex);

            System.out.print(iObs.getSourceId());
            System.out.print(",");
            System.out.print(iObs.getSensorId());
            System.out.print(",");
            System.out.print(iObs.getObsTimeLong());
            System.out.print(",");
            System.out.print(iObs.getLatitude());
            System.out.print(",");
            System.out.print(iObs.getLongitude());
            System.out.print(",");
            System.out.print(iObs.getElevation());
            System.out.print(",");
            System.out.print(iObs.getValue());
            System.out.print(",");
            System.out.print(iObs.getQchCharFlag());
            System.out.print(",");
            System.out.println(iObs.getConfValue());
        }
    }

    /**
     * Catches the instance of {@code WDEMgr} and instantaites it.
     * @param sArgs Not used
     * @throws Exception Not used
     */
    public static void main2(String[] sArgs) throws Exception {
        WDEMgr wdeMgr = WDEMgr.getInstance();
        wdeMgr.startup();

        logger.info("system started");

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

//		wdeMgr.shutdown();

//		wde.emc.CacheClimateRecords.getInstance().toString(new java.io.PrintWriter(System.out, true));

        // join the thread pool
    }

    public void run() {
        logger.info("Generating report " + collectionReportPath);

        CollectionReport cr = new CollectionReport();
        cr.setReportPath(collectionReportPath);
        cr.collectDailyStatistics();
    }

    /**
     * Orders the initialization of services based off the root config file.
     *
     */
    public void startup() {
        try {
            // maintain references to started components so other components
            // have an opportunity to grab them before garbage collection
            String[] sComponents = m_oConfig.getStringArray("start");
            for (int nIndex = 0; nIndex < sComponents.length; nIndex++) {
                logger.info("Starting " + Class.forName(sComponents[nIndex]));
                m_oComponents.add(Class.forName(sComponents[nIndex]));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

//        VDTController.getInstance();
        logger.info("startup");
    }

    /**
     * Empties the components list {@code m_oComponents}.
     */
    public void shutdown() {
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
    public synchronized DataSource getDataSource(String sName) {
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
    public void register(String sClassname, AsyncQ<IObsSet> oAyncQ) {
        // search for a classname match
        int nIndex = m_sSeqClassnames.length;
        while (nIndex-- > 0) {
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
    public void queue(IObsSet iObsSet) {
        int nState = iObsSet.getState();
        if (nState < m_oSequence.size()) {
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
    private class DataSourceName implements Comparable<String> {
        private String m_sName;
        private DataSource m_iDataSource;

        private DataSourceName() {
        }

        /**
         * Initializes the name and data source for the {@code DataSourceName}
         * object.
         *
         * @param sName The name to be used with the new object.
         * @param iDataSource The {@code DataSource} to be used with the new
         *        object.
         */
        private DataSourceName(String sName, DataSource iDataSource) {
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
        public int compareTo(String sName) {
            return m_sName.compareTo(sName);
        }
    }
}
