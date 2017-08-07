<%@page import="org.owasp.encoder.Encode"%>
<!DOCTYPE html>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="X-UA-Compatible" content="IE=Edge">

    <title>Weather Data Environment - Collector Map Interface</title>

    <!-- Bootstrap3 UI framework -->
    <link href="/style/vendor/bootstrap.min.css" rel="stylesheet"/>

    <style type="text/css">
        v\: * {
            behavior: url(#default#VML);
        }
    </style>

    <link href="/style/WDEMap.css" rel="stylesheet" type="text/css" media="screen"/>
    <link href="/style/WDEMap2.css" rel="stylesheet" type="text/css" media="screen"/>
    <link rel="stylesheet" href="/style/wdemap-sidebar.css"/>
    <!-- top menu CSS style -->
    <link href="/style/top-mini-nav.css" rel="stylesheet"/>
    <link href="/style/wxde-mini-style.css" rel="stylesheet"/>
    <link rel="stylesheet" href="/style/main-accessibility-styles.css"/>
    <!--
   <link href="style/jquery/lightness/jquery-ui-full-1.11.4.css" rel="stylesheet">
    -->
      <link href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css"/>
  
    
    
    <link href="style/jquery/jquery.datetimepicker.css" rel="stylesheet">
	<link rel="stylesheet" href="style/leaflet.css" />
  
  
	<script src="script/us-states.js"></script>
	<script src="script/leaflet.js"></script>
<script src="script/leaflet-wxde.js"></script> 

    <!-- Pre-load the images. -->
    <script type="text/javascript">
        if (document.images) {
            imgComplete = new Image(15, 106);
            imgComplete.src = "image/qch/Complete.png";
            imgComplete.alt = "Complete";

            imgManual = new Image(15, 106);
            imgManual.src = "image/qch/Manual.png";
            imgManual.alt = "Manual";

            imgSensorRange = new Image(15, 106);
            imgSensorRange.src = "image/qch/SensorRange.png";
            imgManual.alt = "Sensor Range";

            imgClimateRange = new Image(16, 106);
            imgClimateRange.src = "image/qch/ClimateRange.png";
            imgClimateRange.alt = "Climate Range";

            imgStep = new Image(16, 106);
            imgStep.src = "image/qch/Step.png";
            imgStep.alt = "Step";

            imgLikeInstrument = new Image(16, 106);
            imgLikeInstrument.src = "image/qch/LikeInstrument.png";
            imgLikeInstrument.alt = "Like Instrument";

            imgPersistence = new Image(15, 106);
            imgPersistence.src = "image/qch/Persistence.png";
            imgPersistence.alt = "Persistance";

            imgBarnesSpatial = new Image(15, 106);
            imgBarnesSpatial.src = "image/qch/BarnesSpatial.png";
            imgBarnesSpatial.alt = "Barnes Spatial";

            imgDewpoint = new Image(16, 106);
            imgDewpoint.src = "image/qch/Dewpoint.png";
            imgDewpoint.alt = "Dewpoint";

            imgSealevelPressure = new Image(16, 106);
            imgSealevelPressure.src = "image/qch/SeaLevelPressure.png";
            imgSealevelPressure.alt = "Sea Level Pressure";

            imgIQR = new Image(16, 106);
            imgIQR.src = "image/qch/IQR.png";
            imgIQR.alt = "IQR";

            imgPrecipAccum = new Image(16, 106);
            imgPrecipAccum.src = "image/qch/PrecipAccum.png";
            imgPrecipAccum.alt = "Precipitation Accumulation";

            imgCombinedAlgorithm = new Image(16, 106);
            imgCombinedAlgorithm.src = "image/qch/CombinedAlgorithm.png";
            imgCombinedAlgorithm.alt = "Combined Algorithm";

            imgModelAnalysis = new Image(16, 106);
            imgModelAnalysis.src = "image/qch/ModelAnalysis.png";
            imgModelAnalysis.alt = "Model Analysis";

            imgNearestSurfaceStation = new Image(16, 106);
            imgNearestSurfaceStation.src = "image/qch/NearestSurfaceStation.png";
            imgNearestSurfaceStation.alt = "Nearest Surface Station";

            imgNeighboringVehicle = new Image(16, 106);
            imgNeighboringVehicle.src = "image/qch/NeighboringVehicle.png";
            imgNeighboringVehicle.alt = "Neighboring Vehicle";

            imgStandardDeviation = new Image(16, 106);
            imgStandardDeviation.src = "image/qch/StandardDeviation.png";
            imgStandardDeviation.alt = "Standard Deviation";

            imgVehicleStdDev = new Image(16, 106);
            imgVehicleStdDev.src = "image/qch/VehicleStdDev.png";
            imgVehicleStdDev.alt = "Vehicle Standard Deviation";

            imgSpatialBarnes = new Image(16, 106);
            imgSpatialBarnes.src = "image/qch/SpatialBarnes.png";
            imgSpatialBarnes.alt = "Spatial Barnes";

            imgSpatialIOR = new Image(16, 106);
            imgSpatialIOR.src = "image/qch/SpatialIQR.png";
            imgSpatialIOR.alt = "Spatial IQR";

            imgTimeStep = new Image(16, 106);
            imgTimeStep.src = "image/qch/TimeStep.png";
            imgTimeStep.alt = "Time Step";

            imgOverallDewTemperature = new Image(16, 106);
            imgOverallDewTemperature.src = "image/qch/OverallDewTemperature.png";
            imgOverallDewTemperature.alt = "Overall Dew Temperature";

            imgFiltering = new Image(16, 106);
            imgFiltering.src = "image/qch/Filtering.png";
            imgFiltering.alt = "Filtering";

            picTestPass = new Image(16, 16);
            picTestPass.src = "image/p.png";
            picTestPass.alt = "Pass";

            imgTestFail = new Image(16, 16);
            imgTestFail.src = "image/n.png";
            imgTestFail.alt = "Fail";

            imgTestNotRun = new Image(16, 16);
            imgTestNotRun.src = "image/nr.png";
            imgTestNotRun.alt = "Not Run";

            imgTestBlank = new Image(16, 16);
            imgTestBlank.src = "image/b.png";
            imgTestBlank.alt = "Blank";
        }
    </script>
     
    
<!--
  <script type="text/javascript" src="//code.jquery.com/jquery-2.2.0.min.js"></script>
-->
 
    
   
    
       <script src="script/jquery/jquery-2.2.0.js"></script>
    

    <!--[if IE 8]>
    <link href="/style/IE-styles-mini.css" type="text/css" rel="stylesheet">
    <link href="/style/obs-table-ie82.css" type="text/css" rel="stylesheet">
    <![endif]-->

</head>

<body  class="wde-map-page">
<!-- Include the top part of the page container the Header and Navigation -->
<jsp:include page="/inc/mini-system-ui/miniHeader.jsp"></jsp:include>

<div id="tableModifier">
    <img src="/image/minified-map-ui/fhwa-logo-mini.png" alt="FHWA Logo"/><br><br>
</div>
<!-- 	/////// -->
<!-- 	SIDEBAR -->
<!-- 	END OF SIDEBAR -->
<!-- 	/////// -->
<!-- 	MAP CONTAINER -->
<div id="map-container">
   
  <div id="summary-legend-form" style="display:none;" title="Map Summary View">
    <table>
      <tr>
        <td colspan ="2" class="summary-legend-text">Click on a state to zoom into that region.<br/><br/>
		Click on the Layers button to toggle observation and sensor views.<br/><br/>
		Click on the time control to select historic or forecast observations.
      </tr>
      <tr>
        <td colspan="2" class="summary-legend-text">States without shading have not provided metadata or signed a data sharing agreement.</td>
      </tr>
      <tr>
        <td ><div class="summary-legend-icon" style="background-color: #FF8822"></div></td>
        <td class="summary-legend-text">data sharing agreement in place + sensor metadata + data available</td>
      </tr>
      <tr>
        <td ><div class="summary-legend-icon" style="background-color: #7722FF"></div></td>
        <td class="summary-legend-text">data sharing agreement in place + sensor metadata + data temporarily unavailable</td>
      </tr>
      <tr>
        <td ><div class="summary-legend-icon" style="background-color: #555555"></div></td>
        <td class="summary-legend-text">no data sharing agreement + sensor metadata only</td>
      </tr>
    </table>
    Click the <img class="DialogButton" src="image/icons/doc.png" /> button on the left side of the control bar at the bottom of the page to restore these instructions.      
  </div>
  
  
  <div id="road-legend-form" style="display:none;" title="Road Segment Controls">
    <table>
      <tr>
        <td colspan ="2" class="summary-legend-text">Click on a road segment to display a dialog with the observation details most recent to the selected time for that road segment. The time control can also be used to select historic or forecast observations for road segments.</td>
      </tr>
      <tr>
        <td colspan="2" class="summary-legend-text">States without shading have not provided metadata or signed a data sharing agreement.</td>
      </tr>
      <tr>
        <td ><div class="summary-legend-icon" style="background-color: #F494FE"></div></td>
        <td class="summary-legend-text">road segment has no advisories or warnings in effect</td>
      </tr>
      <tr>
        <td ><div class="summary-legend-icon" style="background-color: #DA03F1"></div></td>
        <td class="summary-legend-text">road segment has potential for future hazardous conditions* (advisory)</td>
      </tr>
      <tr>
        <td ><div class="summary-legend-icon" style="background-color: #71017D"></div></td>
        <td class="summary-legend-text">road segment currently displays indicators of hazardous conditions* (warning)</td>
      </tr>
        <td colspan ="2" class="summary-legend-text">* Hazardous conditions could be ice on roads, low visibility, heavy rains or hail.</td>
    </table>
    Click the <img class="DialogButton" src="image/icons/doc.png" /> button on the left side of the control bar at the bottom of the page to restore these instructions.
  </div>
  
  
  <div id="details-form" style="display:none;" title="Map Controls">
    <p>
    Layers can be turned on or off using the Layers button. Most layers are enabled by default, but are gray when no data are available 
    or the map is not zoomed in enough to display the layer detail. The Segment Obs layer contains a lot of data and you must 
    zoom in four more times to view them.<br/><br/>
    Click the time control on the bottom right to select historic or forecast observations. It defaults to the current time. Click on the home button on the top left of the time control window to return to current date, or double click on the home button to set map to current date and time.<br/><br/>
    Selecting a specific observation type from the list next to the layer button displays the values for the selected observation type across all ESS and mobile stations. 
    Switch between Metric or English units with the radio buttons.<br/><br/>
    Click on an ESS or mobile station to display a dialog with the observation details most recent to the selected time and station. 
    Clicking on a sensor icon will display a dialog containing sensor metadata for the selected station.
    </p>
    <p>
    Click the <img class="DialogButton" src="image/icons/doc.png" /> button on the left side of the control bar at the bottom of the page to restore these instructions.
    </p>
  </div>
   
<div id="dialog-form" style="display:none;">

  <table id="obs-data" class="qualityChecks" >
	<thead class="obs-table-head">  
		<tr align="center">    
			<td class="td-title" colspan="6"><div id="platform-details"> </div>
			</td>   
			<td rowspan="2" class="td-image no-border-left webkit-td-image-fix"><img alt="Complete" src="image/qch/Complete.png"></td>   
			<td rowspan="2" class="td-image"><img alt="Manual" src="image/qch/Manual.png"></td>  
			<td rowspan="2" class="td-image"><img alt="Sensor Range" src="image/qch/SensorRange.png"></td>   
			<td rowspan="2" class="td-image"><img alt="Climate Range" src="image/qch/ClimateRange.png"></td>   
			<td rowspan="2" class="td-image"><img alt="Step" src="image/qch/Step.png"></td>   
			<td rowspan="2" class="td-image"><img alt="Like Instrument" src="image/qch/LikeInstrument.png"></td>  
			<td rowspan="2" class="td-image"><img alt="Persistence" src="image/qch/Persistence.png"></td>  
			<td rowspan="2" class="td-image"><img alt="Inter-quartile Range" src="image/qch/IQR.png"></td>    
			<td rowspan="2" class="td-image"><img alt="Barnes Spatial" src="image/qch/BarnesSpatial.png"></td>    
			<td rowspan="2" class="td-image"><img alt="Dewpoint" src="image/qch/Dewpoint.png"></td>    
			<td rowspan="2" class="td-image"><img alt="Sea Level Pressure" src="image/qch/SeaLevelPressure.png"></td>   
			<td rowspan="2" class="td-image"><img alt="Accumulated Precipitation" src="image/qch/PrecipAccum.png"></td> 
      <td rowspan="2" class="td-image"><img src="image/qch/ModelAnalysis.png" alt="Model Analysis"></td>    
      <td class="td-image" rowspan="2"><img src="image/qch/NeighboringVehicle.png" alt="Neighboring Vehicle"></td>    
      <td class="td-image" rowspan="2"><img src="image/qch/VehicleStdDev.png" alt="Vehicle Standard Deviation"></td>  
			</tr> 
		<tr class="last-tr">    
			<td class="timestamp"><b>Timestamp (UTC)</b></td>    
			<td class="obsType"><b>Observation Type</b></td>   
			<td class="td-ind"><b>Ind</b></td>   
			<td class="td-value"><b>Value</b></td>  
			<td class="unit"><b>Unit</b></td>  
			<td class="conf webkit-td-conf-fix"><b>Conf</b></td> 
			</tr>
		</thead>

<tbody class="obs-table-body">
<tr class="first-tr">    
	<td class="timestamp">2016-04-11 12:00:00</td>    
	<td class="obsType">essAirTemperature</td>   
	<td class="td-ind">0</td>    
	<td class="td-value">44.06</td>    
	<td class="unit">F</td>   
	<td class="conf">100%</td>   
	<td><img src="image/p.png" alt="Icon"></td>   
	<td><img src="image/nr.png" alt="Icon"></td>  
	<td><img src="image/p.png" alt="Icon"></td>   
	<td><img src="image/p.png" alt="Icon"></td>   
	<td><img src="image/p.png" alt="Icon"></td>  
	<td><img src="image/b.png" alt="Icon"></td>   
	<td><img src="image/p.png" alt="Icon"></td>  
	<td><img src="image/p.png" alt="Icon"></td>  
	<td><img src="image/nr.png" alt="Icon"></td> 
	<td><img src="image/b.png" alt="Icon"></td> 
	<td><img src="image/b.png" alt="Icon"></td> 
	<td><img src="image/b.png" alt="Icon"></td>
</tr>

</tbody></table>
</div>
  <div id="LayersMenuContainer">
  <ul id="LayersMenu">
    <li><label for="chkRwisLayer">ESS Obs</label><input type="checkbox" id="chkRwisLayer" /></li>
    <li><label for="chkMobileLayer">Mobile Obs</label><input type="checkbox" id="chkMobileLayer" /></li>
    <li><label for="chkRoadLayer">Segment Obs</label><input type="checkbox" id="chkRoadLayer" /></li>
    <li><label for="chkMetaDataLayer">ESS Metadata</label><input type="checkbox" id="chkMetaDataLayer" /></li>
</ul></div>
    <div id="map_canvas" > </div>
</div>
<!-- 	END OF MAP -->
<!-- 	/////// -->
<!-- 	/////// -->
<!-- 	BUTTOM TOOLBAR -->
<div class="bottom-toolbar-container" id="bottom-tools">
  
  <div id="layerControlContainer"><button id="DialogButton" class="DialogButton" ><img class="DialogButton" src="image/icons/doc.png" /></button> <input id="LayersMenuButton" type="button" value="Layers" /><select class="disableOnSummary" id="obstypes"><option value="0">Select an obs type</option></select>
    
    <fieldset id="unitsFieldSet" title="Units">
          <legend style="display: none;">Units</legend>
          <input type="radio" name="UNIT" checked="checked" id="englishUnits" />
          <label  class="disableOnSummary" for="englishUnits">English</label>
          <input  class="disableOnSummary" type="radio" name="UNIT" id="metricUnits" />
          <label for="metricUnits">Metric</label>
        </fieldset>
    </div>
  <div id="timeUTC"  class="disableOnSummary" ><input class="disableOnSummary" type="text" value="" id="datetimepicker"/>UTC</div>
  <div id="latlong" >Lat, Lon: <span id="latValue"></span>, <span id="lngValue"></span></div>
  <div id="stationCode" >Name: <span id="stationCodeValue"></span></div>
</div>
<!-- 	END OF BOTTOM TOOLBAR -->
<!--    /////// -->
<!-- 	FOOTER -->


<!--<script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
-->

  <script src="script/jquery/jquery-ui-full-1.11.4.js"></script>

<jsp:include page="/inc/mini-system-ui/map-ui-footer.jsp"></jsp:include>
<!-- 	/////// -->
<!-- 	POPULATE THE DOM BEFORE EXECUTING BLOCKING SCRIPTS -->
<script src="/script/xml.js" type="text/javascript"></script>
<script src="/script/QChTests.js" type="text/javascript"></script>
<script src="/script/ObsSorter.js" type="text/javascript"></script>
<script src="/script/StationMgr.js" type="text/javascript"></script>
<script src="/script/Listbox.js" type="text/javascript"></script>
<script src="/script/Common.js" type="text/javascript"></script>
<script src="/script/MapAreas.js" type="text/javascript"></script>
<script src="/script/js/lang/System.js" type="text/javascript"></script>
<script src="/script/js/util/Collections.js" type="text/javascript"></script>

<script src="/script/jquery/jquery.validate.js" type="text/javascript"></script>
	<script src="script/jquery/jquery.datetimepicker.full.js"></script>

  
<script type="text/javascript">
    	
      $.ajax({
        type: "GET",
        url: "ResetSession?roads=<%= Encode.forJavaScript( request.getParameter("roads")) %>"
      });
      
       $( "#LayersMenu" ).menu();

        var totalNavHeights = $('#top-nav').height() +
                $('#menu-nav').height() +
                $('#bottom-tools').height();

        var responsiveMap = function (e) {
            $('#map-container').attr(
                    'style',
                    'height:' +
                    parseInt(12 + $(window).height() - totalNavHeights) +
                    'px'
            );
        };

        responsiveMap();

        $(window).bind('resize', responsiveMap);



$("#LayersMenu").find("input:checkbox").each(function (i) {
        
        this.checked = true;
        this.disabled = true;
        var thisSet = $(this); 
        
        thisSet.parent("li").click(this,function(event)
        {
  //        event.handleObj.data.checked = !event.handleObj.data.checked;
          event.stopPropagation();
        });
        
        
      });
      document.getElementById('chkMetaDataLayer').checked = false;
      document.getElementById('englishUnits').checked = true;


  
    $('#LayersMenuButton').click(function() {
         $('#LayersMenuContainer').slideToggle();
  });
  
  
    
  
    
    
  var statesGroup;
  
  function getHighlightStyle(style)
  {
    var highlightStyle = {};
      for (var styleAttr in style)
      {
        if (style.hasOwnProperty(styleAttr))
          highlightStyle[styleAttr] = style[styleAttr];
      }
      highlightStyle.color = "#CFF";
      
      return highlightStyle;
  }
    
    
  function getPolylineHighlightStyle(style)
  {
    var highlightStyle = {};
      for (var styleAttr in style)
      {
        if (style.hasOwnProperty(styleAttr))
          highlightStyle[styleAttr] = style[styleAttr];
      }
      return highlightStyle;
    }
  
function setStandardStyleProperties(style)
{
  style.radius = 4;
  style.fillColor = style.color;
  style.color='black';
  style.weight = 1;
  style.opacity = 1;
  style.fillOpacity = 1;
  return style;
}
  
  
function setStandardPolylineStyleProperties(style)
{
  style.weight = 5;
  style.opacity = 1;
  style.fillOpacity = 1;
  return style;
}


var tileLayer = L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
/**
    var tileLayer = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png', {
var tileLayer = L.tileLayer('http://localhost:8080/tiles/cache/{z}/{x}/{y}.png', {
 */
  
  
 
 
  
    maxZoom: 17,
//  subdomains: ['1', '2', '3', '4'] ,
  subdomains: ['a', 'b', 'c'] ,
  attribution: '',
  id: 'mapbox.streets'
});

  
      

