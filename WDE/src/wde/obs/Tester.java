/************************************************************************
 * Source filename: Tester.java
 * <p/>
 * Creation date: Mar 22, 2013
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

package wde.obs;

import wde.dao.ObservationDao;

public class Tester {

    private static final String DIVIDER_PATTERN = "\\((|\\))|[\\))][,]";

    public static void main(String args[]) {
        if (args.length < 4) {
            System.out.println("Please provide start and end dates, and gridId and obsTypeId.");
            System.exit(-1);
        }
        ObservationDao od = ObservationDao.getInstance();
        od.getArchiveObs(args[0], args[1], args[2], args[3]);
//        String testStr = args[0];
//        testStr = testStr.substring(1, testStr.length()-1);
//        
//        String[] result = testStr.split(DIVIDER_PATTERN);
        System.out.println("pause");
    }
}
