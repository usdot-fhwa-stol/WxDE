// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Timestamp.java
 */

package wde.cs.ascii;

/**
 * Provides a means of reading timestamp information from a string buffer
 * through the overriden method
 * {@link Timestamp#readData(java.lang.StringBuilder)}
 * <p/>
 * <p>
 * Extends {@code DataValue} to provide an easy way to gather, convert, and
 * compare timestamp observation values.
 * </p>
 */
class Timestamp extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Timestamp}
     * </p>
     */
    Timestamp() {
    }


    /**
     * Sets the formatted timestamp for the base classes instance of
     * {@code CsvCollector} to the timestamp represented by the supplied
     * string buffer.
     *
     * @param sBuffer The string buffer representing the timestamp
     *                value.
     */
    @Override
    public void readData(StringBuilder sBuffer) {
        m_oCsvCollector.setTimestamp(sBuffer);
    }
}
