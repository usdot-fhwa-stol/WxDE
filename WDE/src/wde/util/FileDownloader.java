/************************************************************************
 * Source filename: FileDownloader.java
 * <p/>
 * Creation date: Feb 14, 2013
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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

public class FileDownloader {

    private static final Logger logger = Logger.getLogger(FileDownloader.class);

    public static void main(String[] args) {
        FileDownloader fd = new FileDownloader();
        DOMConfigurator.configure("wdecs_config/log4j.xml");

        // Feb 14, 2013
        fd.download("http://wdecol.synesis-partners.com:1083/queryResults.jsp?obs=0&region=0,-180,85,180&timeRange=1360854497000,1360858097000", "c:/tmp/test123.csv", true);
    }

    public void download(String urlStr, String targetFilePath, boolean compress) {
        try {
           /*
            * Get a connection to the URL and start up a buffered reader.
            */
            long startTime = System.currentTimeMillis();

            logger.info("Connecting to " + urlStr + " ...");

            URL url = new URL(urlStr);
            url.openConnection();
            InputStream reader = url.openStream();

           /*
            * Setup a buffered file writer to write out what we read from the website.
            */
            File targetFile = new File(targetFilePath);
            targetFile.getParentFile().mkdirs();
            FileOutputStream writer = new FileOutputStream(targetFile);
            byte[] buffer = new byte[153600];
            int totalBytesRead = 0;
            int bytesRead = 0;

            logger.info("Reading 150KB blocks at a time.");

            while ((bytesRead = reader.read(buffer)) > 0) {
                writer.write(buffer, 0, bytesRead);
                buffer = new byte[153600];
                totalBytesRead += bytesRead;
            }

            long endTime = System.currentTimeMillis();

            logger.info("Done. " + (new Integer(totalBytesRead).toString()) + " bytes read (" + (new Long(endTime - startTime).toString()) + " millseconds).\n");

            if (compress) {
                String outputFileName = targetFilePath.substring(0, targetFilePath.indexOf("csv")) + "tar.gz";
                File outputFile = new File(outputFileName);
                FileCompressor.compressFile(targetFile, outputFile);
            }

            writer.close();
            reader.close();

            if (compress)
                targetFile.delete();
        } catch (MalformedURLException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        }
    }
}
