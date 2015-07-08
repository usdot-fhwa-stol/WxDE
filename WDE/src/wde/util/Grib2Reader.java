/************************************************************************
 * Source filename: Grib2Reader.java
 * <p/>
 * Creation date: Feb 27, 2015
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

package wde.util;

import net.sourceforge.sizeof.SizeOf;
import wde.data.Grib2DataHolder;

import java.util.HashMap;

public class Grib2Reader {

    public static void main(String[] args) {
        String pathfile1 = "c:/tmp/MRMS_MergedReflectivityComposite_20150304-213011_decoded.grib2";
        String pathfile2 = "c:/tmp/rtma2p5.t16z.2dvarges_ndfd.grb2";

        Grib2DataHolder g2dh1 = Grib2DataHolder.instantiate(pathfile1);
        HashMap<String, Grib2DataHolder> map1 = g2dh1.getMap();
        Grib2DataHolder g2dh2 = Grib2DataHolder.instantiate(pathfile2);
        HashMap<String, Grib2DataHolder> map2 = g2dh2.getMap();
        System.out.println("RTMA object size: "
                + SizeOf.humanReadable(SizeOf.deepSizeOf(g2dh2)));
    }
}
