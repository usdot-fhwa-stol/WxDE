// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @files IObs.java
 */
package clarus.qedc;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code Obs}
 * attributes.
 * </p>
 */
public interface IObs
{
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return observation type identifier.
     */
    public int getTypeId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return identifier of the sensor making the observation.
     */
	public int getSensorId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return timestamp when the observation was recorded.
     */
	public long getTimestamp();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return observation latitude, based on the sensor location.
     */
	public int getLat();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return observation longitude, based on the sensor location.
     */
	public int getLon();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return observation elevation, based on the sensor location.
     */
	public short getElev();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return observation scalar value.
     */
	public double getValue();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return bit-field showing which quality checking algorithms were ran on
     * this object.
     */
	public int getRun();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return flag bit-field showing whether the corresponding quality check
     *  algorithm passed or failed.
     */
	public int getFlags();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return quality confidence level.
     */
	public float getConfidence();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return the timestamp when the obs was most recently updated.
     */
	public long getUpdate();
    /**
     * <b> Mutator </b>
     * <p>
     * Sets the run, pass, and confidence for the quality checking algorithm
     * used.
     * </p>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @param nRun the bit field id of the quality checking algorithm used.
     * @param nFlags whether the corresponding quality checking algorithm passed
     *  or failed.
     * @param fConfidence quality confidence level.
     */
	public void setFlags(int nRun, int nFlags, float fConfidence);
}
