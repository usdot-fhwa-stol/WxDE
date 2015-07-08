// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @files IObs.java
 */
package wde.obs;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code Obs}
 * attributes.
 * </p>
 */
public interface IObs {
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation type identifier.
     */
    public int getObsTypeId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return identifier of the source (WxDE, VDT) the observation originates from.
     */
    public int getSourceId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return identifier of the sensor making the observation.
     */
    public int getSensorId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return timestamp when the observation was recorded.
     */
    public long getObsTimeLong();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation latitude, based on the sensor location.
     */
    public int getLatitude();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation longitude, based on the sensor location.
     */
    public int getLongitude();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation elevation, based on the sensor location.
     */
    public int getElevation();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation scalar value.
     */
    public double getValue();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return flag bit-field showing whether the corresponding quality check
     * algorithm passed or failed.
     */
    public char[] getQchCharFlag();

    /**
     * <b> Mutator </b>
     * <p>
     * Sets the run/pass flags for the quality checking algorithm used.
     * </p>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @param nRun   the bit field id of the quality checking algorithm used.
     * @param nFlags whether the corresponding quality checking algorithm passed
     *               or failed.
     */
    public void setQchCharFlag(char[] nFlags);

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return quality confidence level.
     */
    public float getConfValue();

    /**
     * <b> Mutator </b>
     * <p>
     * Sets the confidence for the quality checking algorithm used.
     * </p>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @param fConfidence quality confidence level.
     */
    public void setConfValue(float fConfidence);

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return the timestamp when the obs was most recently updated.
     */
    public long getRecvTimeLong();


}
