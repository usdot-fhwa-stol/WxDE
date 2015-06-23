/************************************************************************
 * Source filename: CoordinateConversion.java
 * <p/>
 * Creation date: March 13, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * Based on code developed by Sami Salkosuo from ibm:
 * www.ibm.com/developerworks/java/library/j-coordconvert/
 * <p/>
 * See http://www.ibm.com/developerworks/java/library/j-coordconvert/
 * for description on UTM grids
 ***********************************************************************/

package wde.util.coord;

public class CoordinateConversion {

    public CoordinateConversion() {

    }

    public static String latLon2UTM(int latitude, int longitude, boolean justGrid) {
        LatLon2UTM c = new LatLon2UTM();
        return c.convertLatLonToUTM(latitude / 1000000, longitude / 1000000, justGrid);
    }

    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Please provide latitude and longitude.");
            System.exit(-1);
        }

        CoordinateConversion cc = new CoordinateConversion();
        String utmStr = cc.latLon2UTM(Double.parseDouble(args[0]), Double.parseDouble(args[1]), true);

        System.out.println(utmStr);
    }

    public double[] utm2LatLon(String UTM) {
        UTM2LatLon c = new UTM2LatLon();
        return c.convertUTMToLatLong(UTM);
    }

    public String latLon2UTM(double latitude, double longitude, boolean justGrid) {
        LatLon2UTM c = new LatLon2UTM();
        return c.convertLatLonToUTM(latitude, longitude, justGrid);
    }

    public String latLon2MGRUTM(double latitude, double longitude) {
        LatLon2MGRUTM c = new LatLon2MGRUTM();
        return c.convertLatLonToMGRUTM(latitude, longitude);
    }

    public double[] mgrutm2LatLon(String MGRUTM) {
        MGRUTM2LatLon c = new MGRUTM2LatLon();
        return c.convertMGRUTMToLatLong(MGRUTM);
    }

    public double radianToDegree(double radian) {
        return radian * 180 / Math.PI;
    }
}
