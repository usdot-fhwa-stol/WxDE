<%@page contentType="text/html; charset=iso-8859-1" language="java" import="clarus.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />

<%
    // Let queryResults know that the Subscription was created as part of the wizard.
    oSubscription.setWizardRunning(true);
%>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Specify Time Range and Format</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardTimeRange.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/WizardTimeRange.js" language="javascript" type="text/javascript"></script>
  </head>
  
  <body onload="onLoad()">
    <div id="container">
        
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Time and Format</div>
        <div id="titleText2Shadow">Time and Format</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Time and Format</h3>
          Specify which hour within the last seven days for the observations
		  you wish to retrieve. Note, that if you select the current hour, you
		  will receive only the observations processed up to now.
          <br/>
          <br/>
          You may also select the output format for the report. The default is
		  a comma separated value report.
          <div id="statusMessage">&nbsp;</div>
        </div>
    
        <div id="navigation">
          <input id="btnNext" type="button" value="Run Query" onclick="Validate()"/>
        </div>
      </div>

    
      <div id="linkArea2">
        <table id="tblTossItems">
          <tr>
            <th>Select Hour</th>
            <th>Output Format</th>
          </tr>
          <tr>
            <td>
              <select id="listTimeStart"></select>
            </td>
            <td>
              <select id="listFormat">
                <option>CMML</option>
                <option>CSV</option>
                <option>XML</option>
              </select>
            </td>
          </tr>
        </table>
      </div>
    </div> <!-- container -->
    
    <form action='<%= response.encodeURL("showQueryResults.jsp") %>' method="post">
      <input id="timeRange" name="timeRange" type="hidden" value=""/>
      <input id="format" name="format" type="hidden" value=""/>
    </form>

  </body>
</html>
