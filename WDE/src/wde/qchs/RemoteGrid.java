package wde.qchs;


import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;
import wde.util.MathUtil;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * This abstract base class implements common NetCDF patterns for identifying, 
 * downloading, reading, and retrieving observation values for remote data sets.
 */
abstract class RemoteGrid implements Runnable
{
	/**
	* Lookup arrays map names between model and observation types.
	*/
	protected int[] m_nObsTypes;
	protected int[] m_nGridMap;
	protected long m_lStartTime;
	protected long m_lEndTime;
	protected String m_sHrz;
	protected String m_sVrt;
	protected String m_sBaseDir = "/dev/shm/";
	protected String m_sBaseURL;
	protected String[] m_sObsTypes;
	protected ArrayList<Double> m_oHrz = new ArrayList();
	protected ArrayList<Double> m_oVrt = new ArrayList();
	protected List<GridDatatype> m_oGrids;
	protected NetcdfFile m_oPrevNcf;


	/**
	 * Default package private constructor.
	 */
	RemoteGrid()
	{
	}


	/**
	 * A static utility method that returns the nearest match in an array of 
	 * doubles to the target double value. The method assumes that the data 
	 * have a constant delta and approximates the index using value ratios.
	 *
	 * @param oValues		the array of double values to search.
	 * @param oValue		the double value to find.
	 * 
	 * @return	the nearest index of the stored value to the target value
	 */
	protected static int getIndex(ArrayList<Double> oValues, Double oValue)
	{
		double dBasis;
		double dDist;
		double dLeft = oValues.get(0); // test for value in range
		double dRight = oValues.get(oValues.size() - 1);
		if (dRight < dLeft) // handle reversed endpoints
		{
			if (oValue < dRight || oValue > dLeft)
				return -1; // outside of range
			dBasis = dLeft - dRight;
			dDist = dLeft - oValue; // tricksy
		}
		else
		{
			if (oValue < dLeft || oValue > dRight)
				return -1; // outside of range
			dBasis = dRight - dLeft;
			dDist = oValue - dLeft;
		}

		return (int)(dDist / dBasis * (double)oValues.size());
	}


	/**
	 * A utility method that creates an array of double values from the 
	 * in-memory NetCDF data by object layer name.
	 *
	 * @param oNcFile reference to the loaded NetCDF data
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
	 * Abstract method overridden by subclasses to determine the remote and local 
	 * file name for their specific remote data set.
	 *
	 * @param oNow	timestamp Date object used for time-based dynamic URLs
	 * 
	 * @return the URL where remote data can be retrieved.
	 */
	protected abstract String getFilename(Date oNow);


