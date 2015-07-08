/************************************************************************
 * Source filename: ObservationCleanser.java
 * <p/>
 * Creation date: Mar 12, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective: Breaks the obs table into daily chunks to speed up later
 * processing
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.obs;

import org.apache.log4j.xml.DOMConfigurator;
import wde.dao.ObservationDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ObservationCleanser {

    private static final int NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000;

    /**
     * @param args
     */
    public static void main(String args[]) {
        if (args.length < 3) {
            System.out.println("Please provide start date, end date, and mode (1 - fix sensors, 2 - remove duplicates, 3 - vacuum tables, 4 - drop tables");
            System.exit(-1);
        }

        DOMConfigurator.configure("config/wde_log4j.xml");

        ObservationDao od = ObservationDao.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date workingDate0 = null;
        Date endDate0 = null;
        Date workingDate = null;
        Date endDate = null;
        int mode = 0;

        try {
            workingDate0 = sdf.parse(args[0]);
            endDate0 = sdf.parse(args[1]);
            mode = Integer.parseInt(args[2]);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        switch (mode) {

            case 0:
                System.out.println("Fixing sensor references");
                workingDate = workingDate0;
                endDate = endDate0;
                while (workingDate.getTime() <= endDate.getTime()) {
                    String dateStr = sdf.format(workingDate);
                    od.fixSensorIds(dateStr);
                    workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                }
                break;
            case 1:
                System.out.println("Detecting duplicates");
                workingDate = workingDate0;
                endDate = endDate0;
                while (workingDate.getTime() <= endDate.getTime()) {
                    String dateStr = sdf.format(workingDate);
                    od.detectDuplicates(dateStr);
                    workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                }
                break;
            case 2:
                System.out.println("Removing duplicates");
                workingDate = workingDate0;
                endDate = endDate0;
                while (workingDate.getTime() <= endDate.getTime()) {
                    String dateStr = sdf.format(workingDate);
                    od.removeDuplicates(dateStr);
                    workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                }
                break;
            case 3:
                System.out.println("Vacuuming tables");
                workingDate = workingDate0;
                endDate = endDate0;
                while (workingDate.getTime() <= endDate.getTime()) {
                    String dateStr = sdf.format(workingDate);
                    od.vacuumTable(dateStr);
                    workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                }
                break;
            case 4:
                System.out.println("Dropping tables");
                workingDate = workingDate0;
                endDate = endDate0;
                while (workingDate.getTime() <= endDate.getTime()) {
                    String dateStr = sdf.format(workingDate);
                    od.dropTable(dateStr);
                    workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
                }
                break;
            default:
                System.out.println("Unsupported mode: " + mode);
                break;
        }
    }
}
