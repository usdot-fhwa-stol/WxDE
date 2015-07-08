// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file IntArray.java
 */
package wde.qedc;

/**
 * Implementation of an auto-adjusting variable-sized array of integer values.
 * Allows insertion, pushing, and popping of values.
 */
class IntArray {
    /**
     * Size of the integer array.
     */
    private int m_nSize;
    /**
     * The integer array.
     */
    private int[] m_nValues;

    /**
     * Initializes the integer array to default size 10.
     */
    IntArray() {
        m_nValues = new int[10];
    }

    /**
     * Implementation of the binary search algorithm. Searches {@code nValues}
     * for {@code nValue} returning the index if found, otherwise a negative
     * value is returned.
     *
     * @param nValues list of values to search.
     * @param nValue  value to search for.
     * @return the index of the value if found, otherwise returns a negative
     * value.
     */
    private static int binarySearch(int[] nValues, int nValue) {
        int nLow = 0;
        int nHigh = nValues.length - 1;

        while (nLow <= nHigh) {
            int nMid = (nLow + nHigh) >>> 1;

            if (nValues[nMid] < nValue)
                nLow = nMid + 1;
            else if (nValues[nMid] > nValue)
                nHigh = nMid - 1;
            else
                return nMid; // value found
        }

        return -(++nLow);  // value not found
    }

    /**
     * Ensures the integer array has either the given capacity, or twice the
     * prior capacity, whichever is greater.
     *
     * @param nCapacity the capacity to ensure.
     */
    void ensureCapacity(int nCapacity) {
        // expand the array storage as needed
        int nOldCapacity = m_nValues.length;
        if (nCapacity >= nOldCapacity) {
            int nNewCapacity = nOldCapacity * 2;
            if (nCapacity < nNewCapacity)
                nCapacity = nNewCapacity;

            int[] nValues = new int[nCapacity];
            System.arraycopy(m_nValues, 0, nValues, 0, m_nSize);
            m_nValues = nValues;
        }
    }

    /**
     * Inserts the given value ({@code nValue}) into the integer array at the
     * supplied index ({@code nIndex}), shifting array[index...last] to
     * array[nIndex + 1...last + 1] to make room for the inserted value. The
     * size of the array is doubled if it is not large enough.
     *
     * @param nIndex the position in the integer array to insert the value.
     * @param nValue the value to insert.
     */
    void insert(int nIndex, int nValue) {
        ensureCapacity(m_nSize);
        System.arraycopy(m_nValues, nIndex, m_nValues, nIndex + 1,
                m_nSize - nIndex);

        m_nValues[nIndex] = nValue;
        m_nSize++;
    }

    /**
     * Places the value at the end of the array in the first empty position
     * (i.e. {@code array[last + 1] = nValue}). The size of the array is
     * adjusted if not large enough.
     *
     * @param nValue the value to add to the array.
     */
    void push(int nValue) {
        ensureCapacity(m_nSize);
        m_nValues[m_nSize++] = nValue;
    }

    /**
     * Removes and returns the last element in the array.
     *
     * @return last element in the array.
     */
    int pop() {
        return m_nValues[--m_nSize];
    }

    /**
     * <b> Accessor </b>
     *
     * @return size of the array
     */
    int size() {
        return m_nSize;
    }

    /**
     * Returns the index of {@code nValue} in the integer array.
     *
     * @param nValue the value to find in the array.
     * @return the index of {@code nValue} if contained in the integer array,
     * otherwise returns a negative value.
     */
    int binarySearch(int nValue) {
        return binarySearch(m_nValues, nValue);
    }
}
