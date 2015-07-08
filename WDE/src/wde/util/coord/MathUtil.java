/************************************************************************
 * Source filename: Math.java
 * <p/>
 * Creation date: Mar 13, 2013
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

package wde.util.coord;

public abstract class MathUtil {

    protected static void validate(double latitude, double longitude) {
        if (latitude < -90.0 || latitude > 90.0 || longitude < -180.0 || longitude >= 180.0) {
            throw new IllegalArgumentException(
                    "Legal ranges: latitude [-90,90], longitude [-180,180).");
        }
    }

    public double degreeToRadian(double degree) {
        return degree * Math.PI / 180;
    }

    protected double POW(double a, double b) {
        return Math.pow(a, b);
    }

    protected double SIN(double value) {
        return Math.sin(value);
    }

    protected double COS(double value) {
        return Math.cos(value);
    }

    protected double TAN(double value) {
        return Math.tan(value);
    }
}
