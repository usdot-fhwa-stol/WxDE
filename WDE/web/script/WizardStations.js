
var m_oContribList = null;
var m_sSelectedContribs = "";

// onLoad()
// Starts the action for this page.
// Note: The sSelectedContribs is a CSV string containing the contributors selected
//       on the Contributors page.
function onLoad(sSelectedContribs)
{
    CreateClock(false);

    // Store the selected contributors for later.
    m_sSelectedContribs = sSelectedContribs;
    
    var oXmlRequest = new XmlRequest();
    oXmlRequest.getXml("listContributors.jsp", cbGetContribs);
}


// cbGetContribs()
// Get the list of contributors and save it until we fill the station listbox.
function cbGetContribs(oXml, oText)
{
    // Save the list of contributors known to the system for later use.
    m_oContribList = oXml.documentElement.getElementsByTagName("contributor");
    
    // Retrieve the list of stations.
    var oXmlRequest = new XmlRequest();

    // Parse the list of selected contributors and pass each one as a parameter.
    var sContribs = m_sSelectedContribs.split(",");
    for (var i = 0; i < sContribs.length; i++)
    {
        oXmlRequest.addParameter("contribId", sContribs[i]);
    }

    oXmlRequest.getXml("listStations.jsp", cbGetStations);
}


function cbGetStations(oXml, oText)
{
    var oStations = oXml.documentElement.getElementsByTagName("station");
    var oSelect = document.getElementById("listAll");
    
    // The list of stations is returned grouped by contribId.
    // Search for the contribId in the list of contributors so we can attach
    // the contributor's name to the string being inserted into the listbox.
    var sContribName = "";
    var nContribId = -1;
    
    var oRow;
    for (var i = 0; i < oStations.length; i++)
    {
        oRow = oStations[i];
        if (nContribId != oRow.getAttribute("contribId"))
        {
            nContribId = oRow.getAttribute("contribId");
            sContribName = GetContribName(nContribId);
        }
        ListboxInsertItem(oSelect,
                          sContribName + ", " + oRow.getAttribute("category") + ", " + 
                          oRow.getAttribute("stationCode") + ", " + oRow.getAttribute("hasObs"), 
                          oRow.getAttribute("id"),
                          i == 0 ? true : false);
    }
}


function GetContribName(nContribId)
{
    for (var i = 0; i < m_oContribList.length; i++)
    {
        if (m_oContribList[i].getAttribute("id") == nContribId)
            return(m_oContribList[i].getAttribute("name"));
    }
    return("Unknown Contributor (" + nContribId + ")");
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

function Validate(e)
{

    var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
    oStatusMessage.nodeValue = "";

    var statusMessage = document.getElementById("statusMessage");
    
    var oListSel = document.getElementById("listSel"),
    	oListLength = oListSel.childNodes.length;
    
    var sStations = "";
    var nIndex = oListLength;
    
    while (nIndex-- > 0)
        sStations += oListSel.childNodes.item(nIndex).value + ",";

    var oStations = document.getElementById("stations");
    oStations.value = sStations.substring(0, sStations.length - 1);
    
    // Check if user selected at least 1 station
    if ( oListLength > 0 )
    	document.forms[0].submit();
    else {
    	statusMessage.innerHTML = "No station selected! You must select at least 1 station!";
    	statusMessage.style.display = "block";
    }
}


function PrevPage()
{
    document.location = "wizardContributor.jsp";
}
