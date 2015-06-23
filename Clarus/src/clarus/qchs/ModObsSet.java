// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ModObsSet.java
 */
package clarus.qchs;

import java.util.ArrayList;
import clarus.qedc.IObs;

/**
 * This is a list container of {@code ModObs} with a pool containing
 * uninitialized {@code ModObs}.
 * <p>
 * Extends {@code ArrayList<ModObs>} to create the {@code ModObs} container.
 * </p>
 */
public class ModObsSet extends ArrayList<ModObs>
{
    /**
     * Default {@code ModObsSet} and {@code ModObs} pool size.
     */
	public static final int DEFAULT_CAPACITY = 100;

    /**
     * Array list of {@code ModObs}
     */
	private ArrayList<ModObs> m_oModObsPool;
	

    /**
     * <b> Default Constructor </b>
     * <p>
     * Initializes the {@code ModObsSet} size, and {@code ModObs} pool size to
     * the default capacity through the use of {@link ModObsSet#ModObsSet(int)}.
     * </p>
     */
	public ModObsSet()
	{
		this(DEFAULT_CAPACITY);
	}
	

    /**
     * Initializes the {@code ModObsSet} size, and {@code ModObs} pool size to
     * the supplied capacity.
     *
     * @param nInitialCapacity the capacity for the array lists.
     */
	public ModObsSet(int nInitialCapacity)
	{
		super(nInitialCapacity);
		m_oModObsPool = new ArrayList<ModObs>(nInitialCapacity);
	}


	/**
	 * The default operation is to return the original value.
	 * @param iObs
	 * @return observation value of the specified observation.
	 */
	public double modifyValue(IObs iObs)
	{
		// the default operation is to do nothing
		return iObs.getValue();
	}
	

    /**
     * Clears <i> this </i> primary {@code ModObs} array list, moving the
     * records to the {@code ModObs} pool.
     * <p>
     * Overides base class method.
     * </p>
     */
	@Override
	public void clear()
	{
		// move all the instantiated ModObs back to the pool
		int nIndex = size();
		while (nIndex-- > 0)
			m_oModObsPool.add(remove(nIndex));
	}
	
	
    /**
     * If the {@code ModObs} pool contains items, the last item is removed and
     * returned.
     *
     * @return the last item of the pool, or a new {@code ModObs} instance if
	 * the pool is empty.
     */
	public ModObs getModObs()
	{
		int nSize = m_oModObsPool.size();
		if (nSize-- > 0)
			return m_oModObsPool.remove(nSize);
		
		return new ModObs();
	}

	
    /**
     * Clears the supplied {@code ModObs} ({@link ModObs#clear()}) , placing it
     * at the end of the pool.
	 *
     * @param oModObs the object to clear and add to the pool.
     */
	public void putModObs(ModObs oModObs)
	{
		oModObs.clear();
		m_oModObsPool.add(oModObs);
	}
}
