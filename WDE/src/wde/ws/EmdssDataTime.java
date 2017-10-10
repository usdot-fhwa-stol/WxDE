package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerator;

/**
 * Generates a summery of the most recent obs times for mobile platforms and
 * road weather alerts.
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns =
{
  "/emdss/datatime/*"
})
public class EmdssDataTime extends EmdssServlet
{

  private static final Logger g_oLogger = Logger.getLogger(EmdssDataTime.class);

  private static final String LATEST_MOBILE_OBS
          = "SELECT max(obstime)\n"
          + "FROM meta.platform p\n"
          + "INNER JOIN meta.contrib c ON p.contribid = c.id\n"
          + "INNER JOIN meta.sensor s ON s.contribid = c.id\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
          + "WHERE\n"
          + "  p.category = 'M'\n";
  private static final String LATEST_MOBILE_OBS_CONTRIB
          = LATEST_MOBILE_OBS
          + "  AND c.name = ?";

  private static final String LATEST_ALERT_OBS
          = "SELECT max(obstime)\n"
          + "FROM " + OBS_TABLE_PLACEHOLDER + " o\n"
          + "INNER JOIN meta.sensor s ON o.sensorid = s.id\n"
          + "INNER JOIN meta.platform p ON s.platformid = p.id\n"
          + "INNER JOIN meta.contrib c ON p.contribid = c.id\n"
          + "WHERE\n"
          + "  o.obstypeid < 20\n";
  private static final String LATEST_ALERT_OBS_CONTRIB
          = LATEST_ALERT_OBS
          + "  AND c.name = ?";

  public EmdssDataTime() throws NamingException
  {
  }

  @Override
  protected void writeResponse(JsonGenerator oOutputGenerator, HttpServletRequest oRequest) throws IOException
  {
    /**
     * http://www.ral.ucar.edu/projects/rdwx_mdss/proxy.php?path=/datatime/&state=minnesota_vdt
     *
     * {
     * "latest_time": "201402201855",
     * "dir": "latest_vehicles"
     * }, {
     * "latest_time": "201402201848",
     * "dir": "rec_treatment"
     * }, {
     * "latest_time": "201402201830",
     * "dir": "road_wx_dir"
     * }, {
     * "latest_time": "201402201855",
     * "dir": "district_alerts"
     * }
     * ]
     *
     */

    String sState = getState(oRequest);

    HashMap<String, String> oDirQueryMap = new HashMap<>();

    oDirQueryMap.put("road_wx_dir", null);
    oDirQueryMap.put("rec_treatment", null);

    if (sState == null)
    {
      oDirQueryMap.put("district_alerts", LATEST_ALERT_OBS);
      oDirQueryMap.put("latest_vehicles", LATEST_MOBILE_OBS);
    }
    else
    {
      oDirQueryMap.put("district_alerts", LATEST_ALERT_OBS_CONTRIB);
      oDirQueryMap.put("latest_vehicles", LATEST_MOBILE_OBS_CONTRIB);
    }

    SimpleDateFormat oDateFormat = new SimpleDateFormat();
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    oDateFormat.applyPattern(OBSTABLE_DATE_PATTERN);

    Date oDate = new Date();

    String sObsTable = oDateFormat.format(oDate);

    oDateFormat.applyPattern(RESPONSE_DATE_PATTERN);

    oOutputGenerator.writeStartArray();
    for (Map.Entry<String, String> oDirEntry : oDirQueryMap.entrySet())
    {
      oOutputGenerator.writeStartObject();
      oOutputGenerator.writeStringField("dir", oDirEntry.getKey());

      oDate.setTime(0);
      String sDirQuery = oDirEntry.getValue();
      if (sDirQuery != null)
      {
        try (Connection oCon = m_oDatasource.getConnection())
        {
          try (PreparedStatement oStmt = oCon.prepareStatement(sDirQuery.replace(OBS_TABLE_PLACEHOLDER, sObsTable)))
          {
            if (sState != null)
              oStmt.setString(1, sState);
//            Timestamp oSearchTimeStart = new Timestamp(System.currentTimeMillis() - m_lSearchRangeInterval);
//            oStmt.setTimestamp(1, oSearchTimeStart);
            try (ResultSet oResult = oStmt.executeQuery())
            {
              if (oResult.next())
              {
                Timestamp oTime = oResult.getTimestamp(1);

                if (oTime != null)
                  oDate.setTime(oTime.getTime());
              }
            }
          }
        }
        catch (SQLException ex)
        {
          g_oLogger.error("Failed to execute query", ex);
        }
      }

      oOutputGenerator.writeStringField("latest_time", oDateFormat.format(oDate));
      oOutputGenerator.writeEndObject();
    }
    oOutputGenerator.writeEndArray();
  }

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "Short description";
  }

}
