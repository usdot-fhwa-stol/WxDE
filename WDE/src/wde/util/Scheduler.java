/**
 * @file Scheduler.java
 */
package wde.util;

import org.apache.log4j.Logger;
import wde.util.threads.ThreadPool;

import java.util.*;
import java.util.concurrent.Executor;

/**
 * Provides a means of scheduling executable tasks to be ran from the
 * thread-pool on a fixed cycle.
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link Scheduler#getInstance() }
 * </p>
 */
public class Scheduler {
    private static final Logger logger = Logger.getLogger(Scheduler.class);

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
    public static final SimpleTimeZone UTC = new SimpleTimeZone(0, "");


    /**
     * <b> Default Constructor </b>
     * <p>
     * Logs the call to this constructor, which creates a new instance of
     * {@code Scheduler}.
     * </p>
     */
    private Scheduler() {
        logger.info("calling constructor");
    }

    /**
     * <b> Accessor </b>
     *
     * @return singelton instance of {@code Scheduler}.
     */
    public static Scheduler getInstance() {
        return g_oInstance;
    }

    
    /**
     * Determines the next period to execute based off of the offset from midnight and 
     * the period of execution
     *
     * @param nOffset   schedule offset from midnight.
     * @param nPeriod   period of execution.
     * @return Calendar object of the next period .
     */
    public static Calendar getNextPeriod(int nOffset, int nPeriod)
    {
        Calendar iCalendar = new GregorianCalendar(UTC);

        // set the current time to midnight UTC and add the schedule offset
        iCalendar.set(Calendar.HOUR_OF_DAY, 0);
        iCalendar.set(Calendar.MINUTE, 0);
        iCalendar.set(Calendar.SECOND, nOffset);
        iCalendar.set(Calendar.MILLISECOND, 0);

        // adjust the period from seconds to milliseconds
        nPeriod *= 1000;
        // determine the timestamp of the next period
        long lOffsetTime = iCalendar.getTimeInMillis();
        long lDeltaTime = System.currentTimeMillis() - lOffsetTime;
        long lPeriods = lDeltaTime / nPeriod;
        if (lDeltaTime % nPeriod > 0)
            ++lPeriods;
        iCalendar.setTimeInMillis(lOffsetTime + lPeriods * nPeriod);
        return iCalendar;
    }
    
    
    /**
     * Schedules the provided executable to run on a fixed cycle based off the
     * provided offset from midnight on the interval of the provided period.
     *
     * @param iRunnable executable object to be scheduled.
     * @param nOffset   schedule offset from midnight.
     * @param nPeriod   period of execution.
     * @return newly created scheduled task.
     */
    public TimerTask schedule(Runnable iRunnable, int nOffset, int nPeriod, 
       boolean bUseThreadPool) 
    {
        Calendar iCalendar = getNextPeriod(nOffset, nPeriod);
        // create the scheduled task
        Sched oTask = new Sched(iRunnable, bUseThreadPool);
        m_oTimer.scheduleAtFixedRate(oTask, iCalendar.getTime(), nPeriod * 1000);
        return oTask;
    }
	 
	 
	 public TimerTask scheduleOnce(Runnable iRunnable, int nDelay)
	 {
		 Sched oTask = new Sched(iRunnable, true);
		 m_oTimer.schedule(oTask, nDelay);
		 return oTask;
	 }


    /**
     * Allows scheduled thread-pool execution of processes.
     * <p>
     * Extends {@code TimerTask} to allow scheduling for one-time or repeated
     * execution by a Timer.
     * </p>
     */
    private class Sched extends TimerTask {
        /**
         * Object of scheduled execution.
         */
        private Runnable m_iRunnable;
        private boolean m_bUseThreadPool;


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
         *
         * @param iRunnable object to be scheduled to run.
         */
        private Sched(Runnable iRunnable, boolean bUseThreadPool) 
        {
            m_iRunnable = iRunnable;
            m_bUseThreadPool = bUseThreadPool;
        }


        /**
         * Queues the runnable object to be executed by a thread from the
         * thread pool.
         */
        @Override
        public void run() 
        {
            // queue the work to be performed by another thread
            if (m_bUseThreadPool && m_iExecutor != null)
                m_iExecutor.execute(m_iRunnable);
            else
                m_iRunnable.run();
        }
    }
}
