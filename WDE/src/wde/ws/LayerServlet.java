package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.regex.Pattern;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import wde.dao.ObsTypeDao;
import wde.dao.Units;
import wde.metadata.ObsType;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
public abstract class LayerServlet extends HttpServlet
{

  private final Pattern m_oObsRequestPattern = Pattern.compile("^(?:[a-zA-Z0-9_-]*/){1,}platformObs/[-o0-9]*/[0-9]{1,30}(?:/-?[0-9]{1,3}\\.?[0-9]*){4,4}$");
  private final Pattern m_oLayerRequestPatter = Pattern.compile("^(?:[a-zA-Z0-9_-]*/){1,}[0-9]{1,30}/[0-9]{1,2}(?:/-?[0-9]{1,3}\\.?[0-9]*){4,4}/-?[0-9]{1,10}$");
  private static final Logger m_oLogger = Logger.getLogger(LayerServlet.class);

  private final JsonFactory m_oJsonFactory = new JsonFactory();

  private final String m_sPreviousRequestsSessionParam;
  protected static final String OBS_TABLE_PLACEHOLDER = "[OBS_DATE_TABLE]";
  protected static final String PLATFORM_LIST_PLACEHOLDER = "[PLATFORM_ID_LIST]";

  protected final ObsTypeDao m_oObsTypeDao = ObsTypeDao.getInstance();
  protected final Units m_oUnits = Units.getInstance();
  private final boolean m_bHasObs;
  private final int[] m_nZoomLevels;

  private final SimpleDateFormat m_oDateTableFormat;

  protected long m_lSearchRangeInterval = 20 * 60 * 1000;

  protected DataSource m_oDatasource;

  protected boolean m_oQueryUsersMicros;

  protected String m_sObsQueryTemplate
          = "SELECT\n"
          + "o.value,\n"
          + "o.obstime,\n"
          + "o.obstypeid\n"
          + "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON p.id = s.platformid\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON s.id = o.sensorid\n"
          + "WHERE\n"
          + "p.id IN ( " + PLATFORM_LIST_PLACEHOLDER + ")\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n"
          //          + "AND o.latitude >= ?\n"
          //          + "AND o.latitude <= ?\n"
          //          + "AND o.longitude >= ?\n"
          //          + "AND o.longitude <= ?\n"
          + "ORDER BY obstypeid, obstime desc";

  public LayerServlet(boolean bHasObs, int... nZoomLevels) throws NamingException
  {
    this.m_bHasObs = bHasObs;
    this.m_nZoomLevels = nZoomLevels;
    this.m_sPreviousRequestsSessionParam = this.getClass() + "." + this.hashCode() + ".LastRequestBounds";
    this.m_oDateTableFormat = new SimpleDateFormat("'obs.\"obs_'yyyy-MM-dd'\"'");
    m_oDateTableFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    InitialContext oInitCtx = new InitialContext();
    Context oCtx = (Context) oInitCtx.lookup("java:comp/env");

    m_oDatasource = (DataSource) oCtx.lookup("jdbc/wxde");
  }

