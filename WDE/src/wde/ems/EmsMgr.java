// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file EmsMgr.java
 */
package wde.ems;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;

import javax.sql.DataSource;
import java.io.FileWriter;
import java.sql.*;

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
 *
 * @param <T> template type must be specified for new instances.
 */
public class EmsMgr<T> implements Runnable {
    private static final Logger logger = Logger.getLogger(EmsMgr.class);

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
     * Tracks the last time {@link EmsMgr#run() } was called to help with
     * refresh scheduling.
     */
    protected long m_lLastRunTime;
    /**
     * Pointer to configuration instance.
     */
    protected Config m_oConfig;
    /**
     * <b> Default Constructor </b>
     * <p>
     * Calls {@link EmsMgr#run() }, and schedules further executions based off
     * the configured refresh interval.
     * </p>
     */
    private EmsMgr() {
        m_oConfig = ConfigSvc.getInstance().getConfig(this);

        // update the default refresh timing from the configuration
        DEFAULT_OFFSET = m_oConfig.getInt("offset", DEFAULT_OFFSET);
        DEFAULT_REFRESH = m_oConfig.getInt("refresh", DEFAULT_REFRESH);

        // write the metadata files upon initialization
        run();

        // schedule the refresh operation
        Scheduler.getInstance().schedule(this, DEFAULT_OFFSET, DEFAULT_REFRESH, true);
        logger.info("Completing constructor");
    }

    /**
     * <b> Accessor </b>
     *
     * @return Singleton instance of {@code EmsMgr}.
     */
    public static EmsMgr getInstance() {
        return g_oInstance;
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
    public synchronized void run() {
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

        logger.info("executing run");
        WDEMgr wdeMgr = WDEMgr.getInstance();
        // append additional directory slash if one is not present
        if (!sDir.endsWith("/"))
            sDir += "/";

        // build one metadata output file for each specified table
        for (int nIndex = 0; nIndex < sTables.length; nIndex++) {
            String[] sParameters = m_oConfig.getStringArray(sTables[nIndex]);
            String sDataSourceName = sParameters[0];
            String sQuery = sParameters[1];

            if (sDataSourceName == null || sQuery == null)
                continue;

            Connection iConnection = null;
            PreparedStatement ps = null;
            ResultSet rs = null;

            try {
                // open the metadata file
                String tableName = sTables[nIndex];

                if (tableName.equals("platform"))
                    tableName = "station";

                FileWriter oFileWriter =
                        new FileWriter(sDir + tableName + ".csv");

                DataSource iDataSource =
                        wdeMgr.getDataSource(sDataSourceName);

                if (iDataSource == null)
                    continue;

                iConnection = iDataSource.getConnection();
                if (iConnection == null)
                    continue;

                // get the result set and result set metadata
                ps = iConnection.prepareStatement(sQuery);
                rs = ps.executeQuery();
                ResultSetMetaData iMetaData = rs.getMetaData();

                // write the Clarus metadata header
                int nCol = 0;
                for (nCol = 0; nCol < iMetaData.getColumnCount(); ) {
                    if (nCol++ > 0)
                        oFileWriter.write(",");

                    oFileWriter.write("\"");
                    oFileWriter.write(iMetaData.getColumnName(nCol));
                    oFileWriter.write("\"");
                }
                oFileWriter.write("\n");

                // write the result set records
                while (rs.next()) {
                    for (nCol = 0; nCol < iMetaData.getColumnCount(); ) {
                        if (nCol++ > 0)
                            oFileWriter.write(",");

                        String sCol = rs.getString(nCol);
                        if (sCol != null && sCol.length() > 0
                                && !rs.wasNull()) {
                            // write quotes for strings and not numbers
                            int nColType = iMetaData.getColumnType(nCol);
                            if
                                    (
                                    nColType == Types.CHAR ||
                                            nColType == Types.NCHAR ||
                                            nColType == Types.NVARCHAR ||
                                            nColType == Types.VARCHAR
                                    ) {
                                oFileWriter.write("\"");
                                oFileWriter.write(sCol);
                                oFileWriter.write("\"");
                            } else
                                oFileWriter.write(sCol);
                        }
                    }
                    oFileWriter.write("\n");
                }

                oFileWriter.flush();
                oFileWriter.close();
            } catch (Exception oException) {
                logger.info("run " + sQuery);
                logger.error(oException);
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
    }
}
