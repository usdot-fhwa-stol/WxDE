package wde.util.io;

import java.io.InputStream;

/**
 * The <tt>CharTokenizer</tt> is a convenience class for reading character
 * files through an input stream. It can be used to easily convert sets of
 * delimited and fixed-width characters into tokens stored in
 * <tt>StringBuilder</tt> objects.
 *
 * @author bryan.krueger
 */
public class CharTokenizer {
    /**
     * this initial buffer length should avoid memory reallocation
     * for a considerably large number of possible character file formats
     */
    private static int MIN_LINE_LENGTH = 160;

    private int m_nTokenIndex;
    private InputStream m_iInputStream;
    private Pattern m_oTokenDelim;
    private Pattern m_oSetDelim;
    private StringBuilder m_sSetBuffer = new StringBuilder(MIN_LINE_LENGTH);


    /**
     * Constructs a default <tt>CharTokenizer</tt> that can read comma-delimited
     * input streams with lines terminated by a newline character.
     */
    public CharTokenizer() {
        this(",", "\n");
    }


    /**
     * Constructs a <tt>CharTokenizer</tt> that can read input streams
     * formatted using the provided token string with lines terminated by
     * a newline character.
     *
     * @param sTokenDelim the string used as a delimiter marker
     */
    public CharTokenizer(String sTokenDelim) {
        this(sTokenDelim, "\n");
    }


    /**
     * Constructs a <tt>CharTokenizer</tt> that can read input streams
     * formatted using the provided token and line delimiter strings.
     *
     * @param sTokenDelim the string used as a delimiter marker
     * @param sSetDelim   the string used as a line terminator
     */
    public CharTokenizer(String sTokenDelim, String sSetDelim) {
        m_oTokenDelim = new Pattern(defaultString(sTokenDelim, ","));
        m_oSetDelim = new Pattern(defaultString(sSetDelim, "\n"));
    }


    /**
     * Sets the input stream to be read. Using the <tt>InputStream</tt>
     * interface allows many sources of character input to be read.
     *
     * @param iInputStream the source of character input
     */
    public void setInput(InputStream iInputStream) {
        // prepare to read the newly opened input stream
        m_iInputStream = iInputStream;
    }


    /**
     * Returns <tt>true</tt> if there are more characters available
     * that could be converted into tokens.
     *
     * @return <tt>true</tt> if more tokens are available
     */
    public boolean hasTokens() {
        // more tokens might be available if the current set
        // has more characters available for processing
        return (m_nTokenIndex < m_sSetBuffer.length());
    }


    /**
     * Attempts to read the next available token. The read token will be stored
     * in the provided token buffer. The token buffer will have a length of
     * zero if no more tokens are available to be read or there are no
     * characters between the current stream position and the next delimiter.
     *
     * @param sTokenBuffer the buffer where to save collected token characters
     */
    public void nextToken(StringBuilder sTokenBuffer) {
        // always clear the token buffer
        sTokenBuffer.setLength(0);

        // process each token for the current set
        boolean bTokenDelim = false;
        while (!bTokenDelim && m_nTokenIndex < m_sSetBuffer.length()) {
            char cNext = m_sSetBuffer.charAt(m_nTokenIndex++);
            if (cNext == '\r')
                continue;
            sTokenBuffer.append(cNext);
            // exit the loop when a token delimiter is encountered
            bTokenDelim = m_oTokenDelim.matches(cNext);
        }

        // remove token delimiter pattern as needed
        if (bTokenDelim) {
            sTokenBuffer.setLength(sTokenBuffer.length()
                    - m_oTokenDelim.length());
        }
    }


    /**
     * Attempts to read the next available token up to the number of requested
     * characters. This method is used to read tokens from fixed-width
     * formatted input streams. The read token will be stored in the provided
     * token buffer. The token buffer will have a length less than the requested
     * width or a width of zero there are fewer than <tt>nTokenWidth</tt>
     * charaters remaining.
     *
     * @param nTokenWidth  the number of characters that should make up
     *                     the next token
     * @param sTokenBuffer the buffer where to save collected token characters
     */
    public void nextToken(int nTokenWidth, StringBuilder sTokenBuffer) {
        // always clear the token buffer
        sTokenBuffer.setLength(0);

        // attempt to extract the requested number of characters
        while (nTokenWidth-- > 0 && m_nTokenIndex < m_sSetBuffer.length())
            sTokenBuffer.append(m_sSetBuffer.charAt(m_nTokenIndex++));
    }


    /**
     * Conveniently groups characters into a buffered set for conversion into
     * tokens. Sets are marked by the configured set delimiter string.
     *
     * @return <tt>false</tt> when the end of the input stream has been reached
     * @throws java.lang.Exception
     */
    public boolean nextSet() throws Exception {
        if (m_iInputStream == null)
            return false;

        // clear the set buffer and token index
        m_nTokenIndex = 0;
        m_sSetBuffer.setLength(0);

        int nNextChar = 0;
        boolean bSetEnd = false;
        // check for both new line characters and end-of-file
        while (!bSetEnd && (nNextChar = m_iInputStream.read()) >= 0) {
            m_sSetBuffer.append((char) nNextChar);
            bSetEnd = m_oSetDelim.matches((char) nNextChar);
        }

        // remove the line termination characters
        if (bSetEnd)
            m_sSetBuffer.setLength(m_sSetBuffer.length() - m_oSetDelim.length());

        // skip blank lines, there might not be any tokens
        // in the current set, but there might be more sets
        return (nNextChar >= 0);
    }


    /**
     * A convenience method used to set a string to a default value when
     * the input string is null or has zero length.
     *
     * @param sValue   the string value to be tested
     * @param sDefault the value to be returned if the input string is invalid
     * @return <tt>false</tt> when the end of the input stream has been reached
     */
    private String defaultString(String sValue, String sDefault) {
        if (sValue == null || sValue.length() == 0)
            return sDefault;

        return sValue;
    }


    private class Pattern {
        protected char[] m_cPattern;
        protected char[] m_cBuffer;


        protected Pattern(String sPattern) {
            m_cPattern = sPattern.toCharArray();
            m_cBuffer = new char[sPattern.length()];
        }


        public boolean matches(char cNext) {
            // shift the buffered character positions
            int nIndex = 0;
            while (nIndex < m_cBuffer.length - 1)
                m_cBuffer[nIndex] = m_cBuffer[++nIndex];
            // save the new character
            m_cBuffer[nIndex] = cNext;

            // compare the buffer to the pattern
            nIndex = m_cBuffer.length;
            boolean bMatch = true;
            while (nIndex-- > 0 && bMatch)
                bMatch = (m_cBuffer[nIndex] == m_cPattern[nIndex]);

            return bMatch;
        }


        public int length() {
            return m_cPattern.length;
        }
    }
}
