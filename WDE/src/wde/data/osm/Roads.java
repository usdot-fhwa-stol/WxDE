package wde.data.osm;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import wde.util.MathUtil;


/**
 * A singleton that reads a directory of processed Openstreetmap files and then 
 * responds to queries for roadways that are within a specified distance from 
 * a target point.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Roads implements Comparator<Road>
{
	private static final Roads g_oRoads = new Roads();

	ArrayList<GridIndex> m_oGridCache = new ArrayList(); // 2D indexed roads


	/**
	 * Returns a reference to singleton Roads polyline data cache.
	 *
	 * @return reference to Roads instance.
	 */
	public static Roads getInstance()
	{
		return g_oRoads;
	}


	/**
	 * <b> Default Private Constructor </b>
	 * <p>
	 * Creates a new instance of Roads upon class loading and reads binary files 
	 * from a well-defined directory into memory for fast lookup. Client 
	 * components obtain a singleton reference through the getInstance method.
	 * </p>
	 */
	private Roads()
	{
		int nRoadId = 0;
		ArrayList<GridIndex> oGrids = new ArrayList(); // grid/road intersections
		try
		{
			File[] oFiles = new File("/opt/osm").listFiles(); // default location
			for (File oFile : oFiles)
			{
				if (oFile.isDirectory() || !oFile.getName().endsWith(".osm.bin"))
					continue; // skip directories, only need processed OSM binary files

				try (DataInputStream oOsmBin = new DataInputStream(
					new BufferedInputStream(new FileInputStream(oFile))))
				{
					for (;;) // this will execute until end-of-file is thrown
					{
						try
						{
							oGrids.clear(); // reuse grid buffer
							Road oRoad = new Road(++nRoadId, oOsmBin); // load road definition
							SegIterator oSegIt = oRoad.iterator();
							while (oSegIt.hasNext())
							{
								int[] oLine = oSegIt.next(); // determine intersecting segments
								getGrids(oGrids, oLine[0], oLine[1], oLine[2], oLine[3], 0, 0, true);
							}

							for (GridIndex oGrid : oGrids)
							{
								int nRoadIndex = Collections.binarySearch(oGrid, oRoad, this);
								if (nRoadIndex < 0) // include a road in each grid cell only once
									oGrid.add(~nRoadIndex, oRoad);
							}
						}
						catch (Exception oException) // discard exception, continue reading
						{
							if (oException instanceof java.io.EOFException)
								throw oException; // rethrow end-of-file exception
						}
					}
				}
				catch (Exception oException) // should only be end-of-file
				{
				}
			}
		}
		catch (Exception oException)
		{
		}
	}


	/**
	 * Determines the set of grid cells that intersect the specified area.
	 *
	 * @param oGrids	an array that accumulates GridIndex object that 
	 *								intersect the provided region.
	 * @param nXmin		left side of bounding region.
	 * @param nYmin		bottom side of bounding region.
	 * @param nXmax		right side of bounding region.
	 * @param nYmax		top side of bounding region.
	 * @param nTol		margin of tolerance to include in region.
	 * @param nLatTol	margin of tolerance corrected for latitude.
	 */
	private void getGrids(ArrayList<GridIndex> oGrids, int nXmin, int nYmin, 
		int nXmax, int nYmax, int nTol, int nLatTol, boolean bAdd)
	{
		if (nXmin > nXmax) // re-order longitude as needed
		{
			nXmin ^= nXmax;
			nXmax ^= nXmin;
			nXmin ^= nXmax;
		}
	
		if (nYmin > nYmax) // re-order latitude as needed
		{
			nYmin ^= nYmax;
			nYmax ^= nYmin;
			nYmin ^= nYmax;
		}

		nXmin -= nTol; // adjust for tolerances
		nYmin -= nLatTol;
		nXmax += nTol;
		nYmax += nLatTol;

		if (nXmin < -180000000 || nXmax > 179999999 || nYmin < -84999999 || nYmax > 84999999)
			return; // locations fall outside the geographic model
	
		GridIndex oSearch = new GridIndex(); // find polyline sets by hash index
		int nXbeg = oSearch.getGrid(nXmin);
		int nXend = oSearch.getGrid(nXmax);
		int nYbeg = oSearch.getGrid(nYmin);
		int nYend = oSearch.getGrid(nYmax);

		GridIndex oGrid; // <= comparison used to always have at least one grid
		for (int nY = nYbeg; nY <= nYend; nY++)
		{
			for (int nX = nXbeg; nX <= nXend; nX++)
			{
				oSearch.setHash(nX, nY);
				int nCellIndex = Collections.binarySearch(m_oGridCache, oSearch);
				if (bAdd && nCellIndex < 0) // existing grid cell not found
				{
					oGrid = new GridIndex(); // create new grid hash index
					oGrid.m_nHash = oSearch.m_nHash; // copy current search hash value
					nCellIndex = ~nCellIndex;
					m_oGridCache.add(nCellIndex, oGrid); // add grid cell to cache
				}

				if (nCellIndex >= 0)
					oGrids.add(m_oGridCache.get(nCellIndex));
			}
		}
	}


	/**
	 * Finds the nearest road link to a specified location within a tolerance.
	 *
	 * @param nTol	maximum distance a road link can be found from the target.
	 * @param nLon	longitude of the target point.
	 * @param nLat	latitude of the target point.
	 * 
	 * @return the nearest road link to the target point or null if none found.
	 */
	public Road getLink(int nTol, int nLon, int nLat)
	{
		ArrayList<Road> oRoads = new ArrayList();
		int nLonTol = getLinks(oRoads, nTol, nLon, nLat, nLon, nLat);
		if (oRoads.isEmpty()) // no set of links nearby
			return null;

		int nDist = Integer.MAX_VALUE; // track minimum distance
		Road oLink = null;
		for (Road oRoad : oRoads)
		{
			int nSqDist = oRoad.snap(nLonTol, nLon, nLat);
			if (nSqDist >= 0 && nSqDist < nDist)
			{
				oLink = oRoad; // save the polyline data that is the 
				nDist = nSqDist; // shortest distance from the point
			}
		}

		if (nDist == Integer.MAX_VALUE)
			return null; // no link found
	
		return oLink;
	}


	/**
	 * Returns a set of links that fall within the specified region.
	 *
	 * @param oRoads	list of roads that fall within the specified region.
	 * @param nTol		max distance where links will be included in the region.
	 * @param nLon1		the specified region's left side.
	 * @param nLat1		the specified region's bottom side.
	 * @param nLon2		the specified region's right side.
	 * @param nLat2		the specified region's top side.
	 * 
	 * @return	the latitude adjusted tolerance.
	 */
	public int getLinks(ArrayList<Road> oRoads, int nTol, int nLon1, int nLat1, 
		int nLon2, int nLat2)
	{
		ArrayList<GridIndex> oGrids = new ArrayList();
		int nLonTol = (int)(nTol / Math.cos(Math.PI * 
			MathUtil.fromMicro((nLat1 + nLat2) / 2) / 180.0));
		getGrids(oGrids, nLon1, nLat1, nLon2, nLat2, nTol, nLonTol, false);
		if (oGrids.isEmpty()) // no set of links nearby
			return nLonTol;

		for (GridIndex oGrid : oGrids)
		{
			for (Road oRoad : oGrid)
			{
				int nIndex = Collections.binarySearch(oRoads, oRoad, this);
				if (nIndex < 0) // include each road only once
					oRoads.add(~nIndex, oRoad);
			}
		}
		return nLonTol;
	}


	@Override
	public int compare(Road oLhs, Road oRhs)
	{
		return oRhs.m_nId - oLhs.m_nId;
	}


	class Node implements Comparable<Node>
	{
		public long m_lId;
		public int m_nLat;
		public int m_nLon;


		Node()
		{
		}


		Node(String sId, String sLat, String sLon)
		{
			m_lId = Long.parseLong(sId);
			m_nLat = MathUtil.toMicro(Double.parseDouble(sLat));
			m_nLon = MathUtil.toMicro(Double.parseDouble(sLon));
		}

	
		@Override
		public int compareTo(Node oNode)
		{
			if (m_lId < oNode.m_lId)
				return -1;

			if (m_lId > oNode.m_lId)
				return 1;

			return 0;
		}
	}


	private class GridIndex extends ArrayList<Road> implements Comparable<GridIndex>
	{
		private static final int GRID_SPACING = 50000; // ~3.5 miles
		int m_nHash;


		/**
		 * <b> Default Private Constructor </b>
		 * <p>
		 * Contains a set of polylines that represent road links and are grouped 
		 * by a grid hash index.
		 * </p>
		 */
		private GridIndex()
		{
		}

	
		public void setHash(int nX, int nY)
		{
			m_nHash = nX << 16 + nY; // 16-bit hash index by lat/lon
		}


		/**
		 * Convenience method that maps a value to a grid cell.
		 *
		 * @param nValue			the value to be mapped to a grid cell.
		 * @param nPrecision	the width of a grid cell.
		 * @return the grid cell number based on the provided precision.
		 */
		public int getGrid(int nValue)
		{
			return Utility.floor(nValue, GRID_SPACING) / GRID_SPACING;
		}

	
		@Override
		public int compareTo(GridIndex oGridIndex)
		{
			return m_nHash - oGridIndex.m_nHash;
		}
	}
}
