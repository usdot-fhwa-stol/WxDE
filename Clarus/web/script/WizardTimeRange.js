var m_oTimestamps = [];
var MAX_HOURS = 168;        // 168 hours = 7 days

function onLoad()
{
    CreateClock(false);

    GenerateTimestamps();
    
    // Fill the Start Time listbox starting with 1 hour before the current hour.
    var oSelect = document.getElementById("listTimeStart");
    for (var i = 0; i < MAX_HOURS; i++)
    {
        ListboxInsertItem(oSelect, FormatTimestamp(m_oTimestamps[i]), m_oTimestamps[i]);
    }
    
    // Fill the End Time listbox starting with the current hour and ending
    // one hour before the last timestamp.
//    oSelect = document.getElementById("listTimeEnd");
//    for (var i = 0; i < MAX_HOURS - 1; i++)
//    {
//        ListboxInsertItem(oSelect, FormatTimestamp(m_oTimestamps[i]), m_oTimestamps[i]);
//    }
    
    // Set the Output Format selection to CSV.
    oSelect = document.getElementById("listFormat");
    ListboxSelectItem(oSelect, "CSV", false);
}


// FormatTimestamp()
// Returns a string representing the date/time for the UTC timestamp.
function FormatTimestamp(lTimestamp)
{
    var oDate = new Date(lTimestamp);
    return(oDate.getUTCFullYear() + "-" +
           PadString(oDate.getUTCMonth() + 1, "0", 2) + "-" +
           PadString(oDate.getUTCDate(), "0", 2) + " " +
           PadString(oDate.getUTCHours(), "0", 2) + ":" +
           PadString(oDate.getUTCMinutes(), "0", 2));
}



// GenerateTimestamps()
// Generates an array of UTC timestamps for every hour
// starting at the current hour and going back for the specified number of hours.
function GenerateTimestamps()
{
    // Get the current hour.
    var oDate = new Date();
    var lNow = oDate.getTime();
    var lCurrentHour = parseInt(lNow / 3600000);
//    lCurrentHour++;
    lCurrentHour *= 3600000;
    
    for (var i = 0; i < MAX_HOURS; i++)
    {
        m_oTimestamps[i] = lCurrentHour;
        lCurrentHour -= 3600000;
    }
}


function Validate()
{
    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";

    // Retrieve the specified timestamps.
    var lStart = document.getElementById("listTimeStart").value;
//    var lEnd = document.getElementById("listTimeEnd").value;
    var lEnd = lStart - 3600000;
    
    var sDebugStart = FormatTimestamp(parseInt(lStart));
    var sDebugEnd = FormatTimestamp(parseInt(lEnd));
  
    if (lStart != lEnd)
    {
        var oTimeRange = document.getElementById("timeRange");
        oTimeRange.value = lStart + "," + lEnd;
        
        // Get the output format selection.
        var oFormat = document.getElementById("format");
        oFormat.value = ListboxSelectedText(document.getElementById("listFormat")).toUpperCase();

/***
        if (!window.confirm('The user assumes the entire risk related to the use of data contained in the Clarus System.\n' +
                            'The U.S. Department of Transportation (DOT) and its contributors are providing this data "as is,"\n' +
                            'and the U.S. DOT and its contributors disclaim any and all warranties, whether express or\n' +
                            'implied, including (without limitation) any implied warranties of merchantability or fitness\n' +
                            'for a particular purpose. In no event will the U.S. DOT or its contributors be liable to you\n' +
                            'or to any third party for any direct, indirect, incidental, consequential, special or exemplary\n' +
                            'damages or lost profit resulting from any use or misuse of this data.\n\n' +
                            'Press OK if you agree and wish to complete the query.'))
        {
            return;
        }
***/
        
        document.forms[0].submit();
    }
    else
    {
        oStatusMessage.nodeValue = "The Start and End times must be different.";
        ToggleMessageDisplay(true);
    }
}
