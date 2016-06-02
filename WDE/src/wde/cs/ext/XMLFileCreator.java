/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import wde.util.Scheduler;

/**
 *
 * @author aaron.cherney
 */
public class XMLFileCreator
{
	private static final SimpleDateFormat m_oObservationTime = new SimpleDateFormat(
		"yyyy'-'MM'-'dd'T'HH':'mm'Z'");
	
	public static void createObsXML(Calendar oTime, int nHoursBack)
	{
		oTime.add(Calendar.HOUR_OF_DAY, -nHoursBack + 1);
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
			oRoadStation.appendChild(oDoc.createTextNode("oce"));
			oHeader.appendChild(oRoadStation);

			Element oMeasureList = oDoc.createElement("measure-list");
			oObservation.appendChild(oMeasureList);

			for (int i = 0;i < nHoursBack; i++)
			{
				Element oMeasure = oDoc.createElement("measure");
				oMeasureList.appendChild(oMeasure);

				Element oObservationTime = oDoc.createElement("observation-time");
				oObservationTime.appendChild(oDoc.createTextNode(m_oObservationTime.format(oTime.getTime())));
				oMeasure.appendChild(oObservationTime);

				Element oAirTemp = oDoc.createElement("at");
				oAirTemp.appendChild(oDoc.createTextNode(Double.toString(RTMA.getInstance().getReading(5733, oTime.getTimeInMillis(), 43000000, -94000000))));
				oMeasure.appendChild(oAirTemp);

				Element oDewPoint = oDoc.createElement("td");
				oDewPoint.appendChild(oDoc.createTextNode(Double.toString(RTMA.getInstance().getReading(575, oTime.getTimeInMillis(), 43000000, -94000000))));
				oMeasure.appendChild(oDewPoint);

				Element oPresenceOfPrecip = oDoc.createElement("pi");
				if(Radar.getInstance().getReading(0, oTime.getTimeInMillis(), 43000000, -94000000) > 0)
					oPresenceOfPrecip.appendChild(oDoc.createTextNode("1"));
				else
					oPresenceOfPrecip.appendChild(oDoc.createTextNode("0"));
				oMeasure.appendChild(oPresenceOfPrecip);

				Element oWindSpeed = oDoc.createElement("ws");
				oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(RTMA.getInstance().getReading(56104, oTime.getTimeInMillis(), 43000000, -94000000))));
				oMeasure.appendChild(oWindSpeed);

				Element oRoadCondition = oDoc.createElement("sc");
				oRoadCondition.appendChild(oDoc.createTextNode("6"));
				oMeasure.appendChild(oRoadCondition);

				Element oRoadSurfaceTemp = oDoc.createElement("st");
				oRoadSurfaceTemp.appendChild(oDoc.createTextNode("-4.3"));
				oMeasure.appendChild(oRoadSurfaceTemp);

				Element oRoadSubSurfaceTemp = oDoc.createElement("sst");
				oRoadSubSurfaceTemp.appendChild(oDoc.createTextNode("0"));
				oMeasure.appendChild(oRoadSubSurfaceTemp);
				
