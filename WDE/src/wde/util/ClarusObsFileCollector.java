/************************************************************************
 * Source filename: ClarusObsFileCollector.java
 * <p/>
 * Creation date: Feb 14, 2013
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
import java.text.SimpleDateFormat;
import java.util.*;


public class ClarusObsFileCollector extends TimerTask {
    private static final Logger logger = Logger.getLogger(ClarusObsFileCollector.class);

    private static ClarusObsFileCollector instance = null;
    private FileDownloader fd = null;

    private Properties prop = null;
    private String baseDataFolder = null;
    private String baseUrl = null;
    private long pollingInterval;
    private long dataDelay;

    private Timer myTimer = null;

    /**
     *
     */
    private ClarusObsFileCollector() {
        prop = new Properties();
        loadPropertiesFile();
        fd = new FileDownloader();
        myTimer = new Timer();

        run();

        long currentTime = Calendar.getInstance().getTimeInMillis();
        long delay = pollingInterval - currentTime % pollingInterval;

        myTimer.scheduleAtFixedRate(this, delay, pollingInterval);
    }

    /**
     * @return a reference to the ClarusObsFileCollector singleton.
     */
    public static ClarusObsFileCollector getIntance() {
        if (instance == null)
            instance = new ClarusObsFileCollector();

        return instance;
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        DOMConfigurator.configure("config/wdecs_log4j.xml");
        ClarusObsFileCollector.getIntance();
    }

    /**
     *
     */
    public void run() {
        String timeFormat = "yyyyMMdd";

        SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);

        Calendar cal = (Calendar) Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        long currentTime = cal.getTimeInMillis();
        long currentMark = currentTime / pollingInterval * pollingInterval;
        long dataEndTime = currentMark - dataDelay;

        String beginStr = String.valueOf(dataEndTime - pollingInterval);
        String endStr = String.valueOf(dataEndTime);

        cal.setTimeInMillis(dataEndTime);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        String suffix = "_" + String.format("%02d", hour) + ".csv";
        String separator = System.getProperty("file.separator");

        String dateStr = timeFormatter.format(dataEndTime);
        String fileName = dateStr + suffix;
        String targetFilePath = baseDataFolder + separator + dateStr + separator + fileName;
        String urlStr = baseUrl + beginStr + "," + endStr;
        fd.download(urlStr, targetFilePath, true);
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

            // default 1 hour
            dataDelay = Integer.valueOf(prop.getProperty("delay", "60")).intValue() * 60000;
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
