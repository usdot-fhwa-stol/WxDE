/************************************************************************
 * Source filename: SegmentAssessment.java
 * <p/>
 * Creation date: Nov 19, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt;

import org.apache.log4j.Logger;

public class SegmentAssessment extends NcMem {

    private static final Logger logger = Logger.getLogger(SegmentAssessment.class);

    public SegmentAssessment() {

        super();

        for (String attrName : attributeMap.keySet())
            addVariable(attrName, attributeMap.get(attrName));

        for (String varName : variableMap.keySet())
            addVariable(varName, variableMap.get(varName));
    }

    protected void loadVariableList() {

        logger.info("calling loadVariableList");

        attributeMap.put("road_segment_id", Integer.class);

        variableMap.put("all_hazards", Short.class);
        variableMap.put("pavement_condition", Short.class);
        variableMap.put("precipitation", Short.class);
        variableMap.put("visibility", Short.class);
    }
}
