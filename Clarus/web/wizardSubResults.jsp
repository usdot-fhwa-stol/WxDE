<%@page contentType="text/html; charset=iso-8859-1" language="java" import="java.io.*,java.sql.*,java.text.*,java.util.*,javax.sql.*,clarus.*,clarus.emc.*,clarus.qeds.*,util.*" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%
	Config oConfig = ConfigSvc.getInstance().getConfig("clarus.qeds.QedsMgr");
	String sDataSourceName = 
		oConfig.getString("datasource", "java:comp/env/jdbc/clarus_subs");
	
	String sSubDir = oConfig.getString("subscription", "./");
	if (!sSubDir.endsWith("/"))
		sSubDir += "/";

	DataSource iDataSource =
		ClarusMgr.getInstance().getDataSource(sDataSourceName);
	if (iDataSource == null)
		return;

	Connection iConnection = iDataSource.getConnection();
	if (iConnection == null)
		return;

	java.util.Date oDate = new java.util.Date();
    // oDate.setTime(System.currentTimeMillis() + 31536000000L); // Old = 1 year
    oDate.setTime(System.currentTimeMillis() + 7776000000L); // New = 90 days
    SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    int nSubId = 0;
    int nAttempts = 0;
    boolean bFailed = true;

    PreparedStatement oInsertSubscription = iConnection.prepareStatement("INSERT INTO subscription (id, expires, password, cycle, contactName, contactEmail) VALUES (?, ?, ?, ?, ?, ?)");
    oInsertSubscription.setTimestamp(2, new java.sql.Timestamp(oDate.getTime()));
    oInsertSubscription.setString(3, oSubscription.m_sSecret);
    oInsertSubscription.setInt(4, oSubscription.m_nCycle);
    oInsertSubscription.setString(5, oSubscription.m_sContactName);
    oInsertSubscription.setString(6, oSubscription.m_sContactEmail);
    
    // Generate a subscription ID based on today's date with an
    // index going from 00 to 99.
    // NOTE: This only allows 100 subscriptions to be created in one day.
    java.util.Date oToday = new java.util.Date();
    SimpleDateFormat oFilenameFormat = new SimpleDateFormat("yyyyMMdd");
    oFilenameFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    while (bFailed && nAttempts < 100)
    {
        nSubId = Integer.parseInt(oFilenameFormat.format(oToday)) * 100 + nAttempts;
        try
        {
            oInsertSubscription.setInt(1, nSubId);
            oInsertSubscription.execute();
            bFailed = false;
        }
        catch (Exception oException)
        {
            bFailed = true;
        }
        
        nAttempts++;
    }
    
    oInsertSubscription.close();
    
    // fill in the remainder of the subscription rollup information
    PreparedStatement oUpdateSubscription = iConnection.prepareStatement("UPDATE subscription SET lat1=?, lng1=?, lat2=?, lng2=?, obsTypeId=?, minValue=?, `maxValue`=?, qchRun=?, qchFlags=?, format=? WHERE id=?");

    // this region is either set directly or calculated from the point radius
    // the bounding box is not set only when a contributor filter is specified
    if (oSubscription.m_oContribIds.size() == 0)
    {
        oUpdateSubscription.setInt(1, Stations.toMicro(oSubscription.m_dLat1));
        oUpdateSubscription.setInt(2, Stations.toMicro(oSubscription.m_dLng1));
        oUpdateSubscription.setInt(3, Stations.toMicro(oSubscription.m_dLat2));
        oUpdateSubscription.setInt(4, Stations.toMicro(oSubscription.m_dLng2));
    }
    else
    {
        oUpdateSubscription.setNull(1, Types.INTEGER);
        oUpdateSubscription.setNull(2, Types.INTEGER);
        oUpdateSubscription.setNull(3, Types.INTEGER);
        oUpdateSubscription.setNull(4, Types.INTEGER);
    }
  
    // set the observation type
    oUpdateSubscription.setInt(5, oSubscription.m_nObsType);
  
    // set the observation value acceptable range
    if (oSubscription.m_dMin == Double.NEGATIVE_INFINITY)
        oUpdateSubscription.setNull(6, Types.DOUBLE);
    else
        oUpdateSubscription.setDouble(6, oSubscription.m_dMin);
  
    if (oSubscription.m_dMax == Double.POSITIVE_INFINITY)
        oUpdateSubscription.setNull(7, Types.DOUBLE);
    else
        oUpdateSubscription.setDouble(7, oSubscription.m_dMax);
  
    // set the quality checking flags
    if (oSubscription.m_nRun == 0)
    {
        oUpdateSubscription.setNull(8, Types.INTEGER);
        oUpdateSubscription.setNull(9, Types.INTEGER);
    }
    else
    {
        oUpdateSubscription.setInt(8, oSubscription.m_nRun);
        oUpdateSubscription.setInt(9, oSubscription.m_nFlag);
    }

    // update the subscription record
    oUpdateSubscription.setString(10, oSubscription.m_sOutputFormat);
    oUpdateSubscription.setInt(11, nSubId);
    oUpdateSubscription.execute();
    oUpdateSubscription.close();  
  
    // set the point radius values if they were specified
    if (oSubscription.m_oRadius != null)
    {
        PreparedStatement oUpdateRadius = iConnection.prepareStatement("INSERT INTO subRadius (subId, lat, lng, radius) VALUES (?, ?, ?, ?)");
        oUpdateRadius.setInt(1, nSubId);
        oUpdateRadius.setInt(2, Stations.toMicro(oSubscription.m_oRadius.m_dLat));
        oUpdateRadius.setInt(3, Stations.toMicro(oSubscription.m_oRadius.m_dLng));
        oUpdateRadius.setInt(4, Stations.toMicro(oSubscription.m_oRadius.m_dRadius));
        oUpdateRadius.execute();
        oUpdateRadius.close();  
    }
  
    // populate the contributor filter
    PreparedStatement oInsertContributor = iConnection.prepareStatement("INSERT INTO subContrib (subId, contribId) VALUES(?, ?)");
    oInsertContributor.setInt(1, nSubId);
  
    Iterator<Integer> oContributor = oSubscription.m_oContribIds.iterator();
    while (oContributor.hasNext())
    {
        oInsertContributor.setInt(2, oContributor.next().intValue());
        oInsertContributor.execute();
    }
    oInsertContributor.close();
  
    // populate the station filter
    PreparedStatement oInsertStation = iConnection.prepareStatement("INSERT INTO subStation (subId, stationId) VALUES(?, ?)");
    oInsertStation.setInt(1, nSubId);
  
    Iterator<Integer> iterStation = oSubscription.m_oStationIds.iterator();
    while (iterStation.hasNext())
    {
        oInsertStation.setInt(2, iterStation.next().intValue());
        oInsertStation.execute();
    }
    oInsertStation.close();
  
    // release all database resources
    iConnection.close();
  
    // create the directory where the subscriptions will be delivered
    File oDir = new File(sSubDir + nSubId);
    oDir.mkdirs();
    
    String sUrl = "SubFolder.jsp?subId=" + nSubId;

    // Create the file containing the subscription information
    java.util.Date oDateToday = new java.util.Date();
    FileWriter oWriter = new FileWriter(new File(oDir.toString() + "/README.txt"));
    oWriter.write("\r\nSubscription Information:\r\n");
    oWriter.write("  DateCreated = " + oDateFormat.format(oDateToday) + "\r\n");
    
    if (oSubscription.m_dLat1 != -Double.MAX_VALUE)
    {
        oWriter.write("  Lat1 = " + oSubscription.m_dLat1 + "\r\n");
        oWriter.write("  Lon1 = " + oSubscription.m_dLng1 + "\r\n");
        oWriter.write("  Lat2 = " + oSubscription.m_dLat2 + "\r\n");
        oWriter.write("  Lon2 = " + oSubscription.m_dLng2 + "\r\n");
    }
    else
    {
        oWriter.write("  Lat1 = not used\r\n");
        oWriter.write("  Lon1 = not used\r\n");
        oWriter.write("  Lat2 = not used\r\n");
        oWriter.write("  Lon2 = not used\r\n");
    }

    if (oSubscription.m_oRadius != null)
    {
        oWriter.write("  PointRadiusLat    = " + oSubscription.m_oRadius.m_dLat + "\r\n");
        oWriter.write("  PointRadiusLon    = " + oSubscription.m_oRadius.m_dLng + "\r\n");
        oWriter.write("  PointRadiusRadius = " + oSubscription.m_oRadius.m_dRadius + "\r\n");
    }
    else
    {
        oWriter.write("  PointRadiusLat    = not used" + "\r\n");
        oWriter.write("  PointRadiusLon    = not used" + "\r\n");
        oWriter.write("  PointRadiusRadius = not used" + "\r\n");
    }

    oWriter.write("  ObsType  = " + oSubscription.m_nObsType);
    if (oSubscription.m_nObsType == 0)
        oWriter.write(" (all)");

	oWriter.write("\r\n");
    
    oWriter.write("  MinValue = " + oSubscription.m_dMin + "\r\n");
    oWriter.write("  MaxValue = " + oSubscription.m_dMax + "\r\n");

    if (oSubscription.m_nObsType == 0)
    {
        oWriter.write("  RunFlags    = not applicable\r\n");
        oWriter.write("  PassNotPass = not applicable\r\n");
    }
    else
    {
        oWriter.write("  RunFlags    = " + oSubscription.m_nRun + " (" + Integer.toString(oSubscription.m_nRun, 2) + ")\r\n");
        oWriter.write("  PassNotPass = " + oSubscription.m_nFlag + " (" + Integer.toString(oSubscription.m_nFlag, 2) + ")\r\n");
    }

    oWriter.write("  Contributors = ");
    if (oSubscription.m_oContribIds.size() > 0)
    {
		Contribs oContribs = Contribs.getInstance();
		for (int nIndex = 0; nIndex < oSubscription.m_oContribIds.size(); nIndex++)
        {
			if (nIndex > 0)
				oWriter.write(", ");

            Contrib oContrib = oContribs.
				getContrib(oSubscription.m_oContribIds.get(nIndex).intValue());
			if (oContrib != null)
	            oWriter.write(oContrib.getName());
        }
    }
    else
        oWriter.write("none\r\n");

	oWriter.write("\r\n");

    oWriter.write("  StationIds = ");
    if (oSubscription.m_oStationIds.size() > 0)
    {
		Stations oStations = Stations.getInstance();
		for (int nIndex = 0; nIndex < oSubscription.m_oStationIds.size(); nIndex++)
        {
			if (nIndex > 0)
				oWriter.write(", ");

            IStation iStation = oStations.
				getStation(oSubscription.m_oStationIds.get(nIndex).intValue());
			if (iStation != null)
	            oWriter.write(iStation.getCode());
        }
    }
    else
        oWriter.write("none\r\n");

	oWriter.write("\r\n");

	oWriter.flush();
	oWriter.close();
