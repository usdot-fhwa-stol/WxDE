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
import wde.util.MathUtil;

/**
 * Generates a summary of current vehicle locations and their data, optionally
 * filtering by contributor name.
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns =
{
  "/emdss/latest_vehicles/*"
})
public class EmdssVehicles extends EmdssServlet
{

  private static final Logger g_oLogger = Logger.getLogger(EmdssVehicles.class);

  private final String VEHICLES_QUERY;
  private final String VEHICLES_QUERY_CONTRIB;
  private final String VEHICLE_DATA_QUERY;

  private final HashMap<Integer, String> m_oVehicleObsFields = new HashMap<Integer, String>();
  private final HashMap<Integer, String> m_oVehicleObstypeUnits = new HashMap<Integer, String>();

  public EmdssVehicles() throws NamingException
  {
    //Set the obstypes to query, and their labels
    m_oVehicleObsFields.put(2000005, "heading_deg");
    m_oVehicleObsFields.put(51138, "road_temp_f");
    m_oVehicleObsFields.put(2000008, "speed_mph"); // will need to convert from m/s
    m_oVehicleObsFields.put(5733, "temp_f");

    //set the units for theoutput
    m_oVehicleObstypeUnits.put(2000008, "mph");
    m_oVehicleObsFields.put(51138, "F");

    //Build a list of the obstypes being used for the query
    StringBuilder oObstypeListBuilder = new StringBuilder();
    for (Integer nObstype : m_oVehicleObsFields.keySet())
      oObstypeListBuilder.append(",").append(nObstype);

    String sObstypeList = oObstypeListBuilder.substring(1);//leave out the leading ','
    VEHICLE_DATA_QUERY
            = "SELECT o.obstime, o.value, o.latitude, o.longitude, o.obstypeid\n\n"
            + "  FROM meta.platform p\n"
            + "  INNER JOIN meta.sensor s ON s.platformid = p.id\n"
            + "  INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
            + "WHERE\n"
            + "  p.category = 'M'\n"
            + "  AND o.obstime = ?\n"
            + "  AND p.id = ?\n"
            + "  AND o.obstypeid IN (" + sObstypeList + ")\n"
            + "ORDER BY o.obstime DESC, o.obstypeid";

    String sQueryBase
            = "SELECT MAX(o.obstime) AS latest_time, p.id, c.name\n"
            + "  FROM meta.platform p\n"
            + "  INNER JOIN meta.sensor s ON s.platformid = p.id\n"
            + "  INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
            + "  INNER JOIN meta.contrib c ON p.contribid = c.id\n"
            + "WHERE\n"
            + "  p.category = 'M'\n"
            + "  AND o.obstime > ?\n"
            + "  AND o.obstypeid IN (" + sObstypeList + ")\n";

    VEHICLES_QUERY
            = sQueryBase
            + "GROUP BY p.id,c.name\n"
            + "ORDER BY c.name";

    VEHICLES_QUERY_CONTRIB
            = sQueryBase
            + "  AND c.name = ?\n"
            + "GROUP BY p.id,c.name\n"
            + "ORDER BY c.name";
  }

  @Override
  protected void writeResponse(JsonGenerator oOutputGenerator, HttpServletRequest oRequest) throws IOException
  {
    SimpleDateFormat oDateFormat = new SimpleDateFormat();
    oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    oDateFormat.applyPattern(OBSTABLE_DATE_PATTERN);

    Timestamp oSearchTimeStart = new Timestamp(System.currentTimeMillis() - m_lSearchRangeInterval);

    Date oDate = new Date();

    String sObsTable = oDateFormat.format(oDate);

    oDateFormat.applyPattern(RESPONSE_DATE_PATTERN);
    oOutputGenerator.writeStartObject();
    oOutputGenerator.writeStringField("data_time", oDateFormat.format(oDate));

    ArrayList<Integer> oVehicleObstypesWritten = new ArrayList<Integer>();

    oOutputGenerator.writeArrayFieldStart("districts");

    String sState = getState(oRequest);
    String sQuery = (sState != null ? VEHICLES_QUERY_CONTRIB : VEHICLES_QUERY).replace(OBS_TABLE_PLACEHOLDER, sObsTable);
    try (Connection oCon = m_oDatasource.getConnection(); PreparedStatement oVehiclesStmt = oCon.prepareStatement(sQuery); PreparedStatement oVehicleDataStmt = oCon.prepareStatement(VEHICLE_DATA_QUERY.replace(OBS_TABLE_PLACEHOLDER, sObsTable)))
    {
      oVehiclesStmt.setTimestamp(1, oSearchTimeStart);
      if (sState != null)
        oVehiclesStmt.setString(2, sState);
      try (ResultSet oVehicleResult = oVehiclesStmt.executeQuery())
      {
        boolean bFirstVehicle = true;
        if (oVehicleResult.next())
        {
          String sLastDistrict = null;

          do
          {
            String sCurrentDistrict = oVehicleResult.getString("name");
            if (sCurrentDistrict == null)
              sCurrentDistrict = "";
            if (!sCurrentDistrict.equalsIgnoreCase(sLastDistrict))
            {
              if (!bFirstVehicle)
              {
                oOutputGenerator.writeEndArray();
                oOutputGenerator.writeEndObject();
              }
              bFirstVehicle = false;
              oOutputGenerator.writeStartObject();
              sLastDistrict = sCurrentDistrict;

              oOutputGenerator.writeStringField("display_name", sCurrentDistrict);
              oOutputGenerator.writeStringField("district_name", sCurrentDistrict);
              oOutputGenerator.writeNumberField("max_lat", 0d);
              oOutputGenerator.writeNumberField("max_lon", 0d);
              oOutputGenerator.writeNumberField("min_lat", 0d);
              oOutputGenerator.writeNumberField("min_lon", 0d);
              oOutputGenerator.writeArrayFieldStart("vehicles");

            }

            oOutputGenerator.writeStartObject();

            Timestamp oLatestVehicleTime = oVehicleResult.getTimestamp("latest_time");
            int nVehicleId = oVehicleResult.getInt("id");

            oDate.setTime(oLatestVehicleTime.getTime());
            oOutputGenerator.writeStringField("id", Integer.toString(nVehicleId));
            oOutputGenerator.writeStringField("obs_time", oDateFormat.format(oDate));

            oVehicleDataStmt.setTimestamp(1, oLatestVehicleTime);
            oVehicleDataStmt.setInt(2, nVehicleId);

            oVehicleObstypesWritten.clear();
            //loop through results and write out first value for each obstype.
            try (ResultSet oVehicleDataResult = oVehicleDataStmt.executeQuery())
            {
              if (oVehicleDataResult.next())
              {
                oOutputGenerator.writeStringField("lat", Double.toString(MathUtil.fromMicro(oVehicleDataResult.getInt("latitude"))));
                oOutputGenerator.writeStringField("lon", Double.toString(MathUtil.fromMicro(oVehicleDataResult.getInt("longitude"))));
                writeFirstObsValue(oVehicleDataResult, oVehicleObstypesWritten, m_oVehicleObsFields, m_oVehicleObstypeUnits, oOutputGenerator, true, false);
              }
            }
            oOutputGenerator.writeEndObject();
          }
          while (oVehicleResult.next());

          oOutputGenerator.writeEndArray();
          oOutputGenerator.writeEndObject();
        }
      }
    }
    catch (SQLException ex)
    {
      g_oLogger.error("SQL error", ex);
    }
    oOutputGenerator.writeEndArray();
    oOutputGenerator.writeEndObject();

    /**
     * http://www.ral.ucar.edu/projects/rdwx_mdss/proxy.php?path=/latest_vehicles/&state=minnesota
     *
     * {
     * "data_time": "201402201905",
     * "districts": [
     * {
     * "display_name": "minnesota",
     * "district_name": "minnesota",
     * "max_lat": 49.70000076293945,
     * "max_lon": -88.5,
     * "min_lat": 43.40000152587891,
     * "min_lon": -97.69999694824219,
     * "vehicles": [
     * {
     * "heading_deg": "-9999.0",
     * "id": "209554",
     * "lat": "48.1213",
     * "lon": "-96.1816",
     * "obs_time": "1392923390",
     * "road_temp_f": "-9999.0",
     * "speed_mph": "16",
     * "temp_f": "-9999.0"
     * }, {
     * "heading_deg": "-9999.0",
     * "id": "207554",
     * "lat": "45.0371",
     * "lon": "-93.0283",
     * "obs_time": "1392923395",
     * "road_temp_f": "-9999.0",
     * "speed_mph": "49",
     * "temp_f": "-9999.0"
     * }, *
     * ...
     *
     * {
     * "heading_deg": "-9999.0",
     * "id": "204550",
     * "lat": "44.7243",
     * "lon": "-92.8410",
     * "obs_time": "1392923385",
     * "road_temp_f": "-9999.0",
     * "speed_mph": "0",
     * "temp_f": "-9999.0"
     * }
     * ]
     * }
     * ]
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
