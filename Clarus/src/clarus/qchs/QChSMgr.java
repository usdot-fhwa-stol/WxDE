// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QChSMgr.java
 */
package clarus.qchs;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import clarus.emc.ObsTypes;
import clarus.emc.IObsType;
import clarus.qedc.IObsSet;
import util.Config;
import util.ConfigSvc;
import util.threads.AsyncQ;
import util.threads.ILockFactory;
import util.threads.StripeLock;

/**
 * Top level of the quality checking heirarchy. This class is registered with
 * the system to process observation sets. As <i> this </i> recieves observation
 * sets, they are passed down the line to the corresponding sequence manager
 * (same observation type), they are then passed to the correct sequence (based
 * off climate-region) for processing.
 *
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
public class QChSMgr extends AsyncQ<IObsSet> implements ILockFactory<QChSeqMgr>
{
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
	 * <b> Accessor </b>
	 * @return the singleton instance of {@code QChSMgr}.
	 */
	public QChSMgr getInstance()
	{
		return g_oInstance;
	}
	

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
	private QChSMgr()
	{
		// apply QChSMgr configuration
		ConfigSvc oConfigSvc = ConfigSvc.getInstance();
		Config oConfig = oConfigSvc.getConfig(this);
		
		// increase the queue depth for more thread concurrency
		MAX_THREADS = oConfig.getInt("queuedepth", MAX_THREADS);

		setMaxThreads(MAX_THREADS);
		m_oLock = new StripeLock<QChSeqMgr>(this, MAX_THREADS);
		
		// set up the database connection
		Connection iConnection = null;
		ClarusMgr oClarusMgr = ClarusMgr.getInstance();
		try
		{
			DataSource iDataSource = 
				oClarusMgr.getDataSource(oConfig.getString("datasource", null));

			if (iDataSource == null)
				return;

			iConnection = iDataSource.getConnection();
			if (iConnection == null)
				return;
		
			// load the default obs types
			oConfig = oConfigSvc.getConfig("_default");
			String[] sObsTypes = oConfig.getStringArray("obstype");
			if (sObsTypes != null && sObsTypes.length > 0)
			{
				ObsTypes oObsTypes = ObsTypes.getInstance();

				// initialize the qch seq mgrs
				int nIndex = sObsTypes.length;
				while(nIndex-- > 0)
				{
					// resolve the obs type name to an obs type id
					IObsType iObsType = oObsTypes.
						getObsType(sObsTypes[nIndex]);

					if (iObsType != null)
						m_oSeqMgrs.add(new QChSeqMgr(iObsType.getId(), 
							MAX_THREADS, iConnection));
				}
				Collections.sort(m_oSeqMgrs);
			}		

			iConnection.close();
			oClarusMgr.register(getClass().getName(), this);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}

		System.out.println(getClass().getName());
	}
	

	/**
	 * Finds a quality check sequence manager to handle the provided observation
	 * set. The {@link QchSeqMgr#run(IObsSet)} method is then envoked on the
	 * retrieved sequence manager, and provided observation set. This performs
	 * quality checking algorithms on the supplied set by climate-region.
	 * @param iObsSet observation set to quality check.
	 */
	@Override
	public void run(IObsSet iObsSet)
	{
		// find a seq mgr to handle the obs set
		QChSeqMgr oSeqMgr = m_oLock.readLock();
		oSeqMgr.setObsTypeId(iObsSet.getObsType());
		int nIndex = Collections.binarySearch(m_oSeqMgrs, oSeqMgr);
		m_oLock.readUnlock();
		
		// the seq mgr list is read-only and can be unlocked before indexing
		if (nIndex >= 0)
		{
			oSeqMgr = m_oSeqMgrs.get(nIndex);
			oSeqMgr.run(iObsSet);
		}
		
		// queue obs set for next process
		ClarusMgr.getInstance().queue(iObsSet);
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
     *
     * @see ILockFactory
     * @see StripeLock
     */
	public QChSeqMgr getLock()
	{
		return new QChSeqMgr();
	}
}
