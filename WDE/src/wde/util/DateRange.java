/************************************************************************
 * Source filename: DateRange.java
 * <p/>
 * Creation date: Feb 26, 2013
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

package wde.util;

import java.sql.Timestamp;

public class DateRange {

    private Timestamp beginDate;

    private Timestamp endDate;

    /**
     * @return the beginDate
     */
    public Timestamp getBeginDate() {
        return beginDate;
    }

    /**
     * @param beginDate the beginDate to set
     */
    public void setBeginDate(Timestamp beginDate) {
        this.beginDate = beginDate;
    }

    /**
     * @return the endDate
     */
    public Timestamp getEndDate() {
        return endDate;
    }

    /**
     * @param endData the endDate to set
     */
    public void setEndDate(Timestamp endDate) {
        this.endDate = endDate;
    }

    public boolean inRange(Timestamp t) {
        if (t.getTime() > beginDate.getTime() && t.getTime() < endDate.getTime())
            return true;

        return false;
    }
}
