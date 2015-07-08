package util.net;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import util.Log;
import util.Scheduler;
import util.Text;
import util.threads.ILockFactory;
import util.threads.StripeLock;


public class JavascriptManager
	extends HttpServlet
	implements  Runnable, ILockFactory<Object>
{
	/**
	 * Root directory for the javascript files that will be managed
	 */
	String m_sSrcDir;

	/**
	 * Output directory to store the results of requests so they can
	 * be loaded and reused for future requests
	 */
	String m_sDestDir;

	/**
	 * Location of the executable jar used to compress files
	 */
	String m_sCompressorPath;

	/**
	 * True if files should be munged
	 */
	boolean m_bMunge = true;

	/**
	 * True if files should be optimized
	 */
	boolean m_bOptimize = true;

	/**
	 * True if all semicolons should be preserved
	 */
	boolean m_bPreserveSemis = true;

	/**
	 * Column at which to add a linebreak to the output. -1 will result in
	 * no linebreaks and 0 will add a line break after every semicolon
	 */
	int m_nLineBreak = -1;

	/**
	 * Setting to true allows parameters in the querystring to override
	 * the configured munge, optimize, preserve-semis, and line-break
	 * settings.
	 */
	boolean m_bDebug = false;

	/**
	 * An arry of files that should be included at the front of every
	 * request
	 */
	File[] m_oAlwaysInclude;

	/**
	 * A map of cached requests. Once a request for a set of files has been
	 * made, future requests will simply load the previous result and
	 * return it rather than re-process the files.
	 */
	TreeMap<String, CachedRequest> m_oCachedRequests =
		new TreeMap<String, CachedRequest>();

	/**
	 * Pointer to Log instance
	 */
	Log m_oLog = Log.getInstance();

	/**
	 * Stripe lock to control access to the cache.
	 */
	StripeLock<Object> m_oCacheAccessLocks =
		new StripeLock<Object>(this, 10);


	@Override
	public void init(ServletConfig oConfig)
		throws ServletException
	{
		String sInput;
		m_sSrcDir = oConfig.getInitParameter("srcDir");
//		m_sDestDir = oConfig.getInitParameter("destDir");
		m_sCompressorPath = oConfig.getInitParameter("compressorPath");

//		if(!Text.endsWith(m_sDestDir, "/"))
//			m_sDestDir += "/";
		if(!Text.endsWith(m_sSrcDir, "/"))
			m_sSrcDir += "/";

//		sInput = oConfig.getInitParameter("alwaysInclude");
//		if(sInput != null)
//		{
//			String[] sFiles = sInput.split(":");
//			int nI = sFiles.length;
//			m_oAlwaysInclude = new File[nI];
//			while(--nI > -1)
//				m_oAlwaysInclude[nI] = new File(m_sSrcDir + sFiles[nI]);
//		}
//		else
			m_oAlwaysInclude = new File[0];

		sInput = oConfig.getInitParameter("lineBreak");
		if(sInput != null)
		{
			try
			{
				m_nLineBreak = Integer.parseInt(sInput);
			}
			catch(Exception oException)
			{
				m_oLog.write(oException);
			}
		}
		sInput = oConfig.getInitParameter("munge");
		if(sInput != null)
			m_bMunge = sInput.compareTo("1") == 0;

		sInput = oConfig.getInitParameter("optimize");
		if(sInput != null)
			m_bOptimize = sInput.compareTo("1") == 0;

		sInput = oConfig.getInitParameter("preserveSemis");
		if(sInput != null)
			m_bPreserveSemis = sInput.compareTo("1") == 0;

		sInput = oConfig.getInitParameter("debug");
		if(sInput != null)
			m_bDebug = sInput.compareTo("1") == 0;

		int nPollInterval = 3600;

		sInput = oConfig.getInitParameter("pollInterval");
		if(sInput != null)
		{
			try
			{
				nPollInterval = Integer.parseInt(sInput);
			}
			catch(Exception oException)
			{
				m_oLog.write(oException);
			}
		}

		Scheduler.getInstance().schedule(this, 0, nPollInterval);
	}


	@Override
	protected void doPost(HttpServletRequest oReq,
		HttpServletResponse oResp)
		throws ServletException, IOException
	{
		doGet(oReq, oResp);
	}

	/**
	 * Returns the time that the cached response for the set of files
	 * being requested was last updated. If a {@see CachedRequest} object
	 * has not been created for the set of files being requested,
	 * {@see System#currentTimeMillis} is returned.
	 * @param oReq
	 * @return time in millis that the requested set of files was last
	 * modified
	 */
	@Override
	protected long getLastModified(HttpServletRequest oReq)
	{
		//Lock access to the cache
		m_oCacheAccessLocks.readLock();
		String sRequestKey = oReq.getQueryString();
		//Lookup the cached request for the requested file
		CachedRequest oCachedRequest =
			m_oCachedRequests.get(sRequestKey);
		long lReturn;
		//If a cached request was found, return the last time that
		//it was updated, otherwise return current system time.
		if(oCachedRequest != null)
			lReturn = oCachedRequest.m_lLastUpdated;
		else
			lReturn = System.currentTimeMillis();
		m_oCacheAccessLocks.readUnlock();
		return lReturn;
	}



	@Override
	protected void doGet(HttpServletRequest oReq, HttpServletResponse oResp)
		throws ServletException, IOException
	{
		//Get the querystring and use it to look for a cached request
		String sRequestKey = oReq.getQueryString();
		OutputStream oResponseOut = oResp.getOutputStream();
		m_oCacheAccessLocks.readLock();
		CachedRequest oCachedRequest =
			m_oCachedRequests.get(sRequestKey);

		//If a cached request is found, write its result to the
		//response's outptu stream and return.
		if(oCachedRequest != null)
		{
			try
			{
				oResponseOut.write
					(oCachedRequest.m_yBufferedFile);
				oResponseOut.close();
				m_oCacheAccessLocks.readUnlock();
				return;
			}
			catch(Exception oException)
			{
				m_oCachedRequests.remove(sRequestKey);
			}
		}
		m_oCacheAccessLocks.readUnlock();

		//Get a write lock to edit m_oCachedRequests, and then
		//check again to make sure another thread didn't
		//cache the request between returning the readlock and
		//acquiring the write lock

		m_oCacheAccessLocks.writeLock();

		oCachedRequest =
			m_oCachedRequests.get(sRequestKey);

		if(oCachedRequest != null)
		{
			try
			{
				oResponseOut.write
					(oCachedRequest.m_yBufferedFile);
				oResponseOut.close();
				m_oCacheAccessLocks.writeUnlock();
				return;
			}
			catch(Exception oException)
			{
				m_oCachedRequests.remove(sRequestKey);
			}
		}

		//Get the lits of files to generate the request
		String sFileList = oReq.getParameter("files");
		String[] sFiles;
		if(sFileList.length() > 0)
			sFiles = sFileList.split(":");
		else
			sFiles = new String[0];


		//If debug mode wasn't configured, generate the request
		//using the configured settings. If it was, check
		//for passed parameters to override the configured settings.
		if(!m_bDebug)
			oCachedRequest = createCachedFile(sFiles, m_bMunge,
				m_bOptimize, m_bPreserveSemis,
				m_nLineBreak);
		else
		{
			//Variables to pass to the YUI compressor's command line
			boolean bMunge = m_bMunge;
			boolean bOptimize = m_bOptimize;
			boolean bPreserveSemis = m_bPreserveSemis;
			int nLineBreak = m_nLineBreak;


			String sInput = oReq.getParameter("munge");
			if(sInput != null)
				bMunge = sInput.compareTo("1") == 0;

			sInput = oReq.getParameter("optimize");
			if(sInput != null)
				bOptimize = sInput.compareTo("1") == 0;

			sInput = oReq.getParameter("preserveSemis");
			if(sInput != null)
				bPreserveSemis = sInput.compareTo("1") == 0;

			sInput = oReq.getParameter("lineBreak");
			if(sInput != null)
			{
				try
				{
					nLineBreak =
						Integer.parseInt(sInput);
				}
				catch(Exception oException){}
			}

			//Pass the command line variables and the file list
			//to be compressed
			oCachedRequest = createCachedFile(sFiles,
				bMunge, bOptimize, bPreserveSemis,
				nLineBreak);
		}

		//Add the CachedRequest to the list and print the results
		//to the response stream
		m_oCachedRequests.put(sRequestKey, oCachedRequest);
		try
		{
			oResponseOut.write(oCachedRequest.m_yBufferedFile);
			oResponseOut.close();
		}
		catch(Exception oException)
		{
			m_oLog.write(oException);
		}

		m_oCacheAccessLocks.writeUnlock();
	}


	/**
	 * Iterates through the CachedRequest objects and checks all of the
	 * component files used to create them to see if any of the files
	 * have changed. If they have, the request is regenerated.
	 */
	public void run()
	{
		m_oCacheAccessLocks.writeLock();
		try
		{
			//Get the set of map keys to iterate through
			Set<String> oKeys = m_oCachedRequests.keySet();
			Iterator<String> oItr = oKeys.iterator();

			while(oItr.hasNext())
			{
				String sKey = oItr.next();
				CachedRequest oRequest = m_oCachedRequests.get(sKey);

				//Iterate through the array of component files
				//and compare the time that the file was
				//last re-loaded and the time that the file
				//was last modified
				int nI = oRequest.m_oComponentFiles.length;
				boolean bUpdated = false;
				while(--nI > -1)
				{
					bUpdated = oRequest.m_oComponentFiles[nI].lastModified() != oRequest.m_lComponentUpdateTimes[nI];

					if(bUpdated)
					{
						//Update the last-update times
						//for the file
						//that was found,
						//as well as the rest
						//of the files in case more
						//than one was modified
						++nI;
						while(--nI > -1)
							oRequest.m_lComponentUpdateTimes[nI] = oRequest.m_oComponentFiles[nI].lastModified();
					}
				}

				//If one or more compnent-files were updated,
				//regenerate te request
				if(bUpdated)
				{
					oRequest.m_lLastUpdated = System.currentTimeMillis();
					try
					{
						m_oCachedRequests.put(sKey,
						createCachedFile(oRequest.m_oComponentFiles, oRequest.m_bMunge, oRequest.m_bOptimize, oRequest.m_bPreserveSemis, oRequest.m_nLineBreak));
					}
					catch (Exception oException)
					{
						m_oLog.write(oException);
					}
				}
			}

		}
		catch(Exception oException)
		{
			m_oLog.write(oException);
		}
		m_oCacheAccessLocks.writeUnlock();
	}


	private CachedRequest createCachedFile(String[] sComponents,
		boolean bMunge, boolean bOptimize, boolean bPreserveSemis,
		int nLineBreak)
		throws IOException
	{
		File[] oFiles = new File[sComponents.length];
		int nI = oFiles.length;
		while(--nI > -1)
			oFiles[nI] = new File(m_sSrcDir + sComponents[nI]);
		return createCachedFile(oFiles, bMunge, bOptimize,
			bPreserveSemis, nLineBreak);
	}


	private CachedRequest createCachedFile(File[] oComponents,
		boolean bMunge, boolean bOptimize, boolean bPreserveSemis,
		int nLineBreak)
		throws IOException
	{
		//Buffer to use when reading the yui compressor outputstream
		//and writing it to the internal byte array
		byte[] yBuf = new byte[1024];
		//Runtime to execute the external process
		Runtime oRuntime = Runtime.getRuntime();
		//Bytestream to capture output from the yui compressor
		ByteArrayOutputStream oOutput = new ByteArrayOutputStream();
		//String builder to setup the option flags for the  yui
		//compressor
		StringBuilder sOptions = new StringBuilder();
		if(!bMunge)
			sOptions.append(" --nomunge");
		if(!bOptimize)
			sOptions.append(" --disable-optimizations");
		if(bPreserveSemis)
			sOptions.append(" --preserve-semi");
		sOptions.append(" --line-break ");
		sOptions.append(Integer.toString(nLineBreak));
		sOptions.append(" ");

		//Create the string to exetcute the yui compressor
		String sBaseCmd = "java -jar " +
			m_sCompressorPath + sOptions.toString();

		//Add the standard files first
		for(int nFileIndex = 0; nFileIndex < m_oAlwaysInclude.length;
			++nFileIndex)
		{
			Process oCompressProcess = oRuntime.exec
			(sBaseCmd +
				m_oAlwaysInclude[nFileIndex].getAbsolutePath());

			writeInputToOutput(oCompressProcess.getInputStream(),
				oOutput, yBuf, true, false);
		}

		//Compress all the files capture the output
		for(int nFileIndex = 0; nFileIndex < oComponents.length;
			++nFileIndex)
		{
			Process p = oRuntime.exec
			(sBaseCmd +
				oComponents[nFileIndex].getAbsolutePath());

			writeInputToOutput(p.getInputStream(),
				oOutput, yBuf, true, false);
		}

		try
		{
			oOutput.close();
		}
		catch (Exception oException)
		{
			m_oLog.write(oException);
		}

		//return the new CachedRequest
	//	if(bCreateCachedRequest)
			return new CachedRequest(oOutput.toByteArray(),
				oComponents, bMunge, bOptimize,
				bPreserveSemis, nLineBreak);
	//	else
	//		return null;
	}


	/**
	 * Convenience function that writes the entire contents of an input
	 * stream to an outputstream.
	 * @param oIn InputStream to read
	 * @param oOut OutputStream to write to
	 * @param yBuf Buffer to read into and write from. If buffer is null,
	 * bytes will be read and written one at a time.
	 * @param bCloseInput True if the input stream should be closed when the
	 * function exits.
	 * @param bCloseOutput True if the output stream should be closed when
	 * the function exits.
	 * @throws IOException
	 */
	private void writeInputToOutput(InputStream oIn, OutputStream oOut,
		byte[] yBuf, boolean bCloseInput, boolean bCloseOutput)
		throws IOException
	{
		int nCount = 0;
		//Use the buffer if one was passed, otherwise read and write
		//bytes individually
		if(yBuf != null)
			while((nCount = oIn.read(yBuf)) >= 0)
				oOut.write(yBuf, 0, nCount);
		else
			while((nCount = oIn.read()) >= 0)
				oOut.write(nCount);
		//Close streams as requested.
		if(bCloseInput)
			oIn.close();
		if(bCloseOutput)
			oOut.close();
	}


	public Object getLock()
	{
		return new Object();
	}


	/**
	 * Stores the compressed result of previously requested files. The
	 * creation time, component files, last update time, and command
	 * line variables used to generate the request are also stored. The
	 * entire querystring that was parsed to generate the request is
	 * used as the search-key.
	 */
	private class CachedRequest
	{
		/**
		 * Time that the request was initially created
		 */
		long m_lCreated;

		/**
		 * Time that the request was last updated due to one of its
		 * component files being updated.
		 */
		long m_lLastUpdated;

		/**
		 * Array of files that were compressed to enerate therequest
		 */
		File[] m_oComponentFiles;

		/**
		 * Last modified times for the compenent files
		 */
		long[] m_lComponentUpdateTimes;

		/**
		 * True if the YUI compressor should munge output
		 */
		boolean m_bMunge;

		/**
		 * True if the YUI compressor should optimize output
		 */
		boolean m_bOptimize;

		/**
		 * True if the YUI compressor should improve unnecesary o
		 * semicolons
		 */
		boolean m_bPreserveSemis;

		/**
		 * Number of characters between line-breaks in the output.
		 * -1 for no linebreaks, 0 for a linebreak after each
		 * semicolon
		 */
		int m_nLineBreak;

		/**
		 * Byte array containing the output from the YUI compressor
		 */
		public byte[] m_yBufferedFile;

		/**
		 * Constructor to create a new CachedRequest object. Basic
		 * consturctor that just initializes all variables
		 * @param yBuf Output from YUI compressor to store
		 * @param oComponents Array of files that were passed to
		 * the YUI compressor to generate the byte array
		 * @param bMunge True if the output was munged
		 * @param bOptimize True if the output was optimized
		 * @param bPreserveSemis True if semicolons were preserved
		 * in the output
		 * @param nLineBreak Number of characters between linebreaks in
		 * the output,
		 */
		public CachedRequest(byte[] yBuf, File[] oComponents,
			boolean bMunge, boolean bOptimize,
			boolean bPreserveSemis, int nLineBreak)
		{
			m_yBufferedFile = yBuf;
			long lCreated = System.currentTimeMillis();
			m_lCreated = lCreated;
			m_lLastUpdated = lCreated;

			m_lComponentUpdateTimes = new long[oComponents.length];
			m_oComponentFiles = oComponents;


			int nI = oComponents.length;
			while(--nI > -1)
				m_lComponentUpdateTimes[nI] =
					oComponents[nI].lastModified();

			m_bMunge = bMunge;
			m_bOptimize = bOptimize;
			m_bPreserveSemis = bPreserveSemis;
			m_nLineBreak = nLineBreak;
		}
	}
}
