// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ReplObs.java
 */
package wde.qeds;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.ObsTypeDao;
import wde.metadata.ObsType;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.IntKeyValue;
import wde.util.threads.AsyncQ;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Performs configured processing on cached observation sets, either by
 * processing them on local servers, or sending them across a configured route
 * for processing.
 * <p/>
 * <p>
 * Singleton class whose instance can be retrieved by
 * {@link ReplObs#getInstance()}
 * </p>
 * <p/>
 * <p>
 * Extends {@code AsyncQ<IObsSet>} to allow processing of observation sets
 * as they're enqueued.
 * </p>
 */
public class ReplObs extends AsyncQ<IObsSet> {
    private static final Logger logger = Logger.getLogger(ReplObs.class);

    /**
     * Singleton instance of {@code ReplObs}.
     */
    private static ReplObs g_oInstance = new ReplObs();
    /**
     * Array of configured URL routes.
     */
    ArrayList<UrlQueue> m_oUrls;
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
    private WDEMgr wdeMgr = WDEMgr.getInstance();


    /**
     * Configures this new instance of {@code ReplObs}. Initializes routing
     * based on obs type-id. Initializes which observation types to keep, and
     * which to send. Registers <i> this </i> with the WDE manager.
     */
    private ReplObs() {
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
        for (; nIndex < sUrls.length; nIndex++) {
            m_oUrls.add(new UrlQueue(sUrls[nIndex]));
        }

        // initialize network routing based on obs type id
        ObsTypeDao obsTypeDao = ObsTypeDao.getInstance();
        String[] sRoutes = oConfig.getStringArray("route");
        // obs types without a defined route get the default route
        if (sRoutes != null) {
            nIndex = sRoutes.length;
            while (nIndex-- > 0) {
                String[] sObsRoute = sRoutes[nIndex].split(",");
                // resolve the obs type name to an obs type id
                ObsType obsType = obsTypeDao.getObsType(sObsRoute[0]);
                if (obsType != null)
                    m_oRoutes.add(new IntKeyValue<UrlQueue>(Integer.valueOf(obsType.getId()),
                            m_oUrls.get(Integer.parseInt(sObsRoute[1]))));
            }
        }
        Collections.sort(m_oRoutes);

        // register this singleton with the WDE manager
        wdeMgr.register(getClass().getName(), this);

        logger.info(getClass().getName());
    }

    /**
     * Writes the provided observation set to the supplied output stream.
     *
     * @param iDataOut output stream, connected and ready to output.
     * @param iObsSet  the set of observations to print.
     * @throws java.lang.Exception
     */
    private static void writeObsSet(DataOutputStream iDataOut, IObsSet iObsSet)
            throws Exception {
        iDataOut.writeInt(iObsSet.getObsType());

        for (int nIndex = 0; nIndex < iObsSet.size(); nIndex++) {
            IObs iObs = iObsSet.get(nIndex);

            iDataOut.writeInt(iObs.getSensorId());
            iDataOut.writeLong(iObs.getObsTimeLong());
            iDataOut.writeInt(iObs.getLatitude());
            iDataOut.writeInt(iObs.getLongitude());
            iDataOut.writeInt(iObs.getElevation());
            iDataOut.writeDouble(iObs.getValue());

            // Per Bryan, this class probably won't get used
            //iDataOut.writeInt(iObs.getFlags());

            iDataOut.writeFloat(iObs.getConfValue());
        }
    }

    /**
     * <b> Accessor </b>
     *
     * @return singleton instance of {@code ReplObs}.
     */
    public ReplObs getInstance() {
        return g_oInstance;
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
    public void run(IObsSet iObsSet) {
        // first, queue the new obs set to the correct URL
        m_oSearch.setKey(iObsSet.getObsType());
        int nIndex = Collections.binarySearch(m_oRoutes, m_oSearch);
        if (nIndex < 0) {
            nIndex = ~nIndex;
            // obs without a defined route automatically go to the default route
            m_oRoutes.add(nIndex,
                    new IntKeyValue<UrlQueue>(iObsSet.getObsType(), m_oUrls.get(0)));
        }

        // attempt to send everything in the queue to the destination
        UrlQueue oQueue = m_oRoutes.get(nIndex).value();
        oQueue.add(iObsSet);
        sendObsSet(oQueue);

        wdeMgr.queue(iObsSet);
    }

    /**
     * Determines the route to send the provided observation set, if a route is
     * found a connection is established, and the observation set is written
     * across the route.
     *
     * @param iObsSet the observation set to send.
     * @return a negative value if there's no route defined for the obs-set
     * type, otherwise the observation set was sent.
     * @see ReplObs#writeObsSet(DataOutputStream, IObsSet)
     */
    private void sendObsSet(UrlQueue oQueue) {
        // don't try to send any obs sets for a while after the initial failure
        if (oQueue.m_lLastFailure > 0 &&
                System.currentTimeMillis() - oQueue.m_lLastFailure < m_lRetry)
            return;

        // reset the wait timestamp
        oQueue.m_lLastFailure = 0;
        try {
            while (oQueue.size() > 0) {
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
                while (iInputStream.read() >= 0) ;
                iInputStream.close();
            }
        } catch (Exception oException) {
            oQueue.m_lLastFailure = System.currentTimeMillis();
        }
    }

    private class UrlQueue extends ArrayDeque<IObsSet> {
        protected long m_lLastFailure;
        protected URL m_oUrl;

        protected UrlQueue(String sUrl) {
            try {
                m_oUrl = new URL(sUrl);
            } catch (Exception oException) {
                oException.printStackTrace();
            }
        }
    }
}
