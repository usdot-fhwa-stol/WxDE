/************************************************************************
 * Source filename: SiteDao.java
 * <p/>
 * Creation date: Feb 23, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the site table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Site;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SiteDao {

    private static final Logger logger = Logger.getLogger(SiteDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.site WHERE staticId = ? ORDER BY id desc limit 1";

    private static SiteDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> siteMap = null;

    /**
     * Constructor
     */
    private SiteDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        siteMap = new HashMap<>();
        updateSiteMap();
    }

    /**
     * @return SiteDao
     */
    public static SiteDao getInstance() {
        if (instance == null)
            instance = new SiteDao();

        return instance;
    }

    /**
     * @return the siteMap
     */
    public HashMap<String, TimeVariantMetadata> getSiteMap() {
        return siteMap;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public Site getSite(String staticId) {
        Site site = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                site = new Site();
                site.setId(rs.getInt("id"));
                site.setStaticId(staticId);
                site.setUpdateTime(rs.getTimestamp("updateTime"));
                site.setToTime(rs.getTimestamp("toTime"));
                site.setStateSiteId(rs.getString("stateSiteId"));
                site.setContribId(rs.getInt("contribId"));
                site.setDescription(rs.getString("description"));
                site.setRoadwayDesc(rs.getString("roadwayDesc"));
                site.setRoadwayMilepost(rs.getInt("roadwayMilepost"));
                site.setRoadwayOffset(rs.getFloat("roadwayOffset"));
                site.setRoadwayHeight(rs.getFloat("roadwayHeight"));
                site.setCounty(rs.getString("county"));
                site.setState(rs.getString("state"));
                site.setCountry(rs.getString("country"));
                site.setAccessDirections(rs.getString("accessDirections"));
                site.setObstructions(rs.getString("obstructions"));
                site.setLandscape(rs.getString("landscape"));
                site.setState(rs.getString("state"));
                site.setStateSystemId(rs.getString("stateSystemId"));
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

        return site;
    }

    /**
     * Updates the contact map based on what's currently in the database
     */
    public void updateSiteMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.site";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                Site site = getSite(staticId);
                siteMap.put(site.getStaticId(), site);
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
     * @param site
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in site needs to be inserted; false if no change is found
     */
    public boolean updateSite(Site site, boolean atomic) {
        String key = site.getStaticId();

        Site dbSite = (Site) siteMap.get(key);

        if (dbSite == null || !dbSite.equals(site)) {

            if (dbSite == null)
                logger.info("inserting site " + key);
            else
                logger.info("updating site " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.site (staticId, updateTime, stateSiteId, contribId, description, roadwayDesc, roadwayMilepost, roadwayOffset, "
                    + "roadwayHeight, county, state, country, accessDirections, obstructions, landscape, stateSystemId)  VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convert(site.getStateSiteId(), false)
                    + QueryString.convert(site.getContribId(), false)
                    + QueryString.convert(site.getDescription(), false)
                    + QueryString.convert(site.getRoadwayDesc(), false)
                    + QueryString.convert(site.getRoadwayMilepost(), false)
                    + QueryString.convert(site.getRoadwayOffset(), false)
                    + QueryString.convert(site.getRoadwayHeight(), false)
                    + QueryString.convert(site.getCounty(), false)
                    + QueryString.convert(site.getState(), false)
                    + QueryString.convert(site.getCountry(), false)
                    + QueryString.convert(site.getAccessDirections(), false)
                    + QueryString.convert(site.getObstructions(), false)
                    + QueryString.convert(site.getLandscape(), false)
                    + QueryString.convert(site.getStateSystemId(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                site = getSite(key);
                siteMap.put(key, site);
            } else
                db.update(sharedConnId, sql);

            if (dbSite != null)
                md.inactivateRecord("site", dbSite.getId(), now);

            if (atomic)
                md.lastUpate("site", now);

            return true;
        }

        return false;
    }
}
