package wde.radar;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class Radar implements Runnable
{
    //private final Logger logger = Logger.getLogger(this.getClass());

    public static final String MRMS_MERGEDBASEREFLECTIVITY_URL = "http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/";
    public static final String MRMS_MERGEDREFLECTIVITYQC_GRID_NAME = "MergedBaseReflectivityQC_altitude_above_msl";

    private wde.radar.MrmsRadarFileRetriever mrmsRadarFileRetriever;

    private static final Radar g_oRadar = new Radar();

    private GridDatatype m_oGrid;
    private long m_lStartTime;
    private long m_lEndTime;
    private ArrayList<Double> m_oLongitude = new ArrayList();
    private ArrayList<Double> m_oLatitude = new ArrayList();
    private Variable reflectivityVar;

    private Radar()
    {
        run();
//		Scheduler.getInstance().schedule(this, 60 * 55, 3600, true);
    }

    public String getLatestRadarFileUrl() throws IOException {
        if (this.mrmsRadarFileRetriever == null) {
            this.mrmsRadarFileRetriever = new wde.radar.MrmsRadarFileRetriever(MRMS_MERGEDBASEREFLECTIVITY_URL);
        }

        return this.mrmsRadarFileRetriever.getNewestRadarFileUrl();
    }

    public static Radar getInstance()
    {
        return g_oRadar;
    }

    private static ArrayList<Double> fromDoubleArray(NetcdfFile oNcFile, String sName)
            throws Exception
    {
        Array oArray = oNcFile.getRootGroup().findVariable(sName).read();
        int nSize = (int)oArray.getSize();

        ArrayList<Double> oArrayList = new ArrayList(nSize); // reserve capacity
        for (int nIndex = 0; nIndex < nSize; nIndex++)
            oArrayList.add(oArray.getDouble(nIndex)); // copy values

        Collections.sort(oArrayList);

        return oArrayList;
    }

    public void run()
    {
        long lTempStart = System.currentTimeMillis();
        Date oDate = new Date(lTempStart - 55 * 60000); // run 55 minutes late

        try
        {
            URI oUri = new URI(getLatestRadarFileUrl());
            NetcdfFile oNcFile = NetcdfFile.openInMemory(oUri);
            //NetcdfFile oNcFile = NetcdfFile.openInMemory("MRMS_MergedBaseReflectivityQC_00.00_20151215-053418.grib2");

            ArrayList<Double> oLongitude = fromDoubleArray(oNcFile, "lon"); // sorted low to high
            ArrayList<Double> oLatitude = fromDoubleArray(oNcFile, "lat");

            GridDatatype oGrid = null;

            //println("-85.465-> " + oLongitude.contains(-85.46501f));
            //println("26.225-> " + oLatitude.contains(26.225));
            //println(Arrays.toString(oLongitude.toArray()));
            //println(Arrays.toString(oLatitude.toArray()));

            List<GridDatatype> oGrids = new GridDataset(new NetcdfDataset(oNcFile)).getGrids();
            for(GridDatatype grid : oGrids) {
                if (grid.getName().equalsIgnoreCase(MRMS_MERGEDREFLECTIVITYQC_GRID_NAME)) {
                    oGrid = grid;
                    break;
                }
            }

            synchronized(this) // update state variables when everything succeeds
            {
                m_oLongitude = oLongitude;
                m_oLatitude = oLatitude;
                m_oGrid = oGrid;
                m_lStartTime = lTempStart;
                m_lEndTime = lTempStart + 3900000; // data are valid for 65 minutes
            }
        }
        catch(Exception e) // failed to download new data
        {
            e.printStackTrace();
        }
    }

    public void println(String s) {
        System.out.println(s);
    }

    public synchronized float getReflectivity(int lon, int lat) {

//        if (m_oGrid == null) {
//            return Double.NaN;
//        }
//
//        LatLonPointImpl oLatLon = new LatLonPointImpl(lat, lon);
//        ProjectionPointImpl oProjPoint = new ProjectionPointImpl();
//        m_oGrid.getProjection().latLonToProj(oLatLon, oProjPoint);
//
//        println("lon=" + lon + ", lat=" + lat);
//        println("lon_lower= " + m_oLongitude.get(0) + ", lon_upper=" + m_oLongitude.get(m_oLongitude.size() - 1));
//        Double oLongitude = lon; //oLatLon.getLongitude();
//        if (oLongitude < m_oLongitude.get(0) || oLongitude > m_oLongitude.get(m_oLongitude.size() - 1)) {
//            println("longitude: " + oLongitude);
//
//            println("Longitude fell outside of valid range.");
//            return Double.NaN; // projected horizontal coordinate outside data ranage
//        }
//
//        Double oLatitude = lat; //oLatLon.getLatitude();
//        println("lat_lower= " + m_oLatitude.get(0) + ", lat_upper=" + m_oLatitude.get(m_oLatitude.size() - 1));
//        if (oLatitude < m_oLatitude.get(0) || oLatitude > m_oLatitude.get(m_oLatitude.size() - 1)) {
//            println("Latitude fell outside of valid range.");
//            return Double.NaN; // projected vertical coordinate outside data ranage
//        }
//
//        int nLon = Collections.binarySearch(m_oLongitude, oLongitude);
//        if (nLon < 0) { // get twos-complement when no exact match
//            println("Lon was not found (" + oLongitude + "): " + nLon + ", " + ~nLon);
//            nLon = ~nLon;
//
//        }
//
//        int nLat = Collections.binarySearch(m_oLatitude, oLatitude);
//        if (nLat < 0) { // get twos-complement when no exact match
//            println("Lat was not found: " + nLat + ", " + ~nLat);
//            nLat = ~nLat;
//        }
//
//        try
//        {
//
//            VariableDS oVar = m_oGrid.getVariable();
//            println(oVar.getDimensionsString());
//            Array oArray = oVar.read();
//            Index oIndex = oArray.getIndex();
//
//            oIndex.setDim(oVar.findDimensionIndex("lon"), nLon);
//            oIndex.setDim(oVar.findDimensionIndex("lat"), nLat);
//            oIndex.setDim(oVar.findDimensionIndex("time"), 0);
//            oIndex.setDim(oVar.findDimensionIndex("altitude_above_msl"), 0);
//
//            Double dVal = oArray.getDouble(oIndex);
//            if (oVar.isFillValue(dVal) || oVar.isInvalidData(dVal) || oVar.isMissing(dVal))
//                return Double.NaN; // no valid data for specified location
//
//            return dVal;
//        }
//        catch (Exception e)
//        {
//            e.printStackTrace();
//        }

        return -9999.0f;
    }

    public static void main(String[] sArgs)
            throws Exception
    {
        Radar oRadar = Radar.getInstance();


    }
}