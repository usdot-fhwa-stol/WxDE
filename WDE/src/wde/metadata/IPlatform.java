// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IPlatform.java
 */
package wde.metadata;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code Platform}
 * object attributes.
 * </p>
 */
public interface IPlatform extends Comparable<IPlatform> {
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return Platform identifier.
     */
    public int getId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @param nObsType
     * @return Number of sensors at this {@code Platform}.
     */
    public int getSensorCount(int nObsType);

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return contributor identifier.
     */
    public int getContribId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return site identifier.
     */
    public int getSiteId();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return {@code Platform} latitude.
     */
    public double getLocBaseLat();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return {@code Platform} longitude.
     */
    public double getLocBaseLong();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return {@code Platform} elevation above sea-level.
     */
    public double getLocBaseElev();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return {@code Platform} category type.
     */
    public char getCategory();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return {@code Platform} code.
     */
    public String getPlatformCode();

    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     *
     * @return {@code Platform} description.
     */
    public String getDescription();
}
