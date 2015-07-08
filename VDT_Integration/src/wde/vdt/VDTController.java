/************************************************************************
 * Source filename: VDTController.java
 * 
 * Creation date: Oct 24, 2013
 * 
 * Author: zhengg
 * 
 * Project: VDT Integration
 * 
 * Objective:
 * 
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import wde.vdt.probe.ProbeAssembly;

public class VDTController extends TimerTask {
  
    private static final Logger logger = Logger.getLogger(VDTController.class);
    private static VDTController instance = null;
    private GregorianCalendar now = new GregorianCalendar();
    
    private ProbeAssembly probeAssembly = null;
    private VDTDataIngester vdtDataIngester = null;

    private Properties prop = null;
    private int pollingInterval;
    private long dataDelay;
    private String vdtCommand = null;
    private File vdtCommandFolder = null;
    
    private Timer timer = null;
    
    public static void main(String[] args)
    {
        DOMConfigurator.configure("config/vdt_log4j.xml");
        VDTController.getInstance();
    }

    public static VDTController getInstance()
    {
        if (instance == null)
            instance = new VDTController();
        
        return instance;
    }
    
    public void run()
    {
        now.setTimeInMillis(System.currentTimeMillis() - pollingInterval - dataDelay);
        
        try {
            probeAssembly.generateProbeMessage(now);
            
            // Launch VDT to process
            logger.info("Launching the VDT");
            Process proc = Runtime.getRuntime().exec(vdtCommand, null, vdtCommandFolder);
            
            proc.waitFor();
            
            logger.info("VDT finished processing");
            
            vdtDataIngester.processData(now);
            
            // Clean up observation older than one hour
            long currentTime = now.getTimeInMillis();
            if (currentTime % 3600000 < 5000)
                vdtDataIngester.cleanup(currentTime - 3600000);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private VDTController()
    {
        prop = new Properties();
        loadPropertiesFile();
        
        probeAssembly = ProbeAssembly.getInstance();
        vdtDataIngester = VDTDataIngester.getInstance();
        
        timer = new Timer();

        long currentTime = System.currentTimeMillis();
        
        long delay = pollingInterval - (currentTime % pollingInterval) + dataDelay;
        
        logger.info("wait for " + delay/1000 + " seconds before the first run");
        
        timer.scheduleAtFixedRate(this, delay, pollingInterval);
    }
    
    /**
     * 
     */
    private void loadPropertiesFile()
    {
        logger.info("Loading properties file");
        
        String separator = System.getProperty("file.separator");
        String path = System.getProperty("user.dir") + separator + "config" + separator + "vdt_config.properties";
    
        try
        {
            FileInputStream fis = new FileInputStream(path);
            prop.load(fis);
            fis.close();
            
            // default 1 minutes for testing
            pollingInterval = Integer.valueOf(prop.getProperty("pollinginterval", "1")).intValue() * 60000; 
            
            // default 5 seconds
            dataDelay = Integer.valueOf(prop.getProperty("delay", "5")).intValue() * 1000; 
            
            // Need to detect duplicates, maybe use the 
            
            // Command to launch the VDT
            vdtCommand = prop.getProperty("vdtcommand");
            String cmdFolder = prop.getProperty("vdtcommandfolder");
            
            logger.info("Looking for folder " + cmdFolder);
            vdtCommandFolder = new File(cmdFolder);
        }
        catch (IOException e)
        {
            e.printStackTrace();
            System.exit(-1);
        }
    }
}
