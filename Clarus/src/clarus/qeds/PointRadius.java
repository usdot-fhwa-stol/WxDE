// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file PointRadius.java
 */
package clarus.qeds;

import clarus.emc.Stations;

/**
 * Wraps a point, with a radius. The point-radius pair consists of a
 * geographical point (latitude and longitude), and a radius.
 */
public class PointRadius
{
    /**
     * Latitude attribute.
     */
	public double m_dLat;
    /**
     * Longitude attribute.
     */
	public double m_dLng;
    /**
     * Radius attribute.
     */
	public double m_dRadius;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code PointRadius}
	 * </p>
	 */
	private PointRadius()
	{
	}


    /**
     * Creates a new instance of {@code PointRadius} with the provided
     * attribute values, converted from micro-units.
     * @param nLat Latitude value, in micro-units.
     * @param nLon Longitude value, in micro-units.
     * @param nRadius Radius value, in micro-units.
     */
	public PointRadius(int nLat, int nLon, int nRadius)
	{
		m_dLat = Stations.fromMicro(nLat);
		m_dLng = Stations.fromMicro(nLon);
		m_dRadius = Stations.fromMicro(nRadius);
	}


    /**
     * Parses the provided string array for the attribute values.
     * <blockquote>
     * {@code sData[0]} - latitude value. <br />
     * {@code sData[1]} - longituce value. <br />
     * {@code sData[2]} - radius value. <br />
     * </blockquote>
     * @param sData string array containing the attribute values.
     */
	public PointRadius(String[] sData)
	{
		m_dLat = Double.parseDouble(sData[0]);
		m_dLng = Double.parseDouble(sData[1]);
		m_dRadius = Double.parseDouble(sData[2]);
	}
}