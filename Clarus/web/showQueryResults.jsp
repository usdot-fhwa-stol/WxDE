<%@page contentType="text/html; charset=iso-8859-1" language="java" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Query Results</title>
    <link href="style/QueryResults.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/ShowQueryResults.js" language="javascript" type="text/javascript"></script>
  </head>

  <body onload="onLoad();">
    <div id="container">    
      <div id="sidebar">
        <div id="titleArea">
          <div id="titleText"><i>Clarus</i> System</div>
          <div id="titleTextShadow"><i>Clarus</i> System</div>
          <div id="titleText2">Query Results</div>
          <div id="titleText2Shadow">Query Results</div>
          <div id="linkHome"><a href="index.html">home</a></div>
        </div>
        <div id="timeUTC">Time: </div>
        <div id="saveArea">
          <h3>Saving the Query Results</h3>
          You may save the query results by clicking on the link below.
          This will reissue the query and display the results
          in a plain browser window. You can then save the entire page
          by selecting the proper option from your browser's <i>File</i> menu.
          <br/>
          <br/>
          <b>Note:</b> Because the query is reissued, the results you see on the next
          page may be different than the results you are currently viewing.
          <br/>
          <br/>
          <a href="queryResults.jsp">Display the Results for Saving</a>
        </div>
      </div> <!-- sidebar -->

      <div id="resultsArea">
        <iframe id="resultsFrame" src="queryResults.jsp" frameborder="0" ></iframe>
      </div>

    </div> <!-- container -->
  </body>

</html>
