/************************************************************************
 * Source filename: QualityFlag.java
 * <p/>
 * Creation date: Feb 28, 2013
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

public class QualityFlag extends TimeVariantMetadata {

    private static final Logger logger = Logger.getLogger(QualityFlag.class);

    private int sourceId;

    private int qchCharFlagLen;

    private String[] qchFlagLabel = null;

    /**
     * @return the sourceId
     */
    public int getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId the sourceId to set
     */
    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    /**
     * @return the qchCharFlagLen
     */
    public int getQchCharFlagLen() {
        return qchCharFlagLen;
    }

    /**
     * @param qchCharFlagLen the qchCharFlagLen to set
     */
    public void setQchCharFlagLen(int qchCharFlagLen) {
        this.qchCharFlagLen = qchCharFlagLen;
    }

    /**
     * @return the qchFlagLabel
     */
    public String[] getQchFlagLabel() {
        return qchFlagLabel;
    }

    /**
     * @param qchFlagLabel the qchFlagLabel to set
     */
    public void setQchFlagLabel(String[] qchFlagLabel) {
        this.qchFlagLabel = qchFlagLabel;
    }

    public void updateDbRecord(boolean atomic) {
        logger.error("updateDbRecord() not implemented but expectedly invoked");
    }

    public void updateMap() {
        logger.error("updateMap() not implemented but expectedly invoked");
    }
}
