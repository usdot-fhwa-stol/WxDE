/************************************************************************
 * Source filename: ObsTypeUnit.java
 * <p/>
 * Creation date: May 28, 2014
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

package wde.cs.xml;

public class ObsTypeUnit {
    private int obsTypeId;
    private String unit;
    private double multiplier;

    public ObsTypeUnit(int obsTypeId, String unit, double multiplier) {
        this.obsTypeId = obsTypeId;
        this.unit = unit;
        this.multiplier = multiplier;
    }

    /**
     * @return the obsTypeId
     */
    public int getObsTypeId() {
        return obsTypeId;
    }

    /**
     * @return the unit
     */
    public String getUnit() {
        return unit;
    }

    /**
     * @return the multiplier
     */
    public double getMultiplier() {
        return multiplier;
    }
}
