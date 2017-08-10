// global variables
var QCH_MAX = 15;
var m_oMap;
var oMapOptions;
var m_oInfoWindow;
var m_oMarkerMgr = null;
var m_nDefaultZoom;
var m_oStationXmlText;
var m_oMousePos = null;
var m_oStationCode = null;
var m_oClock = null;
var m_oObsTypes = null;

var m_oStations = new StationMgr();

// persistent marker icons
var m_oGrayIcon = new Object(),
    m_oPurpleIcon = new Object(),
    m_oBlueIcon = new Object(),
    m_oBrownIcon = new Object(),
    m_oGrayWhiteIcon = new Object(),
    m_oWhiteIcon = new Object(),
    m_oYellowIcon = new Object();

var mobileWxdeCount = 0;
var mobileVdtCount = 0;
var segmentCount = 0;

var rolloverPoint;
var activeMarker;

var m_oQCh =
[
    [{im:"b",tx:""},{im:"nr",tx:"not run"}],
    [{im:"n",tx:"not passed"},{im:"p",tx:"passed"}]
];

//the segment array
var routePaths = new Array();
var mobileMarkersWithRoutes = new Array();
var vdtMarkersWithRoutes = new Array();

//variables associated with the "Shade" sidebar function
//create marker for pointing at clicked dot
var markerIcon = new google.maps.Marker({
        icon: 'http://maps.google.com/mapfiles/marker_grey.png'
    });
//adding a separate infowindow used by the shade as a
//marker for a clicked dot.
var btnShade = document.getElementById('btnShade');
var markerInfoWindow = new google.maps.InfoWindow({
    content: '<i class="icon-map-marker" style="color: #555; font-size: 14px;"></i>'
});
var currentStation = null;
var sTextStation = null;
var observationTable = null;
var sideBar = document.getElementById('sideBar');
var sideBarContent = document.getElementById('retainBlock').innerHTML;
var btnFullScreen = '<br /> <button type="button" id="fullScreen" class="btn btn-dark pull-right"><span class="icon-fullscreen"></span> FullScreen</button>';
var btnViewStation = '<button type="button" id="viewStation" class="btn btn-dark pull-right"><img src="/image/icons/light/fa-marker.png" alt="Center Station Icon" style="margin-top: -2px;" /> Center Station</button>';
//for segments' dataOver
var dataSegments = new Array();
//trigger for units to disable opening of shade
var unitTriggerFlag = null;

// marker comparator object
var m_oMarkerCompare =
{
    compare : function(oMarkerL, oMarkerR)
    {
        // if the markers are null, then whatever is being compared against it is defaulted to greater than
        if (oMarkerL == null || oMarkerR == null) {
            return 1;
        }
        
        var oStationL = oMarkerL.m_oStation;
        var oStationR = oMarkerR.m_oStation;

        var nValue = oStationL.id - oStationR.id;
        return nValue;
        
        /*  
        if (nValue != 0)
            return nValue;

        if (oStationL.lt < oStationR.lt || oStationL.ln < oStationR.ln)
            return -1;

        if (oStationL.lt > oStationR.lt || oStationL.ln > oStationR.ln)
            return 1;

        return 0;
        */
    }
};

var m_oCircleOverlay = null;

// The Barnes Spatial Radius is based on 69 miles per degree.
var m_oBarnesSpatialRadius = 1.0;

var m_bMapIsMoving = false;

var m_oTimeSendXml_1,
    m_oTimeReceiveXml_1,
    m_oTimeSendXml_2,
    m_oTimeReceiveXml_2;
//var m_oTimeDoneParsing;

var m_oViewport_NE_Lat,
    m_oViewport_NE_Lng,
    m_oViewPort_SW_Lat,
    m_oViewPort_SW_Lng;

var m_oRetrieve_NE_Lat,
    m_oRetrieve_NE_Lng,
    m_oRetrieve_SW_Lat,
    m_oRetrieve_SW_Lng;

var m_bShowASOS = false;

var m_oAllTests = new QChTests();

var m_sUrl;

var m_oStationData = new Array();

var m_oSelectedMarker;

//This set the default unit to either "m" for Metrics, and "e" for English
var m_sCurrentUnits = "e";
var m_sCurrentSource = "w";

var m_dCircleX = [],
    m_dCircleY = [];

function LabelOverlay(oMarkerOptions)
{
    this.m_oLatLng = oMarkerOptions.position;
    this.m_oDataOver = null;
}


LabelOverlay.prototype = new google.maps.OverlayView();


LabelOverlay.prototype.onAdd = function()
{
    var oDiv = document.createElement("div");
    oDiv.id = "dataOver";
    oDiv.innerHTML = "";
    oDiv.style.position = "absolute";
    oDiv.style.visibility = "hidden"; // always create invisible

    this.m_oDataOver = oDiv;
    this.getPanes().floatShadow.appendChild(oDiv);
};


LabelOverlay.prototype.draw = function()
{
    var oProj = this.getProjection();
    var oPos = oProj.fromLatLngToDivPixel(this.m_oLatLng);

    var oDiv = this.m_oDataOver;
    oDiv.style.left = oPos.x + "px";
    oDiv.style.top = (oPos.y - 8) + "px"; // shift label up
};

LabelOverlay.prototype.onRemove = function()
{
    var oDiv = this.m_oDataOver;
    oDiv.parentNode.removeChild(oDiv);
    this.m_oDataOver = null;
};


LabelOverlay.prototype.setText = function(sText)
{
    var oDiv = this.m_oDataOver;
    if (sText.toString().length == 0)
    {
        oDiv.style.display = "";
        oDiv.style.visibility = "hidden";
    } else {
        if(sTextStation != undefined) {
            oDiv.innerHTML = sText;
            if(sTextStation.newValidity === true) {
                oDiv.style.display = '';
                oDiv.style.visibility = 'visible';
            } else {
                oDiv.style.display = '';
                oDiv.style.visibility = 'hidden';
            }
        }
    }
};


function onLoad()
{
    CreateIcon(m_oGrayIcon, "image/mm_12_gray.png", 12, 12, 5, 5);
    CreateIcon(m_oPurpleIcon, "image/mm_12_purple.png", 12, 12, 5, 5);
    CreateIcon(m_oBlueIcon, "image/mm_12_blue.png", 12, 12, 5, 5);
    CreateIcon(m_oBrownIcon, "image/mm_12_brown.png", 12, 12, 5, 5);
    CreateIcon(m_oGrayWhiteIcon, "image/mm_12_graywhite.png", 12, 12, 5, 5);
    CreateIcon(m_oWhiteIcon, "image/mm_12_white.png", 12, 12, 5, 5);
    CreateIcon(m_oYellowIcon, "image/mm_12_yellow.png", 12, 12, 5, 5);

    // See if we are showing ASOS Stations.
    // NOTE: The m_bShowASOS flag is used to enable all internal MHI debugging options.
    m_bShowASOS = (document.URL.indexOf("showasos") >= 0);

    // Turn off the "Clear Circle" button if we are not showing ASOS stations.
    if (!m_bShowASOS)
    {
        var oClearCircle = document.getElementById("btn_circle");
        oClearCircle.style.display = "none";
    }

    // Create the array of Quality Check tests.
    BuildQChTests();

    // Fill the MapAreas listbox.
    FillMapAreaList();

    // Set the dimensions of the map viewport.
    var nHeight = GetWinDimension("height");
    var oDivMap = document.getElementById("map_canvas");

    oDivMap.style.height = (nHeight - 66) + "px";

    // Create the text node for the lat/long display.
    m_oMousePos = document.getElementById("latlong");
    m_oMousePos.appendChild(document.createTextNode(""));

    // Create the text node for the Station Code display.
    m_oStationCode = document.getElementById("stationCode");
    m_oStationCode.appendChild(document.createTextNode(""));

    // Create the text node for the UTC time display.
    m_oClock = document.getElementById("timeUTC");
    m_oClock.appendChild(document.createTextNode(""));
    StartMapClock();

    // Make sure the Metric units are the default
    var oUnitType = document.getElementsByName("rbUnits");
    oUnitType[1].checked = true;

    // compute points based on unit circle
    var dY = 0.0;
    // right side
    for (dY = -1.0; dY < 1.0; dY += 0.01)
    {
        m_dCircleY.push(dY);
        m_dCircleX.push(Math.sqrt(1.0 - (dY * dY)));
    }
    // left side
    for (dY = 1.0; dY > -1.0; dY -= 0.01)
    {
        m_dCircleY.push(dY);
        m_dCircleX.push(-Math.sqrt(1.0 - (dY * dY)));
    }
    // copy the first point to the last point
    m_dCircleY.push(m_dCircleY[0]);
    m_dCircleX.push(m_dCircleX[0]);

    // Allow the browser window to get established before continuing.
    setTimeout(ContinueLoading, 100);

}

