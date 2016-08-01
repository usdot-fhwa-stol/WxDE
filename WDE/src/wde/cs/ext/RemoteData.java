/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package wde.cs.ext;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.Iterator;
import org.apache.log4j.Logger;

/**
 * This abstract base class implements common NetCDF patterns for identifying, 
 * downloading, reading, and retrieving observation values for remote data sets.
 */
abstract class RemoteData 
{
	private static final Logger m_oLogger = Logger.getLogger(RemoteData.class);
	protected int m_nDelay;
	protected int m_nRange;
	protected int m_nLimit;
	protected int[] m_nObsTypes;
	protected String m_sHrz;
	protected String m_sVrt;
	protected String m_sTime;
	protected String m_sBaseDir;
   //protected String m_sBaseDir = "/run/shm/";
	protected String m_sBaseURL;
	protected String[] m_sObsTypes;
	protected ArrayDeque<NcfWrapper> m_oGrids = new ArrayDeque();
	protected int m_nOffset;
	protected int m_nPeriod;
	protected int m_nInitTime;
	
	RemoteData()
	{
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
		NcfWrapper oNc = null;
		Iterator<NcfWrapper> oIt = m_oGrids.iterator(); // find most recent file
		while (oIt.hasNext() && oNc == null) // that encompasses the timestamp
		{
			NcfWrapper oTempNc = oIt.next();
			if (lTimestamp < oTempNc.m_lEndTime && lTimestamp >= oTempNc.m_lStartTime)
				oNc = oTempNc;
		}

		if (oNc == null)
			return Double.NaN; // requested time outside of buffered time ranges
	
		return oNc.getReading(nObsTypeId, lTimestamp, nLat, nLon);
	}
	
	
	/**
	 * Abstract method overridden by subclasses to determine the destination
	 * file name for their specific remote data set
	 */
	protected abstract String getDestFilename(String sScrFilename, Calendar oNow);
	

	/**
	 * Abstract method overridden by subclasses to determine the remote and local 
	 * file name for their specific remote data set.
	 *
	 * @param oNow	Calendar object used for time-based dynamic URLs
	 * 
	 * @return the URL where remote data can be retrieved.
	 */
	protected abstract String getFilename(Calendar oNow);	
	
	
	/**
	*  Loads model files from disk. If the file is not of the disk the 
	*  function will download the file from the URL.
	* 
	*  @param oTime Calendar object used for the time
	*
	*/
	public void loadFile(Calendar oTime)
	{
		String sFilename = getFilename(oTime);
		if (sFilename == null)
			return; // file name could not be resolved

		String sDestFile = getDestFilename(sFilename, oTime);
		m_oLogger.info("Loading file: " + sDestFile);
		try
		{
			File oFile = new File(sDestFile);
			if(!oFile.exists())  //if the file doesn't exist load it from URL
			{
				URL oUrl = new URL(m_sBaseURL + sFilename); // retrieve remote data file
				BufferedInputStream oIn = new BufferedInputStream(oUrl.openStream());
				BufferedOutputStream oOut = new BufferedOutputStream(
					new FileOutputStream(oFile));
				int nByte; // copy remote data to local file
				while ((nByte = oIn.read()) >= 0)
					oOut.write(nByte);
				oIn.close(); // tidy up input and output streams
				oOut.close();
			}

			NcfWrapper oNc = new NcfWrapper(m_nObsTypes, m_sObsTypes, m_sHrz, m_sVrt, m_sTime);
			oNc.load(oTime.getTimeInMillis() - m_nDelay, 
				oTime.getTimeInMillis() + m_nRange, sDestFile);

			 NcfWrapper oRemoveNc = null;
			 synchronized(this)
			{
				if (m_oGrids.size() == m_nLimit) // old NetCDF files fall off bottom
					oRemoveNc = m_oGrids.removeLast();
				m_oGrids.push(oNc); // new NetCDF files go on top
			}
			if (oRemoveNc != null) // non-synchronized remove from local storage
					oRemoveNc.cleanup(); // no list modification, cleanup relatively lengthy
		}
		catch(Exception oException) // failed to download new data
		{
			oException.printStackTrace();
		}
	} 
	
	
	/**
	 * Abstract method overridden by child classes to initialize data files when 
	 * the program is first started.
	 */
	protected abstract void init();
}
