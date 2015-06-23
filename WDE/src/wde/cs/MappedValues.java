// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * MappedValues.java
 */

package wde.cs;

import org.apache.log4j.Logger;
import wde.dao.DbCache;
import wde.util.Text;

import java.io.PrintWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Ties {@code MappedValue} into {@code DbCache} to create a cache of
 * {@code MappedValue}'s, which can then be queried via
 * {@see MappedValues#getValue}. It also provides a means of sorting and
 * comparisons.
 * <p/>
 * <p>
 * Singleton Class whose instance can be accessed with the
 * {@see MappedValues#getInstance} method.
 * </p>
 */
public class MappedValues extends DbCache<MappedValue> {
    private static final Logger logger = Logger.getLogger(MappedValues.class);

    /**
     * The global singleton instance of the {@code MappedValues} class.
     */
    private static MappedValues g_oInstance = new MappedValues();

    /**
     * Record of the values missing from the record list
     * {@see DbCache#m_oRecords m_oRecords} contained in {@see DbCache}, upon
     * retrieval.
     */
    private final ArrayList<CollectorValue> m_oMissingValues =
            new ArrayList<CollectorValue>();

    /**
     * <b> Default Constructor </b>
     * <p/>
     * <p>
     * Called on new instances of {@code MappedValues}. Initializes the
     * {@see DbCache}, and calls {@see DbCache#run} to begin storing data in
     * the cache. Also sets the records up to be sorted by label. Formats the
     * query to be of the form:
     * <pre>
     *  {Observation Type ID}, {Label}, {Value}</pre>
     * </p>
     *
     * @see DbCache
     */
    private MappedValues() {
        // free the secondary array resources
        m_oSort = null;

        // set up the primary sort algorithm
        m_oRecordSort = new SortByTypeLabel();

        m_sQuery = "SELECT obsTypeId, valueLabel, value FROM conf.obsvaluemap";

        run();
    }

    /**
     * Gets the singleton instance of {@code MappedValues}.
     *
     * @return The singleton instance of {@code MappedValues}.
     */
    public static MappedValues getInstance() {
        return g_oInstance;
    }

    /**
     * Prints the record contained in {@see DbCache#m_oRecords m_oRecords}
     * at the supplied index {@code nIndex} in the following format:
     * <pre>
     *      {Observation Type}, {Value}, {Label}</pre>
     *
     * <p>
     * This method is required for the extension of {@see DbCache}.
     * </p>
     *
     * @param oPrintWriter The {@see PrintWriter} to use for the output stream.
     *                     Assumes the {@code PrintWriter} is initialized, and connected to the
     *                     stream for output.
     * @param nIndex       The index of the record to print.
     */
    protected void toString(PrintWriter oPrintWriter, int nIndex) {
        MappedValue oMappedValue = m_oRecords.get(nIndex);

//		oPrintWriter.print("\t\t\t<tr><td>");
        oPrintWriter.print(oMappedValue.m_nObsType);
        oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
        oPrintWriter.print(oMappedValue.m_dValue);
        oPrintWriter.print(",");
//		oPrintWriter.print("</td><td>");
        oPrintWriter.println(oMappedValue.m_sLabel);
//		oPrintWriter.println("</td></tr>");
    }

    /**
     * Sets the attributes of the given {@code MappedValue} to the
     * correpsponding values from the supplied {@code ResultSet}.
     * <p/>
     * <p>
     * This method is required for the extension of {@see DbCache}.
     * </p>
     *
     * @param rs           The {@code ResultSet} with which to retrieve the data.
     *                     Assumes the {@code ResultSet} has been connected, and the query has been
     *                     made.
     * @param oMappedValue The {@code MappedValue} whose attributes are to be
     *                     set.
     */
    protected void setParameters(ResultSet rs, MappedValue oMappedValue) {
        try {
            oMappedValue.m_nObsType = rs.getInt(1);
            oMappedValue.m_sLabel = rs.getString(2);
            oMappedValue.m_dValue = rs.getDouble(3);
        } catch (Exception oException) {
            logger.error(oException);
        }
    }

    /**
     * Creates a key-value pair from the given label and observation
     * type to search the list of records ({@code m_oRecords}). Returns the
     * value of the record if it is contained in the list, otherwise it adds the
     * key-value pair to the list of missing values ({@code m_oMissingValues}),
     * and returns {@see Double#NEGATIVE_INFINITY Negative Infinity}.
     *
     * @param nCollectorId The collector service identifier.
     * @param nObsType     The observation type to be retrieved.
     * @param sLabel       The associated label matching the observation type.
     * @return The value of the record of type {@code nObsType} with label
     * {@code sLabel}, if found in the {@see DbCache} record list
     * ({@see DbCache#m_oRecords m_oRecords}).
     * Otherwise {@see Double#NEGATIVE_INFINITY Negative Infinity}.
     */
    public double getValue(int nCollectorId, int nObsType, CharSequence sLabel) {
        double dValue = Double.NaN;

        MappedValue oRecord = m_oLock.readLock();
        oRecord.setKey(nObsType, sLabel);
        oRecord = search(m_oRecords, oRecord, m_oRecordSort);

        if (oRecord == null) {
            // eliminate duplicate missing mapped values from the log
            synchronized (m_oMissingValues) {
                CollectorValue oSearch =
                        new CollectorValue(nCollectorId, nObsType, sLabel);

                int nIndex = Collections.binarySearch(m_oMissingValues, oSearch);
                if (nIndex < 0) {
                    logger.info("getValue nCollectorId: " + Integer.toString(nCollectorId)
                            + " nObsType: " + Integer.toString(nObsType)
                            + " sLabel: " + sLabel);

                    m_oMissingValues.add(~nIndex, oSearch);
                }
            }
        } else
            dValue = oRecord.m_dValue;

        m_oLock.readUnlock();
        return dValue;
    }


    /**
     * Determines whether or not the supplied mapped values - {@code oLhs} and
     * {@code oRhs} - are equivalent.
     * <p/>
     * <p>
     * This method is required for the extension of {@see DbCache}.
     * </p>
     *
     * @param oLhs The left hand side value.
     * @param oRhs The right hand side value.
     * @return <pre>
     * true if the left hand side matches the right hand side.
     * false otherwise.
     * </pre>
     */
    protected boolean recordsMatch(MappedValue oLhs, MappedValue oRhs) {
        return
                (
                        oLhs.m_nObsType == oRhs.m_nObsType &&
                                oLhs.m_dValue == oRhs.m_dValue &&
                                Text.compareIgnoreCase(oLhs.m_sLabel, oRhs.m_sLabel) == 0
                );
    }

    /**
     * Creates and returns a copy of {@code oMappedValue}.
     * <p/>
     * <p>
     * This method is required for the extension of {@see DbCache}.
     * </p>
     *
     * @param oMappedValue The {@code MappedValue} to copy.
     * @return The new copy of {@code oMappedValue}.
     */
    protected MappedValue copy(MappedValue oMappedValue) {
        return new MappedValue(oMappedValue);
    }

    /**
     * Required for the base class {@see DbCache} implementation of the
     * interface class ILockFactory.
     * <p>
     * This is used to add a container of lockable {@see MappedValue} objects
     * to the {@see StripeLock} Mutex.
     * </p>
     * <p/>
     * <p>
     * This method is required for the extension of {@see DbCache}.
     * </p>
     *
     * @return The new instance of {@code MappedValue}.
     * @see ILockFactory
     * @see StripeLock
     */
    @Override
    public MappedValue getLock() {
        return new MappedValue();
    }

    /**
     * Allows sorting of {@code MappedValue} based off their object type and
     * label by use of the {@see SortByTypeLabel#compare} method.
     * <p/>
     * <p>
     * Implements interface {@code Comparator<MappedValue>} to impose an
     * ordering by comparisons of {@code MappedValue}'s.
     * </p>
     *
     * @see Comparator#compare
     */
    private class SortByTypeLabel implements Comparator<MappedValue> {
        /**
         * <b> Default Constructor </b>
         * <p>
         * Creates new instances of {@code SortByTypeLabel}
         * </p>
         */
        private SortByTypeLabel() {
        }

        /**
         * Compares {@code oLhs} to {@code oRhs}. Gives priority to
         * {@code MappedValue.m_nObsType} by first comparing and returning the
         * difference of these values if non-zero. Otherwise the
         * {@code MappedValue}'s are compared based off their labels via
         * {@see Text#compareIgnoreCase}.
         * <p/>
         * <p>
         * Needed for the implementation of the interface {@see Comparator}.
         * </p>
         *
         * @param oLhs Object to compare.
         * @param oRhs Object to compare to.
         * @return <pre>
         * 0         - {@code oLhs} is equivalent to {@code oRhs}.
         * otherwise - {@code oLhs} is not equivalent to {@code oRhs}.
         * </pre>
         */
        public int compare(MappedValue oLhs, MappedValue oRhs) {
            int nCompare = oLhs.m_nObsType - oRhs.m_nObsType;
            if (nCompare != 0)
                return nCompare;

            return Text.compareIgnoreCase(oLhs.m_sLabel, oRhs.m_sLabel);
        }
    }

    /**
     * Allows sorting of {@code CollectorValue} based off their collector id and
     * label by use of the {@link CollectorValue#compareTo } method.
     * <p/>
     * <p>
     * Implements interface {@code Comparator<CollectorValue>} to impose an
     * ordering by comparisons of {@code CollectorValue}'s.
     * </p>
     *
     * @see Comparator#compare
     */
    private class CollectorValue implements Comparable<CollectorValue> {
        /**
         * The collection service identification number.
         */
        private int m_nCollectorId;
        /**
         * The object type being mapped.
         */
        private int m_nObsType;
        /**
         * The label to map to the object type.
         */
        private CharSequence m_sLabel;

        /**
         * Creates a new instance of {@code CollectorValue} with its
         * corresponding attributes initialized to the supplied values.
         *
         * @param nCollectorId Collector service id for the new
         *                     {@code CollectorValue}
         * @param nObsType     Object type for the new {@code CollectorValue}.
         * @param sLabel       Label for the new {@code CollectorValue}.
         */
        CollectorValue(int nCollectorId, int nObsType, CharSequence sLabel) {
            m_nCollectorId = nCollectorId;
            m_nObsType = nObsType;
            m_sLabel = sLabel;
        }

        /**
         * Compares <i>this</i> to the parameter {@code oRhs}, giving priority
         * first to the collector id, then the object type, and finally the
         * label.
         * <p/>
         * <p>
         * Returns the difference between the collector id if nonzero. If zero,
         * it then returns the difference between the object types if nonzero.
         * Otherwise it then returns the comparison of the labels via
         * {@see Text#compareIgnoreCase}
         * </p>
         *
         * @param oRhs The {@code CollectorValue} to compare to <i>this.</i>
         * @return <pre>
         * 0         - CollectorValues are equivalent.
         * otherwise - not equivalent.
         * </pre>
         */
        public int compareTo(CollectorValue oRhs) {
            int nCompare = m_nCollectorId - oRhs.m_nCollectorId;
            if (nCompare != 0)
                return nCompare;

            nCompare = m_nObsType - oRhs.m_nObsType;
            if (nCompare != 0)
                return nCompare;

            return Text.compareIgnoreCase(m_sLabel, oRhs.m_sLabel);
        }
    }
}
