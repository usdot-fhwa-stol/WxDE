// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ISensor.java
 */
package clarus.emc;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code Sensor}
 * object attributes.
 * </p>
 */
public interface ISensor
{
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return sensor id attribute.
     */
	public int getSensorId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return identifier of the {@code Station} that the {@code Sensor} is
     *      contained.
     */
	public int getStationId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Sensor} type attribute.
     */
	public int getSensorType();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Observation type identifier.
     */
	public int getObsTypeId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Index of Sensor. Distinguishes sensors at the same Station.
     */
	public int getSensorIndex();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return sensor distribution group.
     */
	public int getDistGroup();
    /**
     * Determines whether the supplied timestamp occurs in a time of
     * maintenance.
     * <p>
     * Extensions must implement this method.
     * </p>
     * @param lTimestamp
     * @return true if the timestamp occurs in a time of maintenance.
     */
	public boolean underMaintenance(long lTimestamp);
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Minimum scalar value the Sensor should be able to report.
     */
	public double getRangeMin();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Maximum scalar value the Sensor should be able to report.
     */
	public double getRangeMax();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Maximum positive rate of change the Sensor should be able to
     *      report.
     */
	public double getRatePos();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Maximum negative rate of change the Sensor should be able to
     *      report.
     */
	public double getRateNeg();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Length of time used for qchs persistence test.
     */
	public double getPersistInterval();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Minimum change between values that must occur over
     *      m_dPersisInterval for the observation to pass qchs persistence test.
     */
	public double getPersistThreshold();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Maximum variance between this Sensor and like sensors
     *      observations to pass qchs "Like Instrument Test."
     */
	public double getLikeThreshold();
}