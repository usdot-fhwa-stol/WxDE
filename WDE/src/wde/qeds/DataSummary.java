/************************************************************************
 * Source filename: DataSummary.java
 * <p/>
 * Creation date: May 9, 2014
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

package wde.qeds;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.util.Region;

import javax.servlet.http.HttpServletRequest;
import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DataSummary {

    private static final long NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000L;
    static Logger logger = Logger.getLogger(DataSummary.class);
    private static String SUMMARY_QUERY = "SELECT meta.obstype ot, count(*) FROM obs.\"?\" o"
            + " WHERE obsTime >= ? AND obsTime < ? and o.obstypeid = ot.id"
            + " and o.latitude > ? and o.latitude < ? and o.longitude > ? and o.longitude < ?"
            + " group by ot.obstype order by ot.obstype";
    private static DataSummary instance;
    private DataSource dataSource;

    public DataSummary() {
        Config oConfig = ConfigSvc.getInstance().getConfig("wde.EmsMgr");
        WDEMgr wdeMgr = WDEMgr.getInstance();
        dataSource = wdeMgr.getDataSource(oConfig.getString("datasource", "java:comp/env/jdbc/wxde"));
    }

    /**
     * @return the instance
     */
    public static DataSummary getInstance() {
        if (instance == null) instance = new DataSummary();

        return instance;
    }

    public void getResults(HttpServletRequest request, PrintWriter writer) {

        String region = null;
        String radius = request.getParameter("radius");
        if (radius == null) {
            String lat1 = request.getParameter("lat1");
            String long1 = request.getParameter("long1");
            String lat2 = request.getParameter("lat2");
            String long2 = request.getParameter("long2");
            region = lat1 + "," + long1 + "," + lat2 + "," + long2;
        } else {
            String lat = request.getParameter("lat");
            String lon = request.getParameter("long");
            region = lat + "," + lon + "," + radius;
        }
        double[] coordinates = Region.convert(region);
        String beginTimeStr = request.getParameter("beginTime");
        String endTimeStr = request.getParameter("endTime");

        try (Connection dbConn = dataSource.getConnection()){
            long beginTimeLong = Long.valueOf(beginTimeStr);
            long endTimeLong = Long.valueOf(endTimeStr);

            Timestamp beginTime = new Timestamp(beginTimeLong);
            Timestamp endTime = new Timestamp(endTimeLong);
            Timestamp runningTime = new Timestamp(beginTimeLong);

            HashMap<String, Long> summaryMap = new HashMap<>();

            while (runningTime.getTime() < (endTimeLong + NUM_OF_MILLI_SECONDS_IN_A_DAY)) {
                String tableName = "obs_" + runningTime.toString().substring(0, 10);
                runningTime.setTime(runningTime.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                DatabaseMetaData dbm = dbConn.getMetaData();
                try(ResultSet tables = dbm.getTables(null, null, tableName, null))
                {
                  if (!tables.next()) {
                      logger.debug(tableName + " does not exist");
                      continue;
                  }
                }

                // now that we have database connections, get the summary
                String queryStr = "SELECT ot.obstype, count(*) FROM obs.\"" + tableName + "\" o, meta.obstype ot"
                        + " WHERE obsTime >= ? AND obsTime < ? and o.obstypeid = ot.id"
                        + " and o.latitude >= ? and o.latitude < ?"
                        + " and o.longitude >= ? and o.longitude < ?"
                        + " group by ot.obstype order by ot.obstype";

                logger.info(queryStr);
                try(PreparedStatement ps = dbConn.prepareStatement(queryStr))
                {
                  ps.setTimestamp(1, beginTime);
                  ps.setTimestamp(2, endTime);
                  ps.setLong(3, MathUtil.toMicro(coordinates[0]));
                  ps.setLong(4, MathUtil.toMicro(coordinates[2]));
                  ps.setLong(5, MathUtil.toMicro(coordinates[1]));
                  ps.setLong(6, MathUtil.toMicro(coordinates[3]));

                  try(ResultSet rs = ps.executeQuery())
                  {

                    while (rs.next()) {
                        String key = rs.getString("obsType");
                        int value = rs.getInt("count");
                        Long oldValueLong = summaryMap.get(key);
                        if (oldValueLong != null)
                            value += oldValueLong.intValue();

                        summaryMap.put(key, new Long(value));
                    }
                  }
                }
            }

            if (summaryMap.size() == 0)
                writer.println("No relevant data is available");
            else {
                writer.println("Summary of data available in the WxDE:\n");
                List<String> keyList = new ArrayList<String>(summaryMap.keySet());
                Collections.sort(keyList);
                for (String key : keyList)
                    writer.println(key + " " + summaryMap.get(key));
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
