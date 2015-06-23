/************************************************************************
 * Source filename: ObservationArchiver.java
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

package wde.obs;

import org.apache.log4j.xml.DOMConfigurator;
import wde.dao.ObsTypeDao;
import wde.dao.ObservationDao;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;

public class ObservationArchiver {

    private static final long NUM_OF_MILLI_SECONDS_IN_A_DAY = 86400000L;
    private static ObservationArchiver instance = null;
    private Set<String> obsTypeIds = null;

    private ObservationArchiver() {
        obsTypeIds = ObsTypeDao.getInstance().getObsTypeMap().keySet();
    }

    /**
     * @return a reference to the ClarusObsFileDbLoader singleton.
     */
    public static ObservationArchiver getIntance() {
        if (instance == null)
            instance = new ObservationArchiver();

        return instance;
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        if (args.length < 2) {
            System.out.println("Please provide start and end dates.");
            System.exit(-1);
        }

        DOMConfigurator.configure("config/wde_log4j.xml");

        ObservationDao od = ObservationDao.getInstance();
        ObservationArchiver oa = new ObservationArchiver();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Date workingDate = null;
        Date endDate = null;

        try {
            workingDate = sdf.parse(args[0]);
            endDate = sdf.parse(args[1]);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        String dateStr1 = null;
        String dateStr2 = sdf.format(workingDate);
        while (workingDate.getTime() <= endDate.getTime()) {
            dateStr1 = dateStr2;
            workingDate.setTime(workingDate.getTime() + NUM_OF_MILLI_SECONDS_IN_A_DAY);
            dateStr2 = sdf.format(workingDate);

            od.archiveObservations(dateStr1, dateStr2, oa.getObsTypeIds());
        }
    }

    /**
     * @return the obsTypeIds
     */
    public Set<String> getObsTypeIds() {
        return obsTypeIds;
    }
}
