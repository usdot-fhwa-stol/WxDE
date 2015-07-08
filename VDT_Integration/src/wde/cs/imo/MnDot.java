package wde.cs.imo;

import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

public class MnDot
{
    private static final Logger logger = Logger.getLogger(MnDot.class);
    
	public static final int FILE_AGE = 300000;  // 5 minutes
	public static final int MSGSIZE_MAX = 4096;
	
	private static final String[] SHORT_DAY = 
	{
		"-1 ", "-2 ", "-3 ", "-4 ", "-5 ", "-6 ", "-7 ", "-8 ", "-9 "
	};

	private int m_nPrevGroup = -1;
	private int m_nInterval;
	private int m_nPort;
	private String m_sRoot;
	private String mixedFile;
	private InputStream m_iInput;
	private OutputStream m_iOutput;
	private InetAddress m_oAddress;
	private HashMap<String, FileWriter> m_iFileMap = new HashMap<>();
	private GregorianCalendar m_oNow = new GregorianCalendar();
	private SimpleDateFormat m_oDateFormat = new SimpleDateFormat("yyyyMMdd");
	private SimpleDateFormat m_oTsFormat =
		new SimpleDateFormat("yyyyMMdd'.'HHmm");
	
	private FileWriter mixedFileWriter;

	public MnDot(String[] sArgs)
	{
		try
		{
			m_oAddress = InetAddress.getByName(sArgs[0]);
			m_nPort = Integer.parseInt(sArgs[1]);
			m_nInterval = Integer.parseInt(sArgs[2]);
			m_sRoot = sArgs[3];
			mixedFile = sArgs[4];
		}
		catch (Exception oException)
		{
		}
	}


	public boolean checkInstance()
	{
		// last network interaction must be less than three minutes old
		File oFile = new File(m_sRoot + "heartbeat.bin");
		return (oFile.exists() && 
			(System.currentTimeMillis() - oFile.lastModified() < FILE_AGE));
	}
	

	public static void main(String[] sArgs)
    {
        DOMConfigurator.configure("config/vdt_log4j.xml");
        
    	// address, port, interval, destination directory
    	MnDot oMnDot = new MnDot(sArgs);
    
    	for(;;) // loops only when bad data are received
    	{
    		if (!oMnDot.checkInstance())
    			oMnDot.recv();
    
    		try
    		{
    			Thread.sleep(FILE_AGE);
    		}
    		catch (Exception oThreadException)
    		{
    		}
    	}
    }


    private static void writeConsole(CharSequence oSeq, String sPrefix)
	{
		for (int nIndex = 0; nIndex < oSeq.length(); nIndex++)
			System.out.print(oSeq.charAt(nIndex));
	}


	private void writeFile(StringBuilder sBuffer, String sPrefix)
		throws Exception
	{
		// close time-segmented files
		m_oNow.setTimeInMillis(System.currentTimeMillis());
		int nNextGroup = m_oNow.get(Calendar.MINUTE) / m_nInterval;
		if (nNextGroup != m_nPrevGroup) // new group needed
		{
			Iterator<FileWriter> iFileIt = m_iFileMap.values().iterator();
			while (iFileIt.hasNext())
				iFileIt.next().close();
			
			if (mixedFileWriter != null) {
			    mixedFileWriter.close();
			    mixedFileWriter = null;
			}

			m_nPrevGroup = nNextGroup;
			m_iFileMap.clear(); // remove all file objects
		}

		FileWriter oWriter = m_iFileMap.get(sPrefix);
		long longTime = m_oNow.getTime().getTime();
		long time = longTime - (longTime % (m_nInterval * 60000));
		m_oNow.setTimeInMillis(time);
		String postfix = "." + m_oTsFormat.format(m_oNow.getTime()) + ".txt";
        m_oNow.set(Calendar.MINUTE, m_nPrevGroup * m_nInterval);
        Date oDate = m_oNow.getTime();
        
		if (oWriter == null)
		{
			oWriter = createFileWriter(sPrefix, oDate, postfix);
			m_iFileMap.put(sPrefix, oWriter);
		}
		
		if (mixedFileWriter == null)
		    mixedFileWriter = createFileWriter(mixedFile, oDate, postfix);

		String recvTime = String.valueOf(System.currentTimeMillis());
		mixedFileWriter.write(recvTime);
		
		for (int nIndex = 0; nIndex < sBuffer.length(); nIndex++) {
			oWriter.write(sBuffer.charAt(nIndex));
			mixedFileWriter.write(sBuffer.charAt(nIndex));
		}
	}
	
