/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import wde.util.Scheduler;
import wde.util.IntKeyValue;

/**
 * National Digital Forecast Database. This class controls the scheduling of 
 * downloading the NDFD files including sky(cloud coverage), td(dew point), 
 * temp(air temperature), and wspd(wind speed).
 */
public class NDFD extends RemoteGrid
{
	private static final NDFD g_oNDFD = new NDFD();
	List<IntKeyValue<NDFDFile>> m_oNDFDFiles = new ArrayList<>();
	
	
	
	private NDFD()
	{
		m_nOffset = 3300;
		m_nPeriod = 3600;
		m_oNDFDFiles.add(new IntKeyValue(593, new NDFDSky()));
		m_oNDFDFiles.add(new IntKeyValue(575, new NDFDTd()));
		m_oNDFDFiles.add(new IntKeyValue(5733, new NDFDTemp()));
		m_oNDFDFiles.add(new IntKeyValue(56104, new NDFDWspd()));
		Scheduler.getInstance().schedule(this, m_nOffset, m_nPeriod, true);
	}
	
	/**
	 * Returns a reference to singleton NDFD model data cache.
	 *
	 * @return reference to NDFD data lookup instance.
	 */
	public static NDFD getInstance()
	{
		return g_oNDFD;
	}
	
	
	/**
	 * Regularly called on a schedule to refresh the cached model data with 
	 * the most recently published model files.
	 */
	@Override
	public void run()
	{
		Calendar oTime = new GregorianCalendar(Scheduler.UTC);
		for (IntKeyValue<NDFDFile> file : m_oNDFDFiles)
			file.value().loadFile(oTime);
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
	@Override
	public synchronized double getReading(int nObsTypeId, long lTimestamp, 
		int nLat, int nLon)
	{
		IntKeyValue<Object> oCompare = new IntKeyValue(nObsTypeId, null);
		IntKeyValue<NDFDFile> oFile = null;
		int nIndex = m_oNDFDFiles.size();
		while (nIndex-- > 0)
		{
			if (m_oNDFDFiles.get(nIndex).compareTo(oCompare) == 0)
				oFile = m_oNDFDFiles.get(nIndex);
		}

		if (oFile == null)
			return Double.NaN;

		return oFile.value().getReading(nObsTypeId, lTimestamp, nLat, nLon);
	}
	
	
	/**
	 * Always returns null because this class does not represent an actual
	 * model file.
	 * 
	 * @param sScrFilename
	 * @param oNow
	 * @return 
	 */
	@Override
	protected String getDestFilename(String sScrFilename, Calendar oNow)
	{
		return null;
	}
	
	/**
	 * Always returns null because this class does not represent an actual
	 * model file.
	 * 
	 * @param oNow
	 * @return 
	 */
	@Override
	protected String getFilename(Calendar oNow)
	{
		return null;
	}
	
	
	public static void main(String sArgs[])
	{
		NDFD oNDFD = NDFD.getInstance();
		System.out.println(oNDFD.getReading(593, System.currentTimeMillis(), 43000000, -94000000));
		System.out.println(oNDFD.getReading(575, System.currentTimeMillis(), 43000000, -94000000));
		System.out.println(oNDFD.getReading(5733, System.currentTimeMillis(), 43000000, -94000000));
		System.out.println(oNDFD.getReading(56104, System.currentTimeMillis(), 43000000, -94000000));
	}
}