/************************************************************************
 * Source filename: MetadataUpdater.java
 * <p/>
 * Creation date: Feb 19, 2013
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

package wde.metadata;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import wde.dao.*;
import wde.util.QueryString;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

public class MetadataUpdater {

    private static final Logger logger = Logger.getLogger(MetadataUpdater.class);

    private Properties prop = null;

    private String separator = null;

    private String baseMetadataFolder = null;

    private MetadataDao md = null;

    private SimpleDateFormat sdf = null;

    private HashMap<String, TimeVariantMetadata> orgMap = null;

    private HashMap<String, TimeVariantMetadata> contactMap = null;

    private HashMap<String, TimeVariantMetadata> contribMap = null;

    private HashMap<String, TimeVariantMetadata> siteMap = null;

    private HashMap<String, TimeVariantMetadata> sensorTypeMap = null;

    private HashMap<String, TimeVariantMetadata> sourceMap = null;

    private HashMap<String, TimeInvariantMetadata> obsTypeMap = null;

    private HashMap<String, TimeVariantMetadata> platformMap = null;

    private HashMap<String, TimeInvariantMetadata> qchparmMap = null;

    private MetadataUpdater() {
        prop = new Properties();
        separator = System.getProperty("file.separator");
        md = MetadataDao.getInstance();
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S");
    }

    public static void main(String args[]) {
        DOMConfigurator.configure("config/wde_log4j.xml");

        MetadataUpdater mu = new MetadataUpdater();
        mu.loadPropertiesFile();
//        mu.updateOrganizationFromFile();
//        mu.updateContactFromFile();
//        mu.updateContribFromFile();
//        mu.updateSiteFromFile();
//        mu.updatePlatformFromFile();
//        mu.updateImageFromFile();
//        mu.updateSensorTypeFromFile();
//        mu.updateObsTypeFromFile();
//        mu.updateQchparmFromFile();
        mu.updateSensorFromFile2();
        logger.info("Completed processing");
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");
        boolean terminate = false;

        String path = System.getProperty("user.dir") + separator + "config" + separator + "metadata_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            baseMetadataFolder = prop.getProperty("basemetadatafolder");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (terminate)
            System.exit(-1);
    }

    public void updateOrganizationFromFile() {
        String filePath = baseMetadataFolder + separator + "organization.csv";
        Organization org = null;
        ArrayList<TimeVariantMetadata> orgs = new ArrayList<>();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                org = new Organization();
                String[] token = QueryString.parseCSVLine(str);
                org.setStaticId(token[0]);
                org.setName(token[1]);
                org.setLocation(token[2]);
                orgs.add(org);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        orgMap = OrganizationDao.getInstance().getOrgMap();
        md.updateAllTimeVariantRecords(orgs, orgMap, "organization");
    }

    public void updateContactFromFile() {
        String filePath = baseMetadataFolder + separator + "contact.csv";
        Contact contact = null;
        ArrayList<TimeVariantMetadata> contacts = new ArrayList<>();
        orgMap = OrganizationDao.getInstance().getOrgMap();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("id"))
                    continue;

                contact = new Contact();
                String[] token = QueryString.parseCSVLine(str);
                contact.setStaticId(token[0]);
                contact.setName(token[1]);
                contact.setTitle(token[2]);
                if (token[3].length() != 0) {
                    TimeVariantMetadata tvm = orgMap.get(token[3]);
                    if (tvm != null)
                        contact.setOrgId(tvm.getId());
                    else
                        logger.error("Encountered invalid ordId: " + token[3] + " in: " + str);
                }

                contact.setPhonePrimary(token[4]);
                contact.setPhoneAlt(token[5]);
                contact.setPhoneMobile(token[6]);
                contact.setFax(token[7]);
                contact.setEmail(token[10]);
                contact.setAddress1(token[12]);
                contact.setAddress2(token[13]);
                contact.setCity(token[14]);
                contact.setState(token[15]);
                contact.setZip(token[16]);
                contact.setCountry(token[17]);
                contacts.add(contact);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        contactMap = ContactDao.getInstance().getContactMap();
        md.updateAllTimeVariantRecords(contacts, contactMap, "contact");
    }

    public void updateContribFromFile() {
        String filePath = baseMetadataFolder + separator + "contrib.csv";
        Contrib contrib = null;
        ArrayList<TimeVariantMetadata> contribs = new ArrayList<>();
        orgMap = OrganizationDao.getInstance().getOrgMap();
        contactMap = ContactDao.getInstance().getContactMap();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                contrib = new Contrib();
                String[] token = QueryString.parseCSVLine(str);
                contrib.setStaticId(token[0]);
                TimeVariantMetadata tvm = null;
                if (token[1].length() != 0) {
                    tvm = orgMap.get(token[1]);
                    if (tvm != null)
                        contrib.setOrgId(tvm.getId());
                    else
                        logger.error("Encountered invalid ordId: " + token[1] + " in: " + str);
                }

                contrib.setName(token[2]);
                contrib.setAgency(token[3]);
                contrib.setMonitorHours(Integer.parseInt(token[4]));
                if (token[5].length() != 0) {
                    tvm = contactMap.get(token[5]);
                    if (tvm != null)
                        contrib.setContactId(tvm.getId());
                    else
                        logger.error("Encountered invalid contactId: " + token[5] + " in: " + str);
                }
                if (token[6].length() != 0) {
                    tvm = contactMap.get(token[6]);
                    if (tvm != null)
                        contrib.setAltContactId(tvm.getId());
                    else
                        logger.error("Encountered invalid altContactId: " + token[6] + " in: " + str);
                }
                if (token[7].length() != 0) {
                    tvm = contactMap.get(token[7]);
                    if (tvm != null)
                        contrib.setMetadataContactId(tvm.getId());
                    else
                        logger.error("Encountered invalid metadataContactId: " + token[7] + " in: " + str);
                }
                boolean display = (token[8].equals("1")) ? true : false;
                contrib.setDisplay(display);
                contrib.setDisclaimerLink(token[9]);
                contribs.add(contrib);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        contribMap = ContribDao.getInstance().getContribMap();
        md.updateAllTimeVariantRecords(contribs, contribMap, "contrib");
    }

    public void updateSiteFromFile() {
        String filePath = baseMetadataFolder + separator + "site.csv";
        Site site = null;
        ArrayList<TimeVariantMetadata> sites = new ArrayList<>();
        contribMap = ContribDao.getInstance().getContribMap();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                site = new Site();
                String[] token = QueryString.parseCSVLine(str);
                site.setStaticId(token[0]);
                site.setStateSiteId(token[1]);
                TimeVariantMetadata tvm = null;
                if (token[2].length() != 0) {
                    tvm = contribMap.get(token[2]);
                    if (tvm != null)
                        site.setContribId(tvm.getId());
                    else
                        logger.error("Encountered invalid contribId: " + token[2] + " in: " + str);
                }
                site.setDescription(token[3]);
                site.setRoadwayDesc(token[4]);
                if (token[5].length() != 0)
                    site.setRoadwayMilepost(Integer.parseInt(token[5]));
                if (token[6].length() != 0)
                    site.setRoadwayOffset(Float.parseFloat(token[6]));
                if (token[7].length() != 0)
                    site.setRoadwayHeight(Float.parseFloat(token[7]));
                site.setCounty(token[8]);
                site.setState(token[9]);
                site.setCountry(token[10]);
                site.setAccessDirections(token[11]);
                site.setObstructions(token[14]);
                site.setLandscape(token[15]);
                site.setStateSystemId(token[21]);
                sites.add(site);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        siteMap = SiteDao.getInstance().getSiteMap();
        md.updateAllTimeVariantRecords(sites, siteMap, "site");
    }

    public void updateImageFromFile() {
        String filePath = baseMetadataFolder + separator + "image.csv";
        Image image = null;
        ArrayList<TimeVariantMetadata> images = new ArrayList<>();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                image = new Image();
                String[] token = QueryString.parseCSVLine(str);
                image.setStaticId(token[0]);
                if (token[1].length() != 0)
                    image.setSiteId(Integer.parseInt(token[1]));
                image.setDescription(token[2]);
                image.setLinkURL(token[3]);
                images.add(image);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        ImageDao id = ImageDao.getInstance();
        md.updateAllTimeVariantRecords(images, id.getImageMap(), "image");
    }

    public void updateSensorTypeFromFile() {
        String filePath = baseMetadataFolder + separator + "sensorType.csv";
        SensorType sensorType = null;
        ArrayList<TimeVariantMetadata> sensorTypes = new ArrayList<>();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                sensorType = new SensorType();
                String[] token = QueryString.parseCSVLine(str);
                sensorType.setStaticId(token[0]);
                sensorType.setMfr(token[1]);
                sensorType.setModel(token[2]);
                sensorTypes.add(sensorType);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        sensorTypeMap = SensorTypeDao.getInstance().getSensorTypeMap();
        md.updateAllTimeVariantRecords(sensorTypes, sensorTypeMap, "sensorType");
    }

    public void updatePlatformFromFile() {
        String filePath = baseMetadataFolder + separator + "station.csv";
        Platform platform = null;
        ArrayList<TimeVariantMetadata> platforms = new ArrayList<>();
        contactMap = ContactDao.getInstance().getContactMap();
        siteMap = SiteDao.getInstance().getSiteMap();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                platform = new Platform();
                String[] token = QueryString.parseCSVLine(str);
                platform.setStaticId(token[0]);
                if (token[1].length() != 0)
                    platform.setPlatformCode(token[1]);
                if (token[2].length() != 0)
                    platform.setCategory(token[2].charAt(0));
                platform.setDescription(token[3]);
                if (token[4].length() != 0)
                    platform.setType(Integer.parseInt(token[4]));
                TimeVariantMetadata tvm = null;
                if (token[5].length() != 0) {
                    tvm = contribMap.get(token[5]);
                    if (tvm != null)
                        platform.setContribId(tvm.getId());
                    else
                        logger.error("Encountered invalid contribId: " + token[5] + " in: " + str);
                }
                tvm = siteMap.get(token[6]);
                if (tvm != null)
                    platform.setSiteId(tvm.getId());
                if (token[7].length() != 0)
                    platform.setLocBaseLat(Double.parseDouble(token[7]));
                if (token[8].length() != 0)
                    platform.setLocBaseLong(Double.parseDouble(token[8]));
                if (token[9].length() != 0)
                    platform.setLocBaseElev(Double.parseDouble(token[9]));
                platform.setLocBaseDatum(token[10]);
                if (token[11].length() != 0)
                    platform.setPowerType(token[11].charAt(0));
                boolean doorOpen = (token[12].equals("1")) ? true : false;
                platform.setDoorOpen(doorOpen);
                if (token[13].length() != 0)
                    platform.setBatteryStatus(Integer.parseInt(token[13]));
                if (token[14].length() != 0)
                    platform.setLineVolts(Integer.parseInt(token[14]));
                if (token[15].length() != 0) {
                    tvm = contactMap.get(token[15]);
                    if (tvm != null)
                        platform.setMaintContactId(tvm.getId());
                    else
                        logger.error("Encountered invalid maintContactId " + token[15] + " in: " + str);
                }

                platforms.add(platform);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        platformMap = PlatformDao.getInstance().getPlatformUpdateMap();
        md.updateAllTimeVariantRecords(platforms, platformMap, "platform");
    }

    public void updateObsTypeFromFile() {
        String filePath = baseMetadataFolder + separator + "obsType.csv";
        ObsType obsType = null;
        ArrayList<TimeInvariantMetadata> obsTypes = new ArrayList<>();
        sourceMap = SourceDao.getInstance().getSourceMap();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                obsType = new ObsType();

                String[] token = QueryString.parseCSVLine(str);
                obsType.setId(token[0]);
                obsType.setObsType(token[1]);
                obsType.setObs1204Unit(token[2]);
                obsType.setObsDesc(token[3]);
                obsType.setObsInternalUnit(token[4]);
                boolean active = (token[5].equals("1")) ? true : false;
                obsType.setActive(active);
                obsType.setObsEnglishUnit(token[6]);
                obsTypes.add(obsType);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        md.insertTimeInvariantRecords(obsTypes, "obsType");
    }

    public void updateQchparmFromFile() {
        String filePath = baseMetadataFolder + separator + "qchparm.csv";
        Qchparm qchparm = null;
        ArrayList<TimeInvariantMetadata> qchparms = new ArrayList<>();
        sensorTypeMap = SensorTypeDao.getInstance().getSensorTypeMap();
        obsTypeMap = ObsTypeDao.getInstance().getObsTypeMap();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("\"id"))
                    continue;

                qchparm = new Qchparm();
                String[] token = QueryString.parseCSVLine(str);
                qchparm.setId(token[0]);
                if (token[1].length() != 0) {
                    TimeVariantMetadata tvm = sensorTypeMap.get(token[1]);
                    if (tvm != null)
                        qchparm.setSensorTypeId(tvm.getId());
                    else
                        logger.error("Encountered invalid sensorType staticId " + token[1] + " in: " + str);
                }
                if (token[2].length() != 0) {
                    if (obsTypeMap.get(token[2]) != null)
                        qchparm.setObsTypeId(Integer.parseInt(token[2]));
                    else
                        logger.error("Encountered invalid obsType id " + token[2] + " in: " + str);
                }
                boolean isDefault = (token[3].equals("1")) ? true : false;
                qchparm.setDefault(isDefault);
                if (token[4].length() != 0)
                    qchparm.setMinRange(Float.parseFloat(token[4]));
                if (token[5].length() != 0)
                    qchparm.setMaxRange(Float.parseFloat(token[5]));
                if (token[6].length() != 0)
                    qchparm.setResolution(Float.parseFloat(token[6]));
                if (token[7].length() != 0)
                    qchparm.setAccuracy(Float.parseFloat(token[7]));
                if (token[8].length() != 0)
                    qchparm.setRatePos(Double.parseDouble(token[8]));
                if (token[9].length() != 0)
                    qchparm.setRateNeg(Double.parseDouble(token[9]));
                if (token[10].length() != 0)
                    qchparm.setRateInterval(Double.parseDouble(token[10]));
                if (token[11].length() != 0)
                    qchparm.setPersistInterval(Double.parseDouble(token[11]));
                if (token[12].length() != 0)
                    qchparm.setPersistThreshold(Double.parseDouble(token[12]));
                if (token[13].length() != 0)
                    qchparm.setLikeThreshold(Double.parseDouble(token[13]));
                qchparms.add(qchparm);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        md.insertTimeInvariantRecords(qchparms, "qchparm");
    }

    /**
     * Used to load sensor data from a csv file with the following header:
     * id,stationId,sensorIndex,obsTypeId,qchparmId,distGroup,nsOffset,ewOffset,elevOffset,surfaceOffset,
     * installDate,calibDate,maintDate,maintBegin,maintEnd,serial,embeddedMaterial,sensorLocation
     */
    public void updateSensorFromFile() {
        String filePath = baseMetadataFolder + separator + "sensors_new.csv";
        Sensor sensor = null;
        ArrayList<TimeVariantMetadata> sensors = new ArrayList<>();
        platformMap = PlatformDao.getInstance().getPlatformUpdateMap();
        sourceMap = SourceDao.getInstance().getSourceMap();
        obsTypeMap = ObsTypeDao.getInstance().getObsTypeMap();
        qchparmMap = QchparmDao.getInstance().getQchparmMap();
        DistGroupDao distGroupDao = DistGroupDao.getInstance();

        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            String str = null;
            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("id"))
                    continue;

                sensor = new Sensor();

                // need to set the sourceId
                sensor.setSourceId(sourceMap.get("1").getId());

                String[] token = QueryString.parseCSVLine(str);
                sensor.setStaticId(token[0]);
                TimeVariantMetadata tvm = null;
                if (token[1].length() != 0) {
                    tvm = platformMap.get(token[1]);
                    if (tvm != null) {
                        sensor.setPlatformId(tvm.getId());
                        sensor.setContribId(((Platform) tvm).getContribId());
                    } else
                        logger.error("Encountered invalid platformId " + token[1] + " in: " + str);
                }

                if (token[2].length() != 0)
                    sensor.setSensorIndex(Integer.parseInt(token[2]));
                TimeInvariantMetadata tim = null;
                if (token[3].length() != 0) {
                    tim = obsTypeMap.get(token[3]);
                    if (tim != null)
                        sensor.setObsTypeId(Integer.parseInt(token[3]));
                    else
                        logger.error("Encountered invalid obsTypeId " + token[3] + " in: " + str);
                }
                if (token[4].length() != 0) {
                    tim = qchparmMap.get(token[4]);
                    if (tim != null)
                        sensor.setQchparmId(Integer.parseInt(token[4]));
                    else
                        logger.error("Encountered invalid qchparmId " + token[4] + " in: " + str);
                }
                DistGroup distGroup = distGroupDao.getDistGroup(token[5]);
                if (distGroup != null)
                    sensor.setDistGroup(distGroup.getId());
                if (token[6].length() != 0)
                    sensor.setNsOffset(Float.parseFloat(token[6]));
                if (token[7].length() != 0)
                    sensor.setEwOffset(Float.parseFloat(token[7]));
                if (token[8].length() != 0)
                    sensor.setElevOffset(Float.parseFloat(token[8]));
                if (token[9].length() != 0)
                    sensor.setSurfaceOffset(Float.parseFloat(token[9]));
                Timestamp ts = null;
                Date d = null;
                if (token[10].length() != 0) {
                    d = sdf.parse(token[10]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setInstallDate(ts);
                if (token[11].length() != 0) {
                    d = sdf.parse(token[11]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setCalibDate(ts);
                if (token[12].length() != 0) {
                    d = sdf.parse(token[12]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setMaintDate(ts);
                if (token[13].length() != 0) {
                    d = sdf.parse(token[13]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setMaintBegin(ts);
                if (token[14].length() != 0) {
                    d = sdf.parse(token[14]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setMaintEnd(ts);
                sensor.setEmbeddedMaterial(token[16]);
                sensor.setSensorLocation(token[17]);
                sensors.add(sensor);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            t.printStackTrace();
        }

        SensorDao sd = SensorDao.getInstance();
//        md.updateAllTimeVariantRecords(sensors, sd.getSensorMap(), "meta.sensor");
        md.updateTimeVariantRecords(sensors, sd.getSensorMap(), "meta.sensor");
    }

    /**
     * Used to load sensor data from a csv file with the following header:
     * id, sourceId, staticId, updateTime, toTime, platformId, contribId, sensorIndex, obsTypeId,
     * qchparmId, distGroup, nsOffset, ewOffset, elevOffset, surfaceOffset, installDate, calibDate,
     * maintDate, maintBegin, maintEnd, embeddedMaterial, sensorLocation
     */
    public void updateSensorFromFile2() {
        String filePath = baseMetadataFolder + separator + "sensors_2013-12-16.csv";
        Sensor sensor = null;
        ArrayList<TimeVariantMetadata> sensors = new ArrayList<>();
        sourceMap = SourceDao.getInstance().getSourceMap();

        String str = null;
        try {
            BufferedReader file = new BufferedReader(new FileReader(filePath));

            while ((str = file.readLine()) != null) {

                // Skip empty line and the title line
                if (str.length() == 0 || str.startsWith("id"))
                    continue;

                sensor = new Sensor();

                String[] token = QueryString.parseCSVLine(str);
                sensor.setSourceId(Integer.parseInt(token[1]));
                sensor.setStaticId(token[2]);

                if (token[5].length() != 0) {
                    sensor.setPlatformId(Integer.parseInt(token[5]));
                    sensor.setContribId(Integer.parseInt(token[6]));
                }

                if (token[7].length() != 0)
                    sensor.setSensorIndex(Integer.parseInt(token[7]));

                sensor.setObsTypeId(Integer.parseInt(token[8]));
                sensor.setQchparmId(Integer.parseInt(token[9]));
                sensor.setDistGroup(Integer.parseInt(token[10]));
                if (token[11].length() != 0)
                    sensor.setNsOffset(Float.parseFloat(token[11]));
                if (token[12].length() != 0)
                    sensor.setEwOffset(Float.parseFloat(token[12]));
                if (token[13].length() != 0)
                    sensor.setElevOffset(Float.parseFloat(token[13]));
                if (token[14].length() != 0)
                    sensor.setSurfaceOffset(Float.parseFloat(token[14]));
                Timestamp ts = null;
                Date d = null;
                if (token[15].length() != 0) {
                    d = sdf.parse(token[15]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setInstallDate(ts);
                if (token[16].length() != 0) {
                    d = sdf.parse(token[16]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setCalibDate(ts);
                if (token[17].length() != 0) {
                    d = sdf.parse(token[17]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setMaintDate(ts);
                if (token[18].length() != 0) {
                    d = sdf.parse(token[18]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setMaintBegin(ts);
                if (token[19].length() != 0) {
                    d = sdf.parse(token[19]);
                    ts = new Timestamp(d.getTime());
                }
                sensor.setMaintEnd(ts);
                sensor.setEmbeddedMaterial(token[20]);
                sensor.setSensorLocation(token[21]);
                sensors.add(sensor);
            }
            file.close();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        } catch (Throwable t) {
            System.out.println(str);
            t.printStackTrace();
        }

        SensorDao sd = SensorDao.getInstance();
//        md.updateAllTimeVariantRecords(sensors, sd.getSensorMap(), "meta.sensor");
        md.updateTimeVariantRecords(sensors, sd.getSensorMap(), "meta.sensor");
    }
}