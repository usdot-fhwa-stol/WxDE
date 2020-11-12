// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file XmlCollector.java
 */
package wde.cs.xml;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;
import wde.cs.CsMgr;
import wde.cs.ICollector;
import wde.dao.ObservationDao;
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.obs.IObsSet;
import wde.obs.ObsMgr;
import wde.util.MathUtil;
import wde.util.Text;
import wde.util.net.NetConn;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.sql.*;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Date;
import org.apache.log4j.Logger;


/**
 * Provides a means of reading values from Xml files, matching the current value
 * to the proper DataValue and creating observation objects from the values,
 * the observation objects are then added to the Obs-cache.
 * <p/>
 * <p>
 * Implements {@code ICollector} to perform the collection and processing of
 * Xml observations from contributors.
 * </p>
 */
public class XmlCollector extends DefaultHandler implements ICollector {
    /**
     * Collector Service Id.
     */
    protected int m_nId;
    /**
     * Time to wait, in seconds, after the collection attemp occurs.
     */
    protected int m_nDelay;
    /**
     * Latitude read from input for mobile data
     */
    protected int m_nLat;
    /**
     * Longitude read from input for mobile data
     */
    protected int m_nLon;
    /**
     * Elevation read from input for mobile data
     */
    protected short m_tElev;
    /**
     * Sensor index to be used if no sensor index is specified.
     */
    protected int m_nDefaultSensorIndex;
    /**
     * Current sensor index.
     */
    protected int m_nCurrentSensorIndex;
    /**
     * Whether or not to ignore the default sensor index.
     */
    protected boolean m_bIgnoreDefault = false;
    /**
     * Timestamp tracks time at which data was collected.
     */
    protected long m_lTimestamp;
    /**
     * Retry collection attempt flag.
     */
    protected boolean m_bRetry;
    /**
     * Instance of observation resolving service.
     */
    protected ObservationDao obsDao = ObservationDao.getInstance();
    /**
     * Platform code Id.
     */
    protected StringBuilder platformCodeSB = new StringBuilder();
    /**
     * Holds a string version of the Timestamp
     */
    protected StringBuilder m_sTimestamp = new StringBuilder();
    /**
     * Last date/time of collection (millisecond precision).
     */
    protected Date m_oLastCollection = new Date();
    /**
     * Timestamp format.
     */
    protected DateFormat m_oTsFormat;
    /**
     * File from which data is to be gathered.
     */
    protected DateFormat m_oFilepath;
    /**
     * List of {@link DataValue} objects. When the current path corresponds to
     * a DataValue that value is read in and stored.
     */
    protected ArrayList<DataValue> m_oDataValueHandler = new ArrayList<DataValue>();
    /**
     * Keeps track of the current path.
     */
    protected StringBuilder m_sPath = new StringBuilder();
    /**
     * List of {@link ValueHolder} objects hold values until they are written.
     */
    protected ArrayDeque<ValueHolder> m_oValueSet = new ArrayDeque<ValueHolder>();
    /**
     * Holds unused empty ValueHolder objects so they can be reused.
     */
    protected ArrayDeque<ValueHolder> m_oValuePool = new ArrayDeque<ValueHolder>();
    /**
     * Reference to Collector Service Manager for elevation check
     */
    protected CsMgr m_oCsMgr;
    /**
     * Parent Collector Service object.
     */
    protected XmlSvc m_oXmlSvc;
    /**
     * Instance of platform observation resolving service
     */
    protected PlatformDao platformDao = PlatformDao.getInstance();
    /**
     * List of {@link PathCount} objects which store the current data value and
     * the number of characters added to the path.
     */
    //protected ArrayDeque<PathCount> m_oAdded = new ArrayDeque<PathCount>();
    /**
     * Instance of Sensor observation resolving service.
     */
    protected SensorDao sensorDao = SensorDao.getInstance();
    /**
     * Sax Parser used to read the xml file.
     */
    protected SAXParser m_oParser;
    /**
     * StringBuilder used throughout class.
     * <br />It was created to prevent creating new StringBuilders frequently.
     */
    protected StringBuilder m_sBuffer = new StringBuilder();
    /**
     * Flag set at the start of the file.
     * <br />Used to skip the root tag.
     */
    protected boolean m_bStartFile;
    /**
     * If the current path matches a DataValue then that DataValue is stored
     * here. Otherwise contains null.
     */
    protected DataValue m_oCurrentDataValue;
    protected String m_oLastEquipmentId;
    /**
     * Stores CurrentDataValue only if the CurrentDataValue
     * is a WriteRecord object.
     */
    protected DataValue m_oWriteRecord;
    /**
     * Keeps track of the number of data values added to the tags array.
     */
    protected ArrayDeque<Integer> m_oAdded = new ArrayDeque<Integer>();
    /**
     * Stores a data value for each tag read in.
     */
    protected ArrayList<DataValue> m_oTags = new ArrayList<DataValue>();
    /**
     * Dummy value - Placeholder in tags array for values that should be
     * kept
     */
    protected DataValue m_oKeep = new DataValue();
    /**
     * Dummy value - placeholder in a the tags array for values that can be
     * ignored.
     */
    protected DataValue m_oIgnore = new DataValue();
    /**
     * List of contributors that provides context for platform searches.
     */
    int[] m_nContribIds;
    /**
     * Query format string:
     * <pre>    {path Id},{observation type id},{conversion multiplier}, </pre>
     * <pre>    {class name},{units},{values to ignore},{xml path} </pre>
     */
    private String m_sQuery = "SELECT pathId, obsTypeId, multiplier, " +
            "className, unit, ignoreValues, xmlPath " +
            "FROM conf.xmldef WHERE collectorId = ?";

