<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%
	response.setHeader("Cache-Control","no-cache,no-store,must-revalidate");//HTTP 1.1
	response.setHeader("Pragma","no-cache"); //HTTP 1.0
	response.setDateHeader ("Expires", 0); //prevents caching at the proxy server
 %>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width,initial-scale=1,maximum-scale=1" />
    <title>Weather Data Environment - Map</title>
    
	<!-- Bootstrap3 UI framework -->
    <link href="/style/vendor/bootstrap.min.css" rel="stylesheet" />
    
    <link href="/style/Archive.css" rel="stylesheet" />
    
    <script src="http://maps.google.com/maps/api/js?sensor=true" type="text/javascript"></script>
    <script src="/script/jquery/jquery-1.9.1.js" type="text/javascript"></script>
    <script src="/script/jquery/jquery-ui-1.10.2.custom.js" type="text/javascript"></script>
    <link  href="/style/jquery/overcast/jquery-ui-1.10.2.custom.css" rel="stylesheet" type="text/css" />
	<script src="/script/jquery/jquery.validate.js" type="text/javascript"></script>
    
    <script src="/script/jquery/jquery.ui.map.js" type="text/javascript"></script>
    <script src="/script/jquery/jquery.ui.map.overlays.js" type="text/javascript"></script>
    <script src="/script/jquery/jquery.ui.map.services.js" type="text/javascript"></script>
    
	<!-- top menu CSS style -->
	<link href="/style/top-mini-nav.css" rel="stylesheet" />
	
	<!-- side menu CSS style -->
	<link href="/style/side-slide.css" rel="stylesheet" />
	
	<!--[if IE ]>
		<link href="/style/IE-styles-mini.css" type="text/css" rel="stylesheet">
	<![endif]-->

	<!-- app: font-awesome vector images as fonts -->
    <link href="/vendor/font-awesome/css/font-awesome.min.css" rel="stylesheet" />
	
    <!-- jQuery-loader includes -->
    <link href="/script/jquery-loader/jquery.loader.css" rel="stylesheet" />
    <script type="text/javascript" src="/script/jquery-loader/jquery.loader.js"></script>
    
    <!--  Google Analytics Script -->
	<script>
	  (function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
	  (i[r].q=i[r].q||[]).push(arguments);},i[r].l=1*new Date();a=s.createElement(o),
	  m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m);
	  })(window,document,'script','//www.google-analytics.com/analytics.js','ga');
	
	  ga('create', 'UA-39857002-1', 'clarus-system.com');
	  ga('send', 'pageview');
	</script>

	<script>

	$(function() {
		$('body').loader('show');

		$(document).tooltip({
			position: {
			  my: "center bottom-20",
			  at: "center top",
			  using: function( position, feedback ) {
				$( this ).css( position );
				$( "<div>" )
				  .addClass( "arrow" )
				  .addClass( feedback.vertical )
				  .addClass( feedback.horizontal )
				  .appendTo( this );
			  }
			}
		});
			
		var geocoder = new google.maps.Geocoder();;
		var stl = new google.maps.LatLng(38.63, -90.2);
		var hazelwood = new google.maps.LatLng(38.7714, -90.3708);
		var graniteCity = new google.maps.LatLng(38.7014, -90.1486);
		var belleville = new google.maps.LatLng(38.52,-89.9839);
		var kirkwood = new google.maps.LatLng(38.5833, -90.4067);
		
		var farmington = new google.maps.LatLng(42.482,-83.4);
		
		//	var polyCords = [
		//		hazelwood, graniteCity, belleville, kirkwood
		//	];

		var center = farmington;
		var rectBounds = new google.maps.LatLngBounds(
				new google.maps.LatLng((center.lat() - .07), (center.lng() - .07))
				,new google.maps.LatLng((center.lat() + .07), (center.lng() + .07))
		);
		
		var icnTrafficGreen = "/image/map/trafficlight_g.png";
		var icnTrafficYellow = "/image/map/trafficlight_y.png";
		var icnBsmGreen = "/image/map/cctv_g.png";
		var icnBsmYellow = "/image/map/cctv_y.png";
		var icnBothGreen = "/image/map/both_g2.png";
		var icnBothYellow = "/image/map/both_y2.png";
		var icnOff = "/image/map/accesdenied_r.png";
			
		var csBsm =
			'<h1 id="firstHeading" class="firstHeading">{name}</h2>'+
			'<h2 id="secondHeading" class="secondHeading">Basic Safety Message</h2>'+
			'<div id="bodyContent">'+
			'<p><b>{name}</b>, Lorem ipsum dolor sit amet, consectetuer adipiscing elit, '+
			'sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat '+
			'volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation '+
			'ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. </p>'+
			'</div>';
		
		var csTraffic =
			'<h1 id="firstHeading" class="firstHeading">{name}</h2>'+
			'<h2 id="secondHeading" class="secondHeading">Signal Phase And Timing</h2>'+
			'<div id="bodyContent">'+
			'<p><b>{name}</b>, Lorem ipsum dolor sit amet, consectetuer adipiscing elit, '+
			'sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat '+
			'volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation '+
			'ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. </p>'+
			'</div>';
		
		var csOff =
			'<h1 id="firstHeading" class="firstHeading">{name}</h2>'+
			'<h2 id="secondHeading" class="secondHeading">Offline</h2>'+
			'<div id="bodyContent">'+
			'<p><b>{name}</b>, Lorem ipsum dolor sit amet, consectetuer adipiscing elit, '+
			'sed diam nonummy nibh euismod tincidunt ut laoreet dolore magna aliquam erat '+
			'volutpat. Ut wisi enim ad minim veniam, quis nostrud exerci tation '+
			'ullamcorper suscipit lobortis nisl ut aliquip ex ea commodo consequat. </p>'+
			'</div>';
		
		var csBoth = csTraffic + csBsm;
		
		var rseTypes = [csTraffic, csTraffic, csBsm, csBsm, csBoth, csBoth, csOff];
		var rseIcons = [icnTrafficGreen, icnTrafficYellow, icnBsmGreen, icnBsmYellow, icnBothGreen, icnBothYellow, icnOff];
		
		var myRSEs=new Array(
			{title:"10 Mile & Haggerty Rd", latitude:"42.4678", longitude:"-83.43510000000001", rseType: 4},
			{title:"10 Mile & Novi Rd", latitude:"42.4667", longitude:"-83.47450000000001", rseType: 4},
			{title:"12 Mile & Beck Rd", latitude:"42.4958", longitude:"-83.5157", rseType: 4},
			{title:"12 Mile & Bunker Hill Dr", latitude:"42.4983", longitude:"-83.38890000000001", rseType: 4},
			{title:"12 Mile & Drake Rd", latitude:"42.4979", longitude:"-83.3977", rseType: 2},
			{title:"12 Mile & Farmington Rd", latitude:"42.4988", longitude:"-83.3784", rseType: 4},
			{title:"12 Mile & Haggerty Rd", latitude:"42.497", longitude:"-83.4363", rseType: 2},
			{title:"12 Mile & Halsted Rd", latitude:"42.4975", longitude:"-83.41710000000001", rseType: 2},
			{title:"12 Mile & Kendallwood Rd", latitude:"42.499", longitude:"-83.3699", rseType: 6},
			{title:"12 Mile & Meadowbrook Rd", latitude:"42.4964", longitude:"-83.4564", rseType: 6},
			{title:"12 Mile & Middlebelt Rd", latitude:"42.4995", longitude:"-83.33929999999999", rseType: 2},
			{title:"12 Mile & Novi Rd", latitude:"42.4955", longitude:"-83.476", rseType: 2},
			{title:"12 Mile & Twelve Oaks Cres", latitude:"42.4957", longitude:"-83.4662", rseType: 0},
			{title:"8 Mile & Haggerty Rd", latitude:"42.4386", longitude:"-83.43640000000001", rseType: 0},
			{title:"9 Mile & Haggerty Rd", latitude:"42.4536", longitude:"-83.4345", rseType: 0},
			{title:"9 Mile & Novi Rd", latitude:"42.4522", longitude:"-83.4742", rseType: 0},
			{title:"Crescent Blvd & Novi Rd", latitude:"42.4838", longitude:"-83.4756", rseType: 2},
			{title:"Crystal & Glen2", latitude:"42.4438", longitude:"-83.43729999999999", rseType: 2},
			{title:"Crystal & Glen", latitude:"42.4438", longitude:"-83.43729999999999", rseType: 0},
			{title:"Flint & Novi Rd", latitude:"42.4784", longitude:"-83.4756", rseType: 0},
			{title:"Grand River Ave & Haggerty Rd", latitude:"42.4716", longitude:"-83.4352", rseType: 5},
			{title:"Grand River Ave & Novi Rd", latitude:"42.4805", longitude:"-83.4755", rseType: 0},
			{title:"I-696 & Orchard Lake Rd", latitude:"42.4944", longitude:"-83.35809999999999", rseType: 0},
			{title:"I-96 & Beck Rd", latitude:"42.4933", longitude:"-83.5153", rseType: 1},
			{title:"I-96 & Kent Lake Rd", latitude:"42.5162", longitude:"-83.6563", rseType: 1},
			{title:"I-96 & Milford Rd", latitude:"42.5193", longitude:"-83.6169", rseType: 4},
			{title:"I-96 & Novi Rd", latitude:"42.4866", longitude:"-83.476", rseType: 2},
			{title:"Oaks Dr & Beck Rd", latitude:"42.4913", longitude:"-83.47580000000001", rseType: 2},
			{title:"Telegraph Rd & 10 Mile", latitude:"42.4719", longitude:"-83.28189999999999", rseType: 2},
			{title:"Telegraph Rd & 11 Mile Service Dr", latitude:"42.4963", longitude:"-83.28489999999999", rseType: 2},
			{title:"Telegraph Rd & 12 1/2 Mile", latitude:"42.505275", longitude:"-83.285349", rseType: 2},
			{title:"Telegraph Rd & 12 Mile", latitude:"42.501231", longitude:"-83.285197", rseType: 2},
			{title:"Telegraph Rd & 13 Mile", latitude:"42.515406", longitude:"-83.28574999999999", rseType: 2},
			{title:"Telegraph Rd & 9 Mile", latitude:"42.4576", longitude:"-83.27849999999999", rseType: 6},
			{title:"Telegraph Rd & Civic Center Dr", latitude:"42.479569", longitude:"-83.285372", rseType: 6},
			{title:"Telegraph Rd & Crossover N of 10 Mile", latitude:"42.4729", longitude:"-83.28230000000001", rseType: 4},
			{title:"Telegraph Rd & Crossover N of 12 Mile", latitude:"42.5028", longitude:"-83.28530000000001", rseType: 2},
			{title:"Telegraph Rd & Crossover N of 13 Mile", latitude:"42.517158", longitude:"-83.285842", rseType: 2},
			{title:"Telegraph Rd & Crossover N of 9 Mile", latitude:"42.4591", longitude:"-83.27849999999999", rseType: 0},
			{title:"Telegraph Rd & Crossover N of Rushmore", latitude:"42511494.", longitude:"-83.285614", rseType: 0},
			{title:"Telegraph Rd & Crossover S of 10 Mile", latitude:"42.470315", longitude:"-83.28069499999999", rseType: 1},
			{title:"Telegraph Rd & Crossover S of 12 Mile", latitude:"42.499348", longitude:"-83.285138", rseType: 0},
			{title:"Telegraph Rd & Crossover S of 13 Mile", latitude:"42.513728", longitude:"-83.285642", rseType: 0},
			{title:"Telegraph Rd & Crossover S of 9 Mile", latitude:"42.456", longitude:"-83.27840000000001", rseType: 0},
			{title:"Telegraph Rd & Denso Dr", latitude:"42.4779", longitude:"-83.285", rseType: 0},
			{title:"Telegraph Rd & Garner St", latitude:"42.4627", longitude:"-83.279", rseType: 2},
			{title:"Telegraph Rd & Northwestern Service Dr", latitude:"42.4959", longitude:"-83.285", rseType: 0},
			{title:"Telegraph Rd & Raleigh Office Center", latitude:"42.4747", longitude:"-83.2834", rseType: 0},
			{title:"Telegraph Rd & Swanson Rd", latitude:"42.4825", longitude:"-83.2856", rseType: 0},
			{title:"Telegraph Rd & Tel-12 Dr", latitude:"42.4986", longitude:"-83.2852", rseType: 4}
		);
		
		$('#map_canvas').gmap({
			center: center
			,zoom: 11
			,mapTypeId: google.maps.MapTypeId.ROADMAP
		});
		$('#map_canvas').gmap('addMarker', {'position': center, 'draggable': false});
		$('#map_canvas').gmap('addShape', 'Rectangle', {
			'strokeWeight': 0 
			,'fillColor': "#008595" 
			,'fillOpacity': 0.5
			,'bounds': rectBounds
			,'editable': true
			,'bounds_changed': function() {
				rectBounds = this.bounds;
				countEquipmentInBounds();
				$("#neLat").html(rectBounds.getNorthEast().lat());
				$("#neLng").html(rectBounds.getNorthEast().lng());
				$("#swLat").html(rectBounds.getSouthWest().lat());
				$("#swLng").html(rectBounds.getSouthWest().lng());
			}
		});
		
		addMarkers();
		countEquipmentInBounds();
		$("#neLat").html(rectBounds.getNorthEast().lat());
		$("#neLng").html(rectBounds.getNorthEast().lng());
		$("#swLat").html(rectBounds.getSouthWest().lat());
		$("#swLng").html(rectBounds.getSouthWest().lng());

		function addMarkers() {
			$.each( myRSEs, function(i, marker) {
				$('#map_canvas').gmap('addMarker', { 
					position: new google.maps.LatLng(marker.latitude, marker.longitude),
					icon: rseIcons[marker.rseType],
					title: marker.title
				}).click(function() {
					$('#map_canvas').gmap('openInfoWindow', { 
						content: rseTypes[marker.rseType].replace(/\{name\}/g,marker.title),
						maxWidth: getMaxWidth(marker.title)
					}, this);
				});
			});
			$('body').loader('hide');
		}
		
		function getMaxWidth(title){
			if (title.length < 21) {
				return 325;
			} else if (title.length < 30) {
				return 425;
			} else if (title.length < 35) {
				return 500;
			} else {
				return 600;
			}
		}
		
		function countEquipmentInBounds(){
			var pnt;
			var iCount = 0;
			$.each( myRSEs, function(i, marker) {
				pnt = new google.maps.LatLng(marker.latitude, marker.longitude);
				if (rectBounds.contains(pnt)) {
					iCount++;
				}
			});
			$("#cntEquipment").html("Equipment Count: " + iCount);
		}
		
		$('#map_input').focus();
		$('#map_input').bind('keypress', function(e) {
			if(e.keyCode==13){
				moveCenter();
			}
		});
		$("#map_center_button").click(function() {
			moveCenter();
		});
		
		function moveCenter() {
			geocoder.geocode( { 'address': $("#map_input").val()}, function(results, status) {
				if (status == google.maps.GeocoderStatus.OK) {
					center = results[0].geometry.location;
					$('#map_canvas').gmap('get','map').setOptions({'center':center, 'zoom': 11});
					
					var active = $(location).attr("hash");

					if (active == "#tab1") {
						addRectangle();
					} else {
						addCircle();
					}
			   } else {
					alert('Geocode was not successful for the following reason: ' + status);
			   }
			});    
		}
		
		function addRectangle() {
			rectBounds = new google.maps.LatLngBounds(
					new google.maps.LatLng((center.lat() - .07), (center.lng() - .07))
					,new google.maps.LatLng((center.lat() + .07), (center.lng() + .07))
			);

			$("#map_canvas").gmap('clear', 'markers');
			$('#map_canvas').gmap('addMarker', {'position': center});
			$("#map_canvas").gmap('clear', 'overlays');
			$('#map_canvas').gmap('addShape', 'Rectangle', {
				'strokeWeight': 0 
				,'fillColor': "#008595" 
				,'fillOpacity': 0.5
				,'bounds': rectBounds
				,'editable': true
				,'bounds_changed': function() {
					addMarkers();
					rectBounds = this.bounds;
					countEquipmentInBounds();
					$("#neLat").html(rectBounds.getNorthEast().lat());
					$("#neLng").html(rectBounds.getNorthEast().lng());
					$("#swLat").html(rectBounds.getSouthWest().lat());
					$("#swLng").html(rectBounds.getSouthWest().lng());
				}
			});
			addMarkers();
			countEquipmentInBounds();
			$("#neLat").html(rectBounds.getNorthEast().lat());
			$("#neLng").html(rectBounds.getNorthEast().lng());
			$("#swLat").html(rectBounds.getSouthWest().lat());
			$("#swLng").html(rectBounds.getSouthWest().lng());
		}
		
		function addCircle() {
			$("#map_canvas").gmap('clear', 'markers');
			$('#map_canvas').gmap('addMarker', {'position': center});
			$("#map_canvas").gmap('clear', 'overlays');
			var rad = 10005;
			var shape = $('#map_canvas').gmap('addShape', 'Circle', {
				'strokeWeight': 0 
				,'fillColor': "purple" 
				,'fillOpacity': 0.25
				,'center': center
				,'radius': rad 
				,'clickable': false 
				,'editable': true
				,'radius_changed': function() {
					addMarkers();
					rectBounds = shape[0].getBounds();
					countEquipmentInBounds();		

					// center doesn't change, just the radius
					$("#crRadius").html(this.getRadius()/1000 + ' km');
				}
			});

			addMarkers();
			rectBounds = shape[0].getBounds();
			countEquipmentInBounds();		

			$("#crLat").html(center.lat());
			$("#crLng").html(center.lng());
			$("#crRadius").html(rad/1000 + ' km');
		}

	/*
		$('#map_canvas').gmap('addShape', 'Polygon', {
			'strokeWeight': 0 
			,'fillColor': "red" 
			,'fillOpacity': 0.5
			,'paths': polyCords
			,'editable': true
		});

	*/
		
		/*-------------------------------/
		/ @-- added by Robert Roth ------/
		/  This part should be included  /
		/  within this javascript block  /
		/-------------------------------*/
		
		function checkRectangle(){
			if($(location).attr("hash") == "#tab1")
				return true;
			else
				if($(location).attr("hash") == "#tab2")
					return false;
		}
			   
		$("#info_button, #radius_btn, #boundbox_btn").on("display_info", function(){
			if(checkRectangle() == true){
				$("#coordinates_circle").hide();
				$("#coordinates_rectangle").toggle();
			} else {
				$("#coordinates_rectangle").hide();
				$("#coordinates_circle").toggle();
			}
		});
		
		//*** Click functions for both the Box and Radius buttons..
		$("#radius_btn").click( function(){
			if(checkRectangle() == true)
				$("#coordinates_rectangle").hide();
			addCircle();
			$(this).removeClass("btn-primary").addClass("disabled btn-default");
			$("#boundbox_btn").removeClass("disabled").addClass("btn-primary");
		});
		$("#boundbox_btn").click( function(){
			if(checkRectangle() == false)
				$("#coordinates_circle").hide();
			addRectangle();
			$(this).removeClass("btn-primary").addClass("disabled btn-default");
			$("#radius_btn").removeClass("disabled").addClass("btn-primary");
		});
		
		//set hash to tab1 w/c is for the Rectangular Shape
		document.location.hash = "tab1";
				
		//events for the info_button
		$("#info_button").click( function() {
			$(this).trigger("display_info");
		});
		
		//*** Reset map_input input box then focus upon page load..
		$('#map_input').val("");
		$('#map_input').fadeTo();
			
		//on document load disable Bounding Box since it is the default shape..
		$("#boundbox_btn").removeClass("btn-primary").addClass("disabled btn-default");
	});
	</script>
	
	<style type="text/css">
		label {
			width: 120px;
		}
		label.ui-button {
			width: 160px;
		}
	</style>
	
	<link href="/style/wxde-mini-style.css" rel="stylesheet" />
