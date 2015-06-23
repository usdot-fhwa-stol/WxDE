/************************************************************************
 * Source filename: PlatformDao.java
 * <p/>
 * Creation date: Feb 25, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the platform table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.IPlatform;
import wde.metadata.Platform;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class PlatformDao {

    private static final Logger logger = Logger.getLogger(PlatformDao.class);

    private static final String SELECT_QUERY = "SELECT p.*, s.description "
            + "FROM meta.platform p, meta.site s WHERE p.siteId = s.id ORDER BY p.id";

    private static final String SELECT_ID_QUERY = "SELECT p.*, s.description "
            + "FROM meta.platform p, meta.site s WHERE p.id=? AND p.siteId = s.id";

    private static final String SELECT_STATICID_QUERY = "SELECT p.*, s.description "
            + "FROM meta.platform p, meta.site s WHERE p.staticId = ? AND p.totime is null and p.siteid=s.id";

    private static final String SELECT_CONTRIB_PLATFORMCODE_QUERY = "SELECT p.*, s.description "
            + "FROM meta.platform p, meta.site s WHERE p.contribId=? AND p.platformCode=? AND p.totime is null AND p.siteId=s.id";

    private static PlatformDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> platformUpdateMap = null;

    private HashMap<Integer, TimeVariantMetadata> platform1stQueryMap = null;

    // The inner map below contains only the latest platforms
    private HashMap<Integer, HashMap<String, TimeVariantMetadata>> platform2ndQueryMap = null;

    private ArrayList<IPlatform> platforms = null;

    /**
     * Constructor
     */
    private PlatformDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        platformUpdateMap = new HashMap<>();
        platform1stQueryMap = new HashMap<>();
        platform2ndQueryMap = new HashMap<>();
        platforms = new ArrayList<IPlatform>();
        updatePlatformMap();
    }

    /**
     * @return PlatformDao
     */
    public static PlatformDao getInstance() {
        if (instance == null) instance = new PlatformDao();

        return instance;
    }

    /**
     * @return the platformMap
     */
    public HashMap<String, TimeVariantMetadata> getPlatformUpdateMap() {
        return platformUpdateMap;
    }

    /**
     * @param id
     * @return
     */
    public IPlatform getPlatform(int id) {

        logger.info("getPlatform for id: " + id);

        Platform p = (Platform) platform1stQueryMap.get(id);

        if (p == null) {
            p = getPlatformForId(id);
            if (p != null) {
                synchronized (platform1stQueryMap) {
                    platform1stQueryMap.put(id, p);
                }

                // If the platform is the latest record for the contribId/platformCode
                if (p.getToTime() == null) {
                    int contribId = p.getContribId();
                    HashMap<String, TimeVariantMetadata> map = null;
                    synchronized (platform2ndQueryMap) {
                        map = platform2ndQueryMap.get(contribId);
                        if (map == null) {
                            map = new HashMap<>();
                            platform2ndQueryMap.put(contribId, map);
                        }
                        String platformCode = p.getPlatformCode();
                        if (map.get(platformCode) == null)
                            map.put(platformCode, p);
                    }
                }
            }
        }

        return p;
    }

    /**
     * @param staticId
     * @param platformCode
     * @return
     */
    public IPlatform getPlatformForContribId(int contribId, String platformCode) {
        platformCode = platformCode.trim();
        logger.info("getPlatformForContribId for contribId: " + contribId + " platformCode: " + platformCode);

        Platform p = null;

        HashMap<String, TimeVariantMetadata> map = platform2ndQueryMap.get(contribId);
        if (map != null)
            p = (Platform) map.get(platformCode);

        if (map == null || p == null) { // need to get the record from the database
            p = getPlatformForContrib(contribId, platformCode);
            if (p != null) {
                synchronized (platform2ndQueryMap) {
                    if (map == null) {
                        map = new HashMap<>();
                        platform2ndQueryMap.put(contribId, map);
                    }
                    map.put(platformCode, p);
                }

                int id = p.getId();
                synchronized (platform1stQueryMap) {
                    if (platform1stQueryMap.get(id) == null)
                        platform1stQueryMap.put(id, p);
                }
            }
        }

        return p;
    }

    /**
     * @param pList
     */
    public ArrayList<IPlatform> getActivePlatforms() {

        logger.info("getActivePlatforms");

        platforms.clear();
        synchronized (platform1stQueryMap) {
            for (TimeVariantMetadata p : platform1stQueryMap.values())
                if (p.getToTime() == null)
                    platforms.add((IPlatform) p);
        }

        return platforms;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public Platform getPlatformForStaticId(String staticId) {

        logger.info("getPlatformForStaticId for staticId: " + staticId);

        Platform platform = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next())
                platform = populatePlatform(rs);

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

        return platform;
    }

    /**
     * Updates the platform map based on what's currently in the database
     */
    public void updatePlatformMap() {

        logger.info("Updating platform maps");

        PreparedStatement ps = db.prepareStatement(connId, SELECT_QUERY);
        ResultSet rs = null;

        try {
            rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                Platform platform = populatePlatform(rs);

                if (platform.getToTime() == null)
                    platformUpdateMap.put(platform.getStaticId(), platform);

                int pId = platform.getId();
                synchronized (platform1stQueryMap) {
                    if (platform1stQueryMap.get(pId) == null)
                        platform1stQueryMap.put(pId, platform);
                }

                if (platform.getToTime() == null) {
                    Integer contribId = Integer.valueOf(platform.getContribId());
                    synchronized (platform2ndQueryMap) {
                        HashMap<String, TimeVariantMetadata> map = platform2ndQueryMap.get(contribId);
                        if (map == null)
                            map = new HashMap<>();

                        map.put(platform.getPlatformCode(), platform);
                        platform2ndQueryMap.put(contribId, map);
                    }
                }
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

        logger.info("Finished updating platform maps");
    }

    /**
     * @param platform
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in platform needs to be inserted; false if no change is found
     */
    public boolean updatePlatform(Platform platform, boolean atomic) {
        String key = platform.getStaticId();

        Platform dbPlatform = (Platform) platformUpdateMap.get(key);

        if (dbPlatform == null || !dbPlatform.equals(platform)) {

            if (dbPlatform == null)
                logger.info("inserting platform " + key);
            else
                logger.info("updating platform " + key);

            Timestamp now = new Timestamp(Calendar.getInstance()
                    .getTimeInMillis());
            String sql = "INSERT INTO meta.platform (staticId, updateTime, platformCode, category, description, type, contribId, siteId, "
                    + "locBaseLat, locBaseLong, locBaseElev, locBaseDatum, powerType, doorOpen, batteryStatus, lineVolts, maintContactId"
                    + ")  VALUES ("
                    + key
                    + ","
                    + "'"
                    + now
                    + "',"
                    + QueryString.convert(platform.getPlatformCode(), false)
                    + QueryString.convert(platform.getCategory(), false)
                    + QueryString.convert(platform.getDescription(), false)
                    + QueryString.convert(platform.getType(), false)
                    + QueryString.convertId(platform.getContribId(), false)
                    + QueryString.convertId(platform.getSiteId(), false)
                    + QueryString.convert(platform.getLocBaseLat(), false)
                    + QueryString.convert(platform.getLocBaseLong(), false)
                    + QueryString.convert(platform.getLocBaseElev(), false)
                    + QueryString.convert(platform.getLocBaseDatum(), false)
                    + QueryString.convert(platform.getPowerType(), false)
                    + platform.isDoorOpen()
                    + ","
                    + QueryString.convert(platform.getBatteryStatus(), false)
                    + QueryString.convert(platform.getLineVolts(), false)
                    + QueryString.convertId(platform.getMaintContactId(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                platform = getPlatformForStaticId(key);
                platformUpdateMap.put(key, platform);
            } else
                db.update(sharedConnId, sql);

            if (dbPlatform != null)
                md.inactivateRecord("platform", dbPlatform.getId(), now);

            if (atomic) md.lastUpate("platform", now);

            return true;
        }

        return false;
    }

    /**
     * @param id
     * @return
     */
    private Platform getPlatformForId(int id) {

        Platform platform = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_ID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs != null && rs.next())
                platform = populatePlatform(rs);
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

        return platform;
    }

    /**
     * @param contribId
     * @param platformCode
     * @return
     */
    private Platform getPlatformForContrib(int contribId, String platformCode) {

        Platform platform = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_CONTRIB_PLATFORMCODE_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, contribId);
            ps.setString(2, platformCode);
            rs = ps.executeQuery();

            if (rs != null && rs.next())
                platform = populatePlatform(rs);

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

        return platform;
    }

    private Platform populatePlatform(ResultSet rs) {
        Platform platform = new Platform();
        try {
            platform.setId(rs.getInt("id"));
            platform.setStaticId(String.valueOf(rs.getInt("staticId")));
            platform.setUpdateTime(rs.getTimestamp("updateTime"));
            platform.setToTime(rs.getTimestamp("toTime"));
            platform.setPlatformCode(rs.getString("platformCode"));
            String charValueStr = rs.getString("category");
            if (charValueStr.length() > 0)
                platform.setCategory(charValueStr.charAt(0));
            platform.setDescription(rs.getString("description"));
            platform.setType(rs.getInt("type"));
            platform.setContribId(rs.getInt("contribId"));
            platform.setSiteId(rs.getInt("siteId"));
            platform.setLocBaseLat(rs.getDouble("locBaseLat"));
            platform.setLocBaseLong(rs.getDouble("locBaseLong"));
            platform.setLocBaseElev(rs.getDouble("locBaseElev"));
            platform.setLocBaseDatum(rs.getString("locBaseDatum"));
            charValueStr = rs.getString("powerType");
            if (charValueStr != null && charValueStr.length() > 0)
                platform.setPowerType(charValueStr.charAt(0));
            platform.setDoorOpen(rs.getBoolean("doorOpen"));
            platform.setBatteryStatus(rs.getInt("batteryStatus"));
            platform.setLineVolts(rs.getInt("lineVolts"));
            platform.setMaintContactId(rs.getInt("maintContactId"));
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
        return platform;
    }
}
