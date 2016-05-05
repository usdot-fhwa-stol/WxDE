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
  
    
    <script type="text/javascript" src="//code.jquery.com/jquery-1.11.0.min.js"></script>


    <!--
    
    <script src="script/jquery/jquery-2.2.0.js"></script>
-->
	
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
   
<div id="dialog-form" style="display:none;">

  <div id="dialog-form">
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
</div>
  <div id="LayersMenuContainer">
  <ul id="LayersMenu">
    <li><label for="chkRwisLayer">RWIS Obs</label><input type="checkbox" id="chkRwisLayer" /></li>
    <li><label for="chkMobileLayer">Mobile Obs</label><input type="checkbox" id="chkMobileLayer" /></li>
    <li><label for="chkRoadLayer">Road Obs</label><input type="checkbox" id="chkRoadLayer" /></li>
    <li><label for="chkMetaDataLayer">Sensors</label><input type="checkbox" id="chkMetaDataLayer" /></li>
</ul></div>
    <div id="map_canvas" > </div>
</div>
<!-- 	END OF MAP -->
<!-- 	/////// -->
<!-- 	/////// -->
<!-- 	BUTTOM TOOLBAR -->
<div class="bottom-toolbar-container" id="bottom-tools">
  
  <div id="layerControlContainer"> <input id="LayersMenuButton" type="button" value="Layers" /><select  id="obstypes"><option value="0">Select an obs type</option></select>
    
    <fieldset id="unitsFieldSet" title="Units">
          <legend style="display: none;">Units</legend>
          <input type="radio" name="UNIT" checked="checked" id="englishUnits" />
          <label  for="englishUnits">English</label>
          <input type="radio" name="UNIT" id="metricUnits" />
          <label for="metricUnits">Metric</label>
        </fieldset>
    </div>
  <div id="timeUTC" ><input type="text" value="" id="datetimepicker"/></div>
  <div id="latlong" >Lat, Lon: <span id="latValue"></span>, <span id="lngValue"></span></div>
  <div id="stationCode" >Station Code: <span id="stationCodeValue"></span></div>
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
    (function ($) {
      
      
      $.ajax({
        type: "GET",
        url: "ResetSession"
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
  style.fillOpacity = 0.8;
  return style;
}


/**
 * 
 * Real URL: http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png 
 * Local URL: http://localhost:8080/tiles/cache/{z}/{x}/{y}.png
 * 
 */

var tileLayer = L.tileLayer('http://otile{s}.mqcdn.com/tiles/1.0.0/map/{z}/{x}/{y}.png', {
  maxZoom: 18,
  subdomains: ['1', '2', '3', '4'] ,
  attribution: '',
  id: 'mapbox.streets'
});

function onEachFeature(feature, layer)
{
  layer.on({
    mouseover: highlightFeature,
    mouseout: resetHighlight
  });
  function highlightFeature(e)
  {
    var layer = e.target;
    layer.setStyle({
      weight: 5,
      color: '#666',
      dashArray: '',
      fillOpacity: 0.5
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



var style = {
    fillColor: 'blue',
    weight: 2,
    opacity: 1,
    color: 'white',
    dashArray: '3'
  };

statesGroup = L.geoJson(statesData, {style: style, onEachFeature: onEachFeature});

  var  dialog = $( "#dialog-form" ).dialog({
      autoOpen: false,
      modal: true,
      draggable:false,
      resizable: false,
      width:"auto",
      height:"auto"
      
    });
    
    $(window).resize(function() {
    $("#dialog-form").dialog("option", "position", "center");
});

map = L.wxdeSummaryMap('map_canvas', {
  center: [43, -97],
  attributionControl:false,
  zoom: 4,
  layers: [tileLayer],
  statesLayer: statesGroup,
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
    return $('#datetimepicker').datetimepicker('getValue').getTime();
  },
  useMetricUnitsFunction:function()
  {
    return $("#metricUnits").is(':checked');
  },
  selectedObsTypeFunction : function()
  {
    return $('#obstypes').val();
  }
});

  
var rwisOptions = {checkbox: document.getElementById("chkRwisLayer")};
map.registerWxdeLayer(L.wxdeLayer('RwisLayer', createCircleMarkers, setStandardStyleProperties({ color: "#d819d8"}), rwisOptions));

var mobileOptions = {checkbox: document.getElementById("chkMobileLayer")};
map.registerWxdeLayer(L.wxdeLayer('MobileRwisLayer', createCircleMarkers, setStandardStyleProperties({color: "blue"}), mobileOptions));

var metaDataOptions = {hasObs: false, checkbox: document.getElementById("chkMetaDataLayer")};
var metaDataStyle = setStandardStyleProperties({ color: "grey"});

map.registerWxdeLayer(L.wxdeLayer('MetaDataLayer', createCircleMarkers, metaDataStyle, metaDataOptions));


var roadOptions = {checkbox: document.getElementById("chkRoadLayer"), 
  obsRequestBoundsFunction: function(layer)
  {
    return layer.getBounds();
  }}; 
map.registerWxdeLayer(L.wxdeLayer('RoadLayer', processPolylineData, setStandardPolylineStyleProperties({ color: "green"}), roadOptions));


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
startDate.setMinutes(startDate.getMinutes()- startDate.getMinutes() % minuteInterval);


$('#datetimepicker').datetimepicker({
	allowTimes: allowTimes,
  value: startDate,
    onClose : function()
    {
      //alert('close');
      map.refreshLayers();
    }
});

    $('#obstypes').change(function() {
      map.refreshLayers();
    });
    
    $('input[name="UNIT"]').change(function(){
      map.updateObstypeLabels();
      map.updateObsValueUnits();
    });


    $('#datetimepicker').datetimepicker('setDate', startDate );


    })(jQuery);
</script>
</body>
</html>
