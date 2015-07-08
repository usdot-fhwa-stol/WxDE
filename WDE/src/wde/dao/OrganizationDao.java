/************************************************************************
 * Source filename: OrganizationDao.java
 * <p/>
 * Creation date: Feb 21, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the organization table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Organization;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class OrganizationDao {

    private static final Logger logger = Logger.getLogger(OrganizationDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.organization WHERE staticId = ? ORDER BY id desc limit 1";

    private static OrganizationDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> orgMap = null;

    /**
     * Constructor
     */
    private OrganizationDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        orgMap = new HashMap<>();
        updateOrgMap();
    }

    /**
     * @return OrganizationDao
     */
    public static OrganizationDao getInstance() {
        if (instance == null)
            instance = new OrganizationDao();

        return instance;
    }

    /**
     * @return the orgMap
     */
    public HashMap<String, TimeVariantMetadata> getOrgMap() {
        return orgMap;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public Organization getOrg(String staticId) {
        Organization org = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                org = new Organization();
                org.setId(rs.getInt("id"));
                org.setStaticId(staticId);
                org.setUpdateTime(rs.getTimestamp("updateTime"));
                org.setToTime(rs.getTimestamp("toTime"));
                org.setName(rs.getString("name"));
                org.setLocation(rs.getString("location"));
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

        return org;
    }

    /**
     * Updates the org map based on what's currently in the database
     */
    public void updateOrgMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.organization";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                Organization org = getOrg(staticId);
                orgMap.put(org.getStaticId(), org);
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
     * @param org
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in organization needs to be inserted; false if no change is found
     */
    public boolean updateOrganization(Organization org, boolean atomic) {
        String key = org.getStaticId();

        Organization dbOrg = (Organization) orgMap.get(key);

        if (dbOrg == null || !dbOrg.equals(org)) {

            if (dbOrg == null)
                logger.info("inserting organization " + key);
            else
                logger.info("updating organization " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.organization (staticId, updateTime, name, location, purpose, centerId, centerName)  VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convert(org.getName(), false)
                    + QueryString.convert(org.getLocation(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                org = getOrg(key);
                orgMap.put(key, org);
            } else
                db.update(sharedConnId, sql);

            if (dbOrg != null)
                md.inactivateRecord("organization", dbOrg.getId(), now);

            if (atomic)
                md.lastUpate("organization", now);

            return true;
        }

        return false;
    }
}
