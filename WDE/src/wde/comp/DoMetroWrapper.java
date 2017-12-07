package wde.comp;

import org.apache.log4j.Logger;
import java.util.Calendar;
import wde.cs.ext.NDFD;
import wde.cs.ext.RAP;
import wde.cs.ext.RTMA;
import wde.cs.ext.Radar;
import wde.util.MathUtil;

/**
 * This class contains methods to create input arrays for METRo and save the 
 * returned output arrays. It also contains the wrapper function for the C code 
 * to run METRo directly.  By doing this we bypass using METRo’s Python code, 
 * which improves the performance greatly.
 * @author aaron.cherney
 */
public class DoMetroWrapper implements Runnable
{
	public static boolean g_bLibraryLoaded = true;
	private static final Logger m_oLogger = Logger.getLogger(DoMetroWrapper.class);
	static
	{
		try
		{
			System.loadLibrary("DoMetroWrapper");
		}
		catch(Exception e)
		{
			g_bLibraryLoaded = false;
			m_oLogger.error("Failed to load shared library");
		}
	}
	public boolean m_bFail = false;
	private int m_bBridge; 
	private double m_dLat; 
	private double m_dLon;                
   private final long[] m_lOutRoadCond;
	private final double[] m_dOutRoadTemp;
   private final double[] m_dOutSubSurfTemp;
	private final double[] m_dOutSnowIceAcc;
	private final double[] m_dOutLiquidAcc;
	private final double[] m_dFTime;
	private final double[] m_dFTimeSeconds;
	private final double[] m_dDewPoint;
	private final double[] m_dCloudCover;
	private final double[] m_dAirTemp;
	private final double[] m_dPrecipAmt;
	private final double[] m_dWindSpeed;
	private final double[] m_dSfcPres;
	private final double[] m_dPrecipType;
	private final double[] m_dRoadCond;
	private final double[] m_dObsAirTemp;
	private final double[] m_dObsRoadTemp;
	private final double[] m_dObsSubSurfTemp;
	private final double[] m_dObsDewPoint;
	private final double[] m_dObsWindSpeed;
	private final double[] m_dObsTime;
	private final MetroResults m_oMetroResults = MetroResults.getInstance();
	private final MetroMgr m_oMetroMgr = MetroMgr.getInstance();
	private final int m_nNumOfOutputs = (m_oMetroMgr.m_nForecastHours - 1) * 120;
	private int m_nTmtType;
                    
	DoMetroWrapper()
	{
		m_lOutRoadCond = new long[m_nNumOfOutputs];
		m_dOutRoadTemp = new double[m_nNumOfOutputs];
		m_dOutSubSurfTemp = new double[m_nNumOfOutputs];
		m_dOutSnowIceAcc = new double[m_nNumOfOutputs];
		m_dOutLiquidAcc = new double[m_nNumOfOutputs];
		m_dFTime = new double[m_oMetroMgr.m_nForecastHours];
		m_dFTimeSeconds = new double[m_oMetroMgr.m_nForecastHours];
		m_dDewPoint = new double[m_oMetroMgr.m_nForecastHours];
		m_dAirTemp = new double[m_oMetroMgr.m_nForecastHours];
		m_dPrecipAmt = new double[m_oMetroMgr.m_nForecastHours];
		m_dWindSpeed = new double[m_oMetroMgr.m_nForecastHours];
		m_dSfcPres = new double[m_oMetroMgr.m_nForecastHours];
		m_dPrecipType = new double[m_oMetroMgr.m_nForecastHours];
		m_dRoadCond = new double[m_oMetroMgr.m_nObservationHours];
		m_dObsAirTemp = new double[m_oMetroMgr.m_nObservationHours]; 
		m_dObsRoadTemp = new double[m_oMetroMgr.m_nObservationHours];
		m_dObsSubSurfTemp = new double[m_oMetroMgr.m_nObservationHours];
		m_dObsDewPoint = new double[m_oMetroMgr.m_nObservationHours];
		m_dObsWindSpeed = new double[m_oMetroMgr.m_nObservationHours];
		m_dCloudCover = new double[m_oMetroMgr.m_nForecastHours];
		m_dObsTime = new double[m_oMetroMgr.m_nObservationHours];
	}
	
