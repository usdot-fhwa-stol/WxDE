package wde.data.osm;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import wde.data.osm.OsmLatLon;
import wde.data.osm.Road;
import wde.data.osm.SegIterator;
import wde.data.osm.Utility;
import wde.util.MathUtil;

import de.topobyte.osm4j.core.access.OsmIterator;
import de.topobyte.osm4j.core.model.iface.EntityContainer;
import de.topobyte.osm4j.core.model.iface.EntityType;
import de.topobyte.osm4j.core.model.iface.OsmNode;
import de.topobyte.osm4j.core.model.iface.OsmWay;
import de.topobyte.osm4j.pbf.seq.PbfIterator;
//import de.topobyte.osm4j.xml.dynsax.OsmXmlIterator;


/**
 * A singleton that reads a directory of openstreetmap.org PBF files and then 
 * responds to queries for roadways that are within a specified distance from 
 * a target point.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Roads extends ArrayList<Roads.GridIndex> implements Comparator<int[]>
{
	private static final Roads g_oRoads = new Roads();


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
	 * Creates a new instance of Roads upon class loading and reads shape files 
	 * from a well-defined directory into memory for fast lookup. Client 
	 * components obtain a singleton reference through the getInstance method.
	 * </p>
	 */
	private Roads()
	{
		// reusable list that contains the set of grids each road intersects
		ArrayList<GridIndex> oGrids = new ArrayList();
		OsmLatLon oNodeSort = new OsmLatLon(); // use sorting method
		ArrayList<OsmLatLon> oNodes = new ArrayList(); // node data lookup list

//		File[] oDirs = new File("/opt/shp").listFiles(); // default shp location
		File[] oFiles = new File("C:/Users/bryan.krueger/Desktop/wip/Clarus/vdt/osm").listFiles(); // default osm location
		for (File oFile : oFiles)
		{
			if (oFile.isDirectory() || !oFile.getName().endsWith(".pbf"))
				continue; // skip directories, only need pbf files

			try
			{
				BufferedInputStream oOsm = new BufferedInputStream(new FileInputStream(oFile));
				OsmIterator oOsmIt = new PbfIterator(oOsm, false); // read nodes and ways
				for (EntityContainer oCont : oOsmIt) // iterate through entities
				{
					if (oCont.getType() == EntityType.Node) // nodes encountered first
					{
						OsmNode oNode = (OsmNode)oCont.getEntity();
						OsmLatLon oLatLon = new OsmLatLon(oNode.getId(), 
							oNode.getLatitude(), oNode.getLongitude());
						int nIndex = Collections.binarySearch(oNodes, oLatLon, oNodeSort);
						if (nIndex < 0) // save nodes in sorted order
							oNodes.add(~nIndex, oLatLon); // should never be any duplicates
					}
 
					if (oCont.getType() == EntityType.Way) // process ways after nodes
					{
						Road oRoad = new Road(oNodes, oNodeSort, (OsmWay)oCont.getEntity());
						oGrids.clear(); // reuse grid index set
						SegIterator oSegIt = oRoad.iterator();
						while (oSegIt.hasNext()) // get each line segment
							getGrids(oGrids, oSegIt.next()); // determine grid cells of line

						for (GridIndex oGrid : oGrids)
							if (!oGrid.contains(oRoad)) // treat the grid index as a set
								oGrid.add(oRoad); // add polyline definition to index
					}
				}
				oOsm.close(); // done reading shape file
			}
			catch (Exception oException)
			{
				oException.printStackTrace();
			}
		}
	}


	/**
	 * Determines the set of grid cells that intersect the specified area.
	 *
	 * @param oGrids	an array that accumulates GridIndex object that 
	 *								intersect the provided region.
	 * @param oRegion	a pair of integer coordinates representing the 
	 *								region used for comparison to a GridIndex.
	 */
	private void getGrids(ArrayList<GridIndex> oGrids, int[] oRegion)
	{
		int nXmin = oRegion[0]; // copy parts of array
		int nYmin = oRegion[1];
		int nXmax = oRegion[2];
		int nYmax = oRegion[3];
	
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

		if (nXmin < -180000000 || nXmax > 179999999 || nYmin < -84999999 || nYmax > 84999999)
			return; // locations fall outside the geographic model
	
		GridIndex oSearch = new GridIndex(); // find polyline sets by hash index
		int nXbeg = oSearch.getGrid(nXmin);
		int nXend = oSearch.getGrid(nXmax);
		int nYbeg = oSearch.getGrid(nYmin);
		int nYend = oSearch.getGrid(nYmax);

		GridIndex oGrid;
		for (int nY = nYbeg; nY <= nYend; nY++) // grid cells are inclusive
		{
			for (int nX = nXbeg; nX <= nXend; nX++)
			{
				oSearch.setHash(nX, nY);
				int nGrid = Collections.binarySearch(this, oSearch);
				if (nGrid < 0) // existing grid cell not found
				{
					oGrid = new GridIndex(); // create new grid hash index
					oGrid.m_nHash = oSearch.m_nHash; // copy current hash value
					add(~nGrid, oGrid); // add new grid cell to primary cache
				}
				else
					oGrid = get(nGrid);

				nGrid = Collections.binarySearch(oGrids, oSearch);
				if (nGrid < 0) // add grid cell reference to output set
					oGrids.add(~nGrid, oGrid);
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
		ArrayList<GridIndex> oGrids = new ArrayList();
		int nLonTol = (int)(nTol / Math.cos(Math.PI * MathUtil.fromMicro(nLat) / 180.0));
		getGrids(oGrids, new int[]{nLon - nLonTol, nLat - nTol, nLon + nLonTol, nLat + nTol});
		if (oGrids.isEmpty()) // no set of links nearby
			return null;

		int nDist = Integer.MAX_VALUE; // track minimum distance
		Road oLink = null;
		for (GridIndex oGrid : oGrids)
		{
			for (Road oRoad : oGrid)
			{
				// use the longitude adjusted tolerance
				int nSqDist = oRoad.snap(nLonTol, nLon, nLat);
				if (nSqDist >= 0 && nSqDist < nDist)
				{
					oLink = oRoad; // save the polyline data that is the 
					nDist = nSqDist; // shortest distance from the point
				}
			}
		}

		if (nDist == Integer.MAX_VALUE)
			return null; // no link found
	
		return oLink;
	}


	@Override
	public int compare(int[] oLhs, int[] oRhs)
	{
		int nCompare = oLhs[0] - oRhs[0];
		if (nCompare == 0)
			return oLhs[1] - oRhs[1];
		return nCompare;
	}


	class GridIndex extends ArrayList<Road> implements Comparable<GridIndex>
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


	public static void main(String[] sArgs)
		throws Exception
	{
		Roads oRoads = Roads.getInstance();
		Road oRoad = oRoads.getLink(100, -84572239, 43610290);
		System.out.println(oRoad.m_sName);
	}
}
