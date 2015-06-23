// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Contrib.java
 */
package clarus.qeds;

/**
 * Contains information to identify contributors both by id-number, and name.
 * Also provides methods with which to access, and modify this Data.
 */
public class Contrib
{
    /**
     * Contributor identification number.
     */
	int m_nId;
    /**
     * Hours to used to monitor for absent observations.
     */
	int m_nHours;
    /**
     * Flag used to indicate if information is published.
     */
	int m_nDisplay;
    /**
     * Contributor name.
     */
	String m_sName;
	

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code Contrib}
	 * </p>
	 */
	Contrib()
	{
	}
	

    /**
     * <b> Copy Constructor </b>
     * <p>
     * New instances of {@code Contrib} created with this constructor are
     * duplicates of the supplied {@code Contrib}.
     * </p>
     * @param oContributor {@code Contrib} to create a copy of.
     */
	Contrib(Contrib oContributor)
	{
		m_nId = oContributor.m_nId;
		m_nHours = oContributor.m_nHours;
		m_nDisplay = oContributor.m_nDisplay;
		m_sName = oContributor.m_sName;
	}
	

    /**
     * New instance of {@code Contrib} created with this constructor contain
     * the provided attribute values.
     * @param nId contributor identification number.
     * @param sName contributor name.
     */
	Contrib(int nId, String sName, int nHours, int nDisplay)
	{
		m_nId = nId;
		m_nHours = nHours;
		m_nDisplay = nDisplay;
		m_sName = sName;
	}
	

    /**
     * <b> Mutator </b>
     * <p>
     * Sets the contributor identifier.
     * </p>
     * @param nId value to set contributor id of <i> this </i> to.
     */
	void setKey(int nId)
	{
		m_nId = nId;
	}


    /**
     * <b> Accessor </b>
     * @return <i> this </i> contributor identifier.
     */
	public int getId()
	{
		return m_nId;
	}


    /**
     * <b> Accessor </b>
     * @return <i> this </i> contributor name.
     */
	public String getName()
	{
		return m_sName;
	}


    /**
     * <b> Accessor </b>
     * @return <i> this </i> monitored hours.
     */
	public int getHours()
	{
		return m_nHours;
	}


    /**
     * <b> Accessor </b>
     * @return <i> this </i> display flag.
     */
	public int getDisplay()
	{
		return m_nDisplay;
	}
}
