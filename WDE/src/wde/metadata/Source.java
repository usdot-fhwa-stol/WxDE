/************************************************************************
 * Source filename: Source.java
 * <p/>
 * Creation date: Feb 21, 2013
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

import wde.dao.SourceDao;

public class Source extends TimeVariantMetadata {

    private String name = null;

    private String description = null;

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

    public void updateDbRecord(boolean atomic) {
        SourceDao.getInstance().updateSource(this, atomic);
    }

    public void updateMap() {
        SourceDao.getInstance().updateSourceMap();
    }
}
