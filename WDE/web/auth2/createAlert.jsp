<%@page contentType="text/html; charset=UTF-8" language="java" import="wde.qeds.Subscription" %>
<%--<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" /> 
<%
    // Clear out the Subscription object.
    oSubscription.clearAll();
%>--%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Select Contributo Wizard" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
      <link href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css"/>
	<link rel="stylesheet" href="/style/leaflet.css" />
  
    <link href="/style/WDEMap.css" rel="stylesheet" type="text/css" media="screen"/>
    <link href="/style/WDEMap2.css" rel="stylesheet" type="text/css" media="screen"/>
   <link href="/style/createAlert.css" rel="stylesheet" type="text/css" media="screen"/> 

  <script src="/script/jquery/jquery.getUrlParam.js"></script>
	<script src="/script/us-states.js"></script>
	<script src="/script/leaflet.js"></script>
<script src="/script/leaflet-wxde.js"></script> 
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Listbox.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/createAlert.js" type="text/javascript"></script>
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
	

    	<div id="linkArea2">
	        <div id="map-container">
   
  
    <div id="map_canvas" > </div>
</div>
		</div>
		<div id="instructions" class="col-4" style="margin-top:-15px;">
			<h3>Alert Notification Instructions</h3>
			<p>
Alert notifications are used to send a notification message by email, with a specific subject line, when a particular set of weather alert conditions are observed in a particular area. 
To set the area, use the map to zoom to the area for which you would like to receive alert notifications. Select the <b>Set Selected Area</b> button. Click and drag on the map to draw a box around the stations for which you would like to receive alert notifications. To adjust the box, click and drag the icons on the corners of the box. To clear the selected area, click the <b>Clear Selected Area</b> button.
To set the message subject, type the subject text in the <b>Notification Message</b> text box. The message will be sent to your email inbox when your alert conditions have been met.
To create the set of alert conditions, you will compare selected weather observations to alert thresholds. For the first comparison, select a filter from the <b>filter</b> drop-down menu. </p>
      <ul>
        <li>“Any” will be met if any of the stations in your selected area meet the condition.</li>
        <li>“Mean” will be met if the mean value from the stations in your selected area meets the condition.</li>
        <li>“Mode” will be met if the mode value of the stations in your selected area meets the condition.</li>
      </ul>
      <p>To define the condition itself, select an obstype from the <b>obstype</b> drop-down menu and a comparison operator from the <b>comp</b> drop-down menu. Input a threshold value in the <b>val</b> text box, and a tolerance in the <b>tol</b> text box. Add the next alert notification condition by selecting the <input type="button" value="+" /> button next to the first condition. Conditions can be removed by selecting the <input type="button" value="-" /> button. 

			</p>
      
  <ul id="LayersMenu">
    <li><label for="chkRwisLayer">ESS Obs</label><input checked="checked" type="checkbox" id="chkRwisLayer" /></li>
    <li><label for="chkMobileLayer">Mobile Obs</label><input checked="checked" type="checkbox" id="chkMobileLayer" /></li>
    <li><label for="chkRoadLayer">Segment Obs</label><input checked="checked" type="checkbox" id="chkRoadLayer" /></li>
    <li><label for="chkMetaDataLayer">ESS Metadata</label><input checked="checked" type="checkbox" id="chkMetaDataLayer" /></li>
</ul>
    
			<div id="statusMessage" class="msg" style="color: #800; border: 1px #800 solid; display: none; font-size: 1.2em;">
				&nbsp;
			</div>
    
			
	    </div>
		<div class='clearfix'></div>
		<br>
    
      <div id="divReportTabs">
     <div id="divReportStep1"><input id="btnInitReport" type="button" value="Select Area" /></div>
     <div id="divReportStep2">Click and drag to select area.</div>
     <div id="divReportStep3"><input id="btnResetArea" type="button" value="Clear Selected Area" /></div>
     </div>
    <div id="divNotificationMessage" >
        Notification Message:
        <textarea id="txtNotificationMessage"></textarea>
    </div>
      <div id="conditions-container">
        <table id="alert-conditions">
          <thead><th>filter</th><th>obstype</th><th>comp</th><th>val</th><th>tol</th><th></th></thead><tbody></tbody>
        </table>
      </div>
			
      <input type="button" value="Save" id="btnSave" />
      
    
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>
