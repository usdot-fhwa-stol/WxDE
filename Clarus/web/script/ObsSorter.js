//-----------------------------------------------------------
// ObsSorter.js
//      Contains code to hold a sorted array of observations.
//-----------------------------------------------------------

function Obs(oObsXml)
{
    this.sensorId = oObsXml.getAttribute("sensorId");
    this.timestamp = oObsXml.getAttribute("timestamp");
    this.obsDesc = oObsXml.getAttribute("type");
    this.value = oObsXml.getAttribute("value");
    this.englishValue = oObsXml.getAttribute("englishValue");
    this.runFlags = oObsXml.getAttribute("runFlags");
    this.passedFlags = oObsXml.getAttribute("passedFlags");
    this.qchTests = oObsXml.getAttribute("qchTests");
    this.qchFlags = oObsXml.getAttribute("qchFlags");

    var sUnits = oObsXml.getAttribute("units");
    if (sUnits == "null")
    {
        this.units = "";
    }
    else
    {
        this.units = "(" + sUnits + ")";
    }
    
    var sEnglishUnits = oObsXml.getAttribute("englishUnits");
    if (sEnglishUnits == "null")
    {
        this.englishUnits = "";
    }
    else
    {
        this.englishUnits = "(" + sEnglishUnits + ")";
    }
}


function ObsSorter()
{
    this.aList = [];
    
    
    // push()
    // Creates a new object and pushes it onto the end of the array.
    this.push = function(oObsXml)
    {
        this.aList.push(new Obs(oObsXml));
    }
    

    // get()
    // Returns the object at the given position in the array.
    this.get = function(nIndex)
    {
        return(this.aList[nIndex]);
    }
    
    
    // remove()
    // Removes the object at the given position in the array.
    this.remove = function(nIndex)
    {
        this.aList.splice(nIndex, 1);
    }

    
    // length()
    // Returns the number of items in the array.
    this.length = function()
    {
        return(this.aList.length);
    }

    // ==================== Getter Methods ====================
    // These functions return a data value for a given array index.
    
    this.description = function(nIndex)
    {
        return(this.aList[nIndex].obsDesc);
    }

    this.sensorId = function(nIndex)
    {
        return(this.aList[nIndex].sensorId);
    }

    this.timestamp = function(nIndex)
    {
        return(this.aList[nIndex].timestamp);
    }

    this.units = function(nIndex)
    {
        return(this.aList[nIndex].units);
    }

    this.value = function(nIndex)
    {
        return(this.aList[nIndex].value);
    }

    this.englishUnits = function(nIndex)
    {
        return(this.aList[nIndex].englishUnits);
    }

    this.englishValue = function(nIndex)
    {
        return(this.aList[nIndex].englishValue);
    }

    this.runFlags = function(nIndex)
    {
        return(this.aList[nIndex].runFlags);
    }

    this.passedFlags = function(nIndex)
    {
        return(this.aList[nIndex].passedFlags);
    }

    this.qchTests = function(nIndex)
    {
        return(this.aList[nIndex].qchTests);
    }

    this.qchFlags = function(nIndex)
    {
        return(this.aList[nIndex].qchFlags);
    }

    // ========================================================
    
    // sort()
    // Sorts the array according to the obsDesc.
    this.sort = function()
    {
        this.aList.sort(this.cbSortByObsDesc);
    }


    // cbSortByObsDesc()
    // Callback function; called by Array.sort() to compare two items.
    this.cbSortByObsDesc = function(oLeft, oRight)
    {
        var x = oLeft.obsDesc.toLowerCase();
        var y = oRight.obsDesc.toLowerCase();
        return ((x < y) ? -1 : ((x > y) ? 1 : 0));
    }
    

    // toString()
    // Returns a comma-separated string containing all the descriptions.
    this.toString = function()
    {
        var s = "";
        for (var i = 0; i < this.aList.length; i++)
        {
            if (s != "")
                s += ", ";
            s += this.aList[i].obsDesc;
        }
        return(s);
    }


    // binarySearch()
    //     This work is licensed under Creative Commons GNU GPL License
    //     http://creativecommons.org/licenses/GPL/2.0/
    //     Copyright (C) 2006 Russel Lindsay
    //     www.weetbixthecat.com
    //  1. Array MUST be sorted already
    //  2. If the array has multiple elements with the same value the first instance 
    //     in the array is returned e.g.
    //       [1,2,3,3,3,4,5].binarySearch(3);  // returns 2
    //     This means slightly more loops than other binary searches, but I figure 
    //     it's worth it, as the worst case searchs on a 1 million array is around 20
    //  3. The return value is the index of the first matching element, OR 
    //     if not found, a negative index of where the element should be - 1 e.g.
    //       [1,2,3,6,7].binarySearch(5);      // returns -4
    //       [1,2,3,6,7].binarySearch(0);      // returns -1
    //     To insert at this point do something like this:
    //       var array = [1,2,3,6,7];
    //       var index = array.binarySearch(5);
    //       if(index < 0)
    //         array.splice(Math.abs(index)-1, 0, 5); // inserted the number 5
    //  4. mid calculation from 
    //     http://googleresearch.blogspot.com/2006/06/extra-extra-read-all-about-it-nearly.html
    this.binarySearch = function(sObsDesc)
    {
        var left = -1;
        var right = this.aList.length;
        var mid;

        while (right - left > 1)
        {
            mid = (left + right) >>> 1;
            if (this.aList[mid].obsDesc < sObsDesc)
                left = mid;
            else
                right = mid;
        }

        if (right >= this.aList.length)
        {
            return -(right + 1);
        }
        else
        {
            if (this.aList[right].obsDesc != sObsDesc)
                return -(right + 1);
        }

        return right;
    }

}