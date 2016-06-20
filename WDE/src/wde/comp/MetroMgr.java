/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.comp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


import wde.cs.ext.NDFD;
import wde.cs.ext.RTMA;
import wde.cs.ext.Radar;
import wde.cs.ext.RAP;
import wde.data.osm.Road;
import wde.data.osm.Roads;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.util.Scheduler;

/**
 * A singleton class that manages the use of METRo including writing input files, 
 * reading output files, and storing output files using the RoadcastDataFactory.
 * 
 * @author aaron.cherney
 */
public class MetroMgr extends HttpServlet implements Runnable, Comparator<RoadAlert>
{
	private static final MetroMgr g_oMetroMgr = new MetroMgr();
	private RoadcastDataFactory m_oRoadcastDataFactory = RoadcastDataFactory.getInstance();
	private String m_sBaseDir;
	private ArrayList<MapCell> m_oRoadMapCells;  //stores all of the MapCells that contain roads
	private ArrayList<MapCell> m_oBounds;        //stores the bounds of all of the regions the program is ran on
	private ArrayList<RoadAlert> m_oAlerts = new ArrayList();
	private final SimpleDateFormat m_oTimestamp = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm'Z'");
	private final int m_nForecastHours;
	private final int m_nObservationHours;
	private final int m_nThreads;
	private final int m_nOffset;
	private final int m_nPeriod;
	private double m_dLatTop;
	private double m_dLonLeft;
	private double m_dLatBot;
	private double m_dLonRight;
	private final double m_dStepX = 2.5385189277;
	private final double m_dStepY = 2.5378585548;
	private long m_lNow;
	private AtomicInteger m_nRunning = new AtomicInteger();
	private ExecutorService m_oThreadPool;
	


