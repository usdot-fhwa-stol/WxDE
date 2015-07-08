<%@page contentType="text/html; charset=iso-8859-1" language="java" import="java.io.*,java.text.*,java.util.*,util.*" %>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">

<%
	Config oConfig = ConfigSvc.getInstance().getConfig("clarus.ems.EmsMgr");

	java.util.Date oDate = new java.util.Date();
    SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    
    File oDir = new File(oConfig.getString("metadata", null));
    File[] oFileList = oDir.listFiles();
    ArrayList<File> oFiles = new ArrayList<File>();
    for (int i = 0; i < oFileList.length; i++)
    {
        if (oFileList[i].getName().indexOf(".csv") > 0)
            oFiles.add(oFileList[i]);
    }
    Collections.sort(oFiles);
%>

<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Metadata</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/Metadata.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
  </head>
  
  <body onload="CreateClock(false);">

    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Metadata Files</div>
        <div id="titleText2Shadow">Metadata Files</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Metadata Files</h3>
          The <i>Clarus</i> System metadata files contain a textual representation
          of the metadata contained within the database.
          The files are formatted as CSV (comma-separated value) files, allowing them
          to be imported easily into third-party analysis tools.
        </div>
      </div>

      <div id="linkArea2">
        <br/>
        <table id="tblFolders">
          <tr>
            <th>Metadata Files</th>
            <th>Last Update (UTC)</th>
          </tr>
		  <tr><td>&nbsp;</td></tr>
<%
    Iterator<File> oIter = oFiles.iterator();
    File oFile;
    String sDate;
    while (oIter.hasNext())
    {
        oFile = oIter.next();
        oDate.setTime(oFile.lastModified());
        sDate = oDateFormat.format(oDate);
%>
          <tr>
            <td><a href="ShowMetadata.jsp?file=<%= oFile.getName() %>"><%= oFile.getName() %></a></td>
            <td class="fileSize"><%= sDate %></td>
          </tr>
<%
    }
%>
        </table>
      </div>
    </div> <!-- container -->
    
  </body>
</html>