function ContinueLoading()
{
    // Load form defaults to center the map on the correct view
    loadDefaults(document.URL);

    // Get the list of observations.
    GetObsList();
}


// onUnload()
// This function unloads the Google Map stuff, and is called whenever the page
// is unloaded.
// There is an error in version 2.76 of the Google Maps API that causes
// IE to choke up an error.  Putting the GUnload() inside a try/catch
// block prevents the error from being displayed.
function onUnload()
{
    try
    {
        GUnload();
    }
    catch(e)
    {
    }
}


function parseUrl(oParams, sUrl)
{
    // Extract the parameters from the specified URL.
    var sTemp = sUrl.split("?");

    if (sTemp.length > 1)
    {
        var sParameters = sTemp[1].split("&");

        for (var i = 0; i < sParameters.length; i++)
        {
            var sParam = sParameters[i].split("=");
            if (sParam[0] == "lat")
                oParams.lat = parseFloat(sParam[1]);
            else if (sParam[0] == "lon")
                oParams.lon = parseFloat(sParam[1]);
            else if (sParam[0] == "zoom")
                oParams.zoom = parseInt(sParam[1]);
            else if (sParam[0] == "showasos")
                m_bShowASOS = true;
        }
    }
}

function loadDefaults(sUrl)
{
    var oParams = new Object();
    parseUrl(oParams, sUrl);

    oMapOptions =
    {
      center: new google.maps.LatLng(oParams.lat, oParams.lon),
      zoom: oParams.zoom,
      zoomControlOptions: {
          style: google.maps.ZoomControlStyle.LARGE,
          position: google.maps.ControlPosition.RIGHT_TOP
      },
      panControlOptions: {
          position: google.maps.ControlPosition.RIGHT_TOP
      },
      mapTypeId: google.maps.MapTypeId.ROADMAP
    };

    m_oMap = new google.maps.Map(document.getElementById("map_canvas"), oMapOptions);

    google.maps.event.addListener(m_oMap, "mousemove", TrackMouseMovement);
//    google.maps.event.addListener(m_oMap, "moveend", MapWasZoomed);
    // Only allow the Barnes Spatial radius circle to be shown when debugging ASOS stations.
    if (m_bShowASOS)
    {
        google.maps.event.addListener(m_oMap, "singlerightclick", ShowCircle);
    }

//    ComputeBoundaries();

    // Make sure nothing is selected in the MapArea list.
    document.getElementById("jumpList").selectedIndex = 0;
    m_oInfoWindow = new google.maps.InfoWindow({disableAutoPan: true});
}


// ComputeBoundaries()
// This function computes the two sets of boundaries that drive the map display.
// The Viewport is the region defined by what the user can see.
// The Retrieve is the region that is twice as big as the Viewport.
// This allows the user to move the map around a bit without triggering a data retrieval.
function ComputeBoundaries()
{
    // Base the Viewport on the visible extents of the map.
    var oBounds = m_oMap.getBounds();
    m_oViewport_NE_Lat = oBounds.getNorthEast().lat();
    m_oViewport_NE_Lng = oBounds.getNorthEast().lng();
    m_oViewport_SW_Lat = oBounds.getSouthWest().lat();
    m_oViewport_SW_Lng = oBounds().getSouthWest().lng();

    var centerLat = m_oMap.getCenter().lat();
    var centerLng = m_oMap.getCenter().lng();

    var deltaLng = (m_oViewport_NE_Lng - centerLng) * 2;
    var deltaLat = (m_oViewport_NE_Lat - centerLat) * 2;

    m_oRetrieve_NE_Lat = centerLat + deltaLat;
    m_oRetrieve_NE_Lng = centerLng + deltaLng;
    m_oRetrieve_SW_Lat = centerLat - deltaLat;
    m_oRetrieve_SW_Lng = centerLng - deltaLng;
}


function BuildQChTests()
{
    // Store the test names and their label images.
    // NOTE: The img* variables are defined in the <head> portion of the web page,
    //       and refer to images that have been preloaded.
    m_oAllTests.push("QchsSequenceComplete", imgComplete.src);
    m_oAllTests.push("QchsManualFlag", imgManual.src);
    m_oAllTests.push("QchsServiceSensorRange", imgSensorRange.src);
    m_oAllTests.push("QchsServiceClimateRange", imgClimateRange.src);
    m_oAllTests.push("QchsServiceStep", imgStep.src);
    m_oAllTests.push("QchsServiceLike", imgLikeInstrument.src);
    m_oAllTests.push("QchsServicePersist", imgPersistence.src);
    m_oAllTests.push("QchsServiceBarnes", imgBarnesSpatial.src);
    m_oAllTests.push("QchsServiceDewpoint", imgDewpoint.src);
    m_oAllTests.push("QchsServicePressure", imgSealevelPressure.src);
    m_oAllTests.push("QchsModelAnalysis", imgModelAnalysis.src);
    m_oAllTests.push("QchsNeighboringVehicle", imgNeighboringVehicle.src);
    m_oAllTests.push("QchsVehicleStdDev", imgVehicleStdDev.src);
}


function TrackMouseMovement(oMouseLatLong)
{
    m_oMousePos.firstChild.data = "Lat, Lon: " +
                                  oMouseLatLong.latLng.lat().toFixed(6) + ", " +
                                  oMouseLatLong.latLng.lng().toFixed(6);
}


function GetObsList()
{
    m_oTimeSendXml_1 = new Date();

    var oXmlRequest = new XmlRequest();
    // oXmlRequest.getXml("../obsv1/listObsTypes.jsp", cbGetObsList);
    oXmlRequest.getXml("ListObsTypes?" + csrf_nonce_param, cbGetObsList);
}


function DrawDebugRectangles()
{
/*****
    var boxV = new google.maps.Polyline([new GLatLng(m_oViewport_NE_Lat, m_oViewport_NE_Lng),
                              new GLatLng(m_oViewport_SW_Lat, m_oViewport_NE_Lng),
                              new GLatLng(m_oViewport_SW_Lat, m_oViewport_SW_Lng),
                              new GLatLng(m_oViewport_NE_Lat, m_oViewport_SW_Lng),
                              new GLatLng(m_oViewport_NE_Lat, m_oViewport_NE_Lng)],
                              "#000000", 3, 1);
    var boxR = new google.maps.Polyline([new GLatLng(m_oRetrieve_NE_Lat, m_oRetrieve_NE_Lng),
                              new GLatLng(m_oRetrieve_SW_Lat, m_oRetrieve_NE_Lng),
                              new GLatLng(m_oRetrieve_SW_Lat, m_oRetrieve_SW_Lng),
                              new GLatLng(m_oRetrieve_NE_Lat, m_oRetrieve_SW_Lng),
                              new GLatLng(m_oRetrieve_NE_Lat, m_oRetrieve_NE_Lng)],
                              "#FF00FF", 3, 1);
    m_oMap.addOverlay(boxV);
    m_oMap.addOverlay(boxR);
*****/
    DrawBox(42.391388, -96.75389, 43.6083, -96.378);
    DrawBox(48.1347, -93.8784, 48.601833, -92.97047);
}


function DrawBox(fLatSW, fLngSW, fLatNE, fLngNE)
{
    var oOptions =
    {
      strokeColor: "#000",
      strokeOpacity: 1.0,
      strokeWeight: 3,
      path:
      [
    new google.maps.LatLng(fLatSW, fLngSW),
        new google.maps.LatLng(fLatNE, fLngSW),
        new google.maps.LatLng(fLatNE, fLngNE),
        new google.maps.LatLng(fLatSW, fLngNE),
        new google.maps.LatLng(fLatSW, fLngSW)
      ]
    };

    m_oMap.addOverlay(new google.maps.Polyline(oOptions));
}


function LabelClicked(sLabelClicked)
{
    var oUnitType = document.getElementsByName("rbUnits");
    m_sCurrentUnits = sLabelClicked;

    if (sLabelClicked == "m")
        oUnitType[0].checked = true;
    else
        oUnitType[1].checked = true;

    UnitsChanged();
}

