package wde.data.osm;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import wde.util.MathUtil;


/**
 * A singleton that reads a directory of openstreetmap.org PBF files and then 
 * responds to queries for roadways that are within a specified distance from 
 * a target point.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Roads extends DefaultHandler
{
	private static final String[] TYPES = {"secondary", "unclassified", 
		"residential", "service", "track"};
	private static final Roads g_oRoads = new Roads();

	boolean m_bWay;
	ArrayList<GridIndex> m_oGridCache = new ArrayList(); // 2D indexed roads
	ArrayList<GridIndex> m_oGrids = new ArrayList(); // grids that roads intersect
	ArrayList<Node> m_oNodes = new ArrayList(); // temporary node list
	ArrayList<Node> m_oWayNodes = new ArrayList(); // temporary nodes in a way
	TreeMap<String, String> m_oTags = new TreeMap(); // temporary way tags
	Node m_oSearch = new Node(); // convenience node search object


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
	 * Creates a new instance of Roads upon class loading and reads osm XML files 
	 * from a well-defined directory into memory for fast lookup. Client 
	 * components obtain a singleton reference through the getInstance method.
	 * </p>
	 */
	private Roads()
	{
		try
		{
			XMLReader iXmlReader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			iXmlReader.setContentHandler(this);
//		File[] oDirs = new File("/opt/shp").listFiles(); // default shp location
			File[] oFiles = new File("C:/Users/bryan.krueger/Desktop/wip/Clarus/vdt/osm").listFiles(); // default osm location
			for (File oFile : oFiles)
			{
				if (oFile.isDirectory() || !oFile.getName().endsWith(".osm"))
					continue; // skip directories, only need osm xml files

				BufferedInputStream oOsm = new BufferedInputStream(new FileInputStream(oFile));
				iXmlReader.parse(new InputSource(oOsm));
			}
			m_oGrids.clear(); // temporary grid buffer no longer needed
			m_oGrids = null;
			m_oNodes.clear(); // node list no longer needed
			m_oNodes = null;
			m_oSearch = null; // release temporary buffer objects
			m_oTags = null;
			m_oWayNodes = null;
		}
		catch (Exception oException)
		{
		}
	}


	@Override
 	public void startElement(String sUri, String sLocalName, 
		String sQname, Attributes iAtt)
	{
		if (sQname.compareTo("node") == 0) // insert into node lookup array
		{
			Node oNode = new Node(iAtt.getValue("id"), iAtt.getValue("lat"), 
				iAtt.getValue("lon")); // there should be no duplicate node ids
			m_oNodes.add(~Collections.binarySearch(m_oNodes, oNode), oNode);
		}

		if (sQname.compareTo("way") == 0)
			m_bWay = true; // only state change is needed

		if (sQname.compareTo("nd") == 0)
		{
			m_oSearch.m_lId = Long.parseLong(iAtt.getValue("ref"));
			int nIndex = Collections.binarySearch(m_oNodes, m_oSearch);
			if (nIndex >= 0) // add node to temporary way node list
				m_oWayNodes.add(m_oNodes.get(nIndex));
		}

		if (sQname.compareTo("tag") == 0 && m_bWay) // save way tags
			m_oTags.put(iAtt.getValue("k"), iAtt.getValue("v"));
	}


	private static boolean ignore(String sType)
	{
		if (sType == null)
			return true;
	
		int nIndex = TYPES.length;
		while (nIndex-- > 0)
			if (sType.compareTo(TYPES[nIndex]) == 0)
				return true;
	
		return false;
	}


	@Override
 	public void endElement(String sUri, String sLocalName, String sQname)
	{
		if (sQname.compareTo("way") == 0)
		{
			if (!ignore(m_oTags.get("highway"))) // filter out residential roads
			{
				Road oRoad = new Road(m_oTags, m_oWayNodes); // use buffered data
				m_oGrids.clear(); // reuse grid index set
				SegIterator oSegIt = oRoad.iterator();
				while (oSegIt.hasNext()) // get each line segment
					getGrids(m_oGrids, oSegIt.next()); // determine grid cells of line

				for (GridIndex oGrid : m_oGrids) // returned grids are already in cache
					if (!oGrid.contains(oRoad)) // treat each grid index as a set
						oGrid.add(oRoad); // add polyline definition to index
			}
			m_oWayNodes.clear(); // clear way node list
			m_oTags.clear(); // clear key value map
			m_bWay = false; // reset state
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
				int nGrid = Collections.binarySearch(m_oGridCache, oSearch);
				if (nGrid < 0) // existing grid cell not found
				{
					oGrid = new GridIndex(); // create new grid hash index
					oGrid.m_nHash = oSearch.m_nHash; // copy current hash value
					m_oGridCache.add(~nGrid, oGrid); // add new grid cell to primary cache
				}
				else
					oGrid = m_oGridCache.get(nGrid);

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


	public static void main(String[] sArgs)
		throws Exception
	{
		Roads oRoads = Roads.getInstance();
		Road oRoad = oRoads.getLink(100, -95239404, 38959084);
		System.out.println(oRoad.m_sName);
		oRoad = oRoads.getLink(100, -95816923, 39050285);
		System.out.println(oRoad.m_sName);
	}
}
