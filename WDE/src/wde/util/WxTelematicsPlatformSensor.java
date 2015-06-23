/************************************************************************
 * Source filename: WxTelematicsPlatformSensor.java
 * <p/>
 * Creation date: May 23, 2014
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

public class WxTelematicsPlatformSensor {
    static int[] obstypeids = {51139, 5733, 206, 581, 60000, 554, 5146, 543};
    static int[] qchparmids = {2671, 2672, 2673, 2674, 2675, 2676, 2677, 2678};

    public static void main(String[] sArgs) {
        // reserve [300000, 310000) for WxTelematics
        int staticid = 300000;
//        for (int pcode = 760; pcode <= 2743; pcode++) {
//            String sqlStr = String.format("insert into meta.platform " +
//                "(staticid, updatetime, platformcode, category, description, contribid, siteid) " +
//                "values(%d, '2014-05-23', '%d', 'M', 'Weather Telematics', 73, 5820);", staticid, pcode);
//            System.out.println(sqlStr);
//            staticid++;
//        }

        for (int pid = 7658; pid <= 9641; pid++) {
            staticid = 3000000 + 10 * (pid - 7658);
            for (int i = 0; i <= 7; i++) {
                String sqlStr = String.format("insert into meta.sensor " +
                        "(sourceid, staticid, updatetime, platformid, contribid, sensorindex, obstypeid, qchparmid, distgroup) " +
                        "values(1, %d, '2014-05-23', %d, 73, 0, %d, %d, 2);", staticid, pid, obstypeids[i], qchparmids[i]);
                System.out.println(sqlStr);
                staticid++;
            }
        }
    }
}
