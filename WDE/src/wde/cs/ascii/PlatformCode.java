// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package wde.cs.ascii;

/**
 * Provides a means of reading platform code information from a string buffer
 * through the overriden method
 * {@link PlatformCode#readData(java.lang.StringBuilder)}
 * <p/>
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare platform code observation values.
 * </p>
 */
class PlatformCode extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code PlatformCode}
     * </p>
     */
    PlatformCode() {
    }

    /**
     * Sets the sensor platform code for the base classes instance of
     * {@code CsvCollector} to the integer value represented by the supplied
     * string buffer.
     *
     * @param sBuffer The string buffer representing the platform code integer
     *                value.
     */
    @Override
    public void readData(StringBuilder sBuffer) {
        m_oCsvCollector.setPlatformCode(sBuffer);
    }
}