  /**
   * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
   * methods.
   *
   * @param oReq servlet request
   * @param oResp servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest oReq, HttpServletResponse oResp)
          throws ServletException, IOException
  {
    String sRequestUri = oReq.getRequestURI();

    if (sRequestUri.endsWith("GetZoomLevels"))
    {
      try (JsonGenerator oOutputGenerator = m_oJsonFactory.createJsonGenerator(oResp.getOutputStream(), JsonEncoding.UTF8))
      {
        oOutputGenerator.writeStartArray();

        for (Integer nZoomLevel : m_nZoomLevels)
          oOutputGenerator.writeNumber(nZoomLevel);

        oOutputGenerator.writeEndArray();
      }
      return;
    }
    else if (sRequestUri.contains("platformObs"))
    {// Pattern.compile("^(?:[a-zA-Z0-9_-]*/){1,}platformObs/[0-9]*/[0-9]{1,30}$").matcher(requestUrl).matches()
      if (!m_bHasObs || !m_oObsRequestPattern.matcher(sRequestUri).find())
      {
        oResp.setStatus(HttpStatus.SC_BAD_REQUEST);
        return;
      }

      processObsRequest(oReq, oResp);
    }

    if (!m_oLayerRequestPatter.matcher(sRequestUri).find())
    {
      oResp.setStatus(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    String[] sUriParts = sRequestUri.split("/");

    if (sUriParts.length < 8)
    {
      oResp.setStatus(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    try
    {
      long lTimeStamp = Long.parseLong(sUriParts[sUriParts.length - 7]);
      int nZoom = Integer.parseInt(sUriParts[sUriParts.length - 6]);
      double dLat1 = Double.parseDouble(sUriParts[sUriParts.length - 5]);
      double dLng1 = Double.parseDouble(sUriParts[sUriParts.length - 4]);
      double dLat2 = Double.parseDouble(sUriParts[sUriParts.length - 3]);
      double dLng2 = Double.parseDouble(sUriParts[sUriParts.length - 2]);
      int nObsTypeId = Integer.parseInt(sUriParts[sUriParts.length - 1]);

      PlatformRequest oPlatformRequest = new PlatformRequest();
      oPlatformRequest.setRequestBounds(new LatLngBounds(dLat1, dLng1, dLat2, dLng2));
      oPlatformRequest.setRequestTimestamp(lTimeStamp);
      oPlatformRequest.setRequestZoom(nZoom);
      oPlatformRequest.setRequestObsType(nObsTypeId);

      try
      {
        processLayerRequest(oReq, oResp, oPlatformRequest);
      }
      catch (Exception oEx)
      {
        m_oLogger.error("", oEx);
      }
    }
    catch (NumberFormatException oEx)
    {
      m_oLogger.error("Unable to parse URL parts", oEx);
    }

  }

  protected void processObsRequest(HttpServletRequest oReq, HttpServletResponse oResp) throws IOException, ServletException
  {

    String[] sUriParts = oReq.getRequestURI().split("/");

    if (sUriParts.length < 7)
    {
      oResp.setStatus(HttpStatus.SC_BAD_REQUEST);
      return;
    }

    long lRequestTime = Long.parseLong(sUriParts[sUriParts.length - 5]);
    double dLat1 = Double.parseDouble(sUriParts[sUriParts.length - 4]);
    double dLng1 = Double.parseDouble(sUriParts[sUriParts.length - 3]);
    double dLat2 = Double.parseDouble(sUriParts[sUriParts.length - 2]);
    double dLng2 = Double.parseDouble(sUriParts[sUriParts.length - 1]);
    int nPlatformId = Integer.parseInt(sUriParts[sUriParts.length - 6]);
    ObsRequest oObsRequest = new ObsRequest();
    oObsRequest.setRequestBounds(new LatLngBounds(dLat1, dLng1, dLat2, dLng2));
    oObsRequest.setPlatformIds(nPlatformId);
    oObsRequest.setRequestTimestamp(lRequestTime);
    try (JsonGenerator oOutputGenerator = m_oJsonFactory.createJsonGenerator(oResp.getOutputStream(), JsonEncoding.UTF8))
    {

      try (Connection oConnection = m_oDatasource.getConnection())
      {
        try (PreparedStatement oPrepradeStmt = prepareObsStatement(oConnection, oObsRequest))
        {

          try (ResultSet oResult = oPrepradeStmt.executeQuery())
          {

            DecimalFormat oNumberFormatter = new DecimalFormat("0.##");
            SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
            Date oDate = new Date();
            oOutputGenerator.writeStartArray();

            ArrayList<Integer> oReturnedObsTypes = new ArrayList<Integer>();
            while (oResult.next())
            {
              int nObsTypeId = oResult.getInt("obstypeid");
              int nObsTypeIndex = Collections.binarySearch(oReturnedObsTypes, nObsTypeId);
              if (nObsTypeIndex >= 0)
                continue;
              else
                oReturnedObsTypes.add(~nObsTypeIndex, nObsTypeId);

              double dObsValue = oResult.getDouble("value");
              Timestamp oObsTimestamp = oResult.getTimestamp("obstime");

              ObsType oObsType = m_oObsTypeDao.getObsType(nObsTypeId);
              oOutputGenerator.writeStartArray();
              oOutputGenerator.writeString(oObsType.getObsType());
              oOutputGenerator.writeString(oNumberFormatter.format(dObsValue));
              oOutputGenerator.writeString(oObsType.getObsInternalUnit());
              oOutputGenerator.writeString(oNumberFormatter.format(m_oUnits.getConversion(oObsType.getObsInternalUnit(), oObsType.getObsEnglishUnit()).convert(dObsValue)));
              oOutputGenerator.writeString(oObsType.getObsEnglishUnit());
              oDate.setTime(oObsTimestamp.getTime());
              oOutputGenerator.writeString(oDateFormat.format(oDate));
              oOutputGenerator.writeEndArray();
            }
            oOutputGenerator.writeEndArray();
          }
        }
      }
      catch (Exception oEx)
      {
        m_oLogger.error("", oEx);
      }
    }
  }

  protected void processLayerRequest(HttpServletRequest oReq, HttpServletResponse oResp, PlatformRequest oPlatformRequest) throws IOException, ServletException, Exception
  {
    HttpSession oSession = oReq.getSession(true);

    //Get the previous requests for this servlet
    HashMap<Integer, PlatformRequest> oZoomLevelRequests = (HashMap<Integer, PlatformRequest>) oSession.getAttribute(m_sPreviousRequestsSessionParam);

    if (oZoomLevelRequests == null)
    {
      oZoomLevelRequests = new HashMap<>();
      oSession.setAttribute(m_sPreviousRequestsSessionParam, oZoomLevelRequests);
    }

    try (JsonGenerator oOutputGenerator = m_oJsonFactory.createJsonGenerator(oResp.getOutputStream(), JsonEncoding.UTF8))
    {
      oOutputGenerator.writeStartObject();

      Integer nHighestValidZoomIndex = 0;
      int nZoomIndex = m_nZoomLevels.length;
      while (--nZoomIndex >= 0)
      {
        Integer nZoomLevel = m_nZoomLevels[nZoomIndex];

        PlatformRequest oLastRequest = oZoomLevelRequests.get(nZoomLevel);

        //if obs affect whether or not layer elements ar shown, then do a full
        //refresh when obstype or time are changed
        if (hasObs() && oLastRequest != null && (oLastRequest.getRequestObsType() != oPlatformRequest.getRequestObsType() || oLastRequest.getRequestTimestamp() != oPlatformRequest.getRequestTimestamp()))
        {
          oLastRequest = null;
          oZoomLevelRequests.remove(nZoomLevel);
        }

        if (nZoomLevel <= oPlatformRequest.getRequestZoom())
          nHighestValidZoomIndex = Math.max(nZoomLevel, 0);
        else
        {
          //Only keep one zoom level higher than the highest one actually being displayed
          if (nZoomIndex - nHighestValidZoomIndex > 1)
            oZoomLevelRequests.remove(nZoomLevel);
          continue;
        }

        LatLngBounds oLastBounds = oLastRequest != null ? oLastRequest.getRequestBounds() : null;

        //Does the last boundary for this zoom level contain the current
        //request boundary (most likely when zooming in, but could also be due to a resize)
        if (oLastBounds != null && oLastBounds.containsOrIsEqual(oPlatformRequest.getRequestBounds()))
          continue;

        oOutputGenerator.writeArrayFieldStart(nZoomLevel.toString());

        try (Connection oConnection = m_oDatasource.getConnection())
        {
          try (PreparedStatement oStatement = prepareLayerStatement(oConnection, nZoomLevel, oPlatformRequest))
          {
            try (ResultSet oResult = oStatement.executeQuery())
            {
              serializeResult(oOutputGenerator, oLastBounds, oPlatformRequest, oResult);
            }
          }
        }

        oOutputGenerator.writeEndArray();

        oZoomLevelRequests.put(nZoomLevel, oPlatformRequest);
      }
      oOutputGenerator.writeEndObject();
    }
  }

  protected abstract String getQueryWithObsType();

  protected abstract String getQueryWithoutObstype();

  protected PreparedStatement prepareLayerStatement(Connection oConnection, int nZoomLevel, PlatformRequest oPlatformRequest) throws SQLException
  {
    long lStart = oPlatformRequest.getRequestTimestamp() - m_lSearchRangeInterval;
    long lEnd = oPlatformRequest.getRequestTimestamp();

    String sQuery = (oPlatformRequest.hasObsType() ? getQueryWithObsType() : getQueryWithoutObstype()).replace(OBS_TABLE_PLACEHOLDER, getDateObsTableName(lStart));

    int nParameterIndex = 0;
    PreparedStatement oStatement = oConnection.prepareStatement(sQuery);

    if (m_oQueryUsersMicros)
    {
      oStatement.setInt(++nParameterIndex, oPlatformRequest.getRequestBounds().getSouth());
      oStatement.setInt(++nParameterIndex, oPlatformRequest.getRequestBounds().getNorth());
      oStatement.setInt(++nParameterIndex, oPlatformRequest.getRequestBounds().getWest());
      oStatement.setInt(++nParameterIndex, oPlatformRequest.getRequestBounds().getEast());
    }
    else
    {
      oStatement.setDouble(++nParameterIndex, MathUtil.fromMicro(oPlatformRequest.getRequestBounds().getSouth()));
      oStatement.setDouble(++nParameterIndex, MathUtil.fromMicro(oPlatformRequest.getRequestBounds().getNorth()));
      oStatement.setDouble(++nParameterIndex, MathUtil.fromMicro(oPlatformRequest.getRequestBounds().getWest()));
      oStatement.setDouble(++nParameterIndex, MathUtil.fromMicro(oPlatformRequest.getRequestBounds().getEast()));
    }

    if (hasObs())
    {
      oStatement.setTimestamp(++nParameterIndex, new Timestamp(lStart));
      oStatement.setTimestamp(++nParameterIndex, new Timestamp(lEnd));

      if (oPlatformRequest.hasObsType())
        oStatement.setInt(++nParameterIndex, oPlatformRequest.getRequestObsType());
    }
    return oStatement;
  }

  protected PreparedStatement prepareObsStatement(Connection oConnection, ObsRequest oRequest) throws SQLException
  {
    long lStart = oRequest.getRequestTimestamp() - m_lSearchRangeInterval;
    long lEnd = oRequest.getRequestTimestamp();

    StringBuilder oInClauseBuilder = new StringBuilder(50);

    int nQCount = oRequest.getPlatformIds().length;
    while (--nQCount >= 0)
      oInClauseBuilder.append("?,");

    String sQuery = m_sObsQueryTemplate.replace(OBS_TABLE_PLACEHOLDER, getDateObsTableName(lStart)).replace(PLATFORM_LIST_PLACEHOLDER, oInClauseBuilder.substring(0, oInClauseBuilder.length() - 1));

    int nParameterCount = 0;
    PreparedStatement oStatement = oConnection.prepareStatement(sQuery);

    for (int platformId : oRequest.getPlatformIds())
      oStatement.setInt(++nParameterCount, platformId);

    oStatement.setTimestamp(++nParameterCount, new Timestamp(lStart));
    oStatement.setTimestamp(++nParameterCount, new Timestamp(lEnd));
//    stmt.setInt(++parameterCount, request.getRequestBounds().getSouth());
//    stmt.setInt(++parameterCount, request.getRequestBounds().getNorth());
//    stmt.setInt(++parameterCount, request.getRequestBounds().getWest());
//    stmt.setInt(++parameterCount, request.getRequestBounds().getEast());
    return oStatement;
  }

  protected abstract void serializeResult(JsonGenerator oJsonGenerator, LatLngBounds oLastRequestBounds, PlatformRequest oCurrentRequest, ResultSet oResult) throws SQLException, IOException;

  protected boolean hasObs()
  {
    return m_bHasObs;
  }

  /**
   * Handles the HTTP <code>GET</code> method.
   *
   * @param oRequest servlet request
   * @param oResponse servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doGet(HttpServletRequest oRequest, HttpServletResponse oResponse)
          throws ServletException, IOException
  {
    processRequest(oRequest, oResponse);
  }

  /**
   * Handles the HTTP <code>POST</code> method.
   *
   * @param oRequest servlet request
   * @param oResponse servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  @Override
  protected void doPost(HttpServletRequest oRequest, HttpServletResponse oResponse)
          throws ServletException, IOException
  {
    processRequest(oRequest, oResponse);
  }

  protected synchronized String getDateObsTableName(long lTimestamp)
  {

    return m_oDateTableFormat.format(new Date(lTimestamp));
  }

}
