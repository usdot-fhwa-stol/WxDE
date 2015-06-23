// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QChSMgr.java
 */
package wde.qchs;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.ObsTypeDao;
import wde.metadata.ObsType;
import wde.obs.IObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Top level of the quality checking heirarchy. This class is registered with
 * the system to process observation sets. As <i> this </i> recieves observation
 * sets, they are passed down the line to the corresponding sequence manager
 * (same observation type), they are then passed to the correct sequence (based
 * off climate-region) for processing.
 * <p/>
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link QChSMgr#getInstance()}.
 * </p>
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of observation sets as
 * soon as they are enqueued.
 * </p>
 * <p>
 * Implements {@code ILockFactory<QChSeqMgr>} to allow mutually exclusive access
 * to quality check sequence managers through the use of {@link StripeLock}.
 * </p>
 */
public class QChSMgr extends AsyncQ<IObsSet> implements ILockFactory<QChSeqMgr> {
    private static final Logger logger = Logger.getLogger(QChSMgr.class);

    /**
     * Sets the max number of threads for processing <i> this </i> as well as
     * and the sequence managers.
     */
    private static int MAX_THREADS = 5;
    /**
     * Pointer to the singleton instance of {@code QChSMgr}.
     */
    private static QChSMgr g_oInstance = new QChSMgr();

    /**
     * List of sequence managers ordered by the observation-type they manage.
     */
    private ArrayList<QChSeqMgr> m_oSeqMgrs = new ArrayList<QChSeqMgr>();
    /**
     * Lock container for quality check sequence managers.
     */
    private StripeLock<QChSeqMgr> m_oLock;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Configures the quality checking service manager, as well as its contained
     * sequence managers. Creates the connection to the configured datasource.
     * Populates the sorted sequence managers list to manage the configured
     * default observation types. Registers <i> this </i> manager with the
     * clarus manager for processing.
     * </p>
     */
    private QChSMgr() {
        logger.info("Calling constructor");

        // apply QChSMgr configuration
        ConfigSvc oConfigSvc = ConfigSvc.getInstance();
        Config oConfig = oConfigSvc.getConfig(this);

        // increase the queue depth for more thread concurrency
        MAX_THREADS = oConfig.getInt("queuedepth", MAX_THREADS);

        setMaxThreads(MAX_THREADS);
        m_oLock = new StripeLock<QChSeqMgr>(this, MAX_THREADS);

        // set up the database connection
        Connection iConnection = null;
        WDEMgr wdeMgr = WDEMgr.getInstance();
        try {
            DataSource iDataSource =
                    wdeMgr.getDataSource(oConfig.getString("datasource", null));

            if (iDataSource == null)
                return;

            iConnection = iDataSource.getConnection();
            if (iConnection == null)
                return;

            // load the default obs types
            oConfig = oConfigSvc.getConfig("_default");
            String[] sObsTypes = oConfig.getStringArray("obstype");
            if (sObsTypes != null && sObsTypes.length > 0) {
                ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();

                // initialize the qch seq mgrs
                int nIndex = sObsTypes.length;
                while (nIndex-- > 0) {
                    // resolve the obs type name to an obs type id
                    ObsType obsType = obsTypeDao.getObsType(sObsTypes[nIndex]);

                    if (obsType != null)
                        m_oSeqMgrs.add(new QChSeqMgr(Integer.valueOf(obsType.getId()),
                                MAX_THREADS, iConnection));
                }
                Collections.sort(m_oSeqMgrs);
            }

            iConnection.close();
            wdeMgr.register(getClass().getName(), this);
        } catch (Exception e) {
            e.printStackTrace();
        }

        logger.info("Completing constructor");
    }

    /**
     * <b> Accessor </b>
     *
     * @return the singleton instance of {@code QChSMgr}.
     */
    public QChSMgr getInstance() {
        return g_oInstance;
    }

    /**
     * Finds a quality check sequence manager to handle the provided observation
     * set. The {@link QchSeqMgr#run(IObsSet)} method is then envoked on the
     * retrieved sequence manager, and provided observation set. This performs
     * quality checking algorithms on the supplied set by climate-region.
     *
     * @param iObsSet observation set to quality check.
     */
    @Override
    public void run(IObsSet iObsSet) {
        // find a seq mgr to handle the obs set
        QChSeqMgr oSeqMgr = m_oLock.readLock();
        oSeqMgr.setObsTypeId(iObsSet.getObsType());
        int nIndex = Collections.binarySearch(m_oSeqMgrs, oSeqMgr);
        m_oLock.readUnlock();

        // the seq mgr list is read-only and can be unlocked before indexing
        if (nIndex >= 0) {
            oSeqMgr = m_oSeqMgrs.get(nIndex);
            oSeqMgr.run(iObsSet);
        }

        // queue obs set for next process
        WDEMgr.getInstance().queue(iObsSet);
    }


    /**
     * Required for the implementation of the interface class
     * {@code ILockFactory}.
     * <p>
     * This is used to add a container of lockable {@link QChSeqMgr} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link QChSeqMgr}
     * @see ILockFactory
     * @see StripeLock
     */
    public QChSeqMgr getLock() {
        return new QChSeqMgr();
    }
}
