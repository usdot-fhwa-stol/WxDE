/************************************************************************
 * Source filename: Site.java
 * <p/>
 * Creation date: Feb 23, 2013
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

import wde.dao.SiteDao;

public class Site extends TimeVariantMetadata {

    private String stateSiteId = null;

    private int contribId;

    private String description = null;

    private String roadwayDesc = null;

    private int roadwayMilepost;

    private float roadwayOffset;

    private float roadwayHeight;

    private String county = null;

    private String state = null;

    private String country = null;

    private String accessDirections = null;

    private String obstructions = null;

    private String landscape = null;

    private String stateSystemId = null;

    /**
     * @return the stateSiteId
     */
    public String getStateSiteId() {
        return stateSiteId;
    }

    /**
     * @param stateSiteId the stateSiteId to set
     */
    public void setStateSiteId(String stateSiteId) {
        this.stateSiteId = stateSiteId;
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
     * @return the roadwayDesc
     */
    public String getRoadwayDesc() {
        return roadwayDesc;
    }

    /**
     * @param roadwayDesc the roadwayDesc to set
     */
    public void setRoadwayDesc(String roadwayDesc) {
        this.roadwayDesc = roadwayDesc;
    }

    /**
     * @return the roadwayMilepost
     */
    public int getRoadwayMilepost() {
        return roadwayMilepost;
    }

    /**
     * @param roadwayMilepost the roadwayMilepost to set
     */
    public void setRoadwayMilepost(int roadwayMilepost) {
        this.roadwayMilepost = roadwayMilepost;
    }

    /**
     * @return the roadwayOffset
     */
    public float getRoadwayOffset() {
        return roadwayOffset;
    }

    /**
     * @param roadwayOffset the roadwayOffset to set
     */
    public void setRoadwayOffset(float roadwayOffset) {
        this.roadwayOffset = roadwayOffset;
    }

    /**
     * @return the roadwayHeight
     */
    public float getRoadwayHeight() {
        return roadwayHeight;
    }

    /**
     * @param roadwayHeight the roadwayHeight to set
     */
    public void setRoadwayHeight(float roadwayHeight) {
        this.roadwayHeight = roadwayHeight;
    }

    /**
     * @return the county
     */
    public String getCounty() {
        return county;
    }

    /**
     * @param county the county to set
     */
    public void setCounty(String county) {
        this.county = county;
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
     * @return the accessDirections
     */
    public String getAccessDirections() {
        return accessDirections;
    }

    /**
     * @param accessDirections the accessDirections to set
     */
    public void setAccessDirections(String accessDirections) {
        this.accessDirections = accessDirections;
    }

    /**
     * @return the obstructions
     */
    public String getObstructions() {
        return obstructions;
    }

    /**
     * @param obstructions the obstructions to set
     */
    public void setObstructions(String obstructions) {
        this.obstructions = obstructions;
    }

    /**
     * @return the landscape
     */
    public String getLandscape() {
        return landscape;
    }

    /**
     * @param landscape the landscape to set
     */
    public void setLandscape(String landscape) {
        this.landscape = landscape;
    }

    /**
     * @return the stateSystemId
     */
    public String getStateSystemId() {
        return stateSystemId;
    }

    /**
     * @param stateSystemId the stateSystemId to set
     */
    public void setStateSystemId(String stateSystemId) {
        this.stateSystemId = stateSystemId;
    }

    public void updateDbRecord(boolean atomic) {
        SiteDao.getInstance().updateSite(this, atomic);
    }

    public void updateMap() {
        SiteDao.getInstance().updateSiteMap();
    }
}
