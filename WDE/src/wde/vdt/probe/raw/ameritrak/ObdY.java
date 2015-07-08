/************************************************************************
 * Source filename: ObdY.java
 * <p/>
 * Creation date: Oct 4, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: VDT Integration
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt.probe.raw.ameritrak;

import org.apache.log4j.Logger;


public class ObdY extends AmeritrakMessage {

    // Indices
    public static final int ECT = 10;
    public static final int FPRS = 11;
    public static final int ERPM = 12;
    public static final int VSPD = 13;
    public static final int IAT = 14;
    public static final int ATP = 15;
    public static final int RTM1 = 16;
    public static final int FLVI = 17;
    public static final int BPRS = 18;
    public static final int AIRT = 19;
    public static final int EFRT = 20;
    public static final int BP = 21;
    public static final int TA = 22;
    public static final int LTAC = 23;
    public static final int LNAC = 24;
    public static final int SA = 25;
    public static final int YAW = 26;
    public static final int ROLL = 27;
    private static final Logger logger = Logger.getLogger(ObdY.class);
    public String[] body = null;

    public ObdY(String recvTime, String[] rawData) {
        super(recvTime, rawData);

        // For now, no additional parsing beyond the parent
        body = rawData;
    }

    @Override
    public String getField(int index) {
        if (index < ECT || index > ROLL) {
            logger.error("Index out of bound");
            return null;
        }

        if (body[index - OFFSET].equals("*")) {
            logger.debug("No value received for: " + index);
            return null;
        }

        return body[index - OFFSET];
    }
}
