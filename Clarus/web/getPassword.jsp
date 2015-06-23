<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Password Required</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/GetPassword.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/GetPassword.js" language="javascript" type="text/javascript"></script>
  </head>

  <body onload="onLoad()">
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Password Required</div>
        <div id="titleText2Shadow">Password Required</div>
        <div id="linkHome"><a href="index.html">home</a></div>
    
        <div id="instructions">
          <h3>Password Required</h3>
          This subscription is protected by a password.
          Please supply the proper password to gain access.
          <div id="statusMessage">&nbsp;</div>
        </div>
      </div>
    
      <div id="passwordArea">
        <table id="tblPassword" border="0">
          <tr>
            <th>Password</th>
            <th>&nbsp;</th>
          </tr>
          <tr>
            <td>
              <input id="txtPassword" type="text" size="15" onkeypress="return DoEnterKey(this, event, Validate)"/>
            </td>
            <td>
              <input type="button" id="btn_OK" value="OK" onclick="Validate()"/>
            </td>
          </tr>

        </table>
      </div>
      
    </div> <!-- container -->

  </body>
</html>
