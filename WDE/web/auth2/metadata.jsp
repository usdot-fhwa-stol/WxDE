<%@page contentType="text/html; charset=UTF-8" language="java" import="java.io.*,java.text.*,java.util.*,wde.util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<% 
    response.setHeader("Cache-Control","no-cache,no-store,must-revalidate");//HTTP 1.1
    response.setHeader("Pragma","no-cache"); //HTTP 1.0
    response.setDateHeader ("Expires", 0); //prevents caching at the proxy server

	Config oConfig = ConfigSvc.getInstance().getConfig("wde.ems.EmsMgr");

	java.util.Date oDate = new java.util.Date();
    SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    
    File oDir = new File(oConfig.getString("metadata", null));
    File[] oFileList = oDir.listFiles();
    ArrayList<File> oFiles = new ArrayList<File>();
    for (int i = 0; i < oFileList.length; i++)
    {
        if (oFileList[i].getName().indexOf(".csv") > 0 || oFileList[i].getName().indexOf(".nc") > 0)
            oFiles.add(oFileList[i]);
    }
    Collections.sort(oFiles);
%>
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Metadata" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
	<style>
		#tblFolders * {
			font-size: 14px;
		}
		#tblFolders tr {
			border-bottom: solid 1px #D8D8D8;
		}
		.fileSize {
			text-align: center;
		}
	</style>
	
</head>

<body onunload="" id="dataPage" class="metadata-page">

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div id="pageBody" class="container">
		<h1>Metadata Files</h1>
		
		<div class="row">
			<div id="instructions" class="col-12" style="padding: 0 10px;">
				<p>
					The Weather Data Environment <strong>metadata files</strong> contain a textual representation
					of the metadata contained within the database.
					The files are formatted as CSV (comma-separated value) files, allowing them
					to be imported easily into third-party analysis tools.  Among the list of files are road segment
					definition files in the Network Common Data Form (NetCDF) format, which requires a special tool such
					as Panoply to view.  A CSV formatted version is also included for easy viewing.
				</p>
		    </div>
	    </div>

		<a href="#" class="skip-link logical-placement" data-skip="skip-metadata-files">Skip over Metadata Files</a>

		<div class="row">
	    	<div class="col-12">
	
		        <table id="tblFolders" cellpadding="10">
					<caption style="display: none;">Metadata</caption>
		          <tr>
		            <th>Metadata Files</th>
		            <th>Last Update (UTC)</th>
		            <th></th>
		          </tr>
		<%
		    Iterator<File> oIter = oFiles.iterator();
		    File oFile;
		    String sDate;
		    while (oIter.hasNext())
		    {
		        oFile = oIter.next();
		        oDate.setTime(oFile.lastModified());
		        sDate = oDateFormat.format(oDate);
		%>
		          <tr>
		            <td>
		            	<a href="ShowMetadata.jsp?file=<%= oFile.getName() %>" class="link" title="Click to view"><%= oFile.getName() %></a>
	            	</td>
		            <td class="fileSize"><%= sDate %></td>
		            <td style="padding-top: 0;">
		            	<a href="/auth2/metadata/<%= oFile.getName() %>" class="btn btn-dark" id="downloadFile">
							<img src="/image/icons/light/fa-download.png" alt="Download Icon" style="margin-bottom: -1px" /> 
							Download</a>
	            	</td>
		          </tr>
		<%
		    }
		%>
		        </table>
			</div>
		</div>

		<a href="#" id="skip-metadata-files" tabindex="-1"></a>

		<div class='clearfix'></div>
		<br>
		
		</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>
