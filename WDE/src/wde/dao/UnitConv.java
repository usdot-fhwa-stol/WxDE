// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file UnitConv.java
 */

package wde.dao;

/**
 * The {@code UnitConv} class provides an interface for unit converions and
 * comparisons. It is meant to be extended, with an overridden conversion
 * function that performs the corresponding conversion operation. This class
 * implements the Comparable Interface.
 */
public class UnitConv implements Comparable<UnitConv> {
    /**
     * Multiply factor.
     */
    protected double m_dMultiply = 1.0;
    /**
     * Division factor.
     */
    protected double m_dDivide = 1.0;
    /**
     * Addition factor.
     */
    protected double m_dAdd = 0.0;
    /**
     * Unit label corresponding to the units to be converted from.
     */
    protected String m_sFromUnit;
    /**
     * Unit label corresponding to the units to be converted to.
     */
    protected String m_sToUnit;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code UnitConv}. Non-default constructor
     * performs initializations.
     * </p>
     */
    UnitConv() {
    }

    /**
     * Sets the convert-to and convert-from labels to sFromUnit, and sToUnit
     * for a newly created instance of {@code UnitConv}.
     *
     * @param sFromUnit The new convert-from label.
     * @param sToUnit   The new convert-to label.
     */
    UnitConv(String sFromUnit, String sToUnit) {
        setLabels(sFromUnit, sToUnit);
    }

    /**
     * Sets the convert-to and convert-from labels to sFromUnit, and sToUnit.
     *
     * @param sFromUnit The new convert-from label.
     * @param sToUnit   The new convert-to label.
     */
    void setLabels(String sFromUnit, String sToUnit) {
        m_sFromUnit = sFromUnit;
        m_sToUnit = sToUnit;
    }

    /**
     * The {@code convert} method returns the value passed in. It is
     * meant to be the default conversion if no conversions can be found.
     * Extension of {@code UnitConv} perform standard, more useful overridden
     * conversion methods based off the conversion factors.
     *
     * @param dValue The value to be converted.
     * @return The newly converted value.
     */
    public double convert(double dValue) {
        return dValue;
    }


    /**
     * Compares the units by their labels to determine if they're the same.
     *
     * @param oUnitConv The units to compare to the base units.
     * @return 0 - if both the convert-to and convert-from labels of the base units
     * match those of oUnitConv.
     */
    public int compareTo(UnitConv oUnitConv) {
        int nReturn = m_sFromUnit.compareTo(oUnitConv.m_sFromUnit);
        if (nReturn != 0)
            return nReturn;

        return m_sToUnit.compareTo(oUnitConv.m_sToUnit);
    }
}