	/**
	 *	  This function uses JNI to call the C function doMetroWrapper which calls
	 *   Do_Metro to run the METRo heat-balance model
	 * 
	 * @param bBridge  is the road a bridge? 0 = road, 1 = bridge
	 * @param dLat     latitude in decimal degrees
	 * @param dLon     longitude in decimal degrees
	 * @param nObservationHrs  the number of observation hours 
	 * @param nForecastHrs     the number of forecast hours
	 * @param dObsRoadTemp     array containing the observed road temperatures (Celsius)
	 * @param dObsSubSurfTemp  array containing the observed sub surface temperatures (Celsius)
	 * @param dObsAirTemp      array containing the observed air temperatures (Celsius)
	 * @param dObsDewPoint     array containing the observed dew points (Celsius)
	 * @param dObsWindSpeed    array containing the observed wind speeds (km/h)
	 * @param dObsTime         array containing the Hour of Day of each observation
	 * @param dFTime				array containing the Hour of Day of each forecast
	 * @param dFTimeSeconds    array containing the Unix Time in seconds of each forecast
	 * @param dAirTemp         array containing the forecasted air temperatures (Celsius)
	 * @param dDewPoint        array containing the forecasted dew points (Celsius)
	 * @param dWindSpeed			array containing the forecasted wind speeds (km/h)
	 * @param dSfcPres         array containing the forecasted surface pressure (Pa)  (METRo documentation says the units is mb but when I printed out the data that was input into METRo from Environment Canada's python code it was in Pa)
	 * @param dPrecipAmt       array containing the forecasted precipitation amounts (mm)
	 * @param dPrecipType      array containing the forecasted precipitation types (0 = none, 1 = rain, 2 = snow)
	 * @param dRoadCond			array containing the observed road condition (we initialize the 1st based off of precipitation and air temp. after that we use the METRo results as the input, the values are listed below)
	 * @param dCloudCover      array containing the forecasted cloud covers (the value is "octal" being from 0-8)
	 * @param lOutRoadCond     array containing the data output from METRo for the road condition (1 = dry road, 2 = wet road, 3 = ice/snow on the road, 4 = mix water/snow on the road, 5 = dew, 6 = melting snow, 7 = frost, 8 = icing rain)
	 * @param dOutRoadTemp     array containing the data output from METRo for the road temperature (Celsius)
	 * @param dOutSubSurfTemp  array containing the data output from METRo for the sub surface temperature (Celsius)
	 */
	private native void doMetroWrapper(int bBridge, double dLat, double dLon, int nObservationHrs, int nForecastHrs,
         double[] dObsRoadTemp, double[] dObsSubSurfTemp, double[] dObsAirTemp, double[] dObsDewPoint, double[] dObsWindSpeed, 
			double[] dObsTime, double[] dFTime, double[] dFTimeSeconds, double[] dAirTemp, double[] dDewPoint, 
			double[] dWindSpeed, double[] dSfcPres, double[] dPrecipAmt, double[] dPrecipType, double[] dRoadCond, 
			double[] dCloudCover, long[] lOutRoadCond, double[] dOutRoadTemp, double[] dOutSubSurfTemp, double[] dOutSnowIceAcc, 
			double[] dOutLiquidAcc, int nTmtType);
	
	
	/**
	 *   This function runs METRo for a region defined by a MapCell. It is called
	 *   from the run() function of MapCell which is called by MetroMgr.
	 */
	@Override
	public void run()
	{
			doMetroWrapper(m_bBridge, m_dLat, m_dLon, m_oMetroMgr.m_nObservationHours, m_oMetroMgr.m_nForecastHours, m_dObsRoadTemp, m_dObsSubSurfTemp,
				m_dObsAirTemp, m_dObsDewPoint, m_dObsWindSpeed, m_dObsTime, m_dFTime, m_dFTimeSeconds, m_dAirTemp, m_dDewPoint, m_dWindSpeed, m_dSfcPres, m_dPrecipAmt, m_dPrecipType,
				m_dRoadCond, m_dCloudCover, m_lOutRoadCond, m_dOutRoadTemp, m_dOutSubSurfTemp, m_dOutSnowIceAcc, m_dOutLiquidAcc, m_nTmtType);
	}
	
	
	/**
	 * This function fills the input arrays for the C and Fortran code using 
	 * RTMA, NDFD, and RAP.
	 * 
	 * @param oCell the region for the roadcast
	 */
	public void fillArrays(MapCell oCell)
	{
		m_bFail = false;
		double dRoadTemperatureMin = -50;  // originally -40 in METRo
		double dRoadTemperatureHigh = 80;
		double dSubSurRoadTmpHigh = 80;
		double dSubSurRoadTmpMin = -40;
		double dAirTempHigh = 50;
		double dAirTempMin = -60;
		double dMaxWindSpeed = 90;
		double dLowerPressure = 60000; // changed Pressure values to Pa
		double dNormalPressure = 101325; // 1 atmosphere
		double dUpperPressure = 110000;
		long lMetroStartTime = m_oMetroMgr.m_lNow;
		long lObservation = lMetroStartTime - (3600000 * m_oMetroMgr.m_nObservationHours) + 3600000;  //adjust time to the first observation
		NDFD oNDFD = NDFD.getInstance();
		RAP oRAP = RAP.getInstance();
		RTMA oRTMA = RTMA.getInstance();
		int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
		int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonRight) / 2);
		
		//fill observation arrays
		for (int i = 0;i < m_oMetroMgr.m_nObservationHours; i++)
		{
			long lTimestamp = lObservation + (i * 3600000);
		
			m_dObsAirTemp[i] = oRTMA.getReading(5733, lTimestamp, nMicroLat, nMicroLon);
			m_dObsDewPoint[i] = oRTMA.getReading(575, lTimestamp, nMicroLat, nMicroLon);
			m_dObsWindSpeed[i] = oRTMA.getReading(56104, lTimestamp, nMicroLat, nMicroLon) * 3.6; //convert from m/s in RTMA to km/hr for METRo

			
			if (Double.isNaN(m_oMetroResults.getReading(51137, lTimestamp, nMicroLat, nMicroLon)))  //if there is no MetroResults initialize road condition based off of presence of precipitation and air temp
			{
				if (MetroMgr.getInstance().getPresenceOfPrecip(nMicroLat, nMicroLon, lTimestamp) > 0)  //check if there is precipitation
				{
					if (m_dObsAirTemp[i] > 2)  //temp greater than 2 C means rain
						m_oMetroResults.setValue(51137, lTimestamp, nMicroLat, nMicroLon, 2); //wet road
					else if (m_dObsAirTemp[i] < -2) //temp less than -2 C means snow/ice
						m_oMetroResults.setValue(51137, lTimestamp, nMicroLat, nMicroLon, 3);  //ice/snow on the road
					else //temp between -2 C and 2 C means mix
						m_oMetroResults.setValue(51137, lTimestamp, nMicroLat, nMicroLon, 4);  //mix water/snow on the road
				}
				else  //no precipitation so dry road
				{
					m_oMetroResults.setValue(51137, lTimestamp, nMicroLat, nMicroLon, 1);  //dry road
				}
			}
			
			m_dRoadCond[i] = m_oMetroResults.getReading(51137, lTimestamp, nMicroLat, nMicroLon);
			
			m_dObsRoadTemp[i] = m_oMetroResults.getReading(51138, lTimestamp, nMicroLat, nMicroLon);
			if (Double.isNaN(m_dObsRoadTemp[i]) || m_dObsRoadTemp[i] < dRoadTemperatureMin || m_dObsRoadTemp[i] > dRoadTemperatureHigh)  //if there is no MetroResults or ObsRoadTemp is out of range initialize surface temp using RTMA air temp
			{
				m_oMetroResults.setValue(51138, lTimestamp, nMicroLat, nMicroLon, oRTMA.getReading(5733, lTimestamp, nMicroLat, nMicroLon));					
			}
			
			m_dObsRoadTemp[i] = m_oMetroResults.getReading(51138, lTimestamp, nMicroLat, nMicroLon);
			
			m_dObsSubSurfTemp[i] = m_oMetroResults.getReading(51165, lTimestamp, nMicroLat, nMicroLon);
			if (Double.isNaN(m_dObsSubSurfTemp[i]) || m_dObsSubSurfTemp[i] < dSubSurRoadTmpMin || m_dObsSubSurfTemp[i] > dSubSurRoadTmpHigh)  //if there is no MetroResults or ObsSubSurfTemp is out of range initialize subsurface temp using RTMA air temp
			{
				m_oMetroResults.setValue(51165, lTimestamp, nMicroLat, nMicroLon, oRTMA.getReading(5733, lTimestamp, nMicroLat, nMicroLon));
			}
			
			m_dObsSubSurfTemp[i] = m_oMetroResults.getReading(51165, lTimestamp, nMicroLat, nMicroLon);
			
			Calendar oCal = Calendar.getInstance();
			oCal.setTimeInMillis(lTimestamp);
			m_dObsTime[i] = oCal.get(Calendar.HOUR_OF_DAY);
			
			//check that values fall within the correct range, if not exit the function and set that it failed so METRo isn't ran for the MapCell
			if (Double.isNaN(m_dObsRoadTemp[i]) || m_dObsRoadTemp[i] < dRoadTemperatureMin || m_dObsRoadTemp[i] > dRoadTemperatureHigh)
			{
				m_bFail = true;
				m_oLogger.debug("Observation Road Temp out of range: " + " " + m_dObsRoadTemp[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
      
			if (Double.isNaN(m_dObsSubSurfTemp[i]) || m_dObsSubSurfTemp[i] < dSubSurRoadTmpMin || m_dObsSubSurfTemp[i] > dSubSurRoadTmpHigh)
			{
				m_bFail = true;
				m_oLogger.debug("Observation Sub Surface Temp out of range: " + " " + m_dObsSubSurfTemp[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
      
			if (Double.isNaN(m_dObsAirTemp[i]) || m_dObsAirTemp[i] < dAirTempMin || m_dObsAirTemp[i] > dAirTempHigh)
			{
				m_bFail = true;
				m_oLogger.debug("Observation Air Temperature out of range: " + " " + m_dObsAirTemp[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
      
			if (Double.isNaN(m_dObsDewPoint[i]) || m_dObsDewPoint[i] < dAirTempMin || m_dObsDewPoint[i] > dAirTempHigh)
			{
				m_bFail = true;
				m_oLogger.debug("Observation Dew Point out of range: " + " " + m_dObsDewPoint[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
      
			if (Double.isNaN(m_dObsWindSpeed[i]) || m_dObsWindSpeed[i] < 0 || m_dObsWindSpeed[i] > dMaxWindSpeed)
			{
				m_bFail = true;
				m_oLogger.debug("Observation Wind Speed out of range: " + " " + m_dObsWindSpeed[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
		}
		
		//fill forecast arrays
		for (int i = 0; i < m_oMetroMgr.m_nForecastHours; i++)
		{
			long lTimestamp = lMetroStartTime + (3600000 * i);
			//for the first hour of forecast use RTMA, after that use NDFD for air temp, dew point, wind speed, and cloud cover
			if (i == 0)
			{
				m_dAirTemp[i] = oRTMA.getReading(5733, lTimestamp, nMicroLat, nMicroLon);
				m_dDewPoint[i] = oRTMA.getReading(575, lTimestamp, nMicroLat, nMicroLon);
				m_dWindSpeed[i] = oRTMA.getReading(56104, lTimestamp, nMicroLat, nMicroLon)* 3.6; //convert from m/s in RTMA to km/hr for METRo
				m_dCloudCover[i] = oRTMA.getReading(593, lTimestamp, nMicroLat, nMicroLon);
			}
			else
			{
				m_dAirTemp[i] = oNDFD.getReading(5733, lTimestamp, nMicroLat, nMicroLon);
				m_dDewPoint[i] = oNDFD.getReading(575, lTimestamp, nMicroLat, nMicroLon);
				m_dWindSpeed[i] = oNDFD.getReading(56104, lTimestamp, nMicroLat, nMicroLon) * 3.6; //convert from m/s in NDFD to km/hr for METRo
				m_dCloudCover[i] = oNDFD.getReading(593, lTimestamp, nMicroLat, nMicroLon);
			}
			m_dPrecipAmt[i] = oRAP.getReading(587, lTimestamp, nMicroLat, nMicroLon) * 3600;  //convert from kg/(m^2 * sec) to mm in an hour
			m_dSfcPres[i] = oRAP.getReading(554, lTimestamp, nMicroLat, nMicroLon) * 100;  //METRo needs input pressure in Pa
			int nPrecipType = (int)oRAP.getReading(207, lTimestamp, nMicroLat, nMicroLon);
			if (nPrecipType == 3)
				m_dPrecipType[i] = 0; //none
			else if (nPrecipType == 4 || nPrecipType == 6)
				m_dPrecipType[i] = 1;  //rain
			else if(nPrecipType == 5)
				m_dPrecipType[i] = 2;  //snow
			
			Calendar oCal = Calendar.getInstance();
			oCal.setTimeInMillis(lTimestamp);
			m_dFTime[i] = oCal.get(Calendar.HOUR_OF_DAY);
			m_dFTimeSeconds[i] = (int)(lTimestamp / 1000);
			//check that values fall within the correct range, if not exit the function and set that it failed so METRo isn't ran for the MapCell except for Surface Pressure
			if (Double.isNaN(m_dAirTemp[i]) || m_dAirTemp[i] < dAirTempMin || m_dAirTemp[i] > dAirTempHigh)
			{
				m_bFail = true;
				m_oLogger.debug("Forecast Air Temperature out of range: " + " " + m_dAirTemp[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
			if (Double.isNaN(m_dDewPoint[i]) || m_dDewPoint[i] < dAirTempMin || m_dDewPoint[i] > dAirTempHigh)
			{
				m_bFail = true;
				m_oLogger.debug("Forecast Dew Point out of range: " + " " + m_dDewPoint[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
			if (Double.isNaN(m_dWindSpeed[i]) || m_dWindSpeed[i] < 0 || m_dWindSpeed[i] > dMaxWindSpeed)
			{
				m_bFail = true;
				m_oLogger.debug("Forecast Wind Speed out of range: " + " " + m_dWindSpeed[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
			if (m_dSfcPres[i] < dLowerPressure || m_dSfcPres[i] > dUpperPressure)
				m_dSfcPres[i] = dNormalPressure;  //METRo code set the Pressure to normal pressure if it was not in the correct range
			if (Double.isNaN(m_dCloudCover[i]) || m_dCloudCover[i] < 0 || m_dCloudCover[i] > 8)
			{
				m_bFail = true;
				m_oLogger.debug("Forecast Cloud Cover out of range: " + " " + m_dCloudCover[i] + " at " + nMicroLat + " " + nMicroLon + " for hour " + i);
				return;
			}
		}
		
		//fill station variables
		m_bBridge = 0;
		m_dLat = (oCell.m_dLatBot + oCell.m_dLatTop) / 2;
		m_dLon = (oCell.m_dLonLeft + oCell.m_dLonRight) / 2;
		m_nTmtType = 0; 
	}
	
	
	/**
	 * This function saves the output arrays from the C and Fortran code into the
	 * lists kept by MetroResults
	 * 
	 * @param oCell the region for the roadcast
	 */
	public void saveRoadcast(MapCell oCell)
	{
		int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
		int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonLeft) / 2);
		for (int i = 0; i < m_oMetroMgr.m_nForecastHours - 1; i++) 
		{
			//output arrays from metro contains roadcast for every 30 seconds starting 20 minutes after the last observation. so start at 80 for the first hour and add 120 to get to each hour after that
			m_oMetroResults.setValue(51137, m_oMetroMgr.m_lNow + ((i + 1) * 3600000), nMicroLat, nMicroLon, m_lOutRoadCond[80 + (i * 120)]);
			m_oMetroResults.setValue(51138, m_oMetroMgr.m_lNow + ((i + 1) * 3600000), nMicroLat, nMicroLon, m_dOutRoadTemp[80 + (i * 120)]);
			m_oMetroResults.setValue(51165, m_oMetroMgr.m_lNow + ((i + 1) * 3600000), nMicroLat, nMicroLon, m_dOutSubSurfTemp[80 + (i * 120)]);
			m_oMetroResults.setValue(584, m_oMetroMgr.m_lNow + ((i + 1) * 3600000), nMicroLat, nMicroLon, m_dOutSnowIceAcc[80 + (i * 120)]);
			m_oMetroResults.setValue(511310, m_oMetroMgr.m_lNow + ((i + 1) * 3600000), nMicroLat , nMicroLon, m_dOutLiquidAcc[80 + (i * 120)]);
		}
	}
		
	
	public static void main(String[] args)
	{
		DoMetroWrapper oDoMetroWrapper = new DoMetroWrapper();
		NDFD.getInstance();
		RAP.getInstance();
		Radar.getInstance();
		RTMA.getInstance();
		oDoMetroWrapper.run();
	}
}

