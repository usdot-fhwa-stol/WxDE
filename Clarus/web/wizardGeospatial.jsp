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
    <title>Clarus - Geospatial Region</title>
    <link  href="style/Clarus.css" rel="stylesheet" type="text/css" media="screen"/>
    <link  href="style/WizardGeospatial.css" rel="stylesheet" type="text/css" media="screen"/>
    <script src="script/xml.js" language="javascript" type="text/javascript"></script>
    <script src="script/Listbox.js" language="javascript" type="text/javascript"></script>
    <script src="script/Common.js" language="javascript" type="text/javascript"></script>
    <script src="script/WizardGeospatial.js" language="javascript" type="text/javascript"></script>
  </head>

  <body onload="onLoad()">

    <div id="container">
      <div id="titleArea">
        <div id="titleText"><i>Clarus</i> System</div>
        <div id="titleTextShadow"><i>Clarus</i> System</div>
        <div id="titleText2">Define Geospatial Coordinates</div>
        <div id="titleText2Shadow">Define Geospatial Coordinates</div>
        <div id="linkHome"><a href="index.html">home</a></div>
        <div id="timeUTC"></div>

        <div id="instructions">
          <h3>Geospatial Coordinates</h3>
          You may retrieve observations by specifying the geospatial coordinates for
          the desired region.  The region can be defined as a rectangle or as a circle.
          <br/>
          <br/>
          To define a rectangular region, click on the <b><i>Bounding Box</i></b>
          radio button and specify the latitudes and longitudes for the opposite corners
          of the box.
          <br/>
          <br/>
          To define a circular region, click on the <b><i>Point &amp; Radius</i></b>
          radio button and specify the latitude and longitude of the point, along with the
          radius, in kilometers.
          <div id="statusMessage">&nbsp;</div>
        </div>
        <div id="navigation">
          <input id="btnNext" type="button" value="Next Page" onclick="Validate()"/>
        </div>
      </div>
    
      <div id="linkArea2">
        <h4>Select observations by:</h4>
        <table id="tblGeoTypes">
          <tr>
            <td>
              <span onclick="GeoTypeChanged('bb')"><input type="radio" id="rbBoundingBox" name="geoType" checked="true" onchange="GeoTypeChanged('bb')">&nbsp;&nbsp;Bounding Box</span>
            </td>
          </tr>
          <tr>
            <td>
              <span onclick="GeoTypeChanged('pr')"><input type="radio" id="rbPointRadius" name="geoType" onchange="GeoTypeChanged('pr')">&nbsp;&nbsp;Point &amp; Radius</span>
            </td>
          </tr>
        </table>
      </div>
                
      <table id="tblBoundingBox">
        <tr>
          <th id="hdrLat1" class="tblHdr">Latitude 1</th>
          <th id="hdrLng1" class="tblHdr">Longitude 1</th>
          <th id="hdrLat2" class="tblHdr">Latitude 2</th>
          <th id="hdrLng2" class="tblHdr">Longitude 2</th>
        </tr>
        <tr>
          <td id="fldLat1" class="tblFld"><input id="txtLat1" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
          <td id="fldLng1" class="tblFld"><input id="txtLng1" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
          <td id="fldLat2" class="tblFld"><input id="txtLat2" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
          <td id="fldLng2" class="tblFld"><input id="txtLng2" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
        </tr>
      </table>

      <table id="tblPointRadius">
        <tr>
          <th id="hdrLat1" class="tblHdr">Latitude</th>
          <th id="hdrLng1" class="tblHdr">Longitude</th>
          <th id="hdrLat2" class="tblHdr">Radius (km)</th>
        </tr>
        <tr>
          <td id="fldLatPR" class="tblFld"><input id="txtLat" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
          <td id="fldLngPR" class="tblFld"><input id="txtLng" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
          <td id="fldRadius" class="tblFld"><input id="txtRadius" type="text" size="10" onkeypress="return NumbersOnly(this, event)"/></td>
        </tr>
      </table>
    </div> <!-- container -->

    <form action='<%= response.encodeURL("wizardObsTypes.jsp") %>' method="post">
      <input id="region" name="region" type="hidden" value=""/>
    </form>

  </body>
</html>
