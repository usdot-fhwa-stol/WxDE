package wde.qchs;

import ucar.nc2.Variable;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.time.CalendarPeriod;
import ucar.nc2.time.CalendarPeriod.Field;

import java.io.BufferedInputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Radar extends RemoteGrid {

    private static final Radar g_oRadar = new Radar();

    private final Matcher m_oMatcher = Pattern.compile(
            "(MRMS_MergedBaseReflectivityQC_00\\.00_[0-9]{8}-[0-9]{6}\\.grib2\\.gz)")
            .matcher("");

    private Radar() {
        super();

        m_nObsTypes = new int[]{0};
        m_sObsTypes = new String[]{"MergedBaseReflectivityQC_altitude_above_msl"};
        m_sBaseURL = "http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/";

        run(); // manually initialize first run, then set schedule
//		Scheduler.getInstance().schedule(this, 60 * 55, 3600, true);
    }

    @Override
    public Path getStoragePath() {
        return Paths.get("/Users/jschultz/Source/wxde/WDE/data/radar/MergedBaseReflectivityQC").toAbsolutePath();
    }

    @Override
    public URL getDatasetBaseUrl() throws MalformedURLException {
        return new URL("http://mrms.ncep.noaa.gov/data/2D/MergedBaseReflectivityQC/");
    }

    public static Radar getInstance() {
        return g_oRadar;
    }

    public static void main(String[] sArgs) throws Exception {

        Radar oRadar = Radar.getInstance();

        oRadar.testDatasetBaseUrlConcatentation();

        PrintWriter pw = new PrintWriter(System.out, true);

        GridDatatype grid = oRadar.getGridByObs(0);
        GridCoordSystem gcs = grid.getCoordinateSystem();

        CalendarDate now = CalendarDate.present().subtract(CalendarPeriod.of(10, Field.Hour));
        CalendarDate timestamp = CalendarDate.parseISOformat(null, "2016-04-30T04:08:01Z");

        int lat = 43000000;
        int lon = -94000000;

        System.out.println(oRadar.getReading(0, timestamp.getMillis(), lat, lon));

        GridDataSource gridDataSource = oRadar.getGridDataSource();
        gridDataSource.test();
        gridDataSource.shutdown();
    }

    public void dump() throws Exception {
        PrintWriter pw = new PrintWriter(System.out, true);
        NetcdfDataset nc = getGridDataSource().getNetcdfDataset();
        if (nc == null)
            throw new Exception("Could not get NetcdfDataset.");

        NetcdfDataset.debugDump(pw, nc);

        for(Variable variable : nc.getVariables()) {
            pw.println("Variable name=" + variable.getShortName() +
                    " shape=" + Arrays.toString(variable.getShape()) +
                    " size=" + variable.getSize());
        }

        for (CoordinateAxis axis : nc.getCoordinateAxes()) {
            pw.println("Axis name: " + axis.getName() + " size: " + axis.getSize());
        }

        nc.close();
    }

    /**
     * This method is used to determine the remote filename. It first downloads
     * the base URL as the file index and then iterates to find the latest name.
     *
     * @param oNow timestamp Date object used for time-based dynamic URLs
     * @return the name of the remote data file.
     */
    @Override
    protected String getFilename(Date oNow) {
        String sFilename = null;
        StringBuilder sIndex = new StringBuilder();
        try {
            URL baseURL = getDatasetBaseUrl();

            BufferedInputStream oIn = new BufferedInputStream(baseURL.openStream());
            int nByte; // copy remote file index to buffer
            while ((nByte = oIn.read()) >= 0)
                sIndex.append((char) nByte);
            oIn.close();

            m_oMatcher.reset(sIndex);
            while (m_oMatcher.find()) // desired file name should be last match
                sFilename = m_oMatcher.group(1);
        } catch (Exception oException) {
        }

        return sFilename;
    }

    @Override
    protected String getFilenameUrl(Date oNow) {
        return getFilename(oNow);
    }

    /**
     * Retrieves the current radar grid data.
     *
     * @param nObsTypeId the observation type identifier used to find grid data.
     * @return the grid data for the variable specified by observation type.
     */
    @Override
    protected GridDatatype getGridByObs(int nObsTypeId) throws Exception {
        return super.getGridByObs(0);
    }

    @Override
    protected Path getGridDatasetPath() {
        return Paths.get("./data/radar.ncml").toAbsolutePath();
    }
}
