/************************************************************************
 * Source filename: Metadata.java
 * <p/>
 * Creation date: Mar 6, 2013
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

public abstract class TimeInvariantMetadata {

    private String id;

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    public void setId(String id) {
        this.id = id;
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
        if (!getId().equals(tm.getId()))
            return false;

        Field[] fields = tm.getClass().getDeclaredFields();
        try {
            for (Field f : fields) {
                f.setAccessible(true);
                Object thisValue = f.get(this);
                Object otherValue = f.get(tm);
                if (thisValue == null) {
                    if (otherValue != null && otherValue.toString().length() != 0)
                        return false;
                } else {
                    if (otherValue == null)
                        return false;

                    if (!QueryString.escapeSingleQuote(thisValue.toString()).equals(otherValue.toString().trim()))
                        return false;
                }
            }
        } catch (IllegalAccessException iae) {
            iae.printStackTrace();
        }

        return true;
    }

    public abstract void insertDbRecord(boolean atomic);

    public abstract void updateMap();
}
