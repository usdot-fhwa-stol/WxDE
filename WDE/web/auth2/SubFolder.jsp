<%@page import="ucar.ma2.ForbiddenConversionException"%>
<%@page import="javax.xml.ws.http.HTTPException"%>
<%@page contentType="text/html; charset=UTF-8" language="java" import="java.io.*,java.sql.*,java.text.*,java.util.*,javax.sql.*,wde.*,wde.util.*" %>
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
	
	String subId = request.getParameter("subId");
    DecimalFormat oFormatter = new DecimalFormat("#,###");
    File oDir = new File(sSubDir + "/" + subId);
    
	if (!SubscriptionHelper.isAuthorized(request.getRemoteUser(), subId)) {
		response.sendError(401, "Unauthorized!" );
	}

    StringBuilder sbReadme = new StringBuilder();
    
    ArrayList<File> oFiles = new ArrayList<File>();
    if (oDir.exists())
    {
        File[] oFileList = oDir.listFiles();
        for (int i = 0; i < oFileList.length; i++)
        {
		    if (!oFileList[i].getName().equals("README.txt")) {
		        oFiles.add(oFileList[i]);
			}
        }
        Collections.sort(oFiles);

        // Read in the README.txt file, if there is one.
        try
        {
		    BufferedReader oReader = new BufferedReader(new FileReader(oDir + "/README.txt"));
		    String sLine;
		    while ((sLine = oReader.readLine()) != null) {
	        	sbReadme.append(sLine + "<br/>");
	        }

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

    } else {
        request.getRequestDispatcher("missingResource.jsp?resource=Subscription Directory&id=" + request.getParameter("subId")).forward(request, response);
    }
%>
<html>
<head>

	<% String subTitle = "Subscription " + request.getParameter("subId"); %>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="<%=subTitle%>" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
	<!-- Page specific JavaScript -->
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
	
	<style>
		.container a {
			color:#006699;
			text-decoration:none;
		}
		.container a:hover {
			text-decoration:underline;
		}
		.fileSize {
			text-align: right;
		}
		#tblFiles {
			margin-top: 13px;
		}
		#tblFiles td {
			height: 25px;
			border: solid 1px #ddd;
			padding: 5px;
		}
		.dataHeaders {
			font-size: 1.17em;
			font-weight: bold;
			padding-bottom: 25px;
			min-width: 120px;
		}
	</style>
	<script type="text/javascript">
		$(document).ready(function() {
			$.ajax({
	            url:'/resources/auth/subscriptions/<%=request.getParameter("subId")%>', 
	            dataType: 'json',
	            success: function(resp) {
					$("#hName").text(resp.name);
					$("#hDescription").text(resp.description);
					$("#hUuid").text(resp.uuid);
	            }
	        });
		});
	</script>
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
		<h1>Subscriptions</h1>
		<br/>
		
    	<div class="col-5" style="margin-top:-15px;">
        <table id="tblFiles">
			<caption style="display: none;">Files</caption>
          <tr>
            <th class="dataHeaders">Observations</th>
            <th class="dataHeaders">Size (bytes)</th>
          </tr>
<%
	int nIndex = oFiles.size();
	while (nIndex-- > 0)
    {
		File oFile = oFiles.get(nIndex);
%>
          <tr>
            <td><a href="SubShowObs.jsp?subId=<%= request.getParameter("subId") + "&file=" + oFile.getName() %>" target="_blank"><%= oFile.getName() %></a></td>
            <td class="fileSize"><%= oFormatter.format(oFile.length()) %></td>
          </tr>
<%
    }
%>
        </table>
			<br></br>
		</div>
		
		<div id="instructions" class="col-4" style="margin:0; margin-top:-15px;">
          <h3>Subscription: <%= request.getParameter("subId") %></h3>
		       Name = <label id="hName"></label><br>
		       Description = <label id="hDescription"></label><br>
		       UUID = <label id="hUuid"></label><br>
          <%= sbReadme.toString() %>
			<div id="statusMessage" class="msg" style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
				<!-- <img src="/image/close-img.svg" id="close-msg" style="float:right; cursor: pointer; margin-top:2px;" /> -->
			</div>
	    </div>
		<div class='clearfix'></div>
		<br>
		
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
</body>
</html>
