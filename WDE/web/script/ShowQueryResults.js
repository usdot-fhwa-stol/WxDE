// global variables
var m_oClock = null;

function onLoad()
{
    // Set the dimensions of the resultsArea viewport.
    var nWidth = GetWinDimension("width");
    var nHeight = GetWinDimension("height");
    var oDivViewport = document.getElementById("resultsArea");
    var oDivFrame = document.getElementById("resultsFrame");
    var oDivSidebar = document.getElementById("sidebar");
    
    // oDivViewport.style.width = (nWidth - oDivSidebar.clientWidth - 20) + "px";
    // oDivViewport.style.height = (nHeight * 0.99) + "px";
    
    oDivFrame.style.width = (nWidth - oDivSidebar.clientWidth - 15) + "px";
    oDivFrame.style.height = (nHeight * 0.98) + "px";
    
    oDivFrame.style.display = "block";
    
    // Create the text node for the UTC time display.
    m_oClock = document.getElementById("timeUTC");
    m_oClock.appendChild(document.createTextNode(""));
    StartMapClock();
}


function StartMapClock()
{
    m_oClock.firstChild.data = "Time: " + FormatCurrentTime();
    
    // Update the clock every 60 seconds, at the top of the minute.
    setTimeout(StartMapClock, (1000 * (60 - new Date().getSeconds())));
}

