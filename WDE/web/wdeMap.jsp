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
    <link rel="stylesheet" href="/style/wdemap-sidebar.css"/>
    <!-- top menu CSS style -->
    <link href="/style/top-mini-nav.css" rel="stylesheet"/>
    <link href="/style/wxde-mini-style.css" rel="stylesheet"/>
    <link rel="stylesheet" href="/style/main-accessibility-styles.css"/>
    <link href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css"/>

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
    <script src="//maps.googleapis.com/maps/api/js?key=AIzaSyDZYilON7LQQ1Fg8th5rot3cFinuvVjO-8&sensor=false"
            type="text/javascript"></script>
    <script type="text/javascript" src="//code.jquery.com/jquery-1.11.0.min.js"></script>

    <!--[if IE 8]>
    <link href="/style/IE-styles-mini.css" type="text/css" rel="stylesheet">
    <link href="/style/obs-table-ie82.css" type="text/css" rel="stylesheet">
    <![endif]-->

</head>

<body onload="onLoad()" onunload="onUnload()" class="wde-map-page">
<!-- Include the top part of the page container the Header and Navigation -->
<jsp:include page="/inc/mini-system-ui/miniHeader.jsp"></jsp:include>

<div id="tableModifier">
    <img src="/image/minified-map-ui/fhwa-logo-mini.png" alt="FHWA Logo"/><br><br>
</div>
<!-- 	/////// -->
<!-- 	TOP TOOLBAR -->
<div class="top-toolbar-container" id="top-tools">

    <div class="jumplist-container">
        <select id="jumpList" title="Jump List" name="jumpList" onchange="Jump()"></select>
    </div>

    <div class="btn-group pull-left">
        <button type="button" class="btn btn-xs btn-pills" id="btnInfoWindow">
            <img src="/image/icons/dark/fa-infowindow.png" class="button-icon hidden" alt="Infowindow Icons"/>
            <img src="/image/icons/dark/fa-infowindow-alt.png" class="button-icon-alt" alt="Infowindow Icons"/>
            <span>InfoWindow</span>
        </button>
        <button type="button" class="btn btn-xs btn-pills active" id="btnShade">
            <img src="/image/icons/dark/fa-shade.png" class="button-icon" alt="Shade Icon"/>
            <img src="/image/icons/dark/fa-shade-alt.png" class="button-icon-alt hidden" alt="Shade Icon"/>
            <span>Shade</span>
        </button>
    </div>

    <div id="progressLabel">Retrieving station data ...</div>
    <div id="latlong">Lat, Lon:</div>
    <div id="stationCode">Station Code:</div>

</div>
<!-- 	END OF TOP TOOLBAR -->
<!-- 	/////// -->
<!-- 	BUTTOM TOOLBAR -->
<div class="bottom-toolbar-container" id="bottom-tools">

    <div class="left-side-tools">

        <select id="obsTypeList" name="obsTypeList" onchange="GetObsValue()" title="Observations"></select>
        <fieldset title="Units" id="unitsFieldSet" style="display: inline; ">
            <legend style="display: none;">Units</legend>
            <input id="rbMetric" name="rbUnits" type="radio" onclick="LabelClicked('m')" title="Metric Units"/> Metric
            <input id="rbEnglish" name="rbUnits" type="radio" onclick="LabelClicked('e')" title="English Units" checked="checked"/> English
        </fieldset>

        <button type="button" id="toggleRS" class="toggle-button" data-title="Click to Hide" data-source="2"
                data-type="S">
            <img src="/image/icons/light/fa-checked.png" class="checkbox-icon" alt="Checked Box"/>
            <img src="/image/icons/light/fa-unchecked.png" class="checkbox-icon hidden" alt="Unchecked Box"/>
            <img alt="vdt-mobile" src="image/mm_segment_line.png"/> Road Segments
        </button>

        <!--
        <button type="button" id="toggleVDT" class="toggle-button" data-title="Click to Hide" data-source="2"
                data-type="M">
            <img src="/image/icons/light/fa-checked.png" class="checkbox-icon" alt="Checked Box"/>
            <img src="/image/icons/light/fa-unchecked.png" class="checkbox-icon hidden" alt="Unchecked Box"/>
            <img alt="vdt-mobile" src="image/mm_12_brown.png" class="marker-icon"/> VDT Mobile
        </button>
        -->

        <button type="button" id="toggleWxDEMbl" class="toggle-button" data-title="Click to Hide" data-source="1"
                data-type="M">
            <img src="/image/icons/light/fa-checked.png" class="checkbox-icon" alt="Checked Box"/>
            <img src="/image/icons/light/fa-unchecked.png" class="checkbox-icon hidden" alt="Unchecked Box"/>
            <img alt="vdt-mobile" src="image/mm_12_blue.png" class="marker-icon"/> WxDE Mobile
        </button>

        <button type="button" id="toggleWxDE" class="toggle-button" data-title="Click to Hide" data-source="1"
                data-type="PT">
            <img src="/image/icons/light/fa-checked.png" class="checkbox-icon" alt="Checked Box"/>
            <img src="/image/icons/light/fa-unchecked.png" class="checkbox-icon hidden" alt="Unchecked Box"/>
            <img alt="vdt-mobile" src="image/mm_12_purple.png" class="marker-icon"/> Nonmobile
        </button>

    </div>

    <div id="timeUTC"></div>