	/**
	 * Regularly called on a schedule to refresh the cached model data with 
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

		String sFilename = getFilename(oNow.getTime());
		if (sFilename == null || (m_oPrevNcf != null && 
			m_oPrevNcf.getLocation().contains(sFilename)))
			return; // file name could be resolved or matches previous download

		String sDestFile = m_sBaseDir; // ignore intervening directories in path
		int nSepIndex = sFilename.lastIndexOf("/");
		if (nSepIndex >= 0)
			sDestFile += sFilename.substring(nSepIndex); // extract the file name
		else
			sDestFile += sFilename; // local file name

		try
		{
			URL oUrl = new URL(m_sBaseURL + sFilename); // retrieve remote data file
			BufferedInputStream oIn = new BufferedInputStream(oUrl.openStream());
			BufferedOutputStream oOut = new BufferedOutputStream(
				new FileOutputStream(sDestFile));
			int nByte; // copy remote data to local file
			while ((nByte = oIn.read()) >= 0)
				oOut.write(nByte);
			oIn.close(); // tidy up input and output streams
			oOut.close();
			
			NetcdfFile oNcFile = NetcdfFile.open(sDestFile); // stored on RAM disk
			ArrayList<Double> oHrz = fromArray(oNcFile, m_sHrz); // sort order varies
			ArrayList<Double> oVrt = fromArray(oNcFile, m_sVrt);
			
			int[] nGridMap = new int[m_nObsTypes.length]; // create obstype grid index map
			List<GridDatatype> oGrids = new GridDataset(new NetcdfDataset(oNcFile)).getGrids();
			for (int nOuter = 0; nOuter < nGridMap.length; nOuter++)
			{
				nGridMap[nOuter] = -1; // default to invalid index
				for (int nInner = 0; nInner < oGrids.size(); nInner++)
				{
					if (m_sObsTypes[nOuter].contains(oGrids.get(nInner).getName()))
						nGridMap[nOuter] = nInner; // save name mapping				
				}
			}

			synchronized(this) // update state variables when everything succeeds
			{
				m_oHrz = oHrz;
				m_oVrt = oVrt;
				m_oGrids = oGrids;
				m_nGridMap = nGridMap;
				m_lStartTime = oNow.getTimeInMillis() - 300000; // 5-minute margin
				m_lEndTime = m_lStartTime + 4200000; // data are valid for 70 minutes
			}

			synchronized(this) // synchronize slow cleanup operations separately
			{
				if (m_oPrevNcf != null)
				{
					File oPrevNcf = new File(m_oPrevNcf.getLocation());
					m_oPrevNcf.close(); // release previously cached grids
					if (oPrevNcf.exists() && !oPrevNcf.isDirectory())
						oPrevNcf.delete(); // free previous file resources
				}
				m_oPrevNcf = oNcFile; // persist NetCDF data connection
			}			
		}
		catch(Exception oException) // failed to download new data
		{
		}
	}


	/**
	 * Retrieves the grid data associated with supported observation types.
	 *
	 * @param nObsTypeId	the observation type identifier used to find grid data.
	 * 
	 * @return the grid data for the variable specified by observation type.
	 */
	protected GridDatatype getGridByObs(int nObsTypeId)
	{
		boolean bFound = false;
		int nIndex = m_nObsTypes.length;
		while (!bFound && nIndex-- > 0)
			bFound = (m_nObsTypes[nIndex] == nObsTypeId);

		if (!bFound || m_nGridMap[nIndex] < 0) // requested obstype not available
			return null;

		return m_oGrids.get(m_nGridMap[nIndex]); // grid by obstypeid
	}


	/**
	 * Finds the model value for an observation type by time and location.
	 *
	 * @param nObsTypeId	the observation type to lookup.
	 * @param lTimestamp	the timestamp of the observation.
	 * @param nLat				the latitude of the requested data.
	 * @param nLon				the longitude of the requested data.
	 * 
	 * @return	the model value for the requested observation type for the 
	 *					specified time at the specified location.
	 */
	public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon)
	{
		if (lTimestamp < m_lStartTime || lTimestamp >= m_lEndTime)
			return Double.NaN; // requested time outside of buffered time range

		LatLonPointImpl oLatLon = new LatLonPointImpl(MathUtil.fromMicro(nLat), 
			MathUtil.fromMicro(nLon));
		ProjectionPointImpl oProjPoint = new ProjectionPointImpl();
		GridDatatype oGrid = getGridByObs(nObsTypeId);
		if (oGrid == null)
			return Double.NaN; // requested observation type not supported
			
		oGrid.getProjection().latLonToProj(oLatLon, oProjPoint);
		int nHrz = getIndex(m_oHrz, oProjPoint.getX());
		int nVrt = getIndex(m_oVrt, oProjPoint.getY());
		if (nHrz < 0 || nVrt < 0)
			return Double.NaN; // projected coordinates are outside data ranage

		try
		{
			VariableDS oVar = oGrid.getVariable();
			Array oArray = oVar.read();
			Index oIndex = oArray.getIndex();
			oIndex.setDim(oVar.findDimensionIndex(m_sHrz), nHrz);
			oIndex.setDim(oVar.findDimensionIndex(m_sVrt), nVrt);
	
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
}
