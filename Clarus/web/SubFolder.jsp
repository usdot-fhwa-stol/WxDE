<%@page contentType="text/html; charset=iso-8859-1" language="java" import="java.io.*,java.sql.*,java.text.*,java.util.*,javax.sql.*,clarus.*,util.*" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<%
    if (!oSubscription.checkSecurity())
        request.getRequestDispatcher("getPassword.jsp").forward(request, response);

	Config oConfig = ConfigSvc.getInstance().getConfig("clarus.qeds.QedsMgr");
	String sDataSourceName =
		oConfig.getString("datasource", "java:comp/env/jdbc/clarus_subs");

//	String sSubDir = "\\\\clarus1\\subscriptions\\";
	String sSubDir = oConfig.getString("subscription", "./");
	if (!sSubDir.endsWith("/"))
		sSubDir += "/";

    DecimalFormat oFormatter = new DecimalFormat("#,###");
    File oDir = new File(sSubDir + "/" + request.getParameter("subId"));
    
    StringBuilder sbReadme = new StringBuilder();
    
    ArrayList<File> oFiles = new ArrayList<File>();
    if (oDir.exists())
    {
        File[] oFileList = oDir.listFiles();
        for (int i = 0; i < oFileList.length; i++)
        {
            if (!oFileList[i].getName().equals("README.txt"))
                oFiles.add(oFileList[i]);
        }
        Collections.sort(oFiles);

        // Read in the README.txt file, if there is one.
        try
        {
            BufferedReader oReader = new BufferedReader(new FileReader(oDir + "/README.txt"));
            String sLine;
            while ((sLine = oReader.readLine()) != null)
                sbReadme.append(sLine + "<br/>");

			oReader.close();
        }
        catch(Exception oException)
        {
        }

        // If a user goes into a subscription's directory, then set the expiration
        // date for 90 days from now.
        java.util.Date oDate = new java.util.Date();
//        oDate.setTime(System.currentTimeMillis() + 7776000000L); // New = 90 days
        oDate.setTime(System.currentTimeMillis() + 1209600000L); // New = 2 weeks

		DataSource iDataSource =
			ClarusMgr.getInstance().getDataSource(sDataSourceName);
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
    else
        request.getRequestDispatcher("missingResource.jsp?resource=Subscription Directory&id=" + request.getParameter("subId")).forward(request, response);
%>

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Subscription <%= request.getParameter("subId") %></title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/SubFolder.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
  </head>
  
  <body onload="CreateClock(false);">
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Subscribed Observations</div>
        <div id="titleText2Shadow">Subscribed Observations</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Subscription: <%= request.getParameter("subId") %></h3>
          <%= sbReadme.toString() %>
        </div>
      </div>

      <div id="linkArea2">
        <br/>
        <table id="tblFiles">
          <tr>
            <th>Observations</th>
            <th>Size (bytes)</th>
          </tr>
<%
	int nIndex = oFiles.size();
	while (nIndex-- > 0)
    {
		File oFile = oFiles.get(nIndex);
%>
          <tr>
            <td><a href="SubShowObs.jsp?subId=<%= request.getParameter("subId") + "&file=" + oFile.getName() %>"><%= oFile.getName() %></a></td>
            <td class="fileSize"><%= oFormatter.format(oFile.length()) %></td>
          </tr>
<%
    }
%>
        </table>
      </div>
    </div> <!-- container -->
    
  </body>
</html>
