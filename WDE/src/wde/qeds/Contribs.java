// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file Contribs.java
 */
package wde.qeds;

import org.apache.log4j.Logger;
import wde.dao.DbCache;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Provides caching, querying, modifcation, and sorting of {@code Contrib}
 * records.
 * <p>
 * Extends {@code DbCache<Contrib>} to allow efficient access of {@code Contrib}
 * records.
 * </p>
 * <p>
 * Singleton class whose instance can be access via
 * {@link Contribs#getInstance() }.
 * </p>
 */
public class Contribs extends DbCache<Contrib> {
    private static final Logger logger = Logger.getLogger(Contribs.class);

    /**
     * Pointer to the singleton instance of {@code DbCache}.
     */
    private static Contribs g_oInstance = new Contribs();


    /**
     * <b> Default Constructor </b>
     * <p>
     * Sets up the search (by name) and sort (by id) alogrithms, as well as
     * the database query, and begins updating the cache through a call to
     * the inherited method {@link Contribs#run() }.
     * </p>
     */
    private Contribs() {
        // set up the sort and search algorithms
        m_oRecordSort = new SortById();
        m_oSearchSort = new SortByName();

        m_sQuery = "SELECT id, name, monitorHours, display FROM meta.contrib where totime is null";

        run();
    }

    /**
     * <b> Accessor </b>
     *
     * @return the singleton instance of {@code Contribs}.
     */
    public static Contribs getInstance() {
        return g_oInstance;
    }

    /**
     * Prints the record from the primary records list at the provided index in
     * a formatted manner:
     * <blockquote>
     * contributor-id, contributor-name
     * </blockquote>
     *
     * @param oPrintWriter output stream - connected and prepared to stream
     *                     prior to a call to this method.
     * @param nIndex       index of the record to print.
     */
    protected void toString(PrintWriter oPrintWriter, int nIndex) {
        Contrib oContrib = m_oRecords.get(nIndex);

//		oPrintWriter.print("\t\t\t<tr><td>");
        oPrintWriter.print(oContrib.m_nId);
        oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
        oPrintWriter.println(oContrib.m_sName);
//		oPrintWriter.println("</td></tr>");
    }


    /**
     * Sets the attribute of the provided {@code Contrib} object to the values
     * contained in the provided result set.
     *
     * @param rs       database query result containing the {@code Contrib}
     *                 data.
     * @param oContrib object whose parameters are to be set.
     */
    protected void setParameters(ResultSet rs, Contrib oContrib) {
        try {
            oContrib.m_nId = rs.getInt(1);
            oContrib.m_sName = rs.getString(2);
            oContrib.m_nHours = rs.getInt(3);
//			oContrib.m_nDisplay = rs.getInt(4);
            oContrib.m_nDisplay = rs.getBoolean(4) ? 1 : 0;
        } catch (Exception oException) {
            logger.error(oException);
        }
    }


    /**
     * Determines whether or not the provided {@code Contrib} objects match.
     *
     * @param oLhs object to compare to oRhs.
     * @param oRhs object to compare to oLhs.
     * @return true if the records have both the same contributor identification
     * number and contributor name, false otherwise.
     */
    protected boolean recordsMatch(Contrib oLhs, Contrib oRhs) {
        return
                (
                        oLhs.m_nId == oRhs.m_nId &&
                                oLhs.m_sName.compareToIgnoreCase(oRhs.m_sName) == 0 &&
                                oLhs.m_nHours == oRhs.m_nHours &&
                                oLhs.m_nDisplay == oRhs.m_nDisplay
                );
    }


    /**
     * Creates and returns a new instance of {@code Contrib} that is a copy of
     * the provided {@code Contrib}.
     *
     * @param oContrib object to create a copy of.
     * @return the newly created copy.
     */
    protected Contrib copy(Contrib oContrib) {
        return new Contrib(oContrib);
    }


    /**
     * Overrides base class implementation.
     * <p>
     * Required for the base class implementation of the interface class
     * {@code ILockFactory}.
     * </p>
     * <p>
     * This is used to add a container of lockable {@link Contrib} objects
     * to the {@link StripeLock} Mutex.
     * </p>
     *
     * @return A new instance of {@link Contrib}
     * @see ILockFactory
     * @see StripeLock
     */
    @Override
    public Contrib getLock() {
        return new Contrib();
    }


    /**
     * Searches the primary record container for the contributor with the
     * provided identification number. Returns the record if found, otherwise
     * returns null.
     *
     * @param nContribId identification number of interest.
     * @return record corresponding to the provided identification number.
     */
    public Contrib getContrib(int nContribId) {
        Contrib oSearchRecord = m_oLock.readLock();
        oSearchRecord.m_nId = nContribId;

        Contrib oContrib = null;
        int nIndex = Collections.binarySearch(m_oRecords, oSearchRecord, m_oRecordSort);
        if (nIndex >= 0)
            oContrib = m_oRecords.get(nIndex);

        m_oLock.readUnlock();
        return oContrib;
    }


    /**
     * Copies the secondary list of contributors in sorted order into the
     * supplied contributor list.
     *
     * @param oList the list to contain the contributors after a call to this
     *              method.
     */
    public void getContribs(ArrayList<Contrib> oList) {
        oList.clear();
        m_oLock.readLock();

        oList.ensureCapacity(m_oSort.size());
        for (int nIndex = 0; nIndex < m_oSort.size(); nIndex++)
            oList.add(m_oSort.get(nIndex));

        m_oLock.readUnlock();
    }


    /**
     * Provides ordering of {@code Contrib} objects based off contributor
     * identifier.
     * <p>
     * Implements {@code Comparator} interface to enforce a standard interface
     * for comparisons.
     * </p>
     */
    private class SortById implements Comparator<Contrib> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code SortById}
         * </p>
         */
        private SortById() {
        }


        /**
         * Compares {@code Contrib} objects by identification number.
         *
         * @param oLhs object to compare to {@code oRhs}.
         * @param oRhs object to compare to {@code oLhs}.
         * @return 0 if the {@code Contrib} identifiers are equivalent.
         */
        public int compare(Contrib oLhs, Contrib oRhs) {
            return (oLhs.m_nId - oRhs.m_nId);
        }
    }


    /**
     * Provides ordering of {@code Contrib} objects, based off contributor name.
     * <p>
     * Implements {@code Comparator} interface to enforce a standard interface
     * for comparisons.
     * </p>
     */
    private class SortByName implements Comparator<Contrib> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code SortByName}
         * </p>
         */
        private SortByName() {
        }


        /**
         * Compares {@code Contrib} objects by name.
         *
         * @param oLhs object to compare to {@code oRhs}.
         * @param oRhs object to compare to {@code oLhs}.
         * @return 0 if the {@code Contrib} names are equivalent.
         */
        public int compare(Contrib oLhs, Contrib oRhs) {
            return oLhs.m_sName.compareToIgnoreCase(oRhs.m_sName);
        }
    }
}
