// Copyright (c) 2010 Mixon/Hill, Inc. All rights reserved.
/**
 * @file QedsMgr.java
 */
package wde.qeds;

import wde.util.Config;
import wde.util.ConfigSvc;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.OutputStream;
import java.util.Random;

/**
 * Handles the security code for subscriptions. Generates, and draws the captcha
 * images across the http connection.
 *
 * @author bryan.krueger
 */
public class QedsMgr extends HttpServlet {
    /**
     * Configured number of sub directories.
     */
    private int m_nMaxDirs;
    /**
     * File path to the images used to dynamically build the captcha security
     * code.
     */
    private String m_sBaseDir;
    /**
     * Buffered captcha images.
     */
    private BufferedImage[] m_oImages = new BufferedImage[6];
    /**
     * Pseudo-random number generator.
     */
    private Random m_oRandom = new Random();

    /**
     * Pointer to the singleton instance of {@code Subscriptions}.
     * QedsMgr also starts the subscription fulfillment component.
     */
    private Subscriptions m_oSubs = Subscriptions.getInstance();
	 
	 /**
	  * Pointer to the singleton instance of FcstSubscriptions. QedsMgr also
	  * starts the forecast subscription fulfillment component.
	  */
	 private FcstSubscriptions m_oFcstSubs = FcstSubscriptions.getInstance();


    /**
     * Configures the new {@code QedsMgr} instance from the root configuration.
     */
    public QedsMgr() {
        Config oConfig = ConfigSvc.getInstance().getConfig(this);
        m_nMaxDirs = oConfig.getInt("numSubDirs", 0);
        m_sBaseDir = oConfig.getString("imageRoot", "./");

        // add a trailing slash if one was not set
        if (!m_sBaseDir.endsWith("/"))
            m_sBaseDir += "/";
    }


    /**
     * Randomly selects and draws 6 images from the configured directory
     * side-by-side to the provided output stream. This method also generates
     * the security code, based off the file selection.
     *
     * @param iOutputStream output stream, connected and ready to output data.
     * @return the generated security code.
     */
    private String getCode(OutputStream iOutputStream) {
        String sCode = "";

        try {
            // Pick 6 pictures randomly from the subdirectories.
            int nWidth = 0;
            for (int nIndex = 0; nIndex < m_oImages.length; nIndex++) {
                // Get a random directory number;
                int nSubDir = m_oRandom.nextInt(m_nMaxDirs);
                // Get a random file number;
                int nFileNum = m_oRandom.nextInt(10);

                // Build and store the randomly-generated filename.
                File oFile =
                        new File(m_sBaseDir + nSubDir + "/" + nFileNum + ".gif");
                m_oImages[nIndex] = ImageIO.read(oFile);

                nWidth += m_oImages[nIndex].getWidth();

                // Append the digit to the security code string.
                sCode += Integer.toString(nFileNum);
            }

            int nHeight = m_oImages[0].getHeight();

            BufferedImage oOutImage =
                    new BufferedImage(nWidth, nHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D oGfx = oOutImage.createGraphics();

            int nX = 0;
            for (int nIndex = 0; nIndex < m_oImages.length; nIndex++) {
                oGfx.drawImage(m_oImages[nIndex], nX, 0, null);
                nX += m_oImages[nIndex].getWidth();
            }

            ImageIO.write(oOutImage, "jpeg", iOutputStream);
        } catch (Exception oException) {
            oException.printStackTrace();
        }

        return sCode;
    }


    /**
     * Wraps {@link QedsMgr#doPost(HttpServletRequest, HttpServletResponse)}.
     *
     * @param oRequest  http request containing the subscription information.
     * @param oResponse http response, connected and ready to recieve data.
     */
    @Override
    public void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse) {
        doPost(oRequest, oResponse);
    }


    /**
     * Draws the captcha images to the supplied http response, and sets the
     * security code for the requesting subscription.
     *
     * @param oRequest  http request containing the subscription information.
     * @param oResponse http response, connected and ready to recieve data.
     */
    @Override
    public void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse) {
        try {
            oResponse.setContentType("image/jpeg");
            HttpSession oSession = oRequest.getSession(true);

            Subscription oSub = (Subscription) oSession.
                    getAttribute("oSubscription");

            // the object uses only one image buffer that must be synchronized
            synchronized (this) {
                oSub.m_sSecurityCode = getCode(oResponse.getOutputStream());
            }
        } catch (Exception oException) {
            oException.printStackTrace(System.out);
        }
    }
}
