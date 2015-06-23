// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file ObsIter.java
 */
package wde.obs;

import wde.util.Introsort;

/**
 * Allows hashing and iteration over observations based off their latitude and
 * longitude.
 * <p>
 * Extends {@code Obs} to allow constant time iteration of observation records.
 * </p>
 */
public class ObsIter extends Observation {
    /**
     * Size of the table cell.
     */
    private static final int CELL_SIZE = 1000000;

    /**
     * Number of columns in the table.
     */
    private int m_nCols;
    /**
     * Number of rows in the table.
     */
    private int m_nRows;
    /**
     * Index of the observation.
     */
    private int m_nIndex;
    /**
     * Start index offset.
     */
    private int m_nStart;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code ObsIter}
     * </p>
     */
    ObsIter() {
    }


    /**
     * Resets the iterator to its default values.
     */
    void clear() {
        super.clear();
        m_nCols = m_nRows = m_nIndex = m_nStart = 0;
    }


    /**
     * Initializes the hash iterator.
     *
     * @param nLatMin minimum observation latitude.
     * @param nLonMin minimum observation longitude.
     * @param nLatMax maximum observation latitude.
     * @param nLonMax maximum observation longitude.
     */
    void iterator(int nLatMin, int nLonMin, int nLatMax, int nLonMax) {
        m_nCols = (nLonMax - nLonMin) / CELL_SIZE + 1;
        m_nRows = (nLatMax - nLatMin) / CELL_SIZE + 1;
        m_nIndex = m_nCols * m_nRows;
        m_nStart = getHash(nLatMin, nLonMin);
    }


    /**
     * Determines whether there are more values to iterate over.
     *
     * @return true if there are more values, false otherwise.
     */
    boolean hasNext() {
        return (m_nIndex > 0);
    }


    /**
     * Calculates the hash value for the next observation to iterate over.
     */
    void next() {
        super.setHashValue((short) (--m_nIndex / m_nRows * 180 + m_nIndex % m_nRows + m_nStart));
    }


    /**
     * Calculates the hash value for the given latitude and longitude.
     *
     * @param nLat latitude of the observation sensor.
     * @param nLon longitude of the observation sensor.
     * @return the calculated hash value.
     */
    short getHash(int nLat, int nLon) {
        // floor the geo-coordinates correctly
        nLat = Introsort.floor(nLat, CELL_SIZE);
        nLon = Introsort.floor(nLon, CELL_SIZE);

        return (short) ((nLon / CELL_SIZE) * 180 + (nLat / CELL_SIZE));
    }
/*
grid-width = (lon-max - lon-min) / cell-size + 1
grid-height = (lat-max - lat-min) / cell-size + 1
array-length =  grid-width * grid-height
hash-index = (lon - lon-min) / cell-size * grid-height + (lat - lat-min) / cell-size
hash-index = ((lon - lon-min) * grid-height + (lat - lat-min)) / cell-size
 */
}
