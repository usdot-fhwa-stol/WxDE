// -testLat2 38.893 -testLon2 -94.668
package precippk;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;


// CAUTION:
//
// This is demo code only.  Please review it in detail before using it.
// It makes a number of assumptions about the projection,
// such as the order of variables (time, y, x),
// the scan order (E to W fastest; S to N slowest), and so on.
//
// It is critical to review your datasets and this code
// to make sure they are appropriate.
//
// =============
//
// OVERVIEW:
//
// Print an overview of the contents of a NetCDF or GRIB file.
// For each variable, print the value at each corner (for each
//   dimension, at indices 0 and n-1)
//
// If the user specified -projVar and -testVar, print info
// on the projection grid and print the values of testVar
// at the projection grid corners.
//
// If the user also specified -testLat and -testLon,
// print the value of testVar at the specified lat/lon.
//
// =============
//
// USAGE:
//
// To compile:
// mkdir tdcls
// javac -d tdcls -classpath netcdfAll-4.0.jar ReadGrib.java
//
// The file netcdfAll-4.0.jar can be downloaded from:
//    http://www.unidata.ucar.edu/software/netcdf-java/
//
// Parameters:
//   -bugs       debug level.  Default is 0.
//                 If bugs == 5, print each grid point
//   -inFile     input file name.
//   -projVar    projection variable name.  Default = null
//   -testVar    test variable name, like Temperature.  Default = null
//   -testLat    latitude of test point, if any
//   -testLon    longitude of test point, if any
//
// Examples:
//
//   java -cp tdcls:netcdfAll-4.0.jar \
//     precippk.ReadGrib \
//     -bugs 1 \
//     -inFile ST2un2008010100.Grb.Z \
//     -projVar Polar_Stereographic_projection_Grid \
//     -testVar Total_precipitation \
//     -testLat 40.0 \
//     -testLon -105.25
//
//   java -cp tdcls:netcdfAll-4.0.jar \
//     precippk.ReadGrib \
//     -bugs 1 \
//     -inFile rucs.t15z.g88anl.grib2 \
//     -projVar Polar_stereographic \
//     -testVar Temperature \
//     -testLat 40.0 \
//     -testLon -105.25
//
//
//
// =============
//
// NOTES ON THE PROJECTION GRID
//
// The lat/lon values this prints are slightly different
// from those shown at
//   http://www.emc.ncep.noaa.gov/mmb/ylin/pcpanl/QandA/#GRIDINFO
//
// For example the NCEP web site shows:
//   Point (1121,1) is at 19.805N 80.750W.   (origin 1 indexing)
// While this code shows:
//   SE corner: ix: 1120 iy: 0 lat: 19.795334 lon: -80.727036   (origin 0)
//
// There could be many reasons for this, but one possibility is
// that the code used by NCEP contains some known discrepancies.
// For example see the fortran subroutines w3fb06.f and w3fb07.f,
// referenced at
//   http://www.emc.ncep.noaa.gov/mmb/ylin/pcpanl/QandA/#GRIDLOLA
// The file w3fb06.f contains:
//         DATA  RERTH /6.3712E+6/, PI/3.1416/
// While the grib files contain:
//        :GRIB_param_grid_radius_spherical_earth = 6367.47;
//   or   :GRIB_param_grid_radius_spherical_earth = 6371229.0;
//
// My understanding from others who've worked in this area is
// that being off a few hundredths of a degree is to be expected.
//
// =============



