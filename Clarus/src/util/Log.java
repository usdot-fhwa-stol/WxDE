/**
 * @file Log.java
 */
package util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Provides methods to output time-stamped debug, and log information to
 * standard out.
 * <p>
 * Singleton class whose instance can be retrieved via
 * {@link Log#getInstance()}.
 * </p>
 */
public class Log
{
	/**
	 * Pointer to the singleton instance of {@code Log}
	 */
	private static Log g_oInstance = new Log();

	/**
	 * Current timestamp.
	 */
	private Date m_oDate = new Date();
	/**
	 * Timestamp format.
	 */
	private SimpleDateFormat m_oDateFormat =
		new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


	/**
	 * <b> Accessor </b>
	 * @return the singleton instance of {@code Log}.
	 */
	public static Log getInstance()
	{
		return g_oInstance;
	}


	/**
	 * Logs this call to create a new instance of Log.
	 */
	private Log()
	{
		write(this, "constructor");
	}


	/**
	 * Prints the current time in the format specified by the date format
	 * attribute.
	 */
	private synchronized void writeTimestamp()
	{
		m_oDate.setTime(System.currentTimeMillis());
		System.out.print(m_oDateFormat.format(m_oDate));
		System.out.print(" ");
	}


	/**
	 * Logs a timestamped call to the provided method, with its associated
	 * parameters or debug information.
	 * @param oObject calling object whose name will be stamped with this log.
	 * @param sMethod name of the method to log.
	 * @param sMsg parameters or debug info associated with this method.
	 */
	public void write(Object oObject, String sMethod, CharSequence ... sMsg)
	{
		writeTimestamp();

		System.out.print(oObject.getClass().getName());
		System.out.print(" ");
		System.out.print(sMethod);
		System.out.print(" ");

		for (int nIndex = 0; nIndex < sMsg.length; nIndex++)
		{
			if (nIndex > 0)
				System.out.print(" ");

			System.out.print(sMsg[nIndex]);
		}
		System.out.println();
	}


	/**
	 * Prints a timestamped stack trace of the provided exception to standard
	 * out.
	 * @param oException exception to print.
	 */
	public void write(Exception oException)
	{
		writeTimestamp();
		oException.printStackTrace(System.out);
	}
}
