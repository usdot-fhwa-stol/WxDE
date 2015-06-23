// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file MdfSvc.java
 */

package wde.cs.mdf;

import wde.cs.CsMgr;
import wde.cs.ascii.CsvSvc;

import java.sql.Connection;
import java.sql.ResultSet;

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
public class MdfSvc extends CsvSvc {
    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code MdfSvc}
     * </p>
     */
    public MdfSvc() {
    }

    /**
     * Specifies a new instance of {@link MdfCollector}, managed by
     * {@code oCsMgr}.
     *
     * @param oCsMgr      manages the new instance of {@code MdfCollector}.
     * @param iConnection Connection to SQL database, previously connected.
     * @param rs          Resultant set for database queries.
     * @return The newly created {@code MdfCollector}.
     * @throws java.lang.Exception
     */
    @Override
    protected MdfCollector createCollector(int[] nContribIds, CsMgr oCsMgr,
                                           Connection iConnection, ResultSet rs) throws Exception {
        // virtual constructor for an ASCII csv file collector
        MdfCollector oCollector = new MdfCollector
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