    private static final Logger g_oLogger = Logger.getLogger(XmlCollector.class);

    /**
     * <b> Default Constructor </b>
     * <p/>
     * <p>
     * Creates new instances of {@code XmlCollector}. The Non-default
     * constructor performs initialization.
     * </p>
     */
    protected XmlCollector() {
    }


    /**
     * Initializes {@code XmlCollector} attributes. Sets up the timezone,
     * and timestamp formats. Prepares and executes the query ({@code m_sQuery})
     * , and creates the corresponding {@code DataValue} handler for each
     * column, which is then added to the list of {@code DataValue} handlers
     * ({@code m_oDataValueHandler}). Intializes SaxParser to prepare to read
     * the file.
     *
     * @param nId                 collector service Id.
     * @param nDelay              time to wait, in milliseconds, after collection time
     *                            occurs.
     * @param bRetry              retry collection attempt flag.
     * @param nCollectTzId        collector timezone id.
     * @param nContentTzId        content timezone id.
     * @param nTimestampId        timestamp format id.
     * @param nDefaultSensorIndex Sensor index to be used if no sensor index
     *                            is specified.
     * @param sFilepath           string describing timestamp formatted filename.
     * @param oCsMgr              main{@code CsMgr} instance.
     * @param oXmlSvc             parent Xml collector service object.
     * @param iConnection         connection to SQL, assumes it is connected and ready
     *                            to read.
     */
    protected XmlCollector(int nId, int nDelay, boolean bRetry,
      int nCollectTzId, int nContentTzId, int nTimestampId,
      int nDefaultSensorIndex, String sFilepath, int[] nContribIds,
      CsMgr oCsMgr, XmlSvc oXmlSvc, Connection iConnection)
  {
    m_nId = nId;

    // set the number of seconds to wait after the collection time occurs
    m_nDelay = nDelay * 1000; // convert seconds to milliseconds

    // set whether the collector should retry upon failure
    m_bRetry = bRetry;

    m_oTsFormat = oCsMgr.createDateFormat(nTimestampId);
    m_oTsFormat.setTimeZone(oCsMgr.createSimpleTimeZone(nContentTzId));

    m_nDefaultSensorIndex = nDefaultSensorIndex;

    m_oFilepath = new SimpleDateFormat(sFilepath);
    m_oFilepath.setTimeZone(oCsMgr.createSimpleTimeZone(nCollectTzId));

    m_nContribIds = nContribIds; // save reference to list of contrib ids
    m_oXmlSvc = oXmlSvc;
    m_oCsMgr = oCsMgr;

    try(PreparedStatement oXmlDefStmt = iConnection.prepareStatement(m_sQuery))
    {
        oXmlDefStmt.setInt(1, nId);
      try(ResultSet oXmlDefRs = oXmlDefStmt.executeQuery())
      {

        while(oXmlDefRs.next())
        {
          DataValue oDataHandler = (DataValue) Class.forName(
              oXmlDefRs.getString(4)).newInstance();

          oDataHandler.init(oXmlDefRs.getInt(2), oXmlDefRs.getDouble(3),
              oXmlDefRs.getString(5), oXmlDefRs.getString(6), oXmlDefRs.getString(7), this);

          m_oDataValueHandler.add(oDataHandler);
        }

        // create a SAX parser to handle the xml document
        m_oParser = SAXParserFactory.newInstance().newSAXParser();
      }
    }
    catch(Exception oException)
    {
      g_oLogger.error(oException, oException);
    }
  }

