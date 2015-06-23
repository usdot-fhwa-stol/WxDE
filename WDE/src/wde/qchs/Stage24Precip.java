/**
 * Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
 * <p/>
 * Author: 	n/a
 * Date: 	n/a
 * <p/>
 * Modification History:
 * dd-Mmm-yyyy		iii		[Bug #]
 * Change description.
 * <p/>
 * 05-Jul-2012		das
 * Added a null check for NCD file within the run method.
 */
package wde.qchs;

import org.apache.log4j.Logger;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.nc2.NetcdfFile;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Scheduler;
import wde.util.net.FtpConn;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 *
 */
public class Stage24Precip implements Runnable {
    private static final Logger logger = Logger.getLogger(Stage24Precip.class);

    private static final int WIDTH = 1121;
    private static final int HEIGHT = 881;
    private static final int HOURS = 24;

    private static final Stage24Precip g_oInstance = new Stage24Precip();

    private int m_nOffset;
    private int[] m_nLimitX = new int[2];
    private int[] m_nLimitY = new int[2];
    // these arrays will use approximately 400MB of memory
    private double[][][] m_dValues = new double[HOURS][HEIGHT][WIDTH];
    private double[][][] m_dScratch = new double[HOURS][HEIGHT][WIDTH];
    private String m_sUsername;
    private String m_sPassword;
    private String m_sUrl;
    private String m_sDir;
    private SimpleDateFormat[] m_oFormats;
    private MapProjection m_oMapProj;


    private Stage24Precip() {
        Config oConfig = ConfigSvc.getInstance().getConfig(this);
        m_sUsername = oConfig.getString("username", "anonymous");
        m_sPassword = oConfig.getString("password", "clarus@mixonhill.com");
        m_sUrl = oConfig.getString("url", "");
        m_sDir = oConfig.getString("directory", "grib");
        if (!m_sDir.endsWith("/"))
            m_sDir += "/";

        String[] sFilenames = oConfig.getStringArray("file");
        m_oFormats = new SimpleDateFormat[sFilenames.length];
        for (int nIndex = 0; nIndex < sFilenames.length; nIndex++)
            m_oFormats[nIndex] = new SimpleDateFormat(sFilenames[nIndex]);

        m_oMapProj = new MapProjection(6367470.0, 1.07179676972, 90.0, -105.0,
                -1901416.7798861882, -7613145.491528835, 4763.0, 4763.0);

        // the default schedule is hourly, but
        // files are written around :35, so wait a few more minutes
        m_nOffset = oConfig.getInt("offset", 38);
        Scheduler.getInstance().schedule(this,
                m_nOffset * 60, oConfig.getInt("period", 60) * 60, true);

        // initialize the cached dataset
        run();
    }

    public static Stage24Precip getInstance() {
        return g_oInstance;
    }

    // create a 3d matrix of NaN values
    private static void initArray(double[][][] dValues) {
        int nIndexZ = dValues.length;
        while (nIndexZ-- > 0) {
            int nIndexY = dValues[nIndexZ].length;
            while (nIndexY-- > 0) {
                int nIndexX = dValues[nIndexZ][nIndexY].length;
                while (nIndexX-- > 0)
                    dValues[nIndexZ][nIndexY][nIndexX] = Double.NaN;
            }
        }
    }

