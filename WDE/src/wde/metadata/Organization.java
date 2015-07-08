/************************************************************************
 * Source filename: Organization.java
 * <p/>
 * Creation date: Feb 20, 2013
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

import wde.dao.OrganizationDao;

public class Organization extends TimeVariantMetadata {

    private String name = null;

    private String location = null;

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
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location the location to set
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        OrganizationDao.getInstance().updateOrganization(this, atomic);
    }

    public void updateMap() {
        OrganizationDao.getInstance().updateOrgMap();
    }
}