    /**
     * Tells the SaxParser to read the file, adding the observations to the
     * parent Collectior Service's Obs-cache.
     *
     * @param nContribId The contributor id we're interested in collecting from.
     * @param oNetConn   Input stream, previously connected.
     * @param lTimestamp Collection time, not delay-adjusted.
     */
    public void collect(NetConn oNetConn, long lTimestamp) {
        // adjust the task execution time by the delay to derive
        // the actual collection time that generates filenames
        lTimestamp -= m_nDelay;

        // verify that this is a unique collection request
        if (m_oLastCollection.getTime() == lTimestamp)
            return;

        // collect column data here
        m_oLastCollection.setTime(lTimestamp);

        try {
            if (oNetConn.open(m_oFilepath.format(m_oLastCollection))) {
                // the parser will read in all the DataValues from the Xml file
                m_oParser.parse(oNetConn, this);
                oNetConn.close();
            } else
                m_oLastCollection.setTime(0);
        } catch (Exception oException) {
	  	    m_sPath.setLength(0);
		    m_oTags.clear();
		    m_sBuffer.setLength(0);
		    g_oLogger.error(oException, oException);
            try {
                oNetConn.close();
            } catch (Exception oIOException) {
            }

            m_oLastCollection.setTime(0);
        }
    }


    /**
     * Checks whether there is a sensor index appended to {@code sValue}
     * by a period or underscore. If so the sensor index is set, in either
     * case {@code sValue} is appended to {@code sBuffer} so that it can be
     * added to the path.
     *
     * @param sBuffer pointer to m_sBuffer.
     * @param sValue  string to be possibly split, and added to the path.
     */
    private void splitSensor(StringBuilder sBuffer, String sValue) {
        sBuffer.setLength(0);
        int nIndex = sValue.length();
        char cDigit = 0;
        while (nIndex-- > 0 && Character.isDigit(sValue.charAt(nIndex))) ;

        if (nIndex >= 0) {
            cDigit = sValue.charAt(nIndex);
            if (cDigit == '.' || cDigit == '_') {
                m_nCurrentSensorIndex =
                        Text.parseInt(sValue, ++nIndex, sValue.length());
                sBuffer.append(sValue, 0, --nIndex);
            } else
                sBuffer.append(sValue, 0, sValue.length());
        } else
            sBuffer.append(sValue, 0, sValue.length());
    }


    /**
     * At the start of the document sets the {@code m_bStartFile} flag to true.
     */
    @Override
    public void startDocument() {
        m_bStartFile = true;
    }


