/************************************************************************
 * Source filename: ClarusObsFileDbLoader.java
 * <p/>
 * Creation date: Feb 27, 2013
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
import wde.dao.*;
import wde.metadata.*;
import wde.obs.Observation;

import java.io.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;


public class ClarusObsFileDbLoader {
    private static final Logger logger = Logger.getLogger(ClarusObsFileDbLoader.class);
    private static final int NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000;
    private static final int TBD_CONF_VALUE = -9999;
    private static ClarusObsFileDbLoader instance = null;
    private Properties prop = null;
    private String baseDataFolder = null;
    private String baseWorkingFolder = null;
    private boolean timeVariant = false;
    private String startFolderDate = null;
    private String endFolderDate = null;
    private SimpleDateFormat sdf = null;

    /**
     *
     */
    private ClarusObsFileDbLoader() {
        prop = new Properties();
        loadPropertiesFile();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    }

    /**
     * @return a reference to the ClarusObsFileDbLoader singleton.
     */
    public static ClarusObsFileDbLoader getIntance() {
        if (instance == null)
            instance = new ClarusObsFileDbLoader();

        return instance;
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Please provide start and end folder dates.");
            System.exit(-1);
        }
        DOMConfigurator.configure("config/wde_log4j.xml");
        ClarusObsFileDbLoader cofdl = ClarusObsFileDbLoader.getIntance();
        cofdl.setStartFolderDate(args[0]);
        cofdl.setEndFolderDate(args[1]);
        cofdl.process();
    }

    /**
     * @param startFolderDate the startFolderDate to set
     */
    public void setStartFolderDate(String startFolderDate) {
        this.startFolderDate = startFolderDate;
    }

    /**
     * @param endFolderDate the endFolderDate to set
     */
    public void setEndFolderDate(String endFolderDate) {
        this.endFolderDate = endFolderDate;
    }

    /**
     *
     */
    public void process() {
        String timeFormat = "yyyyMMdd";

        SimpleDateFormat timeFormatter = new SimpleDateFormat(timeFormat);
        Date workingDate = null;
        Date endDate = null;

        try {
            workingDate = timeFormatter.parse(startFolderDate);
            endDate = timeFormatter.parse(endFolderDate);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }
        String separator = System.getProperty("file.separator");
        int insertCounter = 0;
        int updateCounter = 0;
        while (workingDate.getTime() <= endDate.getTime()) {
            String currentSourceFolder = timeFormatter.format(workingDate);
            String currentSourceFolderPath = baseDataFolder + separator + currentSourceFolder;
            File dir = new File(currentSourceFolderPath);
            File outputDir = new File(baseWorkingFolder);
            try {
                logger.info("Uncompressing files in: " + currentSourceFolderPath);
                for (File child : dir.listFiles())
                    FileCompressor.uncompressFile(child, outputDir);
            } catch (IOException e) {
                e.printStackTrace();
            }

            File[] files = outputDir.listFiles();
            Arrays.sort(files);
            for (int i = 0; i < files.length; i++) {
                // load data
                RecordCounter rc = loadDataFromFile(files[i]);

                if (rc != null) {
                    insertCounter += rc.getInsertCounter();
                    updateCounter += rc.getUpdateCounter();
                }

                // remove the file
                files[i].delete();
            }

            logger.info("****Inserted " + insertCounter + " records from " + currentSourceFolderPath);
            logger.info("****Updated " + updateCounter + " records from " + currentSourceFolderPath);

            // progress one day
            workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
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
            baseWorkingFolder = prop.getProperty("baseworkingfolder");
            timeVariant = Boolean.valueOf(prop.getProperty("timevariant"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }


    /**
     * Note once loaded the following SQL command shound be executed to populate the contribId field:
     *
     *    update meta.sensor set contribId = p.contribId from meta.platform p where platformId=p.id
     *
     * @param f
     * @return
     */
    private RecordCounter loadDataFromFile(File f) {
        logger.info("loading data from " + f.getName());

        HashMap<String, TimeVariantMetadata> sm = SensorDao.getInstance().getSensorMap();
        HashMap<String, TimeInvariantMetadata> om = ObsTypeDao.getInstance().getObsTypeMap();
        RecordCounter rc = null;

        try {
            BufferedReader file = new BufferedReader(new FileReader(f));
            Observation obs = null;
            HashMap<String, Observation> obsMap = new HashMap<>();

            // Default to an invalid id.  Once entering the while loop, it should be set to a fixed value
            int sourceId = -1;

            String str = null;
            Timestamp now = new Timestamp(Calendar.getInstance().getTimeInMillis());
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("ObsTypeID") || str.startsWith("null") || str.startsWith("END OF RECORDS"))
                    continue;

                obs = new Observation();
                String[] token = QueryString.parseCSVLine(str);

                if (token[0].length() == 0) {
                    logger.error("Observation references empty obsTypeId: " + str);
                    continue;
                }

                TimeInvariantMetadata tim = om.get(token[0]);

                if (tim != null)
                    obs.setObsTypeId(Integer.valueOf(tim.getId()));
                else {
                    logger.error("Observation references invalid ObsType: " + str);
                    continue;
                }

                // skipping ObsTypeName

                boolean moreParsing = false;
                TimeVariantMetadata tvm = null;
                Sensor sensor = null;

                if (token[2].length() == 0) {
                    logger.error("Observation references empty sensorId: " + str);
                    moreParsing = true;
                } else {
                    if (timeVariant)
                        obs.setSensorId(Integer.valueOf(token[2]));
                    else {
                        tvm = sm.get(token[2]);

                        if (tvm != null) {
                            obs.setSensorId(Integer.valueOf(tvm.getId()));
                            sensor = (Sensor) tvm;
                            sourceId = sensor.getSourceId();
                        } else {
                            logger.error("Observation references invalid sensorId: " + str);
                            logger.info("Attempting to derive sensorId from platformId, sensorIndex and obsTypeId");
                            moreParsing = true;
                        }
                    }
                }

                // If the sensorId is either empty or not found, need to derive that from platformId, sensorIndex and obsTypeId 
                if (moreParsing) {
                    // token[3] for sensorIndex, token[4] for platformId
                    int platformId;
                    int sensorIndex;
                    if (token[4].length() != 0) {
                        if (timeVariant) {
                            platformId = Integer.valueOf(token[4]);
                        } else {
                            tvm = PlatformDao.getInstance().getPlatformForStaticId(token[4]);
                            if (tvm == null) {
                                logger.error("Observation references invalid platformId: " + token[4] + " - ignore observation record");
                                continue;
                            }
                            platformId = tvm.getId();
                        }
                    } else {
                        logger.error("Observation references empty platformId - ignore observation record");
                        continue;
                    }

                    if (token[3].length() != 0) {
                        sensorIndex = Integer.parseInt(token[3]);
                    } else {
                        logger.error("Observation references empty sensorIndex - ignore observation record");
                        continue;
                    }
                    ISensor iSensor = SensorDao.getInstance().getSensor(platformId, obs.getObsTypeId(), sensorIndex);
                    if (iSensor != null) {
                        obs.setSensorId(iSensor.getId());
                        sourceId = iSensor.getSourceId();
                    } else {
                        logger.error("Observation references invalid combination of obsTypeId, platformId and sensorIndex - ignore observation record");
                        continue;
                    }
                }

                // skipping ClarusSensorIndex,ClarusStationID,ClarusSiteID,ClarusClimateID,ClarusContribID,Contributor,StationCode

                Timestamp ts = null;
                if (token[10].length() != 0) {
                    Date d = sdf.parse(token[10]);
                    ts = new Timestamp(d.getTime());
                }
                obs.setObsTime(ts);
                obs.setRecvTime(now);

                obs.setSourceId(sourceId);

                obs.setLatitude(MathUtil.toMicro(Double.valueOf(token[11])));
                obs.setLongitude(MathUtil.toMicro(Double.valueOf(token[12])));

                // A bug - should not have converted to micros, need to fix values in DB
//                obs.setElevation((int) (Double.valueOf(token[13])*NUM_OF_MICRO_DEGREES)); 
                obs.setElevation((int) (Double.parseDouble(token[13])));
                obs.setValue(Double.valueOf(token[14]));

                // skipping Units,EnglishValue,EnglishUnits

                float confValue = (token[18].equals("?")) ? TBD_CONF_VALUE : Float.valueOf(token[18]);
                obs.setConfValue(confValue);

                QualityFlag qf = QualityFlagDao.getInstance().getQualityFlag(sourceId, ts);

                int charFlagLen = qf.getQchCharFlagLen();
                char[] qchCharFlag = new char[charFlagLen];

                for (int i = 0; i < charFlagLen; i++)
                    qchCharFlag[i] = token[19 + i].charAt(0);

                obs.setQchCharFlag(qchCharFlag);
                String key = String.valueOf(obs.getObsTypeId()) + "," + String.valueOf(obs.getSensorId()) + "," + obs.getObsTime();

                obsMap.put(key, obs);
            }
            file.close();

            if (sourceId != -1)
                rc = ObservationDao.getInstance().insertObservations(obsMap.values());

        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.error(ioe.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
            logger.error(t.getMessage());
        }

        return rc;
    }
}
