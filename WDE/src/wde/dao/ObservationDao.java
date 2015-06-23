/************************************************************************
 * Source filename: ObservationDao.java
 * <p/>
 * Creation date: Feb 26, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the obs table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.QualityFlag;
import wde.obs.ObsValue;
import wde.obs.Observation;
import wde.util.DatabaseArrayParser;
import wde.util.DateRange;
import wde.util.QueryString;
import wde.util.coord.CoordinateConversion;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;

public class ObservationDao {

    private static final Logger logger = Logger.getLogger(ObservationDao.class);

    private static final String SELECT_OBSTYPE_SENSOR_OBSTIME_QUERY = "SELECT * FROM obs.obs WHERE obsTypeId = ? AND sensorId = ? AND obsTime = ?";

    private static final String INSERT_QUERY = "INSERT INTO obs.obs (obsTypeId, sourceId, sensorId, obsTime, recvTime, "
            + "latitude, longitude, elevation, value, confValue, qchCharFlag) "
            + "VALUES (?,?,?,?,?,?,?,?,?,?,?::character[])";

    private static final String INSERT_INVALIDOBS_QUERY = "INSERT INTO obs.invalidObs "
            + "(sourceId, contribId, platformCode, obsTypeId, sensorIndex, obsTime, recvTime, latitude, longitude, value, qchcharflag) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::character[])";

    private static ObservationDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private QualityFlagDao qfd = null;

    private int insertCounter;

    private int updateCounter;

    private DatabaseArrayParser dap = null;

    private PreparedStatement batchInsertStatement = null;

    private HashMap<String, String> replaceMap = new HashMap<>();

    private HashMap<String, String> sensorIndexMap = new HashMap<>();

    private HashMap<String, String> totimeList = new HashMap<>();

    private ArrayList<String> removeList = new ArrayList<>();

    /**
     * Constructor
     */
    private ObservationDao() {
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = db.getConnection();
        qfd = QualityFlagDao.getInstance();
        dap = new DatabaseArrayParser();
        batchInsertStatement = db.prepareStatement(sharedConnId, INSERT_QUERY);
    }

    /**
     * @return ImageDao
     */
    public static ObservationDao getInstance() {
        if (instance == null)
            instance = new ObservationDao();

        return instance;
    }

    public static Observation retrieveObs(ResultSet rs)
            throws SQLException {
        Observation obs = new Observation();
        obs.setObsTypeId(rs.getInt("obsTypeId"));
        obs.setSourceId(rs.getInt("sourceId"));
        obs.setSensorId(rs.getInt("sensorId"));
        obs.setObsTime(rs.getTimestamp("obsTime"));
        obs.setRecvTime(rs.getTimestamp("recvTime"));
        obs.setLatitude(rs.getInt("latitude"));
        obs.setLongitude(rs.getInt("longitude"));
        obs.setElevation(rs.getInt("elevation"));
        obs.setValue(rs.getDouble("value"));
        obs.setConfValue(rs.getFloat("confValue"));
        Array charArr = rs.getArray("qchCharFlag");
        if (charArr != null) {
            String[] strArray = (String[]) charArr.getArray();
            char[] charArray = new char[strArray.length];
            for (int i = 0; i < strArray.length; i++)
                charArray[i] = strArray[i].charAt(0);
            obs.setQchCharFlag(charArray);
        }

        return obs;
    }

    /**
     * @param obsTypeId
     * @param sensorId
     * @param obsTime
     * @return observation object from the database
     */
    public ArrayList<Observation> getObservations(int obsTypeId, int sensorId, Timestamp obsTime) {

        ArrayList<Observation> observations = new ArrayList<Observation>();

        PreparedStatement ps = db.prepareStatement(connId, SELECT_OBSTYPE_SENSOR_OBSTIME_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, obsTypeId);
            ps.setInt(2, sensorId);
            ps.setTimestamp(3, obsTime);
            rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                observations.add(retrieveObs(rs));
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

        return observations;
    }

    /**
     * @param obs
     * @param pStatement - use null for atomic insert; use batchInsertStatement for batch insert
     */
    public void insertObservation(Observation obs, PreparedStatement pStatement) {
        String qchCharFlagStr = null;

        char[] charArr = obs.getQchCharFlag();
        if (charArr != null) {
            qchCharFlagStr = "";
            for (char aChar : charArr)
                qchCharFlagStr += "\"" + aChar + "\",";
            qchCharFlagStr = "{" + qchCharFlagStr.substring(0, qchCharFlagStr.length() - 1) + "}";
        }

        if (pStatement == null)
            pStatement = db.prepareStatement(connId, INSERT_QUERY);

        try {
            pStatement.setInt(1, obs.getObsTypeId());
            pStatement.setInt(2, obs.getSourceId());
            pStatement.setInt(3, obs.getSensorId());
            pStatement.setTimestamp(4, obs.getObsTime());
            pStatement.setTimestamp(5, obs.getRecvTime());
            pStatement.setInt(6, obs.getLatitude());
            pStatement.setInt(7, obs.getLongitude());
            pStatement.setInt(8, obs.getElevation());
            pStatement.setDouble(9, obs.getValue());
            pStatement.setFloat(10, obs.getConfValue());
            pStatement.setString(11, qchCharFlagStr);

            pStatement.executeUpdate();

            insertCounter++;
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    /**
     * @param obs
     * @param pStatement - use null for atomic insert; use batchInsertStatement for batch insert
     */
    public void insertInvalidObservation(
            int sourceId, int contribId, String platformCode, int obsTypeId, int sensorIndex,
            Timestamp obsTime, Timestamp recvTime, int latitude, int longitude, double value, String qchCharFlagStr) {
        logger.debug("inserting invalid observation for sourceId: " + sourceId + " contribId: " + contribId
                + " platformCode: " + platformCode + " obsTypeId: " + obsTypeId
                + " sensorIndex: " + sensorIndex);

        PreparedStatement ps = db.prepareStatement(connId, INSERT_INVALIDOBS_QUERY);

        try {
            ps.setInt(1, sourceId);
            ps.setInt(2, contribId);
            ps.setString(3, platformCode);
            ps.setInt(4, obsTypeId);
            ps.setInt(5, sensorIndex);
            ps.setTimestamp(6, obsTime);
            ps.setTimestamp(7, recvTime);
            ps.setInt(8, latitude);
            ps.setInt(9, longitude);
            ps.setDouble(10, value);
            ps.setString(11, qchCharFlagStr);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            try {
                ps.close();
                ps = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }

    /**
     * @param observations
     * @return
     */
    public RecordCounter insertObservations(Collection<Observation> observations) {
        insertCounter = 0;
        updateCounter = 0;
        synchronized (sharedConnId) {
            db.updateAutoCommit(sharedConnId, false);
            for (Observation obs : observations)
                insertObservation(obs, batchInsertStatement);

            db.updateAutoCommit(sharedConnId, true);
        }
        logger.info("Inserted " + insertCounter + " records");
        logger.info("Updated " + updateCounter + " records");

        RecordCounter rc = new RecordCounter();
        rc.setInsertCounter(insertCounter);
        rc.setUpdateCounter(updateCounter);

        return rc;
    }

    /**
     * Fix sensor ids that have been mis-allocated
     *
     * @param dateStr
     */
    public void fixSensorIds(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "UPDATE " + table + " o set sensorid=s.id1 from meta.sensor_mapping s where o.sensorid=s.id2 and o.obstime < s.time";
            int rows = db.update(connId, sql);
            logger.info("Updated " + rows + " sensor ids backward in " + table);

            sql = "UPDATE " + table + " o set sensorid=s.id2 from meta.sensor_mapping s where o.sensorid = s.id1 and o.obstime > s.time";
            rows = db.update(connId, sql);
            logger.info("Updated " + rows + " sensor ids forward in " + table);
        }
    }

    /**
     * Remove older versions of observation duplicates
     *
     * @param dateStr
     */
    public void detectDuplicates(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "select sourceid, obstypeid, sensorid, obstime, count(*) from " + table
                    + "group by sourceid, obstypeid, sensorid, obstime having (count(*)>1);";
            ResultSet rs = db.query(connId, sql);
            try {
                if (rs != null && rs.next())
                    System.out.println(" Table " + table + " has duplicates");
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
    }

    public void findIncompatipleSensors(String dateStr, HashMap<String, String> sensorMap, HashMap<String, String> dateMap) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "select distinct(s.platformid, s.id, s.obstypeid, o.obstypeid) from " + table + " o, meta.sensor s where o.sensorid=s.id and o.obstypeid <> s.obstypeid";
            ResultSet rs = db.query(connId, sql);

            String sql2 = "select distinct(s1.platformid, s1.id, s1.obstypeid, s2.obstypeid, s2.id) from " + table + " o, meta.sensor s1, meta.sensor s2 where o.sensorid=s1.id and s1.platformid=s2.platformid and o.obstypeid<>s1.obstypeid and o.obstypeid=s2.obstypeid";
            ResultSet rs2 = db.query(connId, sql2);

            try {
                while (rs != null && rs.next()) {
                    String str = rs.getString(1);
                    str = str.substring(1, str.length() - 1);
                    System.out.println(str);
                    if (sensorMap.get(str) == null) {
                        sensorMap.put(str, "");
                        dateMap.put(str, dateStr);
                    }
                }

                while (rs2 != null && rs2.next()) {
                    String str = rs2.getString(1);
                    str = str.substring(1, str.length() - 1);
                    int ind = str.lastIndexOf(',');
                    String key = str.substring(0, ind);
                    String oldValue = sensorMap.get(key);
                    String value = str.substring(ind + 1, str.length());
                    if (oldValue != null && oldValue.length() != 0) {
                        if (!oldValue.equals(value) && oldValue.indexOf(value) == -1) {
                            value = oldValue + " " + value;
                            sensorMap.put(key, value);
                        }
                    } else
                        sensorMap.put(key, value);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            } finally {
                try {
                    rs.close();
                    rs = null;
                    rs2.close();
                    rs2 = null;
                } catch (SQLException se) {
                    // ignore
                }
            }
        }
    }

    public void fixSensorReferences(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "UPDATE " + table + " o set sensorid=sensorid-12 from meta.sensor s where o.sensorid=s.id and o.obstypeid<>s.obstypeid and s.id >= 406982 and s.id <= 407017";
            int rows = db.update(connId, sql);
            sql = "UPDATE " + table + " o set sensorid=sensorid-24 from meta.sensor s where o.sensorid=s.id and o.obstypeid<>s.obstypeid and s.id >= 407030 and s.id <= 408643";
            rows += db.update(connId, sql);
            logger.info("Updated " + rows + " sensor ids backward in " + table);
        }
    }

    public void fixDupSensors() {
        HashMap<String, ArrayList<SensorInfo>> sensorDupMap = new HashMap<>();
        SensorDao sd = SensorDao.getInstance();

        String sql = "select * from meta.dupDetail order by platformid, obstypeid, sensorindex, id";

        ResultSet rs = db.query(connId, sql);
        try {
            while (rs != null && rs.next()) {
                int platformId = rs.getInt("platformid");
                int obstypeId = rs.getInt("obstypeid");
                int sensorIndex = rs.getInt("sensorindex");
                double elevOffset = rs.getDouble("elevoffset");
                int qchparmid = rs.getInt("qchparmid");
                int id = rs.getInt("id");

                String key = platformId + "," + obstypeId + "," + sensorIndex;
                ArrayList<SensorInfo> sensorList = sensorDupMap.get(key);
                if (sensorList == null) {
                    sensorList = new ArrayList<>();
                    sensorDupMap.put(key, sensorList);
                }

                SensorInfo si = new SensorInfo();
                si.elevOffset = elevOffset;
                si.qchparmid = qchparmid;
                si.id = id;

                sensorList.add(si);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
                rs = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        for (String key : sensorDupMap.keySet()) {
            int sensorIndex = Integer.parseInt(key.substring(key.lastIndexOf(',') + 1));
            ArrayList<SensorInfo> sensorList = sensorDupMap.get(key);
            int size = sensorList.size();
            SensorInfo[] si = new SensorInfo[size];
            for (int i = 0; i < size; i++)
                si[i] = sensorList.get(i);
            switch (size) {
                case 2:
                    // Check if the elevOffset or qchparmid is different
                    if (si[0].elevOffset == si[1].elevOffset && si[0].qchparmid == si[1].qchparmid) {
                        replaceMap.put(String.valueOf(si[1].id), String.valueOf(si[0].id));
                        totimeList.put(String.valueOf(si[1].id), "2014-10-16");
                        removeList.add(String.valueOf(si[1].id));
                    } else {
                        totimeList.put(String.valueOf(si[0].id), "2014-10-16");
                    }
                    break;
                case 3:
                    if (si[1].elevOffset == si[2].elevOffset && si[1].qchparmid == si[2].qchparmid) {

                        if (si[0].elevOffset == si[1].elevOffset && si[0].qchparmid == si[1].qchparmid) {
                            // map the 2nd and 3rd to the 1st
                            replaceMap.put(String.valueOf(si[2].id), String.valueOf(si[0].id));
                            totimeList.put(String.valueOf(si[2].id), "2014-10-16");
                            removeList.add(String.valueOf(si[2].id));
                            replaceMap.put(String.valueOf(si[1].id), String.valueOf(si[0].id));
                            totimeList.put(String.valueOf(si[1].id), "2014-10-16");
                            removeList.add(String.valueOf(si[1].id));
                        } else {
                            // map the 3rd to 2nd and totime flag the 1st
                            replaceMap.put(String.valueOf(si[2].id), String.valueOf(si[1].id));
                            totimeList.put(String.valueOf(si[2].id), "2014-10-16");
                            removeList.add(String.valueOf(si[2].id));
                            totimeList.put(String.valueOf(si[0].id), "2014-10-16");
                        }
                    } else {
                        // totime flag the 1st and 2nd
                        totimeList.put(String.valueOf(si[0].id), "2014-10-16");
                        totimeList.put(String.valueOf(si[1].id), "2014-10-16");
                    }
                    break;
                case 4:
                    // Check if the elevOffset or qchparmid is different
                    if (si[0].elevOffset == si[2].elevOffset && si[0].qchparmid == si[2].qchparmid) {
                        replaceMap.put(String.valueOf(si[2].id), String.valueOf(si[0].id));
                        totimeList.put(String.valueOf(si[2].id), "2014-10-16");
                        removeList.add(String.valueOf(si[2].id));
                    } else {
                        totimeList.put(String.valueOf(si[0].id), "2014-10-16");
                    }
                    if (si[1].elevOffset == si[3].elevOffset && si[1].qchparmid == si[3].qchparmid) {
                        replaceMap.put(String.valueOf(si[3].id), String.valueOf(si[1].id));
                        totimeList.put(String.valueOf(si[3].id), "2014-10-16");
                        removeList.add(String.valueOf(si[3].id));
                        sensorIndexMap.put(String.valueOf(si[1].id), String.valueOf(sensorIndex + 1));
                    } else {
                        sensorIndexMap.put(String.valueOf(si[1].id), String.valueOf(sensorIndex + 1));
                        sensorIndexMap.put(String.valueOf(si[3].id), String.valueOf(sensorIndex + 1));
                        totimeList.put(String.valueOf(si[1].id), "2014-10-16");
                    }

                    break;
                case 5:
                    totimeList.put(String.valueOf(si[0].id), sd.getSensor(si[1].id).getUpdateTime().toString());
                    // Check if the elevOffset or qchparmid is different
                    if (si[1].elevOffset == si[3].elevOffset && si[1].qchparmid == si[3].qchparmid) {
                        replaceMap.put(String.valueOf(si[3].id), String.valueOf(si[1].id));
                        totimeList.put(String.valueOf(si[3].id), "2014-10-16");
                        removeList.add(String.valueOf(si[3].id));
                    } else {
                        totimeList.put(String.valueOf(si[1].id), sd.getSensor(si[3].id).getUpdateTime().toString());
                    }
                    if (si[2].elevOffset == si[4].elevOffset && si[2].qchparmid == si[4].qchparmid) {
                        replaceMap.put(String.valueOf(si[4].id), String.valueOf(si[2].id));
                        totimeList.put(String.valueOf(si[4].id), "2014-10-16");
                        removeList.add(String.valueOf(si[4].id));
                        sensorIndexMap.put(String.valueOf(si[2].id), String.valueOf(sensorIndex + 1));
                    } else {
                        sensorIndexMap.put(String.valueOf(si[2].id), String.valueOf(sensorIndex + 1));
                        sensorIndexMap.put(String.valueOf(si[4].id), String.valueOf(sensorIndex + 1));
                        totimeList.put(String.valueOf(si[2].id), "2014-10-16");
                    }
                    break;
                default:
                    System.out.println("Encounted unexpected number of dups for " + key);
                    break;
            }
        }

        // apply changes from totimeList
        for (String key : totimeList.keySet()) {
            sql = "UPDATE meta.sensor set totime='" + totimeList.get(key) + "' where id=" + key;
            db.update(connId, sql);
            logger.info("Updated totime for sensor " + key);
        }

        // apply changes from the sensorIndexMap
        for (String key : sensorIndexMap.keySet()) {
            sql = "UPDATE meta.sensor set sensorIndex=" + sensorIndexMap.get(key) + " where id=" + key;
            db.update(connId, sql);
            logger.info("Updated sensorIndex for sensor " + key);
        }

        // dump the remove list
        try {
            BufferedWriter file = new BufferedWriter(new FileWriter("./removeList.txt"));
            for (String sensorId : removeList) {
                file.write(sensorId + ",");
            }

            file.write("\r\n");
            file.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        // dump the replaceMap
        try {
            BufferedWriter file = new BufferedWriter(new FileWriter("./replaceMap.csv"));
            for (String key : replaceMap.keySet()) {
                file.write(key + "," + replaceMap.get(key));
                file.write("\r\n");
            }
            file.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void fixObsReferences(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";
            String sql = "update " + table + " set sensorid=srm.sid2 from meta.sensorReplaceMap srm where sensorid=srm.sid1";
            int rows = db.update(connId, sql);
            logger.info("Fixed " + rows + " rows in " + table);
        }
    }

    public void dumpBelleIsleData(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";
            String sql = "insert into obs.simdata (select * from " + table + " where latitude > 42332217 and latitude < 42342876 and longitude > -83003082 and longitude < -82977333)";
            int rows = db.update(connId, sql);
            logger.info("Inserted " + rows + " rows of simulated data from " + table);
        }
    }

    public void removeBelleIsleData(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";
            String sql = "delete from " + table + " where latitude > 42332217 and latitude < 42342876 and longitude > -83003082 and longitude < -82977333";
            int rows = db.update(connId, sql);
            logger.info("Removed " + rows + " rows of simulated data from " + table);
        }
    }

    public void findPlatformMissingSensors(String dateStr, ArrayList<String> platformList) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "select distinct(p.id, o.obstypeid) as platform from " + table +
                    " o, meta.sensor s, meta.platform p where o.sensorid=s.id and s.platformid=p.id and o.obstypeid <> s.obstypeid" +
                    " and o.obstypeid not in (select distinct(s.obstypeid) from meta.sensor s where s.platformid=p.id)";
            ResultSet rs = db.query(connId, sql);

            try {
                while (rs != null && rs.next()) {
                    String platform = rs.getString("platform");
                    if (!platformList.contains(platform))
                        platformList.add(platform);
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
    }

    /**
     * Remove older versions of observation duplicates
     *
     * @param dateStr
     */
    public void removeDuplicates(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "ALTER TABLE " + table + " ADD COLUMN id integer";
            db.update(connId, sql);
            logger.info("Added column id to " + table);

            sql = "UPDATE " + table + " SET id = nextval('obs.obs_id_seq')";
            db.update(connId, sql);
            logger.info("Updated id values in " + table);

            sql = "DELETE from " + table + " o1 USING " + table + " o2 WHERE "
                    + "o1.sourceId = o2.sourceId AND "
                    + "o1.obsTypeId = o2.obsTypeId AND "
                    + "o1.sensorId = o2.sensorId AND "
                    + "o1.obsTime = o2.obsTime AND "
                    + "o1.id > o2.id";
            db.update(connId, sql);
            logger.info("Removed duplicates in " + table);

            sql = "ALTER TABLE " + table + " DROP COLUMN id";
            db.update(connId, sql);
            logger.info("Dropped column id in " + table);
        }
    }

    /**
     * Vacuum the obs table for the given date to free up space
     *
     * @param dateStr
     */
    public void vacuumTable(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "VACUUM full " + table;
            db.update(connId, sql);
            logger.info("Vacuumed " + table);
        }
    }

    public void dropTable(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "drop table " + table;
            db.update(connId, sql);
            logger.info("Dropped " + table);
        }
    }

    public void fixObs(String dateStr) {
        if (tableExists(dateStr)) {
            String table = "obs.\"obs_" + dateStr + "\"";

            String sql = "update " + table + " o set latitude=p.locbaselat*1000000, longitude=p.locbaselong*1000000, elevation=p.locbaseelev from meta.sensor s, meta.platform p where o.sensorid=s.id and s.platformid=p.id and s.contribid=48 and latitude=0 and longitude=0";
            int rows = db.update(connId, sql);
            logger.info("updated " + rows + " rows in " + table);
        }
    }

    public void checkElevation(String dateStr) {
        String table = "obs.\"obs_" + dateStr + "\"";
        String sql = "select count(*) from " + table + " where elevation > 0";

        ResultSet rs = null;

        try {
            rs = db.query(connId, sql);

            if (rs != null && rs.next()) {
                logger.info(table + " contains " + rs.getInt("count") + " records with non-zero elevation");
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

    public void fixElevationValue(String dateStr) {
        String table = "obs.\"obs_" + dateStr + "\"";
        String sql = "update " + table + " set elevation = elevation / 1000000";
        db.update(connId, sql);
        logger.info("Fixed elevation values in " + table);
    }

    /**
     * @param dateStr1
     * @param dateStr2
     * @param obsTypeIds
     */
    public void archiveObservations(String dateStr1, String dateStr2, Set<String> obsTypeIds) {
        ArrayList<Observation> observations = null;
        HashMap<String, ArrayList<Observation>> obsMap = new HashMap<>();
        Observation obs = null;
        String otgp = null;

        DateRange dr = new DateRange();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date bd = null;
        Date ed = null;

        try {
            bd = sdf.parse(dateStr1);
            ed = sdf.parse(dateStr2);
        } catch (ParseException pe) {
            pe.printStackTrace();
            return;
        }

        Timestamp beginDate = new Timestamp(bd.getTime());
        Timestamp endDate = new Timestamp(ed.getTime());
        dr.setBeginDate(beginDate);
        dr.setEndDate(endDate);

        for (String obsTypeId : obsTypeIds) {
            String obsTable = "obs.\"obs_" + dateStr1 + "\"";

            String sql = "WITH rows_to_move as ("
                    + "DELETE from "
                    + obsTable + " WHERE "
                    + "obsTypeId = " + obsTypeId + " RETURNING *) "
                    + "SELECT * from rows_to_move";

            ResultSet rs = db.query(connId, sql);

            logger.info("processing rows of obsTypeId: " + obsTypeId);

            try {
                int counter = 0;
                while (rs != null && rs.next()) {
                    obs = retrieveObs(rs);
                    String gridId = CoordinateConversion.latLon2UTM(obs.getLatitude(), obs.getLongitude(), true);
                    otgp = gridId + "-" + obsTypeId;
                    observations = obsMap.get(otgp);
                    if (observations == null) {
                        logger.info("create observations for obsTypeId: " + obsTypeId + " and gridId: " + gridId);
                        observations = new ArrayList<Observation>();
                        obsMap.put(otgp, observations);
                    }
                    observations.add(obs);
                    counter++;
                }
                logger.info("finished adding " + counter + " records for obsTypeId: " + obsTypeId);
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
        Set<String> otgps = obsMap.keySet();

        for (String otgpair : otgps) {
            observations = obsMap.get(otgpair);
            Collections.sort(observations);

            logger.info("start inserting " + observations.size() + " obs values for gridId-obsTypeId: " + otgpair);

            // Insert all records in observations in archiveObs
            // Insert ao
            int index = otgpair.indexOf('-');
            String sql = "INSERT INTO obs.archiveObs VALUES ("
                    + "'[" + QueryString.convert(dr.getBeginDate(), "", false)
                    + QueryString.convert(dr.getEndDate(), "", true) + ")', '"
                    + otgpair.substring(0, index) + "', "
                    + otgpair.substring(index + 1) + ", ";

            String ovsStr = "";
            int counter = 0;
            for (Observation obs2 : observations) {
                ovsStr += "\"(" + getObsStr(obs2) + ")\", ";
                counter++;
            }

            sql += "'{" + ovsStr.substring(0, ovsStr.length() - 2) + "}')";
            db.update(connId, sql);
            logger.info("inserted " + counter + " obs values for obsTypeId-gridId: " + otgpair);
        }
    }

    /**
     * Note the date range includes the begin date and does not include the end date
     * ;
     * @param dateStr1 - begin date
     * @param dateStr2 - end date
     * @param gridId
     * @param obsTypeId
     */
    public ObsValue[] getArchiveObs(String dateStr1, String dateStr2, String gridId, String obsTypeId) {

        ObsValue[] obsValues = null;
        String sql = "SELECT VALUE FROM obs.archiveobs WHERE duration = '[" + dateStr1 + "," + dateStr2 + ")'"
                + " AND gridId = '" + gridId + "'"
                + " AND obsTypeId = " + obsTypeId;
        ResultSet rs = db.query(connId, sql);
        try {
            while (rs != null && rs.next()) {
                Array ovs = rs.getArray("value");
                String str = ovs.toString();
                ArrayList<String> strList = dap.postgresROW2StringList(str);

                obsValues = new ObsValue[strList.size()];
                int i = 0;
                for (String s : strList) {
                    obsValues[i++] = new ObsValue(s);
                }
            }
        } catch (Exception e) {
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

        return obsValues;
    }

    private String getObsStr(Observation obs) {
        String qchCharFlagStr = null;

        QualityFlag qf = qfd.getQualityFlag(obs.getSourceId(), obs.getObsTime());

        int charFlagLen = qf.getQchCharFlagLen();

        if (charFlagLen > 0) {
            qchCharFlagStr = "";
            for (char aChar : obs.getQchCharFlag())
                qchCharFlagStr += aChar;
        }

        String str = QueryString.convertId(obs.getSourceId(), false)
                + QueryString.convertId(obs.getSensorId(), false)
                + QueryString.convert(obs.getObsTime(), "", false)
                + QueryString.convert(obs.getRecvTime(), "", false)
                + QueryString.convert(obs.getLatitude(), false)
                + QueryString.convert(obs.getLongitude(), false)
                + QueryString.convert(obs.getElevation(), false)
                + QueryString.convert(obs.getValue(), false)
                + QueryString.convert(obs.getConfValue(), false)
                + qchCharFlagStr;

        return str;
    }

    private boolean tableExists(String dateStr) {
        String table = "obs.\"obs_" + dateStr + "\"";
        String table1 = "obs_" + dateStr;

        String sql = "select count(*) from information_schema.tables where table_name='" + table1 + "'";
        ResultSet rs = db.query(connId, sql);
        int count = 0;
        try {
            if (rs != null && rs.next())
                count = rs.getInt(1);
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
        if (count == 0) {
            logger.info("Table " + table + " doesn't exist");
            return false;
        } else
            return true;
    }

    private class SensorInfo {
        double elevOffset;
        int qchparmid;
        int id;
    }
}
