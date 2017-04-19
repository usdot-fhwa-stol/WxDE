var m_oTestTable = null;
var m_oTestLabels = new Object();


function onLoad()
{
  // initialize the test presentation label lookup
  m_oTestLabels["wde.qchs.algo.Barnes"] = "Barnes Spatial";
  m_oTestLabels["wde.qchs.algo.ClimateRange"] = "Climate Range";
  m_oTestLabels["wde.qchs.algo.Complete"] = "Complete";
  m_oTestLabels["wde.qchs.algo.Dewpoint"] = "Dew Point";
  m_oTestLabels["wde.qchs.algo.IQR"] = "IQR";
  m_oTestLabels["wde.qchs.algo.LikeInstrument"] = "Like Instrument";
  m_oTestLabels["wde.qchs.algo.Maintenance"] = "Manual Flag";
  m_oTestLabels["wde.qchs.algo.Persistence"] = "Persistence";
  m_oTestLabels["wde.qchs.algo.PrecipAccum"] = "Precip Accum";
  m_oTestLabels["wde.qchs.algo.SensorRange"] = "Sensor Range";
  m_oTestLabels["wde.qchs.algo.Sounding"] = "Sea Level Pressure";
  m_oTestLabels["wde.qchs.algo.Step"] = "Step Test";

  CreateClock(false);

  // Turn off the Min/Max boxes.
  ShowObsSpecificStuff(false);

  m_oTestTable = document.getElementById("tblTests");

  var oXmlRequest = new XmlRequest();
  oXmlRequest.getXml("/ListObsTypes?" + csrf_nonce_param, cbGetObsTypes);
}


function cbGetObsTypes(oXml, oText)
{
  var oObsTypes = oXml.documentElement.getElementsByTagName("obsType");
  var oSelect = document.getElementById("listObsTypes");

  var oRow;
  ListboxInsertItem(oSelect, "All Observations", "", true);
  //oSelect.append('<option value="" selected>All Observations</option>');

  for (var i = 0; i < oObsTypes.length; i++)
  {
    oRow = oObsTypes[i];
    if (oRow.getAttribute("active") == 1)
    {
      ListboxInsertItem(oSelect, oRow.getAttribute("name"), oRow.getAttribute("id"));
    }
  }
}


function ObsTypeChanged()
{
  var oSelect = document.getElementById("listObsTypes");

  // Remove the Quality Check Tests table and all of its
  // elements.
  while (m_oTestTable.hasChildNodes())
  {
    m_oTestTable.removeChild(m_oTestTable.childNodes[0]);
  }

  // Clear out the Min/Max entry fields.
  var oTxtMin = document.getElementById("txtMin");
  var oTxtMax = document.getElementById("txtMax");
  oTxtMin.value = "";
  oTxtMax.value = "";


  // Always turn off the ObsType-specific elements if the
  // user has selected the "All Observations" item.
  if (oSelect.value == "")
  {
    ShowObsSpecificStuff(false);
  }
  else
  {
    // Turn on the ObsType-specific elements.
    ShowObsSpecificStuff(true);

    var oXmlRequest = new XmlRequest();
    oXmlRequest.addParameter("obsType", oSelect.value);
    oXmlRequest.getXml("../auth/listQualityChecks.jsp?" + csrf_nonce_param, cbGetQualityChecks);
  }
}


function cbGetQualityChecks(oXml, oText)
{
  var oBits = oXml.documentElement.getElementsByTagName("qch");
  for (var i = 0; i < oBits.length; i++)
  {
    var sLabel = m_oTestLabels[oBits[i].getAttribute("label")];
    AddTableCell(sLabel, oBits[i].getAttribute("bit"));
  }
}


// ShowObsSpecificStuff()
// This function shows or hides elements that only apply when an
// observation type has been selected.
// NOTE: This function depends on navigator.appName() to detect the browser
// and takes appropriate action for IE vs. FF.
function ShowObsSpecificStuff(bShow)
{
  var oHdrTst = document.getElementById("hdrTestArea");
  var oHdrMin = document.getElementById("hdrMin");
  var oHdrMax = document.getElementById("hdrMax");
  var oTxtMin = document.getElementById("txtMin");
  var oTxtMax = document.getElementById("txtMax");

  var sTableDisplayStyle = "";
  var sTextDisplayStyle = "";

  if (bShow)
  {
    if (navigator.appName.toUpperCase().indexOf("MICROSOFT") != -1)
    {
      // Internet Explorer doesn't support the "table-cell" style.
      sTableDisplayStyle = "block";
    }
    else
    {
      // Firefox supports "table-cell".
      // Using "block" does funky things with the header for the Min/Max entry fields.
      sTableDisplayStyle = "table-cell";
    }
    sTextDisplayStyle = "block";
  }
  else
  {
    sTableDisplayStyle = "none";
    sTextDisplayStyle = "none";
  }

  oHdrTst.style.display = sTextDisplayStyle;
  oHdrMin.style.display = sTableDisplayStyle;
  oHdrMax.style.display = sTableDisplayStyle;
  oTxtMin.style.display = sTableDisplayStyle;
  oTxtMax.style.display = sTableDisplayStyle;

  oTxtMin.disabled = false;
  oTxtMax.disabled = false;
}


