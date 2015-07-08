/************************************************************************
 * Source filename: SensorTypeDao.java
 * <p/>
 * Creation date: Feb 24, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the sensortype table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.SensorType;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SensorTypeDao {

    private static final Logger logger = Logger.getLogger(SensorTypeDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.sensorType WHERE staticId = ? ORDER BY id desc limit 1";

    private static SensorTypeDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> sensorTypeMap = null;

    /**
     * Constructor
     */
    private SensorTypeDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        sensorTypeMap = new HashMap<>();
        updateSensorTypeMap();
    }

    /**
     * @return SensorTypeDao
     */
    public static SensorTypeDao getInstance() {
        if (instance == null)
            instance = new SensorTypeDao();

        return instance;
    }

    /**
     * @return the sensorTypeMap
     */
    public HashMap<String, TimeVariantMetadata> getSensorTypeMap() {
        return sensorTypeMap;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public SensorType getSensorType(String staticId) {
        SensorType sensorType = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                sensorType = new SensorType();
                sensorType.setId(rs.getInt("id"));
                sensorType.setStaticId(staticId);
                sensorType.setUpdateTime(rs.getTimestamp("updateTime"));
                sensorType.setToTime(rs.getTimestamp("toTime"));
                sensorType.setMfr(rs.getString("mfr"));
                sensorType.setModel(rs.getString("model"));
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

        return sensorType;
    }

    /**
     * Updates the sensorType map based on what's currently in the database
     */
    public void updateSensorTypeMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.sensorType";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                SensorType sensorType = getSensorType(staticId);
                sensorTypeMap.put(sensorType.getStaticId(), sensorType);
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
     * @param sensorType
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in sensorType needs to be inserted; false if no change is found
     */
    public boolean updateSensorType(SensorType sensorType, boolean atomic) {
        String key = sensorType.getStaticId();

        SensorType dbSensorType = (SensorType) sensorTypeMap.get(key);

        if (dbSensorType == null || !dbSensorType.equals(sensorType)) {

            if (dbSensorType == null)
                logger.info("inserting sensorType " + key);
            else
                logger.info("updating sensorType " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.sensorType (staticId, updateTime, mfr, model)  VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convert(sensorType.getMfr(), false)
                    + QueryString.convert(sensorType.getModel(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                sensorType = getSensorType(key);
                sensorTypeMap.put(key, sensorType);
            } else
                db.update(sharedConnId, sql);

            if (dbSensorType != null)
                md.inactivateRecord("sensorType", dbSensorType.getId(), now);

            if (atomic)
                md.lastUpate("sensorType", now);

            return true;
        }

        return false;
    }
}
