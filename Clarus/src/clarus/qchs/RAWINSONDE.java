// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file RAWINDSONDE.java
 */
package clarus.qchs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import clarus.emc.Stations;
import clarus.qchs.algo.HeightTemp;
import clarus.qchs.RAWINSONDE.CachedLatLon;
import clarus.qedc.IObs;
import util.Config;
import util.ConfigSvc;
import util.Scheduler;
import util.Text;
import util.io.CharTokenizer;
import util.net.FtpConn;
import util.threads.ILockFactory;
import util.threads.StripeLock;


/**
 * The RAWINSONDE class implements the Singleton pattern. It is used to maintain
 * a current list of temperature and pressure readings from the National
 * Weather Service balloon soundings along with the originating station data.
 * The information is restored from a local text file on startup and new data
 * are loaded at regular intervals from a NWS ftp server. The local file is
 * updated when new data is collected.
 *
 * @author scot.lange
 */
public class RAWINSONDE implements Runnable, ILockFactory<CachedLatLon>
{	
	private static RAWINSONDE g_oInstance = new RAWINSONDE();

	private static String[] MONTHS =
	{
		" Jan ", " Feb ", " Mar ", " Apr ", " May ", " Jun ",
		" Jul ", " Aug ", " Sep ", " Oct ", " Nov ", " Dec "
	};
	private ArrayList<SoundingRecord> m_oSoundingRecordList =
			new ArrayList<SoundingRecord>();
	private ArrayList<SoundingFile> m_oSoundingFileList =
			new ArrayList<SoundingFile>();
	private ArrayList<CachedLatLon> m_oLatLonCache =
			new ArrayList<CachedLatLon>();
	private ArrayList<ReanalysisRecord> m_oReanalysis =
		new ArrayList<ReanalysisRecord>();
	private SoundingFile m_oSearchSoundingFile = new SoundingFile("", 0);
	private SoundingRecord m_oSearchSoundingRecord = new SoundingRecord();
	private StripeLock<CachedLatLon> m_oCachedLatLonLock = 
			new StripeLock<CachedLatLon>(this, 5);
	private ReanalysisRecord m_oReanalysisSearch = new ReanalysisRecord();
	private final GregorianCalendar m_oCalendar = new GregorianCalendar();
	
	///FTP Values
	private FtpConn m_oFtpConn;
	private String m_sFtpServer;
    private String m_sDataPath;
	private String m_sFtpDirectory;
	private String m_sDirectoryListingFile;


