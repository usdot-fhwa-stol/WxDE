// global variables
var QCH_MAX = 12;
var m_oMap;
var m_oMarkerMgr = null;
var m_nMinZoomPinsVisible;
var m_nDefaultZoom;
var m_oStationXmlText;
var m_oMousePos = null;
var m_oStationCode = null;
var m_oClock = null;
var m_oObsTypes = null;

var m_oStations = new StationMgr();

// persistent marker icons
var m_oGrayIcon = CreateIcon("image/mm_12_gray.png");
//var m_oPurpleIcon = CreateIcon("image/mm_12_purple.png");
var m_oGreenIcon = CreateIcon("image/mm_12_green.png");
var m_oWhiteIcon = CreateIcon("image/mm_12_white.png");

var m_oQCh =
[
	[{im:"b",tx:""},{im:"nr",tx:"not run"}],
	[{im:"n",tx:"not passed"},{im:"p",tx:"passed"}]
];

var m_oCircleOverlay = null;

// The Barnes Spatial Radius is based on 69 miles per degree.
var m_oBarnesSpatialRadius = 1.0;

var m_bMapIsMoving = false;

var m_oTimeSendXml_1;
var m_oTimeReceiveXml_1;
var m_oTimeSendXml_2;
var m_oTimeReceiveXml_2;
//var m_oTimeDoneParsing;

var m_oViewport_NE_Lat;
var m_oViewport_NE_Lng;
var m_oViewPort_SW_Lat;
var m_oViewPort_SW_Lng;

var m_oRetrieve_NE_Lat;
var m_oRetrieve_NE_Lng;
var m_oRetrieve_SW_Lat;
var m_oRetrieve_SW_Lng;

var m_bShowASOS = false;

var m_oAllTests = new QChTests();

var m_sUrl;
var m_oStationData = new Array();
var m_oSelectedMarker;
var m_nMinStationId = 2147483647;
var m_bDivsCreated = false;
var m_sCurrentUnits = "m";

var m_dCircleX = [];
var m_dCircleY = [];

