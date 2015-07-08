//-----------------------------------------------------------
// ListboxInsertItem()
//      Inserts an item into a listbox.
//      Returns the Option object to the caller so extra data can
//      be added to it, if necessary.
//-----------------------------------------------------------
function ListboxInsertItem(oSelect, sText, sValue, bSelected)
{
    bSelected = typeof bSelected !== 'undefined' ? bSelected : false;

    var oEntry = document.createElement("option");
    oEntry.appendChild(document.createTextNode(sText));
    oEntry.value = sValue;
    //oEntry.selected = bSelected;
    if (bSelected) {
        oEntry.setAttribute("selected", "selected");
    }

    oSelect.appendChild(oEntry);

    return(oEntry);
}


//-----------------------------------------------------------
// ListboxRemoveSelected()
//      Remove selected items from a listbox.
//-----------------------------------------------------------
function ListboxRemoveSelected(oSelect)
{
    for (var i = oSelect.childNodes.length; i > 0; i--)
    {
        oEntry = oSelect.childNodes.item(i - 1);
        if (oEntry.selected)
        {
            oSelect.removeChild(oEntry);
        }
    }
}


//-----------------------------------------------------------
// ListboxRemoveAll()
//      Removes all items in a list box.
//-----------------------------------------------------------
function ListboxRemoveAll(oSelect)
{
    while (oSelect.childNodes.length > 0)
    {
        oSelect.removeChild(oSelect.lastChild);
    }
}


//-----------------------------------------------------------
// ListboxRemoveItem()
//      Removes a given item in a list box.
//-----------------------------------------------------------
function ListboxRemoveItem(oSelect, nIndex)
{
    oSelect.remove(nIndex);
}


//-----------------------------------------------------------
// ListboxSelectItem()
//      Walks the items in a listbox and selects the entry that matches sTarget.
//      If bCheckValue is true, then it checks the value.
//      If bCheckValue is false, then it checks the text entry.
//-----------------------------------------------------------
function ListboxSelectItem(oSelect, sTarget, bCheckValue)
{
    for (var i = 0; i < oSelect.childNodes.length; i++)
    {
        // Debug("Checking " + sTarget + " against " + oSelect.childNodes.item(i).value + " / " + oSelect.childNodes.item(i).text);
        if (bCheckValue)
        {
            // Internet Explorer seems to choke sometimes
            // when selecting the item.  It appears to be a timing issue,
            // so putting it in a try block seems to resolve it.
            if (oSelect.childNodes.item(i).value == sTarget)
            {
                try
                {
                    oSelect.childNodes.item(i).selected = true;
                    break;
                }
                catch(oErr)
                {
                    // Debug("Could not set item #" + i + " in listbox " + oSelect.name);
                    // break;
                }
            }
        }
        else
        {
            if (oSelect.childNodes.item(i).text == sTarget)
            {
                try
                {
                    oSelect.childNodes.item(i).selected = true;
                    break;
                }
                catch(oErr)
                {
                    // Debug("Could not set item #" + i + " in listbox " + oSelect.name);
                    // break;
                }
            }
        }
    }
}

//-----------------------------------------------------------
// ListboxTossSelected()
//      Toss selected items from one list to another.
//-----------------------------------------------------------
function ListboxTossSelected(oSelectFrom, oSelectTo)
{
    // Copy selected items from the ListFrom to the ListTo.
    var bFound;
    var oEntryFrom;
    for (var iFrom = 0; iFrom < oSelectFrom.childNodes.length; iFrom++)
    {
        oEntryFrom = oSelectFrom.childNodes.item(iFrom);
        if (oEntryFrom.selected)
        {
            // Search the To list to see if the item is already in there.
            bFound = false;
            for (var iTo = 0; iTo < oSelectTo.childNodes.length; iTo++)
            {
                if (oSelectTo.childNodes.item(iTo).value == oEntryFrom.value)
                {
                    bFound = true;
                    break;
                }
            }
            
            // Add the item to the To list if it isn't in there.
            if (!bFound)
            {
                ListboxInsertItem(oSelectTo, oEntryFrom.firstChild.data, oEntryFrom.value);
            }
        }
    }
}


//-----------------------------------------------------------
// ListboxSelectedText()
//      Get the string associated with the selected item in a listbox.
//      NOTE: Only works with a single-select listbox.
//-----------------------------------------------------------
function ListboxSelectedText(oSelect)
{
    if (oSelect.item(oSelect.selectedIndex).childNodes.length != 0)
        return(oSelect.item(oSelect.selectedIndex).firstChild.data);
    else
        return("");
}


