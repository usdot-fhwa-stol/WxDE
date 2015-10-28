package wde.data.shp;


import java.io.DataInputStream;

import wde.util.MathUtil;


/**
 * Holds information associated with a geo-coordinate polyshape.  A polyshape can be used
 * to represent roads, rivers, rail lines, city boundaries, state boundaries, or any other
 * 2-D map shape.
 * 
 * A polyshape is defined by "parts" and geo-coordinate points.  Each "part" is composed
 * of at least one set of longitude and latitude coordinates.  The points are broken into
 * parts in order to allow for discontiguous shapes like dotted lines or "donuts".
 * 
 * In the array of "parts", each entry points to the index of the points array where the
 * part begins.  The first entry in the parts array is always 0.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public abstract class Polyshape implements Comparable<Polyshape>
{
  /** Minimum polyshape longitude. */
	public final int m_nXmin;
  /** Minimum polyshape latitude. */
	public final int m_nYmin;
  /** Maximum polyshape longitude. */
	public final int m_nXmax;
  /** Maximum polyshape latitude. */
	public final int m_nYmax;
  /** Array of polyshape "parts". */
	protected int[] m_nParts;
  /** Array of polyshape latitude & longitude coordinates. */
	protected int[] m_nPoints;


	/**
	* Creates a new "blank" instance of Polyshape.
	*/
	protected Polyshape()
	{
		m_nXmin = m_nYmin = m_nXmax = m_nYmax = 0;
	}


  /**
   * Creates a new instance of Polyshape with name, data input, and math transform
   * specified.
   * 
	 * @param oDataInputStream data stream containing information used to build the polyshape object
   * @throws java.lang.Exception
   */
	protected Polyshape(DataInputStream oDataInputStream)
		throws Exception
	{
		// discard the record, length, and type information
		int nRecordNumber = oDataInputStream.readInt();
		int nContentLength = oDataInputStream.readInt();
		
		int nType = Utility.swap(oDataInputStream.readInt());
		nContentLength -= 2;

		int nXmin = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
		int nYmin = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
		int nXmax = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
		int nYmax = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
		nContentLength -= 16;
		
		if (nXmax < nXmin) // swap the min and max values as needed
		{
			m_nXmin = nXmax;
			m_nXmax = nXmin;
		}
		else
		{
			m_nXmin = nXmin;
			m_nXmax = nXmax;
		}

		if (nYmax < nYmin)
		{
			m_nYmin = nYmax;
			m_nYmax = nYmin;
		}
		else
		{
			m_nYmin = nYmin;
			m_nYmax = nYmax;
		}

		int nNumParts = Utility.swap(oDataInputStream.readInt());
		m_nParts = new int[nNumParts];
		nContentLength -= 2;

		int nNumPoints = Utility.swap(oDataInputStream.readInt());
		m_nPoints = new int[(nNumPoints * 2)];
		nContentLength -= 2;

		int nIndex = 0;
		while (nNumParts-- > 0)
		{
			m_nParts[nIndex++] = Utility.swap(oDataInputStream.readInt()) * 2;
			nContentLength -= 2;
		}

		nIndex = 0;
		while (nNumPoints-- > 0)
		{
			m_nPoints[nIndex++] = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
			m_nPoints[nIndex++] = MathUtil.toMicro(Utility.swapD(oDataInputStream.readLong()));
			nContentLength -= 8;
		}

		while (nContentLength-- > 0) // ignore remaining non-point data
			oDataInputStream.readShort();
  }


  /**
   * Abstract class to determine if the specified coordinate is within the specified distance
   * of a polyshape.
   * 
	 * @param dMaxDistance maximum distance for point to be considered "in" the polyshape
	 * @param nX longitudinal coordinate
	 * @param nY latitudinal coordinate
	 * @return true if the point is "in" the polyshape, false otherwise
   */
	public abstract boolean contextSearch(double dMaxDistance, int nX, int nY);


  /**
   * Determines if the specified point is within the bounds of this polyshape.  This method
   * does not take into account any padding distance, the point must either be within the
   * actual area enclosed by polyshape or on the polyshape itself.
   * 
   * @param nLat latitudinal coordinate
   * @param nLon longitudinal coordinate
   * @param nTol tolerance distance from bounds still considered inside
   * @return ture if the point is strictly in the polyshape, false otherwise
   */
	public boolean isInsideBounds(int nLat, int nLon, int nTol)
	{
		int nT = m_nYmax + nTol; // adjust bounds to include the tolerance
		int nR = m_nXmax + nTol; // negative tolerance will shrink bounds
		int nB = m_nYmin - nTol;
		int nL = m_nXmin - nTol;

    return (nLon >= nL && nLon <= nR && nLat <= nT && nLat >= nB);
	}


  /**
   * Compares this polysyape to the specified polyshape for order by minimum longitude.
   * Returns a negative integer, zero, or a positive integer as this object's minimum
   * longitude is less than, equal to, or greater than the specified object's minimum
   * longitude.
	 * 
	 * @param oRhs the polyshape object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object's minimum
   * longitude is less than, equal to, or greater than the specified object's minimum
   * longitude.
   */
	@Override
	public int compareTo(Polyshape oRhs)
	{
		if (this == oRhs)
			return 0;
		
		if (oRhs == null || m_nXmin >= oRhs.m_nXmin)
			return 1;

		return -1;
  }
}
