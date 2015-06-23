/************************************************************************
 * Source filename: Contact.java
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

import wde.dao.ContactDao;

public class Contact extends TimeVariantMetadata {

    private String name = null;

    private String title = null;

    private int orgId;

    private String phonePrimary = null;

    private String phoneAlt = null;

    private String phoneMobile = null;

    private String fax = null;

    private String email = null;

    private String address1 = null;

    private String address2 = null;

    private String city = null;

    private String state = null;

    private String zip = null;

    private String country = null;

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
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

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
     * @return the phonePrimary
     */
    public String getPhonePrimary() {
        return phonePrimary;
    }

    /**
     * @param phonePrimary the phonePrimary to set
     */
    public void setPhonePrimary(String phonePrimary) {
        this.phonePrimary = phonePrimary;
    }

    /**
     * @return the phoneAlt
     */
    public String getPhoneAlt() {
        return phoneAlt;
    }

    /**
     * @param phoneAlt the phoneAlt to set
     */
    public void setPhoneAlt(String phoneAlt) {
        this.phoneAlt = phoneAlt;
    }

    /**
     * @return the phoneMobile
     */
    public String getPhoneMobile() {
        return phoneMobile;
    }

    /**
     * @param phoneMobile the phoneMobile to set
     */
    public void setPhoneMobile(String phoneMobile) {
        this.phoneMobile = phoneMobile;
    }

    /**
     * @return the fax
     */
    public String getFax() {
        return fax;
    }

    /**
     * @param fax the fax to set
     */
    public void setFax(String fax) {
        this.fax = fax;
    }

    /**
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * @return the address1
     */
    public String getAddress1() {
        return address1;
    }

    /**
     * @param address1 the address1 to set
     */
    public void setAddress1(String address1) {
        this.address1 = address1;
    }

    /**
     * @return the address2
     */
    public String getAddress2() {
        return address2;
    }

    /**
     * @param address2 the address2 to set
     */
    public void setAddress2(String address2) {
        this.address2 = address2;
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity(String city) {
        this.city = city;
    }

    /**
     * @return the state
     */
    public String getState() {
        return state;
    }

    /**
     * @param state the state to set
     */
    public void setState(String state) {
        this.state = state;
    }

    /**
     * @return the zip
     */
    public String getZip() {
        return zip;
    }

    /**
     * @param zip the zip to set
     */
    public void setZip(String zip) {
        this.zip = zip;
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        ContactDao.getInstance().updateContact(this, atomic);
    }

    public void updateMap() {
        ContactDao.getInstance().updateContactMap();
    }
}
