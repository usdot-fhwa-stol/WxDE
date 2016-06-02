/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;


/**
 *
 * @author aaron.cherney
 */
public class RoadcastReader
{
	public static class RoadcastData
	{
		private long lTimestamp;
		private double dRoadSurfaceTemp;
		private double dRoadSubSurfaceTemp;
		private double dAirTemp;
		private double dDewPoint;
		private double dWindSpeed;
		private double dSnowIceOnRoad;
		private double dRainOnRoad;
		private double dSnowPrecip;
		private double dRainPrecip;
		private double dCloudCoverage;
	}
	
	private static String m_sRoadcast = "C:/Users/aaron.cherney/TestFiles/XML/roadcast";
	private static ArrayList<RoadcastData> m_oRoadcastData = new ArrayList();
	private static SimpleDateFormat m_oTimeStamp = new SimpleDateFormat("yyyy'-'MM'-'dd'T'HH':'mm'Z'");
	private static final RoadcastReader g_oRoadcastReader = new RoadcastReader();
	private int[] m_nObsType = new int[] {};
	private String[] m_sObsType = new String[] {};
	
	
	private RoadcastReader()
	{
		ReadXMLFile();
	}
	
	
	public static RoadcastReader getInstance()
	{
		return g_oRoadcastReader;
	}
	
	
	public static void ReadXMLFile()
	{
		StringBuilder sStringBuilder = new StringBuilder();
		Calendar oTime = new GregorianCalendar();
		
		try
		{
			BufferedInputStream oIn = new BufferedInputStream(new FileInputStream(new File(m_sRoadcast)));
			int nByte; // copy remote file index to buffer
			while ((nByte = oIn.read()) >= 0)
				sStringBuilder.append((char)nByte);
			oIn.close();
			
			int nFirstRoadcastIndex = sStringBuilder.indexOf("<first-roadcast>");
			nFirstRoadcastIndex += "<first-roadcast>".length();
			String sFirstRoadcast = sStringBuilder.substring(nFirstRoadcastIndex, nFirstRoadcastIndex + "yyyy-MM-ddTHH:mmZ".length());
			oTime.set(Calendar.YEAR, Integer.parseInt(sFirstRoadcast.substring(0, 4)));
			oTime.set(Calendar.MONTH, Integer.parseInt(sFirstRoadcast.substring(5, 7)) - 1);
			oTime.set(Calendar.DATE, Integer.parseInt(sFirstRoadcast.substring(8, 10)));
			oTime.set(Calendar.HOUR_OF_DAY, Integer.parseInt(sFirstRoadcast.substring(11, 13)));
			oTime.set(Calendar.MINUTE, Integer.parseInt(sFirstRoadcast.substring(14, 16)));

			int nPredictionIndex = sStringBuilder.indexOf("<prediction>");
			int nCounter = 0;
			while(nPredictionIndex >= 0)
			{
				int nIndexBegin;
				int nIndexEnd;
				m_oRoadcastData.add(new RoadcastData());
				m_oRoadcastData.get(nCounter).lTimestamp = oTime.getTimeInMillis();
				oTime.add(Calendar.MINUTE, 20);
				
				nIndexBegin = sStringBuilder.indexOf("<at>", nPredictionIndex) + "<at>".length();
				nIndexEnd = sStringBuilder.indexOf("</at>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dAirTemp = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));				
				
				nIndexBegin = sStringBuilder.indexOf("<td>", nPredictionIndex) + "<td>".length();
				nIndexEnd = sStringBuilder.indexOf("</td>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dDewPoint = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<ws>", nPredictionIndex) + "<ws>".length();
				nIndexEnd = sStringBuilder.indexOf("</ws>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dWindSpeed = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<sn>", nPredictionIndex) + "<sn>".length();
				nIndexEnd = sStringBuilder.indexOf("</sn>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dSnowIceOnRoad = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<ra>", nPredictionIndex) + "<ra>".length();
				nIndexEnd = sStringBuilder.indexOf("</ra>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dRainOnRoad = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<qp-sn>", nPredictionIndex) + "<qp-sn>".length();
				nIndexEnd = sStringBuilder.indexOf("</qp-sn>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dSnowPrecip = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<qp-ra>", nPredictionIndex) + "<qp-ra>".length();
				nIndexEnd = sStringBuilder.indexOf("</qp-ra>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dRainPrecip = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<st>", nPredictionIndex) + "<st>".length();
				nIndexEnd = sStringBuilder.indexOf("</st>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dRoadSurfaceTemp = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<sst>", nPredictionIndex) + "<sst>".length();
				nIndexEnd = sStringBuilder.indexOf("</sst>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dRoadSubSurfaceTemp = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nIndexBegin = sStringBuilder.indexOf("<cc>", nPredictionIndex) + "<cc>".length();
				nIndexEnd = sStringBuilder.indexOf("</cc>", nIndexBegin);
				m_oRoadcastData.get(nCounter).dCloudCoverage = Double.parseDouble(sStringBuilder.substring(nIndexBegin, nIndexEnd));
				
				nPredictionIndex = sStringBuilder.indexOf("<prediction>", nPredictionIndex + 1);
				nCounter++;
			}
		}
		catch (Exception e)
		{
		}
	}
	
	
	
	public static double getRoadcastData(String sObsType, long lTimestamp)
	{
		int nIndex = -1;
		
		for (int i = 0; i < m_oRoadcastData.size() - 1; i++)
		{
			if(lTimestamp >= m_oRoadcastData.get(i).lTimestamp && lTimestamp < m_oRoadcastData.get(i + 1).lTimestamp)
			{
				nIndex = i;
				break;
			}
		}
		
		if (nIndex < 0)
			return Double.NaN;     //timestamp is not in the range of the file
		
		if ("st".equals(sObsType))
			return m_oRoadcastData.get(nIndex).dRoadSurfaceTemp;
		if ("at".equals(sObsType))
			return m_oRoadcastData.get(nIndex).dAirTemp;
		
		if ("td".equals(sObsType))
			return m_oRoadcastData.get(nIndex).dDewPoint;
		
		return Double.NaN;
	}
	public static void main(String[] args)
	{
		RoadcastReader oRoadcastReader = RoadcastReader.getInstance();
		for (int i = 0; i < m_oRoadcastData.size(); i++)
		{
			System.out.println(m_oTimeStamp.format(m_oRoadcastData.get(i).lTimestamp));
			System.out.println("\t" + m_oRoadcastData.get(i).dAirTemp);
			System.out.println("\t" + m_oRoadcastData.get(i).dCloudCoverage);
			System.out.println("\t" + m_oRoadcastData.get(i).dDewPoint);
			System.out.println("\t" + m_oRoadcastData.get(i).dRainOnRoad);
			System.out.println("\t" + m_oRoadcastData.get(i).dRainPrecip);
			System.out.println("\t" + m_oRoadcastData.get(i).dRoadSubSurfaceTemp);
			System.out.println("\t" + m_oRoadcastData.get(i).dRoadSurfaceTemp);
			System.out.println("\t" + m_oRoadcastData.get(i).dSnowIceOnRoad);
			System.out.println("\t" + m_oRoadcastData.get(i).dSnowPrecip);
			System.out.println("\t" + m_oRoadcastData.get(i).dWindSpeed);
		}
	}
}
