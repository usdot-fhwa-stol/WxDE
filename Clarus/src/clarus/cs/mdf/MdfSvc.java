// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file MdfSvc.java
 */

package clarus.cs.mdf;

import clarus.cs.ascii.*;
import java.sql.Connection;
import java.sql.ResultSet;
import clarus.cs.CsMgr;

/**
 * Instances of {@code MdfSvc} are set up to gather mdf data. These objects are
 * {@link Runnable}, so can be targets of Thread instances.
 * <p>
 * Extension of {@code CsvSvc}, performs the same queries as the base class.
 * </p>
 * <p>
 * Base class method {@see CollectorSvc#init} must be called before collection
 * attempts to ensure proper behavior from the collection service.
 * </p>
 */
public class MdfSvc extends CsvSvc
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code MdfSvc}
	 * </p>
	 */
	public MdfSvc()
	{
	}

    /**
     * Specifies a new instance of {@link MdfCollector}, managed by
     * {@code oCsMgr}.
     *
     * @param oCsMgr manages the new instance of {@code MdfCollector}.
     * @param iConnection Connection to SQL database, previously connected.
     * @param iResultSet Resultant set for database queries.
     *
     * @return The newly created {@code MdfCollector}.
     *
     * @throws java.lang.Exception
     */
	@Override
	protected CsvCollector createCollector(CsMgr oCsMgr, Connection iConnection, 
		ResultSet iResultSet) throws Exception
	{
		// virtual contstructor for an ASCII csv file collector
		MdfCollector oCollector = new MdfCollector
		(
			iResultSet.getInt(1), iResultSet.getInt(2), iResultSet.getBoolean(3),
			iResultSet.getInt(4), iResultSet.getInt(5), iResultSet.getInt(6),
			iResultSet.getInt(7), iResultSet.getString(8),
			iResultSet.getString(9), iResultSet.getString(10),
			iResultSet.getString(11), oCsMgr, this, iConnection
		);
		
		return oCollector;
	}
}
