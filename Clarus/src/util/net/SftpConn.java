/**
 * @HttpConn.java
 */
package util.net;

import java.io.IOException;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * Provides a means of streaming files over an sftp connection.
 * <p>
 * Extends NetConn to provide a standard interface for connecting to and
 * streaming files across a network connection.
 * </p>
 */
public class SftpConn extends NetConn
{
	private ChannelSftp m_oChannel;
	private JSch m_oJSch;
	private Session m_oSession;

	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code SftpConn}
	 * </p>
	 */
	private SftpConn()
	{
	} 


	/**
	 * Initializes the class attributes to the corresponding provided values.
	 * @param sUrl root network connection address.
	 * @param sUsername connection username.
	 * @param sPassword password corresponding to the provided password.
	 */
	public SftpConn(String sUrl, String sUsername, String sPassword)
	{
		super(sUrl, sUsername, sPassword);

		m_oStringBuilder.append(sUrl); // reduce URL to host name
		m_oStringBuilder.delete(0, 7); // remove "sftp://"
		if (sUrl.endsWith("/")) // remove trailing separator
			m_oStringBuilder.setLength(m_oStringBuilder.length() - 1);
		
		m_sUrl = m_oStringBuilder.toString();
		m_oJSch = new JSch();
	}


	/**
	 * Pseudo-connection test for valid remote file options.
	 *
	 * @return true if file options are initialized, false otherwise.
	 */
	@Override
	public boolean connect()
	{
		boolean bSuccess = false;

		try
		{
			m_oSession = m_oJSch.getSession(m_sUsername, m_sUrl);
			m_oSession.setPassword(m_sPassword);
			m_oSession.setConfig("StrictHostKeyChecking", "no");

			m_oSession.connect();
			m_oChannel = (ChannelSftp)m_oSession.openChannel("sftp");
			m_oChannel.connect();

			bSuccess = true;
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
		}

		return bSuccess;
	}


	/**
	 * Free session resources.
	 */
	@Override
	public void disconnect()
	{
		m_oChannel.disconnect();
		m_oSession.disconnect();
	}


	/**
	 * Opens the provided file stored at the root URL, inserting the username
	 * and password if provided. Connects the input stream to this file.
	 * @param sFilename name of the file to stream, stored at the location
	 * specified by the provided URL.
	 * @return true if the connection was a success, false otherwise.
	 */
	@Override
	public boolean open(String sFilename)
	{
		boolean bSuccess = false;

		try
		{
			m_iInputStream = m_oChannel.get(sFilename);
			bSuccess = true;
		}
		catch (Exception oException)
		{
			oException.printStackTrace();
//			m_oLog.write(this, "open", oException.toString(), m_sFilename);
		}

		return bSuccess;
	}


	/**
	 * Closes the input stream via {@link java.io.InputStream#close()}
	 */
	@Override
	public void close() throws IOException
	{
		if (m_iInputStream != null)
		{
			m_iInputStream.close();
			m_iInputStream = null;
		}
	}
}
