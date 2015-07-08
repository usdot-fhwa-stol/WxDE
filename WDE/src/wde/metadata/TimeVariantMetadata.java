/************************************************************************
 * Source filename: TimeVariantMetadata.java
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

import wde.util.QueryString;

import java.lang.reflect.Field;
import java.sql.Timestamp;

public abstract class TimeVariantMetadata {

    private int id;

    private String staticId = null;

    private Timestamp updateTime;

    private Timestamp toTime;

    /**
     * @return the id
     */
    public int getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the staticId
     */
    public String getStaticId() {
        return staticId;
    }

    /**
     * @param staticId the staticId to set
     */
    public void setStaticId(String staticId) {
        this.staticId = staticId;
    }

    /**
     * @return the updateTime
     */
    public Timestamp getUpdateTime() {
        return updateTime;
    }

    /**
     * @param updateTime the updateTime to set
     */
    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }

    /**
     * @return the toTime
     */
    public Timestamp getToTime() {
        return toTime;
    }

    /**
     * @param toTime the toTime to set
     */
    public void setToTime(Timestamp toTime) {
        this.toTime = toTime;
    }

    /**
     * Uses reflection to avoid writing explicit static comparators for child classes.
     *
     * Advantage: Extensible and less coding
     * Disadvantage: Relative slow in performance
     *
     * Limitation: Works on only primitive fields.  May need to be extended to support arrays and composites
     * if the need arises in the future.
     *
     * Luckily the performance is not a key factor here since metadata don't change that much that often
     *
     * @param tm
     * @return true if the the two are equal and false otherwise
     */
    public boolean equals(TimeVariantMetadata tm) {
        if (!getStaticId().equals(tm.getStaticId())) {
//            System.out.println("The staticId is different");
//            System.exit(1);
            return false;
        }

        Field[] fields = tm.getClass().getDeclaredFields();
        try {
            for (Field f : fields) {
                f.setAccessible(true);
                Object thisValue = f.get(this);
                Object otherValue = f.get(tm);
                if (thisValue == null) {
                    if (otherValue != null && otherValue.toString().length() != 0) {
//                        System.out.println("thisValue is null but the otherValue is: " + otherValue.toString());
//                        System.exit(1);
                        return false;
                    }
                } else {
                    if (otherValue == null) {
//                        System.out.println("thisValue is not null but the otherValue is null");
//                        System.exit(1);
                        return false;
                    }
                    if (!QueryString.escapeSingleQuote(thisValue.toString()).equals(otherValue.toString().trim())) {
//                        System.out.println("thisValue: " + thisValue.toString() + " otherValue: " + otherValue.toString().trim());
//                        System.exit(1);
                        return false;
                    }
                }
            }
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }

        return true;
    }

    public abstract void updateDbRecord(boolean atomic);

    public abstract void updateMap();
}
