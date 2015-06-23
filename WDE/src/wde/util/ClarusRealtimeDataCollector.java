/************************************************************************
 * Source filename: ClarusCollector.java
 * <p/>
 * Creation date: February 11, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WxDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.util;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ClarusRealtimeDataCollector {
    private static final Logger logger = Logger.getLogger(ClarusRealtimeDataCollector.class);

    private static ClarusRealtimeDataCollector instance = null;

    private Properties prop = null;

    private String clarusServer = null;

    private int port = 1082; // default port

    private int bufferSize = 1024; // default buffer size

    private int retryWaitTime = 1000; // default wait time if no content is available in socket

    private int readAgainWaitTime = 100; // default wait time if some content has been retrieved

    private String newLine = "\r\n"; // default new line

    private ConcurrentLinkedQueue<String> strQ = null;

    /**
     *
     */
    private ClarusRealtimeDataCollector() {
        prop = new Properties();
        strQ = new ConcurrentLinkedQueue<String>();
        DOMConfigurator.configure("config/wdecs_log4j.xml");
        loadPropertiesFile();
    }

    /**
     * @return a reference to the ClarusCollector singleton.
     */
    public static ClarusRealtimeDataCollector getIntance() {
        if (instance == null)
            instance = new ClarusRealtimeDataCollector();

        return instance;
    }

    /**
     * @param args
     */
    public static void main(String args[]) {
        ClarusRealtimeDataCollector cc = ClarusRealtimeDataCollector.getIntance();

        // Need to create a separate thread to put the processor on standby
        DataProcessor dp = new DataProcessor();
        dp.start();

        cc.collectData();
    }

    public ConcurrentLinkedQueue<String> getStringQ() {
        return strQ;
    }

    /**
     *
     */
    private void loadPropertiesFile() {
        logger.info("Loading properties file");
        boolean terminate = false;

        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "wdecs_config.properties";

        try {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();

            clarusServer = prop.getProperty("clarusserver");
            port = Integer.parseInt(prop.getProperty("port"));
            bufferSize = Integer.parseInt(prop.getProperty("buffersize"));
            retryWaitTime = Integer.parseInt(prop.getProperty("retrywaittime"));
            readAgainWaitTime = Integer.parseInt(prop.getProperty("readagainwaittime"));
            newLine = prop.getProperty("newline");
            System.out.println(newLine);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        if (terminate)
            System.exit(-1);
    }

    /**
     *
     */
    private void collectData() {
        Socket socket = null;
        try {
            socket = new Socket(clarusServer, port);
            InputStream istream = socket.getInputStream();
            String residualStr = "";

            byte[] b = new byte[bufferSize];
            int len = -1;
            while (true) {
                len = istream.read(b, 0, b.length);
                if (len > -1) {
                    String content = residualStr + new String(b);

                    int clipIndex = content.lastIndexOf(newLine);
                    ;

                    if (clipIndex != -1) {
                        strQ.add(content.substring(0, clipIndex + 2));
                        logger.info("added " + len + " bytes to the data queue");
                        residualStr = content.substring(clipIndex);
                    } else
                        residualStr = content;

                    for (int i = 0; i < b.length; i++)
                        b[i] = 0;

                    try {
                        Thread.sleep(readAgainWaitTime);
                    } catch (InterruptedException ie) {
                        logger.error(ie.getMessage());
                    }
                } else {
                    try {
                        Thread.sleep(retryWaitTime);
                    } catch (InterruptedException ie) {
                        logger.error(ie.getMessage());
                    }
                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            logger.error(ioe.getMessage());
        } finally {
            try {
                if (socket != null)
                    socket.close();
            } catch (IOException ioe) {
                logger.error(ioe.getMessage());
                // Send an email - Look in the wde.qeds package at ContribMonitor. The method is sendMail

                ioe.printStackTrace();
            }
        }
    }
}
