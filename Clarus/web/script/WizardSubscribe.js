

function onLoad()
{
    CreateClock(false);

    // Set the Output Format selection to CSV.
    var oSelect = document.getElementById("listFormat");
    ListboxSelectItem(oSelect, "CSV", false);

    var oCode = document.getElementById("txtCaptcha");
    oCode.focus();
}


function Validate()
{
    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";

    var oCode = document.getElementById("txtCaptcha");
    var oPassword = document.getElementById("txtPassword");

    // The secret code and password are required
    var sValue = oCode.value;
    if (sValue.length == 0)
    {
        oStatusMessage.nodeValue = "The Security Code cannot be blank";
        ToggleMessageDisplay(true);
        return;
    }
  
    if (sValue.length < 6)
    {
        oStatusMessage.nodeValue = "The Security Code must be 6 characters long";
        ToggleMessageDisplay(true);
        return;
    }
  
    if (!ValidateDigits(sValue))
    {
        oStatusMessage.nodeValue = "The Security Code must contain only digits";
        ToggleMessageDisplay(true);
        return;
    }

    sValue = oPassword.value;
    
    // Make sure the user meant to create a subscription without a password.
    if (sValue.length == 0)
    {
        var sMsg = "You are creating a subscription that can be viewed by everyone.\n\n" +
                   "If this is what you want, press OK. Otherwise, press Cancel to specify a password.";
        if (!window.confirm(sMsg))
        {
            return;
        }
    }
    
    if (sValue.length > 0)
    {
        // If there is a password, make sure it is at least 6 characters long.
        if (sValue.length < 6)
        {
            oStatusMessage.nodeValue = "The Password must contain at least 6 characters";
            ToggleMessageDisplay(true);
            return;
        }
        
        // Make sure the password has valid characters.
        if (!ValidateByChars(sValue, "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"))
        {
            oStatusMessage.nodeValue = "The Password is limited to letters and numbers only";
            ToggleMessageDisplay(true);
            return;
        }
    }
  
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

    // Construct the server message when all the validations succeed
    var oXmlRequest = new XmlRequest();

    oXmlRequest.addParameter("secret", oPassword.value);
    oXmlRequest.addParameter("code", oCode.value);
    oXmlRequest.addParameter("format", ListboxSelectedText(document.getElementById("listFormat")));
    oXmlRequest.addParameter("cycle", ListboxSelectedText(document.getElementById("listCycle")));
    oXmlRequest.addParameter("contactEmail", document.getElementById("contactEmail").value);
    oXmlRequest.addParameter("contactName", document.getElementById("contactName").value);

    // Execute the subscription creation and process the response.
    // Need to get the session id from the form for this operation
    var sSessionId = document.getElementById("jsessionid").value;
    oXmlRequest.getXml("verifySecurityCode.jsp;jsessionid=" + sSessionId, cbSave);
}


function cbSave(oXml, sText)
{
    // The only response is the successful subscription identifier or the security code failure
    // Debug(sText);

    // Verify the security code is valid
    var oRows = oXml.documentElement.getElementsByTagName("code");

    if (oRows.length == 1 && oRows[0].getAttribute("valid") == 1)
    {
        // Now post the rest of the data and get the subscription results
        document.forms[0].submit();
    }
    else
    {
        var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
        oStatusMessage.nodeValue = "The Security Code is incorrect";
        ToggleMessageDisplay(true);
    }
}
