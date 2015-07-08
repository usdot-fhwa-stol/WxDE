// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qchs;

import java.util.Comparator;
import clarus.emc.Stations;
import util.Introsort;

/**
 *
 */
public class SurfaceRecord implements Comparator<SurfaceRecord>
{
	public static final int PRECISION = 2500000;
	public static final int OFFSET = PRECISION / 2;

	private int m_nHash = 0;
	private int m_nPeriod = 0;
	private double m_dMin = 0.0;
	private double m_dMax = 0.0;


	SurfaceRecord()
	{
	}


	SurfaceRecord(double dLat, double dLon, int nPeriod,
		double dMin, double dMax)
	{
		// adjust the source range coordinates to the grid cell format
		setHash(Stations.toMicro(dLat), Stations.toMicro(dLon), nPeriod);
		m_nPeriod = nPeriod;
		m_dMin = dMin;
		m_dMax = dMax;
	}


	void setHash(int nLat, int nLon, int nPeriod)
	{
		// add half of the precision value to offset the initial coordinates
		// then floor the coordinates to the precision
		nLat = Introsort.floor(nLat + OFFSET, PRECISION) / 100000;
		nLon = Introsort.floor(nLon + OFFSET, PRECISION) / 100000;
		// hash is to the nearest tenth of a degree
		// the hash is then shifted and adds the period
		m_nHash = (nLon * 1800 + nLat) * 16 + nPeriod;
	}


	public boolean inRange(double dValue)
	{
		return (dValue >= m_dMin && dValue <= m_dMax);
	}


	public int compare(SurfaceRecord oLhs, SurfaceRecord oRhs)
	{
		return (oLhs.m_nHash - oRhs.m_nHash);
	}
}
