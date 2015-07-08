/************************************************************************
 * Source filename: DataProcessor.java
 * <p/>
 * Creation date: Feb 11, 2013
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

import java.util.concurrent.ConcurrentLinkedQueue;

public class DataProcessor extends Thread {
    private static final Logger logger = Logger.getLogger(DataProcessor.class);

    ConcurrentLinkedQueue<String> strQ = null;

    public DataProcessor() {
        strQ = ClarusRealtimeDataCollector.getIntance().getStringQ();
    }

    public void run() {
        String content = null;

        while (true) {
            synchronized (strQ) {
                if (!strQ.isEmpty())
                    content = strQ.remove();
            }
            if (content != null) {
                // placeholder for processing content, need to drop incomplete record
                logger.info("processing data queue");
                logger.info(content);

                content = null;
            } else {
                // wait for a while
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    logger.error(ie.getMessage());
                }
            }
        }

    }
}
