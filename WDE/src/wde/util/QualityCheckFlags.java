/************************************************************************
 * Source filename: QualityCheckFlags.java
 * <p/>
 * Creation date: May 1, 2013
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

package wde.util;

public class QualityCheckFlags {

    private int runFlags;

    private int passFlags;

    public QualityCheckFlags(int rf, int pf) {
        runFlags = rf;
        passFlags = pf;
    }

    /**
     * @return the runFlags
     */
    public int getRunFlags() {
        return runFlags;
    }

    /**
     * @return the passFlags
     */
    public int getPassFlags() {
        return passFlags;
    }
}
