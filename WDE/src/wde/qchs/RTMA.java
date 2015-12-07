package wde.qchs;


import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
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
import wde.util.Scheduler;
import wde.util.MathUtil;


/**
 * Real-Time Mesoscale Analysis. This singleton class downloads hourly RTMA 
 * files from the National Weather Service and provides a lookup method to 
 * retrieve the model value for the supported observation types that are then 
 * used to quality check the measured observation.
 */
public final class RTMA implements Runnable
{
	/**
	* Static arrays provide name mapping between RTMA model and WxDE obs types.
	*/
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
		"'pub/data/nccf/com/rtma/prod/rtma2p5.'yyyyMMdd'/rtma2p5.t'HH'z.2dvarges_ndfd.grb2'");
	private List<GridDatatype> m_oGrids;


	/**
	 * <b> Default Private Constructor </b>
	 * <p>
	 * Creates a new instance of RTMA upon class loading. Client components 
	 * obtain a singleton reference through the getInstance method.
	 * </p>
	 */
	private RTMA()
	{
		m_oSrcFile.setTimeZone(TimeZone.getTimeZone("Etc/UTC"));
		run(); // manually initialize first run
		Scheduler.getInstance().schedule(this, 60 * 55, 3600, true); // schedule updates
	}


	/**
	 * Returns a reference to singleton RTMA model data cache.
	 *
	 * @return reference to RTMA instance.
	 */
	public static RTMA getInstance()
	{
		return g_oRTMA;
	}


	/**
	 * Convenience method that creates an array of double values from the 
	 * in-memory NetCDF data by object layer name.
	 *
	 * @param oNcFile reference to the loaded NetCDF RTMA data
	 * @param sName   Name of the observation layer to read.
	 * 
	 * @return an array of the copied (@code Double} observation values.
	 */
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
	

	/**
	 * Regularly called on a schedule to refresh the RTMA model cache with 
	 * the most recently published model file.
	 */
	@Override
	public void run()
	{
		GregorianCalendar oNow = new GregorianCalendar();
		if (oNow.get(Calendar.MINUTE) < 55) // backup to previous hour
			oNow.setTimeInMillis(oNow.getTimeInMillis() - 3600000);
		oNow.set(Calendar.MILLISECOND, 0); // adjust clock to scheduled time
		oNow.set(Calendar.SECOND, 0);
		oNow.set(Calendar.MINUTE, 55);
		String sSrcFile = m_oSrcFile.format(oNow.getTime());

		try
		{
			URI oURI = new URI("ftp://anonymous:clarus.mixonhill.com@" + 
				"ftp.ncep.noaa.gov/" + sSrcFile);
			NetcdfFile oNcFile = NetcdfFile.openInMemory(oURI);
			ArrayList<Double> oX = fromArray(oNcFile, "x"); // sorted low to high
			ArrayList<Double> oY = fromArray(oNcFile, "y");
			
			int[] nGridMap = new int[OBS_TYPES.length]; // create obstype grid index map
			List<GridDatatype> oGrids = new GridDataset(new NetcdfDataset(oNcFile)).getGrids();
			for (int nOuter = 0; nOuter < nGridMap.length; nOuter++)
			{
				nGridMap[nOuter] = -1; // default to invalid index
				for (int nInner = 0; nInner < oGrids.size(); nInner++)
				{
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
				m_lStartTime = oNow.getTimeInMillis() - 300000; // 5-minute margin
				m_lEndTime = m_lStartTime + 4200000; // data are valid for 70 minutes
			}
		}
		catch(Exception oException) // failed to download new data
		{
		}
	}


	/**
	 * Finds the RTMA model value for an observation type by time and location.
	 *
	 * @param nObsTypeId	the observation type to lookup.
	 * @param lTimestamp	the timestamp of the observation.
	 * @param nLat				the latitude of the requested data.
	 * @param nLon				the longitude of the requested data.
	 * 
	 * @return	the RTMA model value for the requested observation type for the 
	 *					specified time at the specified location.
	 */
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

			if (nObsTypeId == 554) // convert pressure Pa to mbar
				return dVal / 100.0;
	
			if (nObsTypeId == 5733) // convert temperature K to C
				return dVal - 273.15;
	
			return dVal; // no conversion necessary for other observation types
		}
		catch (Exception oException)
		{
		}

		return Double.NaN;
	}
}
