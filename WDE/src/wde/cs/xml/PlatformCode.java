// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file PlatformCode.java
 */
package wde.cs.xml;

/**
 * Provides a means of reading platform code information from a StringBuilder
 * through the overriden method
 * {@link PlatformCode#characters(java.lang.StringBuilder)}
 * <p/>
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather and set platform
 * Code values.
 * </p>
 */
class PlatformCode extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code PlatformCode}. Initialization done through
     * base class {@link DataValue#init} method.
     * </p>
     */
    PlatformCode() {
    }

    /**
     * Sets the platform code for the base class instance of {@code XmlCollecotr}
     * to the integer value represented by the StringBuilder {@code sBuffer}.
     *
     * @param sBuffer the StringBuilder representing the sensor index integer
     *                value.
     */
    @Override
    public void characters(StringBuilder sBuffer) {
        m_oXmlCollector.setPlatformCode(sBuffer);
    }
}
