/************************************************************************
 * Source filename: ObsTypeDao.java
 * <p/>
 * Creation date: Feb 25, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the obstype table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.ObsType;
import wde.metadata.TimeInvariantMetadata;
import wde.util.QueryString;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;

public class ObsTypeDao {

    private static final Logger logger = Logger.getLogger(ObsTypeDao.class);

    private static final String SELECT_OBSTYPE_QUERY = "SELECT * FROM meta.obsType WHERE obsType = ?";

    private static ObsTypeDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeInvariantMetadata> obsTypeMap = null;

    private HashMap<String, Integer> obsTypeReverseLookupMap = null;

    /**
     * Constructor
     */
    private ObsTypeDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        obsTypeMap = new HashMap<>();
        obsTypeReverseLookupMap = new HashMap<>();
        updateObsTypeMap();
    }

    /**
     * @return ObsTypeDao
     */
    public static ObsTypeDao getInstance() {
        if (instance == null)
            instance = new ObsTypeDao();

        return instance;
    }

    /**
     * @return the obsTypeMap
     */
    public HashMap<String, TimeInvariantMetadata> getObsTypeMap() {
        return obsTypeMap;
    }

    /**
     * @return a list of ObsTypes that are active
     */
    public ArrayList<ObsType> getObsTypeList() {
        ArrayList<ObsType> obsTypes = new ArrayList<>();
        for (TimeInvariantMetadata ot : obsTypeMap.values()) {
            if (((ObsType) ot).isActive())
                obsTypes.add((ObsType) ot);
        }
        Collections.sort(obsTypes);

        return obsTypes;
    }

    /**
     * @param id
     * @return the record that has the same id
     */
    public ObsType getObsType(int id) {
        return (ObsType) obsTypeMap.get(String.valueOf(id));
    }

    /**
     * @param obsTypeName
     * @return
     */
    public int getObsTypeId(String obsTypeName) {
        return obsTypeReverseLookupMap.get(obsTypeName).intValue();
    }

    /**
     * @param id
     * @return the record that has the same obsTypeName
     */
    public ObsType getObsType(String obsTypeName) {
        ObsType obsType = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_OBSTYPE_QUERY);
        ResultSet rs = null;

        try {
            ps.setString(1, obsTypeName);
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                obsType = new ObsType();
                obsType.setId(String.valueOf(rs.getInt("id")));
                obsType.setObsType(obsTypeName);
                obsType.setObs1204Unit(rs.getString("obs1204Units"));
                obsType.setObsDesc(rs.getString("obsDesc"));
                obsType.setObsInternalUnit(rs.getString("obsInternalUnits"));
                obsType.setActive(rs.getBoolean("active"));
                obsType.setObsEnglishUnit(rs.getString("obsEnglishUnits"));
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

        return obsType;
    }

    /**
     * Updates the obsType map based on what's currently in the database
     */
    public void updateObsTypeMap() {
        String sql = "SELECT id FROM meta.obsType";
        ResultSet rs = db.query(connId, sql);

        try {
            ArrayList<String> ids = new ArrayList<String>();
            while (rs != null && rs.next()) {
                ids.add(String.valueOf(rs.getInt("id")));
            }

            for (String id : ids) {
                ObsType obsType = getObsTypeFromDB(id);
                obsTypeMap.put(id, obsType);
                obsTypeReverseLookupMap.put(obsType.getObsType(), new Integer(id));
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
     * @param obsType
     * @param atomic - false if this is part of a batch processing
     * @return true if a record is inserted; false if database already has the record is found
     */
    public boolean insertObsType(ObsType obsType, boolean atomic) {
        String key = obsType.getId();

        ObsType dbObsType = (ObsType) obsTypeMap.get(key);

        if (dbObsType != null) {
            logger.error("obsType: " + key + " already in the database");
            return false;
        }

        logger.info("inserting obsType " + key);

        Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
        String sql = "INSERT INTO meta.obsType (id, updateTime, obsType, obs1204Units, obsDesc, obsInternalUnits, active, obsEnglishUnits)  VALUES ("
                + key + ","
                + "'" + now + "',"
                + QueryString.convert(obsType.getObsType(), false)
                + QueryString.convert(obsType.getObs1204Unit(), false)
                + QueryString.convert(obsType.getObsDesc(), false)
                + QueryString.convert(obsType.getObsInternalUnit(), false)
                + obsType.isActive() + ","
                + QueryString.convert(obsType.getObsEnglishUnit(), true)
                + ");";

        if (atomic) {
            db.update(connId, sql);

            // Update the id field
            obsType = getObsTypeFromDB(key);
            synchronized (obsTypeMap) {
                obsTypeMap.put(key, obsType);
            }
            md.lastUpate("obsType", now);
        } else
            db.update(sharedConnId, sql);

        return true;
    }

    /**
     * @param id
     * @return the record that has the same id
     */
    private ObsType getObsTypeFromDB(String id) {
        ObsType obsType = null;

        String sql = "SELECT * FROM meta.obsType WHERE id = ?";
        PreparedStatement ps = db.prepareStatement(connId, sql);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(id));
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                obsType = new ObsType();
                obsType.setId(id);
                obsType.setObsType(rs.getString("obsType"));
                obsType.setObs1204Unit(rs.getString("obs1204Units"));
                obsType.setObsDesc(rs.getString("obsDesc"));
                obsType.setObsInternalUnit(rs.getString("obsInternalUnits"));
                obsType.setActive(rs.getBoolean("active"));
                obsType.setObsEnglishUnit(rs.getString("obsEnglishUnits"));
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

        return obsType;
    }
}
