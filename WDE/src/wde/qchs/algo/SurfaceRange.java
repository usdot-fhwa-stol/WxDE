// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file SurfaceRange.java
 */
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.QCh;
import wde.qchs.Surface;
import wde.qchs.SurfaceRecord;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Determines whether or not the observation being tested falls within the
 * observing platform's specific climatological ranges, that vary by date to
 * account for seasonal changes.
 * <p/>
 * <p>
 * Extends {@code QCh} to provide the basic quality checking attributes and
 * methods.
 * </p>
 */
public class SurfaceRange extends QCh {
    /**
     * Calendar object used to determine month for climate range values.
     */
    private final GregorianCalendar m_oCalendar = new GregorianCalendar();

    /**
     * Pointer to the {@code Surface} cache singleton instance.
     */
    private Surface m_oSurface = Surface.getInstance();


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates instances of {@code SurfaceRange}. Initialization done through
     * base class {@code init} methods.
     * </p>
     */
    public SurfaceRange() {
    }


    /**
     * Determines whether or not the provided observation falls within the
     * gridded surface temperature range, that vary by date to account for
     * seasonal changes. Observation passes if the value is greater than the
     * minimum, and less than the maximum.
     *
     * @param nObsTypeId type of observation being tested.
     * @param iSensor    recording sensor, used to determine the platform.
     * @param iObs       observation being tested.
     * @param oResult    contains the results of the test after returning from the
     *                   method call.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        // determine the current calendar period
        // synchronized to reuse the single Gregorian calendar instance
        int nPeriod = 0;
        synchronized (m_oCalendar) {
            m_oCalendar.setTimeInMillis(iObs.getObsTimeLong());
            nPeriod = m_oCalendar.get(Calendar.MONTH);
        }

        // get the surface climate record
        SurfaceRecord oSurfaceRecord =
                m_oSurface.getSurfaceRecord(iObs.getLatitude(), iObs.getLongitude(), nPeriod);

        if (oSurfaceRecord != null) {
            oResult.setPass(oSurfaceRecord.inRange(iObs.getValue()));
            oResult.setConfidence(1.0);
            oResult.setRun();
        }
    }
}
