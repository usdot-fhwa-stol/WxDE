/************************************************************************
 * Source filename: ActX.java
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

public class ActX extends Message {
 
    private static final Logger logger = Logger.getLogger(ActX.class);
    
    // Indices
    public static final int ign = 10;
    public static final int act1 = 11;
    public static final int act2 = 12;
    public static final int act3 = 13;
    public static final int act4 = 14;
    
    public String[] body = null;

    public ActX(String recvTime, String[] rawData) {
        super(recvTime, rawData);
        
        // For now, no additional parsing beyond the parent
        body = rawData;
    }
    
    @Override
    public String getField(int index)
    {
        if (index < ign || index > act4) {
            logger.error("Index out of bound");
            return null;
        }
        
        return body[index - OFFSET];
    }
}
