// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file PlatformObs.java
 */
package wde.qeds;

import wde.metadata.IPlatform;
import wde.obs.IObs;
import wde.util.MathUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Wraps observations with the platform that recorded them.
 * <p/>
 * <p>
 * Implements {@code Comparator<IObs>} to enforce an ordering on observations,
 * based off sensor id.
 * </p>
 */
public class PlatformObs implements Comparator<IObs> {
    /**
     * Observations list.
     */
    final ArrayList<IObs> m_oObs = new ArrayList<IObs>();
    /**
     * Latitude for grouped observations.
     */
    int m_nLat;
    /**
     * Longitude for grouped observations.
     */
    int m_nLon;
    /**
     * Elevation for grouped observations.
     */
    int m_tElev;
    /**
     * Flag showing whether or not this {@code PlatformObs} object contains WxDE
     * observations.
     */
    boolean m_bHasWxDEObs;
    /**
     * Flag showing whether or not this {@code PlatformObs} object contains VDT
     * observations.
     */
    boolean m_bHasVDTObs;
    /**
     * Platform id.
     */
    int m_nId;
    /**
     * Platform code.
     */
    String m_sCode;
    /**
     * Platform category.
     */
    char m_cCategory;
    /**
     * Distribution group this platform belongs to.
     */
    int m_nDistGroup;
    /**
     * Timestamp corresponding to the last WxDE update.
     */
    long m_lLastWxDEUpdate;
    /**
     * Timestamp corresponding to the last VDT update.
     */
    long m_lLastVDTUpdate;
    /**
     * Latitudes for road segment definition.
     */
    ArrayList<Double> m_oLat;
    /**
     * Longitudes for road segment definition.
     */
    ArrayList<Double> m_oLon;
    /**
     * Platform corresponding to the observations.
     */
    IPlatform m_iPlatform;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code PlatformObs}
     * </p>
     */
    PlatformObs() {
    }


    /**
     * <b> Constructor </b>
     * <p>
     * New instance of {@code PlatformObs} created with this constructor will
     * have the same platform id as the provided platform, and the platform member
     * ({@code m_iPlatform}) will be a copy of the supplied platform.
     * </p>
     *
     * @param platform Object
     */
    PlatformObs(IPlatform platform) {
        this(platform,
                MathUtil.toMicro(platform.getLocBaseLat()),
                MathUtil.toMicro(platform.getLocBaseLong()),
                (int) platform.getLocBaseElev());
    }


    /**
     * <b> Constructor </b>
     * <p>
     * New instance of {@code PlatformObs} created with this constructor will
     * have the same platform id as the provided platform, and geo-coordinates
     * from mobile observations.
     * </p>
     *
     * @param platform Object
     */
    PlatformObs(IPlatform platform, int nLat, int nLon, int tElev) {
        m_nId = platform.getId();
        m_sCode = platform.getPlatformCode();
        m_cCategory = platform.getCategory();
        m_iPlatform = platform;
        m_nLat = nLat;
        m_nLon = nLon;
        m_tElev = tElev;
    }

    /**
     * @return
     */
    public IPlatform getPlatform() {
        return m_iPlatform;
    }

    /**
     * @return
     */
    public boolean hasWxDEObs() {
        return m_bHasVDTObs;
    }

    /**
     * @return
     */
    public boolean hasVDTObs() {
        return m_bHasVDTObs;
    }

    /**
     * Determines whether or not the observation list contains any observations.
     *
     * @return true if the observation list contains observations, false
     * otherwise.
     */
    synchronized boolean hasObs() {
        return (m_oObs.size() > 0);
    }

    /**
     * @param obsTypeId
     * @param sourceId
     * @return
     */
    synchronized IObs getLatestObs(int obsTypeId, int sourceId) {
        int index = m_oObs.size();
        IObs iObs = null;
        while (index-- > 0) {
            iObs = m_oObs.get(index);
            if (iObs.getObsTypeId() == obsTypeId && iObs.getSourceId() == sourceId)
                return iObs;
        }
        return null;
    }

    /**
     * Adds the supplied observation to the observation list if the observation
     * is new to the list, otherwise existing observations are replaced with
     * newer ones.
     *
     * @param iObs observation to add to the list.
     * @return true if the observation is either added to the list, or replaces
     * an older observation.
     */
    synchronized boolean addObs(IObs iObs) {
        boolean obsAdded = false;

        int nIndex = Collections.binarySearch(m_oObs, iObs, this);

        // add obs that are completely new
        if (nIndex < 0) {
            m_oObs.add(~nIndex, iObs);
            obsAdded = true;
        } else {
            if (m_cCategory == 'M') {
                IObs oldObs = m_oObs.get(nIndex);
                if (oldObs.getSensorId() == iObs.getSensorId() &&
                        oldObs.getLatitude() == iObs.getLatitude() &&
                        oldObs.getLongitude() == iObs.getLongitude()) {
                    m_oObs.set(nIndex, iObs);
                } else
                    m_oObs.add(nIndex + 1, iObs);

                obsAdded = true;
            } else {
                // replace existing obs with newer obs
                if (m_oObs.get(nIndex).getObsTimeLong() <= iObs.getObsTimeLong()) {
                    m_oObs.set(nIndex, iObs);
                    obsAdded = true;
                }
            }
        }

        return obsAdded;
    }

    /**
     * Compares the two observations by sensor id.
     *
     * @param oLhs object to compare to {@code oRhs}
     * @param oRhs object to compare to {@code oLhs}
     * @return 0 if the sensor id's match. otherwise they don't.
     */
    public int compare(IObs oLhs, IObs oRhs) {
        if (m_cCategory != 'M')
            return (oLhs.getSensorId() - oRhs.getSensorId());

        return ((int) (oLhs.getObsTimeLong() - oRhs.getObsTimeLong()));
    }
}
