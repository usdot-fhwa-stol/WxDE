// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ReplObs.java
 */
package clarus.qeds;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import clarus.ClarusMgr;
import clarus.emc.ObsTypes;
import clarus.emc.IObsType;
import clarus.qedc.IObs;
import clarus.qedc.IObsSet;
import util.Config;
import util.ConfigSvc;
import util.IntKeyValue;
import util.threads.AsyncQ;

/**
 * Performs configured processing on cached observation sets, either by
 * processing them on local servers, or sending them across a configured route
 * for processing.
 *
 * <p>
 * Singleton class whose instance can be retrieved by
 * {@link ReplObs#getInstance()}
 * </p>
 *
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of observation sets
 * as they're enqueued.
 * </p>
 */
public class ReplObs extends AsyncQ<IObsSet>
{
	/**
	 * Singleton instance of {@code ReplObs}.
	 */
	private static ReplObs g_oInstance = new ReplObs();

	/**
	 * Configured network connection timeout in ms. Defaults to 1 second.
	 */
	private int m_nTimeout = 1000;

	/**
	 * The milliseconds to wait before trying to resend obs sets after a 
	 * a network or timeout failure. Defaults to 1 minute;
	 */
	private long m_lRetry = 60000;

	/**
	 * Array of configured URL routes.
	 */
	ArrayList<UrlQueue> m_oUrls;

	/**
	 * Array of observation ids mapped to their corresponding URL routes.
	 */
	private ArrayList<IntKeyValue<UrlQueue>> m_oRoutes =
		new ArrayList<IntKeyValue<UrlQueue>>();

	/**
	 * Used to search the routes array by observation id, to retrieve the
	 * corresponding route.
	 */
	private IntKeyValue<Object> m_oSearch = new IntKeyValue<Object>();

	/**
	 * Pointer to the clarus manager singleton instance.
	 */
	private ClarusMgr m_oClarusMgr = ClarusMgr.getInstance();
	

	/**
	 * <b> Accessor </b>
	 * @return singleton instance of {@code ReplObs}.
	 */
	public ReplObs getInstance()
	{
		return g_oInstance;
	}
	

	/**
	 * Configures this new instance of {@code ReplObs}. Initializes routing
	 * based on obs type-id. Initializes which observation types to keep, and
	 * which to send. Registers <i> this </i> with the system manager.
	 */
	private ReplObs()
	{
		ConfigSvc oConfigSvc = ConfigSvc.getInstance();
		Config oConfig = oConfigSvc.getConfig(this);
		
		// update the length of time to wait for a remote connection
		m_nTimeout = oConfig.getInt("timeout", m_nTimeout);
		m_lRetry = oConfig.getLong("retry", m_lRetry);
		
		// create temporary array of URL queues
		String[] sUrls = oConfig.getStringArray("url");
		m_oUrls = new ArrayList<UrlQueue>(sUrls.length);
		// url queues are saved in definition order
		int nIndex = 0;
		for (; nIndex < sUrls.length; nIndex++)
		{
			m_oUrls.add(new UrlQueue(sUrls[nIndex]));
		}
		
		// initialize network routing based on obs type id
		ObsTypes oCacheObsTypes = ObsTypes.getInstance();
		String[] sRoutes = oConfig.getStringArray("route");
		// obs types without a defined route get the default route
		if (sRoutes != null)
		{
			nIndex = sRoutes.length;
			while (nIndex-- > 0)
			{
				String[] sObsRoute = sRoutes[nIndex].split(",");
				// resolve the obs type name to an obs type id
				IObsType iObsType = oCacheObsTypes.getObsType(sObsRoute[0]);
				if (iObsType != null)
					m_oRoutes.add(new IntKeyValue<UrlQueue>(iObsType.getId(),
						m_oUrls.get(Integer.parseInt(sObsRoute[1]))));
			}
		}
		Collections.sort(m_oRoutes);
		
		// register this singleton with the system manager
		m_oClarusMgr.register(getClass().getName(), this);

		System.out.println(getClass().getName());
	}
	

