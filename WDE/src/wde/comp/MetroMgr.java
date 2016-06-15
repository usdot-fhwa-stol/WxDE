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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import wde.comp.RoadcastDataFactory.RoadcastData;


import wde.cs.ext.NDFD;
import wde.cs.ext.RTMA;
import wde.cs.ext.Radar;
import wde.cs.ext.RAP;
import wde.data.osm.Road;
import wde.data.osm.Roads;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;

/**
 * A singleton class that manages the use of METRo including writing input files, 
 * reading output files, and storing output files using the RoadcastDataFactory.
 * 
 * @author aaron.cherney
 */
public class MetroMgr implements Runnable, Comparator<Alert>
{
	private static final MetroMgr g_oMetroMgr = new MetroMgr();
	private RoadcastDataFactory m_oRoadcastDataFactory = RoadcastDataFactory.getInstance();
	private String m_sBaseDir;
	private ArrayList<MapCell> m_oRoadMapCells;
	private ArrayList<MapCell> m_oBounds;
	private ArrayList<Alert> m_oAlerts = new ArrayList();
	private final SimpleDateFormat m_oTimestamp = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm'Z'");
	private final int m_nForecastHours;
	private final int m_nObservationHours;
	private double m_dLatTop;
	private double m_dLonLeft;
	private double m_dLatBot;
	private double m_dLonRight;
	private final int m_nColumns = 2145;               //lon
	private final int m_nRows = 1377;                  //lat
	private final double m_dColLeftLimit = -2763.2046;  //lon
	private final double m_dColRightLimit = 2681.9185;
	private final double m_dRowLeftLimit = -263.78943;   //lat
	private final double m_dRowRightLimit = 3230.8418;
	private final double m_dStepX = 2.5385189277;
	private final double m_dStepY = 2.5378585548;
	private double m_dReflectivityAverage[][] = new double[m_nColumns][m_nRows];
	private double m_dOldestReflectivity[][] = new double[m_nColumns][m_nRows];
	private long m_lNow;
	private AtomicInteger m_nRunning = new AtomicInteger();
	private ExecutorService m_oThreadPool = Executors.newFixedThreadPool(2);
	


