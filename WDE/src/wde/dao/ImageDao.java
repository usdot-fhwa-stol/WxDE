/************************************************************************
 * Source filename: ImageDao.java
 * <p/>
 * Creation date: Feb 24, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the image table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Image;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ImageDao {

    private static final Logger logger = Logger.getLogger(ImageDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.image WHERE staticId = ? ORDER BY id desc limit 1";

    private static ImageDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> imageMap = null;

    /**
     * Constructor
     */
    private ImageDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        imageMap = new HashMap<>();
        updateImageMap();
    }

    /**
     * @return ImageDao
     */
    public static ImageDao getInstance() {
        if (instance == null)
            instance = new ImageDao();

        return instance;
    }

    /**
     * @return the imageMap
     */
    public HashMap<String, TimeVariantMetadata> getImageMap() {
        return imageMap;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public Image getImage(String staticId) {
        Image image = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                image = new Image();
                image.setId(rs.getInt("id"));
                image.setStaticId(staticId);
                image.setUpdateTime(rs.getTimestamp("updateTime"));
                image.setToTime(rs.getTimestamp("toTime"));
                image.setSiteId(rs.getInt("siteId"));
                image.setDescription(rs.getString("description"));
                image.setLinkURL(rs.getString("linkURL"));
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

        return image;
    }

    /**
     * Updates the image map based on what's currently in the database
     */
    public void updateImageMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.image";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                Image image = getImage(staticId);
                imageMap.put(image.getStaticId(), image);
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
     * @param image
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in image needs to be inserted; false if no change is found
     */
    public boolean updateImage(Image image, boolean atomic) {
        String key = image.getStaticId();

        Image dbImage = (Image) imageMap.get(key);

        if (dbImage == null || !dbImage.equals(image)) {

            if (dbImage == null)
                logger.info("inserting image " + key);
            else
                logger.info("updating image " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.image (staticId, updateTime, siteId, description, linkURL)  VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convertId(image.getSiteId(), false)
                    + QueryString.convert(image.getDescription(), false)
                    + QueryString.convert(image.getLinkURL(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                image = getImage(key);
                imageMap.put(key, image);
            } else
                db.update(sharedConnId, sql);

            if (dbImage != null)
                md.inactivateRecord("meta.image", dbImage.getId(), now);

            if (atomic)
                md.lastUpate("image", now);

            return true;
        }

        return false;
    }
}
