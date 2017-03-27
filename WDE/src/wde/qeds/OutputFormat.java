// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file OutputFormat.java
 */
package wde.qeds;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;

/**
 * Creates guidelines for output classes to follow to allow more generalized
 * use.
 * <p/>
 * <p>
 * Forces extensions to implement
 * {@link OutputFormat#fulfill(PrintWriter, ArrayList,
 * Subscription, String, int, long)}
 * </p>
 * <p>
 * Implements {@code Comparator} interface to enforce a standard interface
 * for comparisons. Extension of {@code OutputFormat} must implement the
 * required {@code compareTo} method.
 * </p>
 */
public abstract class OutputFormat implements Comparator<SubObs> {
    protected static String warning1 = "Request exceeded system limit - only the first %d rows are included";

    protected static String warning2 = "Request exceeded system limit - only those filtered from the first %d seconds are included";

    /**
     * File extension suffix.
     */
    protected String m_sSuffix;


    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code OutputFormat}
     * </p>
     */
    protected OutputFormat() {
    }


    /**
     * <b> Accessor </b>
     *
     * @return file extension suffix attribute.
     */
    String getSuffix() {
        return m_sSuffix;
    }


    /**
     * Extension must implement this method to print in the observation data
     * in the corresponding format.
     *
     * @param oWriter     Output stream to write data to.
     * @param oSubObsList List containing observation data to print.
     * @param oSub        Subscription filter.
     * @param sFilename   output filename printed on footer.
     * @param nId         subscription id.
     * @param lLimit      timerange for observations.
     * @param matchCheck  check additional match criteria.
     */
    abstract void fulfill(PrintWriter oWriter, ArrayList<SubObs> oSubObsList,
                          Subscription oSub, String sFilename, int nId, long lLimit, boolean matchCheck);
	/**
     * Extension must implement this method to print in the observation data
     * in the corresponding format.
     *
     * @param oWriter     Output stream to write data to.
     * @param oSubObsList List containing observation data to print.
     * @param oSub        FcstSubscription filter.
     * @param sFilename   output filename printed on footer.
     * @param nId         subscription id.
     * @param lLimit      timerange for observations.
     * @param matchCheck  check additional match criteria.
     */
    abstract void fulfill(PrintWriter oWriter, ArrayList<SubObs> oSubObsList,
                          FcstSubscription oSub, String sFilename, int nId, long lLimit, boolean matchCheck);
}