function ToggleLayer()
{
    var bVDTMobile = document.getElementById("chVDTMobile").checked;
    var bWxDEMobile = document.getElementById("chWxDEMobile").checked;  
    var bFixed = document.getElementById("chFixed").checked;

    var nIndex = m_oStationData.length;
    while (nIndex-- > 0)
    {
        var oMarker = m_oStationData[nIndex];
        var oLabelDiv = oMarker.m_oLabelDiv;

        if (
                (bVDTMobile && oMarker.m_oSource == 2 && oMarker.m_oStation.ca == "M") ||
                (bWxDEMobile && oMarker.m_oSource == 1 && oMarker.m_oStation.ca == "M") ||
                (bFixed && (oMarker.m_oStation.ca == "P" || oMarker.m_oStation.ca == 'T')))
        {
            oMarker.setVisible(true);
            if (oLabelDiv != undefined)
                oLabelDiv.style.display = "block";
        }
        else
        {
            oMarker.setVisible(false);
            if (oLabelDiv != undefined)
                oLabelDiv.style.display = "none";
        }
    }
}

function UnitsChanged()
{
    //change the value of the flag to show the unit change
    //is triggered
    unitTriggerFlag = 1;
    // Fill the ObsTypes list box.
    // The "false" indicates that we don't want to re-fetch the stations in the region.
        FillObsTypesListbox(false);
}


//-----------------------------------------------------------
// ListboxRemoveAll()  ==> From Intermodal:Utils.js
//      Removes all items in a list box.
//-----------------------------------------------------------
function ListboxRemoveAll(oList)
{
    while (oList.childNodes.length > 0)
    {
        oList.removeChild(oList.lastChild);
    }
}


function cbGetObsList(oXml, oText)
{
    m_oTimeReceiveXml_1 = new Date();
    m_oObsTypes = oXml.documentElement.getElementsByTagName("obsType");
    setTimeout("FillObsTypesListbox(true)", 100);
}


function FillObsTypesListbox(bGetStations)
{
    var oSelect = document.getElementById("obsTypeList");

    // Save the currently-selected index.
    var nCurrIndex = oSelect.selectedIndex;

    // Remove all the elements in the listbox.
    ListboxRemoveAll(oSelect);

    oEntry = document.createElement("option");
    oEntry.appendChild(document.createTextNode("(Select Data to Show)"));
    oEntry.value = "";
    oEntry.setAttribute("selected", "selected");
    oSelect.appendChild(oEntry);

    var sUnitAttribute = "units";
    var oUnitType = document.getElementsByName("rbUnits");
    
    if (oUnitType[1].checked)
    {
        sUnitAttribute = "englishUnits";
    }

    var oRow;
    var sText;
    var sUnits;
    
    for (var i = 0; i < m_oObsTypes.length; i++)
    {
        oRow = m_oObsTypes[i];

        if (oRow.getAttribute("active") == "1")
        {
            sText = oRow.getAttribute("name");
            // If there are any units for the chosen Unit system,
            // then append them to the observation string.
            sUnits = oRow.getAttribute(sUnitAttribute);
            
            if (sUnits != "")
              sText += " (" + sUnits + ")";
            
            oEntry = document.createElement("option");
            oEntry.appendChild(document.createTextNode(sText));
            oEntry.value = oRow.getAttribute("id");
            oSelect.appendChild(oEntry);
        }
    }

    // Tack theStation Code option to the end, since it isn't an ObsType.
    oEntry = document.createElement("option");
    oEntry.appendChild(document.createTextNode("Station Code"));
    oEntry.value = "";
    oSelect.appendChild(oEntry);

    // Re-select the selected index.
    if (nCurrIndex != -1)
        oSelect.selectedIndex = nCurrIndex;

    if (bGetStations)
        GetStations();
    else
    {
        var sObsType = oSelect.childNodes.item(oSelect.selectedIndex).firstChild.data;//cache combo box value.
        // update the div innerHTML for each station, displayed div tag content will change
        var nIndex = m_oStationData.length;
        while (nIndex-- > 0)
        {
            var oMarker = m_oStationData[nIndex];

            var oObsValues = oMarker.m_oObsValues;
            var sLabel = "";
            if (oObsValues != undefined)
            {
                var oValueArray = oMarker.m_oObsValues.mv;
                if (m_sCurrentUnits == "e")
                    oValueArray = oMarker.m_oObsValues.ev;

                sLabel = "";
                
                if(oValueArray.length > 1)
                    sLabel = oValueArray[oValueArray.length - 1];
                else
                    sLabel = oValueArray;
//              for (var nValueIndex = 0; nValueIndex < oValueArray.length; nValueIndex++)
//              {
//                  if (nValueIndex > 0)
//                      sLabel += ", ";
//
//                  sLabel += oValueArray[nValueIndex];
//              }
            }
            sTextStation = oMarker;
            //Prevents from changing values when conditions are true
            if (sObsType !== 'Station Code' && sObsType !== '(Select Data to Show)')
                oMarker.m_oLabel.setText(sLabel);
        }
        if(currentStation !== null) //&& m_oInfoWindow.getMap() == m_oMap)
            ShowInfoWindow(currentStation);
    }
}


function GetStations()
{
    // Turn off various objects until the XML data is returned.
    DisableObjects(true);

    // Display a "Retrieving data" label
    ShowProgressLabel();

    m_oTimeSendXml_2 = new Date();
    var oXmlRequest = new XmlRequest();
    oXmlRequest.addParameter("lat1", m_oRetrieve_SW_Lat);
    oXmlRequest.addParameter("lng1", m_oRetrieve_SW_Lng);
    oXmlRequest.addParameter("lat2", m_oRetrieve_NE_Lat);
    oXmlRequest.addParameter("lng2", m_oRetrieve_NE_Lng);
    oXmlRequest.addParameter("format", "json");
    if (m_bShowASOS)
      oXmlRequest.addParameter("showasos", "1");

    oXmlRequest.getXml("GetPlatformsForRegion?" + csrf_nonce_param, cbGetStations);
}


function CreateIcon(oIcon, sImage, nWidth, nHeight, nX, nY)
{
    oIcon.anchor = new google.maps.Point(nX, nY);
    oIcon.size = new google.maps.Size(nWidth, nHeight);
    oIcon.url = sImage;
}


// ShowStationCode()
// This function displays the Station Code in a text field
// whenever the mouse runs over it.
function ShowStationCode(oMarker)
{
    m_oStationCode.firstChild.data = "Station Code: " + oMarker.m_oStation.st;
}

var ShowInfoWindow = function(oMarker)
{
    m_oInfoWindow.close();
    m_oSelectedMarker = oMarker;
    var oXmlRequest = new XmlRequest();
    oXmlRequest.addParameter("sourceId", oMarker.m_oSource);
    oXmlRequest.addParameter("stationId", oMarker.m_oStation.id);
    oXmlRequest.addParameter("lat", oMarker.m_oStation.lt);
    oXmlRequest.addParameter("lon", oMarker.m_oStation.ln);
    oXmlRequest.getXml("GetPlatformsForRegion?" + csrf_nonce_param, cbGetObsForStation);
};

/*
function ShowInfoWindowForSegment(oMarker)
{
    m_oInfoWindow.close();
    m_oSelectedMarker = oMarker;
    var oXmlRequest = new XmlRequest();
    oXmlRequest.addParameter("sourceId", oMarker.m_oSource);
    oXmlRequest.addParameter("stationId", oMarker.m_oStation.id);
    oXmlRequest.addParameter("lat", oMarker.m_oStation.lt);
    oXmlRequest.addParameter("lon", oMarker.m_oStation.ln);
    oXmlRequest.getXml("GetPlatformsForRegion", cbGetObsForStation);
}
*/

