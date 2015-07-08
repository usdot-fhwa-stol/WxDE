function onLoad()
{
    CreateClock(false);

    // ToggleMessageDisplay(false);
    var oXmlRequest = new XmlRequest();
    oXmlRequest.getXml("listContributors.jsp", cbGetContribs);
}


function cbGetContribs(oXml, oText)
{
    var oContribs = oXml.documentElement.getElementsByTagName("contributor");
    var oSelect = document.getElementById("listAll");
    
    var oRow;
    for (var i = 0; i < oContribs.length; i++)
    {
        oRow = oContribs[i];
        ListboxInsertItem(oSelect, oRow.getAttribute("name"), oRow.getAttribute("id"));
    }
}


function Add()
{
    var oListAll = document.getElementById("listAll");
    var oListSel = document.getElementById("listSel");
    ListboxTossSelected(oListAll, oListSel);
}


function Remove()
{
    var oListAll = document.getElementById("listAll");
    var oListSel = document.getElementById("listSel");
    ListboxTossSelected(oListSel, oListAll);
    ListboxRemoveSelected(oListSel);
}

function Validate()
{
    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";
    
    ToggleMessageDisplay(false);

    var sContributors = "";
    var oListSel = document.getElementById("listSel");
    
    var nIndex = oListSel.childNodes.length;
    while (nIndex-- > 0)
        sContributors += oListSel.childNodes.item(nIndex).value + ",";

    var oContributors = document.getElementById("contributors");
    oContributors.value = sContributors.substring(0, sContributors.length - 1);

    if (oContributors.value.length == 0)
    {
        oStatusMessage.nodeValue = "Please choose at least one contributor to use as a filter.";
        ToggleMessageDisplay(true);
    }
    else
    {
        document.contributors = oContributors.value;
        document.forms[0].submit();
    }
}


