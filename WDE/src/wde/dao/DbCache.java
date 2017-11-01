// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file DbCache.java
 */
package wde.dao;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Introsort;
import wde.util.Scheduler;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Contains a searchable cache of the most current record of the queried type.
 * Provides an efficient means of comparing, accessing, and searching recently
 * cached values.
 * <p/>
 * <p>
 * Abstract implementation forces extensions to define extension specific
 * methods:
 * <pre> {@link DbCache#toString(PrintWriter, int)} </pre>
 * <pre> {@link DbCache#setParameters(ResultSet, Object)} </pre>
 * <pre> {@link DbCache#copy(Object)} </pre>
 * <pre> {@link DbCache#recordsMatch(Object, Object) } </pre>
 * <pre> {@link DbCache#getLock() } </pre>
 * </p>
 * <p/>
 * <p>
 * Extensions should also define the {@code Comparator} objects
 * ({@code m_oRecordSort} and {@code m_oSearchSort}) to use for sorting,
 * and the query format ({@code m_sQuery}).
 * </p>
 * <p/>
 * <p>
 * Implements {@see java.lang.Runnable} so instances of {@code DbCache} can be
 * the target of {@link Thread} instances.
 * </p>
 * <p/>
 * <p>
 * Implements the ILockFactory interface to allow
 * {@code DbCache} objects to be modified in a mutually exclusive fashion
 * through the use of {@see StripeLock} containers.
 * </p>
 *
 * @param <T> Template type. Instances of {@code DbCache} must specify a
 *            concrete type at declaration time in place of type: T.
 */
public abstract class DbCache<T> implements Runnable, ILockFactory<T> {
    /**
     * Object lock to control access to {@code DbCache} critical sections.
     */
    protected static final Object g_oLock = new Object();
    private static final Logger logger = Logger.getLogger(DbCache.class);
    /**
     * Default number of locks.
     */
    protected static int DEFAULT_LOCKS = 5;
    /**
     * Default refresh timeout.
     */
    protected static int DEFAULT_REFRESH = 1200;
    /**
     * Name of the datasource.
     */
    protected String m_sDataSourceName;
    /**
     * Formatted database search query.
     */
    protected String m_sQuery;
    /**
     * Primary record container.
     */
    protected ArrayList<T> m_oRecords = new ArrayList<T>();
    /**
     * Comparator with which the primary records list {@code m_oRecords}
     * is sorted.
     */
    protected Comparator<T> m_oRecordSort;
    /**
     * Secondary search array.
     */
    protected ArrayList<T> m_oSort = new ArrayList<T>();
    /**
     * Secondary search array comparator.
     */
    protected Comparator<T> m_oSearchSort;
    /**
     * Contains copies of the shared search record, saved when no primary record
     * is found. Used for refreshing the primary records list.
     */
    protected ArrayList<T> m_oRefresh = new ArrayList<T>();
    /**
     * Pointer to {@code StripeLock} object to allow control over access of
     * critical sections of {@code DbCache}.
     */
    protected StripeLock<T> m_oLock;
    /**
     * Pointer to the {@code WDEMgr} singleton instance.
     */
    protected WDEMgr wdeMgr;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Configures and initializes {@code DbCache}, and schedules the cache
     * refresh.
     * </p>
     */
    protected DbCache() {
        Config oConfig = ConfigSvc.getInstance().getConfig(this);
        wdeMgr = WDEMgr.getInstance();

        m_sDataSourceName = oConfig.getString("datasource", null);
        m_oLock = new StripeLock<T>(this, DEFAULT_LOCKS);
        DEFAULT_REFRESH = oConfig.getInt("refresh", DEFAULT_REFRESH);

        // schedule the refresh operation
        Scheduler.getInstance().schedule(this, 0, DEFAULT_REFRESH, true);
    }

    /**
     * Updates the cache with the most current values. It also creates
     * a backup secondary records list that is sorted with the secondary
     * comparator.
     * <p/>
     * <p>
     * Connects to the datasource, and performs the query. For each record in
     * the result set it searches the primary record container for the queried
     * record, if there is not a match in the primary list it is added to
     * the refresh list. If there is a match in the list, it is removed and
     * the newer value replaces it.
     * </p>
     * <p/>
     * <p>
     * The primary list is then copied to the secondary list, and sorted with
     * the secondary comparator, keeping a copy in an order suited for
     * searching.
     * </p>
     * <p/>
     * <p>
     * Required for implementation of {@link Runnable}.
     * </p>
     */
    public void run() {
        synchronized (g_oLock) {

                DataSource iDataSource =
                        wdeMgr.getDataSource(m_sDataSourceName);
                if (iDataSource == null)
                    return;
            try(Connection iConnection = iDataSource.getConnection();
                    PreparedStatement ps = iConnection.prepareStatement(m_sQuery);
                    ResultSet rs = ps.executeQuery();
                    ) {

                // use inverse logic since the array is initialized to false
                int nIndex = m_oRecords.size();
                boolean[] bKeep = new boolean[nIndex];

                // read lock the primary array and get the record object
                T oT = m_oLock.readLock();
                while (rs.next()) {
                    // update the shared record object for each result
                    setParameters(rs, oT);

                    // search the primary array for a record match
                    nIndex = Collections.binarySearch(m_oRecords, oT, m_oRecordSort);
                    // create a copy of the shared search record and
                    // save it when no primary record is found
                    if (nIndex < 0)
                        m_oRefresh.add(copy(oT));
                    else {
                        // set the keep flag indicator when the record matches
                        if (recordsMatch(m_oRecords.get(nIndex), oT))
                            bKeep[nIndex] = true;
                        else
                            m_oRefresh.add(copy(oT));
                    }
                }
                // read unlock the primary array
                m_oLock.readUnlock();

                // obtain the write lock for the primary array
                m_oLock.writeLock();

                // remove all records with keep flags still set to false
                nIndex = bKeep.length;
                while (nIndex-- > 0) {
                    if (!bKeep[nIndex])
                        m_oRecords.remove(nIndex);
                }

                // reserve space in the primary array for the new records
                nIndex = m_oRefresh.size();
                m_oRecords.ensureCapacity(m_oRecords.size() + nIndex);

                // add saved new records to the primary array
                while (nIndex-- > 0)
                    m_oRecords.add(m_oRefresh.get(nIndex));

                // sort the primary array
                m_oRefresh.clear();
                Introsort.usort(m_oRecords, m_oRecordSort);

                // perform any necessary processing on the secondary array
                if (m_oSort != null) {
                    // clear the secondary search array
                    m_oSort.clear();

                    // copy all the records from the primary array to the secondary array
                    nIndex = m_oRecords.size();
                    m_oSort.ensureCapacity(nIndex);
                    while (nIndex-- > 0)
                        m_oSort.add(m_oRecords.get(nIndex));

                    // sort the secondary array with the secondary search comparator
                    Introsort.usort(m_oSort, m_oSearchSort);
                }

                // write unlock the primary array
                m_oLock.writeUnlock();
            } catch (Exception e) {
                e.printStackTrace();
                logger.error(e.getMessage());
            } 
        }

        logger.info("run " + Integer.toString(m_oRecords.size()));
    }

    /**
     * Performs a binary search algorithm to find the object {@code oT} in
     * the supplied list ({@code oList}). The list must be sorted in ascending
     * order according to the specified {@code Comparator}
     * ({@code iComparator}).
     *
     * @param oList       list to be searched, sorted in ascending order according
     *                    to the supplied comparator.
     * @param oT          object to search the list for.
     * @param iComparator specifies the ordering of the list.
     * @return the object retrieved from the list, or null if the object is
     * not contained in the list.
     */
    protected T search(ArrayList<T> oList, T oT, Comparator<T> iComparator) {
        int nIndex = Collections.binarySearch(oList, oT, iComparator);
        if (nIndex < 0)
            return null;

        return oList.get(nIndex);
    }

    /**
     * Wraps {@link DbCache#toString(PrintWriter, int)}. Calls this method for
     * every record in the primary records list.
     *
     * @param oPrintWriter output stream to write data to. Must be connected
     *                     beforehand.
     */
    public void toString(PrintWriter oPrintWriter) {
        for (int nIndex = 0; nIndex < m_oRecords.size(); nIndex++)
            toString(oPrintWriter, nIndex);
    }

    /**
     * Extensions must implement this method to print records list as fit.
     *
     * @param oPrintWriter output stream to write data to. Must be connected
     *                     beforehand.
     * @param nIndex       index of the object to print as a string.
     */
    protected abstract void toString(PrintWriter oPrintWriter, int nIndex);

    /**
     * Extensions must implement this method to set the parameters of the
     * {@code oT} object as fit.
     *
     * @param iResultSet database query resultant set.
     * @param oT         object whose parameters are to be set.
     */
    protected abstract void setParameters(ResultSet rs, T oT);

    /**
     * Compares the two supplied object to determine if they're equivalent.
     * <p>
     * This function compares the fields within the record but does not include
     * the search fields.
     * </p>
     *
     * @param oLhs record to compare.
     * @param oRhs record to compare.
     * @return true if the records are equivalent, false otherwise.
     */
    protected abstract boolean recordsMatch(T oLhs, T oRhs);

    /**
     * Creates and returns a copy of {@code oT}.
     *
     * @param oT the object to copy.
     * @return a newly created copy of {@code oT}.
     */
    protected abstract T copy(T oT);

    /**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@see T} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link UnitConv}
     * @see ILockFactory
     * @see StripeLock
     */
    public abstract T getLock();
}