</head>
<body onunload="">
	<jsp:include page="/inc/mini-system-ui/miniHeader.jsp"></jsp:include>
	<!-- side menu -->
	<div class="side-menu pull-left fhwa-gradient-color" id="side_menu">
		<br><br><br><br>
		<h3>Settings</h3>
		<hr>
		<p class="tab-trigger fhwa-gradient-color">OPEN SETTINGS</p>
	</div>
	<!-- map system -->
    <div class="map-container">
		
        <div id="map_canvas"></div>
        
    </div>
    
	<jsp:include page="/inc/mini-system-ui/miniMapTools.jsp"></jsp:include>
	<jsp:include page="/inc/main-wxde-ui/mainFooter.jsp"></jsp:include>

	<!-- side menu script placed at the bottom for faster page-layout load -->
	<script type="text/javascript" src="/script/side-slide.js"></script>
	
	<script type="text/javascript">
		$(document).ready(function() {
			
			var topNavHeight = $('#top-nav').height() + $('#menu-nav').height();
			
			function responsiveMap(e) {
				$('.map-container').attr(
						'style',
						'height:' +
						parseInt($(window).height() - (topNavHeight + 50)) +
						'px'
						);
			};
			
			responsiveMap();
			
			$(window).bind('resize', responsiveMap);
		});
	</script>
	
</body>
</html>