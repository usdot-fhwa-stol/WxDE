/************************************************************************
 * Source filename: SourceDao.java
 * <p/>
 * Creation date: Feb 26, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the source table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.Source;
import wde.metadata.TimeVariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class SourceDao {

    private static final Logger logger = Logger.getLogger(SourceDao.class);

    private static final String SELECT_STATICID_QUERY = "SELECT * FROM meta.source WHERE staticId = ? ORDER BY id desc limit 1";

    private static SourceDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> sourceMap = null;

    /**
     * Constructor
     */
    private SourceDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        sourceMap = new HashMap<>();
        updateSourceMap();
    }

    /**
     * @return ImageDao
     */
    public static SourceDao getInstance() {
        if (instance == null)
            instance = new SourceDao();

        return instance;
    }

    /**
     * @return the imageMap
     */
    public HashMap<String, TimeVariantMetadata> getSourceMap() {
        return sourceMap;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    public Source getSource(String staticId) {
        Source source = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                source = new Source();
                source.setId(rs.getInt("id"));
                source.setStaticId(staticId);
                source.setUpdateTime(rs.getTimestamp("updateTime"));
                source.setToTime(rs.getTimestamp("toTime"));
                source.setName(rs.getString("name"));
                source.setDescription(rs.getString("description"));
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

        return source;
    }

    /**
     * Updates the source map based on what's currently in the database
     */
    public void updateSourceMap() {
        String sql = "SELECT DISTINCT(staticId) FROM meta.source";
        ResultSet rs = db.query(connId, sql);
        try {
            ArrayList<String> staticIds = new ArrayList<String>();
            while (rs != null && rs.next()) {
                staticIds.add(String.valueOf(rs.getInt("staticId")));
            }

            for (String staticId : staticIds) {
                Source source = getSource(staticId);
                sourceMap.put(source.getStaticId(), source);
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
     * @param source
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in source needs to be inserted; false if no change is found
     */
    public boolean updateSource(Source source, boolean atomic) {
        String key = source.getStaticId();

        Source dbSource = (Source) sourceMap.get(key);

        if (dbSource == null || !dbSource.equals(source)) {

            if (dbSource == null)
                logger.info("inserting source " + key);
            else
                logger.info("updating source " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());

            String sql = "INSERT INTO meta.source (staticId, updateTime, name, description, qchCharFlagLen, qchFlagLabel)  VALUES ("
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convert(source.getName(), false)
                    + QueryString.convert(source.getDescription(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                source = getSource(key);
                sourceMap.put(key, source);
            } else
                db.update(sharedConnId, sql);

            if (dbSource != null)
                md.inactivateRecord("source", dbSource.getId(), now);

            if (atomic)
                md.lastUpate("source", now);

            return true;
        }

        return false;
    }
}