	private MetroMgr()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nThreads = oConfig.getInt("threads", 1);
		m_oThreadPool = Executors.newFixedThreadPool(m_nThreads);
		m_nForecastHours = oConfig.getInt("fhours", 3);
		m_nObservationHours = oConfig.getInt("ohours", 12);
		m_sBaseDir = oConfig.getString("dir", "/run/shm/");
		m_nOffset = oConfig.getInt("offset", 0);
		m_nPeriod = oConfig.getInt("period", 3600);
		String[] sRegionArray = oConfig.getStringArray("region");
		m_oBounds = new ArrayList(sRegionArray.length);
		//initialize all of the regions bounding boxes
		for (String sRegion : sRegionArray)
		{
			String sRegionBounds = oConfig.getString(sRegion, "");
			String[] sBounds = sRegionBounds.split(",");
			m_dLatTop = Double.parseDouble(sBounds[0]);
			m_dLonLeft = Double.parseDouble(sBounds[1]);
			m_dLatBot = Double.parseDouble(sBounds[2]);
			m_dLonRight = Double.parseDouble(sBounds[3]);
			m_oBounds.add(new MapCell(m_dLatTop, m_dLonLeft, m_dLatBot, m_dLonRight, sRegion));
		}		
		getMapCells(); //initialize list of MapCells that contain roads
		Scheduler.getInstance().schedule(this, m_nOffset, m_nPeriod, true);
	}

	
	/**
	 * Returns a reference to singleton MetroMgr
	 * 
	 * @return Singleton MetroMgr reference
	 */	
	public static MetroMgr getInstance()
	{
		return g_oMetroMgr;
	}
	
	
	/**
	 * A utility method that transforms and saves a Document object as an XML
	 * file to the given destination file.
	 * 
	 * @param oDoc       Document object that is to be transformed
	 * @param sDestFile  Destination file where XML will be saved
	 */
	public void transformToXML(Document oDoc, String sDestFile)
	{
		try
		{
		oDoc.setXmlStandalone(true);  //removes the standalone attribute in the XML declaration, for some reason METRo wouldn't run with the attribute present
		TransformerFactory oTransformerFactory = TransformerFactory.newInstance();
		Transformer oTransformer = oTransformerFactory.newTransformer();
		DOMSource oSource = new DOMSource(oDoc);
		File oFile = new File(sDestFile);
		if (!oFile.exists())
			oFile.createNewFile();
		StreamResult oResult = new StreamResult(oFile);
		oTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
		oTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
		oTransformer.transform(oSource, oResult);
		}
		catch (Exception e)
		{
		}
	}
	
	
	/**
	 * A method that creates and saves the Observation XML file that is used as 
	 * an input for METRo.
	 * 
	 * @param oCell       The region the observations are made in
	 */
	public void createObsXML(MapCell oCell)
	{	
		//set the lat and lon in micro degrees of the middle of the cell
		int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
		int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonRight) / 2 );
		
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		RTMA oRTMA = RTMA.getInstance();
		
		//set the time so that the last observation is for the current hour
		oTime.add(Calendar.HOUR_OF_DAY, -m_nObservationHours + 1);
		try
		{
			//create the structure of the XML file
			DocumentBuilderFactory oDocFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder oDocBuilder = oDocFactory.newDocumentBuilder();

			Document oDoc = oDocBuilder.newDocument();
			
			Element oObservation = oDoc.createElement("observation");
			oDoc.appendChild(oObservation);

			Element oHeader = oDoc.createElement("header");
			oObservation.appendChild(oHeader);

			Element oVersion = oDoc.createElement("version");
			oVersion.appendChild(oDoc.createTextNode("1.0"));
			oHeader.appendChild(oVersion);

			Element oRoadStation = oDoc.createElement("road-station");
			oRoadStation.appendChild(oDoc.createTextNode(nMicroLat + "_" + nMicroLon));
			oHeader.appendChild(oRoadStation);

			Element oMeasureList = oDoc.createElement("measure-list");
			oObservation.appendChild(oMeasureList);

			//create the measure clause in the XML file for each observation hour
			for (int i = 0; i < m_nObservationHours; i++)
			{
				Element oMeasure = oDoc.createElement("measure");
				oMeasureList.appendChild(oMeasure);

				Element oObservationTime = oDoc.createElement("observation-time");
				oObservationTime.appendChild(oDoc.createTextNode(m_oTimestamp.format(oTime.getTime())));
				oMeasure.appendChild(oObservationTime);

				double dAirTemp = oRTMA.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				Element oAirTemp = oDoc.createElement("at");
				oAirTemp.appendChild(oDoc.createTextNode(Double.toString(dAirTemp)));
				oMeasure.appendChild(oAirTemp);

				Element oDewPoint = oDoc.createElement("td");
				oDewPoint.appendChild(oDoc.createTextNode(Double.toString(oRTMA.getReading(575, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oDewPoint);

				int nPresenceOfPrecip = getPresenceOfPrecip(nMicroLat, nMicroLon, oTime.getTimeInMillis());
				Element oPresenceOfPrecip = oDoc.createElement("pi");
				oPresenceOfPrecip.appendChild(oDoc.createTextNode(Integer.toString(nPresenceOfPrecip)));
				oMeasure.appendChild(oPresenceOfPrecip);

				Element oWindSpeed = oDoc.createElement("ws");
				oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(oRTMA.getReading(56104, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oWindSpeed);

				Element oRoadCondition = oDoc.createElement("rc");
				Element oRoadSurfaceTemp = oDoc.createElement("st");
				Element oRoadSubSurfaceTemp = oDoc.createElement("sst");

				
				//if there is no RoadcastData initialize road condition based off of presence of precipitation and air temp
				if (Double.isNaN(m_oRoadcastDataFactory.getReading(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon)))
				{
					if (nPresenceOfPrecip > 0)  //check if there is precipitation
					{
						if (dAirTemp > 2)  //temp greater than 2 C means rain
							m_oRoadcastDataFactory.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 2); //wet road
						else if (dAirTemp < -2) //temp less than -2 C means snow/ice
							m_oRoadcastDataFactory.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 3);  //ice/snow on the road
						else //temp between -2 C and 2 C means mix
							m_oRoadcastDataFactory.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 4);  //mix water/snow on the road
					}
					else  //no precipitation so dry road
						m_oRoadcastDataFactory.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 1);  //dry road
				}
				
				oRoadCondition.appendChild(oDoc.createTextNode(Double.toString(m_oRoadcastDataFactory.getReading(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oRoadCondition);
				

				//if there is no roadcast data initialize surface temp as the air temp from RTMA
				if (Double.isNaN(m_oRoadcastDataFactory.getReading(51138, oTime.getTimeInMillis(), nMicroLat, nMicroLon)))
					m_oRoadcastDataFactory.setValue(51138, oTime.getTimeInMillis(), nMicroLat, nMicroLon, oRTMA.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon));
				
				oRoadSurfaceTemp.appendChild(oDoc.createTextNode(Double.toString(m_oRoadcastDataFactory.getReading(51138, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oRoadSurfaceTemp);
				
				//if there is no roadcast data initialize sub surface temp as the air temp from RTMA
				if (Double.isNaN(m_oRoadcastDataFactory.getReading(51165, oTime.getTimeInMillis(), nMicroLat, nMicroLon)))
					m_oRoadcastDataFactory.setValue(51165, oTime.getTimeInMillis(), nMicroLat, nMicroLon, oRTMA.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon));
				
				oRoadSubSurfaceTemp.appendChild(oDoc.createTextNode(Double.toString(m_oRoadcastDataFactory.getReading(51165, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oRoadSubSurfaceTemp);
				
				oTime.add(Calendar.HOUR_OF_DAY, 1);
			}
			 
			transformToXML(oDoc, m_sBaseDir + "observation" + Thread.currentThread().getId() + ".xml"); //create and save XML file
		}
		catch(Exception e)
		{
		}
	}
	
	
	/**
	 * A method that creates and saves the Station XML file that is used as an 
	 * input for METRo.
	 * 
	 * @param oCell  the region for the file
	 */
	public void createStationXML(MapCell oCell)
	{
		try
		{
			//set the lat and lon in micro degrees of the middle of the cell
			int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
			int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonLeft) / 2 );
			
			//create the structure for the XML file
			DocumentBuilderFactory oDocFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder oDocBuilder = oDocFactory.newDocumentBuilder();

			Document oDoc = oDocBuilder.newDocument();
			
			Element oStation = oDoc.createElement("station");
			oDoc.appendChild(oStation);

			Element oHeader = oDoc.createElement("header");
			oStation.appendChild(oHeader);

			Element oFileType = oDoc.createElement("filetype");
			oFileType.appendChild(oDoc.createTextNode("rwis-configuration"));
			oHeader.appendChild(oFileType);
			
			Element oVersion = oDoc.createElement("version");
			oVersion.appendChild(oDoc.createTextNode("1.0"));
			oHeader.appendChild(oVersion);

			Element oRoadStation = oDoc.createElement("road-station");
			oRoadStation.appendChild(oDoc.createTextNode(nMicroLat + "_" + nMicroLon));
			oHeader.appendChild(oRoadStation);
			
			Element oTimeZone = oDoc.createElement("time-zone");
			oTimeZone.appendChild(oDoc.createTextNode("UTC"));
			oHeader.appendChild(oTimeZone);
			
			Element oProductionDate = oDoc.createElement("production-date");
			oProductionDate.appendChild(oDoc.createTextNode(m_oTimestamp.format(m_lNow)));
			oHeader.appendChild(oProductionDate);
			
			Element oCooridnate = oDoc.createElement("coordinate");
			oHeader.appendChild(oCooridnate);
			
			Element oLatitude = oDoc.createElement("latitude");
			oLatitude.appendChild(oDoc.createTextNode(Double.toString(MathUtil.fromMicro(nMicroLat)))); //use decimal degrees for Lat
			oCooridnate.appendChild(oLatitude);
			
			Element oLongitude = oDoc.createElement("longitude");
			oLongitude.appendChild(oDoc.createTextNode(Double.toString(MathUtil.fromMicro(nMicroLon)))); //use decimal degrees for Lon
			oCooridnate.appendChild(oLongitude);
			
			Element oStationType = oDoc.createElement("station-type");
			oStationType.appendChild(oDoc.createTextNode("road"));
			oHeader.appendChild(oStationType);
			
			Element oRoadLayerList = oDoc.createElement("roadlayer-list");
			oStation.appendChild(oRoadLayerList);

			Element oRoadLayer = oDoc.createElement("roadlayer");
			oRoadLayerList.appendChild(oRoadLayer);

			Element oPostition = oDoc.createElement("position");
			oPostition.appendChild(oDoc.createTextNode("1"));
			oRoadLayer.appendChild(oPostition);

			Element oType = oDoc.createElement("type");
			oType.appendChild(oDoc.createTextNode("asphalt"));
			oRoadLayer.appendChild(oType);

			Element oThickness = oDoc.createElement("thickness");
			oThickness.appendChild(oDoc.createTextNode("0.5"));  //thickness in meters
			oRoadLayer.appendChild(oThickness);

		  
			transformToXML(oDoc, m_sBaseDir + "station" + Thread.currentThread().getId() + ".xml");  //createa and save XML file
		}
		catch(Exception e)
		{
		}
	}
	
	
	/**
	 * A method that creates and saves the Forecast XML file that is used as 
	 * an input for METRo.
	 * 
	 * @param oCell          The region for the forecast
	 */
	public void createForecastXML(MapCell oCell)
	{
		try
		{
			//set the lat and lon in micro degrees of the middle of the cell
			int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
			int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonRight) / 2 );
			Calendar oTime = new GregorianCalendar();
			oTime.setTimeInMillis(m_lNow);

			NDFD oNDFD = NDFD.getInstance();
			RTMA oRTMA = RTMA.getInstance();
			RAP oRAP = RAP.getInstance();
			
			//create structure of the XML file
			DocumentBuilderFactory oDocFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder oDocBuilder = oDocFactory.newDocumentBuilder();

			Document oDoc = oDocBuilder.newDocument();
			
			Element oForecast = oDoc.createElement("forecast");
			oDoc.appendChild(oForecast);

			Element oHeader = oDoc.createElement("header");
			oForecast.appendChild(oHeader);

			Element oVersion = oDoc.createElement("version");
			oVersion.appendChild(oDoc.createTextNode("1.1"));  //wouldn't run as v1.0
			oHeader.appendChild(oVersion);

			Element oProductionDate = oDoc.createElement("production-date");
			oProductionDate.appendChild(oDoc.createTextNode(m_oTimestamp.format(m_lNow)));
			oHeader.appendChild(oProductionDate);
			
			Element oStationID = oDoc.createElement("station-id");
			oStationID.appendChild(oDoc.createTextNode(nMicroLat + "_" + nMicroLon));
			oHeader.appendChild(oStationID);

			Element oPredictionList = oDoc.createElement("prediction-list");
			oForecast.appendChild(oPredictionList);
			
			//create the forecast clause in the XML for each hour of forecasts
			for (int i = 0;i < m_nForecastHours; i++)
			{
				Element oPrediction = oDoc.createElement("prediction");
				oPredictionList.appendChild(oPrediction);

				Element oForecastTime = oDoc.createElement("forecast-time");
				oForecastTime.appendChild(oDoc.createTextNode(m_oTimestamp.format(oTime.getTime())));
				oPrediction.appendChild(oForecastTime);

				double dAirTemp;
				Element oDewPoint = oDoc.createElement("td");
				Element oWindSpeed = oDoc.createElement("ws");
				Element oCloudCoverage = oDoc.createElement("cc");
				//for the first hour use RTMA for the forecast, for all others use NDFD
				if (i == 0)
				{
					dAirTemp = oRTMA.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
					oDewPoint.appendChild(oDoc.createTextNode(Double.toString(oRTMA.getReading(575, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
					oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(oRTMA.getReading(56104, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
					oCloudCoverage.appendChild(oDoc.createTextNode(Integer.toString((int) oRTMA.getReading(593, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));  //cloud coverage has to be an int
				}
				else
				{
					dAirTemp = oNDFD.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
					oDewPoint.appendChild(oDoc.createTextNode(Double.toString(oNDFD.getReading(575, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
					oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(oNDFD.getReading(56104, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
					oCloudCoverage.appendChild(oDoc.createTextNode(Integer.toString((int) oNDFD.getReading(593, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));   //cloud coverage has to be an int for METRo
				}
				Element oAirTemp = oDoc.createElement("at");
				oAirTemp.appendChild(oDoc.createTextNode(Double.toString(dAirTemp)));
				oPrediction.appendChild(oAirTemp);					
				oPrediction.appendChild(oDewPoint);

				double dPrecip = oRAP.getReading(587, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				Element oRainPrecipQuant = oDoc.createElement("ra");
				Element oSnowPrecipQuant = oDoc.createElement("sn");
				//if air temp is greater than 2 C all precip is rain
				if (dAirTemp > 2)
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(3600 * dPrecip)));   //quantity of precip is given in mm/sec, multiply by secs in an hour to get total
					oSnowPrecipQuant.appendChild(oDoc.createTextNode("0"));
				}
				//if air temp is less than -2 C all precip is snow/ice
				else if (dAirTemp < -2)
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode("0"));
					oSnowPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(360 * dPrecip)));   //for snow the we need the input in cm instead of mm
				}
				//if air temp is between -2 C and 2 C precip is a mix of rain and snow
				else
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(1800 * dPrecip)));  //half is rain
					oSnowPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(180 * dPrecip)));   //half is snow
				}
				oPrediction.appendChild(oRainPrecipQuant);
				oPrediction.appendChild(oSnowPrecipQuant);
				oPrediction.appendChild(oWindSpeed);

				Element oSurfacePressure = oDoc.createElement("ap");
				oSurfacePressure.appendChild(oDoc.createTextNode(Double.toString(oRAP.getReading(554, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oPrediction.appendChild(oSurfacePressure);

				oPrediction.appendChild(oCloudCoverage);
				
				oTime.add(Calendar.HOUR_OF_DAY, 1);
			}
			transformToXML(oDoc, m_sBaseDir + "forecast" + Thread.currentThread().getId() + ".xml");  //create and save XML file
		}
		catch(Exception e)
		{
		}
	}
		
	
	/**
	 * A method used to read a Roadcast XML file and then store all of the data
	 * in a list of arrays using the RoadcastDataFactory.
	 * 
	 * @param sFilename the name of the Roadcast XML file
	 */
	public void readRoadcastFile(String sFilename, MapCell oCell)
	{
		StringBuilder sStringBuilder = new StringBuilder();
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		double dLat = (oCell.m_dLatBot + oCell.m_dLatTop) / 2;
		double dLon = (oCell.m_dLonRight + oCell.m_dLonLeft) /2;
		
		try
		{
			// copy remote file index to buffer
			File oFile = new File(sFilename);
			if (!oFile.exists())
				return;
			BufferedInputStream oIn = new BufferedInputStream(new FileInputStream(oFile));
			int nByte; 
			while ((nByte = oIn.read()) >= 0)
				sStringBuilder.append((char)nByte);
			oIn.close();
			
			//get the time of the first roadcast and set the Calendar
			int nFirstRoadcastIndex = sStringBuilder.indexOf("<first-roadcast>");
			nFirstRoadcastIndex += "<first-roadcast>".length();
			String sFirstRoadcast = sStringBuilder.substring(nFirstRoadcastIndex, nFirstRoadcastIndex + "yyyy-MM-ddTHH:mmZ".length());
			oTime.set(Calendar.YEAR, Integer.parseInt(sFirstRoadcast.substring(0, 4)));
			oTime.set(Calendar.MONTH, Integer.parseInt(sFirstRoadcast.substring(5, 7)) - 1);
			oTime.set(Calendar.DATE, Integer.parseInt(sFirstRoadcast.substring(8, 10)));
			oTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sFirstRoadcast.substring(11, 13)));
			oTime.set(Calendar.MINUTE, Integer.parseInt(sFirstRoadcast.substring(14, 16)));
			oTime.set(Calendar.SECOND, 0);
			oTime.set(Calendar.MILLISECOND, 0);

			//start at the 3rd prediction, one hour after the last observation
			int nPredictionIndex = sStringBuilder.indexOf("<prediction>");
			nPredictionIndex = sStringBuilder.indexOf("<prediction>", nPredictionIndex + 1);  
			nPredictionIndex = sStringBuilder.indexOf("<prediction>", nPredictionIndex + 1);
			//get all of the Predictions from the XML file
			while (nPredictionIndex >= 0)
			{
				//update the time
				oTime.add(Calendar.HOUR_OF_DAY, 1); 
				long lTimestamp = oTime.getTimeInMillis();
				
				//get and set the Road Condition
				int nIndexBegin = sStringBuilder.indexOf("<rc>", nPredictionIndex) + "<rc>".length();
				int nIndexEnd = sStringBuilder.indexOf("</rc>", nIndexBegin);
				m_oRoadcastDataFactory.setValue(51137, lTimestamp, dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
				
				//get and set the Road Surface Temperature
				nIndexBegin = sStringBuilder.indexOf("<st>", nPredictionIndex) + "<st>".length();
				nIndexEnd = sStringBuilder.indexOf("</st>", nIndexBegin);
				m_oRoadcastDataFactory.setValue(51138, lTimestamp, dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
				
				//get and set the Road Sub Surface Temperature
				nIndexBegin = sStringBuilder.indexOf("<sst>", nPredictionIndex) + "<sst>".length();
				nIndexEnd = sStringBuilder.indexOf("</sst>", nIndexBegin);
				m_oRoadcastDataFactory.setValue(51165, lTimestamp, dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
						
				//skip two predictions to get to the next hour's prediction
				nPredictionIndex = sStringBuilder.indexOf("<prediction>", nPredictionIndex + 1);  
				nPredictionIndex = sStringBuilder.indexOf("<prediction>", nPredictionIndex + 1);
				nPredictionIndex = sStringBuilder.indexOf("<prediction>", nPredictionIndex + 1);
			}
		}
		catch (Exception e)
		{
		}
	}
	
	
	/**
	 * A method that fills the RoadMapCells Array List with every cell that
	 * contains a road.
	 */
	public final void getMapCells()
	{
		ArrayList<Road> oRoadList = new ArrayList();
		Roads oRoads = Roads.getInstance();
		double[][] dLatLonTR;   //[0][0] is lat
		double[][] dLatLonBL;   //[1][0] is lon
		double[][] dProjTR;     //[0][0] is x
		double[][] dProjBL;     //[1][0] is y
		m_oRoadMapCells = new ArrayList();

		//for each region defined in the configuration file
		for (MapCell oRegion : m_oBounds)
		{
			//convert lat and lon to lambert conformal projection x and y
			dProjTR = RoadcastDataFactory.latLonToLambert(oRegion.m_dLatTop, oRegion.m_dLonRight); //top right
			dProjBL = RoadcastDataFactory.latLonToLambert(oRegion.m_dLatBot, oRegion.m_dLonLeft);	//bottom left
			for (double x = dProjBL[0][0]; x < dProjTR[0][0]; x += m_dStepX)
			{
				for (double y = dProjBL[1][0]; y < dProjTR[1][0]; y += m_dStepY)
				{
					//convert projection back to lat lon
					dLatLonBL = RoadcastDataFactory.lambertToLatLon(x, y);
					dLatLonTR = RoadcastDataFactory.lambertToLatLon(x + m_dStepX, y + m_dStepY);
					//reset the road list
					oRoadList.clear();
					//find roads in the bounding box
					oRoads.getLinks(oRoadList, 1, MathUtil.toMicro(dLatLonBL[1][0]), MathUtil.toMicro(dLatLonBL[0][0]), 
						MathUtil.toMicro(dLatLonTR[1][0]), MathUtil.toMicro(dLatLonTR[0][0]));
					//if there are roads in the list, add the MapCell to the list of cells with roads
					if (!oRoadList.isEmpty())
					{
						m_oRoadMapCells.add(new MapCell(dLatLonTR[0][0], dLatLonBL[1][0],
							dLatLonBL[0][0], dLatLonTR[1][0], oRegion.m_sRegion));
						//add the road IDs to the MapCell's array of roads
						for (Road oRoad : oRoadList)
						{
							m_oRoadMapCells.get(m_oRoadMapCells.size() - 1).m_oRoads.add(oRoad.m_nId);
						}
					}
				}
			}
		}
	}
	
	
	/**
	 * This method returns whether or not there is a presence of precipitation
	 * in the last hour for the given coordinates and time. 1 means there is 
	 * precipitation, 0 means there is no precipitation
	 * 
	 * @param nLat         latitude in micro degrees
	 * @param nLon         longitude in micro degrees
	 * @param lTimestamp   time to look an hour back from
	 * @return   1 if there is precipitation. 0 if there is no precipitation
	 */
	public int getPresenceOfPrecip(int nLat, int nLon, long lTimestamp)
	{
		//check radar files every 2 minutes for the last hour. if reflectivity is greater than 0, there is precipitation
		for (int i = 0; i < 30 ; i++)
		{
			if (Radar.getInstance().getReading(0, lTimestamp - i * 120000, nLat, nLon) > 0)
				return 1;
		}
		
		return 0;
	}
	
	
	/**
	 * This method is ran every hour to set up and execute the process to run 
	 * METRo for every MapCell that contains a road.
	 */
	@Override
	public void run()
	{
		//check if the previous process is still running, if it is skip the next one
		if (m_nRunning.compareAndSet(0, m_oRoadMapCells.size()))
		{
			//reset Alerts
			m_oAlerts.clear();
			//set the time for the process
			m_lNow = System.currentTimeMillis();
			
			//initialize/update the RoadcastDataList
			m_oRoadcastDataFactory.initArrayList(m_lNow, m_nObservationHours, m_nForecastHours);
			
			//queue all of the MapCells into the work queue for the ThreadPool
			for (MapCell oCell : m_oRoadMapCells)
			{
				m_oThreadPool.execute(oCell);
			}
		}
	}
	
	
	/**
	 * This method creates alerts and warnings for all of the roads in the given 
	 * map cell.
	 * 
	 * @param oMapCell  the region to create alerts for
	 */
	public void createAlerts(MapCell oMapCell)
	{
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		
		//create alerts/warnings for each hour of roadcasts
		for (int i = 0; i < m_nForecastHours - 1; i++)  //number of roadcast hours is 1 less than forecast hours input
		{
			//read the road condition from the RoadcastData
			int nRoadCondition = (int) m_oRoadcastDataFactory.getReading(51137, oTime.getTimeInMillis(), (oMapCell.m_dLatBot + oMapCell.m_dLatTop) / 2, (oMapCell.m_dLonLeft + oMapCell.m_dLonRight) / 2);
			//convert METRo road conditions into 1204 ess standard numbers
			switch (nRoadCondition)
			{
				case 2:	
					nRoadCondition = 13; //pavement wet alert
					break;
				case 3:
					nRoadCondition = 14; //pavement snow alert
					break;
				case 4:
					nRoadCondition = 15; //pavement slick alert
					break;
				case 8:
					nRoadCondition = 17; //pavement ice alert
				default:
					nRoadCondition = 1;  //no alert

			}
			if (nRoadCondition > 1)
			{

				for (int nRoadID : oMapCell.m_oRoads)
				{
					//check to see if an alert for the road exists
					RoadAlert oAlert = new RoadAlert(nRoadID, nRoadCondition);
					int nAlertIndex = Collections.binarySearch(m_oAlerts, oAlert, this);
					//if the road does not have an existing alert, add the alert to the list
					if (nAlertIndex < 0)
						m_oAlerts.add(~nAlertIndex, oAlert);
				}
			}
			oTime.add(Calendar.HOUR_OF_DAY, 1);
		}
	}
	
	
	/**
	 * 
	 */
	public void printAlerts()
	{
		if (m_oAlerts.isEmpty())
			System.out.println("No alerts");
		else
			for (RoadAlert oAlert : m_oAlerts)
				System.out.println(oAlert.getRoadAlertMessage());
	}
	
	
	/**
	 * This method returns the RoadAlert message for the given road.
	 * @param oRoad  the desired road to get an alert for
	 * @return the alert message for that road
	 */
	public String getRoadAlert(Road oRoad)
	{
		for (RoadAlert oAlert : m_oAlerts)
			if (oRoad.m_nId == oAlert.m_nRoadID)
				return oAlert.getRoadAlertMessage();
		
		return "No alert";
	}
	
	
	/**
	 * This method deletes old METRo XML to manage space
	 */
	public void cleanupFiles()
	{
		File oForecast = new File(m_sBaseDir + "forecast" + Thread.currentThread().getId() + ".xml");
		File oStation = new File(m_sBaseDir + "station" + Thread.currentThread().getId() + ".xml"); 
		File oObservation = new File(m_sBaseDir + "observation" + Thread.currentThread().getId() + ".xml");
		File oRoadcast = new File(m_sBaseDir + "roadcast" + Thread.currentThread().getId() + ".xml");
		
		oForecast.delete();
		oStation.delete();
		oObservation.delete();
		if (oRoadcast.exists())
			oRoadcast.delete();
	}
	
	
/**
 * Allows RoadAlert objects to be compared by their RoadID
 * @param oLhs  left hand side
 * @param oRhs  right hand side
 * @return 
 */
	@Override
	public int compare(RoadAlert oLhs, RoadAlert oRhs)
	{
		return oLhs.m_nRoadID - oRhs.m_nRoadID;
	}
	
	
	@Override
	protected void doGet(HttpServletRequest oReq, HttpServletResponse oResp)
	{
		
	}
	
	
	@Override
	protected void doPost(HttpServletRequest oReq, HttpServletResponse oResp)
	{
		
	}
	
	
	/**
	 * An inner class that represents a single cell of a grid map. The cell is 
	 * defined by a bounding box of latitudes and longitudes. It contains a list
	 * of roads that are in the region
	 */
	public class MapCell implements Runnable
	{
		private final double m_dLatTop;
		private final double m_dLonLeft;
		private final double m_dLatBot;
		private final double m_dLonRight;
		private final String m_sRegion;
		private ArrayList<Integer> m_oRoads = new ArrayList();
		
		
		MapCell(double dLatTop, double dLonLeft, double dLatBot, double dLonRight, String sRegion)
		{
			m_dLatTop = dLatTop;
			m_dLonLeft = dLonLeft;
			m_dLatBot = dLatBot;
			m_dLonRight = dLonRight;
			m_sRegion = sRegion;
		}
		
		
		/**
		 * This method creates the 3 input files for METRo, runs METRo, reads the 
		 * resulting Roadcast Files, and creates Alerts for all map cells that 
		 * contain roads.
		 */
		@Override
		public void run()
		{
			//create the 3 input XML files
			createForecastXML(this);
			createStationXML(this);
			createObsXML(this);
			//run METRo
			try
			{
				Process oProcess = Runtime.getRuntime().exec("python /usr/local/metro/usr/bin/metro"
					+ " --input-forecast " + m_sBaseDir + "forecast" + Thread.currentThread().getId() + ".xml"
					+ " --input-station " + m_sBaseDir + "station" + Thread.currentThread().getId() + ".xml" 
					+ " --input-observation " + m_sBaseDir + "observation" + Thread.currentThread().getId() + ".xml" 
					+ " --output-roadcast " + m_sBaseDir + "roadcast" + Thread.currentThread().getId() + ".xml");
				oProcess.waitFor();
			}
			catch(Exception e)
			{
			}
			//read and save data from the output Roadcast File
			readRoadcastFile(m_sBaseDir + "roadcast" + Thread.currentThread().getId() + ".xml", this);
			System.out.print(m_nRunning.decrementAndGet() + " " + m_oRoadcastDataFactory.getReading(51138, m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2));
			System.out.println(" " + m_oRoadcastDataFactory.getReading(51165, m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2) + " " + m_oRoadcastDataFactory.getReading(51137, m_lNow + 3600000, (m_dLatTop + m_dLatBot) / 2, (m_dLonRight + m_dLonLeft) / 2));
			//delete old XML files
			cleanupFiles();
			//create alerts for all the roads in the MapCell
			createAlerts(this);
			if (m_nRunning.get() == 0)
				printAlerts();
		}
	}

	
	public static void main(String[] args)
	{
		MetroMgr oMetroMgr = MetroMgr.getInstance();
		oMetroMgr.run();
	}
}