/************************************************************************
 * Source filename: SensorDao.java
 * <p/>
 * Creation date: Feb 25, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Data Access Object for the sensor table
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.dao;

import org.apache.log4j.Logger;
import wde.metadata.ISensor;
import wde.metadata.Sensor;
import wde.metadata.TimeVariantMetadata;
import wde.util.Notification;
import wde.util.QueryString;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

public class SensorDao extends TimerTask {

    private static final Logger logger = Logger.getLogger(SensorDao.class);
    private static final String SELECT_QUERY = "SELECT s.*, q.sensorTypeId, q.obsTypeId, "
            + " q.minRange, q.maxRange, q.ratePos, q.rateNeg, "
            + "q.persistInterval, q.persistThreshold, q.likeThreshold "
            + "FROM meta.sensor s, meta.qchparm q WHERE q.id=s.qchparmId";
    private static final String SELECT_ID_QUERY = "SELECT s.*, q.sensorTypeId, q.obsTypeId, "
            + "q.minRange, q.maxRange, q.ratePos, q.rateNeg, "
            + "q.persistInterval, q.persistThreshold, q.likeThreshold "
            + "FROM meta.sensor s, meta.qchparm q WHERE s.id=? AND q.id=s.qchparmId";
    private static final String SELECT_STATICID_QUERY = "SELECT s.*, q.sensorTypeId, q.obsTypeId, "
            + "q.minRange, q.maxRange, q.ratePos, q.rateNeg, "
            + "q.persistInterval, q.persistThreshold, q.likeThreshold "
            + "FROM meta.sensor s, meta.qchparm q WHERE staticId = ? AND s.qchparmId = q.id ORDER BY s.id desc limit 1";
    private static final String SELECT_SENSORID_QUERY = "select s.id from meta.sensor s, meta.platform p "
            + "where s.sourceId=? and s.platformid=p.id and p.platformcode=? and s.obstypeid=? and s.sensorindex=?";
    private static final String SELECT_PLATFORM_OBSTYPE_QUERY = "SELECT * FROM meta.sensor WHERE platformId = ? AND obsTypeId = ?";
    public static String csvPath = null;
    private static SensorDao instance;

    private DatabaseManager db = null;

    private String connId = null;

    private String sharedConnId = null;

    private MetadataDao md;

    private HashMap<String, TimeVariantMetadata> sensorUpdateMap = null;

    private HashMap<Integer, TimeVariantMetadata> sensor1stQueryMap = null;

    private HashMap<Integer, HashMap<Integer, ArrayList<ISensor>>> sensor2ndQueryMap = null;

    private HashMap<String, Integer> sensorIdLookupMap = null;

    private Properties prop = null;

    private String csvPickupPath = null;

    private String sensorFile = "sensors.csv";

    private long pollingInterval;

    private Timer myTimer = null;

    /**
     * Constructor
     */
    private SensorDao() {
        prop = new Properties();
        loadPropertiesFile();
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        sharedConnId = MetadataDao.getInstance().getConnId();
        md = MetadataDao.getInstance();

        sensorUpdateMap = new HashMap<>();
        sensor1stQueryMap = new HashMap<>();
        sensor2ndQueryMap = new HashMap<>();
        updateSensorMap();

        sensorIdLookupMap = new HashMap<>();

        myTimer = new Timer();
        run();
        myTimer.scheduleAtFixedRate(this, 0, pollingInterval);
    }

    /**
     * @return SensorDao
     */
    public static SensorDao getInstance() {
        if (instance == null)
            instance = new SensorDao();

        return instance;
    }

    /**
     * @return the sensorMap
     */
    public HashMap<String, TimeVariantMetadata> getSensorMap() {
        return sensorUpdateMap;
    }

    /**
     * @param id
     * @return
     */
    public ISensor getSensor(int id) {

        logger.debug("getSensor for id: " + id);

        Sensor s = (Sensor) sensor1stQueryMap.get(id);

        if (s == null) {
            s = getSensorForId(id);
            if (s != null) {
                synchronized (sensor1stQueryMap) {
                    sensor1stQueryMap.put(id, s);
                }

                int platformId = s.getPlatformId();
                int obsTypeId = s.getObsTypeId();
                synchronized (sensor2ndQueryMap) {
                    HashMap<Integer, ArrayList<ISensor>> map = sensor2ndQueryMap.get(platformId);
                    if (map == null) {
                        map = new HashMap<>();
                        sensor2ndQueryMap.put(platformId, map);
                        ArrayList<ISensor> sList = new ArrayList<>();
                        sList.add(s);
                        map.put(obsTypeId, sList);
                    } else {
                        ArrayList<ISensor> sList = map.get(obsTypeId);
                        if (sList == null)
                            sList = new ArrayList<>();

                        if (!sList.contains(s))
                            sList.add(s);
                    }
                }
            }
        }

        return s;
    }

    /**
     * @param platformId
     * @param obsTypeId
     * @param sensorIndex
     * @return
     */
    public ISensor getSensor(int platformId, int obsTypeId, int sensorIndex) {

        logger.info("getSensor for platformId: " + platformId + " obsTypeId: " + obsTypeId + " sensorIndex: " + sensorIndex);

        ISensor s = null;

        ArrayList<ISensor> sList = getSensors(platformId, obsTypeId);

        if (sList != null)
            for (ISensor i : sList) {
                if (i.getSensorIndex() == sensorIndex) {
                    s = i;
                    break;
                }
            }

        return s;
    }

    /**
     * @param platformCode
     * @param obsTypeId
     * @param sensorIndex
     * @return
     */
    public int getSensorId(int sourceId, String platformCode, int obsTypeId, int sensorIndex) {

        String key = sourceId + "|" + platformCode + "|" + obsTypeId + "|" + sensorIndex;
        Integer sensorId = sensorIdLookupMap.get(key);

        if (sensorId != null)
            return sensorId.intValue();

        PreparedStatement ps = db.prepareStatement(connId, SELECT_SENSORID_QUERY);
        ResultSet rs = null;
        int id = -1;

        try {
            ps.setInt(1, sourceId);
            ps.setString(2, platformCode);
            ps.setInt(3, obsTypeId);
            ps.setInt(4, sensorIndex);
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                id = rs.getInt("id");
                sensorIdLookupMap.put(key, new Integer(id));
            } else
                logger.info("Sensor not found for sourceId: " + sourceId + " platformCode: " + platformCode + " obsTypeId: " + obsTypeId + " sensorIndex: " + sensorIndex);
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

        return id;
    }

    /**
     * @param platformId
     * @param obsTypeId
     * @return
     */
    public ArrayList<ISensor> getSensors(int platformId, int obsTypeId) {

        logger.info("getSensors for platformId: " + platformId + " obsTypeId: " + obsTypeId);

        ArrayList<ISensor> sList = null;
        HashMap<Integer, ArrayList<ISensor>> map = sensor2ndQueryMap.get(platformId);

        if (map != null) {
            sList = map.get(obsTypeId);
            if (sList != null) {
                return sList;
            }

            sList = getSensorForPlatform(platformId, obsTypeId);
            synchronized (map) {
                map.put(obsTypeId, sList);
            }
        } else {
            synchronized (sensor2ndQueryMap) {
                map = new HashMap<>();
                sensor2ndQueryMap.put(platformId, map);
                sList = new ArrayList<>();
                map.put(obsTypeId, sList);
            }
        }

        return sList;
    }

    /**
     * Updates the sensor map based on what's currently in the database
     */
    public void updateSensorMap() {
        logger.info("Updating sensor maps");

        PreparedStatement ps = db.prepareStatement(connId, SELECT_QUERY);
        ResultSet rs = null;

        try {
            rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                Sensor sensor = populateSensor(rs);

                synchronized (sensorUpdateMap) {
                    sensorUpdateMap.put(sensor.getStaticId(), sensor);
                }

                int sId = sensor.getId();
                synchronized (sensor1stQueryMap) {
                    if (sensor1stQueryMap.get(sId) == null)
                        sensor1stQueryMap.put(sId, sensor);
                }

                int pId = sensor.getPlatformId();
                synchronized (sensor2ndQueryMap) {
                    HashMap<Integer, ArrayList<ISensor>> map = sensor2ndQueryMap.get(pId);
                    if (map == null) {
                        map = new HashMap<>();
                        sensor2ndQueryMap.put(pId, map);
                    }

                    ArrayList<ISensor> sList = map.get(sensor.getObsTypeId());
                    if (sList == null) {
                        sList = new ArrayList<>();
                        map.put(sensor.getObsTypeId(), sList);
                    }
                    sList.add(sensor);
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

        logger.info("Finished updating sensor maps");
    }

    /**
     * @param sensor
     * @param atomic - false if this is part of a batch processing
     * @return true if a change record in sensor needs to be inserted; false if no change is found
     */
    public boolean updateSensor(Sensor sensor, boolean atomic) {
        logger.info("updateSensor for id: " + sensor.getId());

        String key = sensor.getStaticId();

        Sensor dbSensor = (Sensor) sensorUpdateMap.get(key);

        if (dbSensor == null || !dbSensor.equals(sensor)) {

            if (dbSensor == null)
                logger.info("inserting sensor " + key);
            else
                logger.info("updating sensor " + key);

            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            String sql = "INSERT INTO meta.sensor (sourceId, staticId, updateTime, platformId, contribId, sensorIndex, obsTypeId, qchparmId, distGroup, nsOffset,"
                    + "ewOffset, elevOffset, surfaceOffset, installDate, calibDate, maintDate, maintBegin, maintEnd, embeddedMaterial, sensorLocation" + ")  VALUES ("
                    + QueryString.convertId(sensor.getSourceId(), false)
                    + key + ","
                    + "'" + now + "',"
                    + QueryString.convertId(sensor.getPlatformId(), false)
                    + QueryString.convertId(sensor.getContribId(), false)
                    + QueryString.convert(sensor.getSensorIndex(), false)
                    + QueryString.convertId(sensor.getObsTypeId(), false)
                    + QueryString.convertId(sensor.getQchparmId(), false)
                    + QueryString.convertId(sensor.getDistGroup(), false)
                    + QueryString.convert(sensor.getNsOffset(), false)
                    + QueryString.convert(sensor.getEwOffset(), false)
                    + QueryString.convert(sensor.getElevOffset(), false)
                    + QueryString.convert(sensor.getSurfaceOffset(), false)
                    + QueryString.convert(sensor.getInstallDate(), "'", false)
                    + QueryString.convert(sensor.getCalibDate(), "'", false)
                    + QueryString.convert(sensor.getMaintDate(), "'", false)
                    + QueryString.convert(sensor.getMaintBegin(), "'", false)
                    + QueryString.convert(sensor.getMaintEnd(), "'", false)
                    + QueryString.convert(sensor.getEmbeddedMaterial(), false)
                    + QueryString.convert(sensor.getSensorLocation(), true)
                    + ");";

            if (atomic) {
                db.update(connId, sql);

                // Update the id field
                sensor = getSensorForStaticId(key);
                sensorUpdateMap.put(key, sensor);
            } else
                db.update(sharedConnId, sql);

            if (dbSensor != null)
                md.inactivateRecord("meta.sensor", dbSensor.getId(), now);

            if (atomic)
                md.lastUpate("sensor", now);

            return true;
        }

        return false;
    }

    public void run() {
        logger.info("run() invoked");
        String filePath = csvPickupPath + sensorFile;
        File f = new File(filePath);

        if (f.exists()) {

            ArrayList<InternetAddress> recipients = new ArrayList<>();
            InternetAddress me = null;
            try {
                me = new InternetAddress("george.zheng@leidos.com");
            } catch (AddressException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                logger.error(e.getMessage());
            }
            recipients.add(me);

            Notification notification = Notification.getInstance();
            notification.sendEmail(recipients, "metadata update", "attached file detected", filePath, sensorFile);

            // To be implemented
            f.delete();
        }
    }

    /**
     * @param id
     * @return
     */
    private Sensor getSensorForId(int id) {

        Sensor sensor = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_ID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, id);
            rs = ps.executeQuery();

            if (rs != null && rs.next()) {
                sensor = populateSensor(rs);
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

        return sensor;
    }

    /**
     * @param staticId
     * @return the most recent record that has the same staticId
     */
    private Sensor getSensorForStaticId(String staticId) {

        Sensor sensor = null;

        PreparedStatement ps = db.prepareStatement(connId, SELECT_STATICID_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(staticId));
            rs = ps.executeQuery();

            if (rs != null && rs.next())
                sensor = populateSensor(rs);

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

        return sensor;
    }

    /**
     * @param platformId
     * @param obsTypeId
     * @return
     */
    private ArrayList<ISensor> getSensorForPlatform(int platformId, int obsTypeId) {
        ArrayList<ISensor> sList = new ArrayList<>();

        PreparedStatement ps = db.prepareStatement(connId, SELECT_PLATFORM_OBSTYPE_QUERY);
        ResultSet rs = null;

        try {
            ps.setInt(1, Integer.valueOf(platformId));
            ps.setInt(2, Integer.valueOf(obsTypeId));
            rs = ps.executeQuery();

            while (rs != null && rs.next()) {
                Sensor sensor = populateSensor(rs);
                sList.add(sensor);
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

        return sList;
    }

    private Sensor populateSensor(ResultSet rs) {
        Sensor sensor = new Sensor();

        try {
            sensor.setId(rs.getInt("id"));
            sensor.setSourceId(rs.getInt("sourceId"));
            sensor.setStaticId(String.valueOf(rs.getInt("staticId")));
            sensor.setUpdateTime(rs.getTimestamp("updateTime"));
            sensor.setToTime(rs.getTimestamp("toTime"));
            sensor.setPlatformId(rs.getInt("platformId"));
            sensor.setContribId(rs.getInt("contribId"));
            sensor.setSensorIndex(rs.getInt("sensorIndex"));
            sensor.setObsTypeId(rs.getInt("obsTypeId"));
            sensor.setQchparmId(rs.getInt("qchparmId"));
            sensor.setDistGroup(rs.getInt("distGroup"));
            sensor.setNsOffset(rs.getFloat("nsOffset"));
            sensor.setEwOffset(rs.getFloat("ewOffset"));
            sensor.setElevOffset(rs.getFloat("elevOffset"));
            sensor.setSurfaceOffset(rs.getFloat("surfaceOffset"));
            sensor.setInstallDate(rs.getTimestamp("installDate"));
            sensor.setCalibDate(rs.getTimestamp("calibDate"));
            sensor.setMaintDate(rs.getTimestamp("maintDate"));
            sensor.setMaintBegin(rs.getTimestamp("maintBegin"));
            sensor.setMaintEnd(rs.getTimestamp("maintEnd"));
            sensor.setEmbeddedMaterial(rs.getString("embeddedMaterial"));
            sensor.setSensorLocation(rs.getString("sensorLocation"));
            sensor.setSensorTypeId(rs.getInt("sensorTypeId"));
            sensor.setObsTypeId(rs.getInt("obsTypeId"));
            sensor.setMinRange(rs.getDouble("minRange"));
            sensor.setMaxRange(rs.getDouble("maxRange"));
            sensor.setRatePos(rs.getDouble("ratePos"));
            sensor.setRateNeg(rs.getDouble("rateNeg"));
            sensor.setPersistInterval(rs.getDouble("persistInterval"));
            sensor.setPersistThreshold(rs.getDouble("persistThreshold"));
            sensor.setLikeThreshold(rs.getDouble("likeThreshold"));
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return sensor;
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        String separator = System.getProperty("file.separator");

        String path = null;

        if (csvPath != null)
            path = csvPath;
        else
            path = System.getProperty("user.dir") + separator + "config" + separator + "metadata_recovery_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            csvPickupPath = prop.getProperty("csvpickuppath");
            sensorFile = prop.getProperty("sensorfile");

            // default 2 minutes for testing
            pollingInterval = Integer.valueOf(prop.getProperty("pollinginterval", "5")).intValue() * 60000;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
