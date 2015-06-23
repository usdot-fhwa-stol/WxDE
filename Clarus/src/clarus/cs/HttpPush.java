// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file HttpPush.java
 */

package clarus.cs;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * There is one instance of this class for each upload source. The purpose of
 * this class is to allow contributors to send approved weather observation
 * files directly to the Clarus system through HTTP. Files are only saved to
 * the specified destination directory, and the upload is restricted by a
 * configurable interval between access attempts, the set of origin network
 * addresses, by a set of filenames, and the maximum number of bytes allowed
 * per file.
 */
public class HttpPush extends HttpServlet
{
    /**
     * default file size limit of 4KB.
     */
	private static int MAX_FILESIZE = 4096;

    /**
     * default 5-minute interval allowed between each file upload attempt.
     */
	private static long MIN_INTERVAL = 300000;

    /**
     * Each uploaded file is limited to the specified maximum number of bytes.
     */
	private int m_nMaxFileSize = MAX_FILESIZE;

    /**
     * The minimum amount of time that must elapse between file upload attempts.
     */
	private long m_lMinInterval = MIN_INTERVAL;

    /**
     * Intance boolean indicating the configuration has been successful.
     */
	private boolean m_bInitialized;

    /**
     * Destination directory. 4KB file containing data from the Http request.
     */
	private String m_sDest;

    /**
     * Sorted list of allowed origin network addresses.
     */
	private ArrayList<String> m_oAddresses = new ArrayList<String>();

    /**
     * Sorted list of files allowed to be sent to the destination directory.
     */
	private ArrayList<FileTiming> m_oFiles = new ArrayList<FileTiming>();

	/**
	 * File timing object used to filter the provided filename against the
	 * accepted filename list.
	 */
	private FileTiming m_oFileSearch = new FileTiming("");

	/**
     * Fills the file array with the files to output.
     */
	public HttpPush()
	{
	}


    /**
     * Overrides and masks the base class method.
     * @param oConfig configuration to initialize the servlet with.
     */
	@Override
	public void init(ServletConfig oConfig)
	{
		// get the destination directory
		m_sDest = oConfig.getInitParameter("dest-dir");

		// set up the network address filter
		String[] sValues = oConfig.getInitParameter("addresses").split(";");
		int nIndex = sValues.length;
		while (nIndex-- > 0)
			m_oAddresses.add(sValues[nIndex]);

		Collections.sort(m_oAddresses);

		// set up the filename filter
		sValues = oConfig.getInitParameter("files").split(";");
		nIndex = sValues.length;
		while (nIndex-- > 0)
			m_oFiles.add(new FileTiming(sValues[nIndex]));

		Collections.sort(m_oFiles);

		// indicate the minimum configuration parameters have been set
		m_bInitialized =
		(
			m_sDest != null && m_sDest.length() > 0 &&
			m_oAddresses.size() > 0 && m_oFiles.size() > 0
		);
		
		// append the directory marker as needed
		if (!m_sDest.endsWith("/"))
			m_sDest += "/";
		
		// set the upload timing interval and maximum file size
		String sValue = oConfig.getInitParameter("min-interval");
		if (sValue != null)
			m_lMinInterval = Long.parseLong(sValue);

		sValue = oConfig.getInitParameter("max-filesize");
		if (sValue != null)
			m_nMaxFileSize = Integer.parseInt(sValue);
	}


    /**
     * Overrides and masks the base class method.
     */
	@Override
	public void destroy()
	{
	}


	/**
	 * The base class doGet is overridden to eliminate client 405 errors
	 */
	@Override
	public void doGet(HttpServletRequest oReq, HttpServletResponse oResp)
	{
	}


    /**
     * Limits files origins to DelDOT IP addresses (beginning with 167.21.).
     * Outputs the observation files in intervals of at least five minutes in
     * 4kb chunks.
     *
     * @param oReq Input http stream. Needs to be ready for streaming.
     * @param oResp Output http stream. Needs to be ready for streaming.
     */
	@Override
	public void doPost(HttpServletRequest oReq, HttpServletResponse oResp)
    {
		// files can be uploaded only if all required initialization
		// parameters are properly configured
		if (!m_bInitialized)
			return;
		
		// posted files must originate from only the allowed addresses
		String sRemoteAddr = oReq.getRemoteAddr();
		boolean bAddressFound = false;
		int nIndex = m_oAddresses.size();
		while (!bAddressFound && nIndex-- > 0)
			bAddressFound = sRemoteAddr.startsWith(m_oAddresses.get(nIndex));

		if (!bAddressFound)
			return;

		// the destination filename must exactly match a filename in the filter
		String sFilename = oReq.getParameter("file");
		FileTiming oTiming = m_oFileSearch;
		synchronized(this)
		{
			oTiming.m_sFilename = sFilename;
			nIndex = Collections.binarySearch(m_oFiles, oTiming);
			if (nIndex < 0)
				return;
			else
				oTiming = m_oFiles.get(nIndex);
		
			// limit the interval between uploads to a minimum of 5 minutes
			long lNow = System.currentTimeMillis();
			if (lNow - oTiming.m_lTimestamp < m_lMinInterval)
				return;
			else
				oTiming.m_lTimestamp = lNow;
		}

        try
        {
			InputStream iInputStream = oReq.getInputStream();
			FileOutputStream oFileOutput =
				new FileOutputStream(m_sDest + sFilename);

			// limit the file size
			int nByte = 0;
			nIndex = m_nMaxFileSize;
			while (nIndex-- > 0 && (nByte = iInputStream.read()) >= 0)
				oFileOutput.write(nByte);

			iInputStream.close();
			oFileOutput.flush();
			oFileOutput.close();
        }
        catch (Exception oException)
        {
            oException.printStackTrace();
        }
    }


    /**
     * Associates a timestamp to a filename, and allows file comparison,
     * based off the filename.
     *
     * @see FileTiming#compareTo(clarus.cs.HttpPush.FileTiming) 
     */
	private class FileTiming implements Comparable<FileTiming>
	{
        /**
         * Timestamp associated with the File.
         */
		private long m_lTimestamp;
        /**
         * The filename member.
         */
		private String m_sFilename;

        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code FileTiming}
		 * </p>
         */
		private FileTiming()
		{
		}


        /**
         * Initializes filename.
         * @param sFilename Name to initialize filename member to.
         */
		FileTiming(String sFilename)
		{
			m_sFilename = sFilename;
		}


		/**
         * @param oFileTiming Object to compare to.
         * @return A negative integer, zero, or a positive integer as this
         * object is less than, equal to, or greater than the specified object.
         * <blockquote><pre>
         * zero means they're the same.
         * less than if this filename is lexigraphically less than the argument.
         * greater than if this is lexigraphically greater than the argument.
         * </pre></blockquote>
         *
         */
		public int compareTo(FileTiming oFileTiming)
		{
			return m_sFilename.compareTo(oFileTiming.m_sFilename);
		}
	}
}
