/************************************************************************
 * Source filename: DatabaseManager.java
 * <p/>
 * Creation date: February 19, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 ***********************************************************************/
package wde.dao;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.security.Encryption;

import javax.sql.DataSource;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Properties;

public class DatabaseManager {

    public static boolean wdeMgrInstantiated = false;
    static Logger logger = Logger.getLogger(DatabaseManager.class);
    private static DatabaseManager instance = null;

    private Properties prop = null;

    private DataSource webDataSource = null;

    private String driverClassName = null;

    private String dbUrl = null;

    private String username = null;

    private String password = null;

    /** A collection of connections currently active */
    private HashMap<String, Connection> connections = null;

    /**
     * Constructor
     */
    private DatabaseManager() {
        connections = new HashMap<String, Connection>();
        prop = new Properties();
        loadPropertiesFile();
    }

    /**
     * @return a reference to the DatabaseManager singleton.
     */
    public static DatabaseManager getInstance() {

        if (instance == null)
            instance = new DatabaseManager();

        return instance;
    }

    /**
     * Establishes a connection to a given database.
     *
     * @param url - database URL
     * @return connection id
     */
    public synchronized String getConnection() {

        Connection conn = null;
        String connId = null;

        boolean flag = false;
        try {
            if (webDataSource != null)
                conn = webDataSource.getConnection();
            else {
                Driver drv = (Driver) Class.forName(driverClassName).newInstance();
                DriverManager.registerDriver(drv);
                conn = DriverManager.getConnection(dbUrl, username, password);
            }
            conn.setAutoCommit(true);
            connId = String.valueOf(conn.hashCode());
            connections.put(connId, conn);
            flag = true;
        } catch (SQLException se) {
            logger.error(se);
        } catch (Throwable t) {
            logger.error(t);
        } finally {
            if (!flag && conn != null) {
                try {
                    conn.close();
                    conn = null;
                } catch (SQLException se) {
                    // ignore
                }
            }
        }

        return connId;
    }

    public void updateAutoCommit(String connId, boolean flag) {
        Connection conn = connections.get(connId);
        if (conn != null)
            try {
                conn.setAutoCommit(flag);
            } catch (SQLException se) {
                logger.error(se);
            }
    }

    public ResultSet query(String connId, String queryStr) {
        Query query = createQuery(connId);
        query.load(queryStr);
        ResultSet rs = query.execute();

        return rs;
    }

    public PreparedStatement prepareStatement(String connId, String statementString) {
        Query query = createQuery(connId);
        return query.createPreparedStatement(statementString);
    }

    /**
     * @param connId
     * @param queryStr
     * @return the number of rows affected
     */
    public int update(String connId, String queryStr) {
        Query query = createQuery(connId);
        query.load(queryStr);
        int rows = query.update();

        return rows;
    }

    /**
     * @param connId
     * @return DatabaseMetaData
     */
    public DatabaseMetaData getMetaData(String connId) {
        DatabaseMetaData dmd = null;
        Connection conn = (Connection) connections.get(connId);
        if (conn == null) {
            logger.error("There is no connection associated with id: " + connId);
            return null;
        }

        try {
            dmd = conn.getMetaData();
        } catch (SQLException se) {
            logger.error(se);
        } catch (Throwable t) {
            logger.error(t);
        }

        return dmd;
    }

    /**
     * Creates a query for a given connection.
     *
     * @param connId
     * @return a query object
     */
    public Query createQuery(String connId) {

        Connection conn = (Connection) connections.get(connId);
        if (conn == null) {
            logger.error("There is no connection associated with id: " + connId);
            return null;
        }

        return new Query(conn);
    }

    /**
     * @param connId
     */
    public void releaseConnection(String connId) {
        Connection conn = (Connection) connections.get(connId);
        if (conn == null) {
            logger.error("There is no connection associated with id: " + connId);
            return;
        }

        try {
            conn.close();
            connections.remove(connId);
            conn = null;
        } catch (SQLException se) {
            // ignore
        }
    }

    /**
     * Closes all open database connections.
     */
    public void close() {
        try {
            //	Close all open db connections.
            Connection[] conns = getOpenConnections();
            int len = conns.length;
            for (int i = 0; i < len; i++) {
                conns[i].close();
            }
            instance = null;
        } catch (SQLException se) {
            logger.error(se);
        } catch (Throwable t) {
            logger.error(t);
        }
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        if (!wdeMgrInstantiated) {

            String separator = System.getProperty("file.separator");
            String path = System.getProperty("user.dir") + separator + "config" + separator + "db_config.properties";

            try {
                FileInputStream fis = new FileInputStream(path);
                prop.load(fis);
                fis.close();

                driverClassName = prop.getProperty("driverclassname");
                dbUrl = prop.getProperty("url");
                username = prop.getProperty("username");
                password = prop.getProperty("password");
                password = Encryption.decryptToString(password);
            } catch (IOException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                System.exit(-1);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
                System.exit(-1);
            }
        } else
            webDataSource = WDEMgr.getInstance().getDataSource("java:comp/env/jdbc/wxde");
    }

    /**
     * @return a list of the open connections to the database.
     */
    private Connection[] getOpenConnections() {
        Connection[] conns = new Connection[connections.size()];
        Object[] keys = connections.keySet().toArray();
        int len = keys.length;
        int count = 0;
        Connection conn = null;

        try {
            for (int i = 0; i < len; i++) {
                conn = (Connection) connections.get(keys[i]);

                if (!conn.isClosed()) {
                    conns[count] = conn;
                    count++;
                }
            }
        } catch (SQLException se) {
            logger.error(se);
        } catch (Throwable t) {
            logger.error(t);
        }
        return conns;
    }
}