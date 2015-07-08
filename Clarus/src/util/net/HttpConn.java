/**
 * @HttpConn.java
 */
package util.net;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;

/**
 * Provides a means of streaming files over an http connection.
 * <p>
 * Extends NetConn to provide a standard interface for connecting to and
 * streaming files across a network connection.
 * </p>
 */
public class HttpConn extends NetConn
{
	/**
	 * <b> Default Constructor </b>
	 * <p>
	 * Creates new instances of {@code HttpConn}
	 * </p>
	 */
	private HttpConn()
	{
	}


	/**
	 * Initializes the class attributes to the corresponding provided values.
	 * @param sUrl root network connection address.
	 * @param sUsername connection username.
	 * @param sPassword password corresponding to the provided password.
	 */
	public HttpConn(String sUrl, String sUsername, String sPassword)
	{
		super(sUrl, sUsername, sPassword);
	}


	/**
	 * Opens the provided file stored at the root url, providing the username
	 * and password if provided. Connects the input stream to this file.
	 * @param sFilename name of the file to stream, stored at the location
	 * specified by the provided url.
	 * @return true if the connection was a success, false otherwise.
	 */
	@Override
	public boolean open(String sFilename)
	{
		boolean bSuccess = false;
		m_oStringBuilder.setLength(0);
		m_oStringBuilder.append(m_sUrl);

		if (m_sUrl.endsWith("/"))
		{
			// remove the extra slash from the url
			if (sFilename.startsWith("/"))
				m_oStringBuilder.setLength(m_oStringBuilder.length() - 1);
		}
		else
		{
			// add the needed slash to the composite url
			if (!sFilename.startsWith("/"))
				m_oStringBuilder.append("/");
		}
		// create the composite URL
		m_oStringBuilder.append(sFilename);
		String sUrl = m_oStringBuilder.toString();

		try
		{
			// attempt to establish an HTTP connection
			URL oUrl = new URL(sUrl);
			URLConnection oUrlConnection = oUrl.openConnection();

			// set the username and password if they were provided
			if (m_sUsername != null && m_sPassword != null &&
				m_sUsername.length() > 0 && m_sPassword.length() > 0)
			{
				m_oStringBuilder.setLength(0);
				m_oStringBuilder.append(m_sUsername).append(":").append(m_sPassword);
				String sEncoding = new sun.misc.BASE64Encoder().encode
					(m_oStringBuilder.toString().getBytes());

				m_oStringBuilder.setLength(0);
				m_oStringBuilder.append("Basic ").append(sEncoding);
				oUrlConnection.setRequestProperty
					("Authorization", m_oStringBuilder.toString());
			}

			m_iInputStream = oUrlConnection.getInputStream();
			bSuccess = (m_iInputStream != null);
		}
		catch (Exception oException)
		{
			m_oLog.write(this, "open", oException.toString(), m_sUrl);
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