var cbGetObsForStation = function(oXml, sText, trigger)
{
    var oStationObs = eval("(" + sText + ")");
    var oObs = oStationObs.ob;
    var oMarker = m_oSelectedMarker;

    // Early bailout
    if (oMarker == null)
        return;
    
    var oStation = oMarker.m_oStation;

    // Figure out which Unit Types are being displayed.
    var bShowEnglishUnits = false;
    var oUnitType = document.getElementsByName("rbUnits");
    bShowEnglishUnits = oUnitType[1].checked;

    //begin opening for table which is similar in all if statements anyway
    var sObsTableHeader = '<div class="gmap-info-window" id="gmapIW">' +
                            '<table class="qualityChecks" id="qualityChecks">' +
                                '<thead class="obs-table-head">';
                            
    // CHECK IF DATA SOURCE IS FROM WxDE
    if (oMarker.m_oSource == 1) {
        if (oMarker.m_oStation.ca != "S") {
        sObsTableHeader +=
            "  <tr align=\"center\">" +
            "    <td colspan=\"6\" class=\"td-title\">" + oStation.st + "<br/>" + oStationObs.nm + "<br/>" + "Lat, Lon: " + oStation.lt + ", " + oStation.ln + "<br/>Elevation: " + oStationObs.el + " m</td>" +
            "    <td class=\"td-image no-border-left webkit-td-image-fix\" rowspan=\"2\"><img src=\"" + imgComplete.src + "\" alt=\"Complete\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgManual.src + "\" alt=\"Manual\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgSensorRange.src + "\" alt=\"Sensor Range\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgClimateRange.src + "\" alt=\"Climate Range\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgStep.src + "\" alt=\"Step\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgLikeInstrument.src + "\" alt=\"Like Instrument\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgPersistence.src + "\" alt=\"Persistence\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgIQR.src + "\" alt=\"Inter-quartile Range\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgBarnesSpatial.src + "\" alt=\"Barnes Spatial\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgDewpoint.src + "\" alt=\"Dewpoint\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgSealevelPressure.src + "\" alt=\"Sea Level Pressure\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgPrecipAccum.src + "\" alt=\"Accumulated Precipitation\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgModelAnalysis.src + "\" alt=\"Model Analysis\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgNeighboringVehicle.src + "\" alt=\"Neighboring Vehicle\"/></td>" +
            "    <td class=\"td-image\" rowspan=\"2\"><img src=\"" + imgVehicleStdDev.src + "\" alt=\"Vehicle Standard Deviation\"/></td>";

        } else {
            sObsTableHeader +=
                "  <tr align=\"center\">" +
                "    <td colspan=\"6\" class=\"td-title\">" + 
                oStation.st + "<br/>" + 
                oStationObs.nm + "<br/>" + "Lat, Lon: " + 
                oStation.lt + ", " + 
                oStation.ln + "<br/>Elevation: " + 
                oStationObs.el + " m</td>";
        }
        sObsTableHeader = sObsTableHeader +
            "  </tr>" +
            "  <tr class=\"last-tr\">" +
            "    <td class=\"timestamp\"><b>Timestamp (UTC)</b></td>";
        if (oMarker.m_oStation.ca != "S") {
        	sObsTableHeader +=
        		"    <td class=\"obsType\"><b>Observation Type</b></td>";
        } else {
        	sObsTableHeader +=
            	"    <td class=\"segObsType\"><b>Observation Type</b></td>";
        }
        // CHECK IF TABLE IS NOT FOR SEGMENT DATA, IF FALSE SKIP THE "Ind" TABLE HEADER
        if (oMarker.m_oStation.ca != "S") {
            sObsTableHeader += "    <td class=\"td-ind\"><b>Ind</b></td>";
        }
        // CONTINUE ADDING THE OTHER GENERAL HEADERS
        sObsTableHeader +=
            "    <td class=\"td-value\"><b>Value</b></td>" +
            "    <td class=\"unit\"><b>Unit</b></td>";
        
        // CHECK IF DATA IS FROM VDT, EXECUTE IF TRUE ELSE SKIP
        if (oMarker.m_oStation.ca != "S") {
            sObsTableHeader +=    "    <td class=\"conf webkit-td-conf-fix\"><b>Conf</b></td>";
        }
        
        //CLOSE THE TABLE ROW <TR> FOR THE HEADERS
        sObsTableHeader += "  </tr>";
    // ******************************
    } else { // ELSE DATA IS FROM VDT
        // CHECK IF TABLE IS NOT FOR SEGMENTS BUT FOR VDT MOBILE
        if (oMarker.m_oStation.ca != "S") {
        sObsTableHeader +=
            "  <tr align=\"center\">" +
            "    <td colspan=\"6\" class=\"td-title\">" + oStation.st + "<br/>" + oStationObs.nm + "<br/>" + "Lat, Lon: " + oStation.lt + ", " + oStation.ln + "<br/>Elevation: " + oStationObs.el + " m</td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgCombinedAlgorithm.src + "\" alt=\"Combined Algorithm\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgClimateRange.src + "\" alt=\"Climate Range\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgModelAnalysis.src + "\" alt=\"Model Analysis\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgNearestSurfaceStation.src + "\" alt=\"Nearest Surface Station\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgNeighboringVehicle.src + "\" alt=\"Neighboring Vehicle\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgPersistence.src + "\" alt=\"Persistence\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgSensorRange.src + "\" alt=\"Sensor Range\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgStandardDeviation.src + "\" alt=\"Standard Deviation\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgSpatialBarnes.src + "\" alt=\"Spatial Barnes\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgSpatialIOR.src + "\" alt=\"Spatial IQR\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgTimeStep.src + "\" alt=\"Time Step\"/></td>" +
            "    <td rowspan=\"2\"><img src=\"" + imgOverallDewTemperature.src + "\" alt=\"Overall Dew Temperature\"/></td>" +            
            "    <td rowspan=\"2\"><img src=\"" + imgFiltering.src + "\" alt=\"Filtering\"/></td>";
        } 
        // ******************************
        else { // ELSE IT IS FOR SEGMENTS
            sObsTableHeader +=
                "  <tr align=\"center\">" +
                "    <td colspan=\"6\" class=\"td-title\">" + 
                oStation.st + "<br/>" + 
                oStationObs.nm + "<br/>" + "Lat, Lon: " + 
                oStation.lt + ", " + 
                oStation.ln + "<br/>Elevation: " + 
                oStationObs.el + " m</td>";         
        }
        sObsTableHeader +=
            "  </tr>" +
            "  <tr align=\"center\">" +
            "    <td class=\"timestamp\"><b>Timestamp (UTC)</b></td>";
        if (oMarker.m_oStation.ca != "S") {
        	sObsTableHeader +=
        		"    <td class=\"obsType\"><b>Observation Type</b></td>";
        } else {
        	sObsTableHeader +=
            	"    <td class=\"segObsType\"><b>Observation Type</b></td>";
        }

        // CHECK IF TABLE IS NOT FOR SEGMENTS AND ADD COLUMN HEADER "Ind"
        // OTHERWISE SKIP THIS FOR SEGMENTS
        if (oMarker.m_oStation.ca != "S") {
            sObsTableHeader += "    <td class=\"td-ind\"><b>Ind</b></td>";
        }
        // CARRY ON WITH ADDING THE OTHER GENERAL COLUMNS
        sObsTableHeader +=
            "    <td class=\"td-value\"><b>Value</b></td>" +
            "    <td class=\"unit\"><b>Unit</b></td>";
//       COMMENTED OUT, SEEMS FUZZY LOGIC because we already know the data is from VDT
//      // CHECK IF SOURCE IS NOT FROM VDT, IF TRUE SKIP APPENDING THE "Conf" HEADER
//        if (oMarker.m_oSource != 2) {
//          sObsTableHeader += "    <td class=\"conf webkit-td-conf-fix\"><b>Conf</b></td>";
//      }
        // CLOSE TABLE ROW <TR>
        sObsTableHeader += "  </tr>";
    }
    // CLOSE <THEAD> FOR THE TABLE HEADERS
    sObsTableHeader += '</thead>';
    // OPEN <TBODY> FOR DATA CELLS OF THE TABLE
    var sObsTableRows = '<tbody class="obs-table-body">';

    if (oObs.length > 0)
    {
        // SORT OBS ARRAY BY OBSTYPE NAME AND THEN SENSOR INDEX
        var oCompare =
        {
            compare : function(oLhs, oRhs)
            {
                if (oMarker.m_oStation.ca != "M") {
                    if (oLhs.ot == oRhs.ot)
                    {
                        if (oLhs.si == oRhs.si)
                            return 0;
    
                        if (oLhs.si < oRhs.si)
                            return -1;
    
                        return 1;
                    }
    
                    if (oLhs.ot < oRhs.ot)
                        return -1;
                }
                else {
                    if (oLhs.ts == oRhs.ts)
                        return 0;

                    if (oLhs.ts < oRhs.ts)
                        return 1;

                    return -1;
                }

                return 1;
            }
        };

        js.util.Collections.usort(oObs, oCompare);

        // NOW, BUILD THE TABLE OF OBSERVATIONS
        for (var i = 0; i < oObs.length; i++)
        {
            var iObs = oObs[i];
            
            // ADD CLASS first-tr TO THE VERY FIRST ROW FOR BORDER PURPOSES
            if ( i === 0 ) {
                sObsTableRows += "<tr class='first-tr' onMouseover='this.bgColor=\"#DDDDDD\";setRolloverPoint(" + iObs.lt + "," + iObs.ln + ");' onMouseout='this.bgColor=\"\";rolloverPoint.setMap(null);'>";
                //sObsTableRows += "<tr class='first-tr'>";
            } else {
                sObsTableRows += "<tr onMouseover='this.bgColor=\"#DDDDDD\";setRolloverPoint(" + iObs.lt + "," + iObs.ln + ");' onMouseout='this.bgColor=\"\";rolloverPoint.setMap(null);'>";
                //sObsTableRows += "<tr>";
            }
            
            sObsTableRows += "    <td class=\"timestamp\">" + iObs.ts + "</td>";
            if (oMarker.m_oStation.ca != "S") {
            	sObsTableRows += "    <td class=\"obsType\">" + iObs.ot + "</td>";
            }
            else {
            	sObsTableRows += "    <td class=\"segObsType\">" + iObs.ot + "</td>";
            }

            // APPEND COLUMN "Ind" IF NOT SEGMENT
            if (oMarker.m_oStation.ca != "S") {
                sObsTableRows += "    <td class=\"td-ind\">" + iObs.si + "</td>";
            }

            // CONTINUE TO AD THE OBSERVATION VALUE DATA CELLS
            sObsTableRows += "    <td class=\"td-value\">";

            if (bShowEnglishUnits)
                sObsTableRows += iObs.ev;
            else
                sObsTableRows += iObs.mv;

            sObsTableRows += "</td>";

            // ADD THE APPROPRIATE UNITS LEFT-JUSTIFIED
            sObsTableRows += "    <td class=\"unit\">";
            // ONLY DISPLAY UNITS WHEN THEY ARE NOT NULL
            if (iObs.mu != "null" && iObs.eu != "null")
            {
                if (bShowEnglishUnits)
                    sObsTableRows += iObs.eu;
                else
                    sObsTableRows += iObs.mu;
            }
            sObsTableRows += "</td>";

            // SHOW THE CONFIDENCE "Conf" VALUE RIGHT-JUSTIFIED     
            // CHECK IF DATA SOURCE IS NOT FROM VDT AND EXECUTE CODE, OTHERWISE SKIP
            if (oMarker.m_oSource != 2) {
                sObsTableRows += "    <td class=\"conf\">" + iObs.cv + "%</td>";
            }

            // CHECK IF NOT SEGMENT TABLE AND EXECUTE THE FF:
            if (oMarker.m_oStation.ca != "S") {
                var sRun = Number(iObs.rf).toString(2);
                while (sRun.length < QCH_MAX)
                    sRun = "0" + sRun;
    
                var sPass = Number(iObs.pf).toString(2);
                while (sPass.length < QCH_MAX)
                    sPass = "0" + sPass;

                var nIndex = sRun.length;
                while (nIndex-- > 0)
                {
                    var nRow = Number(sRun.charAt(nIndex));
                    var nCol = Number(sPass.charAt(nIndex));
                    var oFlag = m_oQCh[nRow][nCol];
    
                    sObsTableRows += "    <td><img alt='Icon' src=\"image/";
                    sObsTableRows += oFlag.im;
                    sObsTableRows += ".png\" alt=\"";
                    sObsTableRows += oFlag.tx;
                    sObsTableRows += "\"/></td>";
                }
            }
        }
    }
    else
    {
        // NO OBSERVATIONS FOR THIS Station
        sObsTableRows +=
            "  <tr align=\"center\" class=\"no-data\">" +
            "    <td colspan=\"18\" class=\"no-data\">No current observations available</td>";
        /* REMOVED FOR VISUAL PURPOSES, COMPLETELY UNNECESSARY WHEN EMPTY
        if (oMarker.m_oStation.ca != "S") {
            sObsTableRows = sObsTableRows +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>" +
            "<td>&nbsp;</td>";
        }
        */
    }
    // END TABLE CONTENT ROWS WHICH IS SIMILAR IN ALL IF ELSE ANYWAY
    sObsTableRows += "</tr></tbody>";
    //end the entire table with the footer
    var sObsTableFooter = "</table></div>";
    //concatinate all table parts.
    observationTable = sObsTableHeader + sObsTableRows + sObsTableFooter;
    //begin setup for InfoWindow
    m_oInfoWindow.setOptions({ content : observationTable, maxWidth : 651, maxHeight : 450 });
    //change content of the "shade" with the table
    sideBar.innerHTML = sideBarContent + observationTable + btnViewStation; //+ btnFullScreen;
    
    currentStation = oMarker;
    
    if(hasClass(btnShade, 'active')) {
    //add animation effect to style
        if(unitTriggerFlag === null) {
            markerInfoWindow.open(m_oMap, currentStation);
            animate({
                delay: 10,
                duration: 800,
                delta: quad,
                step: function(delta) {
                    sideBar.style.left = 0 * delta + 'px';
                }
            });
        }
    
    } else {
        m_oInfoWindow.open(m_oMap, oMarker);
    }
};

