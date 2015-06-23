/************************************************************************
 * Source filename: VDTController.java
 * <p/>
 * Creation date: Oct 24, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: VDT Integration
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import wde.dao.DatabaseManager;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.vdt.probe.ProbeAssembly;
import wde.vdt.wde.CsvDataIngester;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class VDTController extends TimerTask {

    private static final Logger logger = Logger.getLogger(VDTController.class);

    public static boolean wdeMgrInstantiated = false;
    public static String dataRoot = null;

    public static String fileDelimiter;
    public static String dateFormatStr;
    public static String timeFormatStr;

    private static VDTController instance = null;

    private ProbeAssembly probeAssembly = null;
    private VDTDataIngester vdtDataIngester = null;
    private CsvDataIngester csvDataIngester = null;

    private Properties prop = null;
    private int pollingInterval;
    private long dataDelay;
    private String vdtCommand = null;
    private File vdtCommandFolder = null;
    private String subscriptionFolder = null;
    private String subscriptionId = null;
    private String rawWxDEFolder = null;
    private SimpleDateFormat wxdeTimeFormat = null;
    private SimpleDateFormat wxdeDateFormat = null;
    private DatabaseManager db = null;
    private String connId = null;

    private Timer timer = null;

    private boolean isWindows;

    private VDTController() {
        String osStr = System.getProperty("os.name").toLowerCase();
        if (osStr.indexOf("win") >= 0)
            isWindows = true;
        else
            isWindows = false;

        prop = new Properties();
        loadPropertiesFile();

        String df = dateFormatStr + "_" + "HH00";
        wxdeTimeFormat = new SimpleDateFormat(df);
        wxdeDateFormat = new SimpleDateFormat(dateFormatStr);

        probeAssembly = ProbeAssembly.getInstance();
        vdtDataIngester = VDTDataIngester.getInstance();
        csvDataIngester = CsvDataIngester.getInstance();
        db = DatabaseManager.getInstance();
        connId = db.getConnection();

        timer = new Timer();

        long currentTime = System.currentTimeMillis();

        long delay = pollingInterval - (currentTime % pollingInterval) + dataDelay;

        logger.info("wait for " + delay / 1000 + " seconds before the first run");

        timer.scheduleAtFixedRate(this, delay, pollingInterval);
    }

    public static void main(String[] args) {
        //DOMConfigurator.configure("config/wde_log4j.xml");
        PropertyConfigurator.configure("config/wde_log4j.properties");
        VDTController.getInstance();
    }

    public static VDTController getInstance() {
        if (instance == null)
            instance = new VDTController();

        return instance;
    }

    public void run() {
        long time = System.currentTimeMillis();
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(time);

        try {
            Date currentDateTime = cal.getTime();
            String fileName = wxdeTimeFormat.format(currentDateTime);
            String separator = System.getProperty("file.separator");

            String subscriptionFilePath = subscriptionFolder + fileName + fileDelimiter + "csv";
            String wxdeOutputFileFolder = rawWxDEFolder + wxdeDateFormat.format(currentDateTime);
            String wxdeOutputFilePath = wxdeOutputFileFolder + separator + "wxde" + fileDelimiter + fileName + fileDelimiter + "nc";
            logger.info("subscriptionFilePath: " + subscriptionFilePath);
            logger.info("wxdeOutputFilePath: " + wxdeOutputFilePath);
            File f1 = new File(subscriptionFilePath);
            File f2 = new File(wxdeOutputFilePath);
            if (f1.exists() && !f2.exists()) {
                logger.info("Ingesting " + subscriptionFilePath);
                if (csvDataIngester.readCsvFile(subscriptionFilePath)) {

                    keepSubscriptionAlive();

                    new File(wxdeOutputFileFolder.toString()).mkdirs();
                    logger.info("Generating " + wxdeOutputFilePath);
                    csvDataIngester.writeNcFile(wxdeOutputFilePath);
                }
            }

            cal.setTimeInMillis(time - pollingInterval - dataDelay);
            // We won't be able to test the VDT on Windows
            if (!isWindows) {
                String[] filePaths = probeAssembly.generateMNProbeMessage(cal);

                if (filePaths != null) {

                    // Launch VDT to process
                    logger.info("Launching the VDT for MN");
                    Process proc = Runtime.getRuntime().exec(vdtCommand, null, vdtCommandFolder);

                    proc.waitFor();

                    moveFile(filePaths[0], filePaths[1]);

                    logger.info("VDT finished processing MN data");
                }
            }

            // But we can at least see whether the ingester works
            vdtDataIngester.processData(cal, "MN");

            if (!isWindows) {
                String[] filePaths = probeAssembly.generateMIProbeMessage(cal);

                if (filePaths != null) {

                    // Launch VDT to process
                    logger.info("Launching the VDT for MI");
                    Process proc = Runtime.getRuntime().exec(vdtCommand, null, vdtCommandFolder);

                    proc.waitFor();

                    moveFile(filePaths[0], filePaths[1]);

                    logger.info("VDT finished processing MI data");
                }
            }

            vdtDataIngester.processData(cal, "MI");

            // Clean up observation older than one hour
            long currentTime = cal.getTimeInMillis();
            if (currentTime % 3600000 < 10000) // 10 seconds within the hour
                vdtDataIngester.cleanup(currentTime - 3600000);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");

        if (!wdeMgrInstantiated) {

            String separator = System.getProperty("file.separator");
            String path = System.getProperty("user.dir") + separator + "config" + separator + "vdt_config.properties";

            try {
                FileInputStream fis = new FileInputStream(path);
                prop.load(fis);
                fis.close();

                // default 1 minutes for testing
                pollingInterval = Integer.valueOf(prop.getProperty("pollinginterval", "1")).intValue() * 60000;

                // default 5 seconds
                dataDelay = Integer.valueOf(prop.getProperty("delay", "5")).intValue() * 1000;

                // Command to launch the VDT
                vdtCommand = prop.getProperty("vdtcommand");
                String cmdFolder = prop.getProperty("vdtcommandfolder");

                logger.info("vdtcommandfolder is " + cmdFolder);
                vdtCommandFolder = new File(cmdFolder);

                fileDelimiter = prop.getProperty("filedelimiter");
                dateFormatStr = prop.getProperty("dateformat");
                timeFormatStr = prop.getProperty("timeformat");
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        } else {
            ConfigSvc configSvc = ConfigSvc.getInstance();
            Config config = configSvc.getConfig(this);

            pollingInterval = config.getInt("pollinginterval", 1) * 60000;
            dataDelay = config.getInt("delay", 5) * 1000;
            vdtCommand = config.getString("vdtcommand", "");

            String cmdFolder = config.getString("vdtcommandfolder", "");
            logger.info("vdtcommandfolder is " + cmdFolder);
            vdtCommandFolder = new File(cmdFolder);

            subscriptionFolder = config.getString("subscriptionfolder", "");
            logger.info("subscriptionFolder is " + subscriptionFolder);
            int startIndex = subscriptionFolder.indexOf("subscriptions") + 14;
            subscriptionId = subscriptionFolder.substring(startIndex, startIndex + 10);
            rawWxDEFolder = config.getString("rawwxde", "");

            fileDelimiter = config.getString("filedelimiter", "");
            dateFormatStr = config.getString("dateformat", "");
            timeFormatStr = config.getString("timeformat", "");

            ProbeAssembly.wdeMgrInstantiated = true;
            VDTDataIngester.wdeMgrInstantiated = true;
        }
    }

    private void moveFile(String sourcePath, String targetPath) {
        try {
            File f = new File(sourcePath);
            if (!f.renameTo(new File(targetPath)))
                logger.error("Failed to move " + sourcePath + " to " + targetPath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void keepSubscriptionAlive() {
        String sql = "UPDATE subs.subscription SET expires=? WHERE id=?";

        PreparedStatement ps = db.prepareStatement(connId, sql);
        Date now = new Date();
        now.setTime(System.currentTimeMillis() + 1209600000L); // New = 2 weeks

        try {
            ps.setTimestamp(1, new Timestamp(now.getTime()));
            ps.setInt(2, Integer.parseInt(subscriptionId));

            ps.execute();
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
}
