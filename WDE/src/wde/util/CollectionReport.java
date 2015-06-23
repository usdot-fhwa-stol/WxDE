/************************************************************************
 * Source filename: CollectionReport.java
 * <p/>
 * Creation date: Aug 6, 2013
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

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class CollectionReport {

    private static final Logger logger = Logger.getLogger(CollectionReport.class);
    private static final int NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000;
    public static boolean wdeMgrInstantiated = false;
    //private static final int NUM_OF_MILLI_SECONDS_IN_AN_HOUR = 3600000;
    private Properties prop = null;
    private String reportPath = null;
    private HashMap<String, int[]> collectionMap = null;
    private DatabaseManager db = null;
    private String connId = null;
    private String timeFormat = null;
    private SimpleDateFormat timeFormatter = null;

    /**
     *
     */
    public CollectionReport() {
        prop = new Properties();
        if (!wdeMgrInstantiated)
            loadPropertiesFile();
        collectionMap = new HashMap<>();
        db = DatabaseManager.getInstance();
        connId = db.getConnection();
        timeFormat = "yyyy-MM-dd";
        timeFormatter = new SimpleDateFormat(timeFormat);
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        DOMConfigurator.configure("config/wde_log4j.xml");
        CollectionReport cr = new CollectionReport();
        cr.reportCollectionStatus(args[0], args[1], args[2]);
//        cr.collectDailyStatistics();
    }

    /**
     * @param reportPath the reportPath to set
     */
    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public void reportCollectionStatus(String startDateStr, String endDateStr, String intervalStr) {
        Date startDate = null;
        Date endDate = null;
        int interval;

        try {
            startDate = timeFormatter.parse(startDateStr);
            endDate = timeFormatter.parse(endDateStr);
            interval = Integer.parseInt(intervalStr);

            collectStatistics(startDate, endDate);
            generateReport(startDate, endDate, interval);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    public void collectDailyStatistics() {
        long now = System.currentTimeMillis();
        Date beginDate = new Date(now - NUM_OF_MILLI_SECONDS_IN_A_DAY);
        Date endDate = new Date(now + 3000);

        collectStatistics(beginDate, endDate);

        Calendar cal = Calendar.getInstance();
        Date endDate2 = cal.getTime();
        cal.add(Calendar.MONTH, -1);
        cal.set(Calendar.DATE, 1);

        generateReport(cal.getTime(), endDate2, 7);
    }

    public void collectStatistics(Date startDate, Date endDate) {
        Date workingDate = new Date(startDate.getTime());

        while (workingDate.getTime() < endDate.getTime()) {

            String currentTableDate = timeFormatter.format(workingDate);

            String tableName = "obs_" + currentTableDate;
            try {
                DatabaseMetaData dbm = db.getMetaData(connId);
                ResultSet tables = dbm.getTables(null, null, tableName, null);
                if (!tables.next()) {
                    tables.close();
                    logger.debug(tableName + " does not exist");

                    // progress one day
                    workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                    continue;
                }
                tables.close();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            }

            try {
                String sql = "select c.id, c.name, p.category, count(*) from meta.contrib c, " +
                        "obs.\"obs_" + currentTableDate + "\"" + " o, conf.csvc cs, meta.sensor s, meta.platform p " +
                        "where o.sensorid = s.id and s.platformid = p.id and p.contribId = c.id " +
                        "group by c.id, c.name, p.category order by c.id, c.name, p.category";

                logger.info(sql);

                ResultSet rs = db.query(connId, sql);
                while (rs != null && rs.next()) {
                    String contribId = rs.getString("id");
                    String contribName = rs.getString("name");
                    String collectionDate = timeFormatter.format(workingDate);
                    String category = rs.getString("category");
                    int count = rs.getInt("count");

                    sql = "select * from conf.dailycollectionstatistics where contribid=" + contribId +
                            " and collectiondate='" + collectionDate + "' and category='" + category + "'";
                    ResultSet rs1 = db.query(connId, sql);
                    String updateStr = null;
                    if (rs1 != null && rs1.next()) {
                        updateStr = "update conf.dailycollectionstatistics set numobservations=" + count + "where contribid=" +
                                contribId + " and collectiondate='" + collectionDate + "' and category='" + category + "'";
                        rs1.close();
                    } else
                        updateStr = "insert into conf.dailycollectionstatistics " +
                                "(contribid, contribname, collectiondate, category, numobservations) values (" + contribId +
                                ", \'" + contribName + "', \'" + collectionDate + "',\'" + category + "'," + count + ")";

                    db.update(connId, updateStr);
                }
                rs.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // progress one day
            workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
        }
    }

    public void generateReport(Date startDate, Date endDate, int interval) {
        logger.info("calling generateReport - startDate: " + startDate + " endDate: " + endDate);

        HashMap<String, Integer> contributorStatusMap = new HashMap<>();
        HashMap<String, Integer> contributorMobileStatusMap = new HashMap<>();

        Date workingDate = new Date(startDate.getTime());
        long delta = endDate.getTime() - startDate.getTime();
        long remainder = delta % (NUM_OF_MILLI_SECONDS_IN_A_DAY * interval);
        int extraDays = (int) ((remainder == 0) ? 0 : remainder / NUM_OF_MILLI_SECONDS_IN_A_DAY);
        long size = delta / (NUM_OF_MILLI_SECONDS_IN_A_DAY * interval) + extraDays;
        logger.info("size: " + size);
        int index = -1;

        String headerStr = "Contributor Name";
        int intervalCounter = 0;
        String currentTableDate = null;

        while (workingDate.getTime() < endDate.getTime()) {

            long remainingTime = endDate.getTime() - workingDate.getTime();

            // This should take care of day light savings
            boolean isYesterday = (Math.abs(remainingTime - NUM_OF_MILLI_SECONDS_IN_A_DAY) < (NUM_OF_MILLI_SECONDS_IN_A_DAY / 6)) ? true : false;

            currentTableDate = timeFormatter.format(workingDate);

            // Need to fix the header
            if (intervalCounter != 0 && intervalCounter == (interval - 1))
                headerStr += " to " + currentTableDate;
            else {
                if (intervalCounter == 0 || remainder != 0 && (remainingTime / NUM_OF_MILLI_SECONDS_IN_A_DAY < extraDays)) {
                    headerStr += "," + currentTableDate;
                    index++;
                    logger.info("intervalCounter: " + intervalCounter + " remainder: " + remainder);
                    logger.info("headerStr: " + headerStr + " index: " + index);
                }
            }

            // populate collectionMap
            try {
                String sql = "select contribid, contribname, category, numobservations from conf.dailycollectionstatistics " +
                        "where collectiondate='" + currentTableDate + "'";
                logger.info(sql);

                ResultSet rs = db.query(connId, sql);
                while (rs != null && rs.next()) {
                    String id = rs.getString("contribid");
                    String name = rs.getString("contribname");
                    String category = rs.getString("category");
                    String key = name + " (" + category + ")";
                    int count = rs.getInt("numobservations");
                    int[] values = null;
                    if (collectionMap.get(key) == null) {
                        values = new int[(int) size];
                        collectionMap.put(key, values);
                    } else {
                        values = collectionMap.get(key);
                    }
                    values[index] += count;

                    if (isYesterday) {
                        Integer contribCount = null;
                        HashMap<String, Integer> map = null;
                        if (category.equals("M"))
                            map = contributorMobileStatusMap;
                        else if (!category.equals("S")) // exclude segments
                            map = contributorStatusMap;

                        if (map != null) {
                            contribCount = map.get(id);
                            if (contribCount == null)
                                contribCount = new Integer(count);
                            else {
                                int oldCount = contribCount.intValue();
                                contribCount = new Integer(oldCount + count);
                            }
                            map.put(id, contribCount);
                        }
                    }
                }
                rs.close();
            } catch (SQLException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }

            // progress one day
            workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
            intervalCounter++;
            if (intervalCounter >= interval)
                intervalCounter = 0;
        }

        // update the collectionstatus table
        try {
            // Initialize the table
            logger.info("deleting conf.collectionstatus");

            String updateStr = "delete from conf.collectionstatus";
            db.update(connId, updateStr);

            String sql = "select distinct(contribid) from conf.csvc where active=1";
            ResultSet rs = db.query(connId, sql);
            while (rs != null && rs.next()) {

                String contribIdStrs = rs.getString("contribid");
                String[] contribIds = contribIdStrs.split(",");

                for (String contribIdStr : contribIds) {

                    int contribId = Integer.parseInt(contribIdStr);
                    updateStr = "insert into conf.collectionstatus (contribid, updatetime, status) " +
                            "values (" + contribId + ", '" + currentTableDate + "', 1)";

                    logger.info(updateStr);

                    db.update(connId, updateStr);
                }
            }
            rs.close();

            for (String id : contributorStatusMap.keySet()) {
                int count = contributorStatusMap.get(id).intValue();
                String statistics = currentTableDate + " RWIS observations: " + count;
                boolean status = !(count > 0);

                updateStr = "update conf.collectionstatus set status=" + (!status ? 0 : 1) +
                        ", statistics='" + statistics + "' where contribid=" + id;

                logger.info(updateStr);

                db.update(connId, updateStr);
            }
            sql = "select * from conf.collectionstatus";
            rs = db.query(connId, sql);
            while (rs != null && rs.next()) {
                int contribId = rs.getInt("contribid");
                Integer count = contributorMobileStatusMap.get(String.valueOf(contribId));
                if (count == null)
                    continue;

                boolean status = rs.getBoolean("status");
                String statistics = rs.getString("statistics");
                if (!status && count.intValue() == 0)
                    status = false;
                else if (status && (statistics == null || statistics.length() == 0) && count.intValue() > 0)
                    status = false;

                if (statistics == null || statistics.length() == 0)
                    statistics = currentTableDate + " Mobile observations: " + count.intValue();
                else
                    statistics += ", Mobile observations: " + count.intValue();

                updateStr = "update conf.collectionstatus set status=" + (!status ? 0 : 1) +
                        ", statistics='" + statistics + "' where contribid=" + contribId;

                logger.info(updateStr);

                db.update(connId, updateStr);
            }
            rs.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Writing content of collectionMap into the csv file
        try {
            BufferedWriter file = new BufferedWriter(new FileWriter(reportPath));
            file.write(headerStr);
            file.write("\r\n");
            Set<String> keySet = collectionMap.keySet();
            ArrayList<String> keyList = new ArrayList<>(keySet);
            java.util.Collections.sort(keyList);
            for (String key : keyList) {
                int[] values = collectionMap.get(key);
                file.write(key);
                for (int i = 0; i < values.length; i++)
                    file.write(", " + values[i]);

                file.write("\r\n");
            }

            file.write("\r\n");
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "wdecr_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            reportPath = prop.getProperty("reportpath");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
