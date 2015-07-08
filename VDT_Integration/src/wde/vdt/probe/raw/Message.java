/************************************************************************
 * Source filename: Message.java
 * 
 * Creation date: Oct 4, 2013
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

package wde.vdt.probe.raw;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;

import org.apache.log4j.Logger;

public abstract class Message {
    
    private static final Logger logger = Logger.getLogger(Message.class);
    
    protected static final int OFFSET = 1;
    
    private static SimpleDateFormat timeFormatter;

    private String agency = null;

    private String esn  = null;
    
    private String vName = null;
    
    private Date acqTime = null;
    
    private Date recTime = null;
    
    private double lat;
    
    private double lon;
    
    private float course;
    
    private int qual;
    
    private float vel;
    
    private boolean populated;
    
    static {
        timeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleTimeZone stz = new SimpleTimeZone(-21600000, "USA/Central", Calendar.MARCH, 8, -Calendar.SUNDAY, 
            7200000, Calendar.NOVEMBER, 8, -Calendar.SUNDAY, 7200000, 3600000);
        timeFormatter.setTimeZone(stz);
    }

    /**
     * @param rawData
     */
    public Message(String recvTime, String[] rawData) {
        
        populated = false;
        
        GregorianCalendar now = new GregorianCalendar();
        now.setTimeInMillis(System.currentTimeMillis());
        recTime = new Date();
        recTime.setTime(Long.parseLong(recvTime));
        
        if (rawData == null) {
            logger.error("Input array is null");
            return;
        }
        if (rawData.length < 9) {
            logger.error("Not enough data to instantiate the Message");
            return;
        }
        
        agency = rawData[0];
        esn = rawData[1];
        vName = rawData[2];
        try {
            if (!rawData[3].equals("null")) {
                acqTime = timeFormatter.parse(rawData[3]);
                acqTime.setTime(acqTime.getTime() - 15000);  // UTC time = GPS time - 15 seconds
                logger.info("recTime - acqTime = " + (recTime.getTime() - acqTime.getTime())/1000 + " seconds");
            }
            if (!rawData[4].equals("null"))
                lat = Float.parseFloat(rawData[4]);
            if (!rawData[5].equals("null"))
                lon = Float.parseFloat(rawData[5]);
            if (!rawData[6].equals("null"))
                course = Float.parseFloat(rawData[6]);
            if (!rawData[7].equals("null"))
                qual = Integer.parseInt(rawData[7]);
            if (qual != 0 && qual != 1)
                logger.error("Invalid qual value: " + qual + " encountered");
            if (!rawData[8].equals("null"))
                vel = Float.parseFloat(rawData[8]);
        }
        catch (Exception e) {
            e.printStackTrace();
            return;
        }
        
        populated = true;
    }
    
    /**
     * @return the agency
     */
    public String getAgency() {
        return agency;
    }

    /**
     * @return the esn
     */
    public String getEsn() {
        return esn;
    }

    /**
     * @return the vName
     */
    public String getvName() {
        return vName;
    }

    /**
     * @return the acqTime
     */
    public String getAcqTime() {
        return acqTime.toString();
    }
    
    /**
     * @return the recTime
     */
    public double getRecTime() {
        return recTime.getTime() / 1000;
    }

    /**
     * @return the acqTime in seconds
     */
    public double getObsTime() {
        return acqTime.getTime() / 1000;
    }

    /**
     * @return the lat
     */
    public double getLat() {
        return lat;
    }

    /**
     * @return the lon
     */
    public double getLon() {
        return lon;
    }

    /**
     * @return the course
     */
    public float getCourse() {
        return course;
    }

    /**
     * @return the qual
     */
    public float getQual() {
        return qual;
    }

    /**
     * @return the vel
     */
    public float getVel() {
        return vel;
    }
    
    /**
     * @return the populated
     */
    public boolean isPopulated() {
        return populated;
    }
    
    public abstract String getField(int index);

}