	private MetroMgr()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nForecastHours = oConfig.getInt("fhours", 3);
		m_nObservationHours = oConfig.getInt("ohours", 12);
		m_sBaseDir = oConfig.getString("dir", "/run/shm/");
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
			m_oBounds.add(new MapCell(0, 0, m_dLatTop, m_dLonLeft, m_dLatBot, m_dLonRight, sRegion));
		}		
		getMapCells();
		initAveragePrecip();
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
		int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
		int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonRight) / 2 );
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		
		oTime.add(Calendar.HOUR_OF_DAY, -m_nObservationHours + 1);
		try
		{
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

			for (int i = 0; i < m_nObservationHours; i++)
			{
				Element oMeasure = oDoc.createElement("measure");
				oMeasureList.appendChild(oMeasure);

				Element oObservationTime = oDoc.createElement("observation-time");
				oObservationTime.appendChild(oDoc.createTextNode(m_oTimestamp.format(oTime.getTime())));
				oMeasure.appendChild(oObservationTime);

				double dAirTemp = RTMA.getInstance().getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				Element oAirTemp = oDoc.createElement("at");
				oAirTemp.appendChild(oDoc.createTextNode(Double.toString(dAirTemp)));
				oMeasure.appendChild(oAirTemp);

				Element oDewPoint = oDoc.createElement("td");
				oDewPoint.appendChild(oDoc.createTextNode(Double.toString(RTMA.getInstance().getReading(575, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oDewPoint);

				int nPresenceOfPrecip = getPresenceOfPrecip(oCell);
				Element oPresenceOfPrecip = oDoc.createElement("pi");
				oPresenceOfPrecip.appendChild(oDoc.createTextNode(Integer.toString(nPresenceOfPrecip)));
				oMeasure.appendChild(oPresenceOfPrecip);

				Element oWindSpeed = oDoc.createElement("ws");
				oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(RTMA.getInstance().getReading(56104, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oWindSpeed);

				Element oRoadCondition = oDoc.createElement("rc");
				Element oRoadSurfaceTemp = oDoc.createElement("st");
				Element oRoadSubSurfaceTemp = oDoc.createElement("sst");

				
				RoadcastData oRoadcastData = m_oRoadcastDataFactory.getRoadcastData(oTime.getTimeInMillis(), "rc");
				if (Double.isNaN(oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex]))
				{
					if (nPresenceOfPrecip > 0)
					{
						if (dAirTemp > 2)
						{
							oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex] = 2;  //wet road
							oRoadCondition.appendChild(oDoc.createTextNode("2")); 
						}
						else if (dAirTemp < -2)
						{
							oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex] = 3;  //ice/snow on the road
							oRoadCondition.appendChild(oDoc.createTextNode("3")); 
						}
						else
						{
							oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex] = 4;  //mix water/snow on the road
						}
					}
					else
						oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex] = 1;  //dry road
				}
				
				oRoadCondition.appendChild(oDoc.createTextNode(Double.toString(oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex])));
				oMeasure.appendChild(oRoadCondition);
				
				oRoadcastData = m_oRoadcastDataFactory.getRoadcastData(oTime.getTimeInMillis(), "st");
				if (Double.isNaN(oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex]))
				{
					oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex] = RTMA.getInstance().getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);	
				}
				
				oRoadSurfaceTemp.appendChild(oDoc.createTextNode(Double.toString(oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex])));
				oMeasure.appendChild(oRoadSurfaceTemp);
				
				oRoadcastData = m_oRoadcastDataFactory.getRoadcastData(oTime.getTimeInMillis(), "sst");
				if (Double.isNaN(oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex]))
				{
					oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex] = RTMA.getInstance().getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				}
				
				oRoadSubSurfaceTemp.appendChild(oDoc.createTextNode(Double.toString(oRoadcastData.m_dValueArray[oCell.m_nRowIndex][oCell.m_nColIndex])));
				oMeasure.appendChild(oRoadSubSurfaceTemp);
				
				oTime.add(Calendar.HOUR_OF_DAY, 1);
			}
			 
			transformToXML(oDoc, m_sBaseDir + "observation" + Thread.currentThread().getId() + ".xml");
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
			int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
			int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonLeft) / 2 );
			
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
			oLatitude.appendChild(oDoc.createTextNode(Double.toString(MathUtil.fromMicro(nMicroLat))));
			oCooridnate.appendChild(oLatitude);
			
			Element oLongitude = oDoc.createElement("longitude");
			oLongitude.appendChild(oDoc.createTextNode(Double.toString(MathUtil.fromMicro(nMicroLon))));
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
			oThickness.appendChild(oDoc.createTextNode("0.5"));
			oRoadLayer.appendChild(oThickness);

		  
			transformToXML(oDoc, m_sBaseDir + "station" + Thread.currentThread().getId() + ".xml");
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
			int nMicroLat = MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2);
			int nMicroLon = MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonRight) / 2 );
			Calendar oTime = new GregorianCalendar();
			oTime.setTimeInMillis(m_lNow);
			DocumentBuilderFactory oDocFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder oDocBuilder = oDocFactory.newDocumentBuilder();

			Document oDoc = oDocBuilder.newDocument();
			
			Element oForecast = oDoc.createElement("forecast");
			oDoc.appendChild(oForecast);

			Element oHeader = oDoc.createElement("header");
			oForecast.appendChild(oHeader);

			Element oVersion = oDoc.createElement("version");
			oVersion.appendChild(oDoc.createTextNode("1.1"));
			oHeader.appendChild(oVersion);

			Element oProductionDate = oDoc.createElement("production-date");
			oProductionDate.appendChild(oDoc.createTextNode(m_oTimestamp.format(m_lNow)));
			oHeader.appendChild(oProductionDate);
			
			Element oStationID = oDoc.createElement("station-id");
			oStationID.appendChild(oDoc.createTextNode(nMicroLat + "_" + nMicroLon));
			oHeader.appendChild(oStationID);

			Element oPredictionList = oDoc.createElement("prediction-list");
			oForecast.appendChild(oPredictionList);

			for (int i = 0;i < m_nForecastHours; i++)
			{
				Element oPrediction = oDoc.createElement("prediction");
				oPredictionList.appendChild(oPrediction);

				Element oForecastTime = oDoc.createElement("forecast-time");
				oForecastTime.appendChild(oDoc.createTextNode(m_oTimestamp.format(oTime.getTime())));
				oPrediction.appendChild(oForecastTime);

				double dAirTemp = NDFD.getInstance().getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				Element oAirTemp = oDoc.createElement("at");
				oAirTemp.appendChild(oDoc.createTextNode(Double.toString(dAirTemp)));
				oPrediction.appendChild(oAirTemp);

				Element oDewPoint = oDoc.createElement("td");
				oDewPoint.appendChild(oDoc.createTextNode(Double.toString(NDFD.getInstance().getReading(575, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oPrediction.appendChild(oDewPoint);

				double dPrecip = RAP.getInstance().getReading(587, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				Element oRainPrecipQuant = oDoc.createElement("ra");
				Element oSnowPrecipQuant = oDoc.createElement("sn");
				if (dAirTemp > 2)
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(3600 * dPrecip)));
					oSnowPrecipQuant.appendChild(oDoc.createTextNode("0"));
				}
				else if (dAirTemp < -2)
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode("0"));
					oSnowPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(360 * dPrecip)));
				}
				else
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(1800 * dPrecip)));
					oSnowPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(180 * dPrecip)));
				}
				oPrediction.appendChild(oRainPrecipQuant);
				oPrediction.appendChild(oSnowPrecipQuant);
				
				Element oWindSpeed = oDoc.createElement("ws");
				oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(NDFD.getInstance().getReading(56104, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oPrediction.appendChild(oWindSpeed);

				Element oSurfacePressure = oDoc.createElement("ap");
				oSurfacePressure.appendChild(oDoc.createTextNode(Double.toString(RAP.getInstance().getReading(554, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oPrediction.appendChild(oSurfacePressure);

				Element oCloudCoverage = oDoc.createElement("cc");
				oCloudCoverage.appendChild(oDoc.createTextNode(Integer.toString((int) NDFD.getInstance().getReading(593, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oPrediction.appendChild(oCloudCoverage);
				
				oTime.add(Calendar.HOUR_OF_DAY, 1);
			}
			transformToXML(oDoc, m_sBaseDir + "forecast" + Thread.currentThread().getId() + ".xml");
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
	public void readRoadcastFile(String sFilename)
	{
		StringBuilder sStringBuilder = new StringBuilder();
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		
		
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
			
			//get latitude from file
			int nIndexBegin = sStringBuilder.indexOf("<latitude>") + "<latitude>".length();
			int nIndexEnd = sStringBuilder.indexOf("</latitude>", nIndexBegin);
			double dLat =  Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
			
			//get longitude from file
			nIndexBegin = sStringBuilder.indexOf("<longitude>") + "<longitude>".length();
			nIndexEnd = sStringBuilder.indexOf("</longitude>", nIndexBegin);
			double dLon = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
			
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
				nIndexBegin = sStringBuilder.indexOf("<rc>", nPredictionIndex) + "<rc>".length();
				nIndexEnd = sStringBuilder.indexOf("</rc>", nIndexBegin);
				m_oRoadcastDataFactory.setValue(lTimestamp, "rc", dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
				
				//get and set the Road Surface Temperature
				nIndexBegin = sStringBuilder.indexOf("<st>", nPredictionIndex) + "<st>".length();
				nIndexEnd = sStringBuilder.indexOf("</st>", nIndexBegin);
				m_oRoadcastDataFactory.setValue(lTimestamp, "st", dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
				
				//get and set the Road Sub Surface Temperature
				nIndexBegin = sStringBuilder.indexOf("<sst>", nPredictionIndex) + "<sst>".length();
				nIndexEnd = sStringBuilder.indexOf("</sst>", nIndexBegin);
				m_oRoadcastDataFactory.setValue(lTimestamp, "sst", dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
						
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
		double[][] dLatLonTR;
		double[][] dLatLonBL;
		double[][] dProjTR;
		double[][] dProjBL;
		m_oRoadMapCells = new ArrayList();

		for (MapCell oRegion : m_oBounds)
		{
			dProjTR = RoadcastDataFactory.latLonToLambert(oRegion.m_dLatTop, oRegion.m_dLonRight);
			dProjBL = RoadcastDataFactory.latLonToLambert(oRegion.m_dLatBot, oRegion.m_dLonLeft);	
			for (double x = dProjBL[0][0]; x < dProjTR[0][0]; x += m_dStepX)
			{
				for (double y = dProjBL[1][0]; y < dProjTR[1][0]; y += m_dStepY)
				{

					dLatLonBL = RoadcastDataFactory.lambertToLatLon(x, y);
					dLatLonTR = RoadcastDataFactory.lambertToLatLon(x + m_dStepX, y + m_dStepY);
					oRoadList.clear();
					oRoads.getLinks(oRoadList, 1, MathUtil.toMicro(dLatLonBL[1][0]), MathUtil.toMicro(dLatLonBL[0][0]), 
						MathUtil.toMicro(dLatLonTR[1][0]), MathUtil.toMicro(dLatLonTR[0][0]));
					if (!oRoadList.isEmpty())
					{
						m_oRoadMapCells.add(new MapCell(RoadcastDataFactory.getIndex(x, m_dColLeftLimit, m_dColRightLimit, m_nColumns),
							RoadcastDataFactory.getIndex(y, m_dRowLeftLimit, m_dRowRightLimit, m_nRows), dLatLonTR[0][0], dLatLonBL[1][0],
							dLatLonBL[0][0], dLatLonTR[1][0], oRegion.m_sRegion));
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
	 * This method calculates if there has been any precipitation at the given lat,lon.
	 * It does this by checking the reflectivity of all the cached Radar files for 
	 * the given coordinates
	 * 
	 */
	public final void initAveragePrecip()
	{
		Radar oRadar = Radar.getInstance();
		long lTime = System.currentTimeMillis();
		for (MapCell oCell : m_oRoadMapCells)
		{
			for (int i = 0; i < m_nObservationHours * 30; i++)
			{
				double dReflectivity = oRadar.getReading(0, lTime - i * 120000, MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2), MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonLeft) / 2));
				if (dReflectivity > 0)
					m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] += dReflectivity;
				if (i == m_nObservationHours * 30 - 1)
					m_dOldestReflectivity[oCell.m_nRowIndex][oCell.m_nColIndex] = dReflectivity;
			}
			m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] /= (m_nObservationHours * 30);
		}
	}
	
	
	public void updateAveragePrecip()
	{
		for (MapCell oCell : m_oRoadMapCells)
		{
			m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] *= (m_nObservationHours * 30);
			m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] -= m_dOldestReflectivity[oCell.m_nRowIndex][oCell.m_nColIndex];
			m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] += Radar.getInstance().getReading(0, System.currentTimeMillis(), MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2), MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonLeft) / 2));
			m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] /= (m_nObservationHours * 30);
			m_dOldestReflectivity[oCell.m_nRowIndex][oCell.m_nColIndex] = Radar.getInstance().getReading(0, System.currentTimeMillis() - m_nObservationHours * 30 * 120000, MathUtil.toMicro((oCell.m_dLatBot + oCell.m_dLatTop) / 2), MathUtil.toMicro((oCell.m_dLonRight + oCell.m_dLonLeft) / 2));
		}
		System.out.println("updated");
	}
	
	
	public int getPresenceOfPrecip(MapCell oCell)
	{
		if (m_dReflectivityAverage[oCell.m_nRowIndex][oCell.m_nColIndex] > 0)
			return 1;
		else
			return 0;
	}
	
	
	/**
	 * This method creates the 3 input files for METRo, runs METRo, reads the 
	 * resulting Roadcast Files, and creates Alerts for all map cells that 
	 * contain roads.
	 */
	@Override
	public void run()
	{
		//if (m_nRunning.compareAndSet(0, m_oRoadMapCells.size()))
		if (m_nRunning.compareAndSet(0, 500))
		{
			m_lNow = System.currentTimeMillis();
			m_oRoadcastDataFactory.initArrayList(m_lNow, m_nObservationHours, m_nForecastHours);
//			for (MapCell oCell : m_oRoadMapCells)
			for (int i = 0; i < 500; i++)
			{
				MapCell oCell = m_oRoadMapCells.get(i);
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
		boolean bFirstTime = true;
		Calendar oTime = new GregorianCalendar();
		oTime.setTimeInMillis(m_lNow);
		for (int i = 0; i < (m_nForecastHours - 1) * 3; i++)
		{
			int nRoadCondition = (int) m_oRoadcastDataFactory.getValue(oTime.getTimeInMillis(), "rc", (oMapCell.m_dLatBot + oMapCell.m_dLatTop) / 2, (oMapCell.m_dLonLeft + oMapCell.m_dLonRight) / 2);
			if (nRoadCondition > 1) //Road Condition 1 is a dry road, do not need an alert for it
			{
				//the first time roadcast creates an alert
				if (bFirstTime)
				{
					for (int nRoadID : oMapCell.m_oRoads)
					{
						//check to see if an alert/warning for the road exists
						Alert oAlert = new Alert(nRoadID, nRoadCondition);
						int nAlertIndex = Collections.binarySearch(m_oAlerts, oAlert, this);
						if (nAlertIndex < 0)
							m_oAlerts.add(~nAlertIndex, oAlert);
						else
						{
							//if the road already has a warning, replace it with the alert
							if (m_oAlerts.get(nAlertIndex).m_nAlertID > 8)
							{
								m_oAlerts.remove(nAlertIndex);
								m_oAlerts.add(nAlertIndex, oAlert);
							}
						}
					}
				}
				//all other future roadcast times create warnings
				else
				{
					for (int nRoadID : oMapCell.m_oRoads)
					{
						Alert oAlert = new Alert(nRoadID, nRoadCondition + 10);
						int nAlertIndex = Collections.binarySearch(m_oAlerts, oAlert, this);
						if (nAlertIndex < 0)
							m_oAlerts.add(~nAlertIndex, oAlert);
					}
				}
			}
			oTime.add(Calendar.HOUR_OF_DAY, 1);
		}
	}
	
	
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
 * Allows Alert objects to be compared by their RoadID
 * @param oLhs  left hand side
 * @param oRhs  right hand side
 * @return 
 */
	@Override
	public int compare(Alert oLhs, Alert oRhs)
	{
		return oLhs.m_nRoadID - oRhs.m_nRoadID;
	}
	
	
	/**
	 * An inner class that represents a single cell of a grid map. The cell is 
	 * defined by a bounding box of latitudes and longitudes. It contains a list
	 * of roads that are in the region
	 */
	public class MapCell implements Runnable
	{
		private final int m_nColIndex;
		private final int m_nRowIndex;
		private final double m_dLatTop;
		private final double m_dLonLeft;
		private final double m_dLatBot;
		private final double m_dLonRight;
		private final String m_sRegion;
		private ArrayList<Integer> m_oRoads = new ArrayList();
		
		MapCell(int nColIndex, int nRowIndex, double dLatTop, double dLonLeft, double dLatBot, double dLonRight, String sRegion)
		{
			m_nColIndex = nColIndex;
			m_nRowIndex = nRowIndex;
			m_dLatTop = dLatTop;
			m_dLonLeft = dLonLeft;
			m_dLatBot = dLatBot;
			m_dLonRight = dLonRight;
			m_sRegion = sRegion;
		}
		
		@Override
		public void run()
		{
			createForecastXML(this);
			createStationXML(this);
			createObsXML(this);
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
			readRoadcastFile(m_sBaseDir + "roadcast" + Thread.currentThread().getId() + ".xml");
			cleanupFiles();
			createAlerts(this);
			System.out.println(m_nRunning.decrementAndGet() + " " + this.m_nRowIndex + " " + this.m_nColIndex + " " + m_oRoadcastDataFactory.getRoadcastData(m_lNow + 3600000, "st").m_dValueArray[this.m_nRowIndex][this.m_nColIndex]);
		}
	}
	
	
	public static void main(String[] args)
	{
		MetroMgr oMetroMgr = MetroMgr.getInstance();
		oMetroMgr.run();
	}
}

