package wde.cs.ext;


import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;


/**
 * This abstract base class implements common NetCDF patterns for identifying, 
 * downloading, reading, and retrieving observation values for remote data sets.
 */
abstract class RemoteGrid implements Runnable
{
	/**
	* Lookup arrays map names between model and observation types.
	*/
	protected int m_nDelay;
	protected int m_nRange;
	protected int m_nLimit;
	protected int[] m_nObsTypes;
	protected String m_sHrz;
	protected String m_sVrt;
	protected String m_sBaseDir = "/dev/shm/";
	protected String m_sBaseURL;
	protected String m_sPrevFilename = "";
	protected String[] m_sObsTypes;
	protected ArrayDeque<NcfWrapper> m_oGrids = new ArrayDeque();


	/**
	 * Default package private constructor.
	 */
	RemoteGrid()
	{
	}


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
	 * Regularly called on a schedule to refresh the cached model data with 
	 * the most recently published model file.
	 */
	@Override
	public void run()
	{
		GregorianCalendar oNow = new GregorianCalendar();
		String sFilename = getFilename(oNow);
		if (sFilename == null || m_sPrevFilename.contains(sFilename))
			return; // file name could not be resolved or matches previous download

		m_sPrevFilename = sFilename; // save source filename
		String sDestFile = m_sBaseDir; // ignore intervening directories in path
		int nSepIndex = sFilename.lastIndexOf("/");
		if (nSepIndex >= 0)
			sDestFile += sFilename.substring(nSepIndex); // extract the file name
		else
			sDestFile += sFilename; // local file name

		try
		{
			URL oUrl = new URL(m_sBaseURL + sFilename); // retrieve remote data file
			BufferedInputStream oIn = new BufferedInputStream(oUrl.openStream());
			BufferedOutputStream oOut = new BufferedOutputStream(
				new FileOutputStream(sDestFile));
			int nByte; // copy remote data to local file
			while ((nByte = oIn.read()) >= 0)
				oOut.write(nByte);
			oIn.close(); // tidy up input and output streams
			oOut.close();

			NcfWrapper oNc = new NcfWrapper(m_nObsTypes, m_sObsTypes, m_sHrz, m_sVrt);
			oNc.load(oNow.getTimeInMillis() - m_nDelay, 
				oNow.getTimeInMillis() + m_nRange, sDestFile);

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
		}
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
}