public class ReadGrib {

public static void main( String[] args) {
  try {
    new ReadGrib( args);
  }
  catch( Exception exc) {
    exc.printStackTrace();
    System.exit(1);
  }
}


void badparms( String msg) {
  prtln("Error: " + msg);
  prtln("See the source code for usage documentation");
  System.exit(1);
}


ReadGrib( String[] args)
throws Exception
{
  int bugs = 0;
  String inFile = null;
  String testVarName = null;
  String projVarName = null;
  double testLat = -999;
  double testLon = -999;
  int iarg = 0;
  while (iarg < args.length) {
    String key = args[iarg++];
    if (iarg >= args.length) badparms("no value for the last key");
    String val = args[iarg++];
    if (key.equals("-bugs")) bugs = Integer.parseInt( val);
    else if (key.equals("-inFile")) inFile = val;
    else if (key.equals("-projVar")) projVarName = val;
    else if (key.equals("-testVar")) testVarName = val;
    else if (key.equals("-testLat")) testLat = Double.parseDouble( val);
    else if (key.equals("-testLon")) testLon = Double.parseDouble( val);
    else badparms("unknown parm: \"" + key + "\"");
  }
  if (inFile == null) badparms("parm not specified: -inFile");

  prtln("");
  prtln("CAUTION: This is demo code only.");
  prtln("Please see the notes in the source code");
  prtln("");

  NetcdfFile cdf = null;
  try {
    cdf = NetcdfFile.open(inFile);
    Group rootGroup = cdf.getRootGroup();
    prtln("getRootGroup: " + rootGroup);

    // Recursively print netcdf info on the entire tree of groups.
    prtGroup( bugs, rootGroup, 0);    // indent = 0

    // Print projection info in the rootGroup.
    if (projVarName == null)
      prtln("-projVar not specified; skipping projection info");
    else if (testVarName == null)
      prtln("-testVar not specified; skipping projection info");
    else prtProjectionInfo( bugs, rootGroup, projVarName, testVarName,
      testLat, testLon);
  }
  catch( Exception exc) {
    prtln("const: caught: " + exc);
    exc.printStackTrace();
    badparms("const: caught: " + exc);
  }
  finally {
    if (cdf != null) {
      try { cdf.close(); }
      catch( IOException exc) {}
    }
  }
  prtln("");
  prtln("CAUTION: This is demo code only.");
  prtln("Please see the notes in the source code");
  prtln("");

} // end const




// Print a netcdf group and recursively all contained groups.

void prtGroup( int bugs, Group grp, int indent) {
  prtln("");
  prtindent( indent, "=== start group ===");
  prtln("");
  prtindent( indent, "getName: \"" + grp.getName() + "\"");
  prtln("");
  prtindent( indent, "getNameAndAttributes: \""
    + grp.getNameAndAttributes() + "\"");
  prtln("");
  prtindent( indent, "getShortName: \"" + grp.getShortName() + "\"");
  prtln("");
  prtDimensions( "getDimensions", grp.getDimensions(), indent);
  prtln("");
  prtVariables( bugs, "getVariables", grp.getVariables(), indent);
  prtln("");
  prtAttributes( "getAttributes", grp.getAttributes(), indent);
  prtln("");
  for (Group subgrp : grp.getGroups()) {
    prtGroup( bugs, subgrp, indent + 1);
  }
  prtindent( indent, "=== end group ===");
  prtln("");
}









// Print a list of netcdf Dimensions.

void prtDimensions(
  String msg,
  List<Dimension> dimList,
  int indent)
{
  prtindent( indent, msg);
  for (Dimension dim : dimList) {
    prtindent( indent, "  dim: " + dim);
  }
} // prtDimensions





// Print a list of netcdf Variables.
// For each variable, print the value at each corner.
// That is, for each dimension, use indices 0 and n-1.

void prtVariables(
  int bugs,
  String msg,
  List<Variable> varList,
  int indent)
{
  prtindent( indent, msg);
  for (Variable var : varList) {
    prtln("");
    prtindent( indent, "  var: " + var);
    DataType dataType = var.getDataType();

    Array arr = null;
    try { arr = var.read(); }
    catch( IOException exc) {
      prtln("caught: " + exc);
    }
    long asize = arr.getSize();
    int[] ashape = arr.getShape();
    int rank = arr.getRank();
    prtindent( indent, "    dataType: " + dataType);
    prtindent( indent, "    asize: " + asize);
    prtindent( indent, "    ashape: " + formatIntVec( ashape));
    prtindent( indent, "    rank: " + rank);

    Index index = arr.getIndex();
    String stg;
    if (rank == 0) {
      stg = getArrString( arr, index, dataType);
      prtindent( indent, "corner value: " + stg);
    }
    else if (rank == 1) {
      // Select first, last elements for each dim
      int inca = Math.max( 1, ashape[0] - 1);
      for (int ia = 0; ia < ashape[0]; ia += inca) {
        index.set( ia);
        stg = getArrString( arr, index, dataType);
        prtindent( indent, "corner value: index0: " + ia
          + "  value: " + stg);
      }
    } // else rank == 1

    else if (rank == 2) {
      // Select first, last elements for each dim
      int inca = Math.max( 1, ashape[0] - 1);
      int incb = Math.max( 1, ashape[1] - 1);
      for (int ia = 0; ia < ashape[0]; ia += inca) {
        for (int ib = 0; ib < ashape[1]; ib += incb) {
          index.set( ia, ib);
          stg = getArrString( arr, index, dataType);
          prtindent( indent, "corner value: index0: " + ia
            + "  index1: " + ib
            + "  value: " + stg);
        }
      }
    } // else rank == 2

    else if (rank == 3) {
      // Select first, last elements for each dim
      int inca = Math.max( 1, ashape[0] - 1);
      int incb = Math.max( 1, ashape[1] - 1);
      int incc = Math.max( 1, ashape[2] - 1);
      for (int ia = 0; ia < ashape[0]; ia += inca) {
        for (int ib = 0; ib < ashape[1]; ib += incb) {
          for (int ic = 0; ic < ashape[2]; ic += incc) {
            index.set( ia, ib, ic);
            stg = getArrString( arr, index, dataType);
            prtindent( indent, "corner value: index0: " + ia
              + "  index1: " + ib
              + "  index2: " + ic
              + "  value: " + stg);
          }
        }
      }
    } // else rank == 3

    else if (rank == 4) {
      // Select first, last elements for each dim
      int inca = Math.max( 1, ashape[0] - 1);
      int incb = Math.max( 1, ashape[1] - 1);
      int incc = Math.max( 1, ashape[2] - 1);
      int incd = Math.max( 1, ashape[3] - 1);
      for (int ia = 0; ia < ashape[0]; ia += inca) {
        for (int ib = 0; ib < ashape[1]; ib += incb) {
          for (int ic = 0; ic < ashape[2]; ic += incc) {
            for (int id = 0; id < ashape[3]; id += incd) {
              index.set( ia, ib, ic, id);
              stg = getArrString( arr, index, dataType);
              prtindent( indent, "corner value: index0: " + ia
                + "  index1: " + ib
                + "  index2: " + ic
                + "  index3: " + id
                + "  value: " + stg);
            }
          }
        }
      }
    } // else rank == 4

    if (bugs >= 5) {
      IndexIterator ixi = arr.getIndexIterator();
      while (ixi.hasNext()) {
        String tstg = null;
        if (dataType == DataType.BOOLEAN)     tstg = "" + ixi.getBooleanNext();
        else if (dataType == DataType.BYTE)   tstg = "" + ixi.getByteNext();
        else if (dataType == DataType.CHAR)   tstg = "" + ixi.getCharNext();
        else if (dataType == DataType.DOUBLE) tstg = "" + ixi.getDoubleNext();
        else if (dataType == DataType.FLOAT)  tstg = "" + ixi.getFloatNext();
        else if (dataType == DataType.INT)    tstg = "" + ixi.getIntNext();
        else if (dataType == DataType.LONG)   tstg = "" + ixi.getLongNext();
        else tstg = "" + ixi.getObjectNext();
        prtindent( indent, "      ixi: " + ixi + "  value: " + tstg);
      }
    } // if bugs
  } // for var in varList
} // prtVariables






String getArrString(
  Array arr,
  Index index,
  DataType dataType)
{
  String tstg;
  if (dataType == DataType.BOOLEAN)     tstg = "" + arr.getBoolean( index);
  else if (dataType == DataType.BYTE)   tstg = "" + arr.getByte( index);
  else if (dataType == DataType.CHAR)   tstg = "" + arr.getChar( index);
  else if (dataType == DataType.DOUBLE) tstg = "" + arr.getDouble( index);
  else if (dataType == DataType.FLOAT)  tstg = "" + arr.getFloat( index);
  else if (dataType == DataType.INT)    tstg = "" + arr.getInt( index);
  else if (dataType == DataType.LONG)   tstg = "" + arr.getLong( index);
  else tstg = "" + arr.getObject( index);
  return tstg;
}





void prtAttributes(
  String msg,
  List<Attribute> attrList,
  int indent)
  {
  prtindent( indent, msg);
  for (Attribute attr : attrList) {
    prtindent( indent, "  attr: " + attr);
  }
} // prtAttributes





void prtProjectionInfo(
  int bugs,
  Group grp,
  String projVarName,
  String testVarName,
  double testLat,
  double testLon)
throws Exception
{

  // Find the projection info
  double[] proj = getProjectionInfo( bugs, grp, projVarName);
  int ip = 0;
  double earthRadiusKm = proj[ip++];  // Spherical earth radius, km, 6367.470
  double deltaX        = proj[ip++];  // Grid X spacing in meters
  double deltaY        = proj[ip++];  // Grid Y spacing in meters
  double lat0          = proj[ip++];  // Proj origin lat, degrees, e.g, 90
  double lon0          = proj[ip++];  // Proj origin lon, degrees, e.g, -105
  double xorigin       = proj[ip++];  // x value of grid origin, meters
  double yorigin       = proj[ip++];  // y value of grid origin, meters

  // Find the ncVar for testVarName
  Variable ncVar = grp.findVariable( testVarName);
  if (ncVar == null) throwerr("Cannot find -testVar: " + testVarName);
  Attribute missValAttr = ncVar.findAttribute("missing_value");
  double missVal = missValAttr.getNumericValue().doubleValue();
  Attribute unitAttr = ncVar.findAttribute("units");
  String unitName = unitAttr.getStringValue();

  prtln("ncVar: " + ncVar);
  prtln("missValAttr: " + missValAttr
    + "  dataType: " + missValAttr.getDataType());
  prtln("missVal: " + missVal);
  prtln("unitName: " + unitName);

  // Get Dimensions and their lengths.
  // The first varies slowest.
  Dimension[] ncDims = ncVar.getDimensions().toArray(
    new Dimension[0]);
  int ndim = ncDims.length;
  int[] dimLens = new int[ndim];
  for (int ii = 0; ii < ndim; ii++) {
    dimLens[ii] = ncDims[ii].getLength();
    prtln("dimension " + ii + ":  name: " + ncDims[ii].getName()
      + "  length: " + ncDims[ii].getLength());
  }

  // CAUTION: assume the dimensions are:
  //   (time, y, x)
  // Verify this by reviewing the netcdf output.
  // The first variable varies slowest; the last fastest.
  //
  // We set the time index to 0.
  // We will substitute the x index for the -1.
  // We will substitute the y index for the -2.

  int[] dimSpecs = {0, -2, -1};      // time, y, x
  if (dimSpecs.length != ndim) throwerr("wrong num dimSpecs");

  // Find xLen, yLen.
  int xLen = -1;
  int yLen = -1;
  for (int ii = 0; ii < ndim; ii++) {
    if (dimSpecs[ii] == -1) xLen = dimLens[ii];
    else if (dimSpecs[ii] == -2) yLen = dimLens[ii];
    else if (dimSpecs[ii] < 0) throwerr("invalid dimSpecs < 0");
  }
  prtln("xLen: " + xLen);
  prtln("yLen: " + yLen);


  Array ncArray = ncVar.read();



  // Print the x,y,lat,lon at the 4 corners of the projection

  // SW corner
  prtCorner( bugs, "SW", ncArray, dimSpecs, missVal,
    earthRadiusKm, deltaX, deltaY, lat0, lon0, xorigin, yorigin,
    0,           // X cell index
    0);          // Y cell index
  // SE corner
  prtCorner( bugs, "SE", ncArray, dimSpecs, missVal,
    earthRadiusKm, deltaX, deltaY, lat0, lon0, xorigin, yorigin,
    xLen - 1,    // X cell index
    0);          // Y cell index
  // NW corner
  prtCorner( bugs, "NW", ncArray, dimSpecs, missVal,
    earthRadiusKm, deltaX, deltaY, lat0, lon0, xorigin, yorigin,
    0,           // X cell index
    yLen - 1);   // Y cell index
  // NE corner
  prtCorner( bugs, "NE", ncArray, dimSpecs, missVal,
    earthRadiusKm, deltaX, deltaY, lat0, lon0, xorigin, yorigin,
    xLen - 1,    // X cell index
    yLen - 1);   // Y cell index

  // Find overall min, max for testVarName
  double minVal = 1.e300;
  double maxVal = -1.e300;
  for (int ix = 0; ix < xLen; ix++) {
    for (int iy = 0; iy < yLen; iy++) {
      int[] dimVals = Arrays.copyOf( dimSpecs, dimSpecs.length);
      for (int ii = 0; ii < ndim; ii++) {
        if (dimVals[ii] == -1) dimVals[ii] = ix;
        else if (dimVals[ii] == -2) dimVals[ii] = iy;
        else if (dimVals[ii] < 0) throwerr("invalid dimSpecs < 0");
      }
      Index ncIndex = ncArray.getIndex();
      ncIndex.set( dimVals);
      double value = ncArray.getDouble( ncIndex);
      if (bugs >= 5) prtln("ncIndex: " + ncIndex + "  value: " + value);
      if (value != missVal) {
        if (value < minVal) minVal = value;
        if (value > maxVal) maxVal = value;
      }
    }
  }
  prtln("");
  prtln("Overall minimum value: " + minVal);
  prtln("Overall maximum value: " + maxVal);

  // Handle testLat, testLon
  if (testLat > -999 && testLon > -999) {
    double[] xyPair = forwardStereographic(
      earthRadiusKm,  // Spherical earth radius, km, e.g., 6367.470
      lat0,           // Projection origin lat, degrees, e.g, 90
      lon0,           // Projection origin lon, degrees, e.g, -105
      testLat,        // input latitude, degrees
      testLon);       // input longitude, degrees
    double xval = xyPair[0];
    double yval = xyPair[1];
    double xoffset = xval - xorigin;
    double yoffset = yval - yorigin;
    int ix = (int) (xoffset / deltaX);
    int iy = (int) (yoffset / deltaY);

    prtln("");
    prtln("testLat: " + testLat);
    prtln("  xval: " + xval + " meters from projection origin");
    prtln("  xoffset: " + xoffset + " meters from grid origin");
    prtln("  x coord index: " + ix + "  x coord length: " + xLen);

    prtln("");
    prtln("testLon: " + testLon);
    prtln("  yval: " + yval + " meters from projection origin");
    prtln("  yoffset: " + yoffset + " meters from grid origin");
    prtln("  y coord index: " + iy + "  y coord length: " + yLen);

    if (ix < 0 || ix >= xLen) prtln("ix is out of range");
    else if (iy < 0 || iy >= yLen) prtln("iy is out of range");
    else {
      int[] dimVals = Arrays.copyOf( dimSpecs, dimSpecs.length);
      for (int ii = 0; ii < ndim; ii++) {
        if (dimVals[ii] == -1) dimVals[ii] = ix;
        else if (dimVals[ii] == -2) dimVals[ii] = iy;
        else if (dimVals[ii] < 0) throwerr("invalid dimSpecs < 0");
      }
      Index ncIndex = ncArray.getIndex();
      ncIndex.set( dimVals);
      double value = ncArray.getDouble( ncIndex);

      String msg = "Missing";
      if (value != missVal) msg = "" + value;
      prtln("");
      prtln("Value at test point: " + msg);
    } // if ix, iy are valid
  } // if testLat > -999 && testLon > -999


} // end prtProjectionInfo






void prtCorner(
  int bugs,
  String msg,
  Array ncArray,         // Array of variable to be printed
  int[] dimSpecs,        // dimension specs
  double missVal,        // missing value
  double earthRadiusKm,  // Spherical earth radius, km, e.g., 6367.470
  double deltaX,         // Grid X spacing in meters
  double deltaY,         // Grid Y spacing in meters
  double lat0,           // Projection origin lat, degrees, e.g, 90
  double lon0,           // Projection origin lon, degrees, e.g, -105
  double xorigin,        // x value of grid origin, meters
  double yorigin,        // y value of grid origin, meters
  int ix,
  int iy)
throws Exception
{
  double[] resPair = inverseStereographic(    // latOut, lonOut in degrees
    earthRadiusKm,  // Spherical earth radius, km, e.g., 6367.470
    lat0,           // Projection origin lat, degrees, e.g, 90
    lon0,           // Projection origin lon, degrees, e.g, -105
    xorigin + ix * deltaX,   // input x value, meters
    yorigin + iy * deltaY);  // input y value, meters
  double lat = resPair[0];
  double lon = resPair[1];

  int ndim = dimSpecs.length;
  int[] dimVals = Arrays.copyOf( dimSpecs, dimSpecs.length);
  for (int ii = 0; ii < ndim; ii++) {
    if (dimVals[ii] == -1) dimVals[ii] = ix;
    else if (dimVals[ii] == -2) dimVals[ii] = iy;
    else if (dimVals[ii] < 0) throwerr("invalid dimSpecs < 0");
  }
  Index ncIndex = ncArray.getIndex();
  ncIndex.set( dimVals);
  double value = ncArray.getDouble( ncIndex);
  String valMsg = "Missing";
  if (value != missVal) valMsg = "" + value;

  prtln( String.format("%s corner: ix: %5d  iy: %5d  lat: %12.6f  lon: %12.6f value: %s",
    msg, ix, iy, lat, lon, valMsg));
} // end prtCorner










// Find the projection info
// See: http://www.emc.ncep.noaa.gov/mmb/ylin/pcpanl/QandA/#GRIDINFO
// Returns:
//   earthRadiusKm   // Spherical earth radius, km, 6367.470
//   deltaX          // Grid X spacing in meters
//   deltaY          // Grid Y spacing in meters
//   lat0            // Proj origin lat, degrees, e.g, 90
//   lon0            // Proj origin lon, degrees, e.g, -105
//   xorigin         // x value of grid origin, meters
//   yorigin         // y value of grid origin, meters

double[] getProjectionInfo(
  int bugs,
  Group rootGroup,
  String projVarName)
throws Exception
{
  Variable projectionVar = rootGroup.findVariable( projVarName);
  if (projectionVar == null) throwerr("Cannot find -projVar: " + projVarName);
  prtln("projectionVar: " + projectionVar);
  if (projectionVar == null) throwerr("projectionVar not found");

  // Get grid X spacing in meters
  Attribute deltaXAttr = projectionVar.findAttribute("GRIB_param_Dx");
  if (deltaXAttr == null) throwerr("deltaXAttr not found");
  double deltaX = deltaXAttr.getNumericValue().doubleValue();

  // Get grid Y spacing in meters
  Attribute deltaYAttr = projectionVar.findAttribute("GRIB_param_Dy");
  if (deltaYAttr == null) throwerr("deltaYAttr not found");
  double deltaY = deltaYAttr.getNumericValue().doubleValue();

  prtln("deltaXAttr: " + deltaXAttr
    + "  dataType: " + deltaXAttr.getDataType());
  prtln("deltaX: " + deltaX);
  prtln("deltaYAttr: " + deltaYAttr
    + "  dataType: " + deltaYAttr.getDataType());
  prtln("deltaY: " + deltaY);

  // Projection origin latitude is always at the north pole
  double lat0 = 90;    // projection origin, degrees
  prtln("lat0: " + lat0);

  // Get the projection origin longitude
  Attribute lon0Attr = projectionVar.findAttribute("GRIB_param_LoV");
  double lon0Raw = lon0Attr.getNumericValue().doubleValue();
  double lon0 = lon0Raw;
  if (lon0 > 180) lon0 -= 360;

  prtln("lon0Attr: " + lon0Attr
    + "  dataType: " + lon0Attr.getDataType());
  prtln("original lon0: " + lon0Raw + "  corrected: " + lon0);

  // Get the latitude of the grid origin (cell (1,1))
  Attribute latGridAttr = projectionVar.findAttribute("GRIB_param_La1");
  double latGrid = latGridAttr.getNumericValue().doubleValue();

  prtln("latGridAttr: " + latGridAttr
    + "  dataType: " + latGridAttr.getDataType());
  prtln("latGrid: " + latGrid);

  // Get the longitude of the grid origin (cell (1,1))
  Attribute lonGridAttr = projectionVar.findAttribute("GRIB_param_Lo1");
  double lonGridRaw = lonGridAttr.getNumericValue().doubleValue();
  double lonGrid = lonGridRaw;
  if (lonGrid > 180) lonGrid -= 360;

  prtln("lonGridAttr: " + lonGridAttr
    + "  dataType: " + lonGridAttr.getDataType());
  prtln("original lonGrid: " + lonGridRaw + "  corrected: " + lonGrid);

  // Get the earth radius in km
  // Some files use meters, others use km.
  Attribute earthRadiusAttr = projectionVar.findAttribute(
    "GRIB_param_grid_radius_spherical_earth");
  double earthRadiusRaw = earthRadiusAttr.getNumericValue().doubleValue();
  double earthRadiusKm = 0;
  if (earthRadiusRaw >= 6300 && earthRadiusRaw < 6400) {
    earthRadiusKm = earthRadiusRaw;          // already in km
  }
  else if (earthRadiusRaw >= 6300*1000 && earthRadiusRaw < 6400*1000) {
    earthRadiusKm = 0.001 * earthRadiusRaw;  // convert meters to km
  }
  else throwerr("unknown earthRadius units: " + earthRadiusRaw);

  prtln("earthRadiusAttr: " + earthRadiusAttr
    + "  dataType: " + earthRadiusAttr.getDataType());
  prtln("original earthRadius: " + earthRadiusRaw
    + "  corrected to km: " + earthRadiusKm);

  // Find the x,y in meters of the grid origin in this projection.
  double[] xyPair = forwardStereographic(
    earthRadiusKm,  // Spherical earth radius, km, e.g., 6367.470
    lat0,           // Projection origin lat, degrees, e.g, 90
    lon0,           // Projection origin lon, degrees, e.g, -105
    latGrid,        // grid origin lat = input lat value, degrees
    lonGrid);       // grid origin lon = input lon value, degrees
  double xorigin = xyPair[0];
  double yorigin = xyPair[1];
  prtln("xorigin: " + xorigin);
  prtln("yorigin: " + yorigin);

  return new double[] {
    earthRadiusKm,  // Spherical earth radius, km, 6367.470
    deltaX,         // Grid X spacing in meters
    deltaY,         // Grid Y spacing in meters
    lat0,           // Proj origin lat, degrees, e.g, 90
    lon0,           // Proj origin lon, degrees, e.g, -105
    xorigin,        // x value of grid origin, meters
    yorigin};       // y value of grid origin, meters
} // end getProjectionInfo



// Spherical north polar stereographic projection
// See:
//   Map Projections - A Working Manual
//   John P. Snyder
//   USGS Professional Paper 1395, published 1987
//   http://infotrek.er.usgs.gov/pubs/
//   Supt. of Docs No: 19.16:1395
//
// Given latIn, lonIn, find xx, yy.

double[] forwardStereographic(  // returns {xx, yy} in meters
  double earthRadiusKm,  // Spherical earth radius, km, e.g., 6367.470
  double lat0,           // Projection origin lat, degrees, e.g, 90
  double lon0,           // Projection origin lon, degrees, e.g, -105
  double latIn,          // input lat value, degrees
  double lonIn)          // input lon value, degrees
{
  double earthRadiusM = 1000 * earthRadiusKm;  // convert km to m
  // Set k0 so the scale factor at lat = 60 is 1.0
  double k0 = 1 / 1.07179676972;

  lat0 *= Math.PI / 180;        // convert degrees to radians
  lon0 *= Math.PI / 180;        // convert degrees to radians
  latIn *= Math.PI / 180;       // convert degrees to radians
  lonIn *= Math.PI / 180;       // convert degrees to radians
  double xx =  2 * earthRadiusM * k0 * Math.tan( Math.PI/4 - latIn/2)
    * Math.sin(lonIn - lon0);
  double yy = -2 * earthRadiusM * k0 * Math.tan( Math.PI/4 - latIn/2)
    * Math.cos(lonIn - lon0);
  return new double[] {xx, yy};
}





// Inverse spherical north polar stereographic projection.
// See doc for forwardStereographic().
// Given xx, yy, find latOut, lonOut

double[] inverseStereographic(    // returns {latOut, lonOut} in meters
  double earthRadiusKm,  // Spherical earth radius, km, e.g., 6367.470
  double lat0,           // Projection origin lat, degrees, e.g, 90
  double lon0,           // Projection origin lon, degrees, e.g, -105
  double xx,             // input x value, meters
  double yy)             // input y value, meters
{
  double earthRadiusM = 1000 * earthRadiusKm;  // convert km to m
  // Set k0 so the scale factor at lat = 60 is 1.0
  double k0 = 1 / 1.07179676972;

  lat0 *= Math.PI / 180;        // convert degrees to radians
  lon0 *= Math.PI / 180;        // convert degrees to radians
  double rho = Math.sqrt( xx * xx + yy * yy);
  double cc = 2 * Math.atan( rho / (2 * earthRadiusM * k0));

  double tmpa = Math.cos(cc) * Math.sin(lat0);
  double tmpb = yy * Math.sin(cc) * Math.cos(lat0) / rho;
  double latOut = Math.asin( tmpa + tmpb);
  double lonOut = lon0 + Math.atan2( xx, -yy);
  latOut *= 180 / Math.PI;    // convert radians to degrees
  lonOut *= 180 / Math.PI;    // convert radians to degrees
  return new double[] {latOut, lonOut};
}







void prtindent( int indent, String msg) {
  String pad = "";
  for (int ii = 0; ii < indent; ii++) {
    pad += "  ";
  }
  System.out.println( pad + msg);
}


static void throwerr( String msg)
throws Exception
{
  throw new Exception( msg);
}


void prtln( String msg) {
  System.out.println( msg);
}


String formatIntVec( int[] vec) {
  String res = "[";
  for (int ival : vec) {
    res += " " + ival;
  }
  res += " ]";
  return res;
}


} // end class

