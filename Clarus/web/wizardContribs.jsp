<%@page contentType="text/html; charset=iso-8859-1" language="java" import="clarus.qeds.Subscription" %>
<jsp:useBean id="oSubscription" scope="session" class="clarus.qeds.Subscription" />
<jsp:setProperty name="oSubscription" property="*" />
<%
    // Clear out the Subscription object.
    oSubscription.clearAll();
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Strict//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd">
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:v="urn:schemas-microsoft-com:vml">
  <head>
    <meta http-equiv="content-type" content="text/html; charset=UTF-8"/>
    <title>Clarus - Select Contributors</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardContribs.css"rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/WizardContribs.js" language="javascript" type="text/javascript"></script>
  </head>

  <body onload="onLoad()">

    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Observations by Contributor</div>
        <div id="titleText2Shadow">Observations by Contributor</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>
    
        <div id="instructions">
          <h3>Contributors</h3>
          Select one or more contributors from the<br/>
          <b><i>All Contributors</i></b> listbox
          and 'toss' them to the <b><i>Selected Contributors</i></b> listbox by pressing the
          <span class="button">&nbsp;<b>&gt;&gt;</b>&nbsp;</span> button. To remove a contributor from the
          <b><i>Selected Contributors</i></b> listbox, select it and press the
          <span class="button">&nbsp;<b>&lt;&lt;</b>&nbsp;</span> button.
          <br/>
          <br/>
          You may select more than one item in the list by pressing the <i>Ctrl</i> or
          <i>Shift</i> key when making your selection.
          <div id="statusMessage">&nbsp;</div>
        </div>
        
        <div id="navigation">
          <input id="btnNext" type="button" value="Next Page" onclick="Validate()"/>
        </div>
      </div>

      <div id="linkArea2">
        <table id="tblTossItems">
          <tr>
            <th>All Contributors</th>
            <th>&nbsp;</th>
            <th>Selected Contributors</th>
          </tr>
          <tr>
            <td>
              <select id="listAll" style="width: 197px;" size="10" multiple="1" ondblclick="Add()"></select>
            </td>
            <td>
              &nbsp;&nbsp;<input id="btnAdd" type="button" value="&gt;&gt;" onclick="Add()"/>
              <br/>
              <br/>
              &nbsp;&nbsp;<input id="btnRemove" type="button" value="&lt;&lt;" onclick="Remove()"/>
            </td>
            <td>
              <select id="listSel" style="width: 197px;" size="10" multiple="1" ondblclick="Remove()"></select>
            </td>
          </tr>
        </table>
      </div>

    </div> <!-- container -->

    <form action='<%= response.encodeURL("wizardStations.jsp") %>' method="post">
      <input id="contributors" name="contributors" type="hidden" value=""/>
    </form>

  </body>
</html>
