package wde.cs.metar;

import java.io.FileWriter;

/**
 * The Report class is the base for all classes used to read in and store data
 * from METAR records.  It has 3 abstract functions, and a collection of
 * functions used throughout all the reports
 *
 * @author scot.lange
 */
public abstract class Report {
    /**
     * Buffer that all report subclasses will be trying to parse
     */
    protected static StringBuilder g_sBuffer;

    /**
     * Length of buffer.
     */
    protected static int g_nBufferLength;


    /**
     * integer value representing no data
     */
    protected static int nNO_DATA = Integer.MIN_VALUE;

    /**
     * double value representing no data
     */
    protected static double dNO_DATA = Double.NEGATIVE_INFINITY;

    /**
     * string value representing no data
     */
    protected static String sNO_DATA = "";

    /**
     * The number of valid values in the report
     */
    public int m_nObsCount = 0;

    /**
     * parse checks the group at nStartIndex to see if it matches the format
     * for that report.  If it does, it will read in all the applicable data,
     * and then return the index of the group following the last group that
     * was part of the current report.<BR>
     * If not, it will return nStartIndex and the next report will try to parse
     * from the same position.
     *
     * @param nStartIndex     Position in m_sBuffer to start parsing at
     * @param nRecursiveCount Number of groups past the current group to attempt
     *                        to parse, the the current one cannot be successfully parsed
     * @return The index for the rext report to start parsing at.  If parsing
     * fails, nStartIndex will be returned.  If it is successfull, it will
     * the index to the group after the last group used in parsing
     */
    public abstract int parse(int nStartIndex, int nRecursiveCount);


    /**
     * clearValues resets all the values in the report to their initial values
     * (generally strings to "", ints to Integer.MIN_VALUE, and doubles to
     * Double.NaN)
     */
    abstract void clearValues();

    @Override
    public abstract String toString();


    public abstract void toCSV(FileWriter oOut, boolean bLast) throws Exception;

    public abstract void printCSVHeader(FileWriter oOut, boolean bLast) throws Exception;


    /**
     * used to iterate through values held by a report and get their obsids
     *
     * @param nObservationIndex index of the observation.  This index does not
     *                          reference a specific observation value, it is simply to iterate through
     *                          the valid values that are held.
     * @return the obsID for the value at the given index
     */
    public String getObservationType(int nObservationIndex) {
        return sNO_DATA;
    }

    /**
     * used to iterate though values held by a report and get them
     *
     * @param nObservationIndex index of the observation.  This index does not
     *                          reference a specific observation value, it is simply to iterate through
     *                          the valid values that are held.  getObservationID identifies what kind
     *                          of observation it is
     * @return the value for the value at the given index
     */
    public double getObservationValue(int nObservationIndex) {
        return dNO_DATA;
    }


    /**
     * subString simply calls the StringBuilder substring function, but checks
     * that the start and end indexes are valid
     *
     * @param nStart
     * @param nEnd
     * @return
     */
    protected String substring(int nStart, int nEnd) {
        if (nStart >= 0 && nEnd <= g_nBufferLength)
            return g_sBuffer.substring(nStart, nEnd);
        else
            return "";
    }


    /**
     * @param oString
     * @return the integer value of oString, or a constant that represents
     * no data.
     */
    protected int parseInt(String oString) {
        try {
            return Integer.parseInt(oString);
        } catch (Exception oException) {
            return nNO_DATA;
        }
    }


    /**
     * @param oString
     * @return the double value of oString, or a constant representing no
     * data
     */
    protected double parseDouble(String oString) {
        try {
            return Double.parseDouble(oString);
        } catch (Exception oException) {
            return dNO_DATA;
        }
    }

