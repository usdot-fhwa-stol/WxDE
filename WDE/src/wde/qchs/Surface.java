// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package wde.qchs;

import wde.WDEMgr;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.Introsort;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 */
public class Surface {
    private static final String SURFACE_QUERY = "SELECT lat, lon, month, " +
            "minCelsius, maxCelsius FROM surfaceclimaterecord";

    private static final Surface g_oInstance = new Surface();
    private final SurfaceRecord m_oSearch = new SurfaceRecord();
    private ArrayList<SurfaceRecord> m_oRecords = new ArrayList<SurfaceRecord>();


    private Surface() {
        Config oConfig = ConfigSvc.getInstance().getConfig(this);
        String sDataSourceName = oConfig.getString("datasource", null);

        Connection iConnection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            DataSource iDataSource =
                    WDEMgr.getInstance().getDataSource(sDataSourceName);

            if (iDataSource == null)
                return;

            iConnection = iDataSource.getConnection();
            if (iConnection == null)
                return;

            // execute the query
            ps = iConnection.prepareStatement(SURFACE_QUERY);
            rs = ps.executeQuery();

            while (rs.next()) {
                // save the database record into memory, months are zero-based
                SurfaceRecord oRecord =
                        new SurfaceRecord(rs.getDouble(1),
                                rs.getDouble(2), rs.getInt(3) - 1,
                                rs.getDouble(4), rs.getDouble(5));
                m_oRecords.add(oRecord);
            }

            // sort the records
            Introsort.usort(m_oRecords, m_oSearch);
        } catch (Exception oException) {
            oException.printStackTrace();
        } finally {
            try {
                rs.close();
                rs = null;
                ps.close();
                ps = null;
                iConnection.close();
                iConnection = null;
            } catch (SQLException se) {
                // ignore
            }
        }
    }

    /**
     * @return the current instance of the Surface background field class
     */
    public static Surface getInstance() {
        return g_oInstance;
    }

    public SurfaceRecord getSurfaceRecord(int nLat, int nLon, int nPeriod) {
        int nIndex = 0;
        synchronized (m_oSearch) {
            m_oSearch.setHash(nLat, nLon, nPeriod);
            nIndex = Introsort.binarySearch(m_oRecords, m_oSearch, m_oSearch);
        }

        if (nIndex < 0)
            return null;

        return m_oRecords.get(nIndex);
    }
}
