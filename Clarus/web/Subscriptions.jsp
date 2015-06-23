<%@page contentType="text/html; charset=iso-8859-1" language="java" import="java.io.*,java.util.*,util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%
	Config oConfig = ConfigSvc.getInstance().getConfig("clarus.qeds.QedsMgr");

	File oDir = new File(oConfig.getString("subscription", null));
    File[] oFileList = oDir.listFiles();
    ArrayList<File> oFiles = new ArrayList<File>();
    for (int i = 0; i < oFileList.length; i++)
		oFiles.add(oFileList[i]);

	Collections.sort(oFiles);
%>

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Subscriptions</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/Subscriptions.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
  </head>
  
  <body onload="CreateClock(false);">

    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Subscriptions</div>
        <div id="titleText2Shadow">Subscriptions</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Subscriptions</h3>
          Some subscriptions have been protected with a password,
          which must be entered to be granted access to the observation data.
        </div>
      </div>

      <div id="linkArea2">
        <br/>
        <table id="tblFolders">
          <tr><th colspan="5">Subscriptions</th></tr>
          <tr><td colspan="5">&nbsp;</td></tr>
          <tr>
<%
    Iterator<File> oIter = oFiles.iterator();
    File oFile;
    int nCol = 0;
    while (oIter.hasNext())
    {
         oFile = oIter.next();
         if ((nCol % 5) == 0)
         {
             nCol = 0;
%>
          </tr>
          <tr>
<%
         }
%>
            <td><a href="SubFolder.jsp?subId=<%= oFile.getName() %>"><%= oFile.getName() %></a></td>
<%
        nCol++;
    }
%>
          </tr>
        </table>
      </div>
    </div> <!-- container -->
    
  </body>
</html>
