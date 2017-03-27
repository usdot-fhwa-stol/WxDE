package wde.comp;

import java.util.ArrayList;
import wde.data.osm.Road;

/**
 * Package private mutable container class that associates pavement with 
 * observation time and values so that inferred observations can be created. 
 */
public class RoadObs implements Comparable<Road>
{
	int m_nPrecipSit;
	int m_nVisibilitySit;
	int m_nPavementSit;
	int m_nPrevPrecip;
	int m_nPrevVisibility;
	int m_nPrevPavement;
	int m_nElev;
	int m_nSensorId;
	long m_lTimestamp;
	double m_dRefl;
	ArrayList<Double> m_dAirTemp = new ArrayList();
	Road m_oRoad;


	private RoadObs()
	{
	}


	RoadObs(Road oRoad, double dRefl, long lTimestamp, int nElev)
	{
		m_lTimestamp = lTimestamp;
		m_dRefl = dRefl;
		m_oRoad = oRoad;
		m_nElev = nElev;
		m_nPrecipSit = -1;
		m_nVisibilitySit = -1;
		m_nPavementSit = -1;
	}


	@Override
	public int compareTo(Road o)
	{
		return m_oRoad.m_nId - o.m_nId;
	}
}
