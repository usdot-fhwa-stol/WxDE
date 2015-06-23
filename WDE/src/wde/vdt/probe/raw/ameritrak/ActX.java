/************************************************************************
 * Source filename: ActX.java
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


public class ActX extends AmeritrakMessage {

    // Indices
    public static final int ign = 10;
    public static final int act1 = 11;
    public static final int act2 = 12;
    public static final int act3 = 13;
    public static final int act4 = 14;
    private static final Logger logger = Logger.getLogger(ActX.class);
    public String[] body = null;

    public ActX(String recvTime, String[] rawData) {
        super(recvTime, rawData);

        // For now, no additional parsing beyond the parent
        body = rawData;
    }

    @Override
    public String getField(int index) {
        if (index < ign || index > act4) {
            logger.error("Index out of bound");
            return null;
        }

        return body[index - OFFSET];
    }
}
