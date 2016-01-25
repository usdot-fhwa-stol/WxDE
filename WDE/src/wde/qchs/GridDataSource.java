package wde.qchs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ucar.ma2.Array;
import ucar.ma2.Index;
import ucar.ma2.IndexIterator;
import ucar.nc2.constants.FeatureType;
import ucar.nc2.dataset.CoordinateAxis;
import ucar.nc2.dataset.CoordinateAxis1DTime;
import ucar.nc2.dataset.NetcdfDataset;
import ucar.nc2.dataset.VariableDS;
import ucar.nc2.dt.GridCoordSystem;
import ucar.nc2.dt.GridDataset.Gridset;
import ucar.nc2.dt.GridDatatype;
import ucar.nc2.dt.grid.GeoGrid;
import ucar.nc2.dt.grid.GridCoordSys;
import ucar.nc2.dt.grid.GridDataset;
import ucar.nc2.ft.FeatureDataset;
import ucar.nc2.ft.FeatureDatasetFactoryManager;
import ucar.nc2.ncml.Aggregation;
import ucar.nc2.ncml.Aggregation.Dataset;
import ucar.nc2.time.CalendarDate;
import ucar.nc2.util.DiskCache2;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GridDataSource {

    private static final String FEATURE_TYPE = "featureType";
    private static final String FEATURE_TYPE_GRID = "GRID";

    private enum _ {}
    private static final Logger LOGGER = LoggerFactory.getLogger(_.class.getEnclosingClass());
    private final DiskCache2 cache;
    private final NetcdfDataset netcdfDataset;
    private Path cacheDirPath;
    private GridDataset gridDataset;


    protected GridDataSource(NetcdfDataset netcdfDataset, GridDataset gridDataset) {
        if (netcdfDataset == null)
            throw new IllegalArgumentException("netcdfDataset");

        if (gridDataset == null)
            throw new IllegalArgumentException("gridDataset");

        this.netcdfDataset = netcdfDataset;
        this.gridDataset = gridDataset;

        this.cacheDirPath = Paths.get("/tmp/griddatasource-cache/");
        this.cache = new DiskCache2(cacheDirPath.toString(), true, 60 * 24 * 30, 60);
        Aggregation.setPersistenceCache(cache);

        init();
    }

    protected GridDataSource(NetcdfDataset netcdfDataset) {
        if (netcdfDataset == null)
            throw new IllegalArgumentException("netcdfDataset");

        this.netcdfDataset = netcdfDataset;

        this.cacheDirPath = Paths.get("/tmp/griddatasource-cache/");
        this.cache = new DiskCache2(cacheDirPath.toString(), true, 60 * 24 * 30, 60);
        Aggregation.setPersistenceCache(cache);

        init();
    }

    private static Logger getLogger() {
        return LOGGER;
    }

    public static GridDataSource fromLocation(Path location) throws Exception {
        GridDataSource dataSource = null;

        try {
            NetcdfDataset nc = openDataset(location);

            dataSource = GridDataSource.fromDataset(nc);
        } catch (Exception e) {
            getLogger().error("An exception occurred while attempting to create a GridDataSource from location.", e);
            throw e;
        }

        return dataSource;
    }

    public static GridDataSource fromDataset(NetcdfDataset dataset) throws Exception {

        if (!isGridFeatureType(dataset)) {
            throw new Exception("No grid datasets found in file.");
        }

        GridDataSource dataSource = null;
        try {
            NetcdfDataset.setUseNaNs(true);

            GridDataset gridDS = resolveGridDataset(dataset);
            if (gridDS == null) {
                dataSource = new GridDataSource(dataset);
            } else {
                dataSource = new GridDataSource(dataset, gridDS);
            }
        } catch (IOException e) {
            getLogger().error("An exception occured while attempting to create a GridDataSource from file.", e);
        }

        return dataSource;
    }

    private static GridDataset resolveGridDataset(NetcdfDataset dataset) throws Exception {
        FeatureDataset featureDS = FeatureDatasetFactoryManager.wrap(FeatureType.GRID, dataset, null, null);
        if (featureDS == null) {
            throw new Exception("No grid feature types found in dataset.");
        }

        FeatureType featureType = featureDS.getFeatureType();
        assert (featureType == FeatureType.GRID);

        if (!(featureDS instanceof GridDataset)) {
            throw new Exception("The resolved feature type is not a grid. This might be a coordinate system inference problem.");
        }

        return (GridDataset) featureDS;
    }

    private static boolean isGridFeatureType(NetcdfDataset dataset) throws Exception {
        if (dataset == null)
            throw new Exception("The provided dataset object is null.");

        String featureType = dataset.findGlobalAttribute(FEATURE_TYPE).getStringValue();

        return FEATURE_TYPE_GRID.equals(featureType);
    }

    public static NetcdfDataset openDataset(Path location) throws Exception {

        NetcdfDataset nc = null;

        if (Files.notExists(location)) {
            throw new Exception("The location " + location + " doesn't refer to any existing files.");
        }

        nc = NetcdfDataset.acquireDataset(location.toString(), true, null);

        return nc;
    }

    private static long getDateDiff(Date date1, Date date2, TimeUnit timeUnit) {
        long diffInMillies = date2.getTime() - date1.getTime();
        return timeUnit.convert(diffInMillies, TimeUnit.MILLISECONDS);
    }

    protected CalendarDate findClosestAvailableTimestamp(GridCoordSystem gcs, CalendarDate timestamp) throws Exception {
        CalendarDate useTimestamp = null;
        for(CalendarDate currentTimestamp : gcs.getCalendarDates()) {
            if (useTimestamp == null) {
                useTimestamp = currentTimestamp;
            }

            if (useTimestamp.getDifferenceInMsecs(timestamp) > currentTimestamp.getDifferenceInMsecs(timestamp)) {
                useTimestamp = currentTimestamp;
            }
        }

        if (useTimestamp == null) {
            throw new Exception("No timestamps were found in dataset.");
        }

        //return gcs.getTimeAxis1D().findTimeIndexFromCalendarDate(useTimestamp);
        return useTimestamp;
    }

    public static int[] findClosestAxis1DCoordIndex(GridCoordSys gcs, double lat, double lon) throws Exception {
        //Array data = gridVar.read(new int[]{0, 0, 0, 0}, new int[]{0, 0, 0, 1});

        CoordinateAxis latAxis = gcs.getLatAxis();
        if (latAxis == null) {
            throw new Exception("Latitude axis could not be resolved.");
        }

        CoordinateAxis lonAxis = gcs.getLonAxis();
        if (lonAxis == null) {
            throw new Exception("Longitude axis could not be resolved");
        }

        final Array data = latAxis.read();
        final Index useLatIndex = Index.factory(data.getShape());
        Float useLatValue = data.getFloat(useLatIndex);

        IndexIterator iterator = data.getIndexIterator();
        while (iterator.hasNext()) {
            Float value = iterator.getFloatNext();
            if (Math.abs(useLatValue - lat) < Math.abs(useLatValue - lat)) {
                useLatIndex.set(iterator.getCurrentCounter());
                useLatValue = value;
            }
        }

        final Index useLonIndex = Index.factory(data.getShape());
        //Float useLonVa


        int[] index = new int[] { };
//        try {
//            //Array data = grid.readDataSlice(0, 0, -1, -1);
//            //IndexIterator it = data.getIndexIterator();
////            while(it.hasNext()) {
////                Float value = (Float) it.next();
////                if (!Float.isNaN(value)) {
////                    System.out.println(Arrays.toString(it.getCurrentCounter()) + ": " + it.getFloatCurrent());
////                }
////            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }

        return index;
    }

    /**
     * Used to shutdown background threads related to using the Netcdf file caching functionality
     * that would otherwise keep the process from normal termination.
     * <p/>
     * This is to be called during shutdown of the process.
     */
    public void shutdown() {
        if (cache != null) {
            cache.cleanCache(cacheDirPath.toFile(), new StringBuilder(), true);
            shutdownCache();
        }
    }

    protected synchronized void startCache() {
        getLogger().debug("Started Netcdf file cache.");

        if (Files.notExists(cacheDirPath)) {
            try {
                Files.createDirectories(cacheDirPath);

                Runtime.getRuntime().addShutdownHook(new Thread() {

                    @Override
                    public void run() {
                        getLogger().info("Shutting down Netcdf cache.");
                        shutdown();
                    }
                });

                NetcdfDataset.initNetcdfFileCache(100, 200, 15 * 60);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        getLogger().debug("Netcf file cache has been started.");
    }

    protected synchronized void shutdownCache() {
        cache.exit();
        NetcdfDataset.shutdown();
    }

    private void config() {
        /* get configuration values here */
        //cacheDirPath = "/tmp/griddatasource-cache/";
    }

    protected synchronized void init() {
        config();
        startCache();
    }

    public synchronized GridDataset getGridDataset() {
        if (this.gridDataset == null) {
            try {
                this.gridDataset = resolveGridDataset(this.netcdfDataset);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return this.gridDataset;
    }

    public synchronized NetcdfDataset getNetcdfDataset() {
        return this.netcdfDataset;
    }

    public void close() throws IOException {
        if (gridDataset != null)
            gridDataset.close();
    }

    public void open() throws IOException {
        if (gridDataset != null)
            gridDataset.reacquire();
    }

    public synchronized void sync() {
        try {
            NetcdfDataset nc = getNetcdfDataset();
            Aggregation agg = nc.getAggregation();

            agg.finish(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void purge() {
        Aggregation aggregation = getNetcdfDataset().getAggregation();
        List<Dataset> datasets = aggregation.getDatasets();
        boolean syncNeeded = false;
        for (Dataset dataset : datasets) {
            File datasetFile = new File(dataset.getLocation());
            if (datasetFile.exists()) {
                long lastModified = datasetFile.lastModified();
                Date lastModifiedDate = new Date(lastModified);
                Date todaysDate = new Date();

                if (getDateDiff(lastModifiedDate, todaysDate, TimeUnit.DAYS) > 30) {
                    syncNeeded = true;
                    try {
                        datasetFile.delete();
                    } catch (Exception e) {
                        datasetFile.deleteOnExit();
                        e.printStackTrace();
                    }
                }
            }
        }

        if (syncNeeded) {
            sync();
        }
    }

    /**
     * Reads the value from the dataset at the specified index.
     *
     * TODO: Implement nearest neighbor selection using a kDTree when a NaN is found to return a local non-NaN value.
     *
     * @param grid
     * @param indices
     * @return
     * @throws Exception
     */
    public Double readValue(GridDatatype grid, int[] indices) throws Exception {
        final VariableDS gridVariableDS = grid.getVariable();

        final Array array = grid.readDataSlice(indices[0], indices[1], indices[2], indices[3]);

        final Double dValue = array.getDouble(0);
        if (gridVariableDS.isFillValue(dValue) || gridVariableDS.isInvalidData(dValue) || gridVariableDS.isMissing(dValue))
            return Double.NaN;

        return dValue;
    }

    public int[] buildIndex(GridDatatype grid, CalendarDate timestamp, double lat, double lon) throws Exception {

        if (grid == null)
            throw new NullPointerException("grid");

        final GridCoordSystem gcs = grid.getCoordinateSystem();

        //
        // The radar datasets only provide a single instant in time for the time axis,
        // so if the date and time are not exactly equal, they will not be matched up. It is
        // unlikely that this will ever happen, so if the timestamp is not found on the time
        // axis, then find the closest available timestamp for use in building the index.
        //
        if (!gcs.getCalendarDateRange().includes(timestamp)) {
            timestamp = findClosestAvailableTimestamp(gcs, timestamp);
        }

        if (!gcs.getLatLonBoundingBox().contains(lat, lon)) {
            throw new Exception("Coordinates are outside of available data range.");
        }

        //
        // If a time axis doesn't exist within the current grid then provide '-1'
        // as the index for the time dimension. This tells netcdf to use any and all
        // indexes of the outer dimension, even if there aren't any.
        //
        int timeAxisIndex = -1;
        if (gcs.hasTimeAxis1D()) {
            final CoordinateAxis1DTime timeAxis = gcs.getTimeAxis1D();
            timeAxisIndex = timeAxis.findTimeIndexFromCalendarDate(timestamp);
        }

        //
        // Use the (lat,lon) pair to determine the grid cell the coordinates fall within. There
        // isn't a need to handle any sort of fuzzy coordinate search since we are using a
        // Grid Coordinate System, whose dimensions are all connected, meaning that neighbors in
        // index space are connected neighbors in coordinate space. This means that data values
        // that are close to each other in the real world (coordinate space) are close to each
        // other in the data array.
        //
        // All (lat,lon) pairs will belong to a cell within the grid.
        //
        int[] xy = gcs.findXYindexFromLatLonBounded(lat, lon, null);

        final VariableDS oVar = grid.getVariable();
        int[] indexArray = new int[oVar.getShape().length];
        indexArray[grid.getTimeDimensionIndex()] = timeAxisIndex;
        indexArray[grid.getZDimensionIndex()] = -1; // use all z-dimension indices
        indexArray[grid.getYDimensionIndex()] = xy[1];
        indexArray[grid.getXDimensionIndex()] = xy[0];

        return indexArray;
    }

    /**
     * Returns a reference to the {@link ucar.nc2.dt.GridDatatype} that has the coordinate system with the
     * highest rank value. The higher the rank value, the higher the dimensions of the coordinate,
     * and more likely the representative {@link ucar.nc2.dt.GridDatatype} of the {@link ucar.nc2.dt.GridDataset}.
     *
     * @return the {@link ucar.nc2.dt.GridDatatype} most likely to be representative of the {@link ucar.nc2.dt.GridDataset}.
     * @throws Exception
     */
    public GridDatatype getGrid() throws Exception {
        GridDatatype useGrid = null;
        for(Gridset gridset : getGridDataset().getGridsets()) {
            if (gridset.getGeoCoordSystem() instanceof GridCoordSys) {
                for(GridDatatype grid : gridset.getGrids()) {
                    if (useGrid == null) {
                        useGrid = grid;
                        continue;
                    }

                    if (useGrid.getRank() < grid.getRank()) {
                        useGrid = grid;
                    }
                }
            }
        }

        if (useGrid == null) {
            throw new Exception("Grid not found.");
        }

        return useGrid;
    }

    /**
     * Finds the first value within the grid. The value must exist within the
     * first time-dimension and z-dimension index, otherwise and NaN will be
     * returned.
     *
     * @param grid
     * @return
     */
    protected int[] findFirstValue(GridDatatype grid) {
        int index[] = null;
        try {

            Array data = grid.readDataSlice(0, 0, -1, -1);
            IndexIterator indexItr = data.getIndexIterator();
            while (indexItr.hasNext()) {
                Float value = indexItr.getFloatNext();
                if (!Float.isNaN(value)) {
                    System.out.println(Arrays.toString(indexItr.getCurrentCounter()) + ": " + value);
                    index = indexItr.getCurrentCounter();
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return index;
    }

    public void testFindLatLon() throws Exception {
        GeoGrid grid = (GeoGrid) getGrid();
        GridCoordSystem gcs = grid.getCoordinateSystem();

        int[] xy = gcs.findXYindexFromLatLonBounded(43.0d, -94.0d, null);
    }

    public void test() {
        try {
            GeoGrid grid = (GeoGrid) getGrid();

            /**
             * [1202, 2923]: 10.0
             * [1202, 2924]: 11.5
             * [1202, 2925]: 11.5
             * [1202, 2926]: 11.5
             * [1202, 2927]: 13.0
             * [1202, 2928]: 10.5
             * [1202, 2929]: 11.5
             * [1202, 2930]: 12.5
             */

            int[] index = findFirstValue(grid);
            Array data = grid.readDataSlice(0, 0, index[0], index[1]);
            System.out.println(data.getDouble(0));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
