package wde.comp;

import wde.data.osm.Road;

/**
 * Package private mutable container class that associates pavement with 
 * observation time and values so that inferred observations can be created. 
 */
public class RoadObs
{
	int m_nPrecipSit;
	int m_nVisibilitySit;
	int m_nPavementSit;
	long m_lTimestamp;
	double m_dRefl;
	Road m_oRoad;


	private RoadObs()
	{
	}


	RoadObs(Road oRoad, double dRefl, long lTimestamp)
	{
		m_lTimestamp = lTimestamp;
		m_dRefl = dRefl;
		m_oRoad = oRoad;
	}
}