function onEachFeature(feature, layer)
{
  layer.on({
    mouseover: highlightFeature,
    mouseout: resetHighlight,
    click: resetHighlight
  });
  function highlightFeature(e)
  {
    var layer = e.target;
    layer.setStyle({
      weight: 5,
      dashArray: '',
      opacity: 0.8,
      fillOpacity: 0.8
    });
    if (!L.Browser.ie && !L.Browser.opera)
    {
      layer.bringToFront();
    }
  }

  function resetHighlight(e)
  {
    statesGroup.resetStyle(e.target);
  }
}


var statusStyles = [];
statusStyles["1"] = {
    fillColor: '#FF8822',
    weight: 2,
      opacity: 0.5,
      fillOpacity: 0.5,
    color: 'white',
    dashArray: '3'
  };

statusStyles["2"] = {
    fillColor: '#7722FF',
    weight: 2,
      opacity: 0.5,
      fillOpacity: 0.5,
    color: 'white',
    dashArray: '3'
  };

statusStyles["3"] = {
    fillColor: '#555555',
      opacity: 0.5,
      fillOpacity: 0.5,
    color: 'white',
    dashArray: '3'
  };

var style = {
    fillColor: 'green',
    weight: 2,
      opacity: 0.5,
      fillOpacity: 0.5,
    color: 'white',
    dashArray: '3'
  };
  