//Additional tools
//Our own vanilla javascript animation
function linear(progress) {
      return progress;
}
function quad(progress) {
    return Math.pow(progress, 5);
}
function animate(opts) {
      var start = new Date  ;

      var id = setInterval(function() {
        var timePassed = new Date - start;
        var progress = timePassed / opts.duration;

        if (progress > 1) progress = 1;
         
        var delta = opts.delta(progress);
        opts.step(delta);

        if (progress == 1) {
          clearInterval(id);
        }
      }, opts.delay || 10);
       
    }
// checks if element has a class in cls
function hasClass(element, cls) {
    return (' ' + element.className + ' ').indexOf(' ' + cls + ' ') > -1;
}

function setRolloverPoint(lt, ln) {
//  console.log("lt->" + lt + "ln->" + ln);
    var oOptions =
    {
        anchorPoint: new google.maps.Point(1, 1),
        position: new google.maps.LatLng(lt, ln),
        icon: m_oWhiteIcon
    };

    rolloverPoint = new google.maps.Marker(oOptions);
    rolloverPoint.setMap(m_oMap);
}

function cbGetStations(oXml, oText)
{
    m_oStationXmlText = oText;
    var oStationXmlData = eval(oText);

    // REMOVE DATA FOR ASOS STATIONS IF THEY ARE NOT GOING TO BE DISPLAYED
    var nIndex = oStationXmlData.length;
    if (!m_bShowASOS)
    {
        var oTempArray = new Array();
        while (nIndex-- > 0)
        {
            var oStation = oStationXmlData[nIndex];

            // ONLY SAVE THE STATIONS OUTSIDE THE ASOS RANGES
            if (oStation.cn != 4)
                oTempArray.push(oStation);
        }
        oStationXmlData = oTempArray;
    }

    
    nIndex = oStationXmlData.length;

    while (nIndex-- > 0)
    {
        var oStation = oStationXmlData[nIndex];
        var icon = m_oGrayIcon;
        var oSource = 1;
        if (oStation.cn == 4)
        {
            if (oStation.ho == 0)
                icon = m_oGrayWhiteIcon;
            else
                icon = m_oWhiteIcon;
        }
        else
        {
            if (oStation.ho == 1)
            {
                if (oStation.ca == "P" || oStation.ca == "T")
                    icon = m_oPurpleIcon;
                else
                    icon = m_oBlueIcon;
            }
            else if (oStation.ho == 2)
            {
                oSource = 2;
                icon = m_oBrownIcon;
            }
            else if (oStation.ho == 3)
                icon = m_oBlueIcon; // CREATE THE ONE FOR WXDE MOBILE
        }
        
        PopulateMarker(oStation, oSource, icon);
        
        if (oStation.ho == 3) 
        {
            // now create the one for VDT
            oSource = 2;
            icon = m_oBrownIcon;
            PopulateMarker(oStation, oSource, icon);
        }
    }
    
    js.util.Collections.usort(m_oStationData, m_oMarkerCompare);

    // Re-enable the screen objects.
    DisableObjects(false);

    HideProgressLabel();
//  google.maps.event.addListener(m_oMap, "zoomend", MapWasZoomed);

    //    m_oTimeDoneParsing = new Date();

    // DrawDebugRectangles();

    nIndex = oStationXmlData.length;

    // Segments
    var segmentCount = 0;
    var colorHasObs = "#9400D3";
    var colorNoObs = "#BFBFBF";
    var segmentHover = "#FFFF00";
    var zIndexHigh = 1; //variable for zIndex for all segments which increments and finally becomes the highest z-index available
    var tempZIndex = 0; //holds the zIndex for "the" segment the user mouseenters/hovers on
    

    $.each(oStationXmlData, function(index, item) {

        // For segments
        if ((item.ca == 'S' && item.sg != null)) {
            segmentCount++;
    
            var routePathCoordinates2 = new Array();
            for (var x = 0; x < item.sg.length-1; x++) {
                if (x%2 == 0) {
                    routePathCoordinates2.push(new google.maps.LatLng(item.sg[x], item.sg[x+1]));
                }
            }
            var routePath = new google.maps.Polyline({
               path: routePathCoordinates2,
               geodesic: true,
               strokeColor: (item.ho == 0 ? colorNoObs : colorHasObs),
               strokeOpacity: 1.0,
               strokeWeight: 5,
               zIndex : zIndexHigh++ //set zIndex and increment it
             });
         
            routePath.setMap(m_oMap);
    
            google.maps.event.addListener(routePath, "click", function() {
                var tempMarker = $.grep(m_oStationData, function(e) { return e.m_oStation.id == item.id; });
                //reset the flag for unit changed trigger
                unitTriggerFlag = null;
                ShowInfoWindow(tempMarker[0]);
            });
            google.maps.event.addListener(routePath, "mouseover", function() {
                tempZIndex = routePath.zIndex;
                routePath.setOptions({
                    strokeColor: segmentHover,
                    zIndex: zIndexHigh + 1 //set the z-index to the highest z-index available plus 1 for it to be the topmost segment on mouseover
                });
                m_oStationCode.firstChild.data = "Station Code: " + item.st;
            });
            google.maps.event.addListener(routePath, "mouseout", function() {
                routePath.setOptions({
                    strokeColor: (item.ho == 0 ? colorNoObs : colorHasObs),
                    zIndex: tempZIndex //set the z-index back to it's original set upon mouseover earlier
                });
                m_oStationCode.firstChild.data = "Station Code: ";
            });
            
            
            //append routePath to the routePaths array
            routePaths.push(routePath);     
        
        } else if (item.ca == 'M') {

            var mobilePathCoordinates = new Array();

            // build one for a mobile station
            if (item.wxde != null) {
                
                // build the path
                for (var x = 0; x < item.wxde.length-1; x++) {
                    if (x%2 == 0) {
                        mobilePathCoordinates.push(new google.maps.LatLng(item.wxde[x], item.wxde[x+1]));
                    }
                }
                var routePath = new google.maps.Polyline({
                   path: mobilePathCoordinates,
                   geodesic: true,
                   strokeColor: segmentHover,
                   strokeOpacity: 1.0,
                   strokeWeight: 5,
                   zIndex : zIndexHigh++ //set zIndex and increment it
                 });
                     
                // now build the marker
                var mobileMarker;
                var oOptions =
                {
                    anchorPoint: new google.maps.Point(1, 1),
                    position: new google.maps.LatLng(item.lt, item.ln),
                    icon: m_oBlueIcon
                };

                mobileMarker = new google.maps.Marker(oOptions);
                mobileMarker.m_oSource = 1;
                mobileMarker.m_oStation = new Object();;
                mobileMarker.m_oStation.st = item.st;
                mobileMarker.m_oStation.id = item.id;
                mobileMarker.m_oStation.ca = item.ca;
                mobileMarker.m_oStation.cn = item.cn;
                mobileMarker.m_oStation.ho = item.ho;
                mobileMarker.m_oStation.lt = item.lt;
                mobileMarker.m_oStation.ln = item.ln;
                mobileMarker.setMap(m_oMap);
                mobileMarker.returnIcon = m_oBlueIcon;
                mobileMarker.routePath = routePath;
                
                // add listeners to the marker              
                google.maps.event.addListener(mobileMarker, "mouseover", function() {
                    this.routePath.setMap(m_oMap);
                    this.setIcon(m_oYellowIcon);
                    m_oStationCode.firstChild.data = "Station Code: " + item.st;
                });
                var mobileMouseOutListener = google.maps.event.addListener(mobileMarker, "mouseout", function() {
                    this.routePath.setMap(null);
                    this.setIcon(m_oBlueIcon);
                    m_oStationCode.firstChild.data = "Station Code: ";
                });
                google.maps.event.addListener(mobileMarker, "click", function() {
                    resetMarker();
                    this.routePath.setMap(m_oMap);
                    this.setIcon(m_oYellowIcon);
                    google.maps.event.removeListener(mobileMouseOutListener);
                    activeMarker = this;
                    //reset the flag for unit changed trigger
                    unitTriggerFlag = null;
                    ShowInfoWindow(this);
                });

                //Add event listener for oMarker for the shade's indication
                //of which dot was clicked

                mobileMarkersWithRoutes.push(mobileMarker);
            }
            
            // now build one for VDT
            var vdtPathCoordinates = new Array();
            var vdtMarker;
            if (item.vdt != null) {
                
                // build the path
                for (var x = 0; x < item.vdt.length-1; x++) {
                    if (x%2 == 0) {
                        vdtPathCoordinates.push(new google.maps.LatLng(item.vdt[x], item.vdt[x+1]));
                    }
                }
                routePath = new google.maps.Polyline({
                   path: vdtPathCoordinates,
                   geodesic: true,
                   strokeColor: segmentHover,
                   strokeOpacity: 1.0,
                   strokeWeight: 5,
                   zIndex : zIndexHigh++ //set zIndex and increment it
                });
                
                // now build the marker
                oOptions =
                {
                    anchorPoint: new google.maps.Point(1, 1),
                    position: new google.maps.LatLng(item.lt, item.ln),
                    icon: m_oBlueIcon
                };

                vdtMarker = new google.maps.Marker(oOptions);
                vdtMarker.m_oSource = 2;
                vdtMarker.m_oStation = new Object();;
                vdtMarker.m_oStation.st = item.st;
                vdtMarker.m_oStation.id = item.id;
                vdtMarker.m_oStation.ca = item.ca;
                vdtMarker.m_oStation.cn = item.cn;
                vdtMarker.m_oStation.ho = item.ho;
                vdtMarker.m_oStation.lt = item.lt;
                vdtMarker.m_oStation.ln = item.ln;
                vdtMarker.setMap(m_oMap);
                //vdtMarker.returnIcon = m_oBrownIcon;
                vdtMarker.returnIcon  = m_oBlueIcon;
                vdtMarker.routePath = routePath;
                
                // add listeners to the marker
                google.maps.event.addListener(vdtMarker, "mouseover", function() {
                    this.routePath.setMap(m_oMap);
                    this.setIcon(m_oYellowIcon);
                    m_oStationCode.firstChild.data = "Station Code: " + item.st;
                });
                
                var vdtMouseOutListener = google.maps.event.addListener(vdtMarker, "mouseout", function() {
                    this.routePath.setMap(null);
                    this.setIcon(m_oBlueIcon);
                    m_oStationCode.firstChild.data = "Station Code: ";
                });

                google.maps.event.addListener(vdtMarker, "click", function() {
                    resetMarker();
                    this.routePath.setMap(m_oMap);
                    this.setIcon(m_oYellowIcon);
                    google.maps.event.removeListener(vdtMouseOutListener);
                    activeMarker = this;
                    //reset the flag for unit changed trigger
                    unitTriggerFlag = null;
                    ShowInfoWindow(vdtMarker);
                });
                

                vdtMarkersWithRoutes.push(vdtMarker);
            }
        }
    });
    
/*  console.log("total segmentCount -> " + segmentCount);
    console.log("total mobileWxdeCount -> " + mobileWxdeCount);
    console.log("total mobileVdtCount -> " + mobileVdtCount);
    console.log("total mobileMarkersWithRoutes -> " + mobileMarkersWithRoutes.length);
    console.log("total vdtMarkersWithRoutes -> " + vdtMarkersWithRoutes.length);
*/
}

