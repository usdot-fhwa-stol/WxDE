package wde.cs.ext;


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

import java.io.File;
import java.util.ArrayList;
import java.util.List;


/**
 NetCDF Wrapper. This package private class provides convenience methods for 
 * finding data by time range, retrieving data values by observation type, and 
 * cleaning up locally stored NetCDF files when done using them.
 */
class NcfWrapper
{
	protected int[] m_nObsTypes;
	protected int[] m_nGridMap;
	long m_lStartTime;
	long m_lEndTime;
	protected String m_sHrz;
	protected String m_sVrt;
	protected String[] m_sObsTypes;
	protected ArrayList<Double> m_oHrz = new ArrayList();
	protected ArrayList<Double> m_oVrt = new ArrayList();
	protected List<GridDatatype> m_oGrids;
	protected NetcdfFile m_oNcFile;


	/**
	 * Default private constructor. The default constructor should not be needed 
	 * as the file loading operation requires mapping variables to be initialized.
	 */
	private NcfWrapper()
	{
	}

	
	/**
	 * NcfWrapper package private constructor. This constructor is used by the 
	 * parent container to initialize name and observation type mappings used 
	 * by this class to manipulate source-specific data parameters.
	 *
	 * @param nObsTypes	lookup observation type id array corresponding with names.
	 * @param sObsTypes	lookup observation name array corresponding with ids.
	 * @param sHrz			name of the horizontal NetCDF index variable.
	 * @param sVrt			name of the vertical NetCDF index variable.
	 */
	NcfWrapper(int[] nObsTypes, String[] sObsTypes, String sHrz, String sVrt)
	{
		m_nObsTypes = nObsTypes;
		m_sObsTypes = sObsTypes;
		m_sHrz = sHrz;
		m_sVrt = sVrt;
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
	 * Attempts to load the specified NetCDF file. Parent container sets 
	 * its management parameters when this method succeeds.
	 *
	 * @param sFilename	the NetCDF file name to load.
	 */
	void load(String sFilename)
		throws Exception
	{
		NetcdfFile oNcFile = NetcdfFile.open(sFilename); // stored on RAM disk
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

		m_oHrz = oHrz;// update state variables when everything succeeds
		m_oVrt = oVrt;
		m_oGrids = oGrids;
		m_nGridMap = nGridMap;
		m_oNcFile = oNcFile;
	}


	/**
	 * Remove NetCDF files that are no longer in use.
	 */
	void cleanup()
	{
		try
		{
			String sFullPath = m_oNcFile.getLocation(); // save filename before close
			m_oNcFile.close(); // close NetCDF file before removing related files

			int nIndex = sFullPath.lastIndexOf(".gz");
			if (nIndex >= 0) // remove .gz for the gzip special case
				sFullPath = sFullPath.substring(0, nIndex);

			String sPath = "/";
			nIndex = sFullPath.lastIndexOf("/");
			if (nIndex > 0) // check for root directory special case
				sPath = sFullPath.substring(0, nIndex);

			String sPattern = sFullPath.substring(++nIndex);
			File oDir = new File(sPath);
			if (!oDir.exists()) // verify containing directory exists
				return;

			File[] oFiles = oDir.listFiles(); // search for matching file names
			for (nIndex = 0; nIndex < oFiles.length; nIndex++)
			{
				File oFile = oFiles[nIndex]; // delete files related to original NetCDF
				if (oFile.isFile() && oFile.getName().contains(sPattern) && oFile.exists())
					oFile.delete(); // verify file exists before attempting to delete
			}
		}
		catch (Exception oException)
		{
		}
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
	 *		specified time at the specified location.
	 */
	public synchronized double getReading(int nObsTypeId, long lTimestamp, 
		int nLat, int nLon)
	{
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
