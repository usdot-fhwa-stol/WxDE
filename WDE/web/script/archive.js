function Generate()
{
    var oListAll = document.getElementById("listAll");
    var selectedList = takeSelected(oListAll);
    var startDate = document.getElementById("startDate").value;
    var endDate = document.getElementById("endDate").value;
    
    if(startDate && endDate)
    {
        //To pass data from here to archlist.js, convert oListAll to a string and append the
        //string onto the end of the next url
        var appendlist = listToString(selectedList);
        var appendStart = dateToString(startDate);
        var appendEnd = dateToString(endDate);
        
        window.location.href = "archlist.jsp?" + csrf_nonce_param + "&" + appendlist + appendStart + "&" + appendEnd;
    }
    else
    {
        window.alert("Start and End dates must be valid");
    }
}

function takeSelected(takeFrom)
{
    //Check if each item is selected. If it is, populate newList with it.
    var entryFrom;
    var newList = [];
    for(var i = 0; i < takeFrom.childNodes.length; i++)
    {
        entryFrom = takeFrom.childNodes.item(i);
        if(entryFrom.selected)
        {
            //Try taking each selected element and push() it onto an initially empty array
            newList.push(entryFrom);
        }
    }
    return newList;
}

function listToString(list)
{
    var finalString = "";
    
    for(var i = 0; i < list.length; i++)
    {
        finalString = finalString + list[i].value + "&";
    }
    
    return finalString;
}

function dateToString(date)
{
    date = date.replace(/[^0-9]/g, '');
    //date is now in the format 03052016
    //We need date to be in format 20160305
    var temp = date.split('');
    date = [temp[4], temp[5], temp[6], temp[7], temp[0], temp[1], temp[2], temp[3]];
    
    var view = date.join("");
    return view;
}