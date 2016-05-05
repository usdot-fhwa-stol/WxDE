package wde.data.osm;

import java.io.DataInputStream;


/**
 * Holds information associated with a geo-coordinate polyline. A polyline
 * can represent roads, rivers, rail lines, or other linear map features.
 *
 * @author bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Road
{
	public final String m_sName;
	public final int m_nId;
	public final int m_nXmin;
	public final int m_nYmin;
	public final int m_nXmax;
	public final int m_nYmax;
	public final int m_nXmid;
	public final int m_nYmid;
	public final int m_nSpeed = 27; // 27 m/s is approximately 60 mph
	private int[] m_nPoints;


	/**
	 * Creates a new "blank" instance of a road with no metadata or points
	 */
	private Road()
	{
		// initialize all final member variables
		m_nXmin = m_nYmin = m_nXmax = m_nYmax = 
			m_nXmid = m_nYmid = m_nId = Integer.MIN_VALUE;
		m_sName = "".intern();
	}


	Road(int nId, DataInputStream oOsmBin)
		throws Exception
	{
		int nYmax = Integer.MIN_VALUE; // init temp bounds to opposite extremes
		int nXmax = Integer.MIN_VALUE; // so that bounding box can be narrowed
		int nYmin = Integer.MAX_VALUE;
		int nXmin = Integer.MAX_VALUE;

		m_sName = oOsmBin.readUTF().intern(); // intern to conserve memory
		int nSize = oOsmBin.readShort(); // reuse point count
		m_nPoints = new int[nSize * 2]; // copy coordinate pairs
		for (int nIndex = 0; nIndex < m_nPoints.length;)
		{
			int nLat = oOsmBin.readInt(); // points are read in lat/lon order
			int nLon = oOsmBin.readInt();

			if (nLat > nYmax) // adjust vertical bounds
				nYmax = nLat;

			if (nLat < nYmin)
				nYmin = nLat;

			if (nLon > nXmax) // adjust horizontal bounds
				nXmax = nLon;

			if (nLon < nXmin)
				nXmin = nLon;

			m_nPoints[nIndex++] = nLon; // store point data in xy order
			m_nPoints[nIndex++] = nLat;
		}

		m_nYmax = nYmax; // save bounding box
		m_nXmax = nXmax;
		m_nYmin = nYmin;
		m_nXmin = nXmin;
		m_nId = nId; // assign sequential identifier

		double dLen = 0.0; // accumulate total length
		double[] dLens = new double[--nSize]; // individual segment lengths
		int[] nSeg = null;
		int nIndex = 0;
		SegIterator oSegIt = iterator(); // derive midpoint
		while (oSegIt.hasNext())
		{
			nSeg = oSegIt.next(); // there should always be at least one segment
			int nDeltaX = nSeg[2] - nSeg[0];
			int nDeltaY = nSeg[3] - nSeg[1];
			double dSegLen = Math.sqrt(nDeltaX * nDeltaX + nDeltaY * nDeltaY);
			dLen += dSegLen;
			dLens[nIndex++] = dSegLen;
		}

		double dMidLen = dLen / 2.0; // half the length
		dLen = 0.0; // reset total length
		nIndex = 0;
		while (nIndex < dLens.length && dLen < dMidLen)
			dLen += dLens[nIndex++]; // find length immediately before the midpoint

		dLen -= dLens[--nIndex]; // rewind one position
		System.arraycopy(m_nPoints, nIndex * 2, nSeg, 0, nSeg.length);
		double dRatio = (dMidLen - dLen) / dLens[nIndex];

		int nDeltaX = (int)((nSeg[2] - nSeg[0]) * dRatio);
		int nDeltaY = (int)((nSeg[3] - nSeg[1]) * dRatio);
		m_nXmid = nSeg[0] + nDeltaX;
		m_nYmid = nSeg[1] + nDeltaY;
	}


	public final SegIterator iterator()
	{
		return new SegIterator(m_nPoints); // iterate over read-only points
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
