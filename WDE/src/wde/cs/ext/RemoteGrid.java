package wde.cs.ext;


import java.util.Calendar;
import java.util.GregorianCalendar;
import wde.util.Scheduler;


/**
 * This abstract base class implements common NetCDF patterns for identifying, 
 * downloading, reading, and retrieving observation values for remote data sets.
 */
abstract class RemoteGrid extends RemoteData implements Runnable
{
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
	@Override
	protected abstract String getFilename(Calendar oNow);
	
	
	/**
	 * Abstract method overridden by subclasses to determine the destination
	 * file name for their specific remote data set
	 */
	@Override
	protected abstract String getDestFilename(String sScrFilename, Calendar oNow);
	

	/**
	 * Regularly called on a schedule to refresh the cached model data with 
	 * the most recently published model file.
	 */
	@Override
	public void run()
	{
		loadFile(new GregorianCalendar(Scheduler.UTC));
	}
	
	
	/**
	*  Initializes the data for the past 12 hours. 
	*
	*/
	@Override
	protected final void init()
	{
		Calendar iCalendar = Scheduler.getNextPeriod(m_nOffset, m_nPeriod);
		Scheduler.getInstance().schedule(this, m_nOffset, m_nPeriod, true);
		iCalendar.add(Calendar.SECOND, -m_nSecsBack);
		for (int i = 0; i < m_nSecsBack / m_nPeriod; i++)
		{
		  loadFile(iCalendar);
		  iCalendar.add(Calendar.SECOND, m_nPeriod); 
		}
	}
}