/**
 * @file HttpPush.java
 */

package util.net;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.Log;

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
	 * default minimum expected file size of 1 byte.
	 */
	private static int MIN_EXPECTED_FILESIZE = 1;

	/**
	 * default file size limit of 4KB.
	 */
	private static int MAX_FILESIZE = -1;

	/**
	 * default 5-minute interval allowed between each file upload attempt.
	 */
	private static long MIN_INTERVAL = 0;

	/**
	 * Each uploaded file is limited to the specified maximum number of bytes.
	 */
	private static  int m_nMaxFileSize = MAX_FILESIZE;

	/**
	 * The minimum amount of time that must elapse between file upload attempts.
	 */
	private static long m_lMinInterval = MIN_INTERVAL;

	private static int MAX_FILE_NAME_GROUPS = -1;
	private static String FILE_NAME_CONCAT_STR = "--";
	private static int MAX_DIR_COUNT = -1;
	private static boolean UNPACK_JAR_FILES = false;
	private static int MAX_JAR_ENTRIES = -1;
	private static boolean ALLOW_JAR_DIRS = true;
	private static boolean FILTER_FULL_FILEPATH = false;
	private static boolean FILTER_JAR_CONTENTS = false;
	private static boolean DETECT_MULTIPART = false;

	/**
	 * The minimum expected file size. Files under this size will still
	 * be handled, but log output will be created to note that a smaller
	 * than expected file was received.
	 */
	private int m_nMinExpectedFileSize = MIN_EXPECTED_FILESIZE;

	private int m_nMaxNumberOfFileGroups = MAX_FILE_NAME_GROUPS;

	private String m_sFilenameConcatStr = FILE_NAME_CONCAT_STR;

	private int m_nDirCount = MAX_DIR_COUNT;

	private boolean m_bUnpackJarFiles = UNPACK_JAR_FILES;

	private int m_nMaxJarEntries = MAX_JAR_ENTRIES;

	private boolean m_bAllowJarDirectoryStructures = ALLOW_JAR_DIRS;

	private boolean m_bFilterJarContents = FILTER_FULL_FILEPATH;

	private boolean m_bFilterFullFilePath = FILTER_JAR_CONTENTS;

	private boolean m_bDetectMultipart = DETECT_MULTIPART;

	private String m_sJarTempDir;


	/**
	 * Instance boolean indicating the configuration has been successful.
	 */
	private boolean m_bInitialized;

	/**
	 * Destination directory. 4KB file containing data from the HTTP request.
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

	private  ArrayList<Pattern> m_oFilePatterns = new ArrayList<Pattern>();

	/**
	 * File timing object used to filter the provided filename against the
	 * accepted filename list.
	 */
	private FileTiming m_oFileSearch = new FileTiming("", "");

	protected  Log m_oLog = Log.getInstance();

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
		String sValue;
		String[] sValues;
		int nIndex;
		// get the destination directory
		m_sDest = oConfig.getInitParameter("dest-dir");

		// set up the network address filter
		sValue = oConfig.getInitParameter("addresses");
		if(sValue != null && sValue.length() > 0)
		{
			sValues = sValue.split(";");
			nIndex = sValues.length;
			while (nIndex-- > 0)
				m_oAddresses.add(sValues[nIndex]);

			Collections.sort(m_oAddresses);
		}

		// set up the filename filter
		sValue = oConfig.getInitParameter("file-patterns");
		if(sValue != null && sValue.length() > 0)
		{
			sValues = sValue.split(";");
			nIndex = sValues.length;
			while (nIndex-- > 0)
			{
				try
				{
					m_oFilePatterns.add(Pattern.compile(sValues[nIndex]));
				}
				catch(Exception oException)
				{
					m_oLog.write(oException);
				}
			}
		}


		// append the directory marker as needed
		if (!m_sDest.endsWith("/"))
			m_sDest += "/";

		// set the upload timing interval
		sValue = oConfig.getInitParameter("min-interval");
		if (sValue != null)
			m_lMinInterval = Long.parseLong(sValue);

		// setup the maximum file size
		sValue = oConfig.getInitParameter("max-filesize");
		if (sValue != null)
			m_nMaxFileSize = Integer.parseInt(sValue) * 1024;

		// setup the minimum expected filesize
		sValue = oConfig.getInitParameter("min-filesize");
		if (sValue != null)
			m_nMinExpectedFileSize = Integer.parseInt(sValue);

		// setup the maximum number of slashes in a file name
		sValue = oConfig.getInitParameter("max-filename-groups");
		if (sValue != null)
			m_nMaxNumberOfFileGroups = Integer.parseInt(sValue);

		// setup the value substituted for "/" when concatenating
		// the groups into the filename
		sValue = oConfig.getInitParameter("filename-concat-str");
		if (sValue != null)
			m_sFilenameConcatStr = sValue;

		// setup maximum number of directories in a file path
		sValue = oConfig.getInitParameter("max-filename-dir");
		if (sValue != null)
			m_nDirCount = Integer.parseInt(sValue);

		// setup whether jar files are unpacked
		sValue = oConfig.getInitParameter("unpack-jar-files");
		if (sValue != null)
			m_bUnpackJarFiles = Integer.parseInt(sValue) == 1;

		// setup the maximum number of entries allowed in a jar file
		sValue = oConfig.getInitParameter("max-jar-entries");
		if (sValue != null)
			m_nMaxJarEntries = Integer.parseInt(sValue);

		// setup whether directories are allowed within jar files
		sValue = oConfig.getInitParameter("allow-jar-dirs");
		if (sValue != null)
			m_bAllowJarDirectoryStructures = Integer.parseInt(sValue) == 1;

		sValue = oConfig.getInitParameter("filter-jar-contents");
		if (sValue != null)
			m_bFilterJarContents = Integer.parseInt(sValue) == 1;

		sValue = oConfig.getInitParameter("filter-full-filepath");
		if (sValue != null)
			m_bFilterFullFilePath = Integer.parseInt(sValue) == 1;

		sValue = oConfig.getInitParameter("detect-multipart");
		if (sValue != null)
			m_bDetectMultipart = Integer.parseInt(sValue) == 1;

		//setup the temp directory for jar files
		m_sJarTempDir = oConfig.getInitParameter("jar-temp-dir");

		// indicate the minimum configuration parameters have been set
		m_bInitialized =
		(
			m_sDest != null && m_sDest.length() > 0 &&
			(!m_bUnpackJarFiles || m_sJarTempDir != null)
		);
		if(m_sDest != null && !m_sDest.endsWith("/"))
			m_sDest += "/";

		if(m_sJarTempDir != null && !m_sJarTempDir.endsWith("/"))
			m_sJarTempDir += "/";
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
		if (!filterBySourceIp(oReq))
		{
			m_oLog.write(this,"doPost", oReq.getRemoteAddr(), " denied.");
			return;
		}
		else if(m_oAddresses.size() == 0)
			m_oLog.write(this,"doPost", oReq.getRemoteAddr(), " accepted (no filter).");

		if(!filterByFileName(oReq))
		{
			m_oLog.write(this, "doPost", oReq.getPathInfo(), " denied");
			return;
		}
		else if(m_oFilePatterns.size() == 0)
			m_oLog.write(this,"doPost", oReq.getPathInfo(), " accepted (no filter).");

		if(!filterByInterval(oReq))
		{
			m_oLog.write(this, "doPost", oReq.getPathInfo(), " denied(interval)");
			return;
		}

		process(oReq);
	}


	protected void process(HttpServletRequest oReq)
	{

		String sFileKey = oReq.getPathInfo();
		FileTiming oTiming = m_oFileSearch;
		int nIndex;
		//Find the FileTiming object to process the request. If one
		//cannot be found, create it. Any passive filtering has already
		//occurred, so the request is assumed valid.
		synchronized(this)
		{
			oTiming.m_sFilename = sFileKey;
			nIndex = Collections.binarySearch(m_oFiles, oTiming);
			if (nIndex < 0)
			{
				String sFileName = getFileName(sFileKey);
				if(sFileName == null)
					return;
				else
				{
					oTiming = new FileTiming(sFileName,
						sFileKey);
					m_oFiles.add(~nIndex, oTiming);
				}
			}
			else
				oTiming = m_oFiles.get(nIndex);
		}

		try
		{
			//Get the input stream
			InputStream iInputStream = oReq.getInputStream();

			File oFile;

			boolean bJarFile = oTiming.m_sFilename.endsWith(".jar") && m_bUnpackJarFiles;

//			Return if jar files ar not allowed
//			if(!m_bUnpackJarFiles && bJarFile)
//				return;

			//If the file is a .jar file, it is saved to a temp
			//directory. Otherwise it is saved to the destination
			//directory.
			if(bJarFile)
				oFile = new File(m_sJarTempDir + oTiming.toString());
			else
				oFile = new File(m_sDest + oTiming.m_sFilename);
			File oParentDirectory = oFile.getParentFile();
			if(!oParentDirectory.exists())
				oParentDirectory.mkdirs();

			//Get the file's output stream and write it to disk
			FileOutputStream oFileOutput =
				new FileOutputStream(oFile);

			int nByte = -1;
			// limit the file size
			int nFileSize = 0;
			boolean bReadByMultipart = false;

			String sBoundary = oReq.getContentType();

			if(m_bDetectMultipart)
			{
				if(sBoundary.contains("multipart"))
				{
					m_oLog.write(this, "boundary", sBoundary);
					int nStart = sBoundary.indexOf("boundary");
					int nEnd = -1;
					if(nStart < 0)
						bReadByMultipart = false;
					else
					{
						nStart += 9;
						if(sBoundary.charAt(nStart) == '\"')
						{
							++nStart;
							nEnd = sBoundary.indexOf("\"", nStart);
						}
						else
							nEnd = sBoundary.length();
						sBoundary = sBoundary.substring(nStart, nEnd);

						BufferedReader oReader = new BufferedReader
							(new InputStreamReader(iInputStream));

						//read until first part starts
						String sLine;

						while(!(sLine = oReader.readLine()).contains(sBoundary));
						//read until content section of first part
						while(oReader.readLine().length() > 0);

						//use contains so that the last boundary, which adds "--",
						//will also trigger the stop
						BufferedWriter oWriter = new BufferedWriter(new OutputStreamWriter(oFileOutput));
						while((sLine = oReader.readLine()) != null && !sLine.contains(sBoundary))
						{
							oWriter.write(sLine);
							oWriter.newLine();
							if(oFile.length() > m_nMaxFileSize)
								break;
						}


						bReadByMultipart = true;
						oWriter.close();
						iInputStream.close();
						oFileOutput.close();
						if(oFile.length() > m_nMaxFileSize)
							oFile.delete();
					}

				}
				else
					bReadByMultipart = false;
			}

//			if(m_bLookForMultipart)
//			{
//				BufferedReader oReader = new BufferedReader
//					(new InputStreamReader(iInputStream));
//				String sLine = oReader.readLine();
//				if(sLine.startsWith("----------------------------"))
//				{
//					//Read out the header
//					while(sLine.length() > 0)
//						sLine = oReader.readLine();
//					bReadByMultipart = true;
//				}
//				else
//				{
//					int nI = -1;
//					int nLength = sLine.length();
//					while (++nI < nLength)
//						oFileOutput.write(sLine.charAt(nIndex));
//					bReadByMultipart = false;
//				}
//			}
//			else
//				bReadByMultipart = false;
//
			if(!bReadByMultipart)
			{
				nIndex = m_nMaxFileSize;
				if(m_nMaxFileSize >= 0)
				{
					while (nIndex-- > 0 && (nByte =
						iInputStream.read()) >= 0)
						oFileOutput.write(nByte);
					iInputStream.close();
					oFileOutput.close();
					nFileSize = m_nMaxFileSize - nIndex;
				}
				else
					nFileSize = (int)writeInputToOutput(iInputStream, oFileOutput,
						null, true, true);
			}


			//If the file was a jar file, additional processing is
			//necessary to unpack it.
			if(bJarFile)
			{
				//If the maximum file size limit was reached
				//before the jar file could be written
				//completely, just delete the temp file and
				//return
				if(nByte != -1)
				{
					oFile.delete();
					return;
				}

				//Createt a JarFile object to unpack the jar
				JarFile oJarFile =
					new JarFile(oFile.getAbsolutePath());
				File oUnpackDir =
					new File(m_sDest + oTiming.m_sFilename)
					.getParentFile();
				if(!oUnpackDir.exists())
					oUnpackDir.mkdirs();
				//Get the entries in the file
				Enumeration<JarEntry> iEntries =
					oJarFile.entries();

				//Do an initial scan of the jar file's
				//entries to see how many entries it has,
				//if any of the entres are directories,
				//and what the unpacked file size will be.
				boolean bDirectories = false;

				int nEntries = 0;
				long lUszippedSize = 0;
				boolean bAllSizesKnown = true;
				while(iEntries.hasMoreElements())
				{
					JarEntry oEntry = iEntries.nextElement();
					long lFileSize = oEntry.getSize();
					if(lFileSize == -1)
						bAllSizesKnown = false;
					else
						lUszippedSize += lFileSize;
					++nEntries;
					bDirectories = bDirectories ||
						oEntry.isDirectory();
				}
				//Return if the jar files contain directories
				//but this instance is not configured to
				//accept directories
				if(bDirectories &&
					!m_bAllowJarDirectoryStructures)
				{
					oFile.delete();
					return;
				}
				//Return if the number of entries is greater
				//than the configured limit
				if(nEntries > m_nMaxJarEntries && m_nMaxJarEntries >= 0)
				{
					oFile.delete();
					return;
				}
				//Return if the unzipped size is greater than
				//the limit
				if(lUszippedSize > m_nMaxFileSize && m_nMaxFileSize >= 0)
				{
					oFile.delete();
					return;
				}

				iEntries = oJarFile.entries();

				//If the file size was able to be determined or
				//file sizes are not being limited, just write
				//out the files. Otherwise keep track of the
				//total size as files are being written out
				//and break if the limit is reached.
				if(bAllSizesKnown || m_nMaxFileSize < 0 )
				{
					while(iEntries.hasMoreElements())
					{
						JarEntry oEntry = iEntries.nextElement();
						File oJarEntryFile = new File(oUnpackDir, oEntry.getName());
						oParentDirectory = oJarEntryFile.getParentFile();
						if(!oParentDirectory.exists())
							oParentDirectory.mkdirs();
						if(oEntry.isDirectory())
							oJarEntryFile.mkdirs();
						else
						{
							if(m_bFilterJarContents)
							{
								String sFileName = oJarEntryFile.getAbsolutePath();
								if(!filterByFileName(sFileName, sFileName))
									continue;
								if(!filterByInterval(sFileName))
									continue;
							}
							oJarFile.getInputStream(oEntry);
							writeInputToOutput(oJarFile.getInputStream(oEntry), new FileOutputStream(oJarEntryFile), null, true, true);
						}
					}
				}
				else
				{
					nByte = 0;
					// limit the file size
					nIndex = m_nMaxFileSize;

					while(iEntries.hasMoreElements() && nIndex > 0)
					{
						JarEntry oEntry = iEntries.nextElement();
						File oJarEntryFile = new File(oUnpackDir, oEntry.getName());
						oParentDirectory = oJarEntryFile.getParentFile();
						if(!oParentDirectory.exists())
							oParentDirectory.mkdirs();
						FileOutputStream oFileOutputStream = new FileOutputStream(oJarEntryFile);
						iInputStream = oJarFile.getInputStream(oEntry);
						while (nIndex-- > 0 && (nByte = iInputStream.read()) >= 0)
							oFileOutputStream.write(nByte);
						iInputStream.close();
						oFileOutputStream.close();
					}
				}
				//Delete the temporary jar file
				oFile.delete();
			}
			if(nFileSize < m_nMinExpectedFileSize
				&& m_nMinExpectedFileSize >= 0)
			{
				m_oLog.write(this, "process", "Smaller than " +
					"expected file received.");
			}
		}
		catch (Exception oException)
		{
			m_oLog.write(oException);
		}
	}


	protected boolean filterBySourceIp(HttpServletRequest oReq)
	{
		String sRemoteAddr = oReq.getRemoteAddr();
		boolean bAddressFound = false;
		int nIndex = m_oAddresses.size();
		if(nIndex == 0)
			return true;
		while (!bAddressFound && nIndex-- > 0)
			bAddressFound = sRemoteAddr.startsWith(m_oAddresses.get(nIndex));
		return false;
	}


	protected boolean filterByFileName(String sFileKey, String sFileName)
	{
		if(m_oFilePatterns.size() == 0)
			return true;
		FileTiming oTiming = m_oFileSearch;
		String sFileNameToCompare;
		if(m_bFilterFullFilePath)
			sFileNameToCompare = sFileName;
		else
			sFileNameToCompare = sFileName.substring(sFileName.lastIndexOf("[/\\]") + 1);
		synchronized(this)
		{
			oTiming.m_sSearchKey = sFileKey;
			int nIndex = Collections.binarySearch(m_oFiles, oTiming);
			if (nIndex < 0)
			{
				int nI = m_oFilePatterns.size();
				boolean bMatched = false;
				while(--nI >= 0)
					if((bMatched = m_oFilePatterns.get(nI).matcher(sFileNameToCompare).find()))
						break;

				if(bMatched)
				{
					m_oFiles.add(~nIndex,
						new FileTiming(sFileName,
						sFileKey));
					return true;

				}
				else
					return false;
			}
			else
				return true;
		}
	}


	protected boolean filterByFileName(HttpServletRequest oReq)
	{
		String sFileKey = oReq.getPathInfo();
		if (sFileKey == null)
			return false;
		return filterByFileName(sFileKey, getFileName(sFileKey));
	}


	protected boolean filterByInterval(HttpServletRequest oReq)
	{
		String sFileKey = oReq.getPathInfo();
		if (sFileKey == null)
			return false;
		else
			return filterByInterval(sFileKey);
	}


	protected boolean filterByInterval(String sFileKey)
	{

		FileTiming oTiming = m_oFileSearch;
		synchronized(this)
		{
			oTiming.m_sSearchKey = sFileKey;
			int nIndex = Collections.binarySearch(m_oFiles, oTiming);
			if (nIndex < 0)
			{
				String sFileName = getFileName(sFileKey);
				if(sFileName == null)
					return false;
				oTiming = new FileTiming(sFileName, sFileKey);
				m_oFiles.add(~nIndex, oTiming);
			}
			else
				oTiming = m_oFiles.get(nIndex);

			// limit the interval between uploads to a minimum of 5 minutes
			long lNow = System.currentTimeMillis();
			if (lNow - oTiming.m_lTimestamp < m_lMinInterval)
				return false;
			else
			{
				oTiming.m_lTimestamp = lNow;
				return true;
			}
		}
	}


	private String getFileName(String sFileKey)
	{
		if(m_nMaxNumberOfFileGroups < 0 && m_nDirCount < 0)
			return sFileKey.substring(1);
		int nSlashCount = 0;
		//start at 1 (will be incremented before use) so that the initial slash is skipped
		int nI = 0;
		StringBuilder sFilePath = new StringBuilder();
		while(++nI < sFileKey.length())
		{
			char cIn = sFileKey.charAt(nI);
			if(cIn == '/' || cIn == '\\')
			{
				if(++nSlashCount <= m_nDirCount || m_nDirCount < 0)
					sFilePath.append(cIn);
				else
					sFilePath.append(m_sFilenameConcatStr);
			}
			else
				sFilePath.append(cIn);
		}

		if(nSlashCount > m_nMaxNumberOfFileGroups && m_nMaxNumberOfFileGroups >= 0)
			return null;
		else
			return sFilePath.toString();

	}


	protected  long writeInputToOutput(InputStream oIn, OutputStream oOut,
		byte[] yBuf, boolean bCloseInput, boolean bCloseOutput)
		throws IOException
	{
		long lBytesWritten = 0;
		int nCount = 0;
		//Use the buffer if one was passed, otherwise read and write
		//bytes individually
		if(yBuf != null)
			while((nCount = oIn.read(yBuf)) >= 0)
			{
				lBytesWritten += nCount;
				oOut.write(yBuf, 0, nCount);
			}
		else
			while((nCount = oIn.read()) >= 0)
			{
				++lBytesWritten;
				oOut.write(nCount);
			}
		if(bCloseInput)
			oIn.close();
		if(bCloseOutput)
			oOut.close();
		return lBytesWritten;
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


		private String m_sSearchKey;

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
		FileTiming(String sFilename, String sSearchKey)
		{
			m_sFilename = sFilename;
			m_sSearchKey = sSearchKey;
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
			return m_sSearchKey.compareTo(oFileTiming.m_sSearchKey);
		}
	}
}
