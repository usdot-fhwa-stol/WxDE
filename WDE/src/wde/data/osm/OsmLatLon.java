package wde.data.osm;

import java.util.Comparator;

/**
 *
 * @author bryan.krueger
 */
public class OsmLatLon implements Comparable<Long>, Comparator<OsmLatLon>
{
	public final long m_lId;
	public final double m_dLat;
	public final double m_dLon;


	public OsmLatLon()
	{
		this(0, 0, 0); // initialize member variables
	}


	public OsmLatLon(long lId, double dLat, double dLon)
	{
		m_lId = lId;
		m_dLat = dLat;
		m_dLon = dLon;
	}


		@Override
		public int compare(OsmLatLon oLhs, OsmLatLon oRhs)
		{
			if (oLhs.m_lId < oRhs.m_lId)
				return -1;

			if (oLhs.m_lId > oRhs.m_lId)
				return 1;

			return 0;
		}


	@Override
	public int compareTo(Long oId)
	{
		if (m_lId < oId)
			return -1;

		if (m_lId > oId)
			return 1;

		return 0;
	}
}