	/**
	 * @return the current instance of the RAWINSONDE class
	 */
	public static RAWINSONDE getInstance()
	{
		return g_oInstance;
	}

	
	/**
	 * The constructor is only run when the system first starts. It will first
	 * load the station list from a file and get the current list of sounding
	 * files from the ftp server. Depending on how long it will be until the
	 * scheduler will update the station list with new readings, it may or may
	 * not perform an initial update.
	 */
	private RAWINSONDE()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);

		// the reanalaysis data changes rarely and can be loaded once
		String sReanalysisPath = oConfig.getString("reanalysisFile", null);
		try
		{
			BufferedReader oReanalysis = new BufferedReader(
				new InputStreamReader(new FileInputStream(sReanalysisPath)));

			// throw away the header row
			String sLine = oReanalysis.readLine();
			while ((sLine = oReanalysis.readLine()) != null)
				m_oReanalysis.add(new ReanalysisRecord(sLine));

			oReanalysis.close();
			Collections.sort(m_oReanalysis);
		}
		catch (Exception oException)
		{
		}

		m_sDataPath = oConfig.getString("csvFile", null);
		m_sFtpDirectory = oConfig.getString("dataDirectory", null);
		m_sDirectoryListingFile = oConfig.getString("directoryListing", null);
		m_sFtpServer = oConfig.getString("ftpServer", null);
		restore();
		if (m_sDirectoryListingFile != null && m_sDataPath != null
				&& 	m_sFtpDirectory != null && m_sFtpServer != null)
		{
			m_oFtpConn = new FtpConn(m_sFtpServer,
					oConfig.getString("ftpUsername", "Anonymous"),
					oConfig.getString("ftpPassword", "Clarus@mixon-hill.com"));			
		}
		else
			m_oFtpConn = null;

		run();
		Scheduler.getInstance().schedule(this, 
			oConfig.getInt("schedulerOffset", 47),
			oConfig.getInt("schedulerPeriod", 900));
	}	


	/**
	 * Initializes the station list using values loaded from a CSV file.
	 * This is only used when the system is first started up.
	 */
	private void restore()
	{
		if (m_sDataPath == null || m_sDataPath.length() == 0)
			return;		
		StringBuilder sTokenBuffer = new StringBuilder();
		// read each line of the input file as one token
		CharTokenizer oCharTokenizer = new CharTokenizer("\n");		
		try
		{
			// open the input file
			oCharTokenizer.setInput(new FileInputStream(m_sDataPath));
			while (oCharTokenizer.nextSet())
			{
				while (oCharTokenizer.hasTokens())
				{
					oCharTokenizer.nextToken(sTokenBuffer);
					// create the sounding records from the split input
					// the columns have to be converted to strings anyway
					String[] sCols = sTokenBuffer.toString().split(",");
					m_oSoundingRecordList.add(new SoundingRecord(sCols));
				}
			}
			Collections.sort(m_oSoundingRecordList);
		}
		catch (Exception oException)
		{//Failed to load
		}
	}


	private void save()
	{
		try
		{
			FileWriter oFileWriter = new FileWriter(m_sDataPath);
			int nIndex = m_oSoundingRecordList.size();
			while (nIndex-- > 0)
			{
				oFileWriter.write(m_oSoundingRecordList.get(nIndex).toString());
				oFileWriter.write("\n");
			}
			oFileWriter.close();
		}
		catch (Exception oException)
		{//failed to save file
		}
	}


	/**
	 * getcurrentFileList() reads a file containing a directory listing from
	 * the National Weather Service FTP site.  As files are pulled out of the
	 * directory listing, their timestamp is used to determine if the file has
	 * been updated since the last time readings were taken from it.  If it
	 * has been updated, or if it is a new file that is not currently in the
	 * list of SoundingFiles, it is flagged to be updated.
	 */
	public void getCurrentFileList()  
	{
		if (!m_oFtpConn.open(m_sFtpDirectory + m_sDirectoryListingFile ))
			//Couldn't open the directory listing.
			return;
		// set up the buffered reader
		BufferedReader oReader = 
			new BufferedReader(new InputStreamReader(m_oFtpConn));

		Calendar oCalendar = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		int nCurrentMonth = oCalendar.get(Calendar.MONTH);
		int nCurrentYear = oCalendar.get(Calendar.YEAR);
		
		//Get the oldest reading, so that on startup we can filter out some
		//files that don't need to be updated, instead of just updating them all
		long lOldestReading = 0;
		int nIndex = m_oSoundingRecordList.size();
		while (nIndex-- > 0)
			if (m_oSoundingRecordList.get(nIndex).m_lTimeStamp > lOldestReading)
 				lOldestReading = m_oSoundingRecordList.get(nIndex).m_lTimeStamp;

		//Declare a SimpleDateFormat object to parse the file's timestamp
		SimpleDateFormat Format = new SimpleDateFormat("MMM dd HH:mm");
		Format.setLenient(true);
		try
		{
			long lTimeStamp = 0;
			String sLine = null;
			while ((sLine = oReader.readLine()) != null)
			{
				// month names are surrounded by spaces to eliminate the month
				// being used as a user or group owner or as part of a filename
				int nDateIndex = -1;
				nIndex = MONTHS.length;
				while (nDateIndex < 0 && nIndex-- > 0)
					nDateIndex = sLine.lastIndexOf(MONTHS[nIndex]);

				// skip the rest of the operations if no timestamp is found
				if (nDateIndex < 0)
					continue;

				// read the timestamp
				try
				{
					oCalendar.setTime(Format.parse(
						sLine.substring(nDateIndex, nDateIndex + 14).trim()));
					oCalendar.set(Calendar.YEAR, nCurrentYear);
					//The timestamp does not include a year, so it
					//may need to be adjusted back a year
					if (oCalendar.get(Calendar.MONTH) > nCurrentMonth)
						oCalendar.add(Calendar.YEAR, -1);
					//Store the timestamp as a long
					lTimeStamp = oCalendar.getTimeInMillis();
				}
				catch (Exception oException)
				{
					lTimeStamp = 0;
				}

				// read the filename and set up the search object
				m_oSearchSoundingFile.m_lTimeStamp = lTimeStamp;
				m_oSearchSoundingFile.m_sFileName =
					sLine.substring(nDateIndex + 14);

				//Attempt to lookup the SoundingFile in our list
				nIndex = 0;
				nIndex = Collections.binarySearch(
						m_oSoundingFileList, m_oSearchSoundingFile);
				//If it is found, check if we need to update it.
				if (nIndex >= 0)
				{
					SoundingFile oUpdateSoundingFile =
							m_oSoundingFileList.get(nIndex);
					if (m_oSearchSoundingFile.isNewerThanFile(
							oUpdateSoundingFile))
					{
						oUpdateSoundingFile.m_bUpdate = true;
						oUpdateSoundingFile.m_lTimeStamp =
							m_oSearchSoundingFile.m_lTimeStamp;
					}
				}
				//If it wasn't found, insert it.
				else
				{
					m_oSearchSoundingFile.m_bUpdate =
							(m_oSearchSoundingFile.m_lTimeStamp
							> lOldestReading);
					m_oSoundingFileList.add(~nIndex,
						new SoundingFile(m_oSearchSoundingFile));
				}
			}
			oReader.close();
		}
		catch (Exception oException)
		{// Error on a CharTokenizer.NextSet() call.  List may not have		
		}// been fully updated.
	}


	/**
	 * updateFromFtp() goes through the list of files and gets readings
	 * from files that are flagged for update
	 */
	private void updateFromFtp()
	{
		CharTokenizer oCharTokenizer = new CharTokenizer(" ", "\n");
		StringBuilder sCurrentToken = new StringBuilder();
		//Setup the state machine
		int nState = 1;
		int nTokensToSkip = 0;
		try
		{
			//Loop through the files, updating any that are flagged
			int nIndex = m_oSoundingFileList.size();
			while (nIndex-- > 0)
			{
				//Get the current file
				SoundingFile oUpdateFile = m_oSoundingFileList.get(nIndex);
				//If it doesn't need to be updated, continue
				if (!oUpdateFile.m_bUpdate)
					continue;
				//Set update to false at the beginning instead of the end
				//in case an error gets thrown
				oUpdateFile.m_bUpdate = false;
				String sDate = null;
				String sHeight = null;
				String sTemperature = null;
				if (m_oFtpConn.open(m_sFtpDirectory	+ oUpdateFile.m_sFileName))
				{
					oCharTokenizer.setInput(m_oFtpConn);
					while (oCharTokenizer.nextSet())
					{
						while (oCharTokenizer.hasTokens())
						{
							oCharTokenizer.nextToken(sCurrentToken);
							if (sCurrentToken.length() == 0)
								continue;
							//When searching for the 700mb reading, only every
							//3rd token needs to be checked, so tokens will be
							//skipped here if needed
							if (nTokensToSkip-- > 0)
								continue;
							//Look for '=' to see if this is the end of
							//a station record.  Go back to looking for 'TTAA'
							//if it is.
							if (Text.endsWith(sCurrentToken, "="))
								nState = 1;
							//Check the state and take action
							switch(nState)
							{
								case 1: // Looking for "TTAA"
									if (Text.compare(
											sCurrentToken,"TTAA")==0)
											nState = 2;
										break;
								case 2: // Current Token = Timestamp
									sDate = sCurrentToken.toString();
									nState = 3;
									break;
								case 3: // Current Token = Station Code
									m_oSearchSoundingRecord.m_sStationCode =
											sCurrentToken.toString();
									nState = 4;
									break;
								case 4: // Check for 700 mb reading
								{
									if (!Text.startsWith(sCurrentToken, "70"))
									{
										//Not a 700 mb reading, so skip the
										//temp and wind values.
										nTokensToSkip = 2;
										continue;
									}
									//Set height and temp.
									sHeight = sCurrentToken.toString();
									oCharTokenizer.nextToken(sCurrentToken);
									sTemperature = sCurrentToken.toString();

									// Look up the record to update
									int nRecordIndex = Collections.binarySearch(
											m_oSoundingRecordList,
											m_oSearchSoundingRecord);
									//If there is no match, go back to initial
									//state
									if (nRecordIndex < 0)
									{
										nState = 1;
										continue;
									}
									//Set height/temp/date with encoded values
									//found earlier. This is postponed until now
									//so processing won't be wasted decoding
									//values for readings that won't end up
									//being matched to a station.
									m_oSearchSoundingRecord.
									setTimeStampwithEncodedValue(sDate);

									m_oSearchSoundingRecord.
									setHeightWithEncodedValue(sHeight);

									m_oSearchSoundingRecord.
									setTempWithEncodedValue(sTemperature);
									
									SoundingRecord oUpdateRecord =
											m_oSoundingRecordList.get(
											nRecordIndex);
									//Only update the record if the readings
									//are newer than the current ones and valid
									if (m_oSearchSoundingRecord.m_lTimeStamp
										>
										oUpdateRecord.m_lTimeStamp
										&& !Double.isNaN(
											m_oSearchSoundingRecord.m_dHeight)
										&& !Double.isNaN(
											m_oSearchSoundingRecord.
											m_dTemperature))
									{
									m_oCachedLatLonLock.writeLock();
									oUpdateRecord.m_dTemperature =
										m_oSearchSoundingRecord.m_dTemperature;
									oUpdateRecord.m_dHeight =
										m_oSearchSoundingRecord.m_dHeight;
									oUpdateRecord.m_lTimeStamp =
										m_oSearchSoundingRecord.m_lTimeStamp;
									m_oCachedLatLonLock.writeUnlock();
									}
								break;
								}
							}									
						}
					}					
				}
				m_oFtpConn.close();
				nState = 1;
			}			
		}
		catch (Exception oException)
		{
		}
	}


	/**
	 * The method to be executed by the scheduler. It will get a list of files
	 * currently in the ftp directory and look at the timestamps when files were
	 * modified to determine if they should be retrieved to search for new
	 * readings. Once all the readings are taken, the current station list--
	 * along with the current readings--will be written to a file.
	 */
	@Override
	public void run()
	{
		if (m_oFtpConn != null && m_oFtpConn.connect())
		{
			getCurrentFileList();
			updateFromFtp();
			m_oFtpConn.disconnect();
			save();		
		}		
	}
	
	
	/**
	 * Finds the 700 mb height and temp taken at the location closest to
	 * the given latitude and longitude.  If a valid height and temperature
	 * are found, they will be filled into the HeightTemp object passed.
	 * @param oHeightTemp The heightTemp object will hold the nearest height
	 * and temperature reading after method executes
	 * @param nLat 
	 * @param nLon
	 * @return true if a valid HeightTemp was found.
	 */
	public boolean getHeightTemp(HeightTemp oHeightTemp, IObs iObs)
	{
		//Get Cache object to use for lookup
		CachedLatLon oCachedLatLon = m_oCachedLatLonLock.readLock();
		//Set values to do the cache lookup
		oCachedLatLon.m_lLat = iObs.getLat();
		oCachedLatLon.m_lLon = iObs.getLon();
		//See if there is a result stored in the cache
		int nLookupIndex = Collections.binarySearch(m_oLatLonCache,
			oCachedLatLon);

		if (nLookupIndex >= 0)
		{
			//Get the cached result
			oCachedLatLon = m_oLatLonCache.get(nLookupIndex);
		}
		else
		{
			//-------There wasn't a stored result, so do a full search--------
			//Initialize the closest values
			long lPrimary = Long.MIN_VALUE;
			long lSecondary = Long.MIN_VALUE;
			oCachedLatLon.m_nPrimaryStationIndex = -1;
			oCachedLatLon.m_nSecondaryStationIndex = -1;
			oCachedLatLon.m_sPrimaryStationCode = null;
			oCachedLatLon.m_sSecondaryStationCode = null;
			//Loop through all the stations
			int nIndex = m_oSoundingRecordList.size();
			while (nIndex-- > 0)
			{
				SoundingRecord oSoundingRecord = m_oSoundingRecordList.get(nIndex);
				long lDeltaX = iObs.getLat() - oSoundingRecord.m_nLatitude;
				long lDeltaY = iObs.getLon() - oSoundingRecord.m_nLongitude;
				long lDistance = lDeltaX * lDeltaX + lDeltaY * lDeltaY;
				//See if this station is the closest station yet
				if (lDistance < lPrimary || lPrimary == Long.MIN_VALUE)
				{
					//Shift current primary values to secondary, and store new
					//value as primary
					lSecondary = lPrimary;
					lPrimary = lDistance;
					//Store values that will be used for cache

					//Move current primary index to secondary
					oCachedLatLon.m_nSecondaryStationIndex =
							oCachedLatLon.m_nPrimaryStationIndex;
					//Store new primary index
					oCachedLatLon.m_nPrimaryStationIndex = nIndex;
					//Move current primary code to secondary
					oCachedLatLon.m_sSecondaryStationCode =
							oCachedLatLon.m_sPrimaryStationCode;
					//Store new code as primary
					oCachedLatLon.m_sPrimaryStationCode =
							oSoundingRecord.m_sStationCode;
				}
				else if (lDistance < lSecondary || lSecondary == Long.MIN_VALUE)
				{
					//New Secondary value.  Don't need to shift any values, just
					//replace the current secondary value.
					lSecondary = lDistance;
					oCachedLatLon.m_nSecondaryStationIndex = nIndex;
					oCachedLatLon.m_sSecondaryStationCode =
							oSoundingRecord.m_sStationCode;
				}
			}
			//If a station was looked up successfully store the result in the cache
			//and return the validity of the station's current readings,
			//otherwise return false
			if (lPrimary >= 0)
			{
				oCachedLatLon = new CachedLatLon(oCachedLatLon);
				//Valid return, so cache the result
				m_oLatLonCache.add(~nLookupIndex, oCachedLatLon);
			}
		}

		//See if the discovered values are still valid.
		boolean bReturn = oCachedLatLon.verifyPrimaryValues() ||
				oCachedLatLon.verifySecondaryValues();
		if (bReturn)
		{
			//The cached result's SoundingRecord object will be the current
			//readings for the closest station or, if the values for the
			//closest were invalid, the second closest.
			oHeightTemp.setHeightTemp(oCachedLatLon.m_oSoundingRecord.m_dHeight,
				oCachedLatLon.m_oSoundingRecord.m_dTemperature);
		}
		else
		{
			// when two passes fail, use the backup climate record grid
			synchronized(m_oCalendar)
			{
				m_oCalendar.setTimeInMillis(iObs.getTimestamp());
				m_oReanalysisSearch.m_nLat = iObs.getLat();
				m_oReanalysisSearch.m_nLon = iObs.getLon();
				m_oReanalysisSearch.m_nMonth = m_oCalendar.get(Calendar.MONTH);
				nLookupIndex = Collections.binarySearch(m_oReanalysis, m_oReanalysisSearch);
			}

			if (nLookupIndex < 0)
				nLookupIndex = ~nLookupIndex;

			bReturn = nLookupIndex >= 0 && nLookupIndex < m_oReanalysis.size();
			if (bReturn)
			{
				ReanalysisRecord oRecord =  m_oReanalysis.get(nLookupIndex);
				oHeightTemp.setHeightTemp(oRecord.m_dHeight, oRecord.m_dTemp);
			}
		}

		m_oCachedLatLonLock.readUnlock();
		return bReturn; // No valid station found
	}


	/**
	 * Used to implement StripeLock
	 * @return new CachedLatLon object
	 */
	@Override
	public CachedLatLon getLock()
	{
		CachedLatLon oCachedLatLon = new CachedLatLon();
		oCachedLatLon.m_oSoundingRecord = new SoundingRecord();
		return oCachedLatLon;
	}

	
	/**
	 * The CachedLatLon class is used to store the two closest stations
	 * to a given latitude and longitude
	 */
	public class CachedLatLon implements Comparable<CachedLatLon>
	{
		/**
		 * Latitude used to look up the primary and secondary values
		 */
		public int m_lLat;
		/**
		 * Longitude used to look up the primary and secondary values
		 */
		public int m_lLon;
		/**
		 * StationCode of the closest station
		 */
		public String m_sPrimaryStationCode = null;
		/**
		 * Location in m_oActiveStationList of the closest station
		 */
		public int m_nPrimaryStationIndex = -1;
		/**
		 * StationCode of the second closest station
		 */
		public String m_sSecondaryStationCode = null;
		/**
		 * Location in the m_oActiveStationList of the second closest station
		 */
		public int m_nSecondaryStationIndex = -1;
		/**
		 * SoundingRecord object to lookup the full record that the cached
		 * values reference in m_oActiveStationList
		 */
		public SoundingRecord m_oSoundingRecord = null;
		
		private CachedLatLon()
		{

		}


		/**
		 * Copy constructor for CachedLatLon class
		 * @param oCachedLatLon CachedLatLon to get initial values from
		 */
		public CachedLatLon(CachedLatLon oCachedLatLon)
		{
			this.m_lLat = oCachedLatLon.m_lLat;
			this.m_lLon = oCachedLatLon.m_lLon;
			this.m_nPrimaryStationIndex = 
										oCachedLatLon.m_nPrimaryStationIndex;
			this.m_nSecondaryStationIndex = 
										oCachedLatLon.m_nSecondaryStationIndex;
			this.m_sPrimaryStationCode = 
							new String(oCachedLatLon.m_sPrimaryStationCode);
			this.m_sSecondaryStationCode = 
							new String(oCachedLatLon.m_sSecondaryStationCode);
		}


		/**
		 * @return true if the primary cached station is still in the list and 
		 * it currently has valid height and temperature readings
		 */
		public boolean verifyPrimaryValues()
		{
			return verifyValues(m_nPrimaryStationIndex, m_sPrimaryStationCode);
		}
		

		/**
		 * @return true if the secondary cached station is still in the list and
		 * it currently has valid height and temperature readings
		 */
		public boolean verifySecondaryValues()
		{
			if (m_sSecondaryStationCode == null)
				return false;
			else
				return verifyValues(m_nSecondaryStationIndex, 
					m_sSecondaryStationCode);
		}


		/**
		 * @param nIndex Index of the cached station in m_oActiveStationList
		 * @param sCode Expected station code.  Used to verify the station
		 * at nIndex is the correct station, and then to lookup the new index
		 * if the code for the station at nIndex does not match sCode
		 * @return true if two conditions are met:
		 * <BR>1. The code of the station at nIndex matches sCode, or if they
		 * don't match the station's new index can be looked up in
		 * m_oSoundingRecordList
		 * <BR>2. The current readings for the cached station are valid ( not
		 * equal to NaN and taken withen the last 12 hours)
		 */
		private boolean verifyValues(int nIndex, String sCode)
		{
			//Get the current time - 12 hours in milliseconds to test
			//if readings are recent enough.
			long lCutOff = System.currentTimeMillis() - (12*60*60*1000);
			//Get the sounding record at the cached index
			if (nIndex < m_oSoundingRecordList.size())
			{
				m_oSoundingRecord = m_oSoundingRecordList.get(nIndex);
				//Check if the station at the cached index has changed
				if (sCode.compareTo(m_oSoundingRecord.m_sStationCode) == 0)
				{
					//If the station hasn't changed, return the validity of the
					//station's current readings
					return lCutOff <= m_oSoundingRecord.m_lTimeStamp
							&& !Double.isNaN(m_oSoundingRecord.m_dHeight)
							&& !Double.isNaN(m_oSoundingRecord.m_dTemperature);
				}
			}
			//The cached station's index has changed, so set up the
			//SoundingRecord object to lookup the new index
			m_oSoundingRecord.m_sStationCode = sCode;
			//Get the station's new index
			int nLookupIndex = Collections.binarySearch(m_oSoundingRecordList,
					m_oSoundingRecord);
			//Make sure the station is still in the list
			if (nLookupIndex < 0)
			{
				//If the cached station can't be found in the list, remove
				//this value from the cache so that a new one will be looked up.
				m_oLatLonCache.remove(this);
				return false;
			}
			//See if the values passed in are the primary or secondary values,
			//and then update the appropriate index
			if (nIndex == m_nPrimaryStationIndex)
				m_nPrimaryStationIndex = nLookupIndex;
			else
				m_nSecondaryStationIndex = nLookupIndex;
			//Get the cached StationRecord using the new index
			m_oSoundingRecord = m_oSoundingRecordList.get(nLookupIndex);
			//return the validity of the station's current readings
			return lCutOff <= m_oSoundingRecord.m_lTimeStamp
					&& !Double.isNaN(m_oSoundingRecord.m_dHeight)
					&& !Double.isNaN(m_oSoundingRecord.m_dTemperature);
		}


		public int compareTo(CachedLatLon oRhs)
		{
			int nReturnVal = this.m_lLat - oRhs.m_lLat;
			if (nReturnVal == 0)
				return this.m_lLon - oRhs.m_lLon;
			return nReturnVal;
		}
	}

	
	/**
	 * The SoundingFile class hold information for a file hosted
	 * on the National Weather Service ftp site.  It holds the name of a file
	 * as a string, the time that the file was last modified as a long, and 
	 * whether or not the file has been updated as a boolean
	 */
	public class SoundingFile implements Comparable<SoundingFile>
	{
		/**
		 * The name of the file
		 */
		public String m_sFileName;
		/**
		 * Whether or not the file has been updated
		 */
		public boolean m_bUpdate;
		/**
		 * The time that the file was last modified.
		 */
		public long m_lTimeStamp;

		/**
		 * Constructor to initialize both the filename and timestamp
		 * @param sFileName
		 * @param lTimeStamp
		 */
		public SoundingFile(String sFileName, long lTimeStamp)
		{
			m_lTimeStamp = lTimeStamp;
			m_sFileName = sFileName;
			m_bUpdate = true;
		}


		/**
		 * Constructo to initialize only the timestamp
		 * @param sFileName
		 */
		public SoundingFile(String sFileName)
		{
			m_sFileName = sFileName;
			m_lTimeStamp = 0;
			m_bUpdate = true;
		}

		
		/**
		 * Copy constructor for SoundingFile class
		 * @param oSoundingFile
		 */
		public SoundingFile(SoundingFile oSoundingFile)
		{
			this.m_lTimeStamp = oSoundingFile.m_lTimeStamp;
			this.m_sFileName = oSoundingFile.m_sFileName;
			this.m_bUpdate = oSoundingFile.m_bUpdate;
		}


		/**
		 *
		 * @param oSoundingFile
		 * @return true if the filenames for the two SoundingFiles are equal
		 */
		public boolean equals(SoundingFile oSoundingFile)
		{
			return m_sFileName.compareTo(oSoundingFile.m_sFileName) == 0;
		}


		/**
		 * Compares the timestamps for two SoundingFiles
		 * @param oSoundingFile
		 * @return true if this SoundingFile is newer than oSoundingFile
		 */
		public boolean isNewerThanFile(SoundingFile oSoundingFile)
		{
			return this.m_lTimeStamp > oSoundingFile.m_lTimeStamp;
		}


		public int compareTo(SoundingFile oSoundingFile)
		{
			return this.m_sFileName.compareTo(oSoundingFile.m_sFileName);
		}
	}


	/**
	 * The <code>SoundingRecord</code> class is used to hold individual readings
	 * from NWS balloon soundings. It holds the values for the readings, the
	 * originating station, and the methods used to decode values from the NWS
	 * sounding files
	 */
	public class SoundingRecord implements Comparable<SoundingRecord>
	{
		/**
		 * Height that the 700mb reading occurred
		 */
		public double m_dHeight = Double.NaN;
		/**
		 * Station associated with the reading
		 */
		public String m_sStationCode = "";
		/**
		 * Latitude of the station associated with the reading
		 */
		public int m_nLatitude = 0;
		/**
		 * Longitude of the station associated with the reading
		 */
		public int m_nLongitude = 0;
		/**
		 * Temperature taken
		 */
		public double m_dTemperature  = Double.NaN;
		/**
		 * Time that the reading was taken
		 */
		public long m_lTimeStamp;
		/**
		 * Default SoundingRecord constructor
		 */
		public SoundingRecord()
		{			
		}


		/**
		 * @param sCols An array of values to initialize the SoundingRecord
		 * values
		 */
		public SoundingRecord(String[] sCols)
		{
			if (sCols.length > 0)
				m_sStationCode = sCols[0];
			if (sCols.length > 1)
				m_nLatitude = Stations.toMicro(Double.parseDouble(sCols[1]));
			if (sCols.length > 2)
				m_nLongitude = Stations.toMicro(Double.parseDouble(sCols[2]));
			if (sCols.length > 3)
				m_dHeight = Double.parseDouble(sCols[3]);
			if (sCols.length > 4)
				m_dTemperature = Double.parseDouble(sCols[4]);
			if (sCols.length > 5)
				m_lTimeStamp = Long.parseLong(sCols[5]);
		}


		/**
		 * @return values for the record as a string in CSV format
		 */
		@Override public String toString()
		{
			Date oTimeStamp = new Date(m_lTimeStamp);
			return m_sStationCode 
					+ "," + Double.toString(Stations.fromMicro(m_nLatitude))
					+ "," + Double.toString(Stations.fromMicro(m_nLongitude))
					+ "," + Double.toString(m_dHeight)
					+ "," + Double.toString(m_dTemperature)
					+ "," + Long.toString(m_lTimeStamp)
					//Print out the timestamp as a string to make it readable
					//if it needs to be checked.
					+ "," + oTimeStamp.toString();
		}


		/**
		 * @param oComparisonRecord
		 * @return true if the station codes are equal
		 */
		public boolean equals(SoundingRecord oComparisonRecord)
		{
			return this.m_sStationCode.equals(oComparisonRecord.m_sStationCode);
		}


		/**
		 * Converts the height code from hhh to height in meters.
		 * Currently only handles 700 mb height codes.
		 * If the conversion from string fails, the height is set to Double.NaN
		 * @param sEncodedHeightAndPressure encoded value to set height with.
		 */
		public void setHeightWithEncodedValue(String sEncodedHeightAndPressure)
		{			
			try
			{
				m_dHeight = Double.parseDouble(
						sEncodedHeightAndPressure.substring(2, 5));
				if (m_dHeight <= 500)
					m_dHeight += 3000;
				else
					m_dHeight += 2000;
			}
			catch (Exception oException)
			{
				m_dHeight = Double.NaN;
			}
		}


		/**
		 * The encoded value is 5 digits, the first three of which represent the
		 * temperature multiplied by 10. The temperature is then set to the
		 * value of the first three digits divided by 10. If the conversion
		 * from string fails the temperature is set to Double.NaN
		 * @param sEncodedTempAndWind the encoded value to set temp with.
		 */
		public void setTempWithEncodedValue(String sEncodedTempAndWind)
		{
			try
			{
				m_dTemperature = Double.parseDouble(
						sEncodedTempAndWind.substring(0,3))/10;
			}
			catch (Exception oException)
			{
				m_dTemperature = Double.NaN;
			}
		}
		
		
		/**
		 * Set the timestamp using the encoded value.  If the conversion fails,
		 * the time is defaulted to the epoch - 1970.  
		 * @param sEncodedTime 5 digit value containing the encoded timestamp
		 */
		public void setTimeStampwithEncodedValue(String sEncodedTime)
		{
			try
			{
				//The first two digits should be the date plus 50, but may be
				//just the date, so check if it is over 50 before subtracting.
				int nDayOfMonth =Integer.parseInt(sEncodedTime.substring(0, 2));
				if (nDayOfMonth >  50)
					nDayOfMonth -= 50;
				int nHourOfDay = Integer.parseInt(sEncodedTime.substring(2, 4));
				//Get a Calendar object initialized to the current time in UTC
				Calendar oUtcCalendar =
						Calendar.getInstance(TimeZone.getTimeZone("GMT"));				
				//The timestamp doesn't have a second, millisecond value, or
				//minute value so set them to 0
				oUtcCalendar.set(Calendar.SECOND, 0);
				oUtcCalendar.set(Calendar.MINUTE, 0);
				oUtcCalendar.set(Calendar.MILLISECOND, 0);
				//Set the day and hour from the timestamp
				oUtcCalendar.set(Calendar.DAY_OF_MONTH, nDayOfMonth);
				oUtcCalendar.set(Calendar.HOUR_OF_DAY, nHourOfDay);
								
				//The timestamp should not be in the future, so try to fix it
				//if it is. 
				long lCurrentTime = System.currentTimeMillis();				
				while (oUtcCalendar.getTimeInMillis() > lCurrentTime)
					oUtcCalendar.add(Calendar.MONTH, -1);
				
				m_lTimeStamp = oUtcCalendar.getTimeInMillis();
			}
			catch (Exception oException)
			{
				m_lTimeStamp = 0;
			}			
		}


		/**
		 * Comparares <i> this </i> sounding record with the one provided by
		 * station code.
		 * @param oSoundingRecord record to compare with <i> this </i>
		 * @return 0 if the values match
		 */
		public int compareTo(SoundingRecord oSoundingRecord)
		{
			return
				this.m_sStationCode.compareTo(oSoundingRecord.m_sStationCode);
		}
	}


	private class ReanalysisRecord implements Comparable<ReanalysisRecord>
	{
		int m_nLat;
		int m_nLon;
		int m_nMonth;
		double m_dTemp;
		double m_dHeight;


		private ReanalysisRecord()
		{
		}


		private ReanalysisRecord(String sLine)
		{
			// split the comma-separated string and test for the correct length
			String[] sCols = sLine.split(",");
			if (sCols.length < 4)
				return;

			// parse the record, adjusting coordinates as needed
			m_nLat = Stations.toMicro(Double.parseDouble(sCols[0]));
			m_nLon = Stations.toMicro(Double.parseDouble(sCols[1]) - 360.0);
			m_nMonth = Integer.parseInt(sCols[2]);
			m_dTemp = Double.parseDouble(sCols[3]);
			m_dHeight = Double.parseDouble(sCols[4]);
		}


		/**
		 * Comparares <i>this</i> reanalysis record for sorting purposes.
		 * @param oRecord reanalysis record to compare with <i>this</i>
		 * @return less than 0, 0, or greater than 0 if the comparison result
		 *	is less than, equal to, or greater than this object, respectively
		 */
		public int compareTo(ReanalysisRecord oRecord)
		{
			int nIndex = m_nMonth - oRecord.m_nMonth;
			if (nIndex != 0)
				return nIndex;

			nIndex = m_nLat - oRecord.m_nLat;
			if (nIndex != 0)
				return nIndex;

			return (m_nLon - oRecord.m_nLon);
		}
	}
}
