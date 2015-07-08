
function onLoad()
{
    CreateClock(false);

    // Make Bounding Box the default.
    var oGeoType = document.getElementsByName("geoType");
    oGeoType[0].checked = true;
    
    // Clear out all the entry fields.
    document.getElementById("txtLat1").value = "";
    document.getElementById("txtLng1").value = "";
    document.getElementById("txtLat2").value = "";
    document.getElementById("txtLng2").value = "";
    document.getElementById("txtLat").value = "";
    document.getElementById("txtLng").value = "";
    document.getElementById("txtRadius").value = "";
    
    
    GeoTypeChanged();
    ToggleMessageDisplay(false);
}


function GeoTypeChanged(sLabelClicked)
{
    var oGeoType = document.getElementsByName("geoType");
    
    // If the label was clicked, then simulate the selection of the radio button.
    if (sLabelClicked != undefined)
    {
        if (sLabelClicked == "bb")
        {
            oGeoType[0].checked = true;
        }
        else
        {
            oGeoType[1].checked = true;
        }
    }
    
    if (oGeoType[0].checked)
    {
        // Display the Bounding Box fields and hide the Point/Radius fields.
        document.getElementById("tblBoundingBox").style.display = "block";
        document.getElementById("tblPointRadius").style.display = "none";
    }
    else
    {
        // Display the Point/Radius fields and hide the Bounding Box fields.
        document.getElementById("tblBoundingBox").style.display = "none";
        document.getElementById("tblPointRadius").style.display = "block";
    }
}


function Validate()
{
    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";
    
    ToggleMessageDisplay(false);

    
    var oGeoType = document.getElementsByName("geoType");

    if (oGeoType[0].checked)
    {
        // Process the Bounding Box fields.
        var dLat1 = document.getElementById("txtLat1").value;
        var dLng1 = document.getElementById("txtLng1").value;
        var dLat2 = document.getElementById("txtLat2").value;
        var dLng2 = document.getElementById("txtLng2").value;

        if (TestCoordinate(dLat1, "Latitude 1", -85.0, 85.0, oStatusMessage) && 
            TestCoordinate(dLng1, "Longitude 1", -180.0, 180.0, oStatusMessage) && 
            TestCoordinate(dLat2, "Latitude 2", -85.0, 85.0, oStatusMessage) && 
            TestCoordinate(dLng2, "Longitude 2", -180.0, 180.0, oStatusMessage))
        {
            var oRegion = document.getElementById("region");
            oRegion.value = dLat1 + "," + dLng1 + "," + dLat2 + "," + dLng2;
            document.forms[0].submit();
        }
        else
        {
            ToggleMessageDisplay(true);
        }
    }
    else
    {
        // Process the Point/Radius fields.
        var dLat = document.getElementById("txtLat").value;
        var dLng = document.getElementById("txtLng").value;
        var dRadius = document.getElementById("txtRadius").value;

        if (TestCoordinate(dLat, "Latitude", -85.0, 85.0, oStatusMessage) && 
            TestCoordinate(dLng, "Longitude", -180.0, 180.0, oStatusMessage) && 
            TestCoordinate(dRadius, "The Radius", 0.999, 5000.0, oStatusMessage))
        {
            var oRegion = document.getElementById("region");
            oRegion.value = dLat + "," + dLng + "," + dRadius;
            document.forms[0].submit();
        }
        else
        {
            ToggleMessageDisplay(true);
        }
    }
}


