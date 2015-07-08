// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package wde.cs.ascii;

/**
 * Overrides the base class method {@link DataValue#writeData() } to keep from
 * printing {@code Ignore} values.
 */
class Ignore extends DataValue {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Ignore}
     * </p>
     */
    Ignore() {
    }


    /**
     * Overrides and masks the base class method {@link DataValue#writeData()}.
     * This is because {@code Ignore} objects contain data we want to ignore.
     */
    @Override
    public void writeData() {
        // no obs data is saved since we want to ignore the data
    }
}
