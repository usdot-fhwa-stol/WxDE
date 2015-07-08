/************************************************************************
 * Source filename: Segment.java
 * <p/>
 * Creation date: Dec 18, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.metadata;

import wde.dao.ObsTypeDao;

public class Segment extends TimeInvariantMetadata {

    private int contribId;

    private int segmentId;

    private String segmentName = null;

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
     * @return the segmentId
     */
    public int getSegmentId() {
        return segmentId;
    }

    /**
     * @param segmentId the segmentId to set
     */
    public void setSegmentId(int segmentId) {
        this.segmentId = segmentId;
    }

    /**
     * @return the segmentName
     */
    public String getSegmentName() {
        return segmentName;
    }

    /**
     * @param segmentName the segmentName to set
     */
    public void setSegmentName(String segmentName) {
        this.segmentName = segmentName;
    }

    /**
     * Updates database record with value of this object
     * @see wxde.metadata.TimeVariantMetadata#updateDbRecord()
     */
    public void insertDbRecord(boolean atomic) {
        // TBD
    }

    public void updateMap() {
        ObsTypeDao.getInstance().updateObsTypeMap();
    }
}
