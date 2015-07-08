/************************************************************************
 * Source filename: Region.java
 * <p/>
 * Creation date: Jun 18, 2014
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

import wde.qeds.PointRadius;

public class Region {

    /**
     * Splits the provided comma delimited string into the region bounds:
     * min-max latitude-longitude pairs. String should be of the form:
     * <blockquote>
     * min-lat, min-long, max-lat, max-long
     * </blockquote>
     * <pre>        or      </pre>
     * <blockquote>
     * lat, long, radius
     * </blockquote>
     * @param sRegion comma-delimited string containing the coordinates of the
     * region of interest, either of the form: min-lat, min-long, max-lat,
     * max-long or lat, long, radius.
     */
    public static double[] convert(String region) {
        // lat1, long1, lat2, long2
        double[] coordinates = new double[4];

        coordinates[0] = coordinates[1] = -Double.MAX_VALUE;
        coordinates[2] = coordinates[3] = Double.MAX_VALUE;

        String[] sCoordinates = region.split(",");

        if (sCoordinates.length == 4) {
            coordinates[0] = Double.parseDouble(sCoordinates[0]);
            coordinates[1] = Double.parseDouble(sCoordinates[1]);
            coordinates[2] = Double.parseDouble(sCoordinates[2]);
            coordinates[3] = Double.parseDouble(sCoordinates[3]);

            // swap coordinates if the minimum and maximum overlap
            double dTemp = 0.0;
            if (coordinates[0] > coordinates[2]) {
                dTemp = coordinates[0];
                coordinates[0] = coordinates[2];
                coordinates[2] = dTemp;
            }
            if (coordinates[1] > coordinates[3]) {
                dTemp = coordinates[1];
                coordinates[1] = coordinates[3];
                coordinates[3] = dTemp;
            }
        }

        if (sCoordinates.length == 3) {
            PointRadius pr = new PointRadius(sCoordinates);

            // the radius causes both coordinate pairs to be recalculated
            // using mean Earth circumference of 40,076 Km
            double dAdjustment = pr.m_dRadius * 360.0 / 40076.0;
            coordinates[0] = pr.m_dLat - dAdjustment;
            coordinates[1] = pr.m_dLng - dAdjustment;

            coordinates[2] = pr.m_dLat + dAdjustment;
            coordinates[3] = pr.m_dLng + dAdjustment;

            // must check for coordinate boundary conditions
            if (coordinates[0] < -90.0)
                coordinates[0] += 180.0;

            if (coordinates[0] > 90.0)
                coordinates[0] -= 180.0;

            if (coordinates[2] < -90.0)
                coordinates[2] += 180.0;

            if (coordinates[2] > 90.0)
                coordinates[2] -= 180.0;

            if (coordinates[1] <= -180.0)
                coordinates[1] += 360.0;

            if (coordinates[1] > 180.0)
                coordinates[1] -= 360.0;

            if (coordinates[3] <= -180.0)
                coordinates[3] += 360.0;

            if (coordinates[3] > 180.0)
                coordinates[3] -= 360.0;
        }
        return coordinates;
    }
}
