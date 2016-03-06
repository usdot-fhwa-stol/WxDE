package wde.data.osm;

import java.util.Iterator;

/**
 * Enables iterating over a set of points that define a road.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class SegIterator implements Iterator<int[]>
{
	private int m_nPos;
	private int m_nEnd;
	private int[] m_oPoints;
	private final int[] m_oLine = new int[4]; // 2D line


  /**
   * Default private constructor
   */
	private SegIterator()
	{
	}


  /**
   * Package private constructor to read private Road points
   */
	SegIterator(int[] oPoints)
	{
		m_oPoints = oPoints; // local immutable copy of road line segments
		m_nEnd = oPoints.length - 2; // line segment end boundary
	}


	@Override
	public boolean hasNext()
	{
		return (m_nPos < m_nEnd);
	}


	@Override
	public int[] next()
	{
		System.arraycopy(m_oPoints, m_nPos, m_oLine, 0, m_oLine.length);
		m_nPos += 2; // shift line to next point
		return m_oLine;
	}


	@Override
	public void remove()
	{
		throw new UnsupportedOperationException("remove");
	}
}
