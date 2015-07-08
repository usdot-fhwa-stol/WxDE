// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Station.java
 */
package clarus.emc;

/**
 * Provides storage and retrieval of data about observation stations.
 *
 * <p>
 * Implements {@code IStation} to provide a standard interface for data access.
 * </p>
 */
public class Station implements IStation
{
    /**
     * Station identifier.
     */
	int m_nId;
    /**
     * Contributor identifier.
     */
	int m_nContribId;
    /**
     * Site identifier.
     */
	int m_nSiteId;
    /**
     * Climate identifier.
     */
	int m_nClimateId;
    /**
     * Station latitude.
     */
	int m_nLat;
    /**
     * Station longitude.
     */
	int m_nLon;
    /**
     * Station elevation above sea-level.
     */
	short m_tElev;
    /**
     * Station category type -- Permanent, Mobile, Temporary
     */
	char m_cCat;
    /**
     * Station code.
     */
	String m_sCode;
    /**
     * Station description.
     */
    String m_sDesc;

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Station}
	 * </p>
	 */
	Station()
	{
	}
	
    /**
     * <b> Copy Constructor </b>
     * <p>
     * Creates a new instance of {@code Station} that is a duplicate of
     * the supplied {@code Station} object.
     * </p>
     */
	Station(Station oStation)
	{
		m_nId = oStation.m_nId;
		m_nContribId = oStation.m_nContribId;
		m_nSiteId = oStation.m_nSiteId;
		m_nClimateId = oStation.m_nClimateId;
		m_nLat = oStation.m_nLat;
		m_nLon = oStation.m_nLon;
		m_tElev = oStation.m_tElev;
		m_cCat = oStation.m_cCat;
		m_sCode = oStation.m_sCode;
        m_sDesc = oStation.m_sDesc;
	}
	
    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station identifier.
     */
	public int getId()
	{
		return m_nId;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return 0 in current implementation.
     */
	public int getSensorCount(int nObsType)
	{
		return 0;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Contributor identifier.
     */
	public int getContribId()
	{
		return m_nContribId;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Site identifier.
     */
	public int getSiteId()
	{
		return m_nSiteId;
	}
	
    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Climate identifier.
     */
	public int getClimateId()
	{
		return m_nClimateId;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station latitude.
     */
	public int getLat()
	{
		return m_nLat;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station longitude.
     */
	public int getLon()
	{
		return m_nLon;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station elevation above sea-level.
     */
	public short getElev()
	{
		return m_tElev;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station category type.
     */
	public char getCat()
	{
		return m_cCat;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station code.
     */
	public String getCode()
	{
		return m_sCode;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return Station description.
     */
	public String getDesc()
	{
		return m_sDesc;
	}
}
