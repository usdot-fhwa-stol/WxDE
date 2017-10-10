package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonGenerator;
import wde.cs.ext.RTMA;
import wde.cs.ext.Radar;
import wde.dao.UnitConv;
import wde.metadata.ObsType;

/**
 * Generates a summary current observations for a platform for either the
 * current time or a time specified in the request.
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns =
{
  "/emdss/plots/*"
})
public class EmdssPlots extends EmdssServlet
{

  private static final Logger g_oLogger = Logger.getLogger(EmdssPlots.class);

  private final HashMap<Integer, String> m_oDatabaseObsFields = new HashMap<Integer, String>();
  private final HashMap<Integer, String> m_oReadingObsFields = new HashMap<Integer, String>();
  private final HashMap<Integer, String> m_oObstypeUnits = new HashMap<Integer, String>();

  private static final String SITE_DATA_QUERY
          = "SELECT o.value,  o.obstypeid, o.latitude, o.longitude, obstime\n\n"
          + "  FROM meta.platform p\n"
          + "  INNER JOIN meta.sensor s ON s.platformid = p.id\n"
          + "  INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
          + "WHERE\n"
          + "  p.category = 'P'\n"
          + "  AND o.obstime >= ? \n"
          + "  AND o.obstime <= ?\n"
          + "  AND p.platformcode = ?\n"
          + "  ANd p.totime IS NULL\n"
          + "ORDER BY o.obstime DESC, o.obstypeid";

  private static final RTMA g_oRtma = RTMA.getInstance();
  private static final Radar g_oRadar = Radar.getInstance();

  public EmdssPlots() throws NamingException
  {

    m_oDatabaseObsFields.put(5733, "nss_air_temp_mean"); //5733 "essAirTemperature"?
    m_oDatabaseObsFields.put(554, "nss_bar_press_mean"); //554 "essAtmosphericPressure"?

    //These wlil come from RTMA and Radar
    m_oReadingObsFields.put(2001010, "radar_cref"); // 2001010  Radar.getInstance().
    m_oReadingObsFields.put(2001002, "model_bar_press"); // "vdtAtmosphericPressure" RTMA.getInstance().getReading
    m_oReadingObsFields.put(2001001, "model_air_temp"); // 2001001, "vdtAirTemperature"  RTMA.getInstance().getReading

    m_oObstypeUnits.put(5733, "F");
    m_oObstypeUnits.put(2001001, "F");

    m_oObstypeUnits.put(2001002, "mb");
    m_oObstypeUnits.put(554, "mb");

    m_oObstypeUnits.put(2001010, "dBZ");
  }

  @Override
  protected void writeResponse(JsonGenerator oOutputGenerator, HttpServletRequest oRequest) throws IOException
  {

    String sSiteId = oRequest.getParameter("site");

    SimpleDateFormat oDateFormat = new SimpleDateFormat();
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    Timestamp oEndTime;
    String sTime = oRequest.getParameter("date_str");
    if (sTime != null)
    {
      oDateFormat.applyPattern(REQUEST_DATE_PATTERN);
      try
      {
        oEndTime = new Timestamp(oDateFormat.parse(sTime).getTime());
      }
      catch (Exception ex)
      {
        g_oLogger.error("Unable to parse request date: " + sTime, ex);
        return;
      }
    }
    else
    {
      oEndTime = new Timestamp(System.currentTimeMillis());
    }

    Timestamp oStartTime = new Timestamp(oEndTime.getTime() - m_lSearchRangeInterval);
    oDateFormat.applyPattern(OBSTABLE_DATE_PATTERN);
    Date oDate = new Date(oEndTime.getTime());
    String sObsTable = oDateFormat.format(oDate);

    ArrayList<Integer> oVehicleObstypesWritten = new ArrayList<Integer>();

    oOutputGenerator.writeStartObject();
    oOutputGenerator.writeStringField("summary_plot", null);
    oOutputGenerator.writeArrayFieldStart("road_segments");
    oOutputGenerator.writeStartObject();

    try (Connection oCon = m_oDatasource.getConnection(); PreparedStatement oStmt = oCon.prepareStatement(SITE_DATA_QUERY.replace(OBS_TABLE_PLACEHOLDER, sObsTable)))
    {
      oStmt.setTimestamp(1, oStartTime);
      oStmt.setTimestamp(2, oEndTime);
      oStmt.setString(3, sSiteId);

      try (ResultSet oVehicleDataResult = oStmt.executeQuery())
      {
        oVehicleDataResult.next();

        if (oVehicleDataResult.next())
        {
          int nLat = oVehicleDataResult.getInt("latitude");
          int nLon = oVehicleDataResult.getInt("longitude");

          writeReadingValue(oOutputGenerator, g_oRadar.getReading(2001010, oEndTime.getTime(), nLat, nLon), 2001010, m_oReadingObsFields, m_oObstypeUnits);
          writeReadingValue(oOutputGenerator, g_oRtma.getReading(2001002, oEndTime.getTime(), nLat, nLon), 2001002, m_oReadingObsFields, m_oObstypeUnits);
          writeReadingValue(oOutputGenerator, g_oRtma.getReading(2001001, oEndTime.getTime(), nLat, nLon), 2001001, m_oReadingObsFields, m_oObstypeUnits);

          oOutputGenerator.writeNumberField("time", oVehicleDataResult.getTimestamp("obstime").getTime() / 1000);

          writeFirstObsValue(oVehicleDataResult, oVehicleObstypesWritten, m_oDatabaseObsFields, m_oObstypeUnits, oOutputGenerator, true, true);
        }
      }
    }
    catch (SQLException ex)
    {
      g_oLogger.error("Error querying obs", ex);
    }

    oOutputGenerator.writeEndObject();
    oOutputGenerator.writeEndArray();
    oOutputGenerator.writeEndObject();

    /**
     *
     * URL:
     * http://www.ral.ucar.edu/projects/rdwx_mdss/proxy.php?path=/plots&date_str=20140220.1855&site=72655165&state=minnesota_vdt
     *
     * JSON Response:
     * {
     * "summary_plot": "iVBORw0KGgoAAAANSUhEUgAAArwAAAMggg==",
     * "road_segments": {
     * "nss_air_temp_mean": "33.68 deg F",
     * "radar_cref": "18.50 dBZ",
     * "nss_bar_press_mean": "-9999.00 mb",
     * "model_bar_press": "964.75 mb",
     * "model_air_temp": "33.03 deg F",
     * "time": 1392923100
     * }
     * }
     *
     */
  }

  private void writeReadingValue(JsonGenerator oOutputGenerator, double dObsValue, int nObstypeId, HashMap<Integer, String> oObsFields, HashMap<Integer, String> oObstypeUnits) throws IOException
  {
    String sObsType = oObstypeUnits.get(nObstypeId);

    ObsType oObstype = g_oObstypes.getObsType(nObstypeId);
    if (sObsType != null) // if there was an obstype in the map, then do the conversion
    {
      UnitConv oUnitConv = g_oUnits.getConversion(oObstype.getObsInternalUnit(), sObsType);
      dObsValue = oUnitConv.convert(dObsValue);
    }
    else // if there is no obstype to convert to, just look up the internal obstype to write out
      sObsType = oObstype.getObsInternalUnit();

    oOutputGenerator.writeStringField(oObsFields.get(nObstypeId), (Double.compare(dObsValue, Double.NaN) == 0 ? "-9999.0" : Double.toString(dObsValue)) + " " + sObsType);

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