	private FileWriter createFileWriter(String sPrefix, Date date, String postfix)
	    throws Exception
	{
	    FileWriter fw = null;
        String separator = System.getProperty("file.separator");
	    
	    StringBuilder sFilename = new StringBuilder(m_sRoot);
        sFilename.append(sPrefix);
        sFilename.append(separator);
        sFilename.append(m_oDateFormat.format(date));
        new File(sFilename.toString()).mkdirs();
        
        sFilename.append('/');
        sFilename.append(sPrefix);
        sFilename.append(postfix);

        fw = new FileWriter(sFilename.toString());
	    
	    return fw;
	}


	private void send(CharSequence oSeq)
	{
		try
		{
			for (int nIndex = 0; nIndex < oSeq.length(); nIndex++)
				m_iOutput.write(oSeq.charAt(nIndex));

			m_iOutput.flush(); // completely write contents to stream
			writeConsole(oSeq, "O ");
		}
		catch (Exception oException)
		{
		}
	}


	private void recv()
	{
		int nByte = 70000;
		Socket oSocket = null;
		StringBuilder sIn = new StringBuilder(MSGSIZE_MAX);

		try
		{
			oSocket = new Socket(m_oAddress, m_nPort);
			oSocket.setSoTimeout(nByte);
			m_iInput = oSocket.getInputStream();
			m_iOutput = oSocket.getOutputStream();

			send(">Logon:snow,ice;\r\n"); // initiate authentication

			for (;;) // indefinitely repeat
			{
				sIn.setLength(0); // reset the input buffer
				do
				{
					nByte = m_iInput.read();
					sIn.append((char)nByte);
				}
				while (nByte != '\n' && sIn.length() < MSGSIZE_MAX);
				
				if (sIn.length() >= MSGSIZE_MAX)
					throw new Exception(); // force buffer content capture

				// prepend zero for days less than 10 to correct bad date format
				int nOuter = 9;
				while (nOuter-- > 0)
				{
					int nInner = sIn.indexOf(SHORT_DAY[nOuter]);
					if (nInner >= 0)
						sIn.insert(++nInner, "0");
				}				
				
				writeConsole(sIn, "I "); // present received message

				String sPrefix = sIn.substring(sIn.indexOf(">") + 1, 
					sIn.indexOf(":")); // extract the file prefix

				if (sPrefix.compareTo("Ping") == 0 || 
					sPrefix.compareTo("Accepted") == 0)
				{
					if (sPrefix.compareTo("Ping") == 0)
					{
						sIn.setCharAt(sIn.indexOf("Ping") + 1, 'o');
						sIn.setLength(sIn.lastIndexOf(","));
						sIn.append(",Client$Demo;\r\n");
						send(sIn); // respond with Pong command
						
						// write heartbeat file
						RandomAccessFile oHeartbeat = 
							new RandomAccessFile(m_sRoot + "heartbeat.bin", "rw");
						oHeartbeat.writeLong(System.currentTimeMillis());
						oHeartbeat.close();
					}
					else
						send(">Subscribe;\r\n"); // received Accepted
				}
				else {
					writeFile(sIn, sPrefix);
				}
			}
		}
		catch (Exception oBufferException)
		{
			try
			{
				// append buffer content to error log for troubleshooting
				FileWriter oWriter = new FileWriter(m_sRoot + "error.log", true);
				for (int nIndex = 0; nIndex < sIn.length(); nIndex++)
					oWriter.write(sIn.charAt(nIndex));
				oWriter.write("\r\n"); // separate multiple error entries
				oWriter.write("\r\n");
				oWriter.write("\r\n");
				oWriter.close();				

				oSocket.close();
			}
			catch (Exception oLogException)
			{
			}
		}
	}
}
