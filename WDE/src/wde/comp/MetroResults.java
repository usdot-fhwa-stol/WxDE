/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.comp;

import java.util.ArrayList;
import java.util.Arrays;
import ucar.unidata.geoloc.projection.LambertConformal;
import wde.util.MathUtil;

/**
 * Singleton class that has functions to store and retrieve data for the METRo
 * roadcast output files.
 * 
 * @author aaron.cherney
 */
public class MetroResults
{
	private static final MetroResults g_oMetroResults = new MetroResults();
	public ArrayList<RoadcastData> m_oRoadcastDataList = new ArrayList();   //contains all of the RoadcastData ran from METRo
	private final int m_nColumns = 2145;               //lon
	private final int m_nRows = 1377;                  //lat
	private final double m_dColLeftLimit = -2763.2046;  //lon
	private final double m_dColRightLimit = 2681.9185;
	private final double m_dRowLeftLimit = -263.78943;   //lat
	private final double m_dRowRightLimit = 3230.8418;
	
	private MetroResults()
	{
	}
	
	
	/**
	 * Returns a reference to singleton MetroResults
	 * 
	 * @return Singleton MetroResults reference
	 */
	public static MetroResults getInstance()
	{
		return g_oMetroResults;
	}
	
	
	/**
	 * A utility method that matches a given value in kilometers to the nearest
	 * row or column or an array with the given max, and min and number of cells
	 * in the row or column.
	 * 
	 * @param dKm          kilometers of a Lambert Conformal Projection
	 * @param dLeftLimit   the minimum value the kilometers can be
	 * @param dRightLimit  the maximum value the kilometers can be
	 * @param nNumOfCells  the number of cells in the row or column
	 * @return             the nearest index of the row or column to the given km
	 */
	public static int getIndex(double dKm, double dLeftLimit, double dRightLimit, int nNumOfCells)
	{
		double dBasis;
		double dDist;
		double dLeft = dLeftLimit; // test for value in range
		double dRight = dRightLimit;
		if (dRight < dLeft) // handle reversed endpoints
		{
			if (dKm < dRight || dKm > dLeft)
				return -1; // outside of range
			dBasis = dLeft - dRight;
			dDist = dLeft - dKm; // tricksy
		}
		else
		{
			if (dKm < dLeft || dKm > dRight)
				return -1; // outside of range
			dBasis = dRight - dLeft;
			dDist = dKm - dLeft;
		}

		return (int)(dDist / dBasis * (double) nNumOfCells);
	}
		
	
	/**
	 * A utility method that takes a latitude and longitude and converts them 
	 * into a Lambert Conformal Projection
	 * 
	 * @param dLat  latitude in decimal degrees
	 * @param dLon  longitude in decimal degrees
	 * @return      a 2D array where [0][0] is the x coordinate and [1][0] is the
	 *              y coordinate of the projection
	 */
	public static double[][] latLonToLambert(double dLat, double dLon)
	{
		LambertConformal oLam = new LambertConformal(25, 265, 25, 25);
		double[][] dLatLon = new double [2][1];
		double[][] dProj = new double [2][1];
		dLatLon[0][0] = dLat;
		dLatLon[1][0] = dLon;
		oLam.latLonToProj(dLatLon, dProj, 0, 1);
		return dProj;
	}
	
	
	/**
	 * A utility method that takes a latitude and longitude and converts them 
	 * into a Lambert Conformal Projection
	 * 
	 * @param dKmX  x coordinate of the projection
	 * @param dKmY  y coordinate of the projection
	 * @return      a 2D array where [0][0] is the latitude and [1][0] is the
	 *              longitude
	 */
	public static double[][] lambertToLatLon(double dKmX, double dKmY)
	{
		LambertConformal oLam = new LambertConformal(25, 265, 25, 25);
		double[][] dLatLon = new double [2][1];
		double[][] dProj = new double [2][1];
		dProj[0][0] = dKmX;
		dProj[1][0] = dKmY;
		oLam.projToLatLon(dProj, dLatLon);
		dLatLon[1][0] -= 360;
		return dLatLon;
	}

	
	/**
	 * Sets the value of the roadcast for the given timestamp, observation type, 
	 * latitude, longitude.
	 * 
	 * @param nObsTypeID  observation type: rc = 51137, st = 51138, sst = 51165
	 * @param lTimestamp  time of the roadcast   
	 * @param dLat        latitude of the roadcast in decimal degrees
	 * @param dLon        longitude of the roadcast in decimal degrees
	 * @param dValue      value of the observation to be set in the array
	 */
	public void setValue(int nObsTypeID, long lTimestamp, double dLat, double dLon, double dValue)
	{
		//convert the lat and lon into the index for the array
		double[][] dProj = latLonToLambert(dLat, dLon);
		int nColIndex = getIndex(dProj[0][0], m_dColLeftLimit, m_dColRightLimit, m_nColumns);
		int nRowIndex = getIndex(dProj[1][0], m_dRowLeftLimit, m_dRowRightLimit, m_nRows);
		
		//find the correct RoadcastData in the list by obsType and Timestamp
		for (RoadcastData oData : m_oRoadcastDataList)
		{
 			if (oData.m_nObsTypeID == nObsTypeID && oData.m_lTimestamp <= lTimestamp && oData.m_lTimestampEnd > lTimestamp)
			{
				oData.m_dValueArray[nRowIndex][nColIndex] = dValue;
				break;
			}
		}
	}
	
	
	/**
	 * Sets the value of the roadcast for the given timestamp, observation type, 
	 * latitude, longitude.
	 * 
	 * @param nObsTypeID  observation type: rc = 51137, st = 51138, sst = 51165
	 * @param lTimestamp  time of the roadcast   
	 * @param nLat			 latitude of the roadcast in micro degrees
	 * @param nLon        longitude of the roadcast in micro degrees        
	 * @param dValue      value of the observation to be set in the array
	 */
	public void setValue(int nObsTypeID, long lTimestamp, int nLat, int nLon, double dValue)
	{
		setValue(nObsTypeID, lTimestamp, MathUtil.fromMicro(nLat), MathUtil.fromMicro(nLon), dValue);
	}
	
	
	/**
	 * Get the value of a roadcast output based on the timestamp, observation 
	 * type, latitude and longitude.
	 * 
	 * @param lTimestamp  time of the roadcast
	 * @param nObsTypeID  observation type: rc = 51137, st = 51138, sst = 51165
	 * @param dLat        latitude of the roadcast in decimal degrees
	 * @param dLon        longitude of the roadcast in decimal degrees
	 * @return 
	 */
	public double getReading(int nObsTypeID, long lTimestamp, double dLat, double dLon)
	{
		double[][] dProj = latLonToLambert(dLat, dLon); //need projection to get the index for the array
		//find the correct RoadcastData by obs type and timestamp
		for (RoadcastData oData : m_oRoadcastDataList)
		{
			if (oData.m_nObsTypeID == nObsTypeID && oData.m_lTimestamp <= lTimestamp && oData.m_lTimestampEnd > lTimestamp)
				return oData.m_dValueArray[getIndex(dProj[1][0], m_dRowLeftLimit, m_dRowRightLimit, m_nRows)][getIndex(dProj[0][0], m_dColLeftLimit, m_dColRightLimit, m_nColumns)];
		}
		return Double.NaN; //the roadcast for the given parameters doesn't exist
	}
	