    /**
     * startElement uses recursion to do most of the work searching through the
     * xml file for the values we want to read in. It keeps track of the its
     * current location in the file using a StringBuilder {@code m_sPath} and an
     * ArrayList {@code m_oTags}. startElement always adds something to
     * {@code m_oTags} based on the {@code qName} so that endElement can always
     * pull something off.
     * <p>
     * If the tag contains attributes this method recursively calls itself
     * passing in the attribute name as the {@code qName} and null for the other
     * parameters. If the attribute name belongs in the path then it also
     * recursively calls itself, passing in the attribute value as the
     * {@code qName} and null for the other values.
     * </p>
     *
     * @param uri        is not used in this method
     * @param localName  is not used in this method
     * @param qName      The tag name
     * @param attributes A list of values following the tag name in the xml file
     */
    @Override
    public void startElement(String uri, String localName,
                             String qName, Attributes attributes) {
        int nAddCount = 0;
        if (m_bStartFile) {
            m_oAdded.push(0);
            m_bStartFile = false;
            return;
        }

        splitSensor(m_sBuffer, qName);

        m_sPath.append("/");
        m_sPath.append(m_sBuffer);

        m_oCurrentDataValue = findDataValue();
        if (m_oCurrentDataValue != null) {
            m_oTags.add(m_oCurrentDataValue);
            m_oCurrentDataValue.start();
        } else if (startsWith())
            m_oTags.add(m_oKeep);
        else
            m_oTags.add(m_oIgnore);

      if(attributes != null)
      {
        if(startsWith())
        {
          int nAttLength = attributes.getLength();

          //Due to the order of attributes coming in we could have one
          //attribute's path match a data value, but then have another one clear
          //it back to null before we come out of the attribute loop and process the found value.
          //If an attribute does match a value, hold on to it here so we can
          //set it as the current value again before we return
          DataValue oFoundDataValue = m_oCurrentDataValue;

          for(int nAttIndex = 0; nAttIndex < nAttLength; nAttIndex++)
          {
            startElement(null, null, attributes.getQName(nAttIndex), null);
            nAddCount++;
            if(m_oTags.get(m_oTags.size() - 1) == m_oCurrentDataValue)
            {
              m_sBuffer.setLength(0);
              m_sBuffer.append(attributes.getValue(nAttIndex));
              m_oCurrentDataValue.characters(m_sBuffer);
              pop();
              nAddCount--;
              m_oCurrentDataValue = null;
            }
            else if(m_oTags.get(m_oTags.size() - 1) == m_oKeep)
            {
              startElement(null, null, attributes.getValue(nAttIndex), null);
              nAddCount++;
              //if it is either the ignore signal or an actual found
              //value, then we don't need to keep the attribute name/valaue
              //in the path.
              if(m_oTags.get(m_oTags.size() - 1) != m_oKeep)
              {
                pop();
                pop();
                nAddCount -= 2;
              }
              if(m_oCurrentDataValue != null)
                oFoundDataValue = m_oCurrentDataValue;
            }
            else if(m_oTags.get(m_oTags.size() - 1) == m_oIgnore)
            {
              pop();
              nAddCount--;
            }
          }
          m_oCurrentDataValue = oFoundDataValue;
        }
        nAddCount++;
        m_oAdded.addFirst(nAddCount);
      }
    }


    /**
     * If we have a CurrentDataValue this method reads in the value
     * and stores it to be written later
     *
     * @param ch     a character array of the value to be read in
     * @param start  the starting index of the character array
     * @param length the length of the value stored in the char array
     */
    @Override
    public void characters(char[] ch, int start, int length) {
        if (m_oCurrentDataValue != null) {
            String sValue = new String(ch, start, length);

            m_sBuffer.setLength(0);
            m_sBuffer.append(sValue);

            m_oCurrentDataValue.characters(m_sBuffer);
            m_oCurrentDataValue = null;
        }
    }

    /**
     * Retrieves the addCount we set in startElement, and calls {@code pop()}
     * {@code nAddCount} number of times. Also determines whether to reset
     * the sensor index, and always resets {@code m_oCurrentDataValue}.
     * <p>
     * In this overridden version of this method the parameters are not used.
     * </p>
     *
     * @param uri       is not used in this method.
     * @param localName is not used in this method.
     * @param qName     contains the name of the end tag but still isn't used.
     */
    @Override
    public void endElement(String uri, String localName, String qName) {
        int nAddCount = m_oAdded.removeFirst();

        // pop cuts path back to the last / as well as pop off
        // the last thing in m_oTags and call .end() on it
        while (nAddCount-- > 0)
            pop();

        if (!m_bIgnoreDefault)
            m_nCurrentSensorIndex = m_nDefaultSensorIndex;

        m_oCurrentDataValue = null;
    }


    /**
     * Finds the last occurance of '/' in {@code m_sPath}indicating the
     * beginning of the last tag added to the path. Once found it removes the
     * last tag on the path. Also removes the last {@code DataValue} in
     * {@code m_oTags} and calls .end() on that {@code DataValue}. Most
     * of the time calling .end() will do nothing but is necessary to trigger
     * the end of a sensor group or the end of observations from a platform.
     */
    private void pop() {
        int nIndex = m_sPath.length();

        // find the index of the last '/'
        while (nIndex-- >= 0 && m_sPath.charAt(nIndex) != '/') ;
        // cut off everything after the last '/' from the path
        m_sPath.setLength(nIndex);

        // pop off the last DataValue on m_oTags and call end() on it
        m_oTags.remove(m_oTags.size() - 1).end();
    }
//
//
//
//
//
//


