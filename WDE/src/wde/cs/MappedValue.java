// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file MappedValue.java
 */

package wde.cs;

/**
 * Maps a value to an observation type, and label key.
 */
public class MappedValue {
    /**
     * Observation type.
     */
    int m_nObsType;
    /**
     * The value being mapped to the Observation type and label
     */
    double m_dValue;
    /**
     * The label for this observation type.
     */
    CharSequence m_sLabel;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code MappedValue}
     * </p>
     */
    MappedValue() {
    }

    /**
     * <b> Copy Constructor </b>
     * <p/>
     * <p>
     * Copies {@code oMappedValue} to a new instance of {@code MappedValue}.
     * </p>
     *
     * @param oMappedValue The target of the new instance of
     *                     {@code MappedValue}.
     */
    MappedValue(MappedValue oMappedValue) {
        m_nObsType = oMappedValue.m_nObsType;
        m_dValue = oMappedValue.m_dValue;
        m_sLabel = oMappedValue.m_sLabel;
    }

    /**
     * Creates a new instance of {@code MappedValue} containing the supplied
     * paremters.
     *
     * @param nObsType The Observation type contained in this
     *                 {@code MappedValue}
     * @param sLabel   The label assigned to this obseration type.
     * @param dValue   The value mapped to the Key (nObsType, sLabel).
     */
    MappedValue(int nObsType, CharSequence sLabel, double dValue) {
        setKey(nObsType, sLabel);
        m_dValue = dValue;
    }

    /**
     * Sets the "key" of the mapped value.
     *
     * @param nObsType Observation type.
     * @param sLabel   Label for this observation type.
     */
    void setKey(int nObsType, CharSequence sLabel) {
        m_nObsType = nObsType;
        m_sLabel = sLabel;
    }
}
