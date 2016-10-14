<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
	<head>

	    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
	    	<jsp:param value="Stations" name="title"/>
	    </jsp:include>
		<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
    
		<!-- Custom CSS stylesheet for the Reports Table -->
		<link href="/style/reports-table.css" rel="stylesheet" media="all">
	    	
	    <style type="text/css">
	    	.reports-table th {
	    		padding:5px 10px;
	    	}
	    	.container h1 {
	    		margin-bottom: 8px;
	    	}
	    	.container p {
	    		margin-top: 7px;
	    	}
	    </style>
	</head>
	<body id="dataPage">
		<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
		<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>
		
		<div class="container">
			<h1>
				<span id="sourceName"></span> Stations
				<span class="pull-right" id="saveStations"></span>
				<img id="loading" style="display: none; height: 22px; width: 22px; opacity: .5" src="/image/loading-dark.gif">
			</h1>
			
			<a href="<%= response.encodeURL("/auth2/dataSource.jsp")%>" class="btn-light"
				style="margin-left: 0px; display:block; text-decoration: none; max-width: 100px; padding: 5px 10px; font-size: 14px; text-align:center;">
					<img src="/image/icons/dark/fa-arrow-left.png" alt="Data Source Icon" style="margin-bottom: -2px;" />
					Data Source
			</a>
			
			<p><em>* Click column headers containing vertically aligned carets (<img src="/image/icons/dark/fa-sort.png" alt="Sort Icon" />) to sort.</em></p>
			
			<table class="reports-table" id="sortable-table">
				<caption style="display: none;">Reports</caption>
			  <thead>
				<tr>
				  <th id="sort-code" class="sortable">
				  	Station Code
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />
				  </th>
				  <th id="sort-category" class="sortable">
				  	Category
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />				  	
				  </th>
				  <th id="sort-description" class="sortable">
				  	Description
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />				  	
				  </th>
				  <th id="sort-latitude" class="sortable">
				  	Latitude
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />				  	
				  </th>
				  <th id="sort-longitude" class="sortable">
				  	Longitude
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />				  	
				  </th>
				  <th id="sort-elevation" class="sortable">
				  	Elevation
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />				  	
				  </th>
				  <!--
				  <th id="sort-update" class="sortable">
				  	Update Time
				  	<img src="/image/icons/light/fa-sort.png" alt="Sort Icon" style="margin-bottom: -2px;" />				  	
				  </th>
				  -->
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
		
		<script type="text/javascript" src="/vendor/downloadify/js/swfobject.js"></script>
		<script type="text/javascript" src="/vendor/downloadify/js/downloadify.min.js"></script>
		
		<script src="/script/platforms.js" type="text/javascript"></script>
		<script src="/script/simpleSorter.js" type="text/javascript"></script>
	</body>
</html>