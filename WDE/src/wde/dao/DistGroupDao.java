/************************************************************************
 * Source filename: DistGroupDao.java
 * <p/>
 * Creation date: Feb 25, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the distgroup table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.DistGroup;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class DistGroupDao {
    private static final Logger logger = Logger.getLogger(DistGroupDao.class);

    private static DistGroupDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private HashMap<String, DistGroup> distGroupMap = null;

    /**
     * Constructor
     */
    private DistGroupDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        distGroupMap = new HashMap<String, DistGroup>();
    }

    /**
     * @return DistGroupDao
     */
    public static DistGroupDao getInstance() {
        if (instance == null)
            instance = new DistGroupDao();

        return instance;
    }

    /**
     * @param id
     * @return the record for the given id
     */
    public DistGroup getDistGroup(String id) {
        return distGroupMap.get(id);
    }

    /**
     * Updates the distGroup map based on what's currently in the database
     */
    public void updateDistGroups() {
        String sql = "SELECT * FROM meta.distGroup";
        ResultSet rs = db.query(connId, sql);
        try {

            while (rs != null && rs.next()) {
                DistGroup distGroup = new DistGroup();
                distGroup.setId(rs.getInt("id"));
                distGroup.setDescription(rs.getString("description"));
                distGroupMap.put(String.valueOf(distGroup.getId()), distGroup);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }
}