var styleState = function(feature) {
        
        var stateStatus = feature.properties.status;
        if(!stateStatus)
          return style;
        if(stateStatus) 
          return statusStyles[stateStatus];
        else
          return style;
        };




  var  dialog = $( "#dialog-form" ).dialog({
      autoOpen: false,
      modal: true,
      draggable:false,
      resizable: false,
      width:"auto",
      minHeight:"380",
      dialogClass : "no-title-form"
      
    });
    
    
    $(window).resize(function() {
    $("#dialog-form").dialog("option", "position", "center");
});

(function ($) {
      
      $('.disableOnSummary').each(function (idx, el)
        {
          el.disabled = true;
          $(el).addClass('DisabledElement');
        });
        
     $.getJSON('states/status', function(response) {
     var stateStatuses = response;
     
     var statesData = {};
     statesData.type = "FeatureCollection";
     statesData.features = [];
     
     for(var state in statePolygonPoints)
     {
       var stateStatus = stateStatuses[state];
       if(!stateStatus)
         continue;
       
       
       var stateFeature = {};
       stateFeature.type="Feature";
       stateFeature.properties = {};
       stateFeature.properties.code = state;
       stateFeature.properties.status = stateStatus;
       stateFeature.geometry = {};
       stateFeature.geometry.type = "Polygon";
       stateFeature.geometry.coordinates = statePolygonPoints[state];
       
       statesData.features.push(stateFeature);
     }
   
   
statesGroup = L.geoJson(statesData, {style: styleState, onEachFeature: onEachFeature});

map.setStatesLayer(statesGroup);
});

map = L.wxdeSummaryMap('map_canvas', {
  center: [43, -97],
  attributionControl:false,
  zoom: 4,
  layers: [tileLayer],
  stationCodeDiv: document.getElementById('stationCodeValue'),
  latDiv: document.getElementById('latValue'),
  lngDiv: document.getElementById('lngValue'),
  lstObstypes: document.getElementById('obstypes'),
  platformDetailsWindow:
  { 
    dialog:dialog, 
    platformDetailsDiv:$('#platform-details'),
    platformObsTable:$('#obs-data')
  },
  selectedTimeFunction : function()
  {
    var date = $('#datetimepicker').datetimepicker('getValue');
    //time was shifted so it would display UTC time, so it needs to be shifted back
    // to get the right milliseconds value
    return date.getTime() - (date.getTimezoneOffset()*60*1000);
    //return date.getTime();
  },
  useMetricUnitsFunction:function()
  {
    return $("#metricUnits").is(':checked');
  },
  selectedObsTypeFunction : function()
  {
    var obstype = $('#obstypes').val();
    
    if(obstype === 'StationCode')
      return "0";
    else
      return obstype;
  }
});


map.showDialog();


  
var rwisStyle =   setStandardStyleProperties({ color: "#d819d8"});
var rwisHighlightStyle = getHighlightStyle(rwisStyle);
var rwisOptions = {checkbox: document.getElementById("chkRwisLayer"), highlighter: new StaticLayerStyler(rwisHighlightStyle)};
map.registerWxdeLayer(L.wxdeLayer('RwisLayer', createCircleMarkers, new StaticLayerStyler(rwisStyle),  rwisOptions));

var mobileStyle = setStandardStyleProperties({color: "blue"});
var mobileHighlightStyle = getHighlightStyle(mobileStyle);
var mobileOptions = {checkbox: document.getElementById("chkMobileLayer"), highlighter: new StaticLayerStyler(mobileHighlightStyle)};
map.registerWxdeLayer(L.wxdeLayer('MobileRwisLayer', createCircleMarkers, new StaticLayerStyler(mobileStyle), mobileOptions));


var metaDataStyle = setStandardStyleProperties({ color: "grey"});
metaDataStyle.radius = 8;
var metaHighlightStyle = getHighlightStyle(metaDataStyle);
var metaDataOptions = {hasObs: false, checkbox: document.getElementById("chkMetaDataLayer"), highlighter: new StaticLayerStyler(metaHighlightStyle)};
map.registerWxdeLayer(L.wxdeLayer('MetaDataLayer', createCircleMarkers, new StaticLayerStyler(metaDataStyle), metaDataOptions));



var highlightRoadStyle = setStandardPolylineStyleProperties({ color: '#CFF'});
highlightRoadStyle.weight = 16;
var roadOptions = {checkbox: document.getElementById("chkRoadLayer"), 
  highlighter: new RoadHighlighter(highlightRoadStyle, map),
  showObsLabels: false,
  isForecastOnly: true,
  enabledForTime: function(time)
  {
      var now = new Date();

    //  now.setTime(now.getTime() + (now.getTimezoneOffset() * 60 * 1000));
      now.setMinutes(0);
      now.setSeconds(0);
      now.setMilliseconds(0);


      return time > now.getTime();
  },
  obsRequestBoundsFunction: function(layer)
  {
    return L.latLngBounds(layer.getLatLng(), layer.getLatLng()) ;
  }}; 

var roadStyles = [];
roadStyles["0"] = setStandardPolylineStyleProperties({ color: "#F494FE"});
roadStyles["1"] = setStandardPolylineStyleProperties({ color: "#DA03F1"});
roadStyles["2"] = setStandardPolylineStyleProperties({ color: "#71017D"});

var roadStyler = new RoadStatusStyler(roadStyles, map, roadOptions.highlighter);

map.registerWxdeLayer(L.wxdeLayer('RoadLayer', processPolylineData, roadStyler, roadOptions));



var minuteInterval = 20;
var allowTimes = [];
for(var hour = 0; hour< 24; ++hour)
{
  for(var minute = 0; minute <60; minute += minuteInterval)
  {
    allowTimes.push(hour + ':' + minute);
  }
}

var startDate = new Date();
//startDate.setMinutes(startDate.getMinutes()- startDate.getMinutes() % minuteInterval);
//shift time to display as UTC
startDate.setSeconds(0);
startDate.setMilliseconds(0);
startDate.setTime(startDate.getTime() + (startDate.getTimezoneOffset()*60*1000));

//set max time to the next 20-minute interval + 6 hours
var maxTime = new Date(startDate.getTime() + (1000 * 60 * 60 * 6) + (1000 * 60 * 20));
maxTime.setMinutes(maxTime.getMinutes()- maxTime.getMinutes() % minuteInterval);

$('#datetimepicker').datetimepicker({
	step: 20,
  value: startDate,
    onSelectTime : function()
    {
      //alert('close');
      map.refreshLayers();
    },
    yearStart: 2005,
    yearEnd: maxTime.getUTCFullYear(),
    maxTime: maxTime,
    maxDate: maxTime
});

    $('#obstypes').change(function() {
      if(this.value === 'StationCode')
        map.showStationCodeLabels();
      else
      {
        if(map.hasStationCodeLabels)
          map.hideLayerDivs();
          
        map.refreshLayers();
      }
    });
    
    $('input[name="UNIT"]').change(function(){
      map.updateObstypeLabels();
      map.updateObsValueUnits();
    });


    $('#datetimepicker').datetimepicker('setDate', startDate );


$('#DialogButton').click(function() {
         map.showDialog(true);
  });
           
map.on('mousedown', function (e)
{
  $('#datetimepicker').datetimepicker('hide');
});
    })(jQuery);
</script>
</body>
</html>
