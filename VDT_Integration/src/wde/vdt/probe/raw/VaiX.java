/************************************************************************
 * Source filename: VaiX.java
 * 
 * Creation date: Oct 4, 2013
 * 
 * Author: zhengg
 * 
 * Project: VDT Integration
 * 
 * Objective:
 * 
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt.probe.raw;

import org.apache.log4j.Logger;

public class VaiX extends Message {

    private static final Logger logger = Logger.getLogger(VaiX.class);
    
    // Indices
    public static final int mode = 10;
    public static final int tempRoad = 11;
    public static final int tempAir = 12;
    public static final int dewPt = 13;
    public static final int humid = 14;
    
    public String[] body = null;

    public VaiX(String recvTime, String[] rawData) {
        super(recvTime, rawData);
        
        // For now, no additional parsing beyond the parent
        body = rawData;
    }
    
    @Override
    public String getField(int index)
    {
        if (index < mode || index > humid) {
            logger.error("Index out of bound");
            return null;
        }
        
        return body[index - OFFSET];
    }
}
