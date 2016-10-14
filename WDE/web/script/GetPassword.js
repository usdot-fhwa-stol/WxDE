
function onLoad()
{
    var oPassword = document.getElementById("txtPassword");
    oPassword.focus();
}

function Validate()
{
    // Set up the status message output
    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";
    
    var oPassword = document.getElementById("txtPassword");

    var oXmlRequest = new XmlRequest();
    oXmlRequest.addParameter("secretAttempt", oPassword.value);
    oXmlRequest.getXml("setPassword.jsp?" + csrf_nonce_param, cbSetPassword);
}


function cbSetPassword(oXml, oText)
{
    var oResult = oXml.documentElement.getElementsByTagName("results");
    if (oResult[0].getAttribute("passwordMatches") == "true")
    {
        document.location = document.URL;
    }
    else
    {
        var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
        oStatusMessage.nodeValue = "Invalid password.";
        ToggleMessageDisplay(true);
        return;
    }
}