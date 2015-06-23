/************************************************************************
 * Source filename: MathUtil.java
 * <p/>
 * Creation date: Apr 22, 2013
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

public class MathUtil {

    /**
     * Converts {@code dValue} from its standard units to micro-units.
     * @param dValue value to convert to micro units.
     * @return the converted value.
     */
    public static int toMicro(double dValue) {
        return ((int) Math.round(dValue * 1000000.0));
    }

    /**
     * converts {@code nValue} from micro-units to its standard units.
     * @param nValue value to convert from micro units.
     * @return the converted value.
     */
    public static double fromMicro(int nValue) {
        return (((double) nValue) / 1000000.0);
    }
}
