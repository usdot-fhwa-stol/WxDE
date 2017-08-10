<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ page import="java.io.*, java.text.*, java.util.*" %>
<%
	response.setHeader("Cache-Control","no-cache,no-store,must-revalidate");//HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
 %>
<!DOCTYPE html>
<html>
	<head>

	    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
	    	<jsp:param value="Data Source" name="title"/>
	    </jsp:include>
		<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
		
		<script type="text/javascript" src="/vendor/downloadify/js/swfobject.js"></script>
		<script type="text/javascript" src="/vendor/downloadify/js/downloadify.min.js"></script>
    
		<!-- Custom CSS stylesheet for the Reports Table -->
		<link href="/style/reports-table.css" rel="stylesheet" media="all">
		
	</head>
	<body id="dataPage" class="datasource">
		<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
		<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
		
		<div id="pageBody" class="container">
			<h1>
				Data Source
				<span class="pull-right" id="saveDataSource" style="width: 120px"></span>
				<img id="loading" style="display: none; height: 22px; width: 22px; opacity: .5" src="/image/loading-dark.gif" alt="loading">
			</h1>
			
			<p><em>* Click column headers containing vertically aligned carets (<img src="/image/icons/dark/fa-sort.png" alt="Sort Icon" />) to sort.</em></p>
			
			<p><a href="<%= response.encodeURL("/resources/download")%>" download="download.txt">Click Here</a></p>
			<table class="reports-table jsonData" id="sortable-table">
				<caption style="display: none;">Reports</caption>
			  <thead>
				<tr>
				  <th id="sort-name" class="sortable">
				    Data Source
				    <img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />
				  </th>
				  <th id="sort-agency" class="sortable">
				  	Agency
				    <img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />
				  </th>
				  <th id="" class="">Disclaimer</th>
				  <th id="" class="">Stations</th>
				</tr>
			  </thead>
			  
			  <tbody>
			  </tbody>
			</table>
			
		</div>	
		
		<div id="descriptionModal" title="View Feedback" style="display:none;">
			<p id="descriptionWriter" style="margin-left: 20px">
			</p>
		</div>	
		
		<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
		
		<script src="/script/dataSource.js" type="text/javascript"></script>
		<script src="/script/simpleSorter.js" type="text/javascript"></script>

	</body>
</html>
