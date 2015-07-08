/************************************************************************
 * Source filename: ContribDao.java
 * <p/>
 * Creation date: Feb 22, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the contrib table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Contrib;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ContribDao {

    private static final Logger logger = Logger.getLogger(ContribDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.contrib WHERE staticId = ? ORDER BY id desc limit 1";

    private static ContribDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> contribMap = null;

    /**
     * Constructor
     */
    private ContribDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        contribMap = new HashMap<>();
        updateContribMap();
    }

    /**
     * @return ContribDao
     */
    public static ContribDao getInstance() {
        if (instance == null)
            instance = new ContribDao();

        return instance;
    }

    /**
     * @return the contribMap
     */
    public HashMap<String, TimeVariantMetadata> getContribMap() {
        return contribMap;
    }

    /**
     * @param staticId
     * @return
     */
    public int getIdFromStaticId(int staticId) {
        String key = Integer.toString(staticId);
        Contrib contrib = (Contrib) contribMap.get(key);

        if (contrib != null)
            return contrib.getId();

        return -1;
    }

    /**
     * Updates the contrib map based on what's currently in the database
     */
    public void updateContribMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.contrib";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                Contrib contrib = getContrib(staticId);
                contribMap.put(contrib.getStaticId(), contrib);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    /**
     * @param contrib
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in contrib needs to be inserted; false if no change is found
     */
    public boolean updateContrib(Contrib contrib, boolean atomic) {
        String key = contrib.getStaticId();

        Contrib dbContrib = (Contrib) contribMap.get(key);

        if (dbContrib == null || !dbContrib.equals(contrib)) {

            if (dbContrib == null)
                logger.info("inserting contrib " + key);
            else
                logger.info("updating contrib " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.contrib (staticId, updateTime, orgId, name, agency, monitorHours, contactId, altContactId, metadataContactId, display, disclaimerLink) VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convertId(contrib.getOrgId(), false)
                    + QueryString.convert(contrib.getName(), false)
                    + QueryString.convert(contrib.getAgency(), false)
                    + contrib.getMonitorHours() + ","
                    + QueryString.convertId(contrib.getContactId(), false)
                    + QueryString.convertId(contrib.getAltContactId(), false)
                    + QueryString.convertId(contrib.getMetadataContactId(), false)
                    + contrib.isDisplay() + ","
                    + QueryString.convert(contrib.getDisclaimerLink(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                contrib = getContrib(key);
                contribMap.put(key, contrib);
            } else
                db.update(sharedConnId, sql);

            if (dbContrib != null)
                md.inactivateRecord("meta.contrib", dbContrib.getId(), now);

            if (atomic)
                md.lastUpate("contrib", now);

            return true;
        }

        return false;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    private Contrib getContrib(String staticId) {

        Contrib contrib = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                contrib = new Contrib();
                contrib.setId(rs.getInt("id"));
                contrib.setStaticId(staticId);
                contrib.setUpdateTime(rs.getTimestamp("updateTime"));
                contrib.setToTime(rs.getTimestamp("toTime"));
                contrib.setOrgId(rs.getInt("orgId"));
                contrib.setName(rs.getString("name"));
                contrib.setAgency(rs.getString("agency"));
                contrib.setMonitorHours(rs.getInt("monitorHours"));
                contrib.setContactId(rs.getInt("contactId"));
                contrib.setAltContactId(rs.getInt("AltContactId"));
                contrib.setMetadataContactId(rs.getInt("metadataContactId"));
                contrib.setDisplay(rs.getBoolean("display"));
                contrib.setDisclaimerLink(rs.getString("disclaimerLink"));
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

        return contrib;
    }
}
