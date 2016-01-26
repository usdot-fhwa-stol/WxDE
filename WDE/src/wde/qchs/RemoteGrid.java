package wde.qchs;

import wde.util.MathUtil;

import ucar.nc2.FileWriter2;
import ucar.nc2.NetcdfFile;
import ucar.nc2.NetcdfFileWriter.Version;
import ucar.nc2.constants.DataFormatType;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.write.Nc4ChunkingDefault;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;


/**
 * This abstract base class implements common NetCDF patterns for identifying,
 * downloading, reading, and retrieving observation values for remote data sets.
 */
abstract class RemoteGrid implements Runnable {
    /**
     * Lookup arrays map names between model and observation types.
     */
    protected int[] m_nObsTypes;
    protected int[] m_nGridMap;
    protected String m_sBaseURL;
    protected String[] m_sObsTypes;
    protected List<GridDatatype> m_oGrids;
    protected GridDataSource m_oGridDataSource;

    /**
     * Default package private constructor.
     */
    RemoteGrid() {
        try {
            init();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Path getIncomingStoragePath() {
        return getStoragePath().resolve("incoming").toAbsolutePath();
    }

    public Path getDatasetsStoragePath() {
        return getStoragePath().resolve("data").toAbsolutePath();
    }

    public abstract Path getStoragePath();

    public abstract URL getDatasetBaseUrl() throws MalformedURLException;

    protected synchronized void init() {
        config();

        try {
            //initGridDataSource();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected synchronized void initGridDataSource() throws Exception {
        Path datasetPath = getGridDatasetPath();
        if (Files.notExists(datasetPath)) {
            //throw new IllegalArgumentException("The configured grid dataset path does not exist: " + );
            throw new FileNotFoundException(datasetPath.toString());
        }

        m_oGridDataSource = GridDataSource.fromLocation(datasetPath);
    }

    protected synchronized void config() {
        /* pull values from configsvc here */
    }

    /**
     * Abstract method overridden by subclasses to determine the remote and local
     * file name for their specific remote data set.
     *
     * @param oNow timestamp Date object used for time-based dynamic URLs
     * @return the URL where remote data can be retrieved.
     */
    protected abstract String getFilename(Date oNow);

    protected abstract String getFilenameUrl(Date oNow);

    protected GregorianCalendar getNow() {
        GregorianCalendar oNow = new GregorianCalendar();
        if (oNow.get(Calendar.MINUTE) < 55) // backup to previous hour
            oNow.setTimeInMillis(oNow.getTimeInMillis() - 3600000);
        oNow.set(Calendar.MILLISECOND, 0); // adjust clock to scheduled time
        oNow.set(Calendar.SECOND, 0);
        oNow.set(Calendar.MINUTE, 55);

        return oNow;
    }

    /**
     * Regularly called on a schedule to refresh the cached model data with
     * the most recently published model file.
     */
    //@Override
    public void run() {
        GregorianCalendar oNow = getNow();

        Path datasetFilePath = null;
        try {
            final String sFilenameUrl = getFilenameUrl(oNow.getTime());
            final String sFilename = getFilename(oNow.getTime());

            final Path incomingFolder = getIncomingStoragePath();
            if (!Files.exists(incomingFolder)) {
                Files.createDirectories(incomingFolder);
            }

            final Path datasetsFolder = getDatasetsStoragePath();
            if (!Files.exists(datasetsFolder)) {
                Files.createDirectories(datasetsFolder.toAbsolutePath());
            }

            final Path incomingFilePath = incomingFolder.resolve(sFilename).toAbsolutePath();
            Files.createDirectories(incomingFilePath.getParent());

            if (Files.notExists(incomingFilePath)) {
                //final URL oUrl = new URL(m_sBaseURL + sFilename); // retrieve remote data file
                final URL oUrl = new URL(getDatasetBaseUrl(), sFilenameUrl);
                final BufferedInputStream oIn = new BufferedInputStream(oUrl.openStream());
                final BufferedOutputStream oOut = new BufferedOutputStream(
                        new FileOutputStream(incomingFilePath.toString()));
                int nByte; // copy remote data to local file
                while ((nByte = oIn.read()) >= 0)
                    oOut.write(nByte);
                oIn.close(); // tidy up input and output streams
                oOut.close();
            }

            if (Files.exists(incomingFilePath)) {
                final Path relativeFilePath = incomingFolder.relativize(incomingFilePath);
                if (relativeFilePath.getParent() != null) {
                    Files.createDirectories(Paths.get(datasetsFolder.toString(), relativeFilePath.getParent().toString()));
                }

                final String filename = relativeFilePath.getFileName().toString()
                        .replaceAll(".gz$", "")
                        .replaceAll(".grib2$", ".nc")
                        .replaceAll(".grb2$", ".nc");

                final Path outputFilePath = datasetsFolder.resolve(relativeFilePath)
                        .getParent()
                        .resolve(filename)
                        .toAbsolutePath();

                if (NetcdfFile.canOpen(incomingFilePath.toString())) {
                    final NetcdfFile netcdfFile = NetcdfFile.open(incomingFilePath.toString());
                    if (netcdfFile.getFileTypeId().equals(DataFormatType.GRIB2.getDescription())) {
                        convertToNetcdf4(netcdfFile, outputFilePath.toString());
                        if (Files.notExists(outputFilePath)) {
                            throw new Exception("An issue was encountered after converting the radar file: Output file not found");
                        }

                        datasetFilePath = outputFilePath;

                        final String incomingFileBaseName = incomingFilePath.getFileName().toString()
                                .replaceAll(".gz$", "");
                        final Path filesToRemove = Paths.get(incomingFolder.toString(), incomingFileBaseName + "*");
                        Files.deleteIfExists(filesToRemove);
                    }
                    netcdfFile.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            final GridDataSource gridDataSource = getGridDataSource();
            if (gridDataSource == null) {
                throw new Exception("Grid data source could not be resolved.");
            }

            gridDataSource.purge();

            if (datasetFilePath != null && Files.exists(datasetFilePath))
                gridDataSource.sync();

            final GridDataset oGridDataset = gridDataSource.getGridDataset();
            if (oGridDataset == null)
                throw new Exception("Could not resolve grid datatype.");

            final int[] nGridMap = new int[m_nObsTypes.length]; // create obstype grid index map
            final List<GridDatatype> oGrids = oGridDataset.getGrids();
            for (int nOuter = 0; nOuter < nGridMap.length; nOuter++) {
                nGridMap[nOuter] = -1; // default to invalid index
                for (int nInner = 0; nInner < oGrids.size(); nInner++) {
                    if (m_sObsTypes[nOuter].contains(oGrids.get(nInner).getName()))
                        nGridMap[nOuter] = nInner; // save name mapping
                }
            }

            synchronized (this) // update state variables when everything succeeds
            {
                m_oGrids = oGrids;
                m_nGridMap = nGridMap;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieves the grid data associated with supported observation types.
     *
     * @param nObsTypeId the observation type identifier used to find grid data.
     * @return the grid data for the variable specified by observation type.
     */
    protected GridDatatype getGridByObs(int nObsTypeId) throws Exception {
        boolean bFound = false;
        int nIndex = m_nObsTypes.length;
        while (!bFound && nIndex-- > 0)
            bFound = (m_nObsTypes[nIndex] == nObsTypeId);

        if (!bFound || m_nGridMap[nIndex] < 0) // requested obstype not available
            return null;

        return m_oGrids.get(m_nGridMap[nIndex]); // grid by obstypeid
    }

    public synchronized double getReading(int nObsTypeId, long lTimestamp, int nLat, int nLon) {
        return getReading(nObsTypeId, lTimestamp, MathUtil.fromMicro(nLat), MathUtil.fromMicro(nLon));
    }

    /**
     * Finds the model value for an observation type by time and location.
     *
     * @param nObsTypeId the observation type to lookup.
     * @param lTimestamp the timestamp of the observation.
     * @param nLat       the latitude of the requested data.
     * @param nLon       the longitude of the requested data.
     * @return the model value for the requested observation type for the
     * specified time at the specified location.
     */
    public synchronized double getReading(int nObsTypeId, long lTimestamp, double nLat, double nLon) {
        try {
            final GridDatatype oGrid = getGridByObs(nObsTypeId);
            if (oGrid == null) {
                throw new NullPointerException("The requested observation type is not supported.");
            }

            final GridCoordSystem gcs = oGrid.getCoordinateSystem();
            if (gcs == null) {
                throw new NullPointerException("The coordinate system for the grid wasn't able to be resolved.");
            }

            final CalendarDate timestamp = CalendarDate.of(lTimestamp);
            final int[] index = getGridDataSource().buildIndex(oGrid, timestamp, nLat, nLon);

            return getGridDataSource().readValue(oGrid, index);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Double.NaN;
    }

    protected synchronized GridDataSource getGridDataSource() {
        if (m_oGridDataSource == null) {
            try {
                initGridDataSource();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return m_oGridDataSource;
    }

    protected abstract Path getGridDatasetPath();

    protected void convertToNetcdf4(NetcdfFile netcdfFile, String outputLocation) throws Exception {
        try {
            FileWriter2 writer = new FileWriter2(netcdfFile, outputLocation, Version.netcdf4, new Nc4ChunkingDefault());
            writer.write();
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception("An exception occurred while converting the file: " + netcdfFile.getLocation(), e);
        }
    }

    public void testDatasetBaseUrlConcatentation() {
        try {
            System.out.println(new URL(getDatasetBaseUrl(), "filename.grib2"));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }
}