    public void run() {
        // adjust the time back one hour if the file update has not happened
        Calendar oNow = new GregorianCalendar();
        // adjust the current time from the local time zone to UTC
        long lNow = oNow.getTimeInMillis();
        lNow -= oNow.getTimeZone().getOffset(lNow);
        if (oNow.get(Calendar.MINUTE) < m_nOffset)
            lNow -= 3600000;

        Date oDate = new Date(lNow);
        FtpConn oFtpConn = new FtpConn(m_sUrl, m_sUsername, m_sPassword);

        try {
            // skip the operations if the site cannot be contacted
            if (!oFtpConn.connect()) {
                try {
                    oFtpConn.close();
                } catch (IOException ioe) {
                    // swallow the exception
                }
                return;
            }

            // ensure the destination directory exists
            File oDir = new File(m_sDir);
            if (!oDir.exists())
                oDir.createNewFile();

            // an array with 3D indicies used to read the source data
            // the meaning of the array positions is time, y index, x index
            int[] nDims = new int[3];
            // the time index is always set to zero
            nDims[0] = 0;
            initArray(m_dScratch);
            for (int nIndex = 0; nIndex < m_oFormats.length; nIndex++) {
                int nHour = HOURS;
                // collect the previous 24, 1-hour files of precipitation data
                long lTime = lNow - (HOURS * 3600000);
                while (nHour-- > 0) {
                    // update the timestamp used to generate the source filename
                    oDate.setTime(lTime);
                    lTime += 3600000;

                    // verify that the requested file is available
                    String sSrcFile = m_oFormats[nIndex].format(oDate);
                    if (!oFtpConn.open(sSrcFile))
                        continue;

                    // save the network file to the local working directory
                    String sDestFile =
                            m_sDir + sSrcFile.substring(sSrcFile.lastIndexOf("/"));
                    FileOutputStream oDestFile = new FileOutputStream(sDestFile);
                    int nByte = 0;
                    while ((nByte = oFtpConn.read()) >= 0)
                        oDestFile.write(nByte);

                    // finish writing file contents and close the network file
                    oDestFile.flush();
                    oDestFile.close();
                    oFtpConn.close();

                    // open the precipitation file
                    NetcdfFile oNetCdf = NetcdfFile.open(sDestFile);
                    // iterate through each file and replace previous data with
                    // data from subsequent files, which should be more accurate

                    // if the file was not found then continue.
                    if (oNetCdf == null)
                        continue;

                    Array oArray = oNetCdf.getRootGroup().
                            findVariable("Total_precipitation").read();
                    Index oIndex = oArray.getIndex();

                    // copy the source data into the scratch work array
                    for (int nY = 0; nY < HEIGHT; nY++) {
                        nDims[1] = nY;
                        for (int nX = 0; nX < WIDTH; nX++) {
                            nDims[2] = nX;
                            oIndex.set(nDims);
                            double dValue = oArray.getDouble(oIndex);
                            // only positive values have any meaning
                            if (dValue >= 0.0)
                                m_dScratch[nHour][nY][nX] = dValue;
                        }
                    }
                    oNetCdf.close();
                }
            }

            // swap the new data array for the old data array
            synchronized (this) {
                double[][][] dTemp = m_dValues;
                m_dValues = m_dScratch;
                m_dScratch = dTemp;
            }

            // clean up the working directory
            File[] oFiles = new File(m_sDir).listFiles();
            for (int nIndex = 0; nIndex < oFiles.length; nIndex++)
                oFiles[nIndex].delete();
        } catch (Exception oException) {
            oException.printStackTrace();
            logger.error(oException.getMessage());
        }

        oFtpConn.disconnect();
    }

