<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Create Subscription</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardSubscribe.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/WizardSubscribe.js" language="javascript" type="text/javascript"></script>
  </head>

  <body onload="onLoad()">
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Create Subscription</div>
        <div id="titleText2Shadow">Create Subscription</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Create Subscription</h3>
          The Security Code is a mechanism that helps prevent external
          automated abuse of the <i>Clarus</i> System resources.
          Simply enter the six-digit number shown in the picture in the
          <b><i>Security Code</i></b> field.
          There is no need to memorize or write down the Security Code, as it
          is only used to verify the presence of a human operator.
          <br/>
          <br/>
          The <b><i>Password</i></b> field is used to prevent unauthorized access
          to your subscription's observations. You may leave the <b><i>Password</i></b> field
          empty if you do not wish to protect your subscription's observations.
          <br/>
          <br/>
          You may also select the output format for the subscription.
          <br/>
          <br/>
					*Contact information is optional and is used only to send notifications when the 
					<i>Clarus</i> System has maintenance or enhancement activities planned.
          <div id="statusMessage">&nbsp;</div>
        </div>
        <div id="navigation">
          <input id="btnNext" type="button" value="Subscribe" onclick="Validate()"/>
        </div>
      </div>

      <div id="linkArea2">
        <table id="tblCaptcha" border="0">
          <tr>
            <th>Security<br/>Code</th>
            <th>Password</th>
	    <th>Interval<br/>(in minutes)</th>
            <th>Output<br/>Format</th>
          </tr>
          <tr>
            <td><input type="text" id="txtCaptcha" onkeypress="return DigitsOnly(this, event)"/></td>
            <td><input type="text" id="txtPassword"/></td>
            <td>
              <select id="listCycle">
                <option value="5">5</option>
                <option value="10">10</option>
                <option value="15">15</option>
                <option value="20" selected="selected">20</option>
                <option value="25">25</option>
                <option value="30">30</option>
              </select>
            </td>
            <td>
              <select id="listFormat">
                <option>CMML</option>
                <option>CSV</option>
                <option>KML</option>
                <option>XML</option>
              </select>
            </td>
          </tr>
          <tr>
            <td id="cellCaptchaPic" colspan="4">
              <br/>
              <img src='<%= response.encodeURL("QedsMgr;jsessionid=" + request.getSession().getId()) %>'/>
            </td>
          </tr>
          <tr>
          	<th colspan="2">*Contact Email</th>
          	<th colspan="2">*Contact Name</th>
          </tr>
          <tr>
          	<td colspan="2"><input id="contactEmail" type="text" max="255" style="width: 100%;"/></td>
          	<td colspan="2"><input id="contactName" type="text" max="128"/></td>
          </tr>
        </table>
      </div>
                
    </div> <!-- container -->

    <form action='<%= response.encodeURL("wizardSubResults.jsp") %>' method="post">
      <input id="jsessionid" type="hidden" value="<%= request.getSession().getId() %>"/>
    </form>

  </body>
</html>