				oTime.add(Calendar.HOUR_OF_DAY, 1);
			}
		  
			TransformerFactory oTransformerFactory = TransformerFactory.newInstance();
			Transformer oTransformer = oTransformerFactory.newTransformer();
			DOMSource oSource = new DOMSource(oDoc);
			File oFile = new File("C:/Users/aaron.cherney/TestFiles/XML/obs");
			if (!oFile.exists())
				oFile.createNewFile();
			StreamResult oResult = new StreamResult(oFile);

			oTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
			oTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			oTransformer.transform(oSource, oResult);
			System.out.println("Obs file saved!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void createStationXML(int nRoadLayers)
	{
		try
		{
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
			oRoadStation.appendChild(oDoc.createTextNode("oce"));
			oHeader.appendChild(oRoadStation);
			
			Element oTimeZone = oDoc.createElement("time-zone");
			oTimeZone.appendChild(oDoc.createTextNode("EST5EDT"));
			oHeader.appendChild(oTimeZone);
			
			Element oProductionDate = oDoc.createElement("production-date");
			oProductionDate.appendChild(oDoc.createTextNode(m_oObservationTime.format(System.currentTimeMillis())));
			oHeader.appendChild(oProductionDate);
			
			Element oCooridnate = oDoc.createElement("coordinate");
			oHeader.appendChild(oCooridnate);
			
			Element oLatitude = oDoc.createElement("latitude");
			oLatitude.appendChild(oDoc.createTextNode("46"));
			oCooridnate.appendChild(oLatitude);
			
			Element oLongitude = oDoc.createElement("longitude");
			oLongitude.appendChild(oDoc.createTextNode("-84"));
			oCooridnate.appendChild(oLongitude);
			
			Element oStationType = oDoc.createElement("station-type");
			oStationType.appendChild(oDoc.createTextNode("road"));
			oHeader.appendChild(oStationType);
			
			Element oRoadLayerList = oDoc.createElement("roadlayer-list");
			oStation.appendChild(oRoadLayerList);

			for (int i = 0;i < nRoadLayers; i++)
			{
				Element oRoadLayer = oDoc.createElement("roadlayer");
				oRoadLayerList.appendChild(oRoadLayer);

				Element oPostition = oDoc.createElement("position");
				oPostition.appendChild(oDoc.createTextNode(Integer.toString(i+1)));
				oRoadLayer.appendChild(oPostition);
				
				Element oType = oDoc.createElement("type");
				oType.appendChild(oDoc.createTextNode("asphalt"));
				oRoadLayer.appendChild(oType);
				
				Element oThickness = oDoc.createElement("thickness");
				oThickness.appendChild(oDoc.createTextNode("0.5"));
				oRoadLayer.appendChild(oThickness);
			}
		  
			TransformerFactory oTransformerFactory = TransformerFactory.newInstance();
			Transformer oTransformer = oTransformerFactory.newTransformer();
			DOMSource oSource = new DOMSource(oDoc);
			File oFile = new File("C:/Users/aaron.cherney/TestFiles/XML/station");
			if (!oFile.exists())
				oFile.createNewFile();
			StreamResult oResult = new StreamResult(oFile);

			oTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
			oTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			oTransformer.transform(oSource, oResult);
			System.out.println("Station file saved!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void createForecastXML(Calendar oTime, int nHoursForward)
	{
		try
		{
			DocumentBuilderFactory oDocFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder oDocBuilder = oDocFactory.newDocumentBuilder();

			Document oDoc = oDocBuilder.newDocument();
			
			Element oForecast = oDoc.createElement("forecast");
			oDoc.appendChild(oForecast);

			Element oHeader = oDoc.createElement("header");
			oForecast.appendChild(oHeader);

			Element oVersion = oDoc.createElement("version");
			oVersion.appendChild(oDoc.createTextNode("1.0"));
			oHeader.appendChild(oVersion);

			Element oProductionDate = oDoc.createElement("production-date");
			oProductionDate.appendChild(oDoc.createTextNode(m_oObservationTime.format(System.currentTimeMillis())));
			oHeader.appendChild(oProductionDate);
			
			Element oStationID = oDoc.createElement("station-id");
			oStationID.appendChild(oDoc.createTextNode("oce"));
			oHeader.appendChild(oStationID);

			Element oPredictionList = oDoc.createElement("prediction-list");
			oForecast.appendChild(oPredictionList);

			for (int i = 0;i < nHoursForward; i++)
			{
				Element oPrediction = oDoc.createElement("prediction");
				oPredictionList.appendChild(oPrediction);

				Element oForecastTime = oDoc.createElement("forecast-time");
				oForecastTime.appendChild(oDoc.createTextNode(m_oObservationTime.format(oTime.getTime())));
				oPrediction.appendChild(oForecastTime);

				Element oAirTemp = oDoc.createElement("at");
				oAirTemp.appendChild(oDoc.createTextNode(Double.toString(NDFD.getInstance().getReading(5733, oTime.getTimeInMillis(), 43000000, -94000000))));
				oPrediction.appendChild(oAirTemp);

				Element oDewPoint = oDoc.createElement("td");
				oDewPoint.appendChild(oDoc.createTextNode(Double.toString(NDFD.getInstance().getReading(575, oTime.getTimeInMillis(), 43000000, -94000000))));
				oPrediction.appendChild(oDewPoint);

				Element oRainPrecipQuant = oDoc.createElement("ra");
				oRainPrecipQuant.appendChild(oDoc.createTextNode("1"));
				oPrediction.appendChild(oRainPrecipQuant);

				Element oSnowPrecipQuant = oDoc.createElement("sn");
				oSnowPrecipQuant.appendChild(oDoc.createTextNode("6"));
				oPrediction.appendChild(oSnowPrecipQuant);
				
				Element oWindSpeed = oDoc.createElement("ws");
				oWindSpeed.appendChild(oDoc.createTextNode(Double.toString(NDFD.getInstance().getReading(56104, oTime.getTimeInMillis(), 43000000, -94000000))));
				oPrediction.appendChild(oWindSpeed);

				Element oSurfacePressure = oDoc.createElement("ap");
				oSurfacePressure.appendChild(oDoc.createTextNode("1"));
				oPrediction.appendChild(oSurfacePressure);

				Element oCloudCoverage = oDoc.createElement("cc");
				oCloudCoverage.appendChild(oDoc.createTextNode(Double.toString(NDFD.getInstance().getReading(593, oTime.getTimeInMillis(), 43000000, -94000000))));
				oPrediction.appendChild(oCloudCoverage);
				
				oTime.add(Calendar.HOUR_OF_DAY, 1);
			}
		  
			TransformerFactory oTransformerFactory = TransformerFactory.newInstance();
			Transformer oTransformer = oTransformerFactory.newTransformer();
			DOMSource oSource = new DOMSource(oDoc);
			File oFile = new File("C:/Users/aaron.cherney/TestFiles/XML/forecast");
			if (!oFile.exists())
				oFile.createNewFile();
			StreamResult oResult = new StreamResult(oFile);

			oTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
			oTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			oTransformer.transform(oSource, oResult);
			System.out.println("Forecast file saved!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	
	public static void main(String[] args)
	{
		Radar.getInstance();
		RTMA.getInstance();
		NDFD.getInstance();
		createObsXML(new GregorianCalendar(Scheduler.UTC), 3);
		createStationXML(2);
		createForecastXML(new GregorianCalendar(Scheduler.UTC), 1);
		System.exit(0);
	}
}
