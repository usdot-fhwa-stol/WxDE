package wde.data.osm;

import java.util.ArrayList;
import java.util.TreeMap;


/**
 * Holds information associated with a geo-coordinate polyline. A polyline
 * can represent roads, rivers, rail lines, or other linear map features.
 *
 * @author bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Road
{
	public final int m_nXmin;
	public final int m_nYmin;
	public final int m_nXmax;
	public final int m_nYmax;
	public final int m_nXmid;
	public final int m_nYmid;
	public final int m_nLength;
	public final int m_nSpeed = 27; // 27 m/s is approximately 60 mph
	public final String m_sName;
	public final String m_sType;
	private int[] m_oPoints;


	/**
	 * Creates a new "blank" instance of a road with no metadata or points
	 */
	private Road()
	{
		// initialize all final member variables
		m_nXmin = m_nYmin = m_nXmax = m_nYmax = m_nXmid = m_nYmid = m_nLength = 0;
		m_sName = m_sType = "".intern();
	}


	public Road(TreeMap<String, String> oTags, ArrayList<Roads.Node> oNodes)
	{
		int nYmax = Integer.MIN_VALUE; // init temp bounds to opposite extremes
		int nXmax = Integer.MIN_VALUE; // so that bounding box can be narrowed
		int nYmin = Integer.MAX_VALUE;
		int nXmin = Integer.MAX_VALUE;
		
		m_oPoints = new int[oNodes.size() * 2]; // copy coordinates to int array
		for (int nIndex = 0; nIndex < oNodes.size(); nIndex++)
		{
			Roads.Node oNode = oNodes.get(nIndex);

			int nLat = oNode.m_nLat;
			int nLon = oNode.m_nLon;

			if (nLat > nYmax) // adjust vertical bounds
				nYmax = nLat;

			if (nLat < nYmin)
				nYmin = nLat;

			if (nLon > nXmax) // adjust horizontal bounds
				nXmax = nLon;

			if (nLon < nXmin)
				nXmin = nLon;

			int nPointIndex = nIndex * 2; // save point data
			m_oPoints[nPointIndex] = nLon; // points are stored in xy order
			m_oPoints[++nPointIndex] = nLat;
		}

		m_nYmax = nYmax; // save bounding box
		m_nXmax = nXmax;
		m_nYmin = nYmin;
		m_nXmin = nXmin;

		m_nXmid = m_nYmid = m_nLength = 0; // finish setting member variables
		String sName = oTags.get("name");
		if (sName == null)
			m_sName = "".intern();
		else // interning should save some memory
			m_sName = sName.intern();
		sName = oTags.get("highway");
		if (sName == null)
			m_sType = "".intern();
		else // interning should save some memory
			m_sType = sName.intern();
	}


	public SegIterator iterator()
	{
		return new SegIterator(m_oPoints); // iterate over read-only points
	}


	/**
	 * Copies the linear mid-point coordinate of the polyline to 
	 * the provided array. The provided array must be of at least size 2.
	 *
	 * @param oMidPoint integer array to save 2D mid-point coordinate
	 */
	public void getMidPoint(int[] oMidPoint)
	{
		if (oMidPoint.length < 2)
			return;

		oMidPoint[0] = m_nXmid;
		oMidPoint[1] = m_nYmid;
	}


	/**
	 * Determines if a point is within the snap distance of the polyline. This
	 * method presumes that the polyline point data are set.
	 *
	 * @param nTol maximum distance for the point associate with the polyline
	 * @param nX   longitudinal coordinate
	 * @param nY   latitudinal coordinate
	 * @return squared distance from the point to the line
	 */
	public int snap(int nTol, int nX, int nY)
	{
		if (!Utility.isInside(nX, nY, m_nYmax, m_nXmax, m_nYmin, m_nXmin, nTol))
			return Integer.MIN_VALUE; // point not inside minimum bounding rectangle

		int nDist = Integer.MAX_VALUE; // narrow to the minimum dist
		int nSqTol = nTol * nTol; // squared tolerance for comparison

		SegIterator oSegIt = iterator(); // reset iterator
		while (oSegIt.hasNext())
		{
			int[] oL = oSegIt.next(); // is point inside line bounding box
			if (Utility.isInside(nX, nY, oL[3], oL[2], oL[1], oL[0], nTol))
			{
				int nSqDist = Utility.getPerpDist(nX, nY, oL[0], oL[1], oL[2], oL[3]);
				if (nSqDist >= 0 && nSqDist <= nSqTol && nSqDist < nDist)
					nDist = nSqDist; // reduce to next smallest distance
			}
		}

		if (nDist == Integer.MAX_VALUE) // point did not intersect with line
			nDist = Integer.MIN_VALUE;
		return nDist;
	}
}
