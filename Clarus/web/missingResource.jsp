<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Missing Resource</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/MissingResource.css" rel="stylesheet" type="text/css" media="screen"/>
  </head>

  <body>
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Missing Resource</div>
        <div id="titleText2Shadow">Missing Resource</div>
        <div id="linkHome"><a href="index.html">home</a></div>
    
        <div id="instructions">
          <h3>Missing Resource</h3>
          The <%= request.getParameter("resource") %> you are trying to access is no longer on
          the <i>Clarus</i> System.
          The link you used to access the page may have expired.
          <div id="statusMessage">&nbsp;</div>
        </div>
      </div>
      
      <div id="infoArea">
        <table id="tblInfo" border="0">
          <tr>
            <th>Resource Type</th>
            <th>Resource Name</th>
          </tr>
          <tr>
            <td>
              <%= request.getParameter("resource") %>
            </td>
            <td>
              <%= request.getParameter("id") %>
            </td>
          </tr>
        </table>
      </div>
      
    </div> <!-- container -->

  </body>
</html>
