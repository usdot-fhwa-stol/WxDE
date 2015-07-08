/**
 * @file FtpConn.java
 */
package util.net;

import java.io.IOException;
import java.net.SocketTimeoutException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

/**
 * Provides a means of streaming files over an ftp connection.
 * <p>
 * Extends NetConn to provide a standard interface for connecting to and
 * streaming files across a network connection.
 * </p>
 */
public class FtpConn extends NetConn
{
	/**
	 * FTP connection client.
	 */
	FTPClient m_oFtp = new FTPClient();


	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code FtpConn}
	 * </p>
	 */
	private FtpConn()
	{
	}


	/**
	 * Initializes the class attributes to the corresponding provided values.
	 * Formats the url to contain only the server address, and sets the client
	 * communication timeouts.
	 * @param sUrl network connection address.
	 * @param sUsername connection username.
	 * @param sPassword password corresponding to the provided password.
	 */
	public FtpConn(String sUrl, String sUsername, String sPassword)
	{
		super(sUrl, sUsername, sPassword);

		// truncate the FTP URL to contain only the server address
		m_oStringBuilder.setLength(0);
		m_oStringBuilder.append(m_sUrl);
		// remove the ftp:// portion of the URL
		m_oStringBuilder.delete(0, 6);

		// remove any trailing slash
		if (m_sUrl.endsWith("/"))
			m_oStringBuilder.setLength(m_oStringBuilder.length() - 1);

		m_sUrl = m_oStringBuilder.toString();

		// set communication timeouts
		m_oFtp.setDefaultTimeout(20000);
		m_oFtp.setConnectTimeout(20000);
		m_oFtp.setDataTimeout(20000);
	}


	/**
	 * Connects to the ftp-url, and logs in with the given username and
	 * password.
	 *
	 * @return true if the connection is successful, false otherwise.
	 */
	@Override
	public boolean connect()
	{
		boolean bSuccess = false;

		try
		{
			m_oFtp.connect(m_sUrl);
			int nReplyCode = m_oFtp.getReplyCode();
			bSuccess = FTPReply.isPositiveCompletion(nReplyCode);
			if (bSuccess)
				m_oFtp.login(m_sUsername, m_sPassword);
		}
		catch (Exception oException)
		{
			m_oLog.write(this, "connect", oException.toString(), m_sUrl);
		}

		return bSuccess;
	}


	/**
	 * Logs out and disconnects the ftp-connection.
	 */
	@Override
	public void disconnect()
	{
		try
		{
			m_oFtp.logout();
			m_oFtp.disconnect();
		}
		catch (Exception oException)
		{
			m_oLog.write(this, "disconnect", oException.toString(), m_sUrl);
		}
	}


	/**
	 * Prepares a filestream across the ftp-connection to the provided filename.
	 * Passes this stream to the input-stream attribute.
	 *
	 * @param sFilename file contained at the location specified by the ftp-url.
	 *
	 * @return true if the stream was prepared successfully, false otherwise.
	 */
	@Override
	public boolean open(String sFilename)
	{
		boolean bSuccess = false;

		try
		{
			m_oFtp.setFileType(FTP.BINARY_FILE_TYPE);
			m_iInputStream = m_oFtp.retrieveFileStream(sFilename);
			bSuccess = (m_iInputStream != null);
		}
		catch (SocketTimeoutException oSocketTimeoutException)
		{
			m_oLog.write(this, "open",
				oSocketTimeoutException.toString(), sFilename);
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}

		return bSuccess;
	}


	/**
	 * Closes the input stream and ftp connection.
	 */
	@Override
	public void close() throws IOException
	{
		// the FTP object needs to finalize file transfers
		if (m_iInputStream != null)
		{
			m_iInputStream.close();
			m_iInputStream = null;
			m_oFtp.completePendingCommand();
		}
	}
}
