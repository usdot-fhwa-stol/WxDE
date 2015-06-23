// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qchs;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.ListIterator;
import java.util.TimeZone;
import clarus.emc.Stations;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.ma2.Array;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.grid.GridDataset;
import ucar.unidata.geoloc.ProjectionImpl;
import ucar.unidata.geoloc.ProjectionPointImpl;
import util.Config;
import util.ConfigSvc;
import util.Scheduler;
import util.net.FtpConn;


/**
 * The RSAS class downloads a grib2 file from the NOAA.gov ftp site, and
 * gets data from the file for wind u and v components (used to calculate wind
 * magnitude, specific humidity (used to calculate relative humidity), and
 * temperature.   The Grib class is designed for the grib2 file to
 * contain data points corresponding to theNCEP g88 grid.  It has been setup to 
 * simplify reconfiguration if the grid used by the grib2 file changes, but it
 * will not dynamically adapt to the change.
 * @author scot.lange
 */
public class RSAS implements Runnable//, singleton
{
	//Grid size
	private static final int POINT_COUNT_X = 580;
	private static final int POINT_COUNT_Y = 548;
	private static final int GRID_SIZE = POINT_COUNT_X * POINT_COUNT_Y;

	//Max specific humidity for every 5 degrees celsius
	private float[] m_fMaxHumidity = new float[] {0.0001f,   //-40
												  0.0001f,   //-35
												  0.0003f,   //-30
												  0.0003f,   //-25
												  0.00075f,  //-20
												  0.00075f,  //-15
												  0.002f,    //-10
												  0.002f,    //-5
												  0.003767f, //0
												  0.005387f, //5
												  0.007612f, //10
												  0.01062f,  //15
												  0.014659f, //20
												  0.019826f, //25
												  0.027125f, //30
												  0.035f,    //35
												  0.047f,    //40
												  0.069f,	 //45
												  0.095f};	 //50
	
	
	/**
	 * Projection object used to convert x,y coordinates on the grid to lat/lon
	 */
	ProjectionImpl m_oProjection = null;
	//Get the values along the x-axis (native coordinates in the grid)
	Array oXPoints;
	//Get the values along the y-axis (native coordinates in the grid)
	Array oYPoints;

	private double[] m_dPressure = new double[GRID_SIZE];
	private double[] m_dTemperature = new double[GRID_SIZE];
	private double[] m_dHumidity = new double[GRID_SIZE];
	private double[] m_dWindMagnitude = new double[GRID_SIZE];

	//single instance
	private static RSAS oRSAS = new RSAS();
	
	private RSAS()
	{
		run();
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		Scheduler.getInstance().schedule(this, 
										oConfig.getInt("schedulerOffset",60*25),
										oConfig.getInt("schedulerPeriod",3600));
	}

	public static RSAS getInstance()
	{
		return oRSAS;
	}


	/**
	 * For debugging
	 * @param args
	 * @throws java.lang.Exception
	 */
	public static void main2(String args[]) throws Exception
	{
		RSAS.getInstance().getReadings(581, Stations.toMicro(34.4), Stations.toMicro(-90.40));
	}