function resetMarker() {
    if (activeMarker) {
        if (activeMarker.routePath) {
            activeMarker.routePath.setMap(null);
        }
        if (activeMarker.returnIcon) {
            activeMarker.setIcon(activeMarker.returnIcon);
        }
    }
}

function resetMouseOver() {
    if (activeMarker) {
        google.maps.event.addListener(activeMarker, "mouseout", function() {
            if (this.routePath) {
                this.routePath.setMap(null);
            }
            this.setIcon(activeMarker.returnIcon);
            m_oStationCode.firstChild.data = "Station Code: ";
        }); 
    }
}

//function to toggle ALL segments
function toggleSegments() {
    // this should initially be TRUE
    var toggleCheck = document.getElementById("toggleSegments").checked;
    
    if (!toggleCheck) {
        for ( var x = 0; x < routePaths.length; x++) {
            routePaths[x].setVisible(false);
        }
    } else {
        for ( var x = 0; x < routePaths.length; x++) {
            routePaths[x].setVisible(true);
        }
    }
}

function PopulateMarker(oStation, oSource, icon)
{
    var oMarker;
    var oOptions =
    {
        anchorPoint: new google.maps.Point(1, 1),
        position: new google.maps.LatLng(oStation.lt, oStation.ln),
        icon: m_oPurpleIcon,
        flat: true
    };
    oOptions.icon = icon;
    oOptions.returnIcon = icon;
    oMarker = new google.maps.Marker(oOptions);

    // attach mouse events to each marker
    google.maps.event.addListener(oMarker, "mouseover", function(oMarker) {
        ShowStationCode(this);
        this.setIcon(m_oYellowIcon);
    });

    var mouseOutListener = google.maps.event.addListener(oMarker, "mouseout", function() {
        m_oStationCode.firstChild.data = "Station Code: ";
        this.setIcon(icon);
    });
    
    google.maps.event.addListener(oMarker, "click", function(oMarker) {
        resetMarker();
        google.maps.event.removeListener(mouseOutListener);  //TODO:  Great, you've removed the mouseout listener.  Now, put it back in when the info window closes.
        activeMarker = this;
        //reset the flag for unit changed trigger
        unitTriggerFlag = null;
        ShowInfoWindow(this);
    });

    // attach the parsed station data to the map marker
    oMarker.m_oSource = oSource;
    oMarker.m_oStation = oStation;
    oMarker.m_oLabel = new LabelOverlay(oOptions);
    oMarker.newValidity = true;
    m_oStationData.push(oMarker);
    oMarker.setMap(m_oMap);
    oMarker.m_oLabel.setMap(m_oMap);
    
    // hide segment center point
    if (oStation.ca == 'S' && oStation.sg != null) {
        oMarker.setVisible(false);
        segmentCount++;
    }
    // hide segment center point
    if (oStation.ca == 'M') {
        if  (oStation.wxde != null) {
            oMarker.setVisible(false);
            mobileWxdeCount++;
        }
        
        if  (oStation.vdt != null) {
            oMarker.setVisible(false);
            mobileVdtCount++;
        }
    }
}

