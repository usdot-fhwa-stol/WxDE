<%@page contentType="text/html; charset=UTF-8" language="java" %>
<jsp:useBean id="oSubscription" scope="session" class="wde.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
  <head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta http-equiv="X-UA-Compatible" content="IE=Edge">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    
    <title>WDE - Query Results</title>
    
    <script src="/script/Common.js" type="text/javascript"></script>
    <script src="/script/ShowQueryResults.js" type="text/javascript"></script>
    	
    <!-- Bootstrap3 UI framework -->
    <link href="/style/vendor/bootstrap.min.css" rel="stylesheet" />
    
    <link href="/style/QueryResults.css" rel="stylesheet" type="text/css" media="screen"/>
    
    <link href="/style/Archive.css" rel="stylesheet" />
    <link  href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" />
    
    <script src="//maps.google.com/maps/api/js?sensor=true" type="text/javascript"></script>
    <script src="/script/jquery/jquery-1.9.1.js" type="text/javascript"></script>
    <script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
	<script src="/script/jquery/jquery.validate.js" type="text/javascript"></script>
    
    <script src="/script/jquery/jquery.ui.map.js" type="text/javascript"></script>
    <script src="/script/jquery/jquery.ui.map.overlays.js" type="text/javascript"></script>
    <script src="/script/jquery/jquery.ui.map.services.js" type="text/javascript"></script>
	
	<!-- top menu CSS style -->
	<link href="/style/top-mini-nav.css" rel="stylesheet" />
	
	<link href="/style/wxde-mini-style.css" rel="stylesheet" />
	
    <link rel="stylesheet" href="/style/main-accessibility-styles.css" />
	
	<!--[if IE ]>
		<link href="/style/IE-styles-mini.css" type="text/css" rel="stylesheet">
	<![endif]-->
    
  </head>

  <body onload="onLoad();" class="fhwa-gradient-color show-query-results">
  	
  	<jsp:include page="/inc/mini-system-ui/miniHeader.jsp" ></jsp:include>
    
    <div id="container" class="container-fluid clearfix">    
      
      <div id="sidebar" class="col-3 full-height">
        
        <div id="saveArea">
          
          <h4>Saving the Query Results</h4>
          
          <hr>
          
		  <div id="timeUTC">Time: </div> 
		  
		  <br>
		  
		  <p>
	          You can save the query results by clicking on the link below.
	          This will reissue the query and display the results
	          in a plain browser window. You can then save the entire page
	          by selecting the proper option from your browser's <i>File</i> menu.
          </p>
          
          <p>
	          <b>Note:</b> Because the query is reissued, the results you see on the next
	          page may be different than the results you are currently viewing.
          </p>
          
          <p>Display the Results for Saving:</p>
          
          <a class="btn btn-info" id="fullscreen-button" href="<%= response.encodeURL( "queryResults.jsp")%>">
	       	  <img src="/image/icons/light/fa-expand.png" alt="Fullscreen Icon" />	
              Click Here
       	  </a>
       	  
        </div>
        
      </div> <!-- sidebar -->

      <div id="resultsArea" class="col-9 full-height">
        <iframe class="resultsFrame" id="resultsFrame" src="queryResults.jsp" frameborder="0" title="Results" longdesc="This iframe presents the content of the selected report. This content may or may not be user-friendly or easily readable."></iframe>
      </div>

    </div> <!-- container -->
    
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>
  
  </body>

</html>
