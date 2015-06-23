
var m_oTable = null;

function onLoad()
{
    CreateClock(false);

    m_oTable = document.getElementById("tblMain");

    // Add the row for the map area links.
    var oTblRow = m_oTable.insertRow(-1);
    var oTblCell;
    var oItem;

    // Fill in the United States links.
    var oRow;
    for (var i = 0; i < oMapAreas.length; i++)
    {
        oRow = oMapAreas[i];

        if (oRow.country == "USA")
        {
            // Create a new table cell the first time through
            // and then after every tenth item.
            if ((i % 10) == 0)
            {
                oTblCell = oTblRow.insertCell(-1);
                oTblCell.innerHTML = "";
            }

            if (oRow.active == 1)
            {
                oItem = document.createElement("a");
                oItem.href = "wdemap.html" +
                             "?lat=" + oRow.lat +
                             "&lon=" + oRow.lon +
                             "&zoom=" + oRow.zoom;
                oItem.innerHTML = oRow.name + "<br/>";
            }
            else
            {
                oItem = document.createElement("span");
                oItem.className = "dim";
                oItem.innerHTML = oRow.name + "<br/>";
            }

            oTblCell.appendChild(oItem);
        }
    }

    // Add an empty cell.
    oTblCell = oTblRow.insertCell(-1);
    oItem = document.createTextNode("");
    oTblCell.appendChild(oItem);

    // Now, fill in the Canadian links.
    for (var i = 0; i < oMapAreas.length; i++)
    {
        oRow = oMapAreas[i];

        if (oRow.country == "Canada")
        {
            // Create a new table cell the first time through
            // and then after every tenth item.
            if ((i % 10) == 0)
            {
                oTblCell = oTblRow.insertCell(-1);
                oTblCell.innerHTML = "";
            }

            if (oRow.active == 1)
            {
                oItem = document.createElement("a");
                oItem.href = "wdemap.html" +
                             "?lat=" + oRow.lat +
                             "&lon=" + oRow.lon +
                             "&zoom=" + oRow.zoom;
                oItem.innerHTML = oRow.name + "<br/>";
            }
            else
            {
                oItem = document.createElement("span");
                oItem.className = "dim";
                oItem.innerHTML = oRow.name + "<br/>";
            }

            oTblCell.appendChild(oItem);
        }
    }
}