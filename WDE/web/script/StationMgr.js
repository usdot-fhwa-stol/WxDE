//-----------------------------------------------------------
// StationMgr.js
//      Contains code to manage the array of Stations.
//-----------------------------------------------------------

function Station(oStationXml, oMarker)
{
    this.stationCode = oStationXml.getAttribute("stationCode");
    this.latitude = oStationXml.getAttribute("lat");
    this.longitude = oStationXml.getAttribute("lng");
    this.distGroup = oStationXml.getAttribute("distgrp");

    // If the obsValue attribute is filled in, then stick it in the label field.
    var sObsValue = oStationXml.getAttribute("obsValue");
    if (sObsValue != undefined)
    {
        this.label = sObsValue;
    }
    
    this.internalValue = sObsValue;
    this.englishValue = oStationXml.getAttribute("englishValue");
    
    this.marker = oMarker;
}


function StationMgr()
{
    this.aList = [];

    // cbSortByStationCode()
    // Callback function; called by Array.sort() to compare two items.
    this.cbSortByStationCode = function(oLeft, oRight)
    {
//        var x = oLeft.stationCode.toLowerCase();
//        var y = oRight.stationCode.toLowerCase();
//        return ((x < y) ? -1 : ((x > y) ? 1 : 0));

        if (oLeft.stationId < oRight.stationId)
            return -1;

        if (oLeft.stationId > oRight.stationId)
            return 1;

        return 0;
    };
    

    // sort()
    // Sorts the array according to the stationCode.
    this.sort = function()
    {
        this.aList.sort(this.cbSortByStationCode);
    };


    // push()
    // Creates a new object and pushes it onto the end of the array.
    this.push = function(oStationXml, oMarker)
    {
        oStationXml.marker = oMarker;
        this.aList.push(oStatonXml);
//        this.aList.push(new Station(oStatonXml, oMarker));
    };


    // add(oStationXml)
    // maintains the station list as a set 
    // new stations are added to the end of the list while an existing station index is returned
    this.add = function(oStationXml)
    {
        var nIndex = this.aList.length;
        while (--nIndex >= 0 && this.aList[nIndex].stationId != oStationXml.stationId);

        // if the index is less than zero, the station was not found
        if (nIndex < 0)
        {
            this.aList.push(oStationXml);
            return (-this.aList.length);
        }

        return nIndex;
    };
    

    // insert()
    // Creates a new object and inserts it into the array at the given index.
    this.insert = function(nIndex, oStationXml, oMarker)
    {
        oStationXml.marker = oMarker;
        this.aList.push(oStationXml);
        this.sort();

//        oStation = new Station(oStationXml, oMarker);
        
        // A negative index can be returned by the binarySearch() method,
        // and indicates where the item should be inserted.
        // Therefore, convert the negative index to a positive.
//        if (nIndex < 0)
//            nIndex = nIndex * -1;

        // Walk the array from the last item to the insert point.
        // Copy each item into the next spot.
        // Then, copy the new item into the insert point.
//        if (this.aList.length > 0)
//        {
            // this.aList.push();
//            for (var i = this.aList.length; i > nIndex; i--)
//            {
//                this.aList[i] = this.aList[i - 1];
//            }
//        }
//        this.aList[nIndex] = oStation;
//        this.aList[nIndex] = oStationXml;

/*****
        // Split the array into two pieces at the point where the item should be inserted.
        var oRemainder = this.aList.splice(nIndex);
        
        // Append the item to the end of the main array.
//        this.aList.push(oStation);
        this.aList.push(oStationXml);
        
        // Reattach the remaining part of the array.
        this.aList = this.aList.concat(oRemainder);
*****/
    };
    
    
    // insertIfMissing()
    // If the item is not found in the array, then a new one is created
    // and inserted the proper index.
    this.insertIfMissing = function(oStationXml, oMarker)
    {
        // If the item is in the list, then just bail out.
//        var nIndex = this.binarySearch(oStationXml.getAttribute("stationCode"));
        var nIndex = this.binarySearch(oStationXml.stationId);
        if (nIndex >= 0)
            return;
        
        // A negative index is returned by the binarySearch() method
        // and indicates where the item should be inserted.
        // Therefore, convert the negative index to a positive and subtract 1.
        // Note: Subtract 1 because binarySearch() returns a 1-relative index.
        if (nIndex < 0)
            nIndex = (nIndex * -1) - 1;

        this.insert(nIndex + 1, oStationXml, oMarker);
    };
    
    
    // get()
    // Returns the object at the given position in the array.
    this.get = function(nIndex)
    {
        return(this.aList[nIndex]);
    };
    
    
    // setLabel()
    // Sets the label that is currently being displayed for a station.
    this.setLabel = function(nIndex, sValue)
    {
        this.aList[nIndex].label = sValue;
    };
    
    
    // setLabelDiv()
    // Stores the label <div> tag for a station.
    this.setLabelDiv = function(nIndex, oDiv)
    {
        this.aList[nIndex].labelDiv = oDiv;
    };
    
    
    this.setObsValues = function(nIndex, nInternalValue, nEnglishValue)
    {
        this.aList[nIndex].internalValue = nInternalValue;
        this.aList[nIndex].englishValue = nEnglishValue;
    };


    // changeLabels()
    // Runs through the entire list of stations and changes the label
    // from one unit type to the other.
    this.changeLabels = function(sUnitType)
    {
        for (var i = 0; i < this.aList.length; i++)
        {
            if (sUnitType == "englishUnits")
                this.aList[i].label = this.aList[i].englishValue;
            else
                this.aList[i].label = this.aList[i].internalValue;
        }
    };


    // remove()
    // Removes the object at the given position in the array.
    this.remove = function(nIndex)
    {
        this.aList.splice(nIndex, 1);
    };

    
    // length()
    // Returns the number of items in the array.
    this.length = function()
    {
        return(this.aList.length);
    };

    
    // stationCode()
    // Returns the stationCode for a given index.
    this.stationCode = function(nIndex)
    {
        return(this.aList[nIndex].stationCode);
    };


    // label()
    // Returns the label for a given index.
    this.label = function(nIndex)
    {
        return(this.aList[nIndex].label);
    };


    // labelDiv()
    // Returns the labelDiv for a given index.
    this.labelDiv = function(nIndex)
    {
        return(this.aList[nIndex].labelDiv);
    };
    
    
    // latitude()
    // Returns the latitude for a given index.
    this.latitude = function(nIndex)
    {
//        return(this.aList[nIndex].latitude);
        return(this.aList[nIndex].lat);
    };


    // longitude()
    // Returns the longitude for a given index.
    this.longitude = function(nIndex)
    {
//        return(this.aList[nIndex].longitude);
        return(this.aList[nIndex].lng);
    };
    
    
    // distGroup()
    // Returns the distgrp for a given index;
    this.distGroup = function(nIndex)
    {
        return(this.aList[nIndex].distGroup);
    };


    // marker()
    // Returns the Google Marker for a given index.
    this.marker = function(nIndex)
    {
        return(this.aList[nIndex].marker);
    };


    // toString()
    // Returns a comma-separated string containing the stationCodes
    // of all Stations in the array.
    this.toString = function()
    {
        var s = "";
        for (var i = 0; i < this.aList.length; i++)
        {
            if (s != "")
                s += ", ";
            s += this.aList[i].stationCode;
        }
        return(s);
    };


    // codeSearch(sStationCode)
    // performs a linear search for the station matching the provided code
    this.codeSearch = function(sStationCode)
    {
        var nIndex = this.aList.length;
        while (--nIndex >= 0 && this.aList[nIndex].stationCode != sStationCode);
        return nIndex;
    };


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
    this.binarySearch = function(nStationId)
    {
        var left = -1;
        var right = this.aList.length;
        var mid;

        while (right - left > 1)
        {
            mid = (left + right) >>> 1;
            if (this.aList[mid].stationId < nStationId)
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
            if (this.aList[right].stationId!= nStationId)
                return -(right + 1);
        }

        return right;
    };

    this.binarySearch_Save = function(sStationCode)
    {
        var left = -1;
        var right = this.aList.length;
        var mid;

        while (right - left > 1)
        {
            mid = (left + right) >>> 1;
            if (this.aList[mid].stationCode < sStationCode)
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
            if (this.aList[right].stationCode != sStationCode)
                return -(right + 1);
        }

        return right;
    };
}
