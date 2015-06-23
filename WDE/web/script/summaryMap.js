/*
 * @author rothrob
 * @date   09/24/2014
 * @desc   This contains
 */
var map, boundaries = [];
	
var initialize = function() {
	
	$("#collectorStatus").hide();
	
    var styles = [
      {
        stylers: [
          { hue: "#00ffdd" },
          { saturation: -100 }
        ]
      },{
        featureType: "road",
        elementType: "geometry",
        stylers: [
          { lightness: 20 },
          { visibility: "simplified" }
        ]
      },{
        featureType: "road",
        elementType: "labels",
        stylers: [
          { visibility: "on" }
        ]
      }
    ];

    var styledMap = new google.maps.StyledMapType(styles, {name: "Styled Map"});

    var mapDiv = document.getElementById('map-canvas');

    //create the alert/warning icon for collection status
    //display marker on the map
	var imgFlag = new Image(32, 32);
    imgFlag.src = "image/datamap-warning-marker.png";
    
    map = new google.maps.Map(mapDiv, {
      center: new google.maps.LatLng(34, -95),
      zoom: 4,
      mapTypeIds: [ google.maps.MapTypeId.ROADMAP, 'map_style' ] 
    });
    
    map.mapTypes.set('map_style', styledMap);
    map.setMapTypeId('map_style');

	//start/show loader.gif
	$('#loading').fadeIn('slow');
	window.states = new Array();
	
	$.getJSON('resources/boundary/states', function(response) {
		var State = {};
		var States = [];
		var stateIndex = 0;
		response = response.dataMapState;
		
		var collectorStatus = function(areaName, contributors) {
			//build the contibutor information box (lower right box)
			var sCollectionStatus = "<b>" + areaName + "</b><br><ul>";
			
			$("#collectorStatus").height("80px").css({
				top: "-142px"
			});
			
			if (contributors) {
				//This isn't pointless.  Easier to wrap a single instance into an array and just have one processing loop.
				var tempContributors = new Array();
				
 				if ($.isArray(contributors)) {
 					tempContributors = contributors;
				} else {
 					tempContributors.push(contributors);
				}

				if (tempContributors.length === 2) {
					$("#collectorStatus").height("124px").css({
	    				top: "-186px"
	 				});
				} else if (tempContributors.length > 2) {
					$("#collectorStatus").height("168px").css({
	    				top: "-230px"
	 				});
				}
				
				for (var c = 0, len = tempContributors.length; c < len; c++) {
					if (tempContributors[c]) {
						if (tempContributors[c].collectionStatus) {
							sCollectionStatus = sCollectionStatus + "<li>" +
								tempContributors[c].agency + " - <i>" + 
								tempContributors[c].collectionStatus.statistics + "</i></li>";
						} else {
							sCollectionStatus = sCollectionStatus + "<li>" +
								tempContributors[c].agency + " - <i>No Current Collection Status Data</i></li>";
						}
					} else {
						sCollectionStatus = sCollectionStatus + 
						tempContributors.agency + " - <i>No Current Collection Status Data</i>";
					}
				}
			} else {
				sCollectionStatus = sCollectionStatus + "<i>No Current Collection Status Data</i>";
			}//if else this.contributors end
			$("#collectorStatus").html(sCollectionStatus);
			$("#collectorStatus").show();
		};//function collectorStatus()
		
		// Get the boundaries from the boundary webservice
        $.get('us-states-polygon-kml.xml', function(data) {
			var stateColors = ['#FF8822', '#7722FF', '#555555'];
			//variable for zIndex for all segments which increments
			//and finally becomes the highest z-index available
			var zIndexHigh = 1,
				tempZIndex = null,
				tempColor = null;
			
			//traverse through the XML file for each state
			//and build the polygon with the points
			$(data).find('state').each(function() {
				var points = this.getElementsByTagName('point');
				var arrPoints = [];
				var code = this.getAttribute('code');
				var name = this.getAttribute('name');
				var color = null,
					link = null,
					contributors = null;
				//this is the accessible dropdown list @rothrob
				var statesList = $('#statesList');

				statesList.append('<option data-state="" value="" selected>Select a state...</option>');
				
				//traverse through JSON response and cache
				//data we need for this state, NOT sure
				//if caching to an Object for all states
				//first is a better logic than this
				for(x in response) {
					if(code == response[x].postalCode) {
						color = response[x].dataType;
						contributors = response[x].contributors;
						if(contributors) {
							var tempContrib = new Array();
			 				if ($.isArray(contributors)) {
			 					tempContrib = contributors;
			 				} else {
			 					tempContrib.push(contributors);
			 				}
							for(var y = 0, len = tempContrib.length; y < len; y++) {
								if(tempContrib[y].collectionStatus) {
									if(tempContrib[y].collectionStatus.status != 0 && admin==1) {
										new google.maps.Marker({
											anchorPoint: new google.maps.Point(1, 1),
											position: new google.maps.LatLng(response[x].latitude, response[x].longitude),
											icon: imgFlag.src,
											map: map
										});
										break;
									}//if
								}//if collectionStatus
							}//for
						}//if contributors
						link = '?lat='+ parseFloat(response[x].latitude) +'&lon='+ parseFloat(response[x].longitude) +'&zoom='+ parseInt(response[x].zoom);
					}//if
				}//for x in response
				
				//create a State object @rothrob
				State = {
					name: name,
					contributor: contributors,
					link: link
				};
				//populate an array of states
				States.push(State);

				statesList.append('<option data-state="' + stateIndex++ + '">' + name + '</option>');
				
				//build the Polygon "paths" using the points and
				//building the Array to be used.
				for(i=0, len=points.length; i < len; i++) {
					arrPoints[i] = new google.maps.LatLng(parseFloat(points[i].getAttribute('lat')), parseFloat(points[i].getAttribute('lng')));
				}
				//create the gmap overlay
	            boundary = new google.maps.Polygon({
					paths: arrPoints,
					strokeColor: '#888',
					strokeColor: '#555',
					strokeOpacity: 1,
					strokeWeight: 1,
					fillColor: stateColors[color],
					fillOpacity: 0.4,
					zIndex : zIndexHigh++, //set zIndex and increment it
					areaName : name, //state name e.g. Alabama
					contributors : contributors //contributors data from the response
				});
	            
				google.maps.event.addListener(boundary, 'mouseover', function() {
					tempZIndex = this.zIndex;
					tempColor = this.fillColor;
					this.setOptions({
						fillOpacity : 0.4,
						fillColor: "#00FF00",
						transition: "fillColor 0.5s ease",
						zIndex : zIndexHigh + 1, //set the z-index to the highest z-index available plus 1 for it to be the topmost segment on mouseover
						strokeWeight : 2
					});
					
					collectorStatus(this.areaName, this.contributors);							

				});//event mouseover end
				
	    		//set mouseout event handler, this is what makes the states
	    		//change back to it's original color upon mouseout
				google.maps.event.addListener(boundary, 'mouseout', function() {
					this.setOptions({
						fillOpacity : 0.4,
						fillColor: tempColor,
						zIndex : tempZIndex, //set the z-index back to it's original set upon mouseover earlier
						strokeWeight : 1
					});
				});
	    		
				//click event handler, this builds the link to the particular
				//state in wdeMap.jsp when clicking on it's respective state in
				//summaryMap.jsp
				google.maps.event.addListener(boundary, "click", function() {
					var urlLink = window.location.protocol + "//"
							+ window.location.host + "/wdeMap.jsp" + link;
					window.location = urlLink;
				});
	            
				boundaries.push(boundary);
				boundary.setMap(map);
				
			});//$.each points end
    	});//get XML file end
		
        //@rothrob
        //This is the Map Dropdown list element for accessibility
		$('#statesList')
        //On change is the event that triggers the Contributor box (lower right of map)
        .on('change', function(evt) {
				//if (evt.target.val() == '')
				//	return;
			evt.stopPropagation();
			evt.preventDefault();
			var state = $(evt.target.options[evt.target.selectedIndex]).data('state');
			collectorStatus(States[state].name, States[state].contributor);
		})
		//This adds the Contributor function on focus which fixes the event for the
		//very first option in the list which is Alaska, without this the Contributor
		//box will not show initially.
		.on('focus', function( evt ){
			var state = $(evt.target.options[evt.target.selectedIndex]).data('state');
			collectorStatus(States[state].name, States[state].contributor);
		})
        //This trigger the loading of the link for the state selected in the box
        .on('keydown', function( evt ) {
        	if( evt.keyCode === 16 ) {
				var state = $(evt.target.options[evt.target.selectedIndex]).data('state');
 				window.location = window.location.protocol + "//" + window.location.host + "/wdeMap.jsp" + States[state].link;
        		
        	}
        });
        
	});//getJSON request end
    
    $('#loading').fadeOut('slow');

};//initialize()
		
