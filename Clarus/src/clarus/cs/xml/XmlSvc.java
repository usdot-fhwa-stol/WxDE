// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file XmlSvc.java
 */
package clarus.cs.xml;

import java.sql.Connection;
import java.sql.ResultSet;
import clarus.cs.CollectorSvc;
import clarus.cs.CsMgr;


/**
 * Instances of {@code XmlSvc} are set up to create XmlCollector objects.
 * 
 * <p>
 * Extension of {@code CollectorSvc}, set up to create the right type of 
 * Collector object to read observations from the corresponding file.
 * </p>
 * <p>
 * Base class method {@see CollectorSvc#init} must be called before calling
 * createCollector to ensure proper behavior from the collection service.
 * </p>
 */
public class XmlSvc extends CollectorSvc
{

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Sets up the query format ({@code  m_sQuery}):
	 * <pre>    {defId}, {collection delay}, {retry flag}, </pre>
     * <pre>    {collector timezone id}, {content timezone id}, </pre>
     * <pre>    {timestamp id}, {default sensor index}, {filepath} </pre>
	 * </p>
	 */
	public XmlSvc()
	{
		//set the query used to retrieve the xml service configuration
		m_sQuery = "SELECT defId, collectDelay, retryFlag, collectTzId, " +
			"contentTzId, timestampId, defaultSensorIndex, " +
			"filepath FROM xmlcollector WHERE csvcId = ?";
	}


	/**
	 * Specifies a new instance of {@link XmlCollector}.
	 *
	 * @param oCsMgr Manages the new instance of {@code XmlCollector}.
	 * @param iConnection Connection to SQL database, previously connected.
	 * @param iResultSet Resultant set for database queries.
	 *
	 * @returns The newly created {@code XmlCollector} object.
	 *
	 * @throws java.lang.Exception
	 */
	@Override
	protected XmlCollector createCollector(CsMgr oCsMgr, Connection iConnection,
		ResultSet iResultSet) throws Exception
	{
		// virtual constructor for an xml file collector
		XmlCollector oCollector = new XmlCollector
		(
			iResultSet.getInt(1), iResultSet.getInt(2), iResultSet.getBoolean(3),
			iResultSet.getInt(4), iResultSet.getInt(5), iResultSet.getInt(6),
			iResultSet.getInt(7), iResultSet.getString(8),
			oCsMgr, this, iConnection
		);

		return oCollector;
	}
}