function DisableObjects(bDisabled)
{
    var oShowObs = document.getElementById("btn_show");
    var oObsTypeList = document.getElementById("obsTypeList");
    var oJumpList = document.getElementById("jumpList");

    if (oShowObs != null)
        oShowObs.disabled = bDisabled;

    if (oObsTypeList != null)
        oObsTypeList.disabled = bDisabled;

    if (oJumpList != null)
        oJumpList.disabled = bDisabled;
}

function Debug(sText)
{
    alert("DEBUG:\n" + sText);
}

//This gets triggered when the value in the combo box
//for Observation Types is changed.
function GetObsValue()
{
    var oSelect = document.getElementById( "obsTypeList" );//cache combo box.
    var sObsType = oSelect.childNodes.item(oSelect.selectedIndex).firstChild.data;//cache combo box value.
    var nIndex = m_oStationData.length;

    ShowProgressLabel();

    //Resets m_oObsValues of all Markers/Station
    for( var mIndex = 0, len = m_oStationData.length; mIndex < len; mIndex++ ) {
        oMarker = m_oStationData[mIndex];
        oMarker.m_oObsValues = undefined;
        oMarker.m_oLabel.m_oDataOver.innerHTML = "";
        oMarker.m_oLabel.setText("");
    }

    //Show station code and return to prevent XHR request
    if ( sObsType == "Station Code" ) {
        nIndex = m_oStationData.length;
        while (nIndex-- > 0) {
            var oMarker = m_oStationData[nIndex];
            sTextStation = oMarker;
            oMarker.m_oLabel.setText(oMarker.m_oStation.st);
        }
        HideProgressLabel();
        return; // quit early
    }
    
    //Empty dataOver and return to prevent XHR request
    if ( sObsType == "(Select Data to Show)" ) {
        nIndex = m_oStationData.length;
        while (nIndex-- > 0) {
            var oMarker = m_oStationData[nIndex];
            sTextStation = oMarker;
            oMarker.m_oLabel.setText("");
        }
        HideProgressLabel();
        return; // quit early
    }

    var oXmlRequest = new XmlRequest(); // request the obs values
    oXmlRequest.addParameter("obsType", oSelect.value);
    oXmlRequest.getXml("GetPlatformsForRegion?" + csrf_nonce_param, cbGetObsValue);
}


function cbGetObsValue(oXml, sText)
{
    HideProgressLabel();

    //var oValueArray = undefined;
    var oObsValues = eval(sText);
    
    if(oObsValues != undefined) { // 1
        //locate stations with data and assigns mv and ev values to
        //those stations
        for (var nIndex = 0; nIndex < oObsValues.length; nIndex++)
        {// 2
            var oValues = oObsValues[nIndex];
            if (oValues.mv != undefined && !isNaN(oValues.mv)) // when metric units exist english units exist
            {
                oValues.m_oStation = new Object(); // create station for the comparison
                oValues.m_oStation.id = oValues.id;
                oValues.m_oStation.lt = oValues.lt;
                oValues.m_oStation.ln = oValues.ln;
                oValues.m_oStation.si = oValues.si;
    
                var nMarkerIndex = js.util.Collections.binarySearch(m_oStationData, oValues, m_oMarkerCompare);
                
                if (nMarkerIndex >= 0)
                {
                    var oMarker = m_oStationData[nMarkerIndex];
                    
                    if(oMarker.m_oSource == oValues.m_oStation.si) {
                        oMarker.m_oObsValues = oValues;
                    } else {
                        oMarker.m_oObsValues = undefined;
                        
                        if(oValues.m_oStation.si == 1)
                            oMarker = m_oStationData[nMarkerIndex + 1];
                        else
                            oMarker = m_oStationData[nMarkerIndex - 1];
                        
                        oMarker.m_oObsValues = oValues;
                    }
                    
                    var sLabel = oMarker.m_oObsValues.mv;
                        if (m_sCurrentUnits == "e")
                            sLabel = oMarker.m_oObsValues.ev;

                        sTextStation = oMarker;
                        oMarker.m_oLabel.setText(sLabel);
                }
            }
        }//end 2
    }//end 1
}


function StartMapClock()
{
    m_oClock.firstChild.data = FormatCurrentTime();

    // Update the clock every 60 seconds, at the top of the minute.
    setTimeout(StartMapClock, (1000 * (60 - new Date().getSeconds())));
}


function ShowProgressLabel()
{
    var oDivLabel = document.getElementById("progressLabel");
    oDivLabel.style.top = ((GetWinDimension("height") / 2) - 24) + "px";
    oDivLabel.style.visibility = "visible";
}


function HideProgressLabel()
{
    document.getElementById("progressLabel").style.visibility = "hidden";
}


function ClearCircle()
{
    // Remove the previous circle, if any.
    if (m_oCircleOverlay != null)
    {
        m_oMap.removeOverlay(m_oCircleOverlay);
    }
}


function DrawCircle(oCenterPoint, oRadiusPoint, sLineColor, nLineWeight)
{
    var oPoints = [];
    var nIndex = m_dCircleY.length;
    while (nIndex-- > 0)
    {
        var dLatOffset = oCenterPoint.lat() + m_dCircleY[nIndex];
        oPoints.push(new google.maps.LatLng(dLatOffset, oCenterPoint.lng() + m_dCircleX[nIndex] / Math.cos(dLatOffset * Math.PI / 180)));
    }

    m_oCircleOverlay = new google.maps.Polyline(oPoints, sLineColor, nLineWeight, 1);
    m_oMap.addOverlay(m_oCircleOverlay);
}


// DrawCircle() is based on
// http://maps.forum.nu/gm_clickable_circle.html
function DrawCircle2(oCenterPoint, oRadiusPoint, sLineColor, nLineWeight)
{
    var oNormalProj = G_NORMAL_MAP.getProjection();
    var zoom = m_oMap.getZoom();

    var centerPt = oNormalProj.fromLatLngToPixel(oCenterPoint, zoom);
    // var radiusPt = m_NormalProj.fromLatLngToPixel(radiusMarker, zoom);
    var radiusPt = oNormalProj.fromLatLngToPixel(oRadiusPoint, zoom);

    var circlePoints = Array();

    with (Math)
    {
        var radius = floor(sqrt(pow((centerPt.x-radiusPt.x),2) + pow((centerPt.y-radiusPt.y),2)));

        for (var a = 0 ; a < 361 ; a+=10 )
        {
            var aRad = a*(PI/180);
            y = centerPt.y + radius * sin(aRad);
            x = centerPt.x + radius * cos(aRad);
            var p = new google.maps.Point(x,y);
            circlePoints.push(oNormalProj.fromPixelToLatLng(p, zoom));
        }

        m_oCircleOverlay = new google.maps.Polyline(circlePoints, sLineColor, nLineWeight, 1);
        m_oMap.addOverlay(m_oCircleOverlay);
    }
}


