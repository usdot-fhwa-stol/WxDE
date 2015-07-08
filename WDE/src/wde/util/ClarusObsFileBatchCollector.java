/************************************************************************
 * Source filename: ClarusObsFileBatchCollector.java
 * <p/>
 * Creation date: June 07, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
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

import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;


public class ClarusObsFileBatchCollector {
    private static final Logger logger = Logger.getLogger(ClarusObsFileBatchCollector.class);
    String timeFormat = null;
    SimpleDateFormat timeFormatter = null;
    private FileDownloader fd = null;
    private Properties prop = null;
    private String baseDataFolder = null;
    private String baseUrl = null;
    private long pollingInterval;
    private Date startDate = null;
    private Date endDate = null;

    /**
     *
     */
    public ClarusObsFileBatchCollector(String startDateStr, String endDateStr) {
        prop = new Properties();
        loadPropertiesFile();
        fd = new FileDownloader();

        timeFormat = "yyyyMMdd";
        timeFormatter = new SimpleDateFormat(timeFormat);

        try {
            startDate = timeFormatter.parse(startDateStr);
            endDate = timeFormatter.parse(endDateStr);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        DOMConfigurator.configure("config/wdecs_log4j.xml");
        ClarusObsFileBatchCollector cs = new ClarusObsFileBatchCollector(args[0], args[1]);
        cs.Collect();
    }

    /**
     *
     */
    public void Collect() {
        Calendar cal = (Calendar) Calendar.getInstance();

        long workingTime = startDate.getTime();
        long endTime = endDate.getTime();

        while (workingTime < endTime) {

            String beginStr = String.valueOf(workingTime - pollingInterval);
            String endStr = String.valueOf(workingTime);

            cal.setTimeInMillis(workingTime);
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            String suffix = "_" + String.format("%02d", hour) + ".csv";
            String separator = System.getProperty("file.separator");

            String dateStr = timeFormatter.format(workingTime);
            String fileName = dateStr + suffix;
            String targetFilePath = baseDataFolder + separator + dateStr + separator + fileName;
            String urlStr = baseUrl + beginStr + "," + endStr;
            fd.download(urlStr, targetFilePath, true);

            workingTime += pollingInterval;
        }
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "wdecs_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            baseDataFolder = prop.getProperty("basedatafolder");
            baseUrl = prop.getProperty("baseurl");

            // default 2 minutes for testing
            pollingInterval = Integer.valueOf(prop.getProperty("pollingInterval", "2")).intValue() * 60000;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