// 		var parseXml;
		
// 		if (window.DOMParser) {
// 		    parseXml = function(xmlStr) {
// 		        return ( new window.DOMParser() ).parseFromString(xmlStr, "text/xml");
// 		    };
// 		} else if (typeof window.ActiveXObject != "undefined" && new window.ActiveXObject("Microsoft.XMLDOM")) {
// 		    parseXml = function(xmlStr) {
// 		        var xmlDoc = new window.ActiveXObject("Microsoft.XMLDOM");
// 		        xmlDoc.async = "false";
// 		        xmlDoc.loadXML(xmlStr);
// 		        return xmlDoc;
// 		    };
// 		} else {
// 		    parseXml = function() { return null; };
// 		}
	
// 		$(document).ready( function() {
// 			setTimeout( function() {
// 				initialize();
// 			}, 500);
// 		});
// Initialize Google Map
google.maps.event.addDomListener(window, 'load', initialize);

$('#map-canvas').keyup(function(event) {
    event.preventDefault();
    event.stopPropagation();
    var o = 128; // half a tile's width 
    switch(event.which) {
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
        case 189: // -
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

var mapListHTML = 	'<p>When the state list dropdown box is in focus, ' + 
					'you can use the Up and Down arrow keys to traverse the list and select a state. ' +
					'You can type the first few characters of a state to quickly jump to that state. ' +
					'You can also gently press the spacebar to display the dropdown list. ' +
					'Once a state is selected, press the shift key to drill down to the map of that state.</p>';

$(document).ready( function() {
	$('#accessibility_dialog').append( mapListHTML );
});