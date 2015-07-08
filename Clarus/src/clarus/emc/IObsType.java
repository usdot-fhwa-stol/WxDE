// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IObsType.java
 */
package clarus.emc;

/**
 * <b>Interface implementation. </b>
 * <p>
 * Forces implementations to provide methods for accessing {@code ObsType}
 * object attributes.
 * </p>
 */
public interface IObsType
{
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return id attribute.
     */
	public int getId();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return observation name attribute.
     */
	public String getName();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return unit identifier attribute.
     */
	public String getUnit();
    /**
     * <b> Accessor </b>
     * <p>
     * Extensions must implement this method.
     * </p>
     * @return english unit identifier attribute.
     */
	public String getEnglishUnit();
}
