/************************************************************************
 * Source filename: RecordCounter.java
 * <p/>
 * Creation date: Mar 2, 2013
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

package wde.dao;

public class RecordCounter {

    private int insertCounter;

    private int updateCounter;

    /**
     * @return the insertCounter
     */
    public int getInsertCounter() {
        return insertCounter;
    }

    /**
     * @param insertCounter the insertCounter to set
     */
    public void setInsertCounter(int insertCounter) {
        this.insertCounter = insertCounter;
    }

    /**
     * @return the updateCounter
     */
    public int getUpdateCounter() {
        return updateCounter;
    }

    /**
     * @param updateCounter the updateCounter to set
     */
    public void setUpdateCounter(int updateCounter) {
        this.updateCounter = updateCounter;
    }

}
