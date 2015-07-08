/************************************************************************
 * Source filename: Image.java
 * <p/>
 * Creation date: Feb 24, 2013
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

import wde.dao.ImageDao;

public class Image extends TimeVariantMetadata {

    private int siteId;

    private String description = null;

    private String linkURL = null;


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
     * @return the linkURL
     */
    public String getLinkURL() {
        return linkURL;
    }

    /**
     * @param linkURL the linkURL to set
     */
    public void setLinkURL(String linkURL) {
        this.linkURL = linkURL;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void updateDbRecord(boolean atomic) {
        ImageDao.getInstance().updateImage(this, atomic);
    }

    public void updateMap() {
        ImageDao.getInstance().updateImageMap();
    }
}
