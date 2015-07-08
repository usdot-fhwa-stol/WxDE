/************************************************************************
 * Source filename: MetadataDao.java
 * <p/>
 * Creation date: Feb 20, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.TimeInvariantMetadata;
import wde.metadata.TimeVariantMetadata;

import java.sql.Timestamp;
import java.util.*;

public class MetadataDao {

    private static final Logger logger = Logger.getLogger(MetadataDao.class);

    private static MetadataDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    /**
     * Constructor
     */
    private MetadataDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
    }

    /**
     * @return FederationManagementDao
     */
    public static MetadataDao getInstance() {
        if (instance == null)
            instance = new MetadataDao();

        return instance;
    }

    /**
     * Updates all records in a time variant records table for the given tableName
     *
     * @param objs
     * @param objMap
     * @param tableName
     */
    public void updateAllTimeVariantRecords(ArrayList<TimeVariantMetadata> objs, HashMap<String, TimeVariantMetadata> objMap, String tableName) {
        logger.info("Updating all records in table " + tableName);

        HashMap<String, TimeVariantMetadata> objMapCopy = (HashMap<String, TimeVariantMetadata>) objMap.clone();
        TimeVariantMetadata obj = null;

        synchronized (connId) {
            db.updateAutoCommit(connId, false);
            Iterator<TimeVariantMetadata> iterator = objs.iterator();
            while (iterator.hasNext()) {
                obj = iterator.next();
                String key = obj.getStaticId();
                if (objMapCopy.get(key) != null)
                    objMapCopy.remove(key);

                obj.updateDbRecord(false);
            }

            Collection<TimeVariantMetadata> remainingObjs = objMapCopy.values();
            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            for (TimeVariantMetadata obj1 : remainingObjs) {
                if (obj1.getToTime() == null) {
                    inactivateRecord(tableName, obj1.getId(), now);
                    obj = obj1;
                }
            }
            db.updateAutoCommit(connId, true);
            if (obj != null) {
                obj.updateMap();
                lastUpate(tableName, now);
            }
        }
    }

    /**
     * @param objs
     * @param objMap
     * @param tableName
     */
    public void updateTimeVariantRecords(ArrayList<TimeVariantMetadata> objs, HashMap<String, TimeVariantMetadata> objMap, String tableName) {
        logger.info("Updating records in table " + tableName);

        TimeVariantMetadata obj = null;

        synchronized (connId) {
            db.updateAutoCommit(connId, false);
            Iterator<TimeVariantMetadata> iterator = objs.iterator();
            while (iterator.hasNext()) {
                obj = iterator.next();
                obj.updateDbRecord(false);
            }

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());

            db.updateAutoCommit(connId, true);
            if (obj != null) {
                obj.updateMap();
                lastUpate(tableName, now);
            }
        }
    }

    /**
     * Updates all records in a time invariant records table for the given tableName
     *
     * @param objs
     * @param objMap
     * @param tableName
     */
    public void insertTimeInvariantRecords(ArrayList<TimeInvariantMetadata> objs, String tableName) {
        logger.info("Updating records in table " + tableName);

        TimeInvariantMetadata obj = null;

        synchronized (connId) {
            db.updateAutoCommit(connId, false);
            Iterator<TimeInvariantMetadata> iterator = objs.iterator();
            while (iterator.hasNext()) {
                obj = iterator.next();
                obj.insertDbRecord(false);
            }

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            db.updateAutoCommit(connId, true);
            if (obj != null) {
                obj.updateMap();
                lastUpate(tableName, now);
            }
        }
    }

    /**
     * @return the connId
     */
    public String getConnId() {
        return connId;
    }

    /**
     * Updates the toTime field of an existing record with the current time, essentially marking it inactive
     * @param org
     */
    public void inactivateRecord(String tableName, int id, Timestamp now) {
        String sql = "UPDATE " + tableName + " SET toTime = '" + now + "' WHERE id = " + id;
        db.update(connId, sql);
    }

    /**
     * inserts a new record in the lastUpdate table with tableName that has last changed
     * @param tableName
     * @param now
     */
    public void lastUpate(String tableName, Timestamp now) {
        String sql = "INSERT INTO meta.lastUpdate (name, updateTime) VALUES ('" + tableName + "', '" + now + "');";
        db.update(connId, sql);
    }
}