	/**
	 * The run function downloads the most recent GRIB2 file from the
	 * NOAA.gov ftp site.  The file is then parsed for the wind speed,
	 * temperature, specific humidity, and pressure records and the relevant
	 * data points are pulled out and stored in arrays.
	 */
	public void run()
	{
		//Set up the FtpConn object using config values
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		
		FtpConn oFtpConn = new FtpConn(
				oConfig.getString("ftpServer", "ftp://ftp.ncep.noaa.gov"),
				oConfig.getString("ftpUsername", "Anonymous"),
				oConfig.getString("ftpPassword", "Clarus@mixon-hill.com"));	
		String sBaseDirectory = oConfig.getString("dataDirectory", 
												"pub/data/nccf/com/rucs/prod/");	
		
	
		//Set up number formatter to zero pad numbers ( 1 => 01 )
		NumberFormat oNumberFormat = NumberFormat.getIntegerInstance();
		oNumberFormat.setMinimumIntegerDigits(2);
		//Set up calendar object to get current date and hour
		Calendar oCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));		
		if (!oFtpConn.connect())
			return;
		//Build the folder name for the current day's readings.
		String sFileDirectory = "rucs." + oCalendar.get(Calendar.YEAR) 
				+oNumberFormat.format(oCalendar.get(Calendar.MONTH) + 1)
				+oNumberFormat.format(oCalendar.get(Calendar.DAY_OF_MONTH))+"/";
		int nHour = oCalendar.get(Calendar.HOUR_OF_DAY) + 1;
		//Build the file name for the current hour's readings
		String sFile= "rucs.t" + oNumberFormat.format(nHour) + "z.g88anl.grib2";
		//Start looking for files corresponding to the current hour,
		//and loop through earlier hours until the most recent file is found.
		while (!(oFtpConn.open(sBaseDirectory + sFileDirectory + sFile))
				&& nHour-- > 0)
			sFile = "rucs.t" + oNumberFormat.format(nHour) + "z.g88anl.grib2";
		//Set up the temp file to store the grib information

		
		//Create an ArrayList to hold the bytes from the new Grib2 file
		ArrayList<Byte> yGribFileByteList = new ArrayList<Byte>();
		try
		{
			//Read bytes from the file into the ArrayList
			int nByte;
			while ((nByte = oFtpConn.read()) != -1)
				yGribFileByteList.add((byte)nByte);

			//Close the FTP connection
			oFtpConn.close();

			//Create an array to copy bytes into, and to create the
			//NetcdfFile object
			byte[] yGribFileByteArray = new byte[yGribFileByteList.size()];
			ListIterator<Byte> oByteIterator =yGribFileByteList.listIterator(0);
			nByte = -1;
			//copy all the bytes
			while(oByteIterator.hasNext())
				yGribFileByteArray[++nByte] = oByteIterator.next();
			//Create the NetcdfFile object from the byte array
			NetcdfFile oCdfFile =
					NetcdfFile.openInMemory("bleh", yGribFileByteArray);
//			NetcdfFile oCdfFile = NetcdfFile.open("c:/temp/OutputFile.grib2");
			Group oRootGroup = oCdfFile.getRootGroup();		
			//If the projection object hasn't been created, create it.
			if(m_oProjection == null)
			{
				m_oProjection = new GridDataset(
						new NetcdfDataset(oCdfFile)).getGrids().
						get(0).getProjection();
				
				//Get the values along the x-axis
				//(native coordinates in the grid)
				oXPoints = oRootGroup.findVariable("x").read();
				//Get the values along the y-axis
				//(native coordinates in the grid)
				oYPoints = oRootGroup.findVariable("y").read();
			}
			//Get the desired value arrays out of the grid data
			Array oTemperature = oRootGroup.findVariable("Temperature").read();
			Array oUWind =oRootGroup.findVariable("U-component_of_wind").read();
			Array oVWind =oRootGroup.findVariable("V-component_of_wind").read();
			Array oHumidity=oRootGroup.findVariable("Specific_humidity").read();

			//Loop through all the values and copy values from the Array objects
			//into primitive arrrays, converting units where necessary.
			for(int nI = 0; nI < GRID_SIZE; ++nI)
			{
				//convert temperature from Kelvin to Celsius
				m_dTemperature[nI] = oTemperature.getDouble(nI) - 273.15;
				//Convert WindU and WindV to total magnitude
				m_dWindMagnitude[nI] = Math.sqrt(
						Math.pow(oUWind.getDouble(nI), 2)
						+ Math.pow(oVWind.getDouble(nI), 2));
				//Convert specific humidity to relative humidity
				double dHumidityIndex = (m_dTemperature[nI] / 5 + 8);
				int nLowerHumidityIndex = (int)dHumidityIndex;
				dHumidityIndex %= 1;
				m_dHumidity[nI] = m_fMaxHumidity[nLowerHumidityIndex];
				m_dHumidity[nI] += dHumidityIndex * 
						(m_fMaxHumidity[nLowerHumidityIndex + 1]
						- m_dHumidity[nI]);
				m_dHumidity[nI] = (oHumidity.getDouble(nI) / m_dHumidity[nI])
						* 100;
			}
		}
		catch(Exception oException)
		{
			//Failed to download file, cannot get new data
			return;
		}
	}


	/**
	 * getReading() takes a latitude and longitude, and returns up to 25
	 * readings from the given lat and lon and its surrounding points.  Fewer
	 * than 25 points are returned if the given lat/lon are on an edge or in
	 * a corner of the grid.  null is returned if the lat/lon are out of the
	 * valid range.
	 * @param oObsType - the type of observation to return values for
	 * @param dLat - latitude the observation was taken at
	 * @param dLon - longitude the observation was taken at
	 * @return up to 25 readings at, and surrounding dLat and dLon
	 */
	public double[] getReadings(int nObsID, int nLat, int nLon)
	{
		try
		{
			//Get the native grid coordinates from the projection object
			ProjectionPointImpl oPoint =
					m_oProjection.latLonToProj(Stations.fromMicro(nLat),
											   Stations.fromMicro(nLon));
			double dNativeX = oPoint.x;
			double dNativeY = oPoint.y;
			//Make sure that this point is actually in the grid.
			if(!m_oProjection.getDefaultMapArea().contains(dNativeX, dNativeY))
				return null;
			//Look through the native coordinates that each point on the x and
			//y axis correspond with to find the two points on the axis that
			//bound the native coordinate value.  when the loop exits, the second
			//of the bounding values is the one that will be stored
			int nX;
			int nY;
			for(nX = 0; nX < POINT_COUNT_X; ++nX)
				if(oXPoints.getDouble(nX) > dNativeX)
					break;
			for(nY = 0; nY < POINT_COUNT_Y; ++nY)
				if(oYPoints.getDouble(nY) > dNativeY)
					break;
			//Since the second of the two bounding points is currently stored,
			//see if the first one is closer
			if(dNativeX - oXPoints.getDouble(nX - 1) <
					oXPoints.getDouble(nX) - dNativeX)
				--nX;
			if(dNativeY - oYPoints.getDouble(nY - 1) <
					oYPoints.getDouble(nY) - dNativeY)
				--nX;
			//Now that the x, y coordinates have been found, just return the values.
			switch (nObsID)
			{
				case 5733: return getValues(m_dTemperature,nX, nY);
				case 581:  return getValues(m_dHumidity, nX, nY);
				case 554:  return getValues(m_dPressure, nX, nY);
				case 56104:return getValues(m_dWindMagnitude, nX, nY);
				default:   return null;
			}
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}

		return null;
	}


	/**
	 * getValues will check the points surrounding nX, and nY to see how
	 * many of them are inside the bounds of the grid, and then return
	 * an the surrounding points in an array - up to 25 if the passed point
	 * is not against a side or in a corner
	 * @param fValues array to get the values from
	 * @param nX
	 * @param nY
	 * @return an array of values centered around the given x,y
	 */
	private double[] getValues(double[] dValues, int nX, int nY)
	{
		//Do an initial loop through to find out how many valid points
		//will be found.  This will range from 9 to 25 depending on where
		//in the grid the coordinates came from (if it is on a side or a corner)
		int nValidPointCount = 0;
		for (int i = nX - 2; i < nX + 3; i++)
			for (int j = nY - 2; j<nY + 3; j++)
				if (i >= 0 && i < POINT_COUNT_X && j >= 0 && j < POINT_COUNT_Y)
					++nValidPointCount;
		//create an array to hold the valid values and go back through to
		//get the values.
		double[] dReturn = new double[nValidPointCount];
		nValidPointCount = -1;
		for (int i = nX - 2; i < nX + 3; i++)
			for (int j = nY - 2; j < nY + 3; j++)
				if (i >= 0 && i < POINT_COUNT_X && j >= 0 && j < POINT_COUNT_Y)				 
						dReturn[++nValidPointCount] =
								dValues[toSingleDimensionIndex(i, j)];
		return dReturn;
	}


	private int toSingleDimensionIndex(int nX, int nY)
	{			
			return POINT_COUNT_X * nY + nX;
	}		
		
	
	private class LatLon
	{
		public int  m_nLat;
		public int m_nLon;
		public LatLon(int nLat, int nLon)
		{
			m_nLat = nLat;
			m_nLon = nLon;
		}


		public double distanceTo(LatLon oLatLon)
		{
			return Math.pow(this.m_nLon - oLatLon.m_nLon, 2)
					+ Math.pow(this.m_nLat - oLatLon.m_nLat, 2);
		}
		public LatLon()
		{
		}
	}
}
