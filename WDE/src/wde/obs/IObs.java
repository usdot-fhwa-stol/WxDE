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
    int getObsTypeId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return identifier of the source (WxDE, VDT) the observation originates from.
     */
    int getSourceId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return identifier of the sensor making the observation.
     */
    int getSensorId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return timestamp when the observation was recorded.
     */
    long getObsTimeLong();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation latitude, based on the sensor location.
     */
    int getLatitude();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation longitude, based on the sensor location.
     */
    int getLongitude();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation elevation, based on the sensor location.
     */
    int getElevation();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return observation scalar value.
     */
    double getValue();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return flag bit-field showing whether the corresponding quality check
     * algorithm passed or failed.
     */
    char[] getQchCharFlag();

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
    void setQchCharFlag(char[] nFlags);

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return quality confidence level.
     */
    float getConfValue();

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
    void setConfValue(float fConfidence);

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return the timestamp when the obs was most recently updated.
     */
    long getRecvTimeLong();


}