</div>
<!-- 	END OF BOTTOM TOOLBAR -->
<!-- 	/////// -->
<!-- 	SIDEBAR -->
<div id="sideBar" class="side-bar">
    <div id="retainBlock">
				<span class="close-panel pull-right" id="closeShade" title="Close">
					<img src="/image/icons/dark/fa-close.png" alt="Close Icon" style="margin-top: -4px;"/>
				</span>

        <h3>Observations</h3>
    </div>
    <p>Click any marker on the map to load data.</p>
</div>
<!-- 	END OF SIDEBAR -->
<!-- 	/////// -->
<!-- 	MAP CONTAINER -->
<div id="map-container">
    <div id="map_canvas"></div>
</div>
<!-- 	END OF MAP -->
<!-- 	/////// -->
<div id="button1">
    <input id="btn_circle" name="btn_circle" type="button" value="Clear Circle" onclick="ClearCircle()"/>
</div>
<!--    /////// -->
<!-- 	FOOTER -->
<script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
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
<script src="/script/wdemap.js" type="text/javascript"></script>
<script src="/script/jquery/jquery.validate.js" type="text/javascript"></script>

<script type="text/javascript">
    (function ($) {

        var totalNavHeights = $('#top-nav').height() +
                $('#top-tools').height() +
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

        //make sidebar toggle visibility by sliding. ok
        //add control by the side as trigger
        //load data into it.
        var $sideBar = $('#sideBar'); //cache sideBar once
        var sbWidth = $sideBar.width() + 20;

        //start $('#sideBar') and children functions
        $sideBar
                .css({
                    'left': -sbWidth,
                    'display': 'block'
                })
                .on('click', function () {
                    var $this = $(this);
                    if (parseInt($this.css('left')) !== 0) {
                        $this.animate({'left': 0}, 400);
                    }
                    $this = null;
                })//chaining event for child .close-panel
                .on('click', '#closeShade', function () {
                    resetMouseOver();
                    resetMarker();
                    sbWidth = $sideBar.width() + 20;
                    if (parseInt($sideBar.css('left')) >= 0) {
                        $sideBar.animate({'left': -sbWidth}, 400);
                    } else {
                        $sideBar.animate({'left': 0}, 400);
                    }
                }) //chaining event for child #fullScreen(anchor)
                .on('click', '#fullScreen', function () {
                    var obsWindow = window.open("", "obsWindow", "width=695 height=780");
                    obsWindow.document.write('<html><head><title>Print Table!</title><link rel="stylesheet" type="text/css" href="/style/printable-table.css"></head><body>');
                    obsWindow.document.write(
                            '<script>' +
                            '	function printThis() {' +
                            '		document.getElementById("btnPrint").style.display="none";' +
                            '		document.getElementById("qualityChecks").print();' +
                            '}<script/>'
                    );
                    obsWindow.document.write('< /script>');
                    obsWindow.document.write('<button type="button" id="btnPrint" onclick="printThis()">Print</button>');
                    obsWindow.document.write(observationTable);
                    obsWindow.document.write('</body></html>');
                    console.log(observationTable);
                })//chaining viewStation button event
                .on('click', '#viewStation', function () {
                    m_oMap.panTo(currentStation.position);
                });

        //This is used to toggle between InfoWindow and
        //The sidebar "Shade"
        //When the user clicks the #btnShade button
        //The user opts to use the Shade
        //The following events will be triggered:
        $('#btnShade').on('click', function () {
            var $this = $(this);
            //When the user select InfoWindow then the ff events will be triggered:
            if (!($this.hasClass('active'))) {
                $sideBar.css({
                    left: 0,
                    display: 'block'
                });//Display the Shade
                m_oInfoWindow.close();//Close the InfoWindow
                if (currentStation !== null)
                    markerInfoWindow.open(m_oMap, currentStation);
                //Display the markerInfoWindow/markerIcon
                $this.addClass('active');	 //Remove the 'clicked' class from the button
                $this.children('img.button-icon-alt').toggleClass('hidden');
                $this.children('img.button-icon').toggleClass('hidden');

                $this.siblings('#btnInfoWindow').removeClass('active');
                $this.siblings('#btnInfoWindow').children('img.button-icon-alt').toggleClass('hidden');
                $this.siblings('#btnInfoWindow').children('img.button-icon').toggleClass('hidden');
            }
            $this = null;
        });
        //When the user clicks the #btnInfoWindow button
        //The user opts to use the InfoWindow of google maps
        //The following events will be triggered:
        $('#btnInfoWindow').on('click', function () {
            var $this = $(this);
            if (!($this.hasClass('active'))) {
                if (markerInfoWindow.map != null)
                    markerInfoWindow.setMap(null);	//Hide the markerInfoWindow/markerIcon
                if (m_oSelectedMarker != null)
                    m_oInfoWindow.open(m_oMap, m_oSelectedMarker);//Open the InfoWindow
                $sideBar.css('display', 'none');//Hide the Shade
                $this.addClass('active');		//Add the class 'clicked'
                $this.siblings('#btnShade').removeClass('active');

                $this.children('img.button-icon-alt').toggleClass('hidden');
                $this.children('img.button-icon').toggleClass('hidden');

                $this.siblings('#btnShade').removeClass('active');
                $this.siblings('#btnShade').children('img.button-icon-alt').toggleClass('hidden');
                $this.siblings('#btnShade').children('img.button-icon').toggleClass('hidden');
            }
            $this = null;
        });

        $('#btnInfoWindow, #btnShade').on('focus', function () {
            $(this).blur();
        });

    })(jQuery);
</script>
</body>
</html>
