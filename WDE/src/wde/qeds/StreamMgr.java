package wde.qeds;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.SensorDao;
import wde.metadata.ISensor;
import wde.obs.IObs;
import wde.obs.IObsSet;
import wde.util.Config;
import wde.util.ConfigSvc;
import wde.util.threads.AsyncQ;
import wde.util.threads.ThreadPool;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;

public class StreamMgr extends AsyncQ<IObsSet> {
    private static final Logger logger = Logger.getLogger(StreamMgr.class);

    private static StreamMgr g_oInstance = new StreamMgr();

    private StringBuilder sBuffer = new StringBuilder();
    private ArrayList<OutputStream> m_oClients = new ArrayList<>();
    private SensorDao sensorDao;
    private ThreadPool m_oThreadPool;


    public StreamMgr() {
        m_oThreadPool = ThreadPool.getInstance();
        m_oThreadPool.execute(new Listener());

        String sName = getClass().getName();
        sensorDao = SensorDao.getInstance();
        WDEMgr.getInstance().register(getClass().getName(), this);
        logger.info(sName);
    }

    public StreamMgr getInstance() {
        return g_oInstance;
    }

    @Override
    public void run(IObsSet iObsSet) {
        for (int nIndex = 0; nIndex < iObsSet.size(); nIndex++) {
            IObs iObs = iObsSet.get(nIndex);
            int nSensorId = iObs.getSensorId();
            ISensor iSensor = sensorDao.getSensor(nSensorId);
            if (iSensor == null || iSensor.getDistGroup() == 1)
                continue;    // skip records that cannot be distributed

            sBuffer.setLength(0); // clear buffer
            sBuffer.append(iObs.getObsTypeId());
            sBuffer.append(",");
            sBuffer.append(nSensorId);
            sBuffer.append(",");
            sBuffer.append(iObs.getObsTimeLong());
            sBuffer.append(",");
            sBuffer.append(iObs.getLatitude());
            sBuffer.append(",");
            sBuffer.append(iObs.getLongitude());
            sBuffer.append(",");
            sBuffer.append(iObs.getElevation());
            sBuffer.append(",");
            sBuffer.append(String.format("%5.3f", iObs.getValue()));
            sBuffer.append(",");
            sBuffer.append(String.format("%4.2f", iObs.getConfValue()));
            sBuffer.append(",");
            sBuffer.append(String.valueOf(iObs.getQchCharFlag()));
            sBuffer.append("\r\n");

            synchronized (this) {
                int nClientIndex = m_oClients.size();
                while (nClientIndex-- > 0) {
                    OutputStream iOutput = m_oClients.get(nClientIndex);
                    try {
                        for (int nChar = 0; nChar < sBuffer.length(); nChar++)
                            iOutput.write(sBuffer.charAt(nChar));

                        iOutput.flush();
                    } catch (Exception oException) {
                        m_oClients.remove(nClientIndex);
                    }
                }
            }
        }

        WDEMgr.getInstance().queue(iObsSet); // queue obs for next process
    }


    private class Listener implements Runnable {
        private ServerSocket m_oServerSocket;


        private Listener() {
            try {
                Config oConfig = ConfigSvc.getInstance().getConfig(StreamMgr.this);
                m_oServerSocket = new ServerSocket(oConfig.getInt("port", 1083));
            } catch (Exception oException) {
            }
        }


        @Override
        public void run() {
            try {
                Socket oSocket = m_oServerSocket.accept();
                m_oThreadPool.execute(this);

                synchronized (StreamMgr.this) {
                    m_oClients.add(oSocket.getOutputStream());
                }
            } catch (Exception oException) {
            }
        }
    }
}
