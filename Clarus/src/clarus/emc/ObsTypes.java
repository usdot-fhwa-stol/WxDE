// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ObsTypes.java
 */
package clarus.emc;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Provides means to cache {@code ObsType} objects, as well as modify,
 * access, copy, compare and print these objects contained in the
 * {@code DbCache}.
 *
 * <p>
 * Singleton class only allows one instance of {@code ObsTypes} which can
 * be accessed by the {@link ObsTypes#getInstance() } method.
 * </p>
 *
 * <p>
 * Extends {@code DbCache} to allow recurring records to be accessed
 * efficiently.
 * </p>
 */
public class ObsTypes extends DbCache<ObsType>
{
    /**
     * The singleton instance of {@code ObsTypes}
     */
	private static ObsTypes g_oInstance = new ObsTypes();

	/**
     * <b> Accessor </b>
     *
     * @return The singleton instance of {@code ObsTypes}
     */
	public static ObsTypes getInstance()
	{
		return g_oInstance;
	}
	
	/**
     * Initializes base class search and sort algorithms, as well as the
     * database query.
     *
     * <p>
     * Calls base class method {@link DbCache#run()} to begin caching
     * collected data.
     * </p>
     */
	private ObsTypes()
	{
		// set up the sort and search algorithms
		m_oRecordSort = new SortById();
		m_oSearchSort = new SortByName();
		
		m_sQuery = "SELECT id, obsType, obsInternalUnits, obsEnglishUnits " +
			"FROM obsType WHERE active = 1";
		
		run();
	}

	/**
     * Prints {@code ObsTypes} in a comma delimited manner formatted as
     * follows:
     * <p>
     * {observation type id}, {observation type name}, {units}, {english units}
     * </p>
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     * @param oPrintWriter output stream to write to, ready for streaming.
     * @param nIndex index of the record in the primary {@code DbCache} record
     *          list.
     */
	protected void toString(PrintWriter oPrintWriter, int nIndex)
	{
		ObsType oObsType = m_oRecords.get(nIndex);

//		oPrintWriter.print("\t\t\t<tr><td>");
		oPrintWriter.print(oObsType.m_nId);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oObsType.m_sName);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.print(oObsType.m_sUnit);
		oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
		oPrintWriter.println(oObsType.m_sEnglishUnit);
//		oPrintWriter.println("</td></tr>");
	}

	/**
     * Sets {@code oObsType} attributes to those contained in the current
     * row pointed to by the specified result set.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param iResultSet set containing the {@code ObsType} data.
     * @param oObsType the record whose parameters are to be modified.
     */
	protected void setParameters(ResultSet iResultSet, ObsType oObsType)
	{
		try
		{
			oObsType.m_nId = iResultSet.getInt(1);
			oObsType.m_sName = iResultSet.getString(2);
			oObsType.m_sUnit = iResultSet.getString(3);
			oObsType.m_sEnglishUnit = iResultSet.getString(4);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}
	}

	/**
     * Searches the primary {@code DbCache} records list for the record
     * containing the supplied observation type id.
     *
     * @param nId observation id correspoding to the record of interest.
     * @return the corresponding record in the primary records list, null if
     *   not found.
     */
	public IObsType getObsType(int nId)
	{
		ObsType oSearchRecord = m_oLock.readLock();		
		oSearchRecord.m_nId = nId;
		
		oSearchRecord = search(m_oRecords, oSearchRecord, m_oRecordSort);
		m_oLock.readUnlock();
		return oSearchRecord;
	}

    /**
     * Searches the secondary {@code DbCache} search array for the observation
     * with the supplied name.
     *
     * @param sName name of the observation-type of interest.
     * @return the retrieved record, or null if not found.
     */
	public IObsType getObsType(String sName)
	{
		ObsType oSearchRecord = m_oLock.readLock();		
		oSearchRecord.m_sName = sName;
		
		oSearchRecord = search(m_oSort, oSearchRecord, m_oSearchSort);
		m_oLock.readUnlock();
		return oSearchRecord;
	}

    /**
     * Retrieves the list of cached {@code IObsType} objects.
     * 
     * @return the list of cached {@code IObsType} records.
     */
	public ArrayList<IObsType> getList()
	{
        m_oLock.readLock();

        int nIndex = m_oRecords.size();
		ArrayList<IObsType> oList = new ArrayList<IObsType>(nIndex);
        while (nIndex-- > 0)
            oList.add(m_oRecords.get(nIndex));

        m_oLock.readUnlock();

		return oList;
	}

    /**
     * Compares the {@code ObsType} attributes of {@code oLhs} with those of
     * {@code oRhs}.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oLhs record to compare to {@code oRhs}
     * @param oRhs record to compare to {@code oLhs}
     * @return
     * true if the attribute values of {@code oLhs} match that of {@code oRhs},
     * false otherwise.
     */
	protected boolean recordsMatch(ObsType oLhs, ObsType oRhs)
	{
		return
		(
			oLhs.m_sName.compareTo(oRhs.m_sName) == 0 && 
			(
				// equality is used to test for null units
				oLhs.m_sUnit == oRhs.m_sUnit ||
				oLhs.m_sUnit.compareTo(oRhs.m_sUnit) == 0
			) && 
			(
				// equality is used to test for null units
				oLhs.m_sEnglishUnit == oRhs.m_sEnglishUnit ||
				oLhs.m_sEnglishUnit.compareTo(oRhs.m_sEnglishUnit) == 0
			)
		);
	}

	/**
     * Creates a new instance of {@code ObsType}, with the same attribute
     * values as the supplied {@code ObsType}.
     *
     * <p>
     * Specifies a required {@code DbCache} extension method.
     * </p>
     *
     * @param oObsType the object to create a copy of.
     * @return The newly created copy of {@code oObsType}.
     */
	protected ObsType copy(ObsType oObsType)
	{
		return new ObsType(oObsType);
	}
	
    /**
     * This is used to add a container of lockable {@code ObsType} objects
     * to the {@link util.threads.StripeLock} Mutex.
     * <p>
     * Specifies a required {@code DbCache} extension method, which is in turn
     * required for the base class's implementation of the interface class
     * {@code ILockFactory}.
     * </p>
     *
     * @return A new instance of {@code ObsType}
     *
     * @see util.threads.ILockFactory
     * @see util.threads.StripeLock
     */
	@Override
	public ObsType getLock()
	{
		return new ObsType();
	}
	
	/**
     * Specifies the primary records list sort order.
     * {@link SortById#compare(ObsType, ObsType)} compares {@code ObsType}
     * objects by object type id.
     *
     * <p>
     * Implements {@code Comparator<ObsType>} to define comparison of
     * {@code ObsType} objects by observation type id.
     * </p>
     */
	private class SortById implements Comparator<ObsType>
	{
         /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortById}
		 * </p>
         */
		private SortById()
		{
		}

        /**
         * Compares {@code ObsType} objects by id.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(ObsType oLhs, ObsType oRhs)
		{
			return (oLhs.m_nId - oRhs.m_nId);
		}
	}

	/**
     * Specifies the secondary records list sort order.
     * {@link SortByName#compare(ObsType, ObsType)} compares {@code ObsType}
     * objects by name.
     *
     * <p>
     * Implements {@code Comparator<ObsType>} to define comparison of
     * {@code ObsType} by name.
     * </p>
     */
	private class SortByName implements Comparator<ObsType>
	{
        /**
         * <b> Default Constructor </b>
		 * <p>
		 * Creates new instances of {@code SortByName}
		 * </p>
         */
		private SortByName()
		{
		}

        /**
         * Compares {@code ObsType} objects by name.
         *
         * @param oLhs object to compare {@code oRhs} to.
         * @param oRhs object to compare {@code oLhs} to.
         * @return 0 if the objects match.
         */
		public int compare(ObsType oLhs, ObsType oRhs)
		{
			return oLhs.m_sName.compareTo(oRhs.m_sName);
		}
	}
}
