package wde.qchs;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import wde.data.shp.Polyline;
import wde.data.shp.PolylinePart;
import wde.data.shp.Utility;
import wde.util.MathUtil;


/**
 * A singleton that reads a directory of SHP files and then responds to queries 
 * for polylines that are within a specified distance from a target point.
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
		File[] oDirs = new File("/opt/shp").listFiles(); // default shp location
		for (File oDir : oDirs)
		{
			if (!oDir.isDirectory())
				continue; // shp files should be inside indiviual directories

			String sShpFile = oDir.getPath() + "/" + oDir.getName() + ".shp";
			try
			{
				DataInputStream oShp = new DataInputStream(
					new BufferedInputStream(new FileInputStream(sShpFile)));
				oShp.skipBytes(100); // ignore shp header section
				ArrayList<GridIndex> oGrids = new ArrayList();
				Polyline oPolyline = new Polyline();
				while (oShp.available() > 0) // polyline data are available
				{
					oGrids.clear(); // reuse grid index set
					int[] oPolyData = Polyline.read(oShp); // read polyline definition
					oPolyline.set(oPolyData);
					while (oPolyline.hasNext()) // for each part of the polyline
					{
						PolylinePart oPart = oPolyline.next();
						while (oPart.hasNext()) // get each line segment
							getGrids(oGrids, oPart.next()); // determine grid cells of line
					}

					for (GridIndex oGrid : oGrids)
						if (!oGrid.contains(oPolyData)) // treat the grid index as a set
							oGrid.add(oPolyData); // add polyline definition to index
				}
				oShp.close(); // done reading shape file
			}
			catch (Exception oException)
			{
			}
		}
	}


	/**
	 * Determines the set of grid cells that intersect the specified area.
	 *
	 * @param nXmin	the smallest horizontal coordinate of the target region.
	 * @param nYmin	the smallest vertical coordinate of the target region.
	 * @param nXmax	the largest horizontal coordinate of the target region.
	 * @param nYmax	the largest vertical coordinate of the target region.
	 * 
	 * @return the list of grid cells that intersect the defined region.
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
	public Polyline getLink(int nTol, int nLon, int nLat)
	{
		ArrayList<GridIndex> oGrids = new ArrayList();
		int nLonTol = (int)(nTol / Math.cos(Math.PI * MathUtil.fromMicro(nLat) / 180.0));
		getGrids(oGrids, new int[]{nLon - nLonTol, nLat - nTol, nLon + nLonTol, nLat + nTol});
		if (oGrids.isEmpty()) // no set of links nearby
			return null;

		int nDist = Integer.MAX_VALUE; // track minimum distance
		int[] nPolyData = null;
		Polyline oLink = new Polyline();
		for (GridIndex oGrid : oGrids)
		{
			for (int[] oPoly : oGrid)
			{
				oLink.set(oPoly); // use the longitude adjusted tolerance
				int nSqDist = oLink.snap(nLonTol, nLon, nLat);
				if (nSqDist >= 0 && nSqDist < nDist)
				{
					nPolyData = oPoly; // save the polyline data the 
					nDist = nSqDist; // shortest distance from the point
				}
			}
		}

		if (nDist == Integer.MAX_VALUE)
			return null; // no link found
	
		oLink.set(nPolyData);
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


	/**
	 * <b> Default Private Constructor </b>
	 * <p>
	 * Contains a set of polylines that represent road links and are grouped 
	 * by a grid hash index.
	 * </p>
	 */
	class GridIndex extends ArrayList<int[]> implements Comparable<GridIndex>
	{
		private static final int GRID_SPACING = 50000; // ~3.5 miles
		int m_nHash;


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
