/************************************************************************
 * Source filename: ApplicationInterfaceServlet.java
 * <p/>
 * Creation date: May 9, 2014
 * <p/>
 * Author: zhengg
 * <p/>
 * Project: WDE
 * <p/>
 * Objective:
 * <p/>
 * Developer's notes:
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 ***********************************************************************/

package wde.qeds;

import org.apache.log4j.Logger;
import wde.WDEMgr;
import wde.dao.DatabaseManager;
import wde.util.Config;
import wde.util.ConfigSvc;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

public class ApplicationInterfaceServlet extends HttpServlet {

    private static final Logger logger = Logger.getLogger(ApplicationInterfaceServlet.class);

    private DatabaseManager dm = null;
    private String connId = null;
    private String subDir = null;

    public ApplicationInterfaceServlet() {
        if (dm == null) {
            WDEMgr.getInstance();
            dm = DatabaseManager.getInstance();
            connId = dm.getConnection();

            Config config = ConfigSvc.getInstance().getConfig("wde.qeds.QedsMgr");
            subDir = config.getString("subscription", "./");
            if (!subDir.endsWith("/"))
                subDir += "/";
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String path = request.getServletPath();

        if (path.contains("downloadSubscription")) {
            String uuid = request.getParameter("uuid");
            String fileName = request.getParameter("file");

            if (uuid != null && fileName != null) {
                String ipAddress = request.getHeader("X-FORWARDED-FOR");
                if (ipAddress == null)
                    ipAddress = request.getRemoteAddr();

                logger.info("Statistics download subscription result requester IP address: " + ipAddress);
                retrieveSubscriptionResult(uuid, fileName, response);
            }
        }
    }

    public void retrieveSubscriptionResult(String uuid, String fileName, HttpServletResponse response) throws IOException {

        PrintWriter printWriter = response.getWriter();

        // lookup subId from UUID
        String queryStr = "SELECT id from subs.subscription where guid = ?";
        logger.info("Statistics download subscription result: " + queryStr);

		ResultSet rs = null;
        try (PreparedStatement ps = dm.prepareStatement(connId, queryStr))
		{
			ps.setString(1, uuid);
			rs = ps.executeQuery();
        }
		catch (SQLException ex) {
            printWriter.write("An error occurred attempting to retrieve subscriptions.");
            return;
        }

        String subId = null;
        try {
            printWriter = response.getWriter();

            if (rs.next())
                subId = String.valueOf(rs.getInt("id"));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                rs.close();
                rs = null;
            } catch (SQLException se) {
                // ignore
            }
        }

        if (subId == null) {
            printWriter.write("No subscription found for UUID " + uuid);
            return;
        }


		try (PreparedStatement oUpdateSubscription = dm.prepareStatement(connId, "UPDATE subs.subscription SET expires=? WHERE id=?"))
		{
			Date oDate = new java.util.Date();
			oDate.setTime(System.currentTimeMillis() + 1209600000L); // 2 weeks
			oUpdateSubscription.setTimestamp(1, new java.sql.Timestamp(oDate.getTime()));
			oUpdateSubscription.setInt(2, Integer.valueOf(subId));
			oUpdateSubscription.execute();
		}
		catch (SQLException oEx)
		{
			oEx.printStackTrace();
		}
		
        try {
            BufferedReader oReader = new BufferedReader(new FileReader(subDir + subId + "/" + fileName));

            String sLine;
            while ((sLine = oReader.readLine()) != null)
                printWriter.write(sLine + "\r\n");

            oReader.close();
        } catch (Exception e) {
            printWriter.write(fileName + "for UUID " + uuid + " does not exist.");
        }
    }
}
