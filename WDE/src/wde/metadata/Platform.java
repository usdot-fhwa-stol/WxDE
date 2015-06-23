/************************************************************************
 * Source filename: Platform.java
 * <p/>
 * Creation date: Feb 25, 2013
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

import wde.dao.PlatformDao;

public class Platform extends TimeVariantMetadata implements IPlatform {

    private String platformCode = null;

    private char category;

    private String description = null;

    private int type;

    private int contribId;

    private int siteId;

    private double locBaseLat;

    private double locBaseLong;

    private double locBaseElev;

    private String locBaseDatum = null;

    private char powerType;

    private boolean doorOpen;

    private int batteryStatus;

    private int lineVolts;

    private int maintContactId;

    /**
     * @return the platformCode
     */
    public String getPlatformCode() {
        return platformCode;
    }

    /**
     * @param platformCode the platformCode to set
     */
    public void setPlatformCode(String platformCode) {
        this.platformCode = platformCode.trim();
    }

    /**
     * @return the category
     */
    public char getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(char category) {
        this.category = category;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the contribId
     */
    public int getContribId() {
        return contribId;
    }

    /**
     * @param contribId the contribId to set
     */
    public void setContribId(int contribId) {
        this.contribId = contribId;
    }

    /**
     * @return the siteId
     */
    public int getSiteId() {
        return siteId;
    }

    /**
     * @param siteId the siteId to set
     */
    public void setSiteId(int siteId) {
        this.siteId = siteId;
    }

    /**
     * @return the locBaseLat
     */
    public double getLocBaseLat() {
        return locBaseLat;
    }

    /**
     * @param locBaseLat the locBaseLat to set
     */
    public void setLocBaseLat(double locBaseLat) {
        this.locBaseLat = locBaseLat;
    }

    /**
     * @return the locBaseLong
     */
    public double getLocBaseLong() {
        return locBaseLong;
    }

    /**
     * @param locBaseLong the locBaseLong to set
     */
    public void setLocBaseLong(double locBaseLong) {
        this.locBaseLong = locBaseLong;
    }

    /**
     * @return the locBaseElev
     */
    public double getLocBaseElev() {
        return locBaseElev;
    }

    /**
     * @param locBaseElev the locBaseElev to set
     */
    public void setLocBaseElev(double locBaseElev) {
        this.locBaseElev = locBaseElev;
    }

    /**
     * @return the locBaseDatum
     */
    public String getLocBaseDatum() {
        return locBaseDatum;
    }

    /**
     * @param locBaseDatum the locBaseDatum to set
     */
    public void setLocBaseDatum(String locBaseDatum) {
        this.locBaseDatum = locBaseDatum;
    }

    /**
     * @return the powerType
     */
    public char getPowerType() {
        return powerType;
    }

    /**
     * @param powerType the powerType to set
     */
    public void setPowerType(char powerType) {
        this.powerType = powerType;
    }

    /**
     * @return the doorOpen
     */
    public boolean isDoorOpen() {
        return doorOpen;
    }

    /**
     * @param doorOpen the doorOpen to set
     */
    public void setDoorOpen(boolean doorOpen) {
        this.doorOpen = doorOpen;
    }

    /**
     * @return the batteryStatus
     */
    public int getBatteryStatus() {
        return batteryStatus;
    }

    /**
     * @param batteryStatus the batteryStatus to set
     */
    public void setBatteryStatus(int batteryStatus) {
        this.batteryStatus = batteryStatus;
    }

    /**
     * @return the lineVolts
     */
    public int getLineVolts() {
        return lineVolts;
    }

    /**
     * @param lineVolts the lineVolts to set
     */
    public void setLineVolts(int lineVolts) {
        this.lineVolts = lineVolts;
    }

    /**
     * @return the maintContactId
     */
    public int getMaintContactId() {
        return maintContactId;
    }

    /**
     * @param maintContactId the maintContactId to set
     */
    public void setMaintContactId(int maintContactId) {
        this.maintContactId = maintContactId;
    }

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return 0 in current implementation.
     */
    public int getSensorCount(int nObsType) {
        return 0;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        PlatformDao.getInstance().updatePlatform(this, atomic);
    }

    public void updateMap() {
        PlatformDao.getInstance().updatePlatformMap();
    }

    public int compareTo(IPlatform p) {
        //return p.getPlatformCode().compareTo(platformCode);
        String left = p.getCategory() + p.getPlatformCode();
        String right = category + platformCode;
        return left.compareTo(right);
    }
}
