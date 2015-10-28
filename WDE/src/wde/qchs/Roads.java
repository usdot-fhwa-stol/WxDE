package wde.qchs;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Collections;

import wde.data.shp.Header;
import wde.data.shp.Polyline;
import wde.data.shp.Utility;
import wde.util.MathUtil;


/**
 * A singleton that reads a directory of SHP files and then responds to queries 
 * for polylines that are within a specified distance from a target point.
 * 
 * @author  bryan.krueger
 * @version 1.0 (October 2, 2015)
 */
public class Roads extends ArrayList<Roads.GridIndex> implements Runnable
{
	private static final Roads g_oRoads = new Roads();

	private String m_sShpDir;


	public static Roads getInstance()
	{
		return g_oRoads;
	}


	private Roads()
	{
		run();
	}


	@Override
	public void run()
	{
		GridIndex oSearch = new GridIndex();

		File oRoot = new File(m_sShpDir);
		File[] oDirs = oRoot.listFiles();
		for (File oDir : oDirs)
		{
			if (!oDir.isDirectory())
				continue;

			String sShpFile = oDir.getPath() + "/" + oDir.getName() + ".shp";
			try
			{
				DataInputStream oShp = new DataInputStream(
					new BufferedInputStream(new FileInputStream(sShpFile)));
				Header oHeader = new Header(oShp);
				while (oShp.available() > 0)
				{
					Polyline oPolyline = new Polyline(oShp);
					int nXbeg = getGrid(oPolyline.m_nXmin, 10000);
					int nXend = getGrid(oPolyline.m_nXmax, 10000);
					int nYbeg = getGrid(oPolyline.m_nYmin, 10000);
					int nYend = getGrid(oPolyline.m_nYmax, 10000);

					for (int nY = nYbeg; nY <= nYend; nY++) // grid cells are inclusive
					{
						for (int nX = nXbeg; nX <= nXend; nX++)
						{
							oSearch.m_nHash = nX << 16 + nY;
							GridIndex oGrid;
							int nGrid = Collections.binarySearch(this, oSearch);
							if (nGrid < 0)
							{
								oGrid = new GridIndex();
								oGrid.m_nHash = oSearch.m_nHash;
								add(~nGrid, oGrid);
							}
							else
								oGrid = get(nGrid);

							oGrid.add(oPolyline);
						}
					}
				}
				oShp.close();
			}
			catch (Exception oException)
			{
			}
		}
	}


	public Polyline getLink(double dTol, int nLon, int nLat)
	{
		GridIndex oLinkSet = new GridIndex();
		nLon = Utility.floor(nLon, 10000) / 10000;
		nLat = Utility.floor(nLat, 10000) / 10000;
		oLinkSet.m_nHash = nLon << 16 + nLat;
		int nIndex = Collections.binarySearch(this, oLinkSet);
		if (nIndex < 0) // no set of links nearby
			return null;

		int nTol = MathUtil.toMicro(dTol);
		oLinkSet = get(nIndex); // retrieve set of potential links
		nIndex = oLinkSet.size();
		while (nIndex-- > 0)
		{
			Polyline oLink = oLinkSet.get(nIndex);
			if (oLink.isInsideBounds(nLon, nLat, nTol) && 
					oLink.contextSearch(dTol, nLon, nLat))
				return oLink;
		}
		return null; // specified point not on any link
	}


	class GridIndex extends ArrayList<Polyline> 
		implements Comparable<GridIndex>
	{
		int m_nHash;


		private GridIndex()
		{
		}


		@Override
		public int compareTo(GridIndex oGridIndex)
		{
			return (m_nHash - oGridIndex.m_nHash);
		}
	}


	static int getGrid(int nValue, int nPrecision)
	{
		int nFlooredValue = nValue / nPrecision;
		if (nValue < 0) // correct for negative numbers
			--nFlooredValue;
		
		return nFlooredValue;
	}
}
