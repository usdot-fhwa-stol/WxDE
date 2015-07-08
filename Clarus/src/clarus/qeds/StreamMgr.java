package clarus.qeds;


import java.io.OutputStream;
import java.net.Socket;
import java.net.ServerSocket;
import java.util.ArrayList;
import clarus.ClarusMgr;
import clarus.emc.ISensor;
import clarus.emc.Sensors;
import clarus.qedc.IObs;
import clarus.qedc.IObsSet;
import util.Config;
import util.ConfigSvc;
import util.threads.AsyncQ;
import util.threads.ThreadPool;


public class StreamMgr extends AsyncQ<IObsSet>
{
	private static StreamMgr g_oInstance = new StreamMgr();

	private StringBuilder sBuffer = new StringBuilder();
	private ArrayList<OutputStream> m_oClients = new ArrayList<>();
	private Sensors m_oSensors;
	private ThreadPool m_oThreadPool;
	

	public StreamMgr getInstance()
	{
		return g_oInstance;
	}


	public StreamMgr()
	{
		m_oThreadPool = ThreadPool.getInstance();
		m_oThreadPool.execute(new Listener());

		String sName = getClass().getName();
		m_oSensors = Sensors.getInstance();
		ClarusMgr.getInstance().register(getClass().getName(), this);
		System.out.println(sName);
	}


	@Override
	public void run(IObsSet iObsSet)
	{
		for (int nIndex = 0; nIndex < iObsSet.size(); nIndex++)
		{
			IObs iObs = iObsSet.get(nIndex);
			int nSensorId = iObs.getSensorId();
			ISensor iSensor = m_oSensors.getSensor(nSensorId);
			if (iSensor == null || iSensor.getDistGroup() < 2)
				continue;	// skip records that cannot be distributed
			
			sBuffer.setLength(0); // clear buffer
			sBuffer.append(iObs.getTypeId());
			sBuffer.append(",");
			sBuffer.append(nSensorId);
			sBuffer.append(",");
			sBuffer.append(iObs.getTimestamp());
			sBuffer.append(",");
			sBuffer.append(iObs.getLat());
			sBuffer.append(",");
			sBuffer.append(iObs.getLon());
			sBuffer.append(",");
			sBuffer.append(iObs.getElev());
			sBuffer.append(",");
			sBuffer.append(String.format("%5.3f", iObs.getValue()));
			sBuffer.append(",");
			sBuffer.append(String.format("%4.2f", iObs.getConfidence()));
			sBuffer.append(",");

			int nRun = iObs.getRun();
			int nFlags = iObs.getFlags();

			int nQch = 12;
			while (nQch-- > 0)
			{
				if ((nRun & 1) == 0)
				{
					if ((nFlags & 1) == 0)
						sBuffer.append('/');
					else
						sBuffer.append('-');
				}
				else
				{
					if ((nFlags & 1) == 0)
						sBuffer.append('N');
					else
						sBuffer.append('P');
				}
				
				nRun >>= 1;
				nFlags >>= 1;
			}
			sBuffer.append("\r\n");

			synchronized(this)
			{
				int nClientIndex = m_oClients.size();
				while (nClientIndex-- > 0)
				{
					OutputStream iOutput = m_oClients.get(nClientIndex);
					try
					{
						for (int nChar = 0; nChar < sBuffer.length(); nChar++)
							iOutput.write(sBuffer.charAt(nChar));

						iOutput.flush();
					}
					catch (Exception oException)
					{
						m_oClients.remove(nClientIndex);
					}
				}
			}
		}

		ClarusMgr.getInstance().queue(iObsSet); // queue obs for next process
	}
	
	
	private class Listener implements Runnable
	{
		private ServerSocket m_oServerSocket;


		private Listener()
		{
			try
			{
				Config oConfig = ConfigSvc.getInstance().getConfig(StreamMgr.this);
				m_oServerSocket = new ServerSocket(oConfig.getInt("port", 1083));
			}
			catch (Exception oException)
			{
			}
		}


		@Override
		public void run()
		{
			try
			{
				Socket oSocket = m_oServerSocket.accept();
				m_oThreadPool.execute(this);

				synchronized(StreamMgr.this)
				{
					m_oClients.add(oSocket.getOutputStream());
				}
			}
			catch (Exception oException)
			{
			}
		}
	}
}
