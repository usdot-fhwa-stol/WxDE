/**
 * ThreadPool.java
 */
package wde.util.threads;

import org.apache.log4j.Logger;
import wde.util.Config;
import wde.util.ConfigSvc;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Creates a pool of threads of which to execute processes via
 * {@link ThreadPool#execute(Runnable)}.
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link ThreadPool#getInstance()}
 * </p>
 * <p>
 * Implements {@code Executor} interface to easily allow execution of processes,
 * and to provide an abstraction over the details of the running process.
 * </p>
 */
public class ThreadPool implements Executor {
    private static final Logger logger = Logger.getLogger(ThreadPool.class);

    /**
     * Configured maximum number of threads to allocate to this thread pool.
     */
    private static int MAX_THREADS = 4;
    /**
     * Pointer to the singleton thread pool instance.
     */
    private static ThreadPool g_oInstance = new ThreadPool();


    /**
     * Executes commands from the thread pool via calls to
     * {@link ThreadPool#execute(Runnable)}.
     */
    private ExecutorService m_oThreadPoolExecutor;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Configures the thread pool thread count, and initializes the
     * {@code ExecutorService}.
     * </p>
     */
    private ThreadPool() {
        Config oConfig = ConfigSvc.getInstance().getConfig(this);
        MAX_THREADS = oConfig.getInt("maxthreads", MAX_THREADS);

        m_oThreadPoolExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        logger.info("Completing constructor");
    }

    /**
     * <b> Accessor </b>
     *
     * @return singleton instance of {@code ThreadPool}.
     */
    public static ThreadPool getInstance() {
        return g_oInstance;
    }

    /**
     * Executes the given {@code Runnable} object at some point in the future
     * from the thread pool.
     * <p>
     * Implements required interface class method
     * {@link Executor#execute(Runnable) }.
     * </p>
     *
     * @param iRunnable the process to execute.
     */
    public void execute(Runnable iRunnable) {
        m_oThreadPoolExecutor.execute(iRunnable);
    }
}
