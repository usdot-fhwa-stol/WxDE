package wde.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * The ISODateFormat is an extension to the SimpleDateFormat intended to
 * automatically handle the most common ISO8601 timestamp format used in XML.
 */
public class ISODateFormat extends SimpleDateFormat {
    /**
     * buffer used for removing the trailing colon in an ISO8601 timestamp
     */
    protected StringBuilder m_sBuffer = new StringBuilder();
    /**
     * flag for determining if the provided date format should look
     * for modifying the time zone information
     */
    boolean m_bISO8601;


    /**
     * The default constructor is protected as the class requires a pattern
     */
    protected ISODateFormat() {
    }


    /**
     * Constructor that accepts a timestamp format
     * used to parse strings into date objects
     *
     * @param sPattern the timestamp format to use.
     */
    public ISODateFormat(String sPattern) {
        super(sPattern);
        m_bISO8601 = sPattern.endsWith("Z");
    }


    /**
     * When the date format pattern ends with a time zone specifier,
     * the parse method checks for a colon within the string to be parsed.
     * The colon is removed from the time zone portion when it is found
     * so that the string can be interpreted as an RFC 822 date and time.
     *
     * @param sSource the date and time string to be converted
     * @return the date object created from the source date and time
     * @throws java.text.ParseException
     */
    @Override
    public Date parse(String sSource)
            throws ParseException {
        // check for a timezone offset colon when the pattern includes the 'Z'
        if (m_bISO8601) {
            int nIndex = sSource.length() - 3;
            if (sSource.charAt(nIndex) == ':') {
                // remove the colon from the timezone offset before parsing
                m_sBuffer.setLength(0);
                m_sBuffer.append(sSource);
                m_sBuffer.deleteCharAt(nIndex);

                sSource = m_sBuffer.toString();
            }
        }

        return super.parse(sSource);
    }
}
