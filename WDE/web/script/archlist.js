var startDate, endDate;

var startYear, startMonth, startDay;

var endYear, endMonth, endDay;

var yearRange, monthRange, dayRange;

var beginDate, terminateDate;

var IDArray, dateString;

function convertArray()
{
    var fullURL = window.location.href;
    var fullArray = fullURL.split("&");
        
    startDate = (fullArray[fullArray.length - 2]).split('');
    endDate = (fullArray[fullArray.length - 1]).split('');
    
    startYear = ([startDate[0], startDate[1], startDate[2], startDate[3]].join(''));
    startMonth = ([startDate[4], startDate[5]].join(''));
    startDay = ([startDate[6], startDate[7]].join(''));
    
    endYear = ([endDate[0], endDate[1], endDate[2], endDate[3]].join(''));
    endMonth = ([endDate[4], endDate[5]].join(''));
    endDay = ([endDate[6], endDate[7]].join(''));
    
    yearRange = ([endDate[0], endDate[1], endDate[2], endDate[3]].join('')) - ([startDate[0], startDate[1], startDate[2], startDate[3]].join(''));
    monthRange = ([endDate[4], endDate[5]].join('')) - ([startDate[4], startDate[5]].join(''));
    dayRange = ([endDate[6], endDate[7]].join('')) - ([startDate[6], startDate[7]].join(''));
    
    beginDate = new Date(startYear, (startMonth - 1), startDay, 00,0,0,0);
    terminateDate = new Date(endYear, (endMonth - 1), endDay, 00,0,0,0);
    
    IDArray = new Array(fullArray.length - 3);
    for (var i = 1; i < (fullArray.length - 2); i++)
    {
        IDArray[i-1] = fullArray[i];
        if(IDArray[i-1].length < 2)
        {
            IDArray[i-1] = "0" + IDArray[i-1];
        }
    }
    
    //Print array as an html <ul>
    return (printArray(IDArray));
}    

function wgetVar()
{
    var protocol = window.location.protocol;
    var hostname = window.location.hostname;
    return "<strong>" + "wget " + protocol + "//" + hostname +
            "/archdl.jsp?file=" + IDArray[0] + "-" + dateString + 
            "</strong>.";
}

function printArray(testArray)
{
    var returnString = "";
    dateString = "";
    var addMonth;
    var addDay;

    //var dateRange = (yearRange*365) + (monthRange*30) + dayRange;
    var dateRange = ((terminateDate.getTime() - beginDate.getTime()) / 86400000) + 1;
    var finalArray = new Array(100);
    
    var displayDate, displayYear, displayMonth, displayDay;
    var urlHelp;
    
    var inc = 0;

    var protocol = window.location.protocol;
    var hostname = window.location.hostname;

    for(var i = 0; i < testArray.length; i++)
    {
        if(!(inc < 100))
        {
            break;
        }
        for(var j = 0; j < dateRange; j++)
        {
            //This inner for-loop traverses all the days selected.
            //We will need to find the date as inc increases so we can display it.
            if(inc < 100)
            {
                displayDate = addDays(beginDate, j);
                displayYear = (displayDate.getFullYear()).toString();
                displayMonth = displayDate.getMonth() + 1;
                if(displayMonth.toString().length < 2)
                {
                    displayMonth = "0" + displayMonth.toString();
                }
                displayDay = displayDate.getDate();
                if(displayDay.toString().length < 2)
                {
                    displayDay = "0" + displayDay.toString();
                }

                dateString = displayYear + displayMonth + displayDay;
                var urlHelp = protocol + "//" + hostname + "/archdl.jsp?" + csrf_nonce_param + "&file=" + testArray[i] + "-" + dateString;
                finalArray[inc] = "<li><a href='" + urlHelp +"' download='" + testArray[i] + "-" + dateString + ".csv.gz'>" + testArray[i] + "-" + dateString + ".csv.gz" + "</a></li>";
                inc++;
            }
            else
            {
                break;
            }
        }
    }
    returnString = finalArray.join('');
    
    return "<ul>" + returnString + "</ul>";
}

function addDays(date, days) {
    var result = new Date(date);
    result.setDate(result.getDate() + days);
    return result;
}