<%@page contentType="text/plain; charset=iso-8859-1" language="java" import="java.io.*,java.sql.*,java.text.*,java.util.*,javax.sql.*,wde.*,wde.util.*" 
%><jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" 
/><jsp:setProperty name="oSubscription" property="*" 
/><%
   	Config oConfig = ConfigSvc.getInstance().getConfig("wde.qeds.QedsMgr");
//	String sSubDir = "\\\\clarus1\\subscriptions\\";
	String sSubDir = oConfig.getString("subscription", "./");
	if (!sSubDir.endsWith("/")) {
		sSubDir += "/";
	}

	String subId = request.getParameter("subId");
	if (!SubscriptionHelper.isAuthorized(request.getRemoteUser(), subId)) {
		response.sendError(401, "Unauthorized!" );
	}

    try
    {
        BufferedReader oReader = new BufferedReader(
			new FileReader(sSubDir + subId + "/" +
			request.getParameter("file")));
		
        String sLine;
        while ((sLine = oReader.readLine()) != null) {
            out.write(sLine + "\r\n");
		}

		oReader.close();
    }
    catch(Exception oException)
    {
        String sFilename = subId + "/" + request.getParameter("file");
        request.getRequestDispatcher("missingResource.jsp?resource=Subscription File&id=" + sFilename).forward(request, response);
    }
    
    try
    {
		String sDataSourceName =
			oConfig.getString("datasource", "java:comp/env/jdbc/wxde");
	
		// update the subscription to two weeks from now when a subscription is downloaded
		java.util.Date oDate = new java.util.Date();
		oDate.setTime(System.currentTimeMillis() + 1209600000L); // 2 weeks
	
		DataSource iDataSource =
		 	WDEMgr.getInstance().getDataSource(sDataSourceName);
		if (iDataSource == null) {
		 	return;
		}
	
		Connection iConnection = iDataSource.getConnection();
		if (iConnection == null) {
			return;
		}
	
		PreparedStatement oUpdateSubscription = iConnection.prepareStatement("UPDATE subs.subscription SET expires=? WHERE id=?");
		oUpdateSubscription.setTimestamp(1, new java.sql.Timestamp(oDate.getTime()));
		oUpdateSubscription.setInt(2, Integer.parseInt(request.getParameter("subId")));
		oUpdateSubscription.execute();
		oUpdateSubscription.close();
		iConnection.close();
    }
    catch (Exception oException)
    {
    	oException.printStackTrace();
    }
%>
