/************************************************************************
 * Source filename: QchparmDao.java
 * <p/>
 * Creation date: Feb 25, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the qchparm table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Qchparm;
import wde.metadata.TimeInvariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class QchparmDao {

    private static final Logger logger = Logger.getLogger(QchparmDao.class);

    private static final String SELECT_ID_QUERY = "SELECT * FROM meta.qchparm WHERE id = ?";

    private static QchparmDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeInvariantMetadata> qchparmMap = null;

    /**
     * Constructor
     */
    private QchparmDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        qchparmMap = new HashMap<>();
        updateQchparmMap();
    }

    /**
     * @return QchparmDao
     */
    public static QchparmDao getInstance() {
        if (instance == null)
            instance = new QchparmDao();

        return instance;
    }

    /**
     * @return the qchparmMap
     */
    public HashMap<String, TimeInvariantMetadata> getQchparmMap() {
        return qchparmMap;
    }

    /**
     * @param id
     * @return the record that has the same id
     */
    public Qchparm getQchparm(String id) {
        Qchparm qchparm = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_ID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(id));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                qchparm = new Qchparm();
                qchparm.setId(id);
                qchparm.setSensorTypeId(rs.getInt("sensorTypeId"));
                qchparm.setObsTypeId(rs.getInt("obsTypeId"));
                qchparm.setDefault(rs.getBoolean("isDefault"));
                qchparm.setMinRange(rs.getFloat("minRange"));
                qchparm.setMaxRange(rs.getFloat("maxRange"));
                qchparm.setResolution(rs.getFloat("resolution"));
                qchparm.setAccuracy(rs.getFloat("accuracy"));
                qchparm.setRatePos(rs.getDouble("ratePos"));
                qchparm.setRateNeg(rs.getDouble("rateNeg"));
                qchparm.setRateInterval(rs.getDouble("rateInterval"));
                qchparm.setPersistInterval(rs.getDouble("persistInterval"));
                qchparm.setPersistThreshold(rs.getDouble("persistThreshold"));
                qchparm.setLikeThreshold(rs.getDouble("likeThreshold"));
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

        return qchparm;
    }

    /**
     * Updates the qchparm map based on what's currently in the database
     */
    public void updateQchparmMap() {
        String sql = "SELECT id FROM meta.qchparm";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> ids = new ArrayList<String>();
            while (rs != null && rs.next()) {
                ids.add(String.valueOf(rs.getInt("id")));
            }

            for (String id : ids) {
                Qchparm qchparm = getQchparm(id);
                qchparmMap.put(id, qchparm);
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
     * @param qchparm
     * @param atomic - false if this is part of a batch processing
     * @return true if a record in qchparm is inserted; false if database already has the record
     */
    public boolean insertQchparm(Qchparm qchparm, boolean atomic) {
        String key = qchparm.getId();

        Qchparm dbQchparm = (Qchparm) qchparmMap.get(key);

        if (dbQchparm != null) {
            logger.error("qchparm: " + key + " already in the database");
            return false;
        }

        logger.info("inserting qchparm " + key);

        Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
        String sql = "INSERT INTO meta.qchparm (id, updateTime, sensorTypeId, obsTypeId, isDefault, minRange, maxRange, resolution,"
                + "accuracy, ratePos, rateNeg, rateInterval, persistInterval, persistThreshold, likeThreshold" + ")  VALUES ("
                + key + ","
                + "'" + now + "',"
                + QueryString.convertId(qchparm.getSensorTypeId(), false)
                + QueryString.convertId(qchparm.getObsTypeId(), false)
                + qchparm.isDefault() + ","
                + QueryString.convert(qchparm.getMinRange(), false)
                + QueryString.convert(qchparm.getMaxRange(), false)
                + QueryString.convert(qchparm.getResolution(), false)
                + QueryString.convert(qchparm.getAccuracy(), false)
                + QueryString.convert(qchparm.getRatePos(), false)
                + QueryString.convert(qchparm.getRateNeg(), false)
                + QueryString.convert(qchparm.getRateInterval(), false)
                + QueryString.convert(qchparm.getPersistInterval(), false)
                + QueryString.convert(qchparm.getPersistThreshold(), false)
                + QueryString.convert(qchparm.getLikeThreshold(), true)
                + ");";

        if (atomic) {
            db.update(connId, sql);

            // Update the id field
            qchparm = getQchparm(key);
            qchparmMap.put(key, qchparm);
            md.lastUpate("qchparm", now);
        } else
            db.update(sharedConnId, sql);

        return true;
    }
}
