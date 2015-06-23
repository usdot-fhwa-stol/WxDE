// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IStation.java
 */
package clarus.emc;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code Station}
 * object attributes.
 * </p>
 */
public interface IStation
{
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return Station identifier.
     */
	public int getId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @param nObsType
     * @return Number of sensors at this {@code Station}.
     */
	public int getSensorCount(int nObsType);
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return contributor identifier.
     */
	public int getContribId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return site identifier.
     */
	public int getSiteId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return climate identifier.
     */
	public int getClimateId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Station} latitude.
     */
	public int getLat();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Station} longitude.
     */
	public int getLon();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Station} elevation above sea-level.
     */
	public short getElev();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Station} category type.
     */
	public char getCat();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Station} code.
     */
	public String getCode();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return {@code Station} description.
     */
    public String getDesc();
}
