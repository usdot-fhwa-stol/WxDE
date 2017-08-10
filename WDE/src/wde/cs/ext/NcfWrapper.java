package wde.cs.ext;


import java.io.File;
import java.util.ArrayList;

import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.LatLonPointImpl;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;

import wde.util.MathUtil;
import wde.util.IntKeyValue;



/**
 NetCDF Wrapper. This package private class provides convenience methods for 
 * finding data by time range, retrieving data values by observation type, and 
 * cleaning up locally stored NetCDF files when done using them.
 */
class NcfWrapper
{
	protected int[] m_nObsTypes;
	protected ArrayList<IntKeyValue<DataStructure>> m_oArrayMap;
	long m_lStartTime;
	long m_lEndTime;
	protected String m_sHrz;
	protected String m_sVrt;
	protected String m_sTime;
	protected String[] m_sObsTypes;
	protected ArrayList<Double> m_oHrz = new ArrayList();
	protected ArrayList<Double> m_oVrt = new ArrayList();
	protected ArrayList<Double> m_oTime = new ArrayList();
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
	NcfWrapper(int[] nObsTypes, String[] sObsTypes, String sHrz, String sVrt, String sTime)
	{
		m_nObsTypes = nObsTypes;
		m_sObsTypes = sObsTypes;
		m_sHrz = sHrz;
		m_sVrt = sVrt;
		m_sTime = sTime;
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
		
		//for the time arrays, if there is only one time return index 0
		if (oValues.size() == 1)    
			return 0;
		
		double nDifference = 180;
		if (oValues.size() < 50)    //this branch will be taken for NDFD files' time array. need a better way to determine this
		{
			if (oValues.get(0) == 1.0) //check if units are in hours
			{
				oValue = oValue / 1000 / 60 / 60 + 1;  //convert from milliseconds to hours, add one to be at the first hour in the array
				//find forecasts that are an hour apart
				while (nDifference > 1) 
				{
					nDifference = oValues.get(oValues.size() - 1) - oValues.get(oValues.size() -2);
					//if the forecasts are more than an hour apart remove them
					if (nDifference > 1)
						oValues.remove(oValues.size() - 1);
				}
			}
			else if (oValues.get(0) == 30.0)  //check if units are in minutes
			{
				oValue = oValue / 1000 / 60 + 30;  //convert from milliseconds to hours, add 30 to be at the first hour in the array
				//find forecasts that are an hour apart
				while (nDifference > 60) 
				{
					nDifference = oValues.get(oValues.size() - 1) - oValues.get(oValues.size() -2);
					//if the forecasts are more than an hour apart remove them
					if (nDifference > 60)
						oValues.remove(oValues.size() - 1);
				}			
			}
		}
		double dLeft = oValues.get(0); // test for value in range
		double dRight = oValues.get(oValues.size() - 1);
		if (dRight < dLeft) // handle reversed endpoints
		{
			if (oValue + .005 < dRight || oValue > dLeft + .001)
				return -1; // outside of range
			dBasis = dLeft - dRight;
			dDist = dLeft - oValue; // tricksy
		}
		else
		{
			if (oValue < dLeft || oValue > dRight + .005)
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
	protected DataStructure getDataStructByObsId(int nObsTypeId)
	{
		IntKeyValue<DataStructure> oSearch = new IntKeyValue(nObsTypeId, null);
		int nIndex = m_oArrayMap.size();
		while (nIndex-- > 0)
		{
			if (m_oArrayMap.get(nIndex).compareTo(oSearch) == 0)
				return m_oArrayMap.get(nIndex).value();
		}
		return null; // requested obstype not available
	}


	/**
	 * Attempts to load the specified NetCDF file. Parent container sets 
	 * its management parameters when this method succeeds.
	 *
	 * @param sFilename	the NetCDF file name to load.
	 */
	void load(long lStartTime, long lEndTime, String sFilename)
		throws Exception
	{
		NetcdfFile oNcFile = NetcdfFile.open(sFilename); // stored on RAM disk
		ArrayList<Double> oHrz = fromArray(oNcFile, m_sHrz); // sort order varies
		ArrayList<Double> oVrt = fromArray(oNcFile, m_sVrt);
		ArrayList<Double> oTime = fromArray(oNcFile, m_sTime);
		// create obstype array mapping
		ArrayList<IntKeyValue<DataStructure>> oArrayMap = new ArrayList(m_nObsTypes.length);
		for (GridDatatype oGrid : new GridDataset(new NetcdfDataset(oNcFile)).getGrids())
		{
			for (int nObsTypeIndex = 0; nObsTypeIndex < m_sObsTypes.length; nObsTypeIndex++)
			{ // save obs type id to grid name mapping
				if (m_sObsTypes[nObsTypeIndex].contains(oGrid.getName()))
					oArrayMap.add(new IntKeyValue(m_nObsTypes[nObsTypeIndex], new DataStructure(m_nObsTypes[nObsTypeIndex], oGrid.getProjection(), oGrid.getVariable(), oGrid.getVariable().read())));
			}
		}

		m_oHrz = oHrz;// update state variables when everything succeeds
		m_oVrt = oVrt;
		m_oTime = oTime;
		m_oArrayMap = oArrayMap;
		m_oNcFile = oNcFile;
		m_lStartTime = lStartTime;
		m_lEndTime = lEndTime;
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
		DataStructure oDataStruct = getDataStructByObsId(nObsTypeId);
		if (oDataStruct == null)
			return Double.NaN; // requested observation type not supported
			
		oDataStruct.m_oProj.latLonToProj(oLatLon, oProjPoint);
		int nHrz = getIndex(m_oHrz, oProjPoint.getX());
		int nVrt = getIndex(m_oVrt, oProjPoint.getY());
		double dTimeSince = lTimestamp - m_lStartTime;
		int nTime = getIndex(m_oTime, dTimeSince);

		if (nHrz < 0 || nVrt < 0 || nTime < 0)
			return Double.NaN; // projected coordinates are outside data ranage
			

		try
		{
			Index oIndex = oDataStruct.m_oArray.getIndex();
			oIndex.setDim(oDataStruct.m_oVar.findDimensionIndex(m_sHrz), nHrz);
			oIndex.setDim(oDataStruct.m_oVar.findDimensionIndex(m_sVrt), nVrt);
			oIndex.setDim(oDataStruct.m_oVar.findDimensionIndex(m_sTime), nTime);
			
			double dVal = oDataStruct.m_oArray.getDouble(oIndex);
			if (oDataStruct.m_oVar.isFillValue(dVal) || oDataStruct.m_oVar.isInvalidData(dVal) || oDataStruct.m_oVar.isMissing(dVal))
				return Double.NaN; // no valid data for specified location

			return dVal;
		}
		catch (Exception oException)
		{
		}

		return Double.NaN;
	}
	
	class DataStructure
	{
		public int m_nObsTypeId;
		public ProjectionImpl m_oProj;
		public VariableDS m_oVar;
		public Array m_oArray;
		
		DataStructure(int nObsTypeId, ProjectionImpl oProj,VariableDS oVar, Array oArray)
		{
			m_nObsTypeId = nObsTypeId;
			m_oProj = oProj;
			m_oVar = oVar;
			m_oArray = oArray;
		}
	}
}
