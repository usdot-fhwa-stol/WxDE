<%@page import="org.owasp.encoder.Encode"%>
<%@page contentType="text/html; charset=UTF-8" language="java" import="java.io.*,java.sql.*,java.text.*,java.util.*,javax.sql.*,wde.*,wde.dao.*,wde.emc.*,wde.metadata.IPlatform,wde.qeds.*,wde.util.*" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
    Config oConfig = ConfigSvc.getInstance().getConfig("wde.qeds.QedsMgr");
	String sDataSourceName = 
		oConfig.getString("datasource", "java:comp/env/jdbc/wxde");
	
	String sSubDir = oConfig.getString("subscription", "./");
	if (!sSubDir.endsWith("/")) {
		sSubDir += "/";
	}
	
	DataSource iDataSource =
		WDEMgr.getInstance().getDataSource(sDataSourceName);
	if (iDataSource == null) {
		return;
	}


	java.util.Date oDate = new java.util.Date();
    // oDate.setTime(System.currentTimeMillis() + 31536000000L); // Old = 1 year
    oDate.setTime(System.currentTimeMillis() + 7776000000L); // New = 90 days
    SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    int nSubId = 0;
    int nAttempts = 0;
    boolean bFailed = true;
    String uuid = UUID.randomUUID().toString();
    
	try(Connection iConnection = iDataSource.getConnection();
      PreparedStatement oInsertSubscription = iConnection.prepareStatement("INSERT INTO subs.subscription (id, expires, cycle, name, description, owner_name, ispublic, guid) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
      PreparedStatement oUpdateSubscription = iConnection.prepareStatement("UPDATE subs.subscription SET lat1=?, lng1=?, lat2=?, lng2=?, obsTypeId=?, minValue=?, maxValue=?, qchRun=?, qchFlags=?, format=? WHERE id=?");
      PreparedStatement oInsertObsTypes = iConnection.prepareStatement("INSERT INTO subs.subobs (subid, obstypeid) VALUES (?, ?)");
      PreparedStatement oUpdateRadius = iConnection.prepareStatement("INSERT INTO subs.subRadius (subId, lat, lng, radius) VALUES (?, ?, ?, ?)");
      PreparedStatement oInsertContributor = iConnection.prepareStatement("INSERT INTO subs.subContrib (subId, contribId) VALUES(?, ?)");
      PreparedStatement oInsertStation = iConnection.prepareStatement("INSERT INTO subs.subStation (subId, stationId) VALUES(?, ?)");)
  {

    
    oInsertSubscription.setTimestamp(2, new java.sql.Timestamp(oDate.getTime()));
    oInsertSubscription.setInt(3, oSubscription.m_nCycle);
    oInsertSubscription.setString(4, oSubscription.getName());
    oInsertSubscription.setString(5, oSubscription.getDescription());
    oInsertSubscription.setString(6, request.getRemoteUser());
    oInsertSubscription.setInt(7, ("private".equals(oSubscription.getSubScope()) ? 0 : 1));
    oInsertSubscription.setString(8, uuid);
    nSubId = Subscriptions.getInstance().getNextId();
    java.util.Date oToday = new java.util.Date();
    SimpleDateFormat oFormat = new SimpleDateFormat("yyyyMMdd");
    oFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    if (nSubId - (Integer.parseInt(oFormat.format(oToday)) * 100) > 99)
    {
       request.getRequestDispatcher("/auth/DailySubLimit.jsp").forward(request, response);
    }
    else
    {
        try
        {
            oInsertSubscription.setInt(1, nSubId);
            oInsertSubscription.execute();
        }
        catch (Exception oException)
        {
        }

        // fill in the remainder of the subscription rollup information


        // this region is either set directly or calculated from the point radius
        // the bounding box is not set only when a contributor filter is specified
        if (oSubscription.m_oContribIds.size() == 0)
        {
            oUpdateSubscription.setInt(1, MathUtil.toMicro(oSubscription.m_dLat1));
            oUpdateSubscription.setInt(2, MathUtil.toMicro(oSubscription.m_dLng1));
            oUpdateSubscription.setInt(3, MathUtil.toMicro(oSubscription.m_dLat2));
            oUpdateSubscription.setInt(4, MathUtil.toMicro(oSubscription.m_dLng2));
        }
        else
        {
            oUpdateSubscription.setNull(1, Types.INTEGER);
            oUpdateSubscription.setNull(2, Types.INTEGER);
            oUpdateSubscription.setNull(3, Types.INTEGER);
            oUpdateSubscription.setNull(4, Types.INTEGER);
        }

        // set the observation type
        if(oSubscription.m_nObsTypes == null)
          oUpdateSubscription.setInt(5, 0);
        else
        {
          oUpdateSubscription.setNull(5, Types.INTEGER);
        
            
          
          oInsertObsTypes.setInt(1, nSubId);
          for (int i = 0; i < oSubscription.m_nObsTypes.length; i++)
          {
            oInsertObsTypes.setInt(2, oSubscription.m_nObsTypes[i]);
            oInsertObsTypes.execute();
          }   
        }

        // set the observation value acceptable range
        if (oSubscription.m_dMin == Double.NEGATIVE_INFINITY) {
            oUpdateSubscription.setNull(6, Types.DOUBLE);
        } else {
            oUpdateSubscription.setDouble(6, oSubscription.m_dMin);
        }

        if (oSubscription.m_dMax == Double.POSITIVE_INFINITY) {
            oUpdateSubscription.setNull(7, Types.DOUBLE);
        } else {
            oUpdateSubscription.setDouble(7, oSubscription.m_dMax);
        }

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

        // set the point radius values if they were specified
        if (oSubscription.m_oRadius != null)
        {
            oUpdateRadius.setInt(1, nSubId);
            oUpdateRadius.setInt(2, MathUtil.toMicro(oSubscription.m_oRadius.m_dLat));
            oUpdateRadius.setInt(3, MathUtil.toMicro(oSubscription.m_oRadius.m_dLng));
            oUpdateRadius.setInt(4, MathUtil.toMicro(oSubscription.m_oRadius.m_dRadius));
            oUpdateRadius.execute();
        }

        // populate the contributor filter
        
        oInsertContributor.setInt(1, nSubId);

        Iterator<Integer> oContributor = oSubscription.m_oContribIds.iterator();
        while (oContributor.hasNext())
        {
            oInsertContributor.setInt(2, oContributor.next().intValue());
            oInsertContributor.execute();
        }

        // populate the station filter
        
        oInsertStation.setInt(1, nSubId);

        Iterator<Integer> iterStation = oSubscription.m_oPlatformIds.iterator();
        while (iterStation.hasNext())
        {
            oInsertStation.setInt(2, iterStation.next().intValue());
            oInsertStation.execute();
        }

        // release all database resources
      

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

        oWriter.write("  ObsType  = " + oSubscription.m_nObsTypes);
        if (oSubscription.m_nObsTypes == null) {
            oWriter.write(" (all)");
            }

            oWriter.write("\r\n");

        oWriter.write("  MinValue = " + oSubscription.m_dMin + "\r\n");
        oWriter.write("  MaxValue = " + oSubscription.m_dMax + "\r\n");

        if (oSubscription.m_nObsTypes == null)
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
                            if (nIndex > 0) {
                                    oWriter.write(", ");
                            }

                    Contrib oContrib = oContribs.
                                    getContrib(oSubscription.m_oContribIds.get(nIndex).intValue());
                            if (oContrib != null) {
                        oWriter.write(oContrib.getName());
                    }
            }
        }
        else {
            oWriter.write("none\r\n");
        }

            oWriter.write("\r\n");

        oWriter.write("  StationCodes = ");
        if (oSubscription.m_oPlatformIds.size() > 0)
        {
                    PlatformDao platformDao = PlatformDao.getInstance();
                    for (int nIndex = 0; nIndex < oSubscription.m_oPlatformIds.size(); nIndex++)
            {
                            if (nIndex > 0) {
                                    oWriter.write(", ");
                            }

                    IPlatform iStation = platformDao.
                                    getPlatform(oSubscription.m_oPlatformIds.get(nIndex).intValue());
                            if (iStation != null) {
                        oWriter.write(iStation.getPlatformCode());
                    }
            }
        }
        else {
            oWriter.write("none\r\n");
        }

            oWriter.write("\r\n");

            oWriter.flush();
            oWriter.close();
        }
    }