    /**
     * startElement does most of the work in searching through the xml file for
     * the values we want to read in.
     * <p>
     * It starts by adding the qName to the path and checking to see if there
     * is a corresponding {@code DataValue} and acts accordingly. If the tag
     * contains attributes then it adds them to the path one by one and checks
     * for a corresponding {@code DataValue}
     * </p>
     * <p>
     * This method must also keep track of the number of characters added to the
     * path as well as CurrentDataValues for future use when endElement is
     * called by the parser.
     * </p>
     *
     * @param uri is not used in this method
     * @param localName is not used in this method
     * @param qName The tag name
     * @param attributes A list of values following the tag name in the xml file
     */
/*
    @Override
	public void startElement(String uri, String localName,
		String qName, Attributes attributes)
	{
		int nAddCount = 0;
		int nFirstAdd = 0;

		if (!m_bStartFile)
		{
			// remove and save the sensor index that might be in the tag name
			splitSensor(m_sBuffer, qName);

			// add the tag name to the path, nAddCount will be incremented later
			m_sPath.append("/");
			m_sPath.append(m_sBuffer);
			nFirstAdd += m_sBuffer.length() + 1;
		}
		else
			m_bStartFile = false;

		// check if the current path matches a DataValue
		int nIndex = findDataValue();
		if (nIndex >= 0)
		{
			m_oCurrentDataValue = m_oDataValueHandler.get(nIndex);
			m_oCurrentDataValue.start();
		}
		if (startsWith())
		{
			// loop through each attribute
			int nAttLength = attributes.getLength();
			for(int nAttIndex = 0; nAttIndex < nAttLength; nAttIndex++)
			{
				// remove and save the sensor index that might be in the tag name
				splitSensor(m_sBuffer, attributes.getQName(nAttIndex));
				// add the name of this attribute to the path
				m_sPath.append("/");
				m_sPath.append(m_sBuffer);
				nAddCount += m_sBuffer.length() + 1;

				// check if the new path matches a DataValue
				nIndex = findDataValue();
				// if the path+attributeName matches
				// then we want to read in the attribute value
				if (nIndex >= 0)
				{
					m_sBuffer.setLength(0);
					m_sBuffer.append(attributes.getValue(nAttIndex));
					m_oDataValueHandler.get(nIndex).characters(m_sBuffer);
				}
				else
				{
					splitSensor(m_sBuffer, attributes.getValue(nAttIndex));

					// add the attribute value to the path and check it
					m_sPath.append("/");
					m_sPath.append(m_sBuffer);
					nAddCount += m_sBuffer.length() + 1;
					nIndex = findDataValue();
					// if the path+attributeName+attributeValue matches
					// then we want to read in the data following this tag
					if (nIndex >= 0)
					{
						m_oCurrentDataValue = m_oDataValueHandler.get(nIndex);
						m_oCurrentDataValue.start();
					}
				}

				// if we don't want a following value remove what we added from the path
				if (m_oCurrentDataValue == null && !startsWith())
				{
						pop(nAddCount);
						nAddCount = 0;
				}
			}
		}
		// this is from adding the qName to the path
		nAddCount += nFirstAdd;

		if(m_oWriteRecord != null)
		{
			m_oAdded.addFirst(new PathCount(m_oWriteRecord, nAddCount));
			m_oWriteRecord = null;
		}
		else
			m_oAdded.addFirst(new PathCount(m_oCurrentDataValue, nAddCount));

	}
*/

    /**
     * Checks if any {@Code DataValue} has a path that starts with but is not
     * equal to the current path.
     *
     * @return true if a DataValue is found.
     * <br /> false otherwise.
     * @see Text#startsWith(java.lang.CharSequence, java.lang.CharSequence)
     */
    private boolean startsWith() {
        int nIndex = m_oDataValueHandler.size();
        boolean bFound = false;
        while (!bFound && nIndex-- > 0)
            bFound = (Text.startsWith(m_oDataValueHandler.get(nIndex).m_sPath,
                    m_sPath) && Text.compare(m_oDataValueHandler.get(nIndex).m_sPath,
                    m_sPath) != 0);

        return bFound;
    }


    /**
     * If we have a CurrentDataValue this method reads in value
     * and stores it in the CurrentDataValue.
     *
     * @param ch a character array of the value to be read in
     * @param start the starting index of the character array
     * @param length the length of the value stored in the char array
     */
/*
	@Override
	public void characters(char[] ch, int start, int length)
	{
		if (m_oCurrentDataValue != null)
		{
			String sValue = new String(ch, start, length);

			m_sBuffer.setLength(0);
			m_sBuffer.append(sValue);

			m_oCurrentDataValue.characters(m_sBuffer);
			m_oCurrentDataValue = null;
		}
	}
 */