function AddTableCell(sLabel, nBitPos)
{
  var nRows = m_oTestTable.rows.length;
  var oRow;

  // Get the number of rows in the table.
  // If there are no rows, then create one.
  // Otherwise, get a reference to the last row.
  if (nRows == 0)
  {
    oRow = m_oTestTable.insertRow(-1);
  }
  else
  {
    oRow = m_oTestTable.rows[nRows - 1];
  }

  // If all 6 cells are used in the last row, then add another row.
  // Note: Each row has 3 pairs of cells for each test.
  // The first half of the pair contains the label and the second half contains the listbox.
  var nCells = oRow.cells.length;

  if (nCells == 6)
  {
    oRow = m_oTestTable.insertRow(-1);
  }

  // Add a cell for the label.
  var oCellLabel = oRow.insertCell(-1);
  oCellLabel.innerHTML = sLabel;
  oCellLabel.className = "label";

  // Add a cell for the listbox.
  var oCellList = oRow.insertCell(-1);
  oCellList.className = "testPF";

  // Stick the bit position onto the cell so it can be extracted in Validate()
  oCellList.bitPosition = nBitPos;

  // Create the listbox.
  var oListbox = document.createElement("select");
  oListbox.onchange = PFChanged;

  // Create the Pass option and append it to the listbox.
  var oOption = document.createElement("option");
  oOption.value = 1;
  oOption.appendChild(document.createTextNode("P"));
  oListbox.appendChild(oOption);
  oCellList.appendChild(oListbox);

  // Create the NotPass option and append it to the listbox.
  oOption = document.createElement("option");
  oOption.value = 0;
  oOption.appendChild(document.createTextNode("N"));
  oListbox.appendChild(oOption);
  oCellList.appendChild(oListbox);

  // Create the Clear option and append it to the listbox.
  oOption = document.createElement("option");
  oOption.value = -1;
  oOption.appendChild(document.createTextNode("(Clear)"));
  oListbox.appendChild(oOption);
  oCellList.appendChild(oListbox);

  // Don't select anything.
  oListbox.selectedIndex = -1;
}


function PFChanged()
{
  if (this.value == -1)
  {
    this.selectedIndex = -1;
  }
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
  var oObsMax = document.getElementById("maxObs");
  var oObsMin = document.getElementById("minObs");

  oObsTypes.value = "";
  oObsMin.value = "";
  oObsTypes.value = "";

  var oFlags = document.getElementById("flags");
  oFlags.value = "--------------------------------";

  // Verify that the minimum and maximum are valid if they are set
  var sMin = document.getElementById("txtMin").value;
  var sMax = document.getElementById("txtMax").value;

  // The remainder of the parameters are dependent on an observation type being selected
  var oObsTypeList = document.getElementById("listObsTypes");
  var nIndex = oObsTypeList.selectedIndex;

  // Index greater than zero skips the "no selection" option
  if (nIndex > 0)
  {
    if (sMin.length > 0 && !ValidateNumbers(sMin))
    {
      oStatusMessage.nodeValue = "The Minimum is not a valid number.";
      return;
    }

    if (sMax.length > 0 && !ValidateNumbers(sMax))
    {
      oStatusMessage.nodeValue = "The Maximum is not a valid number";
      return;
    }

    var sObs = oObsTypeList.options[nIndex].value;

    if (sMin.length > 0)
      oObsMin.value = sMin;


    if (sMax.length > 0)
      oObsMax.value = sMax;

    oObsTypes.value = sObs;


    // Javascript doesn't allow us to set an individual character in a string easily.
    // Therefore, create an array of dashes.  This will be turned into a string, later.
    var aFlags = [];
    for (var i = 0; i < 32; i++)
    {
      aFlags[i] = "-";
    }

    // Walk the rows of the Quality Checks table.
    var oRow;
    var oCell;
    var oSelect;
    var nBitPos;
    for (var iRow = 0; iRow < m_oTestTable.rows.length; iRow++)
    {
      oRow = m_oTestTable.rows[iRow];
      for (var iCol = 0; iCol < oRow.childNodes.length; iCol++)
      {
        // Extract the P/N flags from the listbox for the Quality Check.
        oCell = oRow.childNodes[iCol];

        if (oCell.className == "testPF")
        {
          // If the user selected a particular Quality Check,
          // then set the corresponding bit in the flags array.
          oSelect = oCell.childNodes[0];
          if (oSelect.selectedIndex != -1)
          {
            nBitPos = oCell.bitPosition;
            aFlags[nBitPos] = oSelect.value;
          }
        }
      }
    }

    // Now, create the string of flags from the flag array.
    // NOTE: Build the string from right to left.
    var sFlags = "";
    for (var i = 32; i > 0; i--)
    {
      sFlags += aFlags[i - 1];
    }

    oFlags.value = sFlags;
  }

  document.forms[0].submit();
}


function PrevPage()
{
  document.location = "wizardStations.jsp";
}



