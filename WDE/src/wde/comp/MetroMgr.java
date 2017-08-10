
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
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import ucar.ma2.ArrayDouble;
import ucar.ma2.ArrayFloat;
import ucar.ma2.ArrayInt;
import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;


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
 * A singleton class that manages the use and scheduling of METRo.  It contains
 * a method that determines which MapCells need to be processed by METRo, 
 * utility methods, and a method that create alerts for the MapCells. When its 
 * run() is called, it places all of the MapCells with roads in them into a 
 * thread pool where they are then processed by METRo.
 * 
 * @author aaron.cherney
 */
public class MetroMgr implements Runnable, Comparator<RoadAlert>
{
	private static final MetroMgr g_oMetroMgr = new MetroMgr();
	private MetroResults m_oMetroResults = MetroResults.getInstance();
	private ArrayList<Road> m_oRoads;
	private ArrayList<MapCell> m_oRoadMapCells;  //stores all of the MapCells that contain roads
	private ArrayList<MapCell> m_oBounds;        //stores the bounds of all of the regions the program is ran on
	private ArrayList<RoadAlert> m_oAlerts = new ArrayList();
	private final SimpleDateFormat m_oTimestamp = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm'Z'");
	public final int m_nForecastHours;
	public final int m_nObservationHours;
	private final int m_nThreads;
	private final int m_nOffset;
	private final int m_nPeriod;
	private final int m_nRadarFilesPerHour = 15;
	private double m_dLatTop;
	private double m_dLonLeft;
	private double m_dLatBot;
	private double m_dLonRight;
	private final double m_dStepX = 2.5385189277;
	private final double m_dStepY = 2.5378585548;
	public long m_lNow;
	public AtomicInteger m_nRunning = new AtomicInteger();
	private ExecutorService m_oThreadPool;
	public String m_sBaseDir;
	private String m_sNcBaseDir;
	

	private MetroMgr()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		m_nThreads = oConfig.getInt("threads", 1);
		m_oThreadPool = Executors.newFixedThreadPool(m_nThreads);
		m_sBaseDir = oConfig.getString("pythondir", "/run/shm/");
		m_sNcBaseDir = oConfig.getString("ncdir", "/home/cherneya/");
		m_nForecastHours = oConfig.getInt("fhours", 3);
		m_nObservationHours = oConfig.getInt("ohours", 12);
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
		run();
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
		m_oRoads = new ArrayList();

