// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qchs;

/**
 *
 */
public class MapProjection
{
	private static final double PI4 = Math.PI / 4.0;

	private double m_dRadiusEarth;
	private double m_dScale;
	private double m_dX;
	private double m_dY;
	private double m_dLat;
	private double m_dLon;
	private double m_dDeltaX;
	private double m_dDeltaY;
	private double[] m_dScratch = new double[2];


	protected MapProjection()
	{
	}


	public MapProjection(double dRadiusEarth, double dScale, double dLat,
		double dLon, double dX, double dY, double dDeltaX, double dDeltaY)
	{
		// convert earth radius to meters
		if (dRadiusEarth >= 6300 && dRadiusEarth < 6400)
			m_dRadiusEarth = dRadiusEarth * 1000.0;
		else if (dRadiusEarth >= 6300000.0 && dRadiusEarth < 6400000.0)
			m_dRadiusEarth = dRadiusEarth;
		else
			m_dRadiusEarth = 6367470.0;

		// set projection scale factor so scale at lat 60.0 is 1.0
		m_dScale = 2.0 * m_dRadiusEarth / dScale;

		// correct the latitude and convert to radians
		if (dLat > 90.0)
			dLat = 90.0 - dLat;

		if (dLat <= -90.0)
			dLat += 90.0;

		m_dLat = Math.toRadians(dLat);

		// correct the longitude and convert to radians
		if (dLon > 180.0)
			dLon = dLon - 360.0;

		if (dLon <= -180.0)
			dLon += 360.0;

		m_dLon = Math.toRadians(dLon);

		// directly save the remaining configuration paramterts
		m_dX = dX;
		m_dY = dY;
		m_dDeltaX = dDeltaX;
		m_dDeltaY = dDeltaY;
	}


	// Spherical north polar stereographic projection
	// See:
	//   Map Projections - A Working Manual
	//   John P. Snyder
	//   USGS Professional Paper 1395, published 1987
	//   http://infotrek.er.usgs.gov/pubs/
	//   Supt. of Docs No: 19.16:1395
	//
	//   Given latIn, lonIn, find xx, yy in meters.
	private void forwardStereographic(double[] dXY, double dLat, double dLon)
	{
		// convert degrees into radians and compute projection constants
		// toRadians method could have been used, but expanding the
		// conversion inline avoids one additional division operation
		dLat = Math.tan(PI4 - (dLat * Math.PI / 360.0)) * m_dScale;
		dLon = Math.toRadians(dLon) - m_dLon;
		dXY[0] =  dLat * Math.sin(dLon);
		dXY[1] = -dLat * Math.cos(dLon);
	}


	public void getBounds(int[] nLimitX, int[] nLimitY,
		double dLat, double dLon, double dRadius)
	{
		double dCenterX;
		double dCenterY;

		synchronized(this)
		{
			forwardStereographic(m_dScratch, dLat, dLon);
			// get the indices for the radius around the center point
			dCenterX = m_dScratch[0] - m_dX;
			dCenterY = m_dScratch[1] - m_dY;
		}
		
		// calculate the width and height bounds
		nLimitX[0] = (int)((dCenterX - dRadius) / m_dDeltaX);
		nLimitX[1] = (int)((dCenterX + dRadius) / m_dDeltaX) + 1;
		nLimitY[0] = (int)((dCenterY - dRadius) / m_dDeltaY);
		nLimitY[1] = (int)((dCenterY + dRadius) / m_dDeltaY) + 1;
	}
}
