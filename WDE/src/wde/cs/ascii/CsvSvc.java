// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file CsvSvc.java
 */

package wde.cs.ascii;

import wde.cs.CollectorSvc;
import wde.cs.CsMgr;

import java.sql.Connection;
import java.sql.ResultSet;

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
public class CsvSvc extends CollectorSvc {
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
    public CsvSvc() {
        // set the query used to retrieve the csv service configuration
        m_sQuery = "SELECT id, collectDelay, retryFlag, collectTzId, " +
                "contentTzId, timestampId, skipLines, delimiter, newline, " +
                "stationCode, filepath FROM conf.csvcollector WHERE csvcId = ?";
    }

    /**
     * Specifies a new instance of {@link CsvCollector}, managed by
     * {@code oCsMgr}.
     *
     * @param oCsMgr      manages the new instance of {@code CsvCollector}.
     * @param iConnection Connection to SQL database, previously connected.
     * @param rs          Resultant set for database queries.
     * @return The newly created {@code CsvCollector}.
     * @throws java.lang.Exception
     */
    @Override
    protected CsvCollector createCollector(int[] nContribIds, CsMgr oCsMgr,
                                           Connection iConnection, ResultSet rs) throws Exception {
        // virtual constructor for an ASCII csv file collector
        CsvCollector oCollector = new CsvCollector
                (
                        rs.getInt(1), rs.getInt(2), rs.getBoolean(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6),
                        rs.getInt(7), rs.getString(8),
                        rs.getString(9), rs.getString(10),
                        rs.getString(11), nContribIds, oCsMgr, this, iConnection
                );

        return oCollector;
    }
}
