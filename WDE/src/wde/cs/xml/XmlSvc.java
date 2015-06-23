// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file XmlSvc.java
 */
package wde.cs.xml;

import wde.cs.CollectorSvc;
import wde.cs.CsMgr;

import java.sql.Connection;
import java.sql.ResultSet;


/**
 * Instances of {@code XmlSvc} are set up to create XmlCollector objects.
 * <p/>
 * <p>
 * Extension of {@code CollectorSvc}, set up to create the right type of
 * Collector object to read observations from the corresponding file.
 * </p>
 * <p>
 * Base class method {@see CollectorSvc#init} must be called before calling
 * createCollector to ensure proper behavior from the collection service.
 * </p>
 */
public class XmlSvc extends CollectorSvc {

    /**
     * <b> Default Constructor </b>
     * <p>
     * Sets up the query format ({@code  m_sQuery}):
     * <pre>    {defId}, {collection delay}, {retry flag}, </pre>
     * <pre>    {collector timezone id}, {content timezone id}, </pre>
     * <pre>    {timestamp id}, {default sensor index}, {filepath} </pre>
     * </p>
     */
    public XmlSvc() {
        //set the query used to retrieve the xml service configuration
        m_sQuery = "SELECT defId, collectDelay, retryFlag, collectTzId, " +
                "contentTzId, timestampId, defaultSensorIndex, " +
                "filepath FROM conf.xmlcollector WHERE csvcId = ?";
    }


    /**
     * Specifies a new instance of {@link XmlCollector}.
     *
     * @param oCsMgr      Manages the new instance of {@code XmlCollector}.
     * @param iConnection Connection to SQL database, previously connected.
     * @param rs          Resultant set for database queries.
     * @throws java.lang.Exception
     * @returns The newly created {@code XmlCollector} object.
     */
    @Override
    protected XmlCollector createCollector(int[] nContribIds, CsMgr oCsMgr,
                                           Connection iConnection, ResultSet rs) throws Exception {
        // virtual constructor for an xml file collector
        XmlCollector oCollector = new XmlCollector
                (
                        rs.getInt(1), rs.getInt(2), rs.getBoolean(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6),
                        rs.getInt(7), rs.getString(8),
                        nContribIds, oCsMgr, this, iConnection
                );

        return oCollector;
    }
}
