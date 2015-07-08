/************************************************************************
 * Source filename: Query.java
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
 * This class shields the user from database query preparation specifics.
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;

import java.sql.*;

public class Query {

    static Logger logger = Logger.getLogger(Query.class);

    private Connection connection = null;

    private Statement stmt = null;

    private String queryStr = null;

    private ResultSet results = null;

    /**
     * @param connect
     */
    protected Query(Connection _connection) {
        connection = _connection;
    }

    /**
     * Initializes the query with a given query string.
     *
     * @param queryStr
     * @return boolean flag indicating whether operation is successful
     */
    public boolean load(String _queryStr) {
        boolean flag = false;
        queryStr = _queryStr;

        if (connection == null) {
            logger.error("There is no connection associated with this query.");
            return flag;
        }

        try {
            stmt = connection.createStatement();
            flag = true;
        } catch (SQLException se) {
            logger.error(se);
        } catch (Throwable t) {
            logger.error(t);
        } finally {
            try {
                if (!flag && stmt != null) {
                    stmt.close();
                    stmt = null;
                }
            } catch (SQLException se) {
                // ignore
            }
        }

        return flag;
    }

    /**
     * @param statementString
     * @return
     */
    public PreparedStatement createPreparedStatement(String statementString) {
        PreparedStatement pstmt = null;
        try {
            pstmt = connection.prepareStatement(statementString);
        } catch (SQLException se) {
            logger.error(se);
        }
        return pstmt;
    }

    /**
     * Executes the query.
     *
     * @return boolean flag indicating whether operation is successful
     */
    public ResultSet execute() {
        boolean flag = false;
        results = null;

        if (stmt == null) {
            logger.error("There is no statement opened for this query.");
            return results;
        }

        try {
            results = stmt.executeQuery(queryStr);

            flag = true;
        } catch (SQLException se) {
            logger.error("Error encountered in execute(): " + queryStr);
            logger.error(se);
        } catch (Throwable t) {
            logger.error(t);
        } finally {
            if (!flag && results != null) {
                try {
                    connection.rollback();
                    results.close();
                    results = null;
                } catch (SQLException se) {
                    // ignore
                }
            }
        }

        return results;
    }

    /**
     * Updates the database using the query.
     *
     * @return number of rows modified
     */
    public int update() {
        boolean flag = false;
        int count = 0;

        try {
            count = stmt.executeUpdate(queryStr);

            flag = true;
        } catch (SQLException se) {
            se.printStackTrace();
            logger.error("Error encountered in update(): " + queryStr);
            logger.error(se);
        } catch (Throwable t) {
            t.printStackTrace();
            logger.error(t);
        } finally {
            if (!flag) {
                try {
                    connection.rollback();
                } catch (SQLException se) {
                    // ignore
                }
            }
        }
        return count;
    }

    /**
     * Closes the query object.
     */
    public void close() {
        try {
            stmt.close();
            queryStr = null;
            results = null;
        } catch (SQLException se) {
            logger.error(se);
        }
    }

    /**
     * Get results of an executed Query
     * @return the result set generated from running the query.
     */
    public ResultSet getResults() {
        return results;
    }
}