	/**
	* Get the value of a roadcast output based on the timestamp, observation 
	* type, latitude and longitude.
	* 
	* @param lTimestamp  time of the roadcast
	* @param nObsTypeID  observation type: rc = 51137, st = 51138, sst = 51165
	* @param nLat        latitude of roadcast in micro degrees
	* @param nLon        longitude of roadcast in micro degrees
	* @return 
	*/
	public double getReading(int nObsTypeID, long lTimestamp, int nLat, int nLon)
	{
		return getReading(nObsTypeID, lTimestamp, MathUtil.fromMicro(nLat), MathUtil.fromMicro(nLon));
	}
	
	
	public final void initArrayList(long lNow, int nObservationHours, int nForecastHours)
	{
		//the first time create all of the needed arrays for RoadcastData
		if (m_oRoadcastDataList.isEmpty())
		{
			for (int i = -nObservationHours + 1; i < nForecastHours; i++)    //encompass all times that are used in METRo
			{
				m_oRoadcastDataList.add(new RoadcastData(51137, lNow + 3600000 * i));
				m_oRoadcastDataList.add(new RoadcastData(51138, lNow + 3600000 * i));
				m_oRoadcastDataList.add(new RoadcastData(51165, lNow + 3600000 * i));
			}
		}
		//if there is already RoadcastData, update the list
		else
		{
			//remove the oldest set of RoadcastData
			m_oRoadcastDataList.remove(0);
			m_oRoadcastDataList.remove(0);
			m_oRoadcastDataList.remove(0);
			//add 3 new arrays at the last forecast time
			long lNextHour = m_oRoadcastDataList.get(m_oRoadcastDataList.size() - 1).m_lTimestampEnd;
			m_oRoadcastDataList.add(new RoadcastData(51137, lNextHour));
			m_oRoadcastDataList.add(new RoadcastData(51138, lNextHour));
			m_oRoadcastDataList.add(new RoadcastData(51165, lNextHour));
		}
	}
	
	
	/**
	 * Inner class that encapsulates METRo roadcast data. Contains the timestamp
	 * range, observation type, and array of values for all lat, lon coordinates
	 */
	public class RoadcastData
	{
		public long m_lTimestamp;
		public long m_lTimestampEnd;
		public int m_nObsTypeID;
		public double[][] m_dValueArray;


		RoadcastData()
		{
			
		}

		
		/**
		 * Custom constructor that creates an object by the timestamp and 
		 * observation type.
		 * 
		 * @param lTimestamp     time of the roadcast 
		 * @param nObsTypeID       observation type, can be 51137 (road condition), 
		 *                       51138(road surface temperature), or 
		 *                       51165(road sub surface temperature)
		 */
		RoadcastData(int nObsTypeID, long lTimestamp)
		{
			m_lTimestamp = lTimestamp;
			m_lTimestampEnd = lTimestamp + 3600000;  // roadcast for an hour
			m_nObsTypeID = nObsTypeID;
			m_dValueArray = new double[m_nRows][m_nColumns];
			for (double[] dRow : m_dValueArray)
				Arrays.fill(dRow, Double.NaN);
		}
	}
	
	
	
	public static void main(String[] args)
	{

	}
}


