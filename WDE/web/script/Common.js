var m_oClockShadow;

function CreateClock()
{
    // Create the text node for the UTC time display.
    var oClock = document.getElementById("timeUTC");
    oClock.appendChild(document.createTextNode(""));

    m_oClockShadow = document.createElement("div");
    m_oClockShadow.id = "timeUTCShadow";
    m_oClockShadow.appendChild(document.createTextNode(""));

    // Get the current color for the clock text.
    // NOTE: IE is different than Firefox.
    var sClockColor;
    if (oClock.currentStyle)
    {
        // IE stuff
        sClockColor = oClock.currentStyle["color"];
        if (sClockColor.toUpperCase() == "#FFFFFF")
        {
            sClockColor = "#000000";
        }
        else
        {
            sClockColor = "#FFFFFF";
        }
    }
    else if (window.getComputedStyle)
    {
        // FF stuff
        sClockColor = document.defaultView.getComputedStyle(oClock, null).getPropertyValue("color");
        if (sClockColor == "rgb(255, 255, 255)")
        {
            sClockColor = "#000000";
        }
        else
        {
            sClockColor = "FFFFFF";
        }
    }

    m_oClockShadow.style.color = sClockColor;
    
    var oTitleArea = document.getElementById("titleArea");
    //oTitleArea.appendChild(m_oClockShadow);
    
    StartClock();
}


function FormatCurrentTime()
{
    var oDate = new Date();
    
    // Format the timestamp in this format
    // 2007-05-01 16:30
    var sMonth = oDate.getUTCMonth() + 1;
    if (sMonth < 10)
    {
        sMonth = "0" + sMonth;
    }
    
    var sDate = oDate.getUTCDate();
    if (sDate < 10)
    {
        sDate = "0" + sDate;
    }
    
    var sHours = oDate.getUTCHours();
    if (sHours < 10)
    {
        sHours = "0" + sHours;
    }
    
    var sMinutes = oDate.getUTCMinutes();
    if (sMinutes < 10)
    {
        sMinutes = "0" + sMinutes;
    }
    
    return(oDate.getUTCFullYear() + "-" + sMonth + "-" + sDate + " " +
           sHours + ":" + sMinutes + " UTC");
}


function StartClock(bShowLabel)
{
    var oClock = document.getElementById("timeUTC");
    var sTime = FormatCurrentTime();
    
    oClock.firstChild.data = sTime;
    m_oClockShadow.firstChild.data = sTime;

    // Update the clock every 60 seconds, at the top of the minute.
    setTimeout(StartClock, (1000 * (60 - new Date().getSeconds())));
}


//-----------------------------------------------------------
//-----------------------------------------------------------
function ToggleMessageDisplay(bShowMessage)
{
    var oMessageArea = document.getElementById("statusMessage");
    if (bShowMessage)
        oMessageArea.style.display = "block";
    else
        oMessageArea.style.display = "none";
}


//-----------------------------------------------------------
// NumbersOnly()
//      Ensures the user can only enter numbers.
//      Used for the onKeyPress callback function in an HTML file.
//-----------------------------------------------------------
function NumbersOnly(myfield, e)
{
    var key;
    var keychar;

    if (window.event)
        key = window.event.keyCode;
    else if (e)
        key = e.which;
    else
        return true;

    // Calls the appropriate function if the user hits Enter
    keychar = String.fromCharCode(key);

    // Control keys
    if ((key==null) || (key==0) || (key==8) || (key==9) || (key==27) )
        return true;

    // Allowable characters
    else if ((("0123456789-.").indexOf(keychar) > -1))
        return true;

    else
        return false;
}


//-----------------------------------------------------------
// DigitsOnly()
//      Ensures the user can only enter numbers.
//      Used for the onKeyPress callback function in an HTML file.
//      NOTE: This is the same as NumbersOnly(), except it doesn't allow
//      a decimal or a minus sign.
//-----------------------------------------------------------
function DigitsOnly(myfield, e)
{
    var key;
    var keychar;

    if (window.event)
        key = window.event.keyCode;
    else if (e)
        key = e.which;
    else
        return true;

    // Calls the appropriate function if the user hits Enter
    keychar = String.fromCharCode(key);

    // Control keys
    if ((key==null) || (key==0) || (key==8) || (key==9) || (key==27) )
        return true;

    // Allowable characters
    else if ((("0123456789").indexOf(keychar) > -1))
        return true;

    else
        return false;
}


//-----------------------------------------------------------
//-----------------------------------------------------------
function ValidateByChars(sValue, sValidChars)
{
    // check for invalid characters in the source string
    var bPass = true;
    var nIndex = sValue.length;

    while (nIndex-- > 0 && bPass)
        bPass = (sValidChars.indexOf(sValue.charAt(nIndex)) >= 0);
    
    // if the loop gets all the way through the string it will pass
    return bPass;
}


