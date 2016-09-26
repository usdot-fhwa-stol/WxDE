
function onLoad()
{
    CreateClock(false);

    // Clear out all the entry fields.
    document.getElementById("txtLat1").value = "";
    document.getElementById("txtLng1").value = "";
    document.getElementById("txtLat2").value = "";
    document.getElementById("txtLng2").value = "";

    ToggleMessageDisplay(false);
}

function Validate()
{
    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";
    
    ToggleMessageDisplay(false);

    var oRegion = document.getElementById("region");

    // Process the Bounding Box fields.
    var dLat1 = $("#txtLat1").val();
    var dLng1 = $("#txtLng1").val();
    var dLat2 = $("#txtLat2").val();
    var dLng2 = $("#txtLng2").val();

    if (TestCoordinate(dLat1, "Latitude 1", -85.0, 85.0, oStatusMessage) && 
        TestCoordinate(dLng1, "Longitude 1", -180.0, 180.0, oStatusMessage) && 
        TestCoordinate(dLat2, "Latitude 2", -85.0, 85.0, oStatusMessage) && 
        TestCoordinate(dLng2, "Longitude 2", -180.0, 180.0, oStatusMessage))
    {
        oRegion.value = dLat1 + "," + dLng1 + "," + dLat2 + "," + dLng2;
        document.forms[0].submit();
    } 
    else 
    {
        $('#statusMessage').show();
        ToggleMessageDisplay(true);
    }
}