function onLoad()
{
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
    var oDivMap = document.getElementById("map");
    
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
    oUnitType[0].checked = true;
    
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
    m_oMap = new GMap2(document.getElementById("map"));
    m_oMap.addControl(new GLargeMapControl());

    GEvent.addListener(m_oMap, "mousemove", TrackMouseMovement);
    GEvent.addListener(m_oMap, "moveend", MapWasZoomed);
//    GEvent.addListener(m_oMap, "dragstart", MapDragStarted);
//    GEvent.addListener(m_oMap, "dragend", MapDragEnded);
//    GEvent.addListener(m_oMap, "moveend", MapDragEnded);
    
    // Only allow the Barnes Spatial radius circle to be shown when debugging ASOS stations.
    if (m_bShowASOS)
    {
        GEvent.addListener(m_oMap, "singlerightclick", ShowCircle);
    }
    
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


function loadDefaults(sUrl)
{
    var lng;
    var lat;
    
    // Extract the parameters from the specified URL.
    var sTemp = sUrl.split("?");

    if (sTemp.length > 1)
    {
        var sParameters = sTemp[1].split("&");

        for (var i = 0; i < sParameters.length; i++)
        {
            var sParam = sParameters[i].split("=");
            if (sParam[0] == "lat")
                lat = parseFloat(sParam[1]);
            else if (sParam[0] == "lon")
                lng = parseFloat(sParam[1]);
            else if (sParam[0] == "zoom")
                m_nDefaultZoom = parseInt(sParam[1]);
            else if (sParam[0] == "showasos")
                m_bShowASOS = true;
        }
    }

    
    m_nMinZoomPinsVisible = m_nDefaultZoom - 2;
    m_oMap.setCenter(new GLatLng(lat, lng), m_nDefaultZoom);
    
    ComputeBoundaries();
    
    // Make sure nothing is selected in the MapArea list.
    document.getElementById("jumpList").selectedIndex = 0;
}


// ComputeBoundaries()
// This function computes the two sets of boundaries that drive the map display.
// The Viewport is the region defined by what the user can see.
// The Retrieve is the region that is twice as big as the Viewport.
// This allows the user to move the map around a bit without triggering a data retrieval.
function ComputeBoundaries()
{
    // Base the Viewport on the visible extants of the map.
    m_oViewport_NE_Lat = m_oMap.getBounds().getNorthEast().lat();
    m_oViewport_NE_Lng = m_oMap.getBounds().getNorthEast().lng();
    m_oViewport_SW_Lat = m_oMap.getBounds().getSouthWest().lat();
    m_oViewport_SW_Lng = m_oMap.getBounds().getSouthWest().lng();
    
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
}


function TrackMouseMovement(oMouseLatLong)
{
    m_oMousePos.firstChild.data = "Lat, Lon: " +
                                  oMouseLatLong.lat().toFixed(6) + ", " + 
                                  oMouseLatLong.lng().toFixed(6);
}


function MapWasZoomed(nOldLevel, nNewLevel)
{
	if (m_bDivsCreated)
	{
		// reposition the obs div for each marker
		var oProjection = m_oMap.getCurrentMapType().getProjection();
		var pointMap = oProjection.fromLatLngToPixel(m_oMap.fromDivPixelToLatLng(new GPoint(0,0),true),m_oMap.getZoom());
		for (var nIndex = m_oStationData.length - 1; nIndex >= m_nMinStationId; nIndex--)
		{
			var oMarker = m_oStationData[nIndex]
			if (oMarker != undefined)
			{
				var offset = oProjection.fromLatLngToPixel(oMarker.getLatLng(), m_oMap.getZoom());
				var oLabelDiv = oMarker.m_oLabelDiv;
				oLabelDiv.style.left = (offset.x - pointMap.x) + "px"; //+4
				oLabelDiv.style.top = (offset.y - pointMap.y) + 6 + "px"; //-8
			}
		}
	}
}


function MapWasZoomedOld(nOldLevel, nNewLevel)
{
    // Reload the stations, if necessary.
    MapDragEnded();
    
    // If the map was zoomed, only show the value labels if
    // markers are being displayed.  The markers will only be displayed
    // if we are greater than the minimum zoom level.
    // The minimum zoom level at which pins are displayed is 2 less than
    // the default zoom level for the state.  The labels should disappear
    // one zoom level higher than the pins.
    // For example, if the default zoom level for a state is 7, then the
    // minimum zoom level at which the pins are visible would be 5.
    // The last level at which the labels would appear is 6.  This allows
    // the user to see all the pins for a state without seeing the clutter
    // of the labels.
    ClearLabels("markerLabel");
    if (nNewLevel > m_nMinZoomPinsVisible)
    {
        ShowLabels();
    }
}


function GetObsList()
{
    m_oTimeSendXml_1 = new Date();

    var oXmlRequest = new XmlRequest();
    // oXmlRequest.getXml("../obsv1/listObsTypes.jsp", cbGetObsList);
    oXmlRequest.getXml("ListObsTypes", cbGetObsList);
}


function DrawDebugRectangles()
{
/*****
    var boxV = new GPolyline([new GLatLng(m_oViewport_NE_Lat, m_oViewport_NE_Lng),
                              new GLatLng(m_oViewport_SW_Lat, m_oViewport_NE_Lng),
                              new GLatLng(m_oViewport_SW_Lat, m_oViewport_SW_Lng),
                              new GLatLng(m_oViewport_NE_Lat, m_oViewport_SW_Lng),
                              new GLatLng(m_oViewport_NE_Lat, m_oViewport_NE_Lng)],
                              "#000000", 3, 1);
    var boxR = new GPolyline([new GLatLng(m_oRetrieve_NE_Lat, m_oRetrieve_NE_Lng),
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
    var oBox = new GPolyline([new GLatLng(fLatSW, fLngSW),
                              new GLatLng(fLatNE, fLngSW),
                              new GLatLng(fLatNE, fLngNE),
                              new GLatLng(fLatSW, fLngNE),
                              new GLatLng(fLatSW, fLngSW)],
                              "#000000", 3, 1);
    m_oMap.addOverlay(oBox);
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


function UnitsChanged()
{
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

    // Tack the Station Code option to the end, since it isn't an ObsType.    
    oEntry = document.createElement("option");
    oEntry.appendChild(document.createTextNode("Station Code"));
    oEntry.value = "";
    oSelect.appendChild(oEntry);
    
    // Re-select the selected index.
    if (nCurrIndex != -1)
    {
        oSelect.selectedIndex = nCurrIndex;
    }
    
    if (bGetStations)
    {
        GetStations();
    }
    else
    {
			if (m_bDivsCreated)
			{
				// update the div innerHTML for each station, displayed div tag content will change
				for (var nIndex = m_oStationData.length - 1; nIndex >= m_nMinStationId; nIndex--)
				{
					var oMarker = m_oStationData[nIndex];
					if (oMarker != undefined)
					{
						var oLabelDiv = oMarker.m_oLabelDiv;
						var oObsValues = oMarker.m_oObsValues
						if (oObsValues != undefined)
						{
								var oValueArray = oMarker.m_oObsValues.mv;
								if (m_sCurrentUnits == "e")
									oValueArray = oMarker.m_oObsValues.ev;

								oLabelDiv.innerHTML = "";
								for (var nValueIndex = 0; nValueIndex < oValueArray.length; nValueIndex++)
								{
									if (nValueIndex > 0)
										oMarker.m_oLabelDiv.innerHTML += ", ";
										
									oMarker.m_oLabelDiv.innerHTML += oValueArray[nValueIndex];
								}
						}
					}
				}
			}
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
    
    oXmlRequest.getXml("GetStationsForRegion", cbGetStations);
}


function MapDragStarted()
{
}


function MapDragEnded()
{
    var oRetrieveBounds = new GLatLngBounds(new GLatLng(m_oRetrieve_SW_Lat, m_oRetrieve_SW_Lng),
                                            new GLatLng(m_oRetrieve_NE_Lat, m_oRetrieve_NE_Lng));
    var oViewportBounds = m_oMap.getBounds();
    
    // Don't get a new bunch of stations unless we are within the proper zoom level.
    if (m_oMap.getZoom() >= (m_nDefaultZoom - 1))
    {
        if (!oRetrieveBounds.containsBounds(oViewportBounds))
        {
            ComputeBoundaries();
            GetStations();
        }
    }
}


createIcon = function(sImage, nWidth, nHeight, nX, nY)
{
	var oIcon = new GIcon();
	oIcon.image = sImage;
	oIcon.iconSize = new GSize(nWidth, nHeight);
	oIcon.iconAnchor = new GPoint(nX, nY);

	return oIcon;
}


function CreateIcon(sImage)
{
    var oIcon = new GIcon();
    oIcon.image = sImage;
//    oIcon.shadow = "image/mm_20_shadow.png";
    oIcon.iconSize = new GSize(12, 20);
//    oIcon.shadowSize = new GSize(22, 20);
    oIcon.iconAnchor = new GPoint(6, 20);
    oIcon.infoWindowAnchor = new GPoint(5, 1);

    return oIcon;  
}

function CreateMarker(oStationXml)
{
    var oIcon = m_oGreenIcon;
  
    // If the current Station is an ASOS Station, then only show
    // it if we are showing ASOS Stations. Also, ASOS Stations have
    // a different icon.
    if (oStationXml.distgrp == 1)
    {
        if (!m_bShowASOS)
            return(null);

        // Show a different icon if the current station is an ASOS station.
        oIcon = m_oWhiteIcon;
    }
    else
    {
        // If this Station is not an ASOS Station, then make the
        // marker gray if it doesn't have any observations.
        if (!oStationXml.hasObs)
            oIcon = m_oGrayIcon;
    }

    // See if this station is in the list of stations being managed.
    // If it is not, then add it.
    // If it is, then update the label and the current label ObsType values for the station.
    var oMarker = null;
    var nIndex = m_oStations.add(oStationXml);
    if (nIndex < 0)
    {
        oMarker = new GMarker(new GLatLng(oStationXml.lat, oStationXml.lng), oIcon);
        oStationXml.marker = oMarker;
        
        oMarker.stationData = oStationXml;
        oMarker.obsXmlSaved = false;

        GEvent.addListener(oMarker, "click",
            function(oMarker)
            {
              ShowInfoWindow(this);
            });

        GEvent.addListener(oMarker, "mouseover",
            function(oMarker)
            {
              ShowStationCode(this);
            });

        GEvent.addListener(oMarker, "mouseout",
            function()
            {
              m_oStationCode.firstChild.data = "Station Code: ";
            });
    }
    else
    {
        var sObsValue = oStationXml.obsValue;
        var sEnglishValue = oStationXml.englishValue;
        if (sObsValue != undefined)
        {
            m_oStations.setObsValues(nIndex, sObsValue, sEnglishValue);
        }
    }

    return oMarker;
}


// ShowStationCode()
// This function displays the Station Code in a text field
// whenever the mouse runs over it.
function ShowStationCode(oMarker)
{
    m_oStationCode.firstChild.data = "Station Code: " + oMarker.m_oStation.st;
}


function ShowInfoWindow(oMarker)
{
    m_oSelectedMarker = oMarker;
    var oXmlRequest = new XmlRequest();
    oXmlRequest.addParameter("stationId", oMarker.m_oStation.id);
    oXmlRequest.addParameter("lat", oMarker.m_oStation.lt);
    oXmlRequest.addParameter("lon", oMarker.m_oStation.ln);
    oXmlRequest.getXml("GetStationsForRegion", cbGetObsForStation);
}


function cbGetObsForStation(oXml, sText)
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

    var sObsTableHeader = 
        "<div style=\"width: 665px;\">\n" +
        "<br/>\n" +
        "<table class=\"qualityChecks\">\n" + 
        "  <tr align=\"center\">\n" + 
        "    <td colspan=\"6\">" + oStation.st + "<br/>" + oStationObs.nm + "<br/>" + "Lat, Lon: " + oStation.lt + ", " + oStation.ln + "<br/>Elevation: " + oStationObs.el + " m</td>\n" +
        "    <td rowspan=\"2\"><img src=\"" + imgComplete.src + "\" alt=\"Complete\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgManual.src + "\" alt=\"Manual\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgSensorRange.src + "\" alt=\"Sensor Range\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgClimateRange.src + "\" alt=\"Climate Range\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgStep.src + "\" alt=\"Step\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgLikeInstrument.src + "\" alt=\"Like Instrument\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgPersistence.src + "\" alt=\"Persistence\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgIQR.src + "\" alt=\"Inter-quartile Range\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgBarnesSpatial.src + "\" alt=\"Barnes Spatial\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgDewpoint.src + "\" alt=\"Dewpoint\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgSealevelPressure.src + "\" alt=\"Sea Level Pressure\"/></td>\n" + 
        "    <td rowspan=\"2\"><img src=\"" + imgPrecipAccum.src + "\" alt=\"Accumulated Precipitation\"/></td>\n" + 
        "  </tr>\n" + 
        "  <tr align=\"center\">\n" + 
        "    <td><b>Timestamp (UTC)</b></td>\n" + 
        "    <td><b>Observation Type</b></td>\n" +
        "    <td><b>Ind</b></td>\n" +
        "    <td><b>Value</b></td>\n" +
        "    <td><b>Unit</b></td>\n" +
        "    <td><b>Conf</b></td>\n" +
        "  </tr>\n";
        
    var sObsTableFooter = "</table>\n</div>";
    var sObsTableRows = "";

    if (oObs.length > 0)
    {
		// sort obs array by obs type name and then sensor index
		var oCompare =
		{
			compare : function(oLhs, oRhs)
			{
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

				return 1;
			}
		};

		js.util.Collections.usort(oObs, oCompare);

        // Now, build the table of Observations.
        for (var i = 0; i < oObs.length; i++)
        {
			var iObs = oObs[i];

			sObsTableRows +=
				"  <tr>\n" +
				"    <td>" + iObs.ts + "</td>\n" +
				"    <td class=\"obsType\">" + iObs.ot + "</td>\n" + 
				"    <td class=\"value\">" + iObs.si + "</td>\n";

			// add the observation value
			sObsTableRows += "    <td class=\"value\">";

			if (bShowEnglishUnits)
				sObsTableRows += iObs.ev;
			else
				sObsTableRows += iObs.mv;

			sObsTableRows += "</td>\n";

			// add the appropriate units left-justified
			sObsTableRows += "    <td class=\"obsType\">";
			// only disply units when they are not null
			if (iObs.mu != "null" && iObs.eu != "null")
			{
				if (bShowEnglishUnits)
					sObsTableRows += iObs.eu;
				else
					sObsTableRows += iObs.mu;
			}
			sObsTableRows += "</td>\n";

			// show the confidence value right-justified
			sObsTableRows += "    <td class=\"value\">" + iObs.cv + "%</td>\n";

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

				sObsTableRows += "    <td><img src=\"image/";
				sObsTableRows += oFlag.im;
				sObsTableRows += ".png\" alt=\"";
				sObsTableRows += oFlag.tx;
				sObsTableRows += "\"/></td>\n";
			}

            sObsTableRows += "  </tr>\n";
        }
    }
    else
    {
        // No Observations for this Station.
        sObsTableRows += 
            "  <tr>\n" + 
            "    <td colspan=\"5\">No current observations available</td>\n" +
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
    
//    oMarker.openInfoWindowHtml(sObsTableHeader + sObsTableRows + sObsTableFooter);
    m_oMap.openInfoWindowHtml(new GLatLng(oStation.lt, oStation.ln), 
		sObsTableHeader + sObsTableRows + sObsTableFooter);
}


function createInfoMarker()
{
    var oIcon = new GIcon();
    oIcon.image = "image/infomarker.png";
    oIcon.iconSize = new GSize(11, 11);
    oIcon.iconAnchor = new GPoint(6, 20);
    oIcon.infoWindowAnchor = new GPoint(5, 1);

    var form = document.forms[0];
    var lng = parseFloat(form.elements["infoMarkerLng"].value);
    var lat = parseFloat(form.elements["infoMarkerLat"].value);
    
    var oMarker = new GMarker(new GLatLng(lat, lng), oIcon);

    GEvent.addListener(oMarker, "click", function()
    {
        window.stationXmlText = m_oStationXmlText;
        window.open("StationData.html", "new_win", "width=625, height=650, scrollbars=1, resizable=1, top=10, left=20");
    });
    
    return oMarker;
}


function cbGetStations(oXml, oText)
{
//    m_oTimeReceiveXml_2 = new Date();

    m_oStationXmlText = oText;
//    var oStationXmlData = oXml.documentElement.getElementsByTagName("station");
    var oStationXmlData = eval(oText);

    // Allocate a new Google Marker Manager the first time through.
//    if (m_oMarkerMgr == null)
//        m_oMarkerMgr = new MarkerManager(m_oMap);
    
    // See if the debugMHI flag is set.
    // If it is, then show the ASOS stations.
/***
    if (oXml.documentElement.getAttribute("debugMHI") == 1)
    {
        m_bShowASOS = true;
    }
***/
    
    // Run through the list of stations on the map and remove
    // any that are no longer in the Retrieval bounds.
//    RemoveStationsFromMap();
    
    // Add a marker that shows the URL used.
    // m_oMarkerMgr.addMarker(createInfoMarker(), 0);
    
			var oOptionsOff = 
			{
				"icon": createIcon("image/mm_12_gray.png", 12, 12, 5, 5),
				"clickable": true,
				"draggable": false, 
				"title": ""
			};

			var oOptionsOn =
			{
				"icon": createIcon("image/mm_12_purple.png", 12, 12, 5, 5),
				"clickable": true,
				"draggable": false,
				"title": ""
			};

			var oOptionsMobile =
			{
				"icon": createIcon("image/mm_12_blue.png", 12, 12, 5, 5),
				"clickable": true,
				"draggable": false,
				"title": ""
			};

			var oNwsOff = 
			{
				"icon": createIcon("image/mm_12_graywhite.png", 12, 12, 5, 5),
				"clickable": true,
				"draggable": false, 
				"title": ""
			};

			var oNwsOn = 
			{
				"icon": createIcon("image/mm_12_white.png", 12, 12, 5, 5),
				"clickable": true,
				"draggable": false, 
				"title": ""
			};
			
			// remove data for ASOS stations if the are not going to be displayed
			var nIndex = oStationXmlData.length;
			if (!m_bShowASOS)
			{
				var oTempArray = new Array();
				
				while (nIndex-- > 0)
				{
					var oStation = oStationXmlData[nIndex];
					
					// only save the stations outside the ASOS ranges
//					if ((oStation.id < 755 || oStation.id > 1627) && (oStation.id < 3293 || oStation.id > 5691))
					if (oStation.cn != 4)
						oTempArray.push(oStation);
				}
				
				oStationXmlData = oTempArray;
			}

			var oMarker;
			nIndex = oStationXmlData.length;
			while (nIndex-- > 0)
			{
				var oStation = oStationXmlData[nIndex];
				var oPoint = new GLatLng(oStation.lt, oStation.ln);

				if (oStation.ho == 0)
				{
//					if ((oStation.id > 754 && oStation.id < 1628) || (oStation.id > 3292 && oStation.id < 5692))
          if (oStation.cn == 4)
						oMarker = new GMarker(oPoint, oNwsOff);
					else
						oMarker = new GMarker(oPoint, oOptionsOff);
				}
				else
				{
//					if ((oStation.id > 754 && oStation.id < 1628) || (oStation.id > 3292 && oStation.id < 5692))
          if (oStation.cn == 4)
						oMarker = new GMarker(oPoint, oNwsOn);
					else
					{
						if (oStation.ca == "M")
							oMarker = new GMarker(oPoint, oOptionsMobile);
						else
							oMarker = new GMarker(oPoint, oOptionsOn);
					}
				}
					
				// track the minimum station id
				if (oStation.id < m_nMinStationId)
					m_nMinStationId = oStation.id;
					
				// attach mouse events to each marker
        GEvent.addListener(oMarker, "click",
            function(oMarker)
            {
              ShowInfoWindow(this);
            });

        GEvent.addListener(oMarker, "mouseover",
            function(oMarker)
            {
              ShowStationCode(this);
            });

        GEvent.addListener(oMarker, "mouseout",
            function()
            {
              m_oStationCode.firstChild.data = "Station Code: ";
            });

				// attach the parsed station data to the map marker
				oMarker.m_oStation = oStation;
				m_oStationData[oStation.id] = oMarker;
				
//				if (nIndex == 0)
					m_oMap.addOverlay(oMarker);
//				else
//				{
//					oMarker.initialize(m_oMap);
//					oMarker.redraw(true);
//					m_oMap.pb.push(oMarker);
//				}				
			}

//    for (var i = 0; i < oStationXmlData.length; i++)
//    {
//        oMarker = CreateMarker(oStationXmlData[i]);
//        if (oMarker != null)
//        {
            // Now, add the marker to the Google Marker Manager.
//            m_oMarkerMgr.addMarker(oMarker, m_nMinZoomPinsVisible);
//        }
//    }

//    m_oMarkerMgr.refresh();

    // Re-enable the screen objects.
    DisableObjects(false);

    // Show any value labels that were selected.
//    ShowLabels();
    
    ClearLabels("progressLabel");
		GEvent.addListener(m_oMap, "zoomend", MapWasZoomed);
    
//    m_oTimeDoneParsing = new Date();
    
    // DrawDebugRectangles();
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


function ClearLabels(sLabelType)
{
    var pane = m_oMap.getPane(G_MAP_MARKER_SHADOW_PANE);
    
    try
    {
        var oElement;
        for (var i = pane.childNodes.length - 1; i >= 0; i--)
        {
            oElement = pane.childNodes[i];

            // NOTE: elementType is a dummy variable to denote this element
            //       as something we added to the DOM.            
            if (oElement.elementType == sLabelType)
            {
                // Remove the label element from its station record.
                var oStation = oElement.oStation;
                if (oStation != undefined || oStation != null)
                {
                    oStation.labelDiv = null;
                }
                pane.removeChild(oElement);
            }
        }
    }
    catch (oErr)
    {
    }
}


function GetObsValue()
{
    ClearLabels("markerLabel");

    var oSelect = document.getElementById("obsTypeList");
    var sObsType = oSelect.childNodes.item(oSelect.selectedIndex).firstChild.data;
    
    // Early bailout
    if (sObsType == "(Select Data to Show)")
        return;

    ShowProgressLabel();
    
    var nIndex = 0;
    var oMarker = undefined;

		// create a div tag for the marker to hold obs values when needed
		for (nIndex = m_oStationData.length - 1; nIndex >= m_nMinStationId; nIndex--)
		{
			oMarker = m_oStationData[nIndex];
			if (oMarker != undefined)
			{
				var oLabelDiv = oMarker.m_oLabelDiv;
				if (oLabelDiv == undefined)
				{
					oLabelDiv = document.createElement("div");
					oLabelDiv.id = "dataOver";
					oLabelDiv.style.position = "absolute";
					oLabelDiv.elementType = "markerLabel";
					oMarker.m_oLabelDiv = oLabelDiv;
				}
				
				// clear any existing obs text and data
				oLabelDiv.innerHTML = "";
				oMarker.m_oObsValues = undefined;
			}
		}

		m_bDivsCreated = true;

    // Since we already have the Station Code, there is no need to make an XML request.
    if (sObsType == "Station Code")
    {
        // Run through the list of Stations and set the label for each station.
				for (nIndex = m_oStationData.length - 1; nIndex >= m_nMinStationId; nIndex--)
				{
					oMarker = m_oStationData[nIndex];
					if (oMarker != undefined)
						oMarker.m_oLabelDiv.innerHTML = oMarker.m_oStation.st;
				}
				
			  ClearLabels("progressLabel");
				ShowLabels();
    }
    else
    {
    	// request the obs values
			var oXmlRequest = new XmlRequest();
	    oXmlRequest.addParameter("obsType", oSelect.value);
			oXmlRequest.getXml("GetStationsForRegion", cbGetObsValue);
    }
}


function cbGetObsValue(oXml, sText)
{
  ClearLabels("progressLabel");
  
	var oValueArray = undefined;
	var oObsValues = eval(sText);
	for (var nIndex = 0; nIndex < oObsValues.length; nIndex++)
	{
		var oValues = oObsValues[nIndex];
		// if metric values exist, then english units exist as well
		if (oValues.mv.length > 0)
		{
			var oMarker = m_oStationData[oValues.id];
			if (oMarker != undefined)
			{
				oMarker.m_oObsValues = oValues;

				oValueArray = oMarker.m_oObsValues.mv;
				if (m_sCurrentUnits == "e")
					oValueArray = oMarker.m_oObsValues.ev;

				for (var nValueIndex = 0; nValueIndex < oValueArray.length; nValueIndex++)
				{
					if (nValueIndex > 0)
						oMarker.m_oLabelDiv.innerHTML += ", ";
						
					oMarker.m_oLabelDiv.innerHTML += oValueArray[nValueIndex];
				}
			}
		}
		else
			oMarker.m_oObsValues = undefined;
	}

	ShowLabels();
}


function ShowLabels()
{
	if (m_bDivsCreated)
	{
		var oProjection = m_oMap.getCurrentMapType().getProjection();
		var pointMap = oProjection.fromLatLngToPixel(m_oMap.fromDivPixelToLatLng(new GPoint(0,0),true),m_oMap.getZoom());
		var oPane = m_oMap.getPane(G_MAP_MARKER_SHADOW_PANE);

		// Run through the list of Stations and display the label for each station.
		for (var nIndex = m_oStationData.length - 1; nIndex >= m_nMinStationId; nIndex--)
		{
			var oMarker = m_oStationData[nIndex];
			if (oMarker != undefined)
			{
				var oLabelDiv = oMarker.m_oLabelDiv;
				if (oLabelDiv.innerHTML.length > 0)
				{
					var offset = oProjection.fromLatLngToPixel(oMarker.getLatLng(), m_oMap.getZoom());
					oLabelDiv.style.left = (offset.x - pointMap.x) + "px"; //+4
					oLabelDiv.style.top = (offset.y - pointMap.y) + 6 + "px"; //-8
					oPane.appendChild(oLabelDiv);
        }
      }
		}
	}
}


function ShowLabelsOld()
{
    // Change all the labels to reflect the Units type being displayed.
    var sUnitAttribute = "units";
    var oUnitType = document.getElementsByName("rbUnits");
    if (oUnitType[1].checked)
    {
        sUnitAttribute = "englishUnits";
    }
    m_oStations.changeLabels(sUnitAttribute);
        
    // Get the marker pane.
    var pane = m_oMap.getPane(G_MAP_MARKER_SHADOW_PANE);

    try
    {
        var oSelect = document.getElementById("obsTypeList");
        var sObsType = oSelect.childNodes.item(oSelect.selectedIndex).firstChild.data;
    }
    catch(e)
    {
        return;
    }
    
    // Bail out if labels aren't being displayed.
    if (oSelect.value == "" && sObsType != "Station Code")
    {
        return;
    }
    
    // Walk the list of stations.
    var oLabelDiv;
    var lat;
    var lng;
    for (var nStation = 0; nStation < m_oStations.length(); nStation++)
    {
        var sCompleteLabel = "";
        
        // Don't show labels for ASOS stations unless ASOS stations are being displayed.
        if (!m_bShowASOS && m_oStations.distGroup(nStation) == 1)
        {
            continue;
        }
        
        // If the observation type is the Station Code, then force the label
        // to be set properly.
        if (sObsType == "Station Code")
        {
            m_oStations.setLabel(nStation, m_oStations.stationCode(nStation));
        }

        lat = m_oStations.latitude(nStation);
        lng = m_oStations.longitude(nStation);
        var pointMap = m_oMap.getCurrentMapType().getProjection().fromLatLngToPixel(m_oMap.fromDivPixelToLatLng(new GPoint(0,0),true),m_oMap.getZoom());
        var offset = m_oMap.getCurrentMapType().getProjection().fromLatLngToPixel(new GLatLng(lat,lng),m_oMap.getZoom());
        sCompleteLabel = m_oStations.label(nStation);
        
        // If the station already has a labelDiv placeholder, then use it.
        // Otherwise, create it.
        oLabelDiv = m_oStations.labelDiv(nStation);
        if (oLabelDiv == undefined || oLabelDiv == null)
        {
            this.dataOver = document.createElement("div");
            m_oStations.setLabelDiv(nStation, dataOver);
            
            oLabelDiv = dataOver;
            
            dataOver.style.position = "absolute";
            dataOver.style.left = (offset.x - pointMap.x) + "px"; //+4
            dataOver.style.top = (offset.y - pointMap.y) + "px"; //-8
            
            // NOTE: elementType is a dummy variable to denote this element
            //       is something we are adding to the DOM.            
            dataOver.elementType = "markerLabel";

            pane.appendChild(dataOver);
            
            // Attach the station record to the element so it can be removed
            // from the station list when the element is removed from the page.
            dataOver.oStation = m_oStations.get(nStation);
        }

        // If there is something to display, then set the text in the label's div tag.
        // Otherwise, clear the text in the label's div tag.
        if (sCompleteLabel != "")
        {
            oLabelDiv.innerHTML = "<div name=\"dataOver\" id=\"dataOver\" class=\"over\">" +
                                  "<div class=\"obs1\">" + sCompleteLabel + "</div>" + "</div>";
        }
        else
        {
            oLabelDiv.innerHTML = "";
        }
    }
}


function StartMapClock()
{
    m_oClock.firstChild.data = FormatCurrentTime();
    
    // Update the clock every 60 seconds, at the top of the minute.
    setTimeout(StartMapClock, (1000 * (60 - new Date().getSeconds())));
}


function ShowProgressLabel()
{
    var lat = m_oMap.getCenter().lat();
    var lng = m_oMap.getCenter().lng() - 1.5;
    
    var pane = m_oMap.getPane(G_MAP_MARKER_SHADOW_PANE);
    this.dataOver = document.createElement("div");
    var pointMap = m_oMap.getCurrentMapType().getProjection().fromLatLngToPixel(m_oMap.fromDivPixelToLatLng(new GPoint(0,0),true),m_oMap.getZoom());
    var offset = m_oMap.getCurrentMapType().getProjection().fromLatLngToPixel(new GLatLng(lat,lng),m_oMap.getZoom());

    dataOver.innerHTML = "<div name=\"dataOver\" id=\"dataOver\" class=\"over\">" +
                         "<div class=\"progressLabel\"> Retrieving station data ... </div>";
  
    dataOver.style.position = "absolute";
    dataOver.style.left = (offset.x - pointMap.x) + "px"; //+4
    dataOver.style.top = (offset.y - pointMap.y) + "px"; //-8
    
    // NOTE: elementType is a dummy variable to denote this element
    //       is something we are adding to the DOM.            
    dataOver.elementType = "progressLabel";

    pane.appendChild(dataOver);
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
		oPoints.push(new GLatLng(dLatOffset, oCenterPoint.lng() + m_dCircleX[nIndex] / Math.cos(dLatOffset * Math.PI / 180)));
	}
	
	m_oCircleOverlay = new GPolyline(oPoints, sLineColor, nLineWeight, 1);
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
            y = centerPt.y + radius * sin(aRad)
            x = centerPt.x + radius * cos(aRad)
            var p = new GPoint(x,y);
            circlePoints.push(oNormalProj.fromPixelToLatLng(p, zoom));
        }
        
        m_oCircleOverlay = new GPolyline(circlePoints, sLineColor, nLineWeight, 1);
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
    var oRadiusPoint = new GLatLng(oLatLng.lat() + m_oBarnesSpatialRadius, oLatLng.lng());
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


function GetMarker(sStationCode)
{
    var nIndex = m_oStations.codeSearch(sStationCode);
    if (nIndex >= 0)
    {
        return(m_oStations.get(nIndex).marker);
    }
    else
    {
        Debug(sStationCode + " not found in m_oStations:" +
              "\nnIndex = " + nIndex + "\n" + m_oStations.toString());
        return undefined;
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
    var oSelect = document.getElementById("jumpList");

    if (oSelect.value != "")
    {
        loadDefaults(oSelect.value);
//        GetStations();
    }
}


window.onresize = function()
{
    document.getElementById("map").style.height = (GetWinDimension("height") - 66) + "px";
}