    /**
     * Removes {@code nAddCount} number of characters from the end of the path
     *
     * @param nAddCount number of characters to be removed
     */
/*
	private void pop(int nAddCount)
	{
		int nIndex = m_sPath.length() - nAddCount;
		m_sPath.setLength(nIndex);
	}
*/

    /**
     * Removes the proper number of characters from the end of the path and
     * calls .end() on the proper DataValue. Also determines whether to reset
     * the sensor index, and always resets {@code m_oCurrentDataValue}. In this
     * overridden version of this method the parameters are not used
     *
     * @param uri is not used in this method.
     * @param localName is not used in this method.
     * @param qName contains the name of the end tag but still isn't used.
     */
/*
	@Override
	public void endElement(String uri, String localName, String qName)
	{
		PathCount oPathCount = m_oAdded.removeFirst();
		m_oCurrentDataValue =oPathCount.getDataValue();
		int nAddCount = oPathCount.getAddCount();

		if (m_oCurrentDataValue != null)
			m_oCurrentDataValue.end();

		pop(nAddCount);

		if (!m_bIgnoreDefault)
			m_nCurrentSensorIndex = m_nDefaultSensorIndex;

		m_oCurrentDataValue = null;
	}
 */


    /**
     * Sets the {@code m_bIgnoreDefault} flag to determine whether or not
     * to reset the current sensor index to the default sensor index
     * after a value is read in.
     *
     * @param bIgnore value with which to set {@code m_bIgnoreDefault}.
     */
    public void setIgnoreDefault(boolean bIgnore) {
        m_bIgnoreDefault = bIgnore;
    }


    /**
     * Goes through each {@code ValueHolder}, removes it from the ValueSet array
     * and writes the data. Then it resets the {@Code ValueHolder} and stores it
     * in the array {@code m_oValuePool} so that it can be reused.  Last it
     * resets the platform code and timestamp.
     */
    public void writeValueSet() {
        // go through the ValueSet Deque and create an Obs for each value
        // then reset each ValueHolder and put it in a pool to be reused
        ValueHolder oCurrentValue;

        while (m_oValueSet.size() > 0) {
            oCurrentValue = m_oValueSet.removeFirst();
            oCurrentValue.writeData();
            oCurrentValue.reset();

            // store the empty ValueHolder so it can be reused
            m_oValuePool.addFirst(oCurrentValue);
        }

        // reset common platform code, timestamp, and sensor index
        platformCodeSB.setLength(0);
        m_sTimestamp.setLength(0);
        m_lTimestamp = 0L;
    }


    /**
     * Checks if any {@Code DataValue} has a path that matches the current path,
     * and returns that DataValue.
     *
     * @return the DataValue that is found.
     * <br /> null if no DataValue is found.
     * @see Text#compare(java.lang.CharSequence, java.lang.CharSequence)
     */
    private DataValue findDataValue() {
      int nIndex = m_oDataValueHandler.size();
        while (nIndex-- > 0 && Text.compare(m_oDataValueHandler.get(nIndex).m_sPath, m_sPath) != 0) ;
        if (nIndex < 0)
            return null;

        return m_oDataValueHandler.get(nIndex);
  }


    /**
     * Attempts to get the platform for the observation, from
     * {@code PlatformDao}. If the platform is found, the corresponding sensor
     * is retrieved from the {@code Sensors} cache. Finally, the Obs set for the
     * supplied observation type id is retrieved, and the observation is added.
     *
     * @param nObsTypeId   observation type Id.
     * @param nSensorIndex sensor index.
     * @param dValue       value of the observation.
     */
    void createObs(int nObsTypeId, int nSensorIndex, double dValue) {
        String platformCode = null;
        // an observation must have at least a timestamp and platform
        // the sensor index is optional as it defaults to zero
        if (platformCodeSB.length() > 0 && m_lTimestamp > 0L) {
            long now = System.currentTimeMillis();

            platformCode = platformCodeSB.toString().trim();
            IPlatform platform = null;

            int nIndex = m_nContribIds.length; // derive the platform for this observation
            while (nIndex-- > 0 && platform == null)
                platform = platformDao.getPlatformForContribId(m_nContribIds[nIndex], platformCode);

            // if a platform is found, attempt to find the correct sensor
            ISensor iSensor = null;
            if (platform != null) {

                // override default geo-coordinates if not read from file
                if (platform.getCategory() != 'M') {
                    m_nLat = MathUtil.toMicro(platform.getLocBaseLat());
                    m_nLon = MathUtil.toMicro(platform.getLocBaseLong());
                    m_tElev = (short) platform.getLocBaseElev();
                }

                iSensor = sensorDao.getSensor(platform.getId(), nObsTypeId, nSensorIndex);
            }

            // if both a platform and sensor are found then get the obs set
            IObsSet iObsSet = null;
            if (iSensor != null)
                iObsSet = m_oXmlSvc.getObsSet(nObsTypeId);

            // now that we have all three components, save the observation
            if (iObsSet != null)
						{
							m_tElev = m_oCsMgr.checkElev(m_nLat, m_nLon, m_tElev);
              iObsSet.addObs(1, iSensor.getId(), m_lTimestamp, now,
								m_nLat, m_nLon, m_tElev, dValue); // source is 1 for WxDE
            } else {
                int nContribId = -m_oXmlSvc.m_nId; // save the negative value of
                if (m_nContribIds.length == 1) // the collector service for sets
                    nContribId = m_nContribIds[0]; // otherwise, use the contrib id

                long validWindow = ObsMgr.getInstance().getLifeTime();
                long lForecast = now + validWindow;
                long lExpired = now - validWindow;

                if (m_lTimestamp >= lExpired && m_lTimestamp <= lForecast) {

                    // Add the obs in invalidObs table, 1 for WxDE.
                    obsDao.insertInvalidObservation(1, nContribId, platformCode,
                            nObsTypeId, nSensorIndex, new Timestamp(m_lTimestamp),
                            new Timestamp(now), m_nLat, m_nLon, dValue, null);
                }
            }
        }
    }

    /**
     * Convert the latitude attribute {@code m_nLat} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
    void setLat(StringBuilder sBuffer) {
        try {
            m_nLat = MathUtil.toMicro(Double.parseDouble(sBuffer.toString()));
        } catch (Exception e) {
            m_nLat = Integer.MIN_VALUE;
        }
    }


    /**
     * Convert the longitude attribute {@code m_nLon} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
    void setLon(StringBuilder sBuffer) {
        try {
            m_nLon = MathUtil.toMicro(Double.parseDouble(sBuffer.toString()));
        } catch (Exception e) {
            m_nLon = Integer.MIN_VALUE;
        }
    }

    /**
     * Convert the elevation attribute {@code m_tElev} from the double
     * value contained in the supplied string buffer.
     *
     * @param sBuffer The string containing the geo-coordinate position.
     */
    void setElev(StringBuilder sBuffer) {
        try {
            m_tElev = (short) Double.parseDouble(sBuffer.toString());
        } catch (Exception e) {
            m_tElev = Short.MIN_VALUE;
        }
    }

    /**
     * Set the last equipment id {@code m_oLastEquipmentId} to the
     * value contained in the StringBuilder.
     *
     * @param sBuffer The string containing the sensor index integer.
     */
    void setEquipmentId(StringBuilder sBuffer) {
        try {
            m_oLastEquipmentId = sBuffer.toString();
        } catch (Exception oException) {
            m_oLastEquipmentId = null;
        }
    }


    /**
     * Set the sensor index attribute {@code m_nSensorIndex} to the integer
     * value contained in the StringBuilder.
     *
     * @param sBuffer The string containing the sensor index integer.
     */
    public void setSensorIndex(StringBuilder sBuffer) {
        try {
            m_nCurrentSensorIndex = Text.parseInt(sBuffer);
        } catch (Exception oException) {
            m_nCurrentSensorIndex = m_nDefaultSensorIndex;
        }
    }


    /**
     * Appends the Date contained in the string sDate to {@code m_sTimestamp}
     * so that the time can be added later.
     *
     * @param sDate Date to be added.
     */
    public void setDate(String sDate) {
        m_sTimestamp.setLength(0);
        m_sTimestamp.append(sDate);
    }


    /**
     * Set the timestamp attribute {@code m_lTimestamp} to the time
     * value counter contained in the StringBuilder.
     *
     * @param sBuffer The string containing the time value counter.
     */
    public void setTimestamp(StringBuilder sBuffer) {
        try {
            m_sTimestamp.setLength(0);
            m_sTimestamp.append(sBuffer);
            m_lTimestamp = m_oTsFormat.parse(m_sTimestamp.toString()).getTime();
        } catch (Exception oException) {
            m_lTimestamp = 0L;
        }

    }


