

function onLoad()
{
    CreateClock(false);

    // Set the Output Format selection to CSV.
    var oSelect = document.getElementById("listFormat");
    ListboxSelectItem(oSelect, "CSV", false);

//    var oCode = document.getElementById("txtCaptcha");
//    oCode.focus();
}


function cbSave(oXml, sText)
{
    // The only response is the successful subscription identifier or the security code failure
    // Debug(sText);

    // Verify the security code is valid
/*    var oRows = oXml.documentElement.getElementsByTagName("code");

    if (oRows.length == 1 && oRows[0].getAttribute("valid") == 1)
    {*/
        // Now post the rest of the data and get the subscription results
        document.forms[0].submit();
        /*    }
    else
    {
        var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
        oStatusMessage.nodeValue = "The Security Code is incorrect";
        ToggleMessageDisplay(true);
    }*/
}
