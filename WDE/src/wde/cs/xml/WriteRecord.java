// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file WriteRecord.java
 */
package wde.cs.xml;

/**
 * Provides a means to when the start and stop of the observations belonging to
 * each platform occurs. At the end of each of these groups of observations the
 * values are written and the actual observation objects are created. The
 * Timestamp and platform code are then reset for the next platform.
 * <p/>
 * <p>
 * Extends {@code DataValue} to provide a way to trigger the start and stop of
 * observations by a given platform.
 * </p>
 */
class WriteRecord extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code WriteRecord}. Initialization done through
     * base class {@link DataValue#init} method.
     * </p>
     */
    protected WriteRecord() {
    }


    /**
     * Since this is a DataValue that doesn't have an actual observation to
     * read in, we must reset the CurrentDataValue object in the XmlCollector
     * so that it doesn't affect the reading in of following observations or
     * affect removing tags from the path.
     */
    @Override
    public void start() {
        m_oXmlCollector.resetCurrentDataValue(this);
    }

    /**
     * This method is overriden so that nothing happens when it is called
     * because this DataValue does not have an observation to read in.
     */
    @Override
    public void characters(StringBuilder sBuffer) {
    }


    /**
     * At the end of the observations from this platform the values will all
     * be written and the Timestamp and platform Code will be reset.
     */
    @Override
    public void end() {
        m_oXmlCollector.writeValueSet();
    }
}