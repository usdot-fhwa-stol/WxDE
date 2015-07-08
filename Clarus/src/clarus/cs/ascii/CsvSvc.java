// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsvSvc.java
 */

package clarus.cs.ascii;

import java.sql.Connection;
import java.sql.ResultSet;
import clarus.cs.CollectorSvc;
import clarus.cs.CsMgr;

/**
 * Instances of {@code CsvSvc} are set up to gather csv data. These objects are
 * {@link Runnable}, so can be targets of Thread instances.
 * <p>
 * Extension of {@code CollectorSvc}, set up to make csv queries.
 * </p>
 * <p>
 * Base class method {@see CollectorSvc#init} must be called before collection
 * attempts to ensure proper behavior from the collection service.
 * </p>
 */
public class CsvSvc extends CollectorSvc
{
    /**
     * <b> Default Constructor </b>
     * <p>
     * Sets up the query format ({@code m_sQuery}):
     * <pre>    {id}, {collection delay}, {retry flag}, </pre>
     * <pre>    {collector timezone id}, {content timezone id}, </pre>
     * <pre>    {timestamp id}, {lines-to-skip}, {delimiter},  </pre>
     * <pre>    {newline identifier}, {filepath} </pre>
     * </p>
     */
	public CsvSvc()
	{
		// set the query used to retrieve the csv service configuration
		m_sQuery = "SELECT id, collectDelay, retryFlag, collectTzId, " + 
			"contentTzId, timestampId, skipLines, delimiter, newline, " + 
			"stationCode, filepath FROM csvcollector WHERE csvcId = ?";
	}
	
	/**
     * Specifies a new instance of {@link CsvCollector}, managed by
     * {@code oCsMgr}.
     * 
     * @param oCsMgr manages the new instance of {@code CsvCollector}.
     * @param iConnection Connection to SQL database, previously connected.
     * @param iResultSet Resultant set for database queries.
     *
     * @return The newly created {@code CsvCollector}.
     * 
     * @throws java.lang.Exception
     */
	@Override
	protected CsvCollector createCollector(CsMgr oCsMgr, Connection iConnection, 
		ResultSet iResultSet) throws Exception
	{
		// virtual contstructor for an ASCII csv file collector
		CsvCollector oCollector = new CsvCollector
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
