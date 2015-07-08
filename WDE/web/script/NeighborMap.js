var m_nMinNeighbors = 2;
var m_nMargin = 16;

function onLoad()
{
    // Set the dimensions of the map viewport.
    var oDivMap = document.getElementById("map");
    oDivMap.style.height = (GetWinDimension("height") - m_nMargin) + "px";
    
    // Allow the browser window to get established before continuing.
    setTimeout(ContinueLoading, 100);
}

function ContinueLoading()
{
    m_oMap = new GMap2(document.getElementById("map"));
    m_oMap.addControl(new GLargeMapControl());

    // Load form defaults to center the map on the correct view
    loadDefaults(document.URL);
}


createIcon = function(sImage, nWidth, nHeight, nX, nY)
{
	var oIcon = new GIcon();
	oIcon.image = sImage;
	oIcon.iconSize = new GSize(nWidth, nHeight);
	oIcon.iconAnchor = new GPoint(nX, nY);

	return oIcon;
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
    var nDefaultZoom;
    
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
                nDefaultZoom = parseInt(sParam[1]);
            else if (sParam[0] == "neighbors")
                m_nMinNeighbors = parseInt(sParam[1]);
        }
    }
    
    m_oMap.setCenter(new GLatLng(lat, lng), nDefaultZoom);
    
		var oOptionsIQR = 
		{
			"icon": createIcon("image/green5.png", 9, 9, 4, 4),
			"clickable": false,
			"draggable": false, 
			"title": ""
		};

		var oOptionsBarnes = 
		{
			"icon": createIcon("image/yellow5.png", 9, 9, 4, 4),
			"clickable": false,
			"draggable": false, 
			"title": ""
		};

		var oOptionsNone = 
		{
			"icon": createIcon("image/red5.png", 9, 9, 4, 4),
			"clickable": false,
			"draggable": false, 
			"title": ""
		};

    for (var nIndex = 0; nIndex < m_oNeighbors.length; nIndex++)
    {
    	var oStation = m_oNeighbors[nIndex];
    	--oStation.ct;

    	var oOptions = oOptionsNone;
    	if (oStation.ct > 2)
    		oOptions = oOptionsBarnes;

    	if (oStation.ct > 4)
    		oOptions = oOptionsIQR;
    	
      oMarker = new GMarker(new GLatLng(oStation.lt, oStation.ln), oOptions);
			m_oMap.addOverlay(oMarker);
    }
}


window.onresize = function()
{
    document.getElementById("map").style.height = (GetWinDimension("height") - m_nMargin) + "px";
}