    /**
     * Set the platform code attribute {@code platformCodeSB}. Hyphenates the
     * value contained in the StringBuilder, and appends it to the
     * platform code attribute.
     *
     * @param sBuffer The string containing the non-hyphenated platform code.
     */
    void setPlatformCode(StringBuilder sBuffer) {
        // insert hyphen for combination systems and platform codes
        if (platformCodeSB.length() > 0)
            platformCodeSB.append("-");

        platformCodeSB.append(sBuffer);
    }


    /**
     * Resets the CurrentDataValue to null so that it doesn't affect reading in
     * values or removing things from the path. It also stores the WriteRecord
     * separately so that we can still keep track of it.
     *
     * @param oWriteRecord the DataValue containing the WriteRecord object.
     */
    void resetCurrentDataValue(DataValue oWriteRecord) {
        m_oCurrentDataValue = null;
        m_oWriteRecord = oWriteRecord;
    }


    /**
     * Creates and/or initializes a {@code ValueHolder} object with the current sensor
     * index, the observation type and the value. Then it stores the
     * {@code ValueHolder} in the {@code ValueSet} array so that it can be
     * written when all the observations have been gathered from this platform.
     *
     * @param nObsType the Observation Type for this observation.
     * @param dValue   the value of this observation.
     */
    void addValueHolder(int nObsType, double dValue) {
        ValueHolder oCurrentValue;
        // if we already have a ValueHolder object stored for reuse
        // then that saves us from creating a new object
        if (m_oValuePool.size() > 0)
            oCurrentValue = m_oValuePool.removeFirst();
        else
            oCurrentValue = new ValueHolder();

        oCurrentValue.init(m_nCurrentSensorIndex, nObsType, dValue);

        m_oValueSet.addLast(oCurrentValue);
    }


    /**
     * A private class used to store the observations so they can be written
     * later, after all the observations have been gathered from the given
     * platform.
     */
    protected class ValueHolder {
        /**
         * Sensor Index for this observation.
         */
        protected int m_nSensorIndex;
        /**
         * Observation type.
         */
        protected int m_nObsType;
        /**
         * Value of this observation.
         */
        protected double m_dValue;

        /**
         * Creates new instances of {@code ValueHolder}. Initialization is done
         * through the {@link ValueHolder#init} method.
         * <p/>
         * <p>
         * May be overriden by classes that extend {@code DataValue}.
         * </p>
         */
        public ValueHolder() {
        }


        /**
         * Initializes this object by storing the sensor index, observation
         * type and value.
         *
         * @param nSensorIndex Sensor Index for this observation.
         * @param nObsType     Observation Type.
         * @param dValue       Value of this observation.
         */
        public void init(int nSensorIndex, int nObsType, double dValue) {
            m_nSensorIndex = nSensorIndex;
            m_nObsType = nObsType;
            m_dValue = dValue;
        }


        /**
         * Calls createObs with the values stored in this object
         * so that the Observation object can be created and stored.
         */
        public void writeData() {
            createObs(m_nObsType, m_nSensorIndex, m_dValue);
        }


        /**
         * Resets all the values in this object.
         */
        public void reset() {
            m_nSensorIndex = 0;
            m_nObsType = 0;
            m_dValue = Double.NEGATIVE_INFINITY;
        }
    }


    /**
     * A private class used to store the {@code DataValue} and {@code nAddCount}
     * at each {@code startElement}. These are stored so that they can be
     * retrieved at each {@code endElement}.
     */
//	protected class PathCount
//	{
    /**
     * the {@code DataValue} found at this startElement (very often null)
     */
//		protected DataValue m_oValue;
    /**
     * the number of characters that need to be removed from the path.
     */
//		protected int m_nAddCount;


    /**
     * Creates the {@code PathCount} object by storing the {@code oValue}
     * and {@code nAddCount}.
     *
     * @param oValue the {@code DataValue} found at this startElement.
     * @param nAddCount the number of characters that need to be removed
     *			from the path.
     */
/*
		public PathCount(DataValue oValue, int nAddCount)
		{
			m_oValue = oValue;
			m_nAddCount = nAddCount;
		}
*/

    /**
     * Allows the AddCount to be retrieved.
     *
     * @return the AddCount stored by this object.
     */
/*
		public int getAddCount()
		{
			return m_nAddCount;
		}
*/

    /**
     * Allows the {@code DataValue} to be retrieved.
     *
     * @return the {@code DataValue} stored by this object.
     */
/*
		public DataValue getDataValue()
		{
			return m_oValue;
		}
*/
//	}

}
