// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IClimateRecord.java
 */
package wde.emc;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code ClimateRecord}
 * object attributes.
 * </p>
 */
public interface IClimateRecord {
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return Min value attribute corresponding to the min scalar value of the
     * observation type for the month
     */
    public double getMin();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return Max value attribute corresponding to the max scalar value of the
     * observation type for the month
     */
    public double getMax();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return Average value attribute corresponding to the normal scalar value
     * of the observation type for the month
     */
    public double getAvg();
}
