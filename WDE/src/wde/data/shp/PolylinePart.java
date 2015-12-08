package wde.data.shp;

import wde.data.AbstractIterator;

import java.util.Iterator;

/**
 * Enables iterating over the defined parts of a shape file polyline.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class PolylinePart extends AbstractIterator<int[]> {
	private int m_nStart;
	private int m_nEnd;
	private int[] m_oPolyline;
	private final int[] m_oLine = new int[4]; // 2D line


  /**
   * Default package private constructor must be called by Polyline
   */
	PolylinePart()
	{
	}


	void set(int[] oPolyline, int nStart, int nEnd)
	{
		m_oPolyline = oPolyline;
		m_nStart = nStart;
		m_nEnd = nEnd;
	}


	@Override
	public boolean hasNext()
	{
		return (m_oPolyline != null && m_nStart < m_nEnd);
	}


	@Override
	public int[] next()
	{
		System.arraycopy(m_oPolyline, m_nStart, m_oLine, 0, m_oLine.length);
		m_nStart += 2; // shift line to next point
		return m_oLine;
	}
}