%>

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Subscription Results</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardSubResults.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
  </head>
  
  <body onload="CreateClock(false);">
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Subscription Results</div>
        <div id="titleText2Shadow">Subscription Results</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Subscription Results</h3>
          Your subscription was created successfully.
          The subscription identifier and password will allow you to edit your
          subscription information as needed.
          Please keep both the subscription identifier and password in a safe place.
          <br/>
          <br/>
          Your subscription is scheduled to expire in 14 days.
          However, the expiration date is automatically renewed every time the
          subscription is accessed.
          <br/>
          <br/>
          Subscription fulfillment occurs at the interval you selected, 
	  starting at the top of the hour.
          Please note, however, that it may take up to one interval before your 
	  first set of observations are available.
          <br/>
          <br/>
          You may save the provided URL as a browser favorite link to make it easy to
          retrieve your subscription files.
        </div>
      </div>

      <div id="linkArea2">
        <br/>
        <table id="tblTossItems">
          <tr>
            <th>Subscription Identifier:</th>
            <td><%= nSubId %></td>
          </tr>
          <tr>
            <th>Password:</th>
            <td><%= oSubscription.m_sSecret %></td>
          </tr>
          <tr>
            <th>Expires On:</th>
            <td><%= oDateFormat.format(oDate) %></td>
          </tr>
        </table>
        
        <table id="tblURL">
          <tr>
            <th>URL:</th>
          </tr>
          <tr>
            <td><a href="<%= sUrl %>">http://www.clarus-system.com/<%= sUrl %></a></td>
          </tr>
</table>      </div>
    </div> <!-- container -->
    
  </body>
</html>
