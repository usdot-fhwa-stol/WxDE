package wde.qchs;


import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;
//import wde.util.Scheduler;
import wde.util.MathUtil;


public class RTMA implements Runnable
{
	private static final int[] OBS_TYPES = {575, 554, 5733, 5101, 56105, 56108, 56104};
	private static final String[] NC_NAMES = 
	{
		"Dewpoint_temperature_height_above_ground", "Pressure_surface", 
		"Temperature_height_above_ground", "Visibility_surface", 
		"Wind_direction_from_which_blowing_height_above_ground", 
		"Wind_speed_gust_height_above_ground", "Wind_speed_height_above_ground"
	};
	private static final RTMA g_oRTMA = new RTMA();

	private int[] m_nGridMap;
	private long m_lStartTime;
	private long m_lEndTime;
	private ArrayList<Double> m_oX = new ArrayList();
	private ArrayList<Double> m_oY = new ArrayList();
	private final SimpleDateFormat m_oSrcFile = new SimpleDateFormat(
		"'/pub/data/nccf/com/rtma/prod/rtma2p5.'yyyyMMdd'/rtma2p5.t'HH'z.2dvarges_ndfd.grb2'");
	private List<GridDatatype> m_oGrids;


	private RTMA()
	{
		m_oSrcFile.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		run();
//		Scheduler.getInstance().schedule(this, 60 * 55, 3600, true);
	}


	public static RTMA getInstance()
	{
		return g_oRTMA;
	}


	private static ArrayList<Double> fromArray(NetcdfFile oNcFile, String sName)
		throws Exception
	{
		Array oArray = oNcFile.getRootGroup().findVariable(sName).read();
		int nSize = (int)oArray.getSize();

		ArrayList<Double> oArrayList = new ArrayList(nSize); // reserve capacity
		for (int nIndex = 0; nIndex < nSize; nIndex++)
			oArrayList.add(oArray.getDouble(nIndex)); // copy values
		
		return oArrayList;
	}
	

	@Override
	public void run()
	{
		long lTempStart = System.currentTimeMillis();
		Date oDate = new Date(lTempStart - 55 * 60000); // run 55 minutes late
		String sSrcFile = m_oSrcFile.format(oDate);

		try
		{
			URI oUri = new URI("ftp://anonymous:clarus.mixonhill.com@ftp.ncep.noaa.gov" + sSrcFile);
			NetcdfFile oNcFile = NetcdfFile.openInMemory(oUri);
//			NetcdfFile oNcFile = NetcdfFile.openInMemory("C:/Users/bryan.krueger/Desktop/wip/Clarus/vdt/rtma2p5.t13z.2dvarges_ndfd.grb2");
			ArrayList<Double> oX = fromArray(oNcFile, "x"); // sorted low to high
			ArrayList<Double> oY = fromArray(oNcFile, "y");
			
			int[] nGridMap = new int[OBS_TYPES.length]; // create obstype grid index map
			List<GridDatatype> oGrids = new GridDataset(new NetcdfDataset(oNcFile)).getGrids();
			for (int nOuter = 0; nOuter < nGridMap.length; nOuter++)
			{
				nGridMap[nOuter] = -1; // default to invalid index
				for (int nInner = 0; nInner < oGrids.size(); nInner++)
				{
//					if (oGrids.get(nInner).getName().compareTo(NC_NAMES[nOuter]) == 0)
					if (NC_NAMES[nOuter].contains(oGrids.get(nInner).getName()))
						nGridMap[nOuter] = nInner; // save RTMA name mapping				
				}
			}
			
			synchronized(this) // update state variables when everything succeeds
			{
				m_oX = oX;
				m_oY = oY;
				m_oGrids = oGrids;
				m_nGridMap = nGridMap;
				m_lStartTime = lTempStart;
				m_lEndTime = lTempStart + 3900000; // data are valid for 65 minutes
			}
		}
		catch(Exception oException) // failed to download new data
		{
		}
	}


	public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon)
	{
		if (lTimestamp < m_lStartTime || lTimestamp >= m_lEndTime)
			return Double.NaN; // requested time outside of buffered data range

		boolean bFound = false;
		int nIndex = OBS_TYPES.length;
		while (!bFound && nIndex-- > 0)
			bFound = (OBS_TYPES[nIndex] == nObsTypeId);

		if (!bFound || m_nGridMap[nIndex] < 0) // requested obstype not available
			return Double.NaN;

		GridDatatype oGrid = m_oGrids.get(m_nGridMap[nIndex]); // grid by obstypeid
		LatLonPointImpl oLatLon = new LatLonPointImpl(
			MathUtil.fromMicro(nLat), MathUtil.fromMicro(nLon));
		ProjectionPointImpl oProjPoint = new ProjectionPointImpl();
		oGrid.getProjection().latLonToProj(oLatLon, oProjPoint);

		double dX = oProjPoint.getX();
		if (dX < m_oX.get(0) || dX > m_oX.get(m_oX.size() - 1))
			return Double.NaN; // projected horizontal coordinate outside data ranage

		double dY = oProjPoint.getY();
		if (dY < m_oY.get(0) || dY > m_oY.get(m_oY.size() - 1))
			return Double.NaN; // projected vertical coordinate outside data ranage

		int nX = Collections.binarySearch(m_oX, dX);
		if (nX < 0) // get twos-complement when no exact match
			nX = ~nX;

		int nY = Collections.binarySearch(m_oY, dY);
		if (nY < 0) // get twos-complement when no exact match
			nY = ~nY;

		try
		{
			VariableDS oVar = oGrid.getVariable();
			Array oArray = oVar.read();
			Index oIndex = oArray.getIndex();
			oIndex.setDim(oVar.findDimensionIndex("x"), nX);
			oIndex.setDim(oVar.findDimensionIndex("y"), nY);
	
			double dVal = oArray.getDouble(oIndex);
			if (oVar.isFillValue(dVal) || oVar.isInvalidData(dVal) || oVar.isMissing(dVal))
				return Double.NaN; // no valid data for specified location

			return dVal;
		}
		catch (Exception oException)
		{
		}

		return Double.NaN;
	}


	public static void main(String[] sArgs)
		throws Exception
	{
		RTMA oRTMA = RTMA.getInstance();
		System.out.println(oRTMA.getReading(5733, System.currentTimeMillis(), 45000000, -94000000));
		System.out.println(oRTMA.getReading(554, System.currentTimeMillis(), 45000000, -94000000));
	}
}
