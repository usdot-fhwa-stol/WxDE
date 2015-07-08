/************************************************************************
 * Source filename: InputProbeMessage.java
 * 
 * Creation date: Oct 18, 2013
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

package wde.vdt.probe;

import org.apache.log4j.Logger;

public class InputProbeMessage extends ProbeMessage {
    
    private static final Logger logger = Logger.getLogger(InputProbeMessage.class);
    
    public InputProbeMessage() {

        super();

        for (String varName: variableMap.keySet())
            addVariable(varName, variableMap.get(varName));
    }
    
    protected void loadVariableList() {
        
        logger.info("calling loadVariableList");
        
        super.loadVariableList();
        
        variableMap.put("psn", Integer.class);
        variableMap.put("source_id", String.class);
        variableMap.put("tire_pressure_lf", Short.class);
        variableMap.put("tire_pressure_rf", Short.class);        
        variableMap.put("tire_pressure_lr", Short.class); 
        variableMap.put("tire_pressure_rr", Short.class);
        variableMap.put("tire_pressure_sp", Short.class);
    }
    
    
    public static void main(String[] args) {
        
        InputProbeMessage pm = new InputProbeMessage();
        pm.registerObsTimeVname("z2", "a1");
        pm.registerObsTimeVname("s4", "a2");
        pm.registerObsTimeVname("t1", "a3");
        pm.registerObsTimeVname("w1", "a4");
        pm.registerObsTimeVname("h1", "a5");
        pm.updateValue("obs_time", "z2", 1.0);
        pm.updateValue("obs_time", "s4", 2.0);
        pm.updateValue("obs_time", "t1", 3.0);
        pm.updateValue("obs_time", "w1", 4.0);
        pm.updateValue("obs_time", "h1", 5.0);
        pm.print();  
    }

}
