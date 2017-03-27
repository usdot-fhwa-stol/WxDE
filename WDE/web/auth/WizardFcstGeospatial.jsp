<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.FcstSubscription" %>
<jsp:useBean id="oFcstSubscription" scope="session" class="wde.qeds.FcstSubscription" />
<jsp:setProperty name="oFcstSubscription" property="*" />
<%
    // Clear out the Subscription object.
    oFcstSubscription.clearAll();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Forecast Geospacial Wizard" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
	<script src="/script/xml.js" type="text/javascript"></script>
	<script src="/script/Listbox.js" type="text/javascript"></script>
	<script src="/script/Common.js" type="text/javascript"></script>
	<script src="/script/WizardFcstGeospatial.js" type="text/javascript"></script>
	
	<style>
	.container a {
		color: #006699;
		text-decoration: none;
	}
	
	.container a:hover {
		text-decoration: underline;
	}
	
	.tblHdr,.tblFld {
		text-align: justify !important;
		padding: 10px 5px 10px 15px !important;
		font-size: 1.1em !important;
	}
	
	table {
		border-collapse: separate !important;
	}
	
	.btnNext {
		padding-left: 0px !important;
	}
	</style>
	
	<script type="text/javascript">
    	$(document).ready(function() {
    		$('#dataPage, #dataPage a').addClass('active');
    	});
    </script>

</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
		<h1>Define Geospatial Coordinates</h1>
		
		<div class="col-5" style="margin-top: -15px;">
			<h3>Select observations by bounding box:</h3>
			<table id="tblBoundingBox" style='display: block'>
				<caption style="display: none;">Bounding Box</caption>
				<tr>
					<th id="hdrLat1" class="tblHdr"><label for="txtLat1">Latitude 1</label></th>
					<td id="fldLat1" class="tblFld"><input id="txtLat1" title="Latitude 1" type="text" size="15" onkeypress="return NumbersOnly(this, event)" /></td>
				</tr>
				<tr>
					<th id="hdrLng1" class="tblHdr"><label for="txtLng1">Longitude 1</label></th>
					<td id="fldLng1" class="tblFld"><input id="txtLng1" title="Longitude1" type="text" size="15" onkeypress="return NumbersOnly(this, event)" /></td>
				</tr>
				<tr>
					<th id="hdrLat2" class="tblHdr"><label for="txtLat2">Latitude 2</label></th>
					<td id="fldLat2" class="tblFld"><input id="txtLat2" title="Latitude 2" type="text" size="15" onkeypress="return NumbersOnly(this, event)" /></td>
				</tr>
				<tr>
					<th id="hdrLng2" class="tblHdr"><label for="txtLng2">Longitude 2</label></th>
					<td id="fldLng2" class="tblFld"><input id="txtLng2" title="Longitude 2" type="text" size="15" onkeypress="return NumbersOnly(this, event)" /></td>
				</tr>
			</table>
                        
			<form action='<%= response.encodeURL("WizardFcstObsTypes.jsp") %>'
				method="post">
				<input id="region" name="region" type="hidden" value="" />
			</form>

			<br>
			<button id="btnNextNew" type="button" class="btn-dark"
				style="margin-left:0;" 
				onclick="Validate()">
				Next Page
				<img src="/image/icons/light/fa-arrow-right.png" alt="Next Page Icon"
					style="margin-bottom: -1px;" />
			</button>
		</div>
		<div id="instructions" class="col-4"
			style="margin: 0; margin-top: -15px;">
			<h3>Instructions</h3>
			<p>
				You can retrieve observations by specifying the geospatial
				coordinates for the desired region. The region can be defined as a
				rectangle. <br /> <br /> To define a rectangular
				region,
				specify the latitudes and longitudes for the opposite corners of the
				box. 
                                <br/><br/>
				Besides using the mouse, you can also use the tab key or shift-tab key combination to 
				traverse elements on the page. 
	        	<br/>
	     		<br/>
			</p>
			<div id="statusMessage" class="msg"
				style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
				<!-- <img src="/image/close-img.svg" id="close-msg" style="float:right; cursor: pointer; margin-top:2px;" /> -->
			</div>
		</div>
		<div class='clearfix'></div>
		<br>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

</body>
</html>
