<%@page contentType="text/plain; charset=iso-8859-1" language="java" import="java.io.*,java.sql.*,java.text.*,java.util.*,javax.sql.*,clarus.*,util.*" 
%><jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" 
/><jsp:setProperty name="oSubscription" property="*" 
/><%
    if (!oSubscription.checkSecurity())
        request.getRequestDispatcher("getPassword.jsp").forward(request, response);
    
   	Config oConfig = ConfigSvc.getInstance().getConfig("clarus.qeds.QedsMgr");
//	String sSubDir = "\\\\clarus1\\subscriptions\\";
	String sSubDir = oConfig.getString("subscription", "./");
	if (!sSubDir.endsWith("/"))
		sSubDir += "/";

    try
    {
        BufferedReader oReader = new BufferedReader(
			new FileReader(sSubDir + request.getParameter("subId") + "/" +
			request.getParameter("file")));
		
        String sLine;
        while ((sLine = oReader.readLine()) != null)
            out.write(sLine + "\r\n");

		oReader.close();
    }
    catch(Exception oException)
    {
        String sFilename = request.getParameter("subId") + "/" + request.getParameter("file");
        request.getRequestDispatcher("missingResource.jsp?resource=Subscription File&id=" + sFilename).forward(request, response);
    }
    
    try
    {
			String sDataSourceName =
				oConfig.getString("datasource", "java:comp/env/jdbc/clarus_subs");

			// update the subscription to two weeks from now when a subscription is downloaded
			java.util.Date oDate = new java.util.Date();
			oDate.setTime(System.currentTimeMillis() + 1209600000L); // 2 weeks

			DataSource iDataSource = ClarusMgr.getInstance().getDataSource(sDataSourceName);
			if (iDataSource == null)
				return;

			Connection iConnection = iDataSource.getConnection();
			if (iConnection == null)
				return;

			PreparedStatement oUpdateSubscription = iConnection.prepareStatement("UPDATE subscription SET expires=? WHERE id=?");
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