function ValidateDigits(sValue)
{
  return ValidateByChars(sValue, "0123456789");
}


//-----------------------------------------------------------
//-----------------------------------------------------------
function ValidateNumbers(sValue)
{
    if (!ValidateByChars(sValue, "0123456789-."))
        return false;

    // count the number of "-"
    var nHyphenCount = 0;
    var nIndex = sValue.length;
    while (nIndex-- > 0)
    {
        if (sValue.charAt(nIndex) == '-')
            nHyphenCount++;
    }
  
    // there must be fewer than two "-"
    if (nHyphenCount > 1)
        return false;
    
    // when there is a "-" then it must be the first character
    if (nHyphenCount == 1 && sValue.charAt(0) != '-')
        return false;


    // count the number of "."
    var nPeriodCount = 0;
    nIndex = sValue.length;
    while (nIndex-- > 0)
    {
        if (sValue.charAt(nIndex) == '.')
            nPeriodCount++;
    }
  
    // there must be fewer than two "."
    if (nPeriodCount > 1)
        return false;
    
    if (nPeriodCount == 1)
    {
        // when there is a "." it cannot be the first character in a positive number
        if (sValue.charAt(0) == '.')
            return false;
      
        // "." cannot be the second character in a negative number
        if (nHyphenCount == 1 && sValue.charAt(1) == '.')
            return false;
      
        // "." also cannot be the last character in any number
    }
    
    return true;
}


//-----------------------------------------------------------
// PadString()
// Pads a string with a given character and returns a string of the desired length;
//-----------------------------------------------------------
function PadString(sInput, sPadChar, nLength)
{
    var s = "" + sInput;
    while (s.length < nLength)
    {
        s = sPadChar + s;
    }
    return(s);
}


//-----------------------------------------------------------
// Debug()
// Displays an alert box with "DEBUG" at the top.
// This allows a differentiation between an alert box for real messages for the user
// and debug messages.
//-----------------------------------------------------------
function Debug(sText)
{
    alert("DEBUG:\n" + sText);
}


//-----------------------------------------------------------
// TestCoordinate()
// Checks the value passed in for validity with regards to the minimum and maximum
// for that type of coordinate.
// If the value passed in fails the tests, then the status message is displayed.
//-----------------------------------------------------------
function TestCoordinate(sValue, sLabel, dMin, dMax, oStatusMessage)
{
    if (sValue.length == 0)
    {
        oStatusMessage.nodeValue = sLabel + " cannot be blank";
        return false;
    }
  
    // If there is a decimal point, make sure there is at least one digit to the left of it.
    var nIndex = sValue.indexOf(".");
    if (nIndex >= 0)
    {
        if (nIndex == 0)
            sValue = "0" + sValue;
        else if (nIndex == 1 && sValue.charAt(0) == "-")
            sValue = "-0" + sValue.substring(1);
    }
        
    if (!ValidateNumbers(sValue))
    {
        oStatusMessage.nodeValue = sLabel + " is not a valid number";
        return false;
    }
  
    if (sValue < dMin || sValue > dMax)
    {
        oStatusMessage.nodeValue = sLabel + " must be in the range of " + dMin + " to " + dMax;
        return false;
    }
  
    return true;
}


//-----------------------------------------------------
// DoEnterKey()
//      Performs the desired action when Enter is pressed
//      while in a text field.
//-----------------------------------------------------
function DoEnterKey(myfield, e, oFunctionToCall)
{
    var key;
    var keychar;

    if (window.event)
        key = window.event.keyCode;
    else if (e)
        key = e.which;
    else
        return true;

    // Calls the appropriate function if the user hits enter   
    if (key == 13) 
    {
        oFunctionToCall();
    }
}

function GetWinDimension(sDimension)
{
    var myWidth = 0, myHeight = 0;
    
    if (typeof(window.innerWidth) == 'number')
    {
        //Non-IE
        myWidth = window.innerWidth;
        myHeight = window.innerHeight;
    }
    else if (document.documentElement &&
            (document.documentElement.clientWidth || document.documentElement.clientHeight))
    {
        //IE 6+ in 'standards compliant mode'
        myWidth = document.documentElement.clientWidth;
        myHeight = document.documentElement.clientHeight;
    }
    else if (document.body &&
            (document.body.clientWidth || document.body.clientHeight))
    {
        //IE 4 compatible
        myWidth = document.body.clientWidth;
        myHeight = document.body.clientHeight;
    }
    
    if (sDimension == "width")
        return(myWidth);
    else
        return(myHeight);
}


