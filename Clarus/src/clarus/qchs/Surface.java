// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
package clarus.qchs;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import javax.sql.DataSource;
import clarus.ClarusMgr;
import util.Config;
import util.ConfigSvc;
import util.Introsort;

/**
 *
 */
public class Surface
{
	private static final String SURFACE_QUERY = "SELECT lat, lon, month, " +
		"minCelsius, maxCelsius FROM surfaceclimaterecord";

	private static final Surface g_oInstance = new Surface();

	private ArrayList<SurfaceRecord> m_oRecords = new ArrayList<SurfaceRecord>();
	private final SurfaceRecord m_oSearch = new SurfaceRecord();


	/**
	 * @return the current instance of the Surface background field class
	 */
	public static Surface getInstance()
	{
		return g_oInstance;
	}


	private Surface()
	{
		Config oConfig = ConfigSvc.getInstance().getConfig(this);
		String sDataSourceName = oConfig.getString("datasource", null);
		try
		{
			DataSource iDataSource =
				ClarusMgr.getInstance().getDataSource(sDataSourceName);

			if (iDataSource == null)
				return;

			Connection iConnection = iDataSource.getConnection();
			if (iConnection == null)
				return;

			// execute the query
			ResultSet iResultSet =
				iConnection.createStatement().executeQuery(SURFACE_QUERY);

			while (iResultSet.next())
			{
				// save the database record into memory, months are zero-based
				SurfaceRecord oRecord =
					new SurfaceRecord(iResultSet.getDouble(1),
					iResultSet.getDouble(2), iResultSet.getInt(3) - 1, 
					iResultSet.getDouble(4), iResultSet.getDouble(5));
				m_oRecords.add(oRecord);
			}

			// sort the records
			Introsort.usort(m_oRecords, m_oSearch);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}


	public SurfaceRecord getSurfaceRecord(int nLat, int nLon, int nPeriod)
	{
		int nIndex = 0;
		synchronized(m_oSearch)
		{
			m_oSearch.setHash(nLat, nLon, nPeriod);
			nIndex = Introsort.binarySearch(m_oRecords, m_oSearch, m_oSearch);
		}

		if (nIndex < 0)
			return null;

		return m_oRecords.get(nIndex);
	}
}
