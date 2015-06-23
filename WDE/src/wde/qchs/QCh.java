// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QCh.java
 */
package wde.qchs;

import wde.dao.SensorDao;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.ObsMgr;
import wde.qchs.algo.QChResult;

import java.sql.Connection;

/**
 * Provides a standard interface for quality checking algorithms. Extensions
 * must plug an algorithm into the
 * {@link QCh#check(int, ISensor, IObs, QChResult)} method.
 * <p>
 * Container of information pertaining to how the quality check is run, and its
 * effects on the data.
 * </p>
 * <p>
 * Abstract class, all extensions should provide implementations to all
 * abstract methods, unless they too are abstract classes.
 * </p>
 * <p>
 * Implements {@code Comparable<Qch>} to allow comparisons of {@code Qch} by
 * sequence id.
 * </p>
 * <p>
 * Implements {@code ILockFactory<ModObsSet>} to allow mutually exclusive access
 * to a set of modified observations through the use of the {@link StripeLock}.
 * </p>
 */
public abstract class QCh implements Comparable<QCh> {
    /**
     * Pointer to the sensors cache.
     */
    protected SensorDao sensorDao = SensorDao.getInstance();
    /**
     * Pointer to the observation manager instance.
     */
    protected ObsMgr m_oObsMgr = ObsMgr.getInstance();
    /**
     * Interpreted as a 2-bit integer with the least-significant-bit determining
     * if a failed test can signal a stop and the most-significant-bit
     * determining if a test can override a stop signal and run anyway.
     */
    boolean m_bRunAlways;
    /**
     * Bit signaling a stop.
     */
    boolean m_bSignalStop;
    /**
     * Tracks the bit position corresponding to the qch algorithm being run.
     */
    int m_nBitPosition;
    /**
     * Used for confidence calculations, determines how much influence the
     * qch algorithm has on the results.
     */
    double m_dWeight;
    /**
     * Quality check sequence.
     */
    private int m_nSeq;

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code Qch}
     * </p>
     */
    protected QCh() {
    }


    /**
     * Initializes the member attributes with the supplied values.
     *
     * @param nSeq         sequence id.
     * @param nBitPosition bit position for the qch algorithm.
     * @param nRunAlways   a value of two or three will set the run-always flag
     *                     to signal a stop on a failed test.
     * @param dWeight      quality check influence.
     * @param nConfigId    default initialization does not load any configuration.
     * @param iConnection  default init doesn't load any configuration.
     */
    public void init(int nSeq, int nBitPosition, int nRunAlways,
                     double dWeight, int nConfigId, Connection iConnection) {
        m_nSeq = nSeq;
        m_nBitPosition = nBitPosition;
        // run always is interpreted as a 2-bit integer with the low-bit
        // determining if a failed test can signal a stop and the high-bit
        // determining if a test can override a stop signal and run anyway
        m_bRunAlways = ((nRunAlways & 2) > 0);
        m_bSignalStop = ((nRunAlways & 1) > 0);
        m_dWeight = dWeight;

        // default initialization does not load any configuration
    }


    /**
     * Abstract method should be defined for extensions.
     * <p>
     * Performs the extensions quality checking algorithm.
     * </p>
     *
     * @param nObsTypeId observation type id.
     * @param iSensor    sensor that recorded the observation.
     * @param iObs       observation.
     * @param oResult    the result of the test.
     */
    public abstract void check(int nObsTypeId, ISensor iSensor,
                               IObs iObs, QChResult oResult);


    /**
     * Enforces an ordering on {@code Qch} objects by their sequence id.
     * <p>
     * Required for the implementation of {@code Comparable}.
     * </p>
     *
     * @param oQCh object to compare to <i> this </i>.
     * @return 0 if the records match.
     */
    public int compareTo(QCh oQCh) {
        return (m_nSeq - oQCh.m_nSeq);
    }
}