%>

<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Subscription Results" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
	<style>	
		.container a { color:#006699; text-decoration:none; }
		.container a:hover { text-decoration:underline; }
	</style>
</head>

<body id="dataPage">
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
	
		<h1>Subscription Results</h1>

    	<div class="col-5" style="margin-top:-15px;">
			<h3>Subscription Info</h3>
			<div style="margin-left:10px;"> 
        <b>Name: </b><%= Encode.forHtml( oSubscription.getName()) %><br/>
		        <b>Description: </b><%= Encode.forHtml(oSubscription.getDescription() )%><br/>
				<b>Subscription Identifier: </b><%= nSubId %><br/>
	            <b>Expires On: </b><%= oDateFormat.format(oDate) %><br/>
		        <b>Direct URL: </b><%= request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + "/downloadSubscription?uuid=" + uuid + "&amp;file=&lt;filename&gt;" %><br/>
			</div>
		</div>
		
		<div id="instructions" class="col-4" style="margin:0; margin-top:-15px;">
			<h3>Instructions</h3>
			Your subscription was created successfully.
			<br><br>
			Your subscription is scheduled to expire in 14 days. 
			However, the expiration date is automatically renewed 
			every time the subscription is accessed. 
			<br><br>
			Subscription fulfillment occurs at the interval you selected, 
			starting at the top of the hour. Please note, however, 
			that it may take up to one interval before your first 
			set of observations is available. 
			<br><br>
			The direct access URL can be used by automated processes you create 
			to regularly download your subscription results. The &lt;filename&gt; 
			needs to be replaced with a file name following a yyyyMMdd_HHmm.ext 
			pattern. Substitute your selected subscription time and format 
			parameters in the placeholder positions. For example, 20160511_1935.csv. 
			The minute position before the file type extension should match your 
			subscription interval, i.e. 05, 10, 15, 20, etc. for a five-minute 
			interval; 10, 20, 30, 40, etc. for a 10-minute interval, and so forth.
			<br> 
	    </div>
	    
		<div class='clearfix'></div>
		
		<br>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