    // iterate through the hourly data and sum the individual cells about the
    // center point to generate precipitation accumulation at each location
    // ignoring any of the NaN values where no data are available
    public synchronized double[][] getPrecipAccum(double dLat, double dLon,
                                                  double dRadius, int nHours) {
        int nWidth = 0;
        int nHeight = 0;
        double[][] dValues = null;
        try {
            m_oMapProj.getBounds(m_nLimitX, m_nLimitY, dLat, dLon, dRadius);

            // correct for indicies outside array limits
            if (m_nLimitX[0] < 0)
                m_nLimitX[0] = 0;
            if (m_nLimitX[1] > WIDTH)
                m_nLimitX[1] = WIDTH;

            if (m_nLimitY[0] < 0)
                m_nLimitY[0] = 0;
            if (m_nLimitY[0] > HEIGHT)
                m_nLimitY[0] = HEIGHT;

            nWidth = m_nLimitX[1] - m_nLimitX[0];
            nHeight = m_nLimitY[1] - m_nLimitY[0];

            // verify that there is a range of values to store
            if (nWidth <= 0 || nWidth > WIDTH || nHeight <= 0 || nHeight > HEIGHT) {
                logger.debug("getPrecipAccum for dLat: " + Double.toString(dLat) + " dLon: " + Double.toString(dLon)
                        + " " + Integer.toString(m_nLimitX[0])
                        + " " + Integer.toString(m_nLimitX[1])
                        + " " + Integer.toString(m_nLimitY[0])
                        + " " + Integer.toString(m_nLimitY[1])
                        + " " + Integer.toString(nWidth)
                        + " " + Integer.toString(nHeight));

                return null;
            }

            // the compiler should zero initialize the array
            dValues = new double[nWidth][nHeight];

            // calculate within each hourly column for every xy cell
            int nIndexX = nWidth;
            while (nIndexX-- > 0) {
                // translate to the source index
                int nX = nIndexX + m_nLimitX[0];
                int nIndexY = nHeight;
                while (nIndexY-- > 0) {
                    // translate to the source index
                    int nY = nIndexY + m_nLimitY[0];
                    int nIndexH = nHours;
                    while (nIndexH-- > 0) {
                        if (nIndexH < 0 || nIndexH >= m_dValues.length ||
                                nY < 0 || nY >= m_dValues[nIndexH].length ||
                                nX < 0 || nX >= m_dValues[nIndexH][nY].length) {
                            logger.debug("getPrecipAccum dLat: " + Double.toString(dLat)
                                    + " dLon: " + Double.toString(dLon)
                                    + " nH: " + Integer.toString(nHeight)
                                    + " nY: " + Integer.toString(nY)
                                    + " nX: " + Integer.toString(nX));

                            return null;
                        }

                        double dValue = m_dValues[nIndexH][nY][nX];
                        // when any value in the hourly column is NaN (no value),
                        // no more summation is performed and the final sum is NaN
                        if (nIndexX < 0 || nIndexX >= nWidth || nIndexY < 0 || nIndexY >= nHeight)
                            logger.debug("***nWidth: " + nWidth + " nHeight: " + nHeight + " nIndexX: " + nIndexX + " nIndexY: " + nIndexY);

                        if (Double.isNaN(dValue) ||
                                Double.isNaN(dValues[nIndexX][nIndexY])) {
                            dValues[nIndexX][nIndexY] = Double.NaN;
                        } else
                            dValues[nIndexX][nIndexY] += dValue;
                    }
                }
            }
        } catch (Exception oException) {
            oException.printStackTrace();
            logger.error(oException);
        }

        return dValues;
    }


//	public static void main(String[] sArgs)
//		throws Exception
//	{
//		MapProjection oMapProj = new MapProjection(6367470.0, 1.07179676972,
//			90.0, -105.0, -1901416.7798861882, -7613145.491528835, 4763.0, 4763.0);
//
//		int[] nLimitX = new int[2];
//		int[] nLimitY = new int[2];
//		oMapProj.getBounds(nLimitX, nLimitY, 38.893, -94.668, 50000);
//
//		System.out.println(nLimitX[0]);
//		System.out.println(nLimitX[1]);
//		System.out.println(nLimitY[0]);
//		System.out.println(nLimitY[1]);
//
//		Stage24Precip.getInstance();
//		String sFilename = "C:/Users/bryan.krueger/Desktop/ST2un2010020219.Grb.Z";
//		String sFilename = "C:/Users/bryan.krueger/Desktop/ST2un2008010100.Grb.Z";
//		String sFilename = "C:/Users/bryan.krueger/Desktop/rucs.t15z.g88anl.grib2";
//		NetcdfFile oNetCdf = NetcdfFile.open(sFilename);
//		oNetCdf.close();
//		oNetCdf = null;
//	}
}
