// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Barnes.java
 */
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.ModObs;
import wde.qchs.ModObsSet;
import wde.util.MathUtil;
import wde.util.threads.ILockFactory;
import wde.util.threads.StripeLock;

import java.sql.ResultSet;
import java.util.ArrayList;

/**
 * Estimates the observation value based off the location of the observation, by
 * taking a weighted average, depending on the distance from the observing
 * sensor, of all the surrounding sensors on the same platform. Then determines
 * whether the observation being checked falls within a configured standard
 * deviation of the estimated value.
 * <p>
 * Extends {@code LikeInstrument} since the Barnes algorithm depends on
 * other similar instruments in the area, on the same platform for its weighted
 * mean calculation.
 * </p>
 */
public class Barnes extends LikeInstrument {
    /**
     * Number of locks to provide for the {@code ModObsSet StripeLock}
     */
    public static final int DEFAULT_LOCKS = 5;

    /**
     * Flag that determines if the Barnes check runs independently of IQR
     */
    protected boolean m_bIgnoreIQR;

    /**
     * Weighting factor denominator
     */
    protected double m_dExp;

    /**
     * Lockable container of {@code ModObsSet} objects.
     */
    protected StripeLock<ModObsSet> m_oLock =
            new StripeLock<ModObsSet>(new ModObsSets(), DEFAULT_LOCKS);


    /**
     * Default constructor creates new instances of {@code Barnes} quality
     * checking algorithm. For initialization, use the {@code init} methods.
     *
     * @see Barnes#init(java.sql.ResultSet)
     * @see Barnes#init(int, int, int, double, int, java.sql.Connection)
     */
    public Barnes() {
    }

    protected static int sphereAdjust(int nRadius, int nLat) {
        double dRatio1 = ModObs.sphere(nLat - nRadius);
        double dRatio2 = ModObs.sphere(nLat + nRadius);
        // choose the smallest ratio to maximize the region
        if (dRatio1 > dRatio2)
            dRatio1 = dRatio2;
        return MathUtil.toMicro(MathUtil.fromMicro(nRadius) / dRatio1);
    }

    /**
     * Initializes <i> this </i> instance with data retrieved from the
     * result set. Configures the Like Instrument test, and calculates the
     * weighting factor denominator.
     * <p>
     * This is called through the base class
     * {@link Step#init(int, int, int, double, int, java.sql.Connection) }
     * method.
     * </p>
     *
     * @param iResultSet Result set from the {@link Step#QCHCONFIG_QUERY} query.
     * @throws java.lang.Exception
     */
    @Override
    protected void init(ResultSet rs) throws Exception {
        super.init(rs);

        // pre-calculate the weighting factor denominator
        m_dExp = -2.0 * m_nGeoRadiusMax / m_dSdMin * m_nGeoRadiusMax / m_dSdMin;
    }

    /**
     * Obtains a {@code ModObsSet} lock for use with the calling thread.
     *
     * @return the protected modified observation set.
     */
    protected ModObsSet readLock() {
        return m_oLock.readLock();
    }

    /**
     * Releases the lock assigned to the current thread.
     */
    protected void readUnlock() {
        m_oLock.readUnlock();
    }

    /**
     * Estimates a value at the given latitude and longitude by finding a
     * weighted average of all observations contained in the
     * provided observation set.
     *
     * @param nLat       latitude of the observation to estimate.
     * @param nLon       longitue of the observation to estimate.
     * @param oObsSet    ArrayList containing the observations for calculation.
     * @param oModObsSet locked modified observation set that will contain the
     *                   modified observations after this method returns.
     * @return the weighted mean, or NAN if there weren't enough observations
     * to form the weighted mean-value.
     */
    protected double estimateValue(int nLat, int nLon, IObs iTargetObs,
                                   ArrayList<IObs> oObsSet, ModObsSet oModObsSet) {
        oModObsSet.clear();
        oModObsSet.ensureCapacity(oObsSet.size());

        // obs inside the max radius and outside the min radius are used
        // for this comparison to avoid finding each platform explicitly
        long lOuter = m_nGeoRadiusMax;
        lOuter *= lOuter;

        // estimate the value at the obs location using a weighted mean
        double dSum = 0.0;
        double dWeights = 0.0;

        // iterate through the obs and copy the data into the modified obs
        ModObs oModObs = oModObsSet.getModObs();
        int nIndex = oObsSet.size();
        while (nIndex-- > 0) {
            IObs iObs = oObsSet.get(nIndex);
            oModObs.setObs(iObs);
            oModObs.setDstSqr(nLat, nLon);

            // include the obs when the squared distance is in the region
            // exclude the target sensor, but include platform sensors
            if (iObs.getSensorId() != iTargetObs.getSensorId() &&
                    oModObs.m_lDstSqr < lOuter) {
                double dValue = oModObsSet.modifyValue(iObs);
                double dWeight = Math.exp(oModObs.m_lDstSqr / m_dExp);

                // save the obs value and accumulate the weighted values
                oModObs.m_dValue = dValue;
                oModObsSet.add(oModObs);
                oModObs = oModObsSet.getModObs();

                // weighted mean calculations
                dSum += dValue * dWeight;
                dWeights += dWeight;
            }
        }
        // return the modified obs to the pool
        oModObsSet.putModObs(oModObs);

        if (dWeights == 0.0)
            return Double.NaN;

        return dSum / dWeights;
    }


