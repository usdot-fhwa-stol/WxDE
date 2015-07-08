/************************************************************************
 * Source filename: SensorRecovery.java
 * <p/>
 * Creation date: Jul 24, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import wde.dao.DatabaseManager;
import wde.metadata.Sensor;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class MissingSensorGenerator {

    private static final Logger logger = Logger.getLogger(MissingSensorGenerator.class);

    private static final String UPDATE_TIME = "2008-12-01 00:00:00";

    private Properties prop = null;

    private String separator = null;

    private String csvCreatePath = null;

    private String sensorFile = null;

    private DatabaseManager db = null;

    private String conn = null;

    private SimpleDateFormat sdf = null;

    private MissingSensorGenerator() {
        prop = new Properties();
        separator = System.getProperty("file.separator");
        db = DatabaseManager.getInstance();
        conn = db.getConnection();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

    public static void main(String args[]) {
        DOMConfigurator.configure("config/wde_log4j.xml");
        MissingSensorGenerator sr = new MissingSensorGenerator();
        sr.loadPropertiesFile();
        sr.insertSensors();
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        String path = System.getProperty("user.dir") + separator + "config" + separator + "metadata_recovery_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            csvCreatePath = prop.getProperty("csvcreatepath");
            sensorFile = prop.getProperty("sensorfile");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    private Sensor populate(ResultSet rs) {
        Sensor sensor = new Sensor();

        // populate sensor with the first record in rs
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
        } catch (SQLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        return sensor;
    }

    private void insertSensorsToFile(HashMap<String, Sensor> sensorMap) {
        if (sensorMap == null || sensorMap.size() == 0)
            return;

        try {
            BufferedWriter file = new BufferedWriter(new FileWriter(csvCreatePath + sensorFile));
            file.write("id| sourceId| staticId| updateTime| toTime| platformId| contribId| sensorIndex| " +
                    "obsTypeId| qchparmId| distGroup| nsOffset| ewOffset| elevOffset| surfaceOffset| " +
                    "installDate| calibDate| maintDate| maintBegin| maintEnd| embeddedMaterial| sensorLocation\r\n");

            Collection<Sensor> sensors = sensorMap.values();
            for (Sensor sensor : sensors) {
                file.write(sensor.getId() + "| ");
                file.write(sensor.getSourceId() + "| ");
                file.write(sensor.getStaticId() + "| ");
                file.write(sensor.getUpdateTime() + "| ");
                Timestamp ts = sensor.getToTime();
                file.write((ts == null) ? "| " : ts + "| ");
                file.write(sensor.getPlatformId() + "| ");
                file.write(sensor.getContribId() + "| ");
                file.write(sensor.getSensorIndex() + "| ");
                file.write(sensor.getObsTypeId() + "| ");
                file.write(sensor.getQchparmId() + "| ");
                file.write(sensor.getDistGroup() + "| ");
                file.write(sensor.getNsOffset() + "| ");
                file.write(sensor.getEwOffset() + "| ");
                file.write(sensor.getElevOffset() + "| ");
                file.write(sensor.getSurfaceOffset() + "| ");
                ts = sensor.getInstallDate();
                file.write((ts == null) ? "| " : ts + "| ");
                ts = sensor.getCalibDate();
                file.write((ts == null) ? "| " : ts + "| ");
                ts = sensor.getMaintDate();
                file.write((ts == null) ? "| " : ts + "| ");
                ts = sensor.getMaintBegin();
                file.write((ts == null) ? "| " : ts + "| ");
                ts = sensor.getMaintEnd();
                file.write((ts == null) ? "| " : ts + "| ");
                String str = sensor.getEmbeddedMaterial();
                file.write((str == null) ? "| " : "\"" + str + "\"| ");
                str = sensor.getSensorLocation();
                file.write((str == null) ? "| " : str + "| ");

                file.write("\r\n");
            }
            file.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private String parseString(String str) {
        char SINGLEQUOTE = '\'';

        // Strip out all quotation marks and escape SINGLEQUOTE
        for (int index = 0; index < str.length(); index++) {
            if (str.charAt(index) == SINGLEQUOTE) {
                str = str.substring(0, index) + SINGLEQUOTE + str.substring(index);
                index++;
            }
        }

        return str;
    }

    private void insertSensors() {
        HashMap<String, Sensor> sensorMap = new HashMap<>();
        Sensor sensor = null;
        String key = null;

        String sql = "select distinct contribid, platformcode, obstypeid, sensorindex from obs.invalidobs order by contribid, platformcode, obstypeid, sensorindex";
//        String sql = "select distinct contribid, platformcode, obstypeid, sensorindex from obs.\"invalidobs_2015-01-16\" order by contribid, platformcode, obstypeid, sensorindex";
        logger.info(sql);

        PreparedStatement ps = db.prepareStatement(conn, sql);
        ResultSet rs = null;

        try {
            rs = ps.executeQuery();

            SensorRecoveryObj sro = null;
            ArrayList<SensorRecoveryObj> sros = new ArrayList<>();
            while (rs != null && rs.next()) {
                sro = new SensorRecoveryObj(
                        rs.getInt("contribid"), rs.getString("platformCode"),
                        rs.getInt("obstypeid"), rs.getInt("sensorindex"));
                sros.add(sro);
            }
            rs.close();
            ps.close();

            logger.info("iterating through SensorRecoveryObjs");
            for (SensorRecoveryObj s : sros) {
                int platformId = 0;
                int cid = s.getContribId();
                if (cid > 0) {
                    sql = "select id from meta.platform where contribid=" + cid + " and platformcode='" + parseString(s.getPlatformCode()).trim() + "'";
                    //logger.info(sql);
                    ps = db.prepareStatement(conn, sql);
                    rs = ps.executeQuery();
                    if (rs == null || !rs.next()) {
                        logger.error("Did not find platform for contribid: " + cid + " platformcode: " + s.getPlatformCode());
                        continue;
                    }

                    platformId = rs.getInt("id");
                    rs.close();
                    ps.close();
                } else { // contribid is negative, need to get the list from the conf.csvc table
                    int id = -cid;
                    sql = "select contribid from conf.csvc where id=" + id;
                    ps = db.prepareStatement(conn, sql);
                    rs = ps.executeQuery();
                    if (rs == null || !rs.next()) {
                        logger.error("Did not find contribid for csvc id: " + id);
                        continue;
                    }
                    String contribidStr = rs.getString("contribid");
                    String[] contribids = contribidStr.split(",");
                    rs.close();
                    ps.close();
                    for (String contribid : contribids) {
                        sql = "select id from meta.contrib where staticid=" + contribid + " and totime is null";
                        ps = db.prepareStatement(conn, sql);
                        rs = ps.executeQuery();
                        if (rs == null || !rs.next()) {
                            continue;
                        }
                        cid = rs.getInt("id");
                        rs.close();
                        ps.close();

                        sql = "select id from meta.platform where contribid=" + cid + " and platformcode='" + parseString(s.getPlatformCode()).trim() + "'";
                        ps = db.prepareStatement(conn, sql);
                        rs = ps.executeQuery();
                        if (rs == null || !rs.next()) {
                            continue;
                        }
                        platformId = rs.getInt("id");
                        rs.close();
                        ps.close();
                    }
                    if (platformId == 0) {
                        logger.error("Did not find platform for contribids: " + contribidStr + " platformcode: " + s.getPlatformCode());
                        continue;
                    }
                }

                sql = "select * from meta.sensor where platformid='" + platformId + "' and obstypeid=" + s.getObsTypeId() + " and sensorindex=" + s.getSensorIndex() + " and totime is null";
                //logger.info(sql);
                ps = db.prepareStatement(conn, sql);
                rs = ps.executeQuery();
                boolean foundSensor = false;
                if (rs != null && rs.next()) {
                    foundSensor = true;
                    logger.error("***Sensor already exists - sensorid: " + rs.getInt("id"));
                }

                rs.close();
                ps.close();

                if (foundSensor)
                    continue;

                if (s.getSensorIndex() > 0) {
                    sql = "select * from meta.sensor where platformid="
                            + platformId + " and obstypeid=" + s.getObsTypeId()
                            + " and sensorindex=0 and totime is null";
                    //logger.info(sql);
                    ps = db.prepareStatement(conn, sql);
                    rs = ps.executeQuery();
                    if (rs != null && rs.next()) {
                        sensor = populate(rs);
                        sensor.setSensorIndex(s.getSensorIndex());
                        foundSensor = true;
                        Date d = sdf.parse(UPDATE_TIME);
                        sensor.setUpdateTime(new Timestamp(d.getTime()));
                        key = sensor.getContribId() + ","
                                + sensor.getPlatformId() + ","
                                + sensor.getObsTypeId() + ","
                                + sensor.getSensorIndex();
                        if (sensorMap.get(key) == null)
                            sensorMap.put(key, sensor);
                    }
                    rs.close();
                    ps.close();

                    if (foundSensor)
                        continue;
                }

                // find the most commonly used qchparm from the same contribid for the obstypeid
                sql = "select * from meta.sensor where obstypeid=" + s.getObsTypeId()
                        + " and qchparmid in "
                        + "(select qchparmid from meta.sensor where obstypeid=" + s.getObsTypeId()
                        + " and platformid in (select id from meta.platform where contribid=" + cid
                        + ") group by qchparmid order by count(qchparmid) desc limit 1) and contribid=" + cid;
                //logger.info(sql);
                ps = db.prepareStatement(conn, sql);
                rs = ps.executeQuery();
                if (rs != null && rs.next()) {
                    sensor = populate(rs);
                    sensor.setPlatformId(platformId);
                    sensor.setSensorIndex(s.getSensorIndex());
                    foundSensor = true;
                    Date d = sdf.parse(UPDATE_TIME);
                    sensor.setUpdateTime(new Timestamp(d.getTime()));
                    key = sensor.getContribId() + ","
                            + sensor.getPlatformId() + ","
                            + sensor.getObsTypeId() + ","
                            + sensor.getSensorIndex();
                    if (sensorMap.get(key) == null)
                        sensorMap.put(key, sensor);
                }
                rs.close();
                ps.close();

                if (!foundSensor)
                    logger.info("Did not find a matching sensor for contribid: " + cid + " platformcode: " + s.getPlatformCode() + " obstypeid:" + s.getObsTypeId() + " and sensorIndex:" + s.getSensorIndex());
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }

        insertSensorsToFile(sensorMap);

        System.out.println("Done");
    }
}
