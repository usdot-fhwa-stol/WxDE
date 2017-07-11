<%@page contentType="text/html; charset=UTF-8" language="java" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>

    <jsp:include page="/inc/main-wxde-ui/headers-and-styles.jsp">
    	<jsp:param value="Select Contributo Wizard" name="title"/>
    </jsp:include>
	<jsp:include page="/inc/main-wxde-ui/script-file-sources.jsp"></jsp:include>
	
      <link href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css"/>
	<link href="/style/reports-table.css" rel="stylesheet" media="all">
	<link href="/style/viewAlerts.css" rel="stylesheet">
   <!--    <script src="/script/jquery/jquery-2.2.0.js"></script>
  <script src="/script/jquery/jquery-ui-full-1.11.4.js"></script>-->
    <script src="/script/xml.js" type="text/javascript"></script>
    <script src="/script/Listbox.js" type="text/javascript"></script>
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/viewAlerts.js" type="text/javascript"></script>
</head>

<body>
	<jsp:include page="/inc/main-wxde-ui/mainHeader.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainMenu.jsp"></jsp:include>

	<div class="container">
    <input type="button" value="Create New Alert" id="btnCreateAlert" />
    <div id="divNotifications">
    </div>
	</div>

	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
	
</body>
</html>
