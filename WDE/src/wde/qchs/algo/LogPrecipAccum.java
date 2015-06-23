// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package wde.qchs.algo;

import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.qchs.Stage24Precip;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.MathUtil;
import wde.util.threads.ThreadPool;

import java.io.FileWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.SimpleTimeZone;

/**
 *
 */
public class LogPrecipAccum extends LikeInstrument implements Runnable {
    /**
     * Configuration item that determines how many hours in the past to
     * accumulate precipitation data, i.e. 3 hour precip is 3 hours
     */
    protected int m_nHours = -1;
    /**
     * The configuration object is persisted so the hour parameters
     * can be determined upon the first check execution
     */
    protected Config m_oConfig;
    /**
     * Service for requesting precipitation background fields
     */
    protected Stage24Precip m_oPrecip = Stage24Precip.getInstance();

    private int m_nObsTypeId;
    private long m_lCurrentHour;
    private StringBuilder m_sBuffer = new StringBuilder(2000000);
    private StringBuilder m_sWriteBuffer = new StringBuilder(2000000);
    private Date m_oFileDate = new Date();
    private Date m_oObsDate = new Date();
    private DecimalFormat m_oDecimalFormat = new DecimalFormat("#0.00");
    private SimpleDateFormat m_oFilenameFormat;
    private SimpleDateFormat m_oObsDateFormat;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code PrecipAccum}
     * </p>
     */
    public LogPrecipAccum() {
        m_oConfig = ConfigSvc.getInstance().getConfig(this);
        m_lCurrentHour = System.currentTimeMillis() / 3600000;
        m_oObsDateFormat = new SimpleDateFormat("yyyyMMdd HH:mm:ss");
        m_oObsDateFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
    }


    private synchronized void writeLog(int nObsTypeId, IObs iObs,
                                       boolean bPass, double dMin, double dMax, double dTmin, double dTmax,
                                       double[][] dStage24) {
        // only set the obs type id once
        if (m_nObsTypeId == 0) {
            m_nObsTypeId = nObsTypeId;
            // generate the file name pattern
            String sPattern = "'\\\\clarus5\\subscriptions\\1000000060\\'" +
                    "yyyyMMdd_HH'00-PRECIP" + Integer.toString(nObsTypeId) + ".csv'";
            m_oFilenameFormat = new SimpleDateFormat(sPattern);
            m_oFilenameFormat.setTimeZone(new SimpleTimeZone(0, "UTC"));
        }

        // determine if it is time to write out the log information
        long lNow = System.currentTimeMillis() / 3600000;
        if (lNow > m_lCurrentHour) {
            // truncate time to the most current hour
            m_lCurrentHour = lNow;
            m_oFileDate.setTime(m_lCurrentHour * 3600000);

            // swap string builders and launch thread to write data
            StringBuilder sTempBuffer = m_sWriteBuffer;
            sTempBuffer.setLength(0);
            m_sWriteBuffer = m_sBuffer;
            m_sBuffer = sTempBuffer;
            ThreadPool.getInstance().execute(this);
        }

        m_oObsDate.setTime(iObs.getObsTimeLong());
        m_sBuffer.append(m_oObsDateFormat.format(m_oObsDate));
        m_sBuffer.append(",");
        m_sBuffer.append(nObsTypeId);
        m_sBuffer.append(",");
        m_sBuffer.append(iObs.getSensorId());
        m_sBuffer.append(",");
        m_sBuffer.append(MathUtil.fromMicro(iObs.getLatitude()));
        m_sBuffer.append(",");
        m_sBuffer.append(MathUtil.fromMicro(iObs.getLongitude()));
        m_sBuffer.append(",");
        m_sBuffer.append(iObs.getElevation());
        m_sBuffer.append(",");
        m_sBuffer.append(m_oDecimalFormat.format(iObs.getValue()));
        m_sBuffer.append(",");
        if (bPass)
            m_sBuffer.append("P");
        else
            m_sBuffer.append("N");
        m_sBuffer.append(",min ");
        m_sBuffer.append(dMin);
        m_sBuffer.append(",max ");
        m_sBuffer.append(dMax);
        m_sBuffer.append(",Tmin ");
        m_sBuffer.append(dTmin);
        m_sBuffer.append(",Tmax ");
        m_sBuffer.append(dTmax);
        for (int nRow = 0; nRow < dStage24.length; nRow++) {
            double[] dValues = dStage24[nRow];
            for (int nCol = 0; nCol < dValues.length; nCol++) {
                m_sBuffer.append(",");
                m_sBuffer.append(m_oDecimalFormat.format(dValues[nCol]));
            }
        }
        m_sBuffer.append("\n");
    }


    public void run() {
        // the file name should be stable long enough to get the data written
        // and the string builder should remain untouched for the same period
        try {
            // generate the filename with UNC directories
            String sFilename = m_oFilenameFormat.format(m_oFileDate);
            FileWriter oWriter = new FileWriter(sFilename);

            // write the buffered qch data to the file
            for (int nIndex = 0; nIndex < m_sWriteBuffer.length(); nIndex++)
                oWriter.write(m_sWriteBuffer.charAt(nIndex));

            // finish up the file
            oWriter.flush();
            oWriter.close();
        } catch (Exception oException) {
            oException.printStackTrace();
        }
    }


    /**
     * Check the precipitation accumulation with the neighbor values within
     * 50 km around the test point. This algorithm uses Stage 2 and Stage 4
     * precipitation data--known as the stage neighbors--and values are in cm.
     * More than 75% of the stage neighbors must have a value to perform this
     * check. If the test value is less than 0.1 cm, then at least 3 stage
     * neighbors must have a value of less than 1.0 cm to pass. Otherwise, the
     * standard obs type neighbors are acquired and combined with the stage
     * neighbor values to determine a valie precipiation accumulation range
     * within which the test value must fall for the check to pass.
     *
     * @param nObsTypeId observation type.
     * @param iSensor    recording sensor.
     * @param iObs       observation in question.
     * @param oResult    results of the check, after returning from this method.
     */
    @Override
    public void check(int nObsTypeId, ISensor iSensor,
                      IObs iObs, QChResult oResult) {
        // only set the number of hours to retrieve once
        if (m_nHours < 0)
            m_nHours = m_oConfig.getInt(Integer.toString(nObsTypeId), 0);

        if (m_nHours == 0)
            return;

        int nLat = iObs.getLatitude();
        int nLon = iObs.getLongitude();
        long lTimestamp = iObs.getObsTimeLong();
        // retrieve the background field in a 50km radious about the test point
        double[][] dValues = m_oPrecip.getPrecipAccum(MathUtil.fromMicro(nLat),
                MathUtil.fromMicro(nLon), 50000, m_nHours);

        if (dValues == null)
            return;

        double dMin = Double.MAX_VALUE;
        double dMax = -Double.MAX_VALUE;
        // determine the percentage of background values that have no data
        int nTotal = 0;
        int nNoData = 0;
        int n1CmCount = 0;
        for (int nY = 0; nY < dValues.length; nY++) {
            for (int nX = 0; nX < dValues[nY].length; nX++) {
                ++nTotal;
                double dValue = dValues[nY][nX];
                if (Double.isNaN(dValue))
                    ++nNoData;
                else {
                    // count how many stage neighbors are 1 cm or less
                    if (dValue <= 1.0)
                        ++n1CmCount;

                    // determine the min and max stage values
                    if (dValue > dMax)
                        dMax = dValue;

                    if (dValue < dMin)
                        dMin = dValue;
                }
            }
        }

        // the algorithm requires more than 3/4 valid background field values
        if (nTotal < nNoData * 4)
            return;

        double dTmin = 0.0;
        double dTmax = 0.0;
        // precipitation values are in cm
        double dValue = iObs.getValue();
        if (dValue > 0.1) {
            // retrieve a background field for the current obs type
            ArrayList<IObs> oObsSet = new ArrayList<IObs>();
            m_oObsMgr.getBackground(nObsTypeId, nLat - m_nGeoRadiusMax,
                    nLon - m_nGeoRadiusMax, nLat + m_nGeoRadiusMax,
                    nLon + m_nGeoRadiusMax, lTimestamp + m_lTimerangeMin,
                    lTimestamp + m_lTimerangeMax, oObsSet);

            // update the min and max neighbor values
            int nIndex = oObsSet.size();
            while (nIndex-- > 0) {
                double dNeighborValue = oObsSet.get(nIndex).getValue();
                if (dNeighborValue > dMax)
                    dMax = dNeighborValue;

                if (dNeighborValue < dMin)
                    dMin = dNeighborValue;
            }

            // create the comparison range
            dTmin = dMin - 0.5;
            dTmax = dMax * 2.0;

            if (dTmax < 0.8)
                dTmax = 0.8;
            if (dTmax > 5.0)
                dTmax = dMax + 2.5;

            // the test passes if the target value is within the range
            oResult.setPass(dTmin < dValue && dValue < dTmax);
        } else {
            // at least 3 stage neighbors must be <= 10mm to pass
            oResult.setPass(n1CmCount > 2);
        }

        if (oResult.getPass())
            oResult.setConfidence(1.0);
        oResult.setRun();

        // write log entry for algorithm evaluation
        writeLog(nObsTypeId, iObs, oResult.getPass(),
                dMin, dMax, dTmin, dTmax, dValues);
    }
}
