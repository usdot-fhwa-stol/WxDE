/************************************************************************
 * Source filename: Contrib.java
 * <p/>
 * Creation date: Feb 22, 2013
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

import wde.dao.ContribDao;

public class Contrib extends TimeVariantMetadata {

    private int orgId;

    private String name = null;

    private String agency = null;

    private int monitorHours;

    private int contactId;

    private int altContactId;

    private int metadataContactId;

    private boolean display = true;

    private String disclaimerLink = null;

    /**
     * @return the orgId
     */
    public int getOrgId() {
        return orgId;
    }

    /**
     * @param orgId the orgId to set
     */
    public void setOrgId(int orgId) {
        this.orgId = orgId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the agency
     */
    public String getAgency() {
        return agency;
    }

    /**
     * @param agency the agency to set
     */
    public void setAgency(String agency) {
        this.agency = agency;
    }

    /**
     * @return the monitorHours
     */
    public int getMonitorHours() {
        return monitorHours;
    }

    /**
     * @param monitorHours the monitorHours to set
     */
    public void setMonitorHours(int monitorHours) {
        this.monitorHours = monitorHours;
    }

    /**
     * @return the contactId
     */
    public int getContactId() {
        return contactId;
    }

    /**
     * @param contactId the contactId to set
     */
    public void setContactId(int contactId) {
        this.contactId = contactId;
    }

    /**
     * @return the altContactId
     */
    public int getAltContactId() {
        return altContactId;
    }

    /**
     * @param altContactId the altContactId to set
     */
    public void setAltContactId(int altContactId) {
        this.altContactId = altContactId;
    }

    /**
     * @return the metadataContactId
     */
    public int getMetadataContactId() {
        return metadataContactId;
    }

    /**
     * @param metadataContactId the metadataContactId to set
     */
    public void setMetadataContactId(int metadataContactId) {
        this.metadataContactId = metadataContactId;
    }

    /**
     * @return the display
     */
    public boolean isDisplay() {
        return display;
    }

    /**
     * @param display the display to set
     */
    public void setDisplay(boolean display) {
        this.display = display;
    }

    /**
     * @return the disclaimerLink
     */
    public String getDisclaimerLink() {
        return disclaimerLink;
    }

    /**
     * @param disclaimerLink the disclaimerLink to set
     */
    public void setDisclaimerLink(String disclaimerLink) {
        this.disclaimerLink = disclaimerLink;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        ContribDao.getInstance().updateContrib(this, atomic);
    }

    public void updateMap() {
        ContribDao.getInstance().updateContribMap();
    }
}
