// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @files IncrementalArrayList.java
 */
package clarus.qedc;

import java.util.AbstractList;

/**
 * <p>
 * The IncrementalArrayList is an array used to store extremely large numbers
 * of elements. It does this by incrementally allocating a set of fixed-length
 * 1-dimensional arrays. Indexing speed is reduced slightly, but the
 * allocate/copy operations used to expand the array is eliminated.
 * </p>
 */
public class IncrementalArrayList extends AbstractList<Object>
{
    /**
     * Length of the bit mask.
     */
	private int m_nBitCount;
    /**
     * An integer with the number of bits specified by the bit count set.
     */
	private int m_nBitMask;
    /**
     * Capacity growth increment.
     */
	private int m_nCapacityIncrement;
    /**
     * A reference to the next available element in the current Object array.
     */
	private int m_nRowIndex;
    /**
     * The index of the current Object element array.
     */
	private int m_nColumnIndex = -1;
    /**
     * The upper limit of the number of allowed Object arrays.
     */
	private int m_nMaxColumnIndex = -1;
    /**
     * Size of the {@code IncrementalArrayList}.
     */
	private int m_nSize;
    /**
     * The generic 2-dimensional object array to hold data elements.
     */
	private Object[][] m_oElements;
	

    /**
     * <b> Default Constructor </b>
     * <p>
     * Sets the maximum capacity of new instances of the array list to the
     * default size.
     * </p>
     */
	public IncrementalArrayList()
	{
		// the default maximum capacity is 2^31 elements
		this(Integer.MAX_VALUE);
	}
	

    /**
     * IncrementalArrayList constructor that accepts the intial maximum
	 * capacity and sizes the internal storage accordingly.
     * @param nMaxCapacity
     */
	public IncrementalArrayList(int nMaxCapacity)
	{
		// eliminate negative capacities
		if (nMaxCapacity < 0)
			nMaxCapacity *= -1;
		
		// allocate the minimum size for the array when necessary
		if (nMaxCapacity < 256)
			nMaxCapacity = 256;
		
		// get the total number of bits that represent the maximum capacity
		int nTotalBitCount = 0;
		while ((nMaxCapacity >>>= 1) != 0)
			++nTotalBitCount;
		
		// calculate the length of the bit mask
		m_nBitCount = nTotalBitCount / 2;
		// create the bit mask and determine the array growth factor
		m_nBitMask = (int)(Math.pow(2.0, m_nBitCount));
		m_nCapacityIncrement = m_nBitMask--;

		// allocate the remaining bits to the column array container
		nTotalBitCount -= m_nBitCount;
		m_oElements = new Object[(int)(Math.pow(2.0, nTotalBitCount))][];
		
		// the first element added will allocate the column array
		m_nRowIndex = m_nBitMask;
	}
	

    /**
     * Save the provided element at the specified index.
     *
     * @param nIndex The index where the element is to be inserted.
     * @param oElement A reference to the object being stored.
     */
	@Override
	public void add(int nIndex, Object oElement)
	{
		// allocate the next column array as needed and increment the array size
		if (++m_nRowIndex == m_nCapacityIncrement)
		{
			if (m_nMaxColumnIndex == m_oElements.length)
				throw new OutOfMemoryError();
			
			if (m_nColumnIndex == m_nMaxColumnIndex++)
				m_oElements[m_nMaxColumnIndex] = new Object[m_nCapacityIncrement];

			++m_nColumnIndex;
			m_nRowIndex = 0;
		}
		++m_nSize;
		
		// shift the elements down one position
		moveElements(nIndex, ++nIndex, m_nSize - nIndex);
		
		// set the index position to the new element
	}
	

    /**
     * A convenience method for shifting array elements when elements are 
	 * inserted or removed.
	 * 
     * @param nSrcIndex The index from which to start moving elements.
     * @param nDestIndex The index where moved elements should be placed.
     * @param nLength The number of elements being moved.
     */
	protected void moveElements(int nSrcIndex, int nDestIndex, int nLength)
	{
	}
	

    /**
     * Returns the element stored in the array at the supplied index.
	 * 
     * @param nIndex index of the object of interest.
     * @return the object stored in the array at the supplied index.
     */
	@Override
	public Object get(int nIndex)
	{
		// bounds check
		if (nIndex < 0 || nIndex >= m_nSize)
			throw new IndexOutOfBoundsException();
		
		// first the column index is derived, and then the row index
		return m_oElements[nIndex >>> m_nBitCount][nIndex & m_nBitMask];
	}
	

    /**
     * Swaps the value at the given index with the supplied element.
	 *
     * @param nIndex element to swap.
     * @param oElement newly swapped in element.
     * @return the old value that was just swapped out.
     */
	@Override
	public Object set(int nIndex, Object oElement)
	{
		// bounds check
		if (nIndex < 0 || nIndex >= m_nSize)
			throw new IndexOutOfBoundsException();
		
		// get the column array and adjust the index into that context
		Object[] oColumnArray = m_oElements[nIndex >>> m_nBitCount];
		nIndex &= m_nBitMask;
		
		// swap the elements
		Object oOldElement = oColumnArray[nIndex];
		oColumnArray[nIndex] = oElement;
		return oOldElement;
	}
	

    /**
     * Returns the number of elements stored in the array.
	 * 
     * @return size of the {@code IncrementalArrayList}.
     */
	@Override
	public int size()
	{
		return m_nSize;
	}
}
