package wde.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.SimpleTimeZone;

public class DateTimeHelper {

    private static SimpleDateFormat timeFormatter1;
    private static SimpleDateFormat timeFormatter2;

    static {
        timeFormatter1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone stz1 = new SimpleTimeZone(-21600000, "USA/Central", Calendar.MARCH, 9, -Calendar.SUNDAY,
                7200000, Calendar.NOVEMBER, 3, -Calendar.SUNDAY, 7200000, 3600000);
        timeFormatter1.setTimeZone(stz1);

        timeFormatter2 = new SimpleDateFormat("MM/dd/yyyy HH:mm");
        SimpleTimeZone stz2 = new SimpleTimeZone(-18000000, "USA/Eastern", Calendar.MARCH, 9, -Calendar.SUNDAY,
                7200000, Calendar.NOVEMBER, 3, -Calendar.SUNDAY, 7200000, 3600000);
        timeFormatter2.setTimeZone(stz2);
    }

    /**
     * @return the timeFormatter1
     */
    public static SimpleDateFormat getTimeFormatter1() {
        return timeFormatter1;
    }

    /**
     * @return the timeFormatter2
     */
    public static SimpleDateFormat getTimeFormatter2() {
        return timeFormatter2;
    }
}
