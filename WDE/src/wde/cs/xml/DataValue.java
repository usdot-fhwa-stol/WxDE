// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file DataValue.java
 */
package wde.cs.xml;

import wde.dao.ObsTypeDao;
import wde.dao.UnitConv;
import wde.dao.Units;
import wde.metadata.ObsType;
import wde.util.Text;

/**
 * Provides a method to gather data, check if it is valid, convert it to the
 * proper units, and store the gathered Data.
 */
public class DataValue {
    /**
     * Observation type identifier.
     */
    protected int m_nObsType;
    /**
     * Sensor index.
     */
    protected int m_nSensorIndex;
    /**
     * Observation value read in from Xml file.
     */
    protected double m_dValue = Double.NaN;
    /**
     * Conversion Multiplier.
     */
    protected double m_dMultiplier = 1.0;
    /**
     * Values to ignore
     */
    protected String[] m_sIgnoreValues;
    /**
     * Xml Collector object to which this DataValue belongs.
     */
    protected XmlCollector m_oXmlCollector;
    /**
     * Unit Converter.
     */
    protected UnitConv m_oUnitConv;
    /**
     * Path to corresponding data in the Xml file.
     * <p/>
     * <p>
     * Public to other classes in this Package.
     * </p>
     */
    String m_sPath;

    /**
     * <b> Default Constructor </b>
     * <p/>
     * <p>
     * Creates new instances of {@code DataValue}. Call the
     * {@link DataValue#init} method for initialization.
     * </p>
     */
    protected DataValue() {
    }

    /**
     * Initializes attribute values. Gets conversion data based off the given
     * unit identifier string, and the observation type id. Creates array of
     * values to be ignored by splitting {@code sIgnoreValues} by ';'.
     * <p/>
     * <p>
     * {@code init} should be called before {@code DataValue} objects are used.
     * Parameters should be values passed in from "xmldef" table in the database.
     * </p>
     *
     * @param nObsTypeId    observation type identifier.
     * @param dMultiplier   conversion multiplier.
     * @param sUnit         unit type.
     * @param sIgnoreValues values to ignore
     * @param sPath         path to corresponding data in the Xml file.
     * @param oXmlCollector Xml Collector.
     */
    void init(int nObsTypeId, double dMultiplier, String sUnit,
              String sIgnoreValues, String sPath, XmlCollector oXmlCollector) {
        m_sPath = sPath;

        m_oXmlCollector = oXmlCollector;
        m_nObsType = nObsTypeId;

        if (dMultiplier != 0.0) m_dMultiplier = dMultiplier;

        //get the destination unit for the obs type
        ObsType obsType = ObsTypeDao.getInstance().getObsType(nObsTypeId);
        // get a unit conversion based on the source and destination units
        if (obsType != null)
            m_oUnitConv = Units.getInstance().getConversion(sUnit,
                    obsType.getObsInternalUnit());

        if (sIgnoreValues != null && sIgnoreValues.length() > 0) {
            m_sIgnoreValues = sIgnoreValues.split(";");
            // trim whitespace
            int nIndex = m_sIgnoreValues.length;
            while (nIndex-- > 0)
                m_sIgnoreValues[nIndex] = m_sIgnoreValues[nIndex].trim();
        }
    }

    /**
     * May be overriden by classes that extend {@code DataValue} to perform
     * necessary initializations at the beginning of a data set.
     */
    public void start() {
    }

    /**
     * First removes anything from the end of {@code sBuffer} that is not a
     * digit. If {@code sBuffer} contains no data {@code m_dValue} is reset.
     * Makes sure decimal numbers less than one contain a 0 before the decimal.
     * If the value is not supposed to be ignored, sBuffer is parsed into a
     * double and stored in {@code m_dValue}.  The multiplier and the unit
     * conversion are applied to the value before it and the observation type
     * are passed back to XmlCollector to be stored and written later.
     *
     * @param sBuffer The string buffer containing the value.
     */
    public void characters(StringBuilder sBuffer) {
        // remove anything that is not a digit from the end of sBuffer
        int nIndex = sBuffer.length();
        while (nIndex-- > 0 && !Character.isDigit(sBuffer.charAt(nIndex)))
            sBuffer.deleteCharAt(nIndex);

        if (sBuffer.length() == 0) {
            // reset the value
            m_dValue = Double.NaN;
            return;
        }

        // replace -.n and .n with -0.n and 0.n for numbers
        if (sBuffer.charAt(0) == '.')
            sBuffer.insert(0, '0');
        else if (sBuffer.charAt(0) == '-' && sBuffer.charAt(1) == '.')
            sBuffer.insert(1, '0');

        try {
            if (!ignore(sBuffer.toString())) {
                if (m_oUnitConv == null) { // this could happen in the case of WxTelematics data

                    // not sure how to handle 10106
                    if (m_oXmlCollector.m_oLastEquipmentId.equals("10104")) {
                        m_oXmlCollector.setElev(sBuffer);
                    } else {
                        // get the ObsTypeUnit from equipment-id
                        ObsTypeUnit otu = EquipmentObsTypeMap
                                .getObsTypeUnit(m_oXmlCollector.m_oLastEquipmentId);
                        m_dMultiplier = otu.getMultiplier();

                        //get the destination unit for the obs type
                        ObsType obsType = ObsTypeDao.getInstance().getObsType(
                                otu.getObsTypeId());
                        m_nObsType = otu.getObsTypeId();

                        // get a unit conversion based on the source and destination units
                        if (obsType != null)
                            m_oUnitConv = Units.getInstance().getConversion(
                                    otu.getUnit(), obsType.getObsInternalUnit());

                        m_dValue = Text.parseDouble(sBuffer) * m_dMultiplier;
                        m_dValue = m_oUnitConv.convert(m_dValue);

                        // reset for the next time
                        m_oUnitConv = null;
                    }
                } else {
                    // apply the configured multiplier and convert the units
                    m_dValue = Text.parseDouble(sBuffer) * m_dMultiplier;
                    m_dValue = m_oUnitConv.convert(m_dValue);
                }

                // store the value and obsType in a Deque in XmlCollector
                m_oXmlCollector.addValueHolder(m_nObsType, m_dValue);
            }
        } catch (Exception oException) {
        }
    }

    /**
     * May be overriden by classes that extend {@code DataValue} to perform
     * necessary processes at the beginning of a data set traversal.
     * <p/>
     * <p>
     * May be overriden by classes that extend {@code DataValue}.
     * </p>
     */
    public void end() {
    }

    /**
     * Determines whether {@code iCharSeq} should be ignored by checking if is
     * contained in the ignore value list {@code m_sIgnoreValues}.
     *
     * @param iCharSeq value to be checked against the ignore list.
     * @return true if {@code iCharSeq} is contained in the ignore list.
     * <br /> false otherwise.
     */
    protected boolean ignore(CharSequence iCharSeq) {
        boolean bIgnore = false;

        if (m_sIgnoreValues != null) {
            int nIndex = m_sIgnoreValues.length;
            while (nIndex-- > 0 && !bIgnore)
                bIgnore = (Text.compare(m_sIgnoreValues[nIndex], iCharSeq) == 0);
        }

        return bIgnore;
    }
}
