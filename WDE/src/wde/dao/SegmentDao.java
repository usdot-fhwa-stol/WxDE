/************************************************************************
 * Source filename: SegmentDao.java
 * <p/>
 * Creation date: Dec 18, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective: Data Access Object for the segment table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Segment;
import wde.metadata.TimeInvariantMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

public class SegmentDao {

    private static final Logger logger = Logger.getLogger(SegmentDao.class);

    private static SegmentDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private HashMap<String, TimeInvariantMetadata> segmentMap = null;

    /**
     * Constructor
     */
    private SegmentDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        segmentMap = new HashMap<>();
        updateSegmentMap();
    }

    /**
     * @return SegmentDao
     */
    public static SegmentDao getInstance() {
        if (instance == null)
            instance = new SegmentDao();

        return instance;
    }

    /**
     * Updates the obsType map based on what's currently in the database
     */
    public void updateSegmentMap() {
        String sql = "SELECT * FROM meta.segment";
        ResultSet rs = db.query(connId, sql);

        try {

            while (rs != null && rs.next()) {
                Segment segment = new Segment();
                segment.setId(String.valueOf(rs.getInt("id")));
                segment.setContribId(rs.getInt("contribId"));
                segment.setSegmentId(rs.getInt("segmentId"));
                segment.setSegmentName(rs.getString("segmentName"));
                segmentMap.put(segment.getContribId() + "-" + segment.getSegmentId(), segment);
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

    /**
     * @return the segmentMap
     */
    public HashMap<String, TimeInvariantMetadata> getSegmentMap() {
        return segmentMap;
    }
}
