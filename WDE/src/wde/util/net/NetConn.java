/**
 * @file NetConn.java
 */
package wde.util.net;

import java.io.IOException;
import java.io.InputStream;

/**
 * Provides a standard interface/methods to connect and read from a network
 * connected data stream.
 * <p>
 * Abstract class, all extensions must implement abstract methods.
 * </p>
 * <p>
 * Extends {@code InputStream} to provide a method to read input from the
 * stream.
 * </p>
 */
public abstract class NetConn extends InputStream {
    /**
     * Root network connection address.
     */
    protected String m_sUrl;
    /**
     * Connection username.
     */
    protected String m_sUsername;
    /**
     * Connection password corresponding to the username.
     */
    protected String m_sPassword;
    /**
     * Input stream that reads from the connection to the supplied url.
     */
    protected InputStream m_iInputStream;
    /**
     * Used for formatting the url as desired.
     */
    protected StringBuilder m_oStringBuilder = new StringBuilder();

    /**
     * <b> Default Constructor </b>
     * <p>
     * Creates new instances of {@code NetConn}
     * </p>
     */
    protected NetConn() {
    }


    /**
     * Initializes the class attributes to the corresponding provided values.
     *
     * @param sUrl      network connection address.
     * @param sUsername connection username.
     * @param sPassword password corresponding to the provided password.
     */
    protected NetConn(String sUrl, String sUsername, String sPassword) {
        m_sUrl = sUrl;
        m_sUsername = sUsername;
        m_sPassword = sPassword;
    }


    /**
     * Placeholder method for extensions. Should be used to connect to the
     * socket.
     *
     * @return true.
     */
    public boolean connect() {
        return true;
    }


    /**
     * Placeholder method for extensions. Should be used to disconnect from the
     * socket.
     */
    public void disconnect() {
    }


    /**
     * Extensions must implement this method. Should be used to open the
     * provided file contained at the location of the provided URL.
     *
     * @param sFilename file to open.
     * @return true if the connection was a success, false otherwise.
     */
    public abstract boolean open(String sFilename);


    /**
     * Reads the next byte of data from the input stream member. Blocks until
     * there's input data, the end of stream is detected, or an error is thrown.
     *
     * @return next byte of data, or -1 if end of stream is detected.
     * @see InputStream#read()
     */
    @Override
    public int read() throws IOException {
        return m_iInputStream.read();
    }


    /**
     * Extensions must provide a definition for this method. Used to close
     * the stream. Closes this input stream and releases any system resources
     * associated with the stream.
     *
     * @see InputStream#close()
     */
    @Override
    public abstract void close() throws IOException;
}
