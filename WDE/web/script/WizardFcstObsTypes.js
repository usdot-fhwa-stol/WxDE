function onLoad()
{
  CreateClock(false);

  // Turn off the Min/Max boxes.
  //ShowObsSpecificStuff(false);


  var oXmlRequest = new XmlRequest();
  oXmlRequest.getXml("/ListObsTypes", cbGetObsTypes);
}


function cbGetObsTypes(oXml, oText)
{
  var oObsTypes = oXml.documentElement.getElementsByTagName("obsType");
  var oSelect = document.getElementById("listObsTypes");

  var oRow;
  //oSelect.append('<option value="" selected>All Observations</option>');

  for (var i = 0; i < oObsTypes.length; i++)
  {
    oRow = oObsTypes[i];
    if (oRow.getAttribute("active") == 1)
    {
      ListboxInsertItem(oSelect, oRow.getAttribute("name"), oRow.getAttribute("id"), i == 0 ? true : false);
    }
  }
}

function Add()
{
  var oListAll = document.getElementById("listObsTypes");
  var oListSel = document.getElementById("listSelObsTypes");
  ListboxTossSelected(oListAll, oListSel);
}

function Remove()
{
  var oListAll = document.getElementById("listObsTypes");
  var oListSel = document.getElementById("listSelObsTypes");
  ListboxTossSelected(oListSel, oListAll);
  ListboxRemoveSelected(oListSel);
}

function RunQuery()
{
  document.forms[0].action = document.getElementById("runQueryUrl").value;
  Validate();
}

function Subscribe()
{
  document.forms[0].action = document.getElementById("subscribeUrl").value;
  Validate();
}

function Validate()
{
  // Set up the status message output
  var oStatusMessage = document.getElementById("statusMessage").childNodes.item(0);
  oStatusMessage.nodeValue = "";
  var oObsTypes = document.getElementById("obs");

  oObsTypes.value = "";

  // The remainder of the parameters are dependent on an observation type being selected
  var oObsTypeList = document.getElementById("listSelObsTypes");
  var nLength = oObsTypeList.length;

  // Index greater than zero skips the "no selection" option
  if (nLength > 0)
  {
    var sObs = "";

    for (var i = 0; i < nLength - 1; i++)
    {
      sObs += oObsTypeList.options[i].value;
      sObs += ",";
    }
    sObs += oObsTypeList.options[nLength - 1].value;

    oObsTypes.value = sObs;

    document.forms[0].submit();
  }
}







