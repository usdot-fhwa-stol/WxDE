/************************************************************************
 * Source filename: AmeritrakMessage.java
 * <p/>
 * Creation date: Oct 4, 2013
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: VDT Integration
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.vdt.probe.raw.ameritrak;

import org.apache.log4j.Logger;
import wde.util.DateTimeHelper;

import java.util.Date;

public abstract class AmeritrakMessage {

    protected static final int OFFSET = 1;
    private static final Logger logger = Logger.getLogger(AmeritrakMessage.class);
    private String agency = null;

    private String esn = null;

    private String vName = null;

    private Date acqTime = null;

    private Date recTime = null;

    private double lat;

    private double lon;

    private float course;

    private int qual;

    private float vel;

    private boolean populated;

    /**
     * @param rawData
     */
    public AmeritrakMessage(String recvTime, String[] rawData) {

        populated = false;

        recTime = new Date();
        recTime.setTime(Long.parseLong(recvTime));

        if (rawData == null) {
            logger.error("Input array is null");
            return;
        }
        if (rawData.length < 9) {
            logger.error("Not enough data to instantiate the AmeritrakMessage");
            return;
        }

        agency = rawData[0];
        esn = rawData[1];
        vName = rawData[2];
        try {
            if (!rawData[3].equals("null")) {
                acqTime = DateTimeHelper.getTimeFormatter1().parse(rawData[3]);
                acqTime.setTime(acqTime.getTime() - 15000);  // UTC time = GPS time - 15 seconds
                logger.debug("recTime - acqTime = " + (recTime.getTime() - acqTime.getTime()) / 1000 + " seconds");
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
                vel = Float.parseFloat(rawData[8]) * 0.44704f; // converting from mph to m/s
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
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