    /**
     * Deteremines whether or not the observation provided falls within the
     * configured standard deviation of the weighted average estimate calculated
     * by the {@code Barnes#estimateValue(int, int, IObsSet, ModObsSet)} using
     * the like-instrument observations within the configured time-range, and
     * area.
     *
     * @param nObsTypeId type of observation being tested.
     * @param iSensor    observing sensor.
     * @param iObs       observation being tested.
     * @param oResult    results of the test.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        // retrieve the background field
        int nLat = iObs.getLatitude();
        int nLon = iObs.getLongitude();
        long lTimestamp = iObs.getObsTimeLong();

        ArrayList<IObs> oObsSet = new ArrayList<IObs>();

        // bounding box based on latitude to compensate for the spherical earth
        int nLonRadius = sphereAdjust(m_nGeoRadiusMax, nLat);
        m_oObsMgr.getBackground(nObsTypeId, nLat - m_nGeoRadiusMax,
                nLon - nLonRadius, nLat + m_nGeoRadiusMax,
                nLon + nLonRadius, lTimestamp + m_lTimerangeMin,
                lTimestamp + m_lTimerangeMax, oObsSet);

        ModObsSet oModObsSet = readLock();
        double dMean = estimateValue(nLat, nLon, iObs, oObsSet, oModObsSet);

        // standard deviation only has meaning with at least two other obs
        int nIndex = oModObsSet.size();
        if (nIndex < 2) {
            readUnlock();
            return;
        }

        // use the weighted mean to compute the standard deviation and
        // determine how many platforms fall within +/- 350 m elevation
        int nObsCount = 0;
        int nElevMax = iObs.getElevation() + 350;
        int nElevMin = iObs.getElevation() - 350;
        double dSum = 0.0;
        while (nIndex-- > 0) {
            ModObs oModObs = oModObsSet.get(nIndex);
            if (oModObs.m_tElev >= nElevMin && oModObs.m_tElev <= nElevMax)
                ++nObsCount;

            double dMeanDiff = oModObs.m_dValue - dMean;
            dSum += dMeanDiff * dMeanDiff;
        }

        // Barnes spatial test only abdicates when there are
        // more than 4 obs within the elevation limits
        if (nObsCount > 4 && !m_bIgnoreIQR) {
            readUnlock();
            return;
        }

        // apply Bessel's correction and finish estimating the target value
        double dSdMin = Math.sqrt(dSum / --nObsCount) * m_dSdMin;
        double dValue = oModObsSet.modifyValue(iObs);
        readUnlock();

        // indicate the test was run
        if (dValue >= dMean - dSdMin && dValue <= dMean + dSdMin) {
            oResult.setPass(true);
            oResult.setConfidence(1.0);
        }
        oResult.setRun();
    }


    /**
     * Allows {@code ModObsSet} objects to be added to a stripe lock.
     * <p>
     * Implements the {@code ILockFactory} interface to allow
     * {@see ModObsSet}'s to be modified in a mutually exclusive fashion through
     * the use of {@link StripeLock} containers.
     * </p>
     */
    private class ModObsSets implements ILockFactory<ModObsSet> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code ModObsSets}
         * </p>
         */
        public ModObsSets() {
        }


        /**
         * Required for the implementation of the interface class
         * {@code ILockFactory}.
         * <p>
         * This is used to add a container of lockable {@link ModObsSet} objects
         * to the {@link StripeLock} Mutex.
         * </p>
         *
         * @return A new instance of {@link ModObsSet}
         * @see ILockFactory
         * @see StripeLock
         */
        public ModObsSet getLock() {
            return new ModObsSet();
        }
    }
}
