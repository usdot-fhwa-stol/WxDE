// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CollectorSvc.java
 */

package clarus.cs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TimerTask;
import clarus.ClarusMgr;
import clarus.qedc.IObsSet;
import clarus.qedc.ObsMgr;
import util.IntKeyValue;
import util.Scheduler;
import util.net.FtpConn;
import util.net.HttpConn;
import util.net.NetConn;
import util.net.SftpConn;

/**
 * {@code CollectorSvc} is an abstract class. All extension must implement
 * {@see CollectorSvc#createCollector}.
 *
 * <p>
 * Instances of these extensions gather observation data from contributors.
 * {@see CollectorSvc#init} must be called before collection attempts to ensure
 * proper behavior from the collection service.
 * </p>
 *
 * <p>
 * {@code CollectorSvc} implements {@see java.lang.Runnable} so instances of
 * {@code CollectorSvc} can be the target of Thread instances.
 * </p>
 */
public abstract class CollectorSvc implements Runnable
{
    /**
     * Configuration database reference used to identify a contributor data 
     * source.
     */
	protected int m_nContribId;

    /**
     * Configured number of allowed collection attempts.
     */
	protected int m_nRetryCount;

    /**
     * Counter for the number of collection retry attempts.
     */
	protected int m_nRetries;

    /**
     * Human readable name for collector service.
     */
	protected String m_sName;

    /**
     * Formatted SQL query.
     */
	protected String m_sQuery;

    /**
     * List of collectors to gather observation data.
     */
	protected final ArrayList<ICollector> m_oCollectors =
		new ArrayList<ICollector>();

    /**
     * Tracks and schedules the collection of observations, based off the
     * {@code CollectorSvc} configuration.
     */
	protected TimerTask m_oTask;

    /**
     * Initialized in {@see CollectorSvc#init} with the specified endpoint,
     * username, and password. At this point, it is ready to be connected with
     * a call to {@see NetConn#connect}.
     */
	protected NetConn m_oNetConn;

    /**
     * Used as the key value for Obs-cache Collection searches, value is
     * modified using the {@see IntKeyValue#setKey} method.
     */
	private IntKeyValue<IObsSet> m_oSearchObsSet = new IntKeyValue<IObsSet>();

    /**
     * Manages gathered observations.
     */
	private ObsMgr m_oObsMgr = ObsMgr.getInstance();

