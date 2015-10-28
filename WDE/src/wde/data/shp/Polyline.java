package wde.data.shp;

import java.io.DataInputStream;

import wde.util.MathUtil;


/**
 * Holds information associated with a geo-coordinate polyline.  A polyline can be used to
 * represent roads, rivers, rail lines, or other linear map features.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Polyline extends Polyshape
{
  /**
   * Creates a new "blank" instance of Polyline 
   */
  protected Polyline()
  {
  }

	/**
	 * Creates a new instance of Polyline with name, identifier, data input, and math transform
   * specified.
   * 
	 * @param oDataInputStream data stream containing information used to build the polyline object
	 * @throws java.lang.Exception
	 */
	public Polyline(DataInputStream oDataInputStream)
		throws Exception
	{
		super(oDataInputStream);
	}


  /**
	 * Determines if the specified coordinate is within the specified distance of the polyline.
   * 
	 * @param dMaxDistance maximum distance for point to be considered "in" the polyline
	 * @param nX longitudinal coordinate
	 * @param nY latitudinal coordinate
	 * @return true if the point is "in" the polyline, false otherwise
   */
	@Override
	public boolean contextSearch(double dMaxDistance, int nX, int nY)
	{
		int nMaxDistance = MathUtil.toMicro(dMaxDistance);
		boolean bFound = false;

		for (int nPartIndex = 0; nPartIndex < m_nParts.length;)
		{
			int nStartIndex = m_nParts[nPartIndex];
			int	nEndIndex = m_nParts[nPartIndex];
			if (++nPartIndex == m_nParts.length)
				nEndIndex = m_nPoints.length;

			// the array uses even-odd values to represent x, y, x, y, ... x, y points
			// verify there are enough array values to make a complete line segment
			if (nEndIndex >= 4)
			{
				// initialize the line segment starting point
				int nX1 = m_nPoints[nStartIndex++];
				int nY1 = m_nPoints[nStartIndex++];

				while (nStartIndex < nEndIndex)
				{
					// get the next point on the line segment
					int nX2 = m_nPoints[nStartIndex++];
					int nY2 = m_nPoints[nStartIndex++];
					
					// check to see if the point is within the individual line segment bounding box
					if (Utility.isPointInsideRegion(nX, nY, nY2, nX2, nY1, nX1, nMaxDistance))
					{
						double dDistance = getPerpendicularDistance(nX, nY, nX1, nY1, nX2, nY2);
						// break out of the loop when the first intersection is found
						bFound = (dDistance != Double.NaN && dDistance <= dMaxDistance);
						if (bFound)
							nStartIndex = nEndIndex;
					}

					// set the next line starting point
					nX1 = nX2;
					nY1 = nY2;
				}
			}
		}
		
		return bFound;
  }


	/**
	 * Determines the perpendicular distance between the specified point and the polyline
	 * defined by the specified coordinates.  The squared distance is returned in scaled
	 * degrees.
	 * 
	 * @param nX intger longitudinal coordinate of the point scaled to six decimal places
	 * @param nY integer latitudinal coordinate of the point scaled to six decimal places
	 * @param nX1 integer longitudinal coordinate of the polyline's first end point scaled to six decimal places
	 * @param nY1 integer latitudinal coordinate of the polyline's first end point scaled to six decimal places
	 * @param nX2 integer longitudinal coordinate of the polyline's second end point scaled to six decimal places
	 * @param nY2 integer latitudinal coordinate of the polyline's second end point scaled to six decimal places
	 * @return scaled double precision degree distance between the point and the polyline
	 */
	private double getPerpendicularDistance(int nX, int nY, int nX1, int nY1, int nX2, int nY2)
	{
		int nDeltaX = nX2 - nX1;
		int nDeltaY = nY2 - nY1;
		
		long lU = ((nX - nX1) * nDeltaX) + ((nY - nY1) * nDeltaY);
		long lV = (nDeltaX * nDeltaX + nDeltaY * nDeltaY);
	
		// closest point does not fall within the line segment
		if (lU < 0 || lU > lV)
			return Double.NaN;

		// find the point on the line segment 
		// where the perpendicular intersection occurs
		int nXp = nX1 + (int)(lU * nDeltaX / lV);
		int nYp = nY1 + (int)(lU * nDeltaY / lV);

		// get the distance between the specified point and the intersection point 
		double dDeltaX = MathUtil.fromMicro(nX - nXp);
		double dDeltaY = MathUtil.fromMicro(nY - nYp);
		return Math.sqrt((dDeltaX * dDeltaX) + (dDeltaY * dDeltaY));
	}
}
