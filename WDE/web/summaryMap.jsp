<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html lang="en-US">
<head>

<jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
	<jsp:param value="Summary Map" name="title" />
</jsp:include>
<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>

<!-- jQuery-loader includes -->
<link href="/script/jquery-loader/jquery.loader.css" rel="stylesheet" />
<script type="text/javascript"
	src="/script/jquery-loader/jquery.loader.js"></script>

<script type="text/javascript"
	src="https://maps.google.com/maps/api/js?sensor=false"></script>
	
<style type="text/css">
	#map-canvas {
		height: 700px;
		width: 900px;
	}
	
	#tooltip {
		position: absolute;
		z-index: 98;
		width: 200px;
		height: 40px;
		background: #333;
		display: none;
	}
	
	.legends {
		background-color: #FFF;
		margin-left: 20px;
		top: -230px;
	}
	
	.legends2 {
	    border: 1px solid #000000;
	    border-radius: 2px;
	    float: left;
	    font-size: 1em;
	    margin-bottom: -100px;
	    padding: 10px;
	    position: relative;
	    text-align: left;
	    top: -100px;
	    width: 290px;
		background-color: #FFF;
		margin-left: 240px;
		top: -230px;
		height: 168px;
	}
	
	.footer {
		clear: both;
		top: -72px;
	}
	.mask {
		position: absolute;
		background: rgba(0, 0, 0, .5);
		height: 100%;
		width: 100%;
		z-index: 998;
		display: none;
	}
	.mask h2 {
		color: #FFF;
		display: block;
		margin: auto;
		z-index: 999;
	}
	#linkArea2 {
		width: 900px;
		margin-left: auto;
		margin-right: auto;
		border: 1px solid #aaa;
	}
	.accessible-map {
		position: absolute;
		left: -9999px;
	}
	.accessible-map:focus {
		position: relative;
		left: 0;
		float: right;
		margin: 20px;
		font-size: 14px;
	}
	#loading {
		display: none; 
		height: 22px; 
		width: 22px; 
		opacity: .5
	}
</style>

	<!-- CSS stylesheet and hacks for IE version 8 and lower -->
	<!--[if lte IE 8]>
        <style>
        	.footer {
        		top: -58px;
       		}
      		</style>
    <![endif]-->

</head>

<body id="dataPage" class="summary-page">

	<div class="mask">
		<h2>Loading Map</h2>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div id="pageBody" class="container datamap-container">
		
		<select id="statesList" class="accessible-map" title="States"></select>
		
		<h1>
			Summary Map
			<img id="loading" src="/image/loading-dark.gif" alt="loading" />
		</h1>

		<div id="linkArea2">

			<div id="map-canvas"></div>
			<!-- Begin iMapbuilder Code-->
			<!-- 			<script type="text/javascript" src="http://api.imapbuilder.net/1.0/?map=28492&s=b0c59975370b989b9274f7fdf45f2474"></script> -->
			<!-- End iMapbuilder Code-->
			<div id="legalese" class='legends'>
				
				<div>
					The Prototype Weather Data Environment is an experimental product
					being used for evaluation and demonstration
					purposes only. This is provided as a public service.
				</div>
				<div style="margin-top: 10px;">
					No warranties on accuracy of data are intended or provided. See
					link to contributor's data disclaimer in our <a
						style="text-decoration: underline;" href="/termsOfUse.jsp" title="Terms of Use">Terms
						of Use</a>.
				</div>
				<div>
					<br />
					<table border="0" cellspacing="0" cellpadding="0" width="100%">
						<tr>
							<td><div style="width: 16px; height: 16px; border: 1px solid #000; background-color: #FF8822; opacity:0.3"></div></td>
							<td>Data Available</td>
							<td>&nbsp;</td>
							<td><div style="width: 16px; height: 16px; border: 1px solid #000; background-color: #7722FF; opacity:0.3"></div></td>
							<td>Data Restricted</td>
							<td>&nbsp;&nbsp;&nbsp;</td>
							<td><div style="width: 16px; height: 16px; border: 1px solid #000; background-color: #555555; opacity:0.3"></div></td>
							<td>No Data</td>
						</tr>
					</table>
				</div>
			</div>

			<div id="collectorStatus" class='legends2 clearfix'>
				<div>
				</div>
			</div>

		</div>
	</div>
	
	<% if (request.getUserPrincipal() != null && request.isUserInRole("wde_admin")) { %>
		<script type="text/javascript">
			var admin = 1;
		</script>
	<% } else { %> %>
			<script type="text/javascript">
			var admin = 0;
		</script>
	<% } %> %>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
	<script type="text/javascript" src="/script/summaryMap.js"></script>

</body>
</html>
