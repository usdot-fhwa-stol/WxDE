/************************************************************************
 * Source filename: QualityFlagDao.java
 * <p/>
 * Creation date: Feb 28, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the qualityflag table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.QualityFlag;

import java.sql.*;
import java.util.ArrayList;

public class QualityFlagDao {

    private static final Logger logger = Logger.getLogger(QualityFlagDao.class);

    private static final String SELECT_SOURCEID_TIMESTAMP_QUERY = "SELECT * FROM meta.qualityFlag WHERE sourceId = ?"
            + " AND updateTime < ?"
            + " AND toTime IS NULL OR toTime > ?"
            + " ORDER BY id ASC LIMIT 1";

    private static final String SELECT_QUALITY_FLAGS_STR_QUERY = "SELECT s.name, s.id, q.updatetime, q.totime, q.qchflaglabel FROM meta.qualityFlag q, meta.source s where q.sourceid=s.id order by s.id";

    private static final String SELECT_QUALITY_FLAGS_QUERY = "SELECT * FROM meta.qualityFlag";

    private static QualityFlagDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    /**
     * Constructor
     */
    private QualityFlagDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
    }

    /**
     * @return ImageDao
     */
    public static QualityFlagDao getInstance() {
        if (instance == null)
            instance = new QualityFlagDao();

        return instance;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public QualityFlag getQualityFlag(int sourceId, Timestamp timestamp) {
        QualityFlag qualityFlag = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_SOURCEID_TIMESTAMP_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, sourceId);
            ps.setTimestamp(2, timestamp);
            ps.setTimestamp(3, timestamp);
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                qualityFlag = new QualityFlag();
                qualityFlag.setId(rs.getInt("id"));
                qualityFlag.setSourceId(sourceId);
                qualityFlag.setUpdateTime(rs.getTimestamp("updateTime"));
                qualityFlag.setToTime(rs.getTimestamp("toTime"));
                qualityFlag.setQchCharFlagLen(rs.getInt("qchCharFlagLen"));
                Array labels = rs.getArray("qchFlagLabel");
                qualityFlag.setQchFlagLabel((String[]) labels.getArray());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        return qualityFlag;
    }

    /**
     * @return
     */
    public ArrayList<QualityFlag> getQualityFlags() {
        ArrayList<QualityFlag> qfs = new ArrayList<>();
        QualityFlag qualityFlag = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_QUALITY_FLAGS_QUERY);
        ResultSet rs = null;

        try {
            rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                qualityFlag = new QualityFlag();
                qualityFlag.setId(rs.getInt("id"));
                qualityFlag.setSourceId(rs.getInt("sourceid"));
                qualityFlag.setUpdateTime(rs.getTimestamp("updateTime"));
                qualityFlag.setToTime(rs.getTimestamp("toTime"));
                qualityFlag.setQchCharFlagLen(rs.getInt("qchCharFlagLen"));
                Array labels = rs.getArray("qchFlagLabel");
                qualityFlag.setQchFlagLabel((String[]) labels.getArray());
                qfs.add(qualityFlag);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        return qfs;
    }

    /**
     * @return
     */
    public ArrayList<String> getQualityFlagStringArray() {
        ArrayList<String> qualityFlags = new ArrayList<>();

        PreparedStatement ps = db.prepareStatement(connId, SELECT_QUALITY_FLAGS_STR_QUERY);
        ResultSet rs = null;

        try {
            rs = ps.executeQuery();
            String qf = null;

            while (rs != null && rs.next()) {
                String name = rs.getString("name");
                int id = rs.getInt("id");
                Timestamp ts1 = rs.getTimestamp("updateTime");
                Timestamp ts2 = rs.getTimestamp("toTime");
                Array labels = rs.getArray("qchFlagLabel");
                String[] labelArray = (String[]) labels.getArray();

                qf = name + "(" + id + ") [" + ts1.toString() + ", " + ((ts2 != null) ? ts2.toString() : "") + ") - ";
                for (String s : labelArray)
                    qf += s + ",";

                // Remove the last comma
                qf = qf.substring(0, qf.length() - 1);
                qualityFlags.add(qf);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        return qualityFlags;
    }
}
