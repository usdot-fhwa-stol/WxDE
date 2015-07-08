<%@page contentType="text/html; charset=iso-8859-1" language="java" import="clarus.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Select Observations</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardObsTypes.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/WizardObsTypes.js" language="javascript" type="text/javascript"></script>
  </head>

  <body onload="onLoad()">
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Observations</div>
        <div id="titleText2Shadow">Observations</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Observations</h3>
          Specify the observation type to retrieve by selecting it from the
          <b><i>Observation Type</i></b> listbox, or leave it on "All Observations"
          to retrieve all observations.
          <br/>
          <br/>
          When you select a specific observation type,
          you will be presented with optional entry fields for the minimum and maximum
          values, as well as listboxes for each of the Quality Checks that are valid
          for that observation type. Supplying values for the minimum and/or maximum
          will filter the observations retrieved to those values that are within the
          specified range.
          <br/>
          <br/>
          You may also filter an observation based on its Quality Checks by selecting
          "P" (Pass) or "N" (Not Pass) from its respective drop-down list.
          <div id="statusMessage">&nbsp;</div>
        </div>

        <div id="navigation">
          <input type="button" id="runQuery" value="Run Report" onclick="RunQuery()"/>
          &nbsp;&nbsp;
          <input type="button" id="subscript" value="Subscribe" onclick="Subscribe()"/>
        </div>

      </div>
    
      <div id="obsArea">
        <table id="tblObs" border="0">
          <tr>
            <th id="hdrObs">Observation Type</th>
            <th id="hdrMin">Minimum</th>
            <th id="hdrMax">Maximum</th>
          </tr>
          <tr>
            <td>
              <select id="listObsTypes" onchange="ObsTypeChanged()"></select>
            </td>
            <td>
              <input id="txtMin" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/>
            </td>
            <td>
              <input id="txtMax" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/>
            </td>
          </tr>
        </table>
      </div>
      
      <div id="qcTestsArea">
        <h4 id="hdrTestArea">Quality Checks</h4>
        <table id="tblTests" class="qualityCheckFlags"></table>
      </div>
      
      <div id="hourlyMessageArea" style="width: 500px; margin-top: 48px; font-weight: bold;">
      	NOTE: Reports allow up to seven (7) days of observations to be retrieved. 
      	However, only one (1) hour of observations may be viewed at a time.<br/><br/>
      	If you need more continous data, it is recommended that you create a 
      	subscription instead of running a report.
      </div>

    </div> <!-- container -->

    <form action="" method="post">
      <input id="runQueryUrl" type="hidden" value="<%= response.encodeURL("wizardTimeRange.jsp") %>"/>
      <input id="subscribeUrl" type="hidden" value="<%= response.encodeURL("wizardSubscribe.jsp") %>"/>
      <input id="obs" name="obs" type="hidden" value=""/>
      <input id="flags" name="flags" type="hidden" value=""/>
    </form>

  </body>
</html>