	/**
	 * Logs when the provided set of observations is not configured to be
	 * sent or kept. Otherwise, the observation set is sent across its
	 * corresponding route if configured to do so. If the set is configured
	 * to be kept, it is then enqueued into the clarus managers observation
	 * set processing queue.
	 * <p>
	 * Overrides base class run method, which is set to process on enqueue.
	 * </p>
	 *
	 * @param iObsSet the set to process.
	 */
	@Override
	public void run(IObsSet iObsSet)
	{
		// first, queue the new obs set to the correct URL
		m_oSearch.setKey(iObsSet.getObsType());
		int nIndex = Collections.binarySearch(m_oRoutes, m_oSearch);
		if (nIndex < 0)
		{
			nIndex = ~nIndex;
			// obs without a defined route automatically go to the default route
			m_oRoutes.add(nIndex, 
				new IntKeyValue<UrlQueue>(iObsSet.getObsType(), m_oUrls.get(0)));
		}
		
		// attempt to send everything in the queue to the destination
		UrlQueue oQueue = m_oRoutes.get(nIndex).value();
		oQueue.add(iObsSet);
		sendObsSet(oQueue);
		
		m_oClarusMgr.queue(iObsSet);
	}
	

	/**
	 * Determines the route to send the provided observation set, if a route is
	 * found a connection is established, and the observation set is written
	 * across the route.
	 *
	 * @param iObsSet the observation set to send.
	 * @return a negative value if there's no route defined for the obs-set 
	 * type, otherwise the observation set was sent.
	 *
	 * @see ReplObs#writeObsSet(DataOutputStream, IObsSet)
	 */
	private void sendObsSet(UrlQueue oQueue)
	{
		// don't try to send any obs sets for a while after the initial failure
		if (oQueue.m_lLastFailure > 0 &&
			System.currentTimeMillis() - oQueue.m_lLastFailure < m_lRetry)
			return;

		// reset the wait timestamp
		oQueue.m_lLastFailure = 0;
		try
		{
			while (oQueue.size() > 0)
			{
				URLConnection oConn = oQueue.m_oUrl.openConnection();

				// set the connection options
				oConn.setConnectTimeout(m_nTimeout);
				oConn.setDoOutput(true);
				oConn.setRequestProperty("Content-Type",
					"application/octet-stream");

				DataOutputStream iDataOut =
					new DataOutputStream(oConn.getOutputStream());

				// attempt to transmit the top obs set
				IObsSet iObsSet = oQueue.peek();
				writeObsSet(iDataOut, iObsSet);
				// obs sets are only removed if no exception occurs
				oQueue.pop();

				iDataOut.flush();
				iDataOut.close();

				// complete the HTTP handshake by reading any response input
				InputStream iInputStream = oConn.getInputStream();
				while (iInputStream.read() >= 0);
				iInputStream.close();
			}
		}
		catch (Exception oException)
		{
			oQueue.m_lLastFailure = System.currentTimeMillis();
		}
	}


	/**
	 * Writes the provided observation set to the supplied output stream.
	 *
	 * @param iDataOut output stream, connected and ready to output.
	 * @param iObsSet the set of observations to print.
	 * @throws java.lang.Exception
	 */
	private static void writeObsSet(DataOutputStream iDataOut, IObsSet iObsSet)
		throws Exception
	{
		iDataOut.writeInt(iObsSet.getObsType());
		
		for (int nIndex = 0; nIndex < iObsSet.size(); nIndex++)
		{
			IObs iObs = iObsSet.get(nIndex);

			iDataOut.writeInt(iObs.getSensorId());
			iDataOut.writeLong(iObs.getTimestamp());
			iDataOut.writeInt(iObs.getLat());
			iDataOut.writeInt(iObs.getLon());
			iDataOut.writeShort(iObs.getElev());
			iDataOut.writeDouble(iObs.getValue());
			iDataOut.writeInt(iObs.getRun());
			iDataOut.writeInt(iObs.getFlags());
			iDataOut.writeFloat(iObs.getConfidence());
		}
	}


	private class UrlQueue extends ArrayDeque<IObsSet>
	{
		protected long m_lLastFailure;
		protected URL m_oUrl;

		
		protected UrlQueue()
		{
		}


		protected UrlQueue(String sUrl)
		{
			try
			{
				m_oUrl = new URL(sUrl);
			}
			catch (Exception oException)
			{
				oException.printStackTrace();
			}
		}
	}
}
