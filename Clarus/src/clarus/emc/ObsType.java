// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * ObsType.java
 */
package clarus.emc;

/**
 * {@code ObsType} records correspond to a row from the obstype database.
 * <p>
 * Extension of {@code IObsType} providing an interface to access attributes.
 * </p>
 */
public class ObsType implements IObsType
{
	/**
     * Identifier value of the {@code ObsType}.
     */
    int m_nId;
    /**
     * Name of the {@code ObsType}
     */
	String m_sName;
    /**
     * Unit of measure.
     */
	String m_sUnit;
    /**
     * Corresponding English unit of measure.
     */
	String m_sEnglishUnit;


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code ObsType}
	 * </p>
	 */
	ObsType()
	{
	}


    /**
     * <b> Copy Constructor </b>
     *
     * <p>
     * Instances of {@code ObsType} created with this constructor are copies
     * of the supplied {@code ObsType}.
     * </p>
     *
     * @param oObsType The object to copy to a new instance.
     */
	ObsType(ObsType oObsType)
	{
		m_nId = oObsType.m_nId;
		m_sName = oObsType.m_sName;
		m_sUnit = oObsType.m_sUnit;
		m_sEnglishUnit = oObsType.m_sEnglishUnit;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return identifier attribute.
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
     * @return name attribute.
     */
	public String getName()
	{
		return m_sName;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return unit of measure for <i> this </i> {@code ObsType} object.
     */
	public String getUnit()
	{
		return m_sUnit;
	}

    /**
     * <b> Accessor </b>
     * <p>
     * Interface method implementation.
     * </p>
     * @return English units for <i> this </i> {@code ObsType} object.
     */
	public String getEnglishUnit()
	{
		return m_sEnglishUnit;
	}
}
