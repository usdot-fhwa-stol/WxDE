// JScript File
//-----------------------------------------------------------
// QChTests.js
//      Contains code to manage the array of QChTests.
//-----------------------------------------------------------

function QChTest(sTestname, sLabelImage)
{
    this.testname = sTestname;
    this.labelimage = sLabelImage;
    
    // If no label image is given, then this test is not run.
    if (sLabelImage == "")
        this.inuse = false;
    else
        this.inuse = true;
}



function QChTests()
{
    this.aTests = [];
    this.index = 0;

    // push()
    // Creates a new object and pushes it onto the end of the array.
    this.push = function(sTestname, sLabelImage)
    {
        this.aTests.push(new QChTest(sTestname, sLabelImage));
        this.index++;
    }

    
    // get()
    // Returns the object at the given position in the array.
    this.get = function(nIndex)
    {
        return(this.aTests[nIndex]);
    }

    
    // length()
    // Returns the number of items in the array.
    this.length = function()
    {
        return(this.aTests.length);
    }

    
    // getImage()
    // Returns the label image for a test.
    this.getImage = function(nIndex)
    {
        return(this.aTests[nIndex].labelimage);
    }
    
    
    // name()
    // Returns the name of the test for a given index.
    this.name = function(nIndex)
    {
        return(this.aTests[nIndex].testname);
    }

    // inUse()
    // Returns whether or not the test is used.
    this.inUse = function(nIndex)
    {
        return(this.aTests[nIndex].inuse);
    }
    
}

