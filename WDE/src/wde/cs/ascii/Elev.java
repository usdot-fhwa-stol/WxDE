// Copyright (c) 2011 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Elev.java
 */

package wde.cs.ascii;

/**
 * Provides a means of reading elevation information from a string buffer
 * through the overriden method
 * {@link Elev#readData(java.lang.StringBuilder)}
 * <p/>
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare geo-coordinate positioning.
 * </p>
 */
class Elev extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Elev}
     * </p>
     */
    Elev() {
    }


    /**
     * Sets the elevation for the base classes instance of
     * {@code CsvCollector} to the integer value represented by the supplied
     * string buffer.
     * <p>
     * Wraps {@link CsvCollector#setElev(java.lang.StringBuilder)}
     * </p>
     *
     * @param sBuffer The string buffer representing the elevation position
     *                value.
     */
    @Override
    public void readData(StringBuilder sBuffer) {
        m_oCsvCollector.setElev(sBuffer);
    }
}
