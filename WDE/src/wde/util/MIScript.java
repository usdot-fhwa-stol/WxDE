/************************************************************************
 * Source filename: MIScript.java
 * <p/>
 * Creation date: Mar 19, 2014
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

public class MIScript {
    public static void main(String[] sArgs) throws Exception {

        int[][] nTypes = {
                {0, 5733},
                {0, 554},
                {0, 2000005},
                {0, 2000008},
                {0, 2000004},
                {0, 2000002},
                {0, 2000009},
                {0, 2000012},
                {0, 51138},
                {0, 575},
                {1, 5733},
                {0, 581}
        };

        for (int nPlat = 6102; nPlat < 6185; nPlat++) {
            for (int nIndex = 0; nIndex < nTypes.length; nIndex++) {
                int nStaticId = (nPlat + 19899) * 100 + nIndex + 20;
                int nQch = 2649 + nIndex;

                String sSql = String.format(
                        "INSERT INTO meta.sensor "
                                + "(sourceid,staticid,updatetime,platformid,contribid,sensorindex,obstypeid,qchparmid,distgroup)"
                                + "VALUES (2,%d,'2014-03-19',%d,26,%d,%d,%d,2);",
                        nStaticId, nPlat, nTypes[nIndex][0], nTypes[nIndex][1],
                        nQch);

                System.out.println(sSql);
            }
            System.out.println();
        }
    }
}
