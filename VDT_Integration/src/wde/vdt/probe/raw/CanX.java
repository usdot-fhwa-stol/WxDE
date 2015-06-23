/************************************************************************
 * Source filename: CanX.java
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

import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class CanX extends Message {
    
    private static final Logger logger = Logger.getLogger(CanX.class);
    
    // Indices
    public static final int spnList = 10;
    
    public String[] body = null;

    public CanX(String recvTime, String[] rawData) {
        super(recvTime, rawData);
        
        // For now, no additional parsing beyond the parent
        body = rawData;
    }
    
    @Override
    public String getField(int index)
    {
        logger.info("getField() not implemented");
        return null;
    }
    
    public short getAbs()
    {
        short abs = -9999;
        
        String spnListStr = body[spnList - OFFSET];
        
        StringTokenizer st = new StringTokenizer(spnListStr, "|");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.startsWith("563")) {
                StringTokenizer st1 = new StringTokenizer(token);
                st1.nextToken();
                abs = Short.parseShort(st1.nextToken());
                break;
            }
        }
        return abs;
    }
}