    /**
     * the parse functions asumes that nStartIndex is the beginning of a group,
     * and attepmts to return on index to the start of the next group to be
     * parsed.  This function is just a check to ensure that happens.
     *
     * @param nStartIndex any index within the current group
     * @return if nStartIndex is the beginning of a group, it is returned.
     * Other wise the closer of the index to the previous and next groups will
     * be returned
     */
    protected int nearestGroupStart(int nStartIndex) {
        //If we already have the start of a group, just return it.
        if (nStartIndex >= g_nBufferLength)
            return g_nBufferLength - 1;
        if (g_sBuffer.charAt(nStartIndex - 1) == ' ')
            return nStartIndex;
        //Check to see if the previous or next group is closer, and return it.
        int nPrevious = this.previousStart(nStartIndex);
        int nNext = this.nextStart(nStartIndex);
        //Tie goes to next group
        if (nStartIndex - nPrevious < nNext - nStartIndex)
            return nPrevious;
        else
            return nNext;
    }


    /**
     * The METAR groups should be in a preset order, but ocassionally they are
     * out of order, or an errant space creates a group that none of the Report
     * subclasses will be able to parse.  tryNextGroup() allews the report
     * to attempt to parse the next nRecursiveCount groups if it fails to parse
     * the current group.
     *
     * @param nStartIndex     Index of the current group
     * @param nRecursiveCount number of recursive attempts to make before giving
     *                        up
     * @return
     */
    protected int tryNextGroup(int nStartIndex,
                               int nRecursiveCount, Report oParseReport) {
        if (nRecursiveCount > 0) {
            int nNextStart = nextStart(nStartIndex);
            if (nNextStart > g_nBufferLength - 3)
                return nStartIndex;
            int nRecursiveReturn =
                    oParseReport.parse(nNextStart, --nRecursiveCount);
            if (nRecursiveReturn != nNextStart)
                return nRecursiveReturn;
            else
                return nStartIndex;
        } else
            return nStartIndex;
    }

    /**
     * @param nStartIndex an intex within the current group
     * @return An index to the space following nStartIndex, or the length of
     * the buffer if there isn't a space following nStartIndex
     */
    protected int currentGroupEnd(int nStartIndex) {
        if (nStartIndex >= g_nBufferLength)
            return g_nBufferLength;
        int nEnd = g_sBuffer.indexOf(" ", nStartIndex);
        if (nEnd < 0)
            return g_nBufferLength;
        else
            return nEnd;
    }


    /**
     * @param nStartIndex an index within the current group
     * @param m_sBuffer   the buffer being parsed
     * @return an index to the start of the next group
     */
    protected int nextStart(int nStartIndex) {
        if (nStartIndex >= g_nBufferLength)
            return g_nBufferLength - 1;
        int nStart = g_sBuffer.indexOf(" ", nStartIndex) + 1;

        if (nStart < 1)
            return g_nBufferLength - 1;
        else
            return nStart;
    }


    /**
     * @param nStartIndex an index within the current group
     * @return an index to the end of the next group ( space following it,
     * not the last character in it)
     */
    protected int nextEnd(int nStartIndex) {
        if (nStartIndex >= g_nBufferLength)
            return g_nBufferLength;
        int nEnd = g_sBuffer.indexOf(" ", nextStart(nStartIndex));

        if (nEnd < 0)
            return g_nBufferLength;
        else
            return nEnd;
    }


    /**
     * @param nStartIndex an index within the current group
     * @return an index to the beginning of the previous group.
     */
    protected int previousStart(int nStartIndex) {
        if (nStartIndex >= g_nBufferLength)
            nStartIndex = g_nBufferLength - 1;
        int nStart = g_sBuffer.lastIndexOf(" ", previousEnd(
                nStartIndex) - 1) + 1;
        if (nStart < 0)
            return 0;
        else
            return nStart;
    }


    /**
     * @param nStartIndex an index within the current group
     * @return an index to the end of the next group (the space following it,
     * not the last character in it)
     */
    protected int previousEnd(int nStartIndex) {
        if (nStartIndex >= g_nBufferLength)
            nStartIndex = g_nBufferLength - 1;
        int nEnd = g_sBuffer.lastIndexOf(" ", nStartIndex);
        if (nEnd < 0)
            return 0;
        else
            return nEnd;
    }
}
