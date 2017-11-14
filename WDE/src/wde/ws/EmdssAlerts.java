package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerator;
import wde.data.osm.Road;
import wde.data.osm.Roads;
import wde.util.MathUtil;
import static wde.ws.EmdssServlet.OBSTABLE_DATE_PATTERN;

/**
 * Generates lists of current road weather alerts by contributor. Current alerts
 * can be returned for either all contributors or a single contributor specified
 * in the request
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns =
{
  "/emdss/district_alerts/*"
})
public class EmdssAlerts extends EmdssServlet
{

  private static final Logger g_oLogger = Logger.getLogger(EmdssAlerts.class);

  private static final String ALERTS_QUERY_BASE
          = "SELECT p.id, c.name, o.latitude, o.longitude, o.obstypeid, o.obstime, o.recvtime\n"
          + "  FROM meta.platform p\n"
          + "  INNER JOIN meta.sensor s ON s.platformid = p.id\n"
          + "  INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
          + "  INNER JOIN meta.contrib c ON p.contribid = c.id\n"
          + "WHERE\n"
          + "  o.obstime > ?\n"
          + "  AND o.obstypeid < 20\n";

  private static final String ALERTS_QUERY
          = ALERTS_QUERY_BASE
          + "ORDER BY c.name, latitude, longitude";

  private static final String ALERTS_QUERY_CONTRIB
          = ALERTS_QUERY_BASE
          + "  AND c.name = ?"
          + "ORDER BY c.name, latitude, longitude";

  private static final Roads g_oRoads = Roads.getInstance();
  private final int m_nSnapTolerance = 800;

  public EmdssAlerts() throws NamingException
  {
  }

  @Override
  protected void writeResponse(JsonGenerator oOutputGenerator, HttpServletRequest oRequest) throws IOException
  {
    String sState = oRequest.getParameter("state");

    SimpleDateFormat oDateFormat = new SimpleDateFormat();
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    oDateFormat.applyPattern(OBSTABLE_DATE_PATTERN);

    Date oDate = new Date();

    String sObsTable = oDateFormat.format(oDate);
    String sQuery = (sState == null ? ALERTS_QUERY : ALERTS_QUERY_CONTRIB).replace(OBS_TABLE_PLACEHOLDER, sObsTable);

    oDateFormat.applyPattern(RESPONSE_DATE_PATTERN);

    Long lMaxObsTime = Long.MIN_VALUE;

    oOutputGenerator.writeStartObject();
    oOutputGenerator.writeArrayFieldStart("districts");

    boolean bFirstDistrict = true;
    boolean bFirstAlert = true;

    boolean bHasObsAlert = false;
    boolean bHasForecastAlert = false;

    try (Connection oConnection = m_oDatasource.getConnection(); PreparedStatement oStmt = oConnection.prepareStatement(sQuery))
    {
      oStmt.setTimestamp(1, new Timestamp(oDate.getTime() - m_lSearchRangeInterval));
      if (sState != null)
        oStmt.setString(2, sState);

      try (ResultSet oResult = oStmt.executeQuery())
      {

        if (oResult.next())
        {
          String sLastDistrict = null;

          int nLastLat = -1;
          int nLastLng = -1;
          do
          {
            String sDistrict = oResult.getString("name");
            if (sDistrict == null)
              sDistrict = "";

            int nLat = oResult.getInt("latitude");
            int nLng = oResult.getInt("longitude");

            if (!sDistrict.equalsIgnoreCase(sLastDistrict))
            {
              sLastDistrict = sDistrict;
              if (!bFirstDistrict)
              {
                oOutputGenerator.writeEndArray(); // end last site timeseries
                oOutputGenerator.writeEndObject(); // end last site object
                oOutputGenerator.writeEndArray(); // end last district site list
                oOutputGenerator.writeEndObject(); // end last district object
              }
              bFirstDistrict = false;
              bFirstAlert = true;

              oOutputGenerator.writeStartObject();
              //write district values
              /**
               * "district_name": "Minnesota",
               * "max_lat": 49.70000076293945,
               * "max_lon": -88.5,
               * "min_lat": 43.40000152587891,
               * "min_lon": -97.69999694824219,
               */
              oOutputGenerator.writeStringField("name", sDistrict);

              oOutputGenerator.writeNumberField("max_lat", 0d);
              oOutputGenerator.writeNumberField("max_lon", 0d);
              oOutputGenerator.writeNumberField("min_lat", 0d);
              oOutputGenerator.writeNumberField("min_lon", 0d);

              oOutputGenerator.writeArrayFieldStart("sites");
            }

            Timestamp oAlertTime = oResult.getTimestamp("obstime");

            if (nLat != nLastLat || nLng != nLastLng)
            {
              Road oRoad = g_oRoads.getLink(m_nSnapTolerance, nLng, nLat);
              if (oRoad == null)
              {
                g_oLogger.error("No road found for alert at Lat,Lon: " + nLat + ", " + nLng);
                continue;
              }
              if (!bFirstAlert)
              {
                oOutputGenerator.writeEndArray();// end last timeseries
                oOutputGenerator.writeEndObject(); // end last site
              }
              bFirstAlert = false;

              oOutputGenerator.writeStartObject();
              //write site values
              oOutputGenerator.writeStringField("desc", oRoad.m_sName);

              //We aren't handling the case where there is both a forecast alert and a current alert for the same location
              String sObsAlert;
              String sForecastAlert;

              if (oAlertTime.getTime() > oResult.getTimestamp("recvtime").getTime())
              {
                sObsAlert = "clear";
                sForecastAlert = "alert";
              }
              else
              {
                sObsAlert = "alert";
                sForecastAlert = "clear";
              }

              oOutputGenerator.writeStringField("obs_alert_code", sObsAlert);
              oOutputGenerator.writeStringField("hr06_alert_code", sForecastAlert);

              oOutputGenerator.writeStringField("hr24_alert_code", "clear");
              oOutputGenerator.writeStringField("hr72_alert_code", "clear");
              oOutputGenerator.writeStringField("is_road_cond_site", "false");
              oOutputGenerator.writeStringField("is_rwis_site", "false");
              oOutputGenerator.writeStringField("is_wx_obs_site", "false");
              oOutputGenerator.writeNumberField("lat", MathUtil.fromMicro(nLat));
              oOutputGenerator.writeNumberField("lon", MathUtil.fromMicro(nLng));
              oOutputGenerator.writeStringField("site_id", Integer.toString(oRoad.m_nId));
              oOutputGenerator.writeStringField("site_num", null);
              /**
               * "desc": "MN ROAD SEGMENT Interstate 94 1",
               * "hr06_alert_code": "alert",
               * "hr24_alert_code": "clear",
               * "hr72_alert_code": "clear",
               * "is_road_cond_site": true,
               * "is_rwis_site": false,
               * "is_wx_obs_site": false,
               * "lat": 46.84318161010742,
               * "lon": -96.63314819335938,
               * "obs_alert_code": "clear",
               * "site_id": "M00001",
               * "site_num": 72753066,
               */
              oOutputGenerator.writeArrayFieldStart("time_series");
            }

            oOutputGenerator.writeStartObject();
            //write alert values
            /**
             *
             * "alert_code": "clear",
             * "chemical": "apply chem",
             * "pavement": "dry",
             * "plow": "plow",
             * "precip": "none",
             * "road_temp": 32.0,
             * "time": "201402201905",
             * "treatment_alert_code": "alert",
             * "visibility": "normal"
             */

            oOutputGenerator.writeStringField("alert_code", "alert");
            oOutputGenerator.writeStringField("chemical", "clear");
            oOutputGenerator.writeStringField("pavement", null);
            oOutputGenerator.writeStringField("plow", "none");
            oOutputGenerator.writeStringField("precip", null);
            oOutputGenerator.writeStringField("road_temp", null);

            oDate.setTime(oAlertTime.getTime());
            oOutputGenerator.writeStringField("time", oDateFormat.format(oDate));
            lMaxObsTime = Math.max(oAlertTime.getTime(), lMaxObsTime);

            oOutputGenerator.writeStringField("treatment_alert_code", "none");
            oOutputGenerator.writeStringField("visibility", "normal");

            oOutputGenerator.writeEndObject();
          }
          while (oResult.next());

          oOutputGenerator.writeEndArray(); // end last site timeseries
          oOutputGenerator.writeEndObject(); // end last site object
          oOutputGenerator.writeEndArray(); // end last district site list
          oOutputGenerator.writeEndObject(); // end last district object
        }
      }
    }
    catch (Exception ex)
    {
      g_oLogger.error("Unable to query for events", ex);
    }

    oOutputGenerator.writeEndArray(); // end district list

    oOutputGenerator.writeStringField("hr06_alert_summary_code", bHasForecastAlert ? "alert" : "clear");
    oOutputGenerator.writeStringField("hr24_alert_summary_code", "clear");
    oOutputGenerator.writeStringField("hr72_alert_summary_code", "clear");
    oOutputGenerator.writeStringField("obs_alert_summary_code", bHasObsAlert ? "alert" : "clear");

    if(lMaxObsTime != Long.MIN_VALUE)
    {
      oDate.setTime(lMaxObsTime);
      oOutputGenerator.writeStringField("data_time", oDateFormat.format(oDate));
    }

    oOutputGenerator.writeEndObject(); // end response object
    /**
     *
     * http://www.ral.ucar.edu/projects/rdwx_mdss/proxy.php?path=/district_alerts&state=minnesota_vdt
     *
     * JSON Response:
     *
     *
     * {
     * "data_time": "201402201900",
     * "districts": [
     * {
     * "district_name": "Minnesota",
     * "max_lat": 49.70000076293945,
     * "max_lon": -88.5,
     * "min_lat": 43.40000152587891,
     * "min_lon": -97.69999694824219,
     * "sites": [
     * {
     * "desc": "MN ROAD SEGMENT Interstate 94 1",
     * "hr06_alert_code": "alert",
     * "hr24_alert_code": "clear",
     * "hr72_alert_code": "clear",
     * "is_road_cond_site": true,
     * "is_rwis_site": false,
     * "is_wx_obs_site": false,
     * "lat": 46.84318161010742,
     * "lon": -96.63314819335938,
     * "obs_alert_code": "clear",
     * "site_id": "M00001",
     * "site_num": 72753066,
     * "time_series": [
     * {
     * "alert_code": "clear",
     * "chemical": "apply chem",
     * "pavement": "dry",
     * "plow": "plow",
     * "precip": "none",
     * "road_temp": 32.0,
     * "time": "201402201905",
     * "treatment_alert_code": "alert",
     * "visibility": "normal"
     * }, {
     * "alert_code": "alert",
     * "chemical": "none",
     * "pavement": "slick, icy",
     * "plow": "none",
     * "precip": "moderate snow",
     * "road_temp": 32.0,
     * "time": "201402202000",
     * "treatment_alert_code": "clear",
     * "visibility": "normal"
     * }, *
     * ...
     *
     * {
     * "alert_code": "clear",
     * "chemical": "none",
     * "pavement": "dry",
     * "plow": "none",
     * "precip": "none",
     * "road_temp": 24.0,
     * "time": "201402211800",
     * "treatment_alert_code": "clear",
     * "visibility": "normal"
     * }
     * ]
     * }, *
     * ...
     *
     * {
     * "desc": "Effie MN-1 Mile Post 194",
     * "hr06_alert_code": "alert",
     * "hr24_alert_code": "warning",
     * "hr72_alert_code": "warning",
     * "is_road_cond_site": false,
     * "is_rwis_site": true,
     * "is_wx_obs_site": false,
     * "lat": 47.84040069580078,
     * "lon": -93.48519897460938,
     * "obs_alert_code": "warning",
     * "site_id": "MN052",
     * "site_num": 72747030,
     * "time_series": [
     * {
     * "alert_code": "warning",
     * "chemical": "apply chem",
     * "pavement": "ice possible",
     * "plow": "plow",
     * "precip": "",
     * "road_temp": 38.0,
     * "time": "201402201905",
     * "treatment_alert_code": "alert",
     * "visibility": "normal"
     * }, {
     * "alert_code": "warning",
     * "chemical": "apply chem",
     * "pavement": "wet",
     * "plow": "plow",
     * "precip": "moderate snow",
     * "road_temp": 37.0,
     * "time": "201402202000",
     * "treatment_alert_code": "alert",
     * "visibility": "normal"
     * }, *
     * ...
     *
     * {
     * "alert_code": "warning",
     * "chemical": "apply chem",
     * "pavement": "slick, snowy",
     * "plow": "plow",
     * "precip": "moderate snow",
     * "road_temp": 20.0,
     * "time": "201402211800",
     * "treatment_alert_code": "alert",
     * "visibility": "normal"
     * }
     * ]
     * }
     * ]
     * }
     * ],
     * "hr06_alert_summary_code": "alert",
     * "hr24_alert_summary_code": "alert",
     * "hr72_alert_summary_code": "alert",
     * "obs_alert_summary_code": "alert"
     * }
     *
     */

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