    /**
     * Two dimensional list containing lists of observation sets. Each inner
     * list is of the type {@see IntKeyValue} which has a key used for
     * searching, set by {@see IntKeyValue#setKey}.
     */
	private ArrayList<IntKeyValue<IObsSet>> m_oObsSets = 
		new ArrayList<IntKeyValue<IObsSet>>();

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code CollectorSvc}
	 * </p>
	 */
	protected CollectorSvc()
	{
	}
	
	/**
     * Initializes the {@code CollectorSvc} and sets up a network connection
     * with the supplied URL ({@code sEndpoint}), username
     * ({@code sUsername}), and password ({@code sPassword}). Collectors
     * are also created here, and added to the list of collectors for every
     * record contributor in the database. Finally, the task is scheduled, and
     * the number of collection attempt is set.
     *
     * @param nId Configuration id for collector service.
     * @param nContribId Configuration database reference used to
	 *                   identify a contributor data source
     * @param nOffset Schedule offset in seconds.
     * @param nInterval Configured connection interval in seconds.
     * @param sName Human readable name for collector service.
     * @param sEndpoint Connection URL.
     * @param sUsername Username for the connection.
     * @param sPassword Password corresponding to the username.
     * @param oCsMgr The manager to use for this connection.
     * @param iConnection Preconnected session with the SQL database.
     * @param nRetry Number of connection attempts allowed.
     *
     */
	public void init(int nId, int nContribId, int nOffset, int nInterval, 
		String sName, String sEndpoint, String sUsername, String sPassword, 
		CsMgr oCsMgr, Connection iConnection, int nRetry)
	{
		m_nContribId = nContribId;
		m_sName = sName;

		// create ftp, sftp, or http network endpoint connection
		switch (sEndpoint.charAt(0))
		{
			case 'f':
			{
				m_oNetConn = new FtpConn(sEndpoint, sUsername, sPassword);
			} 
			break;

			case 'h':
			{
				m_oNetConn = new HttpConn(sEndpoint, sUsername, sPassword);
			} 
			break;

			case 's':
			{
				m_oNetConn = new SftpConn(sEndpoint, sUsername, sPassword);
			} 
			break;
		}
		
		try
		{
			PreparedStatement iPreparedStatement = iConnection.prepareStatement(m_sQuery);
			iPreparedStatement.setInt(1, nId);
			ResultSet iResultSet = iPreparedStatement.executeQuery();
			while (iResultSet.next())
				m_oCollectors.add(createCollector(oCsMgr, iConnection, 
					iResultSet));

			iResultSet.close();
			iPreparedStatement.close();
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
		
		// configure the number of allowed collection retries
		m_nRetryCount = nInterval / nRetry;
		if (m_nRetryCount > 0)
			--m_nRetryCount;
		
		// tasks are scheduled last to ensure the object is ready
		m_oTask = Scheduler.getInstance().schedule(this, nOffset, nInterval);
	}
	
	
	/**
     * Determines whether the supplied type-id corresponds to a type monitored
     * by the Obs-cache. If it is not already in the Obs-cache, the ObsSet
     * corresponding to the supplied type-id will be inserted into the cache.
     * <p>
     * This method should be synchronized through its calling method.
     * </p>
     * @param nObsTypeId The type identifier of the ObsSet to be found.
     * @return The ObsSet corresponding to the supplied type-id.
     */
	public IObsSet getObsSet(int nObsTypeId)
	{
		m_oSearchObsSet.setKey(nObsTypeId);
		int nIndex = Collections.binarySearch(m_oObsSets, m_oSearchObsSet);
		
		if (nIndex < 0)
		{
			nIndex = ~nIndex;
			IObsSet iObsSet = m_oObsMgr.getObsSet(nObsTypeId);
			m_oObsSets.add(nIndex, new IntKeyValue<IObsSet>(nObsTypeId, iObsSet));
		}
		
		return m_oObsSets.get(nIndex).value();
	}
	
	/**
     * Calls {@see CollectorSvc#collect}. If this call isn't successful set the
     * number of collection retry attempts {@see CollectorSvc#m_nRetries}.
     * 
     * @see CollectorSvc#retry() 
     */
	public void run()
	{
		// ensure only one thread at a time can execute
		synchronized(m_oCollectors)
		{
			if (!collect())
                m_nRetries = m_nRetryCount;
		}
	}
	
	/**
     * Connects to {@see CollectorSvc#m_oNetConn}. Iterates through the
     * contributors on the connection, collecting observations, and submits
     * them for processing.
     * <p>
     * This method should be synchronized through its calling method.
     * </p>
     * @return True if the connection was successful. False otherwise.
     */
	private boolean collect()
	{
		// attempt to connect to the network resource
		boolean bConnected = m_oNetConn.connect();

		if (bConnected)
		{
			int nIndex = 0;
			// tell each collector to try to get its file
			for (; nIndex < m_oCollectors.size(); nIndex++)
			{
				// get observations and submit them for processing
				m_oCollectors.get(nIndex).collect(m_nContribId, m_oNetConn, 
					m_oTask.scheduledExecutionTime());
			}

			m_oNetConn.disconnect();
			
			// submit the collected obs sets for processing
			ClarusMgr oClarusMgr = ClarusMgr.getInstance();
			nIndex = m_oObsSets.size();
			while (nIndex-- > 0)
			{
				IntKeyValue<IObsSet> oObsSetType = m_oObsSets.remove(nIndex);
//				ClarusMgr.displayObsSet(oObsSetType.value());
				oClarusMgr.queue(oObsSetType.value());
			}
		}
		
		return bConnected;
	}
	
	/**
     * If there are more retry attempts {@see CollectorSvc#m_nRetries} this
     * method will call {@see CollectorSvc#collect} until it runs out of
     * collection attempts.
     */
	void retry()
	{
		// ensure only one thread at a time can execute
		synchronized(m_oCollectors)
		{
			// the retry count is decremented for each retry pass
			if (m_nRetries > 0)
            {
				if(collect())
                    m_nRetries = 0;
                else
                    --m_nRetries;
            }
		}
	}
	
	/**
     * Extensions of {@code CollectorSvc} must implement this method. Allows
     * specification of the necessary collector to "plug-in" to the extension of
     * CollectorSvc.
     *
     * @param oCsMgr The manager for the corresponding collector type.
     * @param iConnection The preconfigured SQL connection.
     * @param iResultSet Resultant set of the database query.
     * @return The newly created collector.
     * @throws java.lang.Exception
     */
	protected abstract ICollector createCollector(CsMgr oCsMgr,
		Connection iConnection, ResultSet iResultSet) throws Exception;
}
