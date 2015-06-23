<%@page contentType="text/html; charset=iso-8859-1" language="java" import="clarus.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Select Stations</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardStations.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/WizardStations.js" language="javascript" type="text/javascript"></script>
  </head>
  
  <body onload='onLoad("<%= oSubscription.getContributors() %>")'>
    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Contributor Stations</div>
        <div id="titleText2Shadow">Contributor Stations</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Stations</h3>
          Select one or more stations from the <b><i>All Stations</i></b> listbox
          and 'toss' them to the <b><i>Selected Stations</i></b> listbox by pressing the 
          \/ button.
          To remove a station from the
          <b><i>Selected Stations</i></b> listbox, select it and press the 
          /\ button.
          <br/>
          <br/>
          You may select more than one item in the list by pressing the <i>Ctrl</i> or
          <i>Shift</i> key when making your selection.
          <br/>
          <br/>
          <b><i>Note:</i></b>
          It is not necessary to select any stations. When no stations are selected,
          all of the contributors' stations will be included in the query results.
          <div id="statusMessage">&nbsp;</div>
        </div>

        <div id="navigation">
          <input id="btnNext" type="button" value="Next Page" onclick="Validate()"/>
        </div>
      </div>
    
      <div id="linkArea2">
        <table id="tblTossItems">
          <tr>
            <th>All Stations</th>
          </tr>
          <tr>
            <td>
              <select id="listAll" style="width: 484px;" size="10" multiple="1" ondblclick="Add()"></select>
            </td>
          </tr>
          <tr>
            <td align="center">
              <br/>
              &nbsp;&nbsp;<input id="btnAdd" type="button" value="&nbsp;\/&nbsp;" onclick="Add()"/>
              &nbsp;&nbsp;<input id="btnRemove" type="button" value="&nbsp;/\&nbsp;" onclick="Remove()"/>
            </td>
          </tr>
          <tr>
            <th>Selected Stations</th>
          </tr>
          <tr>
            <td>
              <select id="listSel" style="width: 484px;" size="10" multiple="1" ondblclick="Remove()"></select>
            </td>
          </tr>
        </table>
      </div>
    </div> <!-- container -->
    
    <form action='<%= response.encodeURL("wizardObsTypes.jsp") %>' method="post">
      <input id="stations" name="stations" type="hidden" value=""/>
    </form>
  </body>
</html>