		//for each region defined in the configuration file
		for (MapCell oRegion : m_oBounds)
		{
			//convert lat and lon to lambert conformal projection x and y, need to do this because the grid we are basing our map off of is in lambert conformal
			dProjTR = MetroResults.latLonToLambert(oRegion.m_dLatTop, oRegion.m_dLonRight); //top right
			dProjBL = MetroResults.latLonToLambert(oRegion.m_dLatBot, oRegion.m_dLonLeft);	//bottom left
			for (double x = dProjBL[0][0]; x < dProjTR[0][0]; x += m_dStepX)
			{
				for (double y = dProjBL[1][0]; y < dProjTR[1][0]; y += m_dStepY)
				{
					//convert projection back to lat lon
					dLatLonBL = MetroResults.lambertToLatLon(x, y);
					dLatLonTR = MetroResults.lambertToLatLon(x + m_dStepX, y + m_dStepY);
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
							//check to see the road is in the list, add it if it is not
							synchronized(m_oRoads)
							{
								int nRoadIndex = Collections.binarySearch(m_oRoads, oRoad, oRoads);
								if (nRoadIndex < 0) // include a road in each grid cell only once
										m_oRoads.add(~nRoadIndex, oRoad);
							}
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
		//check radar files every 4 minutes for the last hour. if reflectivity is greater than 0, there is precipitation
		for (int i = 0; i < m_nRadarFilesPerHour ; i++)
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
			//set the time for the process
			Calendar oNow = new GregorianCalendar();
			oNow.set(Calendar.MILLISECOND, 0);
			oNow.set(Calendar.SECOND, 0);
			oNow.set(Calendar.MINUTE, m_nOffset);
			m_lNow = oNow.getTimeInMillis();
			
			//reset Alerts
			m_oAlerts.clear();
			//initialize/update the MetroResults List
			m_oMetroResults.initArrayList(m_lNow, m_nObservationHours, m_nForecastHours);
			
			System.out.println("Started METRo at " + m_oTimestamp.format(m_lNow));
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
			//read the road condition from MetroResults
			int nRoadCondition = (int) m_oMetroResults.getReading(51137, oTime.getTimeInMillis(), (oMapCell.m_dLatBot + oMapCell.m_dLatTop) / 2, (oMapCell.m_dLonLeft + oMapCell.m_dLonRight) / 2);
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
					break;
				default:
					nRoadCondition = 1;  //no alert

			}
			if (nRoadCondition > 1)
			{
				synchronized(m_oAlerts)
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
			}
			oTime.add(Calendar.HOUR_OF_DAY, 1);
		}
	}
	
	
	/**
	 *  This function prints all of the roads that have an alert and the kind of
	 *  alert for that road
	 */
	public void printAlerts()
	{
		if (m_oAlerts.isEmpty())
			System.out.println("No alerts");
		else
			for (RoadAlert oAlert : m_oAlerts)
				System.out.println(oAlert.m_nRoadID + ": " + oAlert.getRoadAlertMessage());
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
				if (Double.isNaN(m_oMetroResults.getReading(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon)))
				{
					if (nPresenceOfPrecip > 0)  //check if there is precipitation
					{
						if (dAirTemp > 2)  //temp greater than 2 C means rain
							m_oMetroResults.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 2); //wet road
						else if (dAirTemp < -2) //temp less than -2 C means snow/ice
							m_oMetroResults.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 3);  //ice/snow on the road
						else //temp between -2 C and 2 C means mix
							m_oMetroResults.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 4);  //mix water/snow on the road
					}
					else  //no precipitation so dry road
						m_oMetroResults.setValue(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon, 1);  //dry road
				}
				
				oRoadCondition.appendChild(oDoc.createTextNode(Double.toString(m_oMetroResults.getReading(51137, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oRoadCondition);
				

				//if there is no roadcast data initialize surface temp as the air temp from RTMA
				if (Double.isNaN(m_oMetroResults.getReading(51138, oTime.getTimeInMillis(), nMicroLat, nMicroLon)))
					m_oMetroResults.setValue(51138, oTime.getTimeInMillis(), nMicroLat, nMicroLon, oRTMA.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon));
				
				oRoadSurfaceTemp.appendChild(oDoc.createTextNode(Double.toString(m_oMetroResults.getReading(51138, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
				oMeasure.appendChild(oRoadSurfaceTemp);
				
				//if there is no roadcast data initialize sub surface temp as the air temp from RTMA
				if (Double.isNaN(m_oMetroResults.getReading(51165, oTime.getTimeInMillis(), nMicroLat, nMicroLon)))
					m_oMetroResults.setValue(51165, oTime.getTimeInMillis(), nMicroLat, nMicroLon, oRTMA.getReading(5733, oTime.getTimeInMillis(), nMicroLat, nMicroLon));
				
				oRoadSubSurfaceTemp.appendChild(oDoc.createTextNode(Double.toString(m_oMetroResults.getReading(51165, oTime.getTimeInMillis(), nMicroLat, nMicroLon))));
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

				int nPrecipType = (int)oRAP.getReading(207, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				double dPrecip = oRAP.getReading(587, oTime.getTimeInMillis(), nMicroLat, nMicroLon);
				Element oRainPrecipQuant = oDoc.createElement("ra");
				Element oSnowPrecipQuant = oDoc.createElement("sn");
				if (nPrecipType == 4 || nPrecipType == 6) //rain
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(3600 * dPrecip)));   //quantity of precip is given in mm/sec, multiply by secs in an hour to get total
					oSnowPrecipQuant.appendChild(oDoc.createTextNode("0"));
				}
				else if (nPrecipType == 5) //snow
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode("0"));
					oSnowPrecipQuant.appendChild(oDoc.createTextNode(Double.toString(360 * dPrecip)));   //for snow the we need the input in cm instead of mm
				}
				else
				{
					oRainPrecipQuant.appendChild(oDoc.createTextNode("0"));
					oSnowPrecipQuant.appendChild(oDoc.createTextNode("0"));
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
				m_oMetroResults.setValue(51137, lTimestamp, dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
				
				//get and set the Road Surface Temperature
				nIndexBegin = sStringBuilder.indexOf("<st>", nPredictionIndex) + "<st>".length();
				nIndexEnd = sStringBuilder.indexOf("</st>", nIndexBegin);
				m_oMetroResults.setValue(51138, lTimestamp, dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
				
				//get and set the Road Sub Surface Temperature
				nIndexBegin = sStringBuilder.indexOf("<sst>", nPredictionIndex) + "<sst>".length();
				nIndexEnd = sStringBuilder.indexOf("</sst>", nIndexBegin);
				m_oMetroResults.setValue(51165, lTimestamp, dLat, dLon, Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd))); 
						
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
	 * This function creates and writes a NetCDF that contains the results from a
	 * run of METRo as well as some of the forecast inputs for that run. It 
	 * follows the formatting from the MDSS Project section 12.6.4.2.
	 */
	public void writeNetcdfFile()
	{
		NDFD oNDFD = NDFD.getInstance();
		RAP oRAP = RAP.getInstance();
		MetroResults oMR = MetroResults.getInstance();
		String sLocation = m_sNcBaseDir + "RCTM.nc";
		int nRoads = m_oRoads.size();		
		File oFile = new File(sLocation);
		if (oFile.exists()) //if the file exists delete it
			oFile.delete();
		
		try
		{
			NetcdfFileWriter oWriter = NetcdfFileWriter.createNew(NetcdfFileWriter.Version.netcdf3, sLocation);
			//create dimensions
			Dimension oMaxSiteNum = oWriter.addDimension(null, "max_site_num", nRoads);
			Dimension oNumTimes = oWriter.addDimension(null, "num_times", m_nForecastHours - 1);
			//create dimension list, only add MaxSiteNum for now
			ArrayList<Dimension> oDims = new ArrayList<>();
			oDims.add(oMaxSiteNum);
			//create all of the variables and attributes
			Variable dCreationTime = oWriter.addVariable(null, "creation_time", DataType.DOUBLE, new ArrayList());
			dCreationTime.addAttribute(new Attribute("long_name", "time at which forecast file was created"));
			dCreationTime.addAttribute(new Attribute("unit", "seconds since 1970-1-1 00:00:00"));
				
			Variable dObsTime = oWriter.addVariable(null, "obs_time", DataType.DOUBLE, "");
			dObsTime.addAttribute(new Attribute("long_name", "time of earliest obs"));
			dObsTime.addAttribute(new Attribute("units", "seconds since 1970-1-1 00:00:00"));
			
			Variable nNumSites = oWriter.addVariable(null, "num_sites", DataType.INT, "");
			nNumSites.addAttribute(new Attribute("long_name", "number of actual_sites"));
			
			Variable nSiteList = oWriter.addVariable(null, "site_list", DataType.INT, oDims);
			nSiteList.addAttribute(new Attribute("long_name", "number of actual_sites"));
			
			Variable nType = oWriter.addVariable(null, "type", DataType.INT, "");
			nType.addAttribute(new Attribute("long_name", "cdl file type"));
			
			oDims.add(oNumTimes); //now add NumTimes to the dimension list because all of the other variables need it as a dimension
			
			Variable fTemp = oWriter.addVariable(null, "T", DataType.FLOAT, oDims);
			fTemp.addAttribute(new Attribute("long_name", "2m air temperature"));
			fTemp.addAttribute(new Attribute("units", "degrees Celsius"));
			
			Variable fDewpt = oWriter.addVariable(null, "dewpt", DataType.FLOAT, oDims);
			fDewpt.addAttribute(new Attribute("long_name", "dew point temperature"));
			fDewpt.addAttribute(new Attribute("units", "degrees Celsius"));
			
			Variable fWindSpeed = oWriter.addVariable(null, "wind_speed", DataType.FLOAT, oDims);
			fWindSpeed.addAttribute(new Attribute("long_name", "windspeed"));
			fWindSpeed.addAttribute(new Attribute("units", "meters per second"));	
			
			Variable nPrecip = oWriter.addVariable(null, "Precip", DataType.FLOAT, oDims);
			nPrecip.addAttribute(new Attribute("long_name", "Presence of Precip"));
			nPrecip.addAttribute(new Attribute("units", "0=None, 1=Precip"));
			
			Variable fRoadTemp = oWriter.addVariable(null, "road_T", DataType.FLOAT, oDims);
			fRoadTemp.addAttribute(new Attribute("long_name", "road surface temperature"));
			fRoadTemp.addAttribute(new Attribute("units", "degrees Celsius"));
			
			Variable fBridgeTemp = oWriter.addVariable(null, "bridge_T", DataType.FLOAT, oDims);
			fBridgeTemp.addAttribute(new Attribute("long_name", "bridge surface temperature"));
			fBridgeTemp.addAttribute(new Attribute("units", "degrees Celsius"));
			
			Variable nRoadCond = oWriter.addVariable(null, "road_condition", DataType.INT, oDims);
			nRoadCond.addAttribute(new Attribute("long_name", "road condition"));
			nRoadCond.addAttribute(new Attribute("units", "33=dry, 34=wet"));	
			
			Variable fSubSurfaceTemp = oWriter.addVariable(null, "subsurface_T", DataType.FLOAT, oDims);
			fSubSurfaceTemp.addAttribute(new Attribute("long_name", "subsurface temperature"));
			fSubSurfaceTemp.addAttribute(new Attribute("units", "degrees Celsius"));
			
			oWriter.create(); //create the file
			//add values for all of the variables to the file.
			ArrayDouble.D0 dScalar = new ArrayDouble.D0();
			dScalar.set(m_lNow / 1000); //set the CreationTime in seconds
			oWriter.write(dCreationTime, dScalar);
			
			dScalar.set((m_lNow + 3600000) / 1000); //set the ObsTime(one hour after METRo ran
			oWriter.write(dObsTime, dScalar);
			
			ArrayInt.D0 nScalar = new ArrayInt.D0();
			nScalar.set(nRoads); //set NumSites, which is the number of roads in the region
			oWriter.write(nNumSites, nScalar);
			
			ArrayInt.D1 nOneDim = new ArrayInt.D1(nRoads);
			for (int i = 0; i < nRoads; i++)
				nOneDim.setInt(i, m_oRoads.get(i).m_nId); //set the SiteList, which is the RoadIds
			oWriter.write(nSiteList, nOneDim);
			
			nScalar.set(3);  //set the cdl file type
			oWriter.write(nType, nScalar);
			
			ArrayFloat.D2 fTwoDim = new ArrayFloat.D2(nRoads, m_nForecastHours - 1);
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++)  //set the Air Temps from NDFD
					fTwoDim.set(i, j, (float)oNDFD.getReading(5733, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid)); //the first roadcast is an hour from the time METRo is ran
			oWriter.write(fTemp, fTwoDim);
			
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++) //set the Dew Point from NDFD
					fTwoDim.set(i, j, (float)oNDFD.getReading(575, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid)); 
			oWriter.write(fDewpt, fTwoDim);
			
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++) //set the WindSpeed from NDFD
					fTwoDim.set(i, j, (float)oNDFD.getReading(56104, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid)); 
			oWriter.write(fWindSpeed, fTwoDim);
			
			ArrayInt.D2 nTwoDim = new ArrayInt.D2(nRoads, m_nForecastHours - 1);
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++) //set the Presence of Precip using RAP
				{
					if ((int)oRAP.getReading(207, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid) == 3)
						nTwoDim.set(i, j, 0); //no precip
					else
						nTwoDim.set(i, j, 1); //precip
				}
			oWriter.write(nPrecip, nTwoDim);
			
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++)  //set the RoadTemp from METRo
					fTwoDim.set(i, j, (float)oMR.getReading(51138, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid)); 
			oWriter.write(fRoadTemp, fTwoDim);	
			
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++) //set the BridgeTemp from METRo (right now same as RoadTemp)
					fTwoDim.set(i, j, (float)oMR.getReading(51138, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid)); 
			oWriter.write(fBridgeTemp, fTwoDim);
			
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++) //set the RoadCond from METRo, 1 means dry road so anything else is considered wet
				{
					if (oMR.getReading(51137, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid) == 1)
						nTwoDim.set(i, j, 33); //dry
					else
						nTwoDim.set(i, j, 34); //wet
				}
			oWriter.write(nRoadCond, nTwoDim);			
			
			for (int i = 0; i < nRoads; i++)
				for (int j = 0; j < m_nForecastHours - 1; j++) //set the SubSurfaceTemp from METRo
					fTwoDim.set(i, j, (float)oMR.getReading(51165, m_lNow + (3600000 * (j + 1)), m_oRoads.get(i).m_nYmid, m_oRoads.get(i).m_nXmid)); 
			oWriter.write(fSubSurfaceTemp, fTwoDim);			
			
			oWriter.close();	//close the file
			
		} catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}
	
	
	public static void main(String[] args)
	{
		MetroMgr oMM = MetroMgr.getInstance();
		
	}
}