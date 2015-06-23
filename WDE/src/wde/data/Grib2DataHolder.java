/************************************************************************
 * Source filename: Grib2DataHolder.java
 * <p/>
 * Creation date: Mar 6, 2015
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.data;

import org.apache.log4j.Logger;
import ucar.nc2.Group;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public abstract class Grib2DataHolder {

    private static final Logger logger = Logger.getLogger(Grib2DataHolder.class);

    /**
     *
     * @param rootGroup
     * @return
     */
    public static Grib2DataHolder instantiate(String filepath) {

        // Check if file exists
        File file = new File(filepath);
        if (!file.exists()) {
            logger.error("File " + filepath + " does not exists!");
            return null;
        }

        NetcdfFile nFile = null;
        Grib2DataHolder g2dh = null;
        try {
            nFile = NetcdfFile.open(filepath);
            Group root = nFile.getRootGroup();

            if (filepath.contains("MergedReflectivityComposite")) {
                g2dh = MergedReflectivityComposite.g2dMap.get(filepath);
                if (g2dh == null) {
                    g2dh = new MergedReflectivityComposite();
                    MergedReflectivityComposite.g2dMap.put(filepath, g2dh);
                }
            } else if (filepath.contains("rtma")) {
                g2dh = RTMA.g2dMap.get(filepath);
                if (g2dh == null) {
                    g2dh = new RTMA();
                    RTMA.g2dMap.put(filepath, g2dh);
                }
            }

            g2dh.populate(root);
        } catch (Exception e) {
            logger.error(e.getMessage());
            g2dh.getMap().put(filepath, null);
        } finally {
            try {
                nFile.close();
            } catch (IOException e) {
                // ignore
            }

        }

        return g2dh;
    }

    protected abstract void populate(Group root) throws IOException;

    public abstract HashMap<String, Grib2DataHolder> getMap();

    public abstract void remove(String filepath);
}