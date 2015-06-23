/**
 * @file Scheduler.java
 */
package util;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executor;
import util.threads.ThreadPool;

/**
 * Provides a means of scheduling executable tasks to be ran from the
 * thread-pool on a fixed cycle.
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link Scheduler#getInstance() }
 * </p>
 */
public class Scheduler
{
	/**
	 * Pointer to the singleton instance of {@code Scheduler}.
	 */
	private static Scheduler g_oInstance = new Scheduler();

	/**
	 * Pointer to the {@link ThreadPool} singleton instance.
	 */
	private Executor m_iExecutor = ThreadPool.getInstance();
	/**
	 * Pointer to the {@code Timer} singleton instance.
	 */
	private Timer m_oTimer = new Timer();
	/**
	 * Default timezone.
	 */
	private SimpleTimeZone m_oUTC = new SimpleTimeZone(0, "");


	/**
	 * <b> Accessor </b>
	 * @return singelton instance of {@code Scheduler}.
	 */
	public static Scheduler getInstance()
	{
		return g_oInstance;
	}


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Logs the call to this constructor, which creates a new instance of
	 * {@code Scheduler}.
	 * </p>
	 */
	private Scheduler()
	{
		Log.getInstance().write(this, "constructor");
	}


	/**
	 * Schedules the provided executable to run on a fixed cycle based off the
	 * provided offset from midnight on the interval of the provided period.
	 * @param iRunnable executable object to be scheduled.
	 * @param nOffset schedule offset from midnight.
	 * @param nPeriod period of execution.
	 * @return newly created scheduled task.
	 */
	public TimerTask schedule(Runnable iRunnable, int nOffset, int nPeriod)
	{
		Calendar iCalendar = new GregorianCalendar(m_oUTC);

		// set the current time to midnight UTC and add the schedule offset
		iCalendar.set(Calendar.HOUR_OF_DAY, 0);
		iCalendar.set(Calendar.MINUTE, 0);
		iCalendar.set(Calendar.SECOND, nOffset);
		iCalendar.set(Calendar.MILLISECOND, 0);

		// adjust the period from seconds to milliseconds
		nPeriod *= 1000;
		// determine the timestamp of the next period
		long lOffsetTime = iCalendar.getTime().getTime();
		long lDeltaTime = System.currentTimeMillis() - lOffsetTime;
		long lPeriods = lDeltaTime / nPeriod;
		if (lDeltaTime % nPeriod > 0)
			++lPeriods;

		iCalendar.setTimeInMillis(lOffsetTime + lPeriods * nPeriod);

		// create the scheduled task
		Sched oTask = new Sched(iRunnable);
		m_oTimer.scheduleAtFixedRate(oTask, iCalendar.getTime(), nPeriod);

		return oTask;
	}


	/**
	 * Schedules the provided runnable interface on a fixed cycle starting
	 * after the specified delay with each execution separated by the period.
	 * @param iRunnable executable object to be scheduled.
	 * @param lDelay milliseconds to wait before beginning initial execution.
	 * @param lPeriod milliseconds between execution cycles.
	 * @return newly created scheduled task.
	 */
	public TimerTask schedule(Runnable iRunnable, long lDelay, long lPeriod)
	{
		Sched oTask = new Sched(iRunnable);
		m_oTimer.scheduleAtFixedRate(oTask, lDelay, lPeriod);
		return oTask;
	}


	/**
	 * Allows scheduled thread-pool execution of processes.
	 * <p>
	 * Extends {@code TimerTask} to allow scheduling for one-time or repeated
	 * execution by a Timer.
	 * </p>
	 */
	private class Sched extends TimerTask
	{
		/**
		 * Object of scheduled execution.
		 */
		private Runnable m_iRunnable;


        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code Sched}
		 * </p>
         */
		private Sched()
		{
		}


		/**
		 * <b> Constructor </b>
		 * <p>
		 * Initializes the runnable object to the provided value.
		 * </p>
		 * @param iRunnable object to be scheduled to run.
		 */
		private Sched(Runnable iRunnable)
		{
			m_iRunnable = iRunnable;
		}


		/**
		 * Queues the runnable object to be executed by a thread from the
		 * thread pool.
		 */
		@Override
		public void run()
		{
			// queue the work to be performed by another thread
			if (m_iExecutor != null)
				m_iExecutor.execute(m_iRunnable);
		}
	}
}
