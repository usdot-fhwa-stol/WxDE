// COPYRIGHT (c) 2006 Mixon/Hill, Inc., ALL RIGHTS RESERVED.


//-----------------------------------------------------------
// KeyValue()
//      This is a helper object for XmlRequest.
//      The KeyValue object stores a key/value pair, suitable
//      for passing off to a JSP page (e.g., "name=Bob").
//      The XmlRequest object will create a new KeyValue and
//      store it in an array.
//-----------------------------------------------------------
function KeyValue(sKey, sValue)
{
    this.sKey = sKey;
    this.sValue = sValue;
  
    this.toString = function()
    {
        return this.sKey + "=" + this.sValue;
    }
  
    this.isEqual = function(sKey)
    {
        return(this.sKey == sKey);
    }
}


UnescapeTheText = function(oNode)
{
    var oText = oNode.data;
    if (oText != null)
    {
        oText = unescape(oText);
        // alert(oText);
    }
    
    if (oNode.attributes != null)
    {
        for (var i = 0; i < oNode.attributes.length; i++)
        {
            oNode.attributes[i].nodeValue = unescape(oNode.attributes[i].nodeValue);
            // alert(oNode.attributes[i].nodeName + " = " + oNode.attributes[i].nodeValue);
        }
    }

    if (oNode.childNodes != null)
    {
        for (var i = 0; i < oNode.childNodes.length; i++)
        {
            UnescapeTheText(oNode.childNodes[i]);
        }
    }
}


//-----------------------------------------------------------
// XmlRequest()
//      The XmlRequest object makes a browser-independent
//      call to a web page.
//-----------------------------------------------------------
function XmlRequest()
{
    this.bExists = false;
    this.oXmlRequest = null;
    this.sessionId = null;
    this.aKeyValues = [];
    this.sDebugAreaId = "";
    this.bDebug = false;
    
    // try to initialize the Microsoft XML request object  
    try
    {
        this.oXmlRequest = new ActiveXObject("Microsoft.XMLHTTP");
        this.bExists = true;
    }
    catch (oMicrosoftError)
    {
    }
  
    // now try to initialize the Firefox XML request object  
    if (!this.bExists)
    {
        try
        {
            this.oXmlRequest = new XMLHttpRequest();
            this.bExists = true;
        }
        catch (oFirefoxError)
        {
        }
    }

    
    this.getCookie = function(name) 
    {
        var dc = document.cookie;
        var prefix = name + "=";
        var begin = dc.indexOf("; " + prefix);
        if (begin == -1)
        {
            begin = dc.indexOf(prefix);
            if (begin != 0) return null;
        }
        else begin += 2;
        var end = document.cookie.indexOf(";", begin);
        if (end == -1) end = dc.length;
        return unescape(dc.substring(begin + prefix.length, end));
    }
    
    
    // Allow the user to turn on debugging and to optionally specify
    // a field into which to dump the parameters.
    this.Debug = function(sDisplayFieldId)
    {
        this.bDebug = true;
        if (sDisplayFieldId != "")
            this.sDebugAreaId = sDisplayFieldId;
    }


    // Add a parameter to the KeyValue object.
    this.addParameter = function(sKey, sValue)
    {
        var sEscapedValue = escape(sValue);
        this.aKeyValues.push(new KeyValue(sKey, sEscapedValue));
    }
    
    this.getParams = function()
    {
        var sParams = "";

        for (var i = 0; i < this.aKeyValues.length; i++)
        {
            if (i != 0)
                sParams += "&";

            sParams += this.aKeyValues[i].toString();
        }
        return(sParams);
    }
    

    // declare xml request method
    this.getXml = function(sUrl, cbCallback, oRequestor)
    {
        var sBaseUrl = sUrl;
        
        var oXmlRequest = this.oXmlRequest;
        // if no request object was created, don't try to use it
        if (this.bExists)
        {
            // insert the session id into the url after the .jsp
//            if (this.sessionId != null)
//            {
//                // for .NET the (S(uuid)) is prepended to <<file>>.jsp
//                sUrl = "(S(" + this.sessionId + "))/" + sUrl;
//            }

            // retrieve the xml from the provided url and save it
            // alert("DEBUG: \ncalling URL=" + sUrl + "\ndocument.cookie=" + document.cookie);
            oXmlRequest.open("POST", sUrl, true);

            // In-line callback function.
            oXmlRequest.onreadystatechange = function()
            {
                if (oXmlRequest.readyState == 4 )
                {
                    if (oXmlRequest.responseXML != null)
                    {
                        if (oXmlRequest.responseXML.documentElement == null)
                        {
                            // alert("xml.js: Database call returned an empty documentElement." +
                            //       "\nURL = " + sUrl);
                            //var iStart = oXmlRequest.responseText.indexOf("[");
                            //var iEnd = oXmlRequest.responseText.length;
                            //alert("xml.js: Database call returned an empty documentElement." +
                            //      "\nURL = " + sBaseUrl + "\nHTTP status = " + oXmlRequest.status +
                            //      "\nResponse Headers:\n" + oXmlRequest.getAllResponseHeaders() +
                            //      "\n-------------------\n" + oXmlRequest.responseText.substring(iStart, iEnd));
                            if (oXmlRequest.responseText != null && oXmlRequest.responseText.length > 0)
                                cbCallback(oXmlRequest.responseXML, oXmlRequest.responseText, oRequestor);
                        }
                        else
                        {
                            // save the session id in this object
//                            this.sessionId = oXmlRequest.responseXML.documentElement.getAttribute("sessionId");
                            
                            // copy the session id to a cookie for use by other pages
//                            document.cookie = "sessionId=" + this.sessionId;
                            
                            // Walk the DOM and unescape any escape characters.
                            UnescapeTheText(oXmlRequest.responseXML.documentElement);
                            
                            // send the xml document object model
                            cbCallback(oXmlRequest.responseXML, oXmlRequest.responseText, oRequestor);
                        }
                    }
                    else
                    {
                        // alert("xml.js: oXmlRequest.responseXml is null");
//                        alert("xml.js: Call to " + sBaseUrl + " returned an empty XML response:\n" + oXmlRequest.responseText);
                        // alert("DEBUG:\n" + oXmlRequest.getAllResponseHeaders());

                        // send the xml document object model
                        cbCallback(oXmlRequest.responseXML, oXmlRequest.responseText, oRequestor);
                    }
                }
            }

            // Build the parameter string, if there are any parameters.
            var sParams = this.getParams();
     
            oXmlRequest.setRequestHeader("Content-type","application/x-www-form-urlencoded");

            if (this.bDebug)
            {
                if (this.sDebugAreaId != "")
                    document.getElementById(this.sDebugAreaId).value = sParams;
                else
                    prompt("Query Parameters", sUrl + "?" + sParams);
            }
            
            oXmlRequest.send(sParams);
        }
    }

    // try to get the session id from a cookie
//    this.sessionId = this.getCookie("sessionId");
}