function ShowCircle(oMousePoint)
{
    // Remove the previous circle, if any.
    ClearCircle();

    // Convert from the mouse coordinate point (x, y of mouse in div)
    // to a latitude/longitude coordinate.  Then, create a point
    // on the radius of the desired circle.
    var oLatLng = m_oMap.fromContainerPixelToLatLng(oMousePoint);
    var oRadiusPoint = new google.maps.LatLng(oLatLng.lat() + m_oBarnesSpatialRadius, oLatLng.lng());
    DrawCircle(oLatLng, oRadiusPoint, "#000000", 2);
}


function RemoveStationsFromMap()
{
    var pane = m_oMap.getPane(G_MAP_MARKER_SHADOW_PANE);
    var oStation;
    for (var i = m_oStations.length() - 1; i >= 0; i--)
    {
        oStation = m_oStations.get(i);
        if (oStation.latitude < m_oRetrieve_SW_Lat ||
            oStation.latitude > m_oRetrieve_NE_Lat ||
            oStation.longitude < m_oRetrieve_SW_Lng ||
            oStation.longitude > m_oRetrieve_NE_Lng)
        {
            m_oStations.remove(i);
            m_oMarkerMgr.removeMarker(oStation.marker);

            // Remove the station's label div tag.
            if (oStation.labelDiv != undefined)
            {
                pane.removeChild(oStation.labelDiv);
            }
        }
    }
}


function FillMapAreaList()
{
    var oSelect = document.getElementById("jumpList");

    // Remove all the elements in the listbox.
    ListboxRemoveAll(oSelect);

    oEntry = document.createElement("option");
    oEntry.appendChild(document.createTextNode("(Re-center map on ...)"));
    oEntry.value = "";
    oEntry.setAttribute("selected", "selected");
    oSelect.appendChild(oEntry);

    var oRow;
    var sLink;
    for (var i = 0; i < oMapAreas.length; i++)
    {
        oRow = oMapAreas[i];

        if (oRow.active == "1")
        {
            sLink = "?lat=" + oRow.lat +
                    "&lon=" + oRow.lon +
                    "&zoom=" + oRow.zoom;
            ListboxInsertItem(oSelect, oRow.name, sLink);
        }
    }
}


function Jump()
{
    var sUrl = document.getElementById("jumpList").value;
    if (sUrl != "")
    {
        var oParams = new Object();
        parseUrl(oParams, sUrl);

        m_oMap.setCenter(new google.maps.LatLng(oParams.lat, oParams.lon));
        m_oMap.setZoom(oParams.zoom);
    }
}

//********************************************
//SECTION FOR TOGGLING MARKERS AND SEGMENTS ON MAP
//JQUERY
;(function() {
    //function for hiding #dataOver associated with the station according to type
    var fnMarker = function( data, source, type, visibility ) {
        //data = m_oStationData
        //source is either 1 for WDE or 2 for VDT (for now)
        //type = oMarker.m_oStation.ca "M for mobile, T/P for nonmobile"
        //visibility is either 'true' or 'false'
        var oMarker, oLabelDiv, index = data.length;
        while(index-- > 0) {
            oMarker = data[index]; //cache the current marker
            oLabelDiv = oMarker.m_oLabel.m_oDataOver; //cache the corresponding #dataOver box
            if (type === "PT") {
                if ((oMarker.m_oStation.ca === "P" || oMarker.m_oStation.ca === "T") && oMarker.m_oSource === source) {
                    oMarker.setVisible(visibility);
                    oMarker.newValidity = visibility;
                    if (oLabelDiv !== undefined && oLabelDiv.innerHTML !== "" && visibility === true) {
                        oLabelDiv.style.display = '';
                        oLabelDiv.style.visibility = 'visible';
                    }
                    else if (oLabelDiv !== undefined && visibility === false) {
                        oLabelDiv.style.display = 'none';
                        oLabelDiv.style.visibility = 'hidden';
                    }
                }
            } else {
                if (oMarker.m_oSource === source && oMarker.m_oStation.ca === type) {
                    oMarker.setVisible(visibility);
                    oMarker.newValidity = visibility;
                    if (oLabelDiv !== undefined && oLabelDiv.innerHTML !== "" && visibility === true) {
                        oLabelDiv.style.display = '';
                        oLabelDiv.style.visibility = 'visible';
                    } else if (oLabelDiv !== undefined && visibility === false) {
                        oLabelDiv.style.display = 'none';
                        oLabelDiv.style.visibility = 'hidden';
                    }
                }
            }   
        }
    };
    //specific function for segments' dataOver
    var segmentDataOver = {
            show: function() {
                //get the length of the cached array dataSegments set when segments were 'hid'
                var dSegment = null, dLabelDiv = null;
                //show cached segments
                for(var x = 0, len = dataSegments.length; x < len; x++) {
                	dSegment = dataSegments[x];
                	dLabelDiv = dSegment.m_oLabel.m_oDataOver;
                	
                	dSegment.newValidity = true;
                	
                	if(dLabelDiv.innerHTML != "") {
	                    dLabelDiv.style.display = "";
	                    dLabelDiv.style.visibility = "visible";
                	}
                }
            },
            hide: function(data) {
                //data is 'm_oStationData'
                //index is used for the loop and contains the number of 'm_oStationData'
                var index = data.length;
                var dataOver = null;
                while(index-- > 0) {
                    if (data[index].m_oStation.ca === "S") {
                    	data[index].newValidity = false;
                        //cache the #dataOver associated with the segment
                        dataOver = data[index].m_oLabel.m_oDataOver;
                        if (dataOver != undefined) {
                            dataOver.style.display = "none";
                            dataOver.style.visibility = "hidden";
                        }
                        //cache segments into this array for performance
                        dataSegments.push(data[index]);
                    }
                }
            }
    };
    var toggleVisibility  = function(checkbox, stations) {
            if (checkbox.hasClass('clicked')) {
                checkbox.removeClass('clicked').attr('data-title', 'Click to Hide')
                        //.find('i').removeClass('icon-sign-blank').addClass('icon-check-sign');
                		.find('img.checkbox-icon').toggleClass('hidden');
                
                if (checkbox.data('type') === "S") {
                    segmentDataOver.show();
                } else {
                    fnMarker(m_oStationData, checkbox.data('source'), checkbox.data('type'), true);
                }
                if(stations != undefined) {
                    var x = 0, len = stations.length;
                    for (x; x < len; x++) {
                    	stations[x].setVisible(true);
                    	stations[x].newValidity = true;
                    }
                }
            } else {
                checkbox.addClass('clicked').attr('data-title', 'Click to Show')
	                //.find('i').removeClass('icon-sign-blank').addClass('icon-check-sign');
	        		.find('img.checkbox-icon').toggleClass('hidden');

                if (checkbox.data('type') === "S") {
                    segmentDataOver.hide(m_oStationData);
                } else {
                    fnMarker(m_oStationData, checkbox.data('source'), checkbox.data('type'), false);
                }
                if(stations != undefined) {
                    var x = 0, len = stations.length;
                    for (x; x < len; x++) {
                    	stations[x].setVisible(false);
                    	stations[x].newValidity = false;
                    }
                }
            }
    };
    
    $('#toggleRS').on('click', function() { //Road Segments
        toggleVisibility($(this), routePaths);
    });
    /*
    //button/checkbox event handler for VDT Mobile
    $("#toggleVDT").on("click", function() {
        toggleVisibility($(this), vdtMarkersWithRoutes);
    });
    */
    //button/checkbox event handler for WxDE Mobile
    $("#toggleWxDEMbl").on("click", function() {
        toggleVisibility($(this), mobileMarkersWithRoutes);
    });
    //button/checkbox event handler for WxDE nonmobile
    $("#toggleWxDE").on("click", function() {   
        toggleVisibility($(this));
    });

})(jQuery);

/*
** @author rothrob
** @date 09/24/2014
** @desc "This code hacks the Google map on this page
**              adding keyboard functions to it."
** Update: 10/29/2014
** Fixing firefox issue with key functions
*/
(function($, window, document) {
	$('#map_canvas').on('keyup', function(evnt) {
	    evnt.preventDefault();
	    var map = m_oMap;
	    var o = 128; // half a tile's width
	    switch(evnt.which) {
	        case 37: // leftArrow
	            map.panBy(-o,0);
	            break;
	        case 38: // upArrow
	            map.panBy(0,-o);
	            break;
	        case 39: // rightArrow
	            map.panBy(o,0);
	            break;
	        case 40: // downArrow
	            map.panBy(0,o);
	            break;
	        case 109: // numpad -
	        case 189: // _
	        case 173:
	            map.setZoom(map.getZoom()-1);
	            break;
	        case 107: // numpad +
	        case 187: // =
	        case 61:
	            map.setZoom(map.getZoom()+1);
	            break;
	    }
	});
})(jQuery, window, document, undefined);
