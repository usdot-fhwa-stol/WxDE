package wde.ws;

import java.io.IOException;
import java.sql.Array;
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
import wde.dao.PlatformDao;
import wde.dao.SensorDao;
import wde.dao.Units;
import wde.metadata.IPlatform;
import wde.metadata.ISensor;
import wde.metadata.ObsType;
import wde.util.MathUtil;
import wde.util.QualityCheckFlagUtil;
import wde.util.QualityCheckFlags;

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

  protected final PlatformDao m_oPlatformDao = PlatformDao.getInstance();
  protected final ObsTypeDao m_oObsTypeDao = ObsTypeDao.getInstance();
  protected final Units m_oUnits = Units.getInstance();
  private final boolean m_bHasObs;
  private final int[] m_nZoomLevels;

  private final SimpleDateFormat m_oDateTableFormat;

  protected long m_lSearchRangeInterval = 20 * 60 * 1000;

  protected DataSource m_oDatasource;

  protected SensorDao m_oSensoDao = SensorDao.getInstance();

  protected boolean m_oQueryUsersMicros;

  protected final static String m_sBaseObsSelect
          = "SELECT\n"
          + "o.value,\n"
          + "o.obstime,\n"
          + "o.obstypeid,\n"
          + "o.qchcharflag,\n"
          + "o.confValue,\n"
          + "o.sourceid,\n"
          + "o.sensorid\n";

  protected final static String m_sBaseObsFrom
          = "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON p.id = s.platformid\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON s.id = o.sensorid\n";

  protected final static String m_sBaseObsWhere
          = "WHERE\n"
          + "p.id IN ( " + PLATFORM_LIST_PLACEHOLDER + ")\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n"
          + "AND o.latitude >= ?\n"
          + "AND o.latitude <= ?\n"
          + "AND o.longitude >= ?\n"
          + "AND o.longitude <= ?\n";

  protected final static String m_sBaseObsOrderBy = "ORDER BY obstypeid, obstime desc";

  protected final static String m_sStandardObsQueryTemplate = m_sBaseObsSelect + m_sBaseObsFrom + m_sBaseObsWhere + m_sBaseObsOrderBy;

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
    try
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
        if (!m_oObsRequestPattern.matcher(sRequestUri).find())
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

      processLayerRequest(oReq, oResp, oPlatformRequest);

    }
    catch (Exception ex)
    {
      m_oLogger.error("", ex);
      oResp.setStatus(500);
    }
  }

  protected void processObsRequest(HttpServletRequest oReq, HttpServletResponse oResp) throws Exception
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
      oOutputGenerator.writeStartObject();

      if (m_bHasObs)
      {

        try (Connection oConnection = m_oDatasource.getConnection())
        {
          try (PreparedStatement oPrepradeStmt = prepareObsStatement(oConnection, oObsRequest))
          {

            try (ResultSet oResult = oPrepradeStmt.executeQuery())
            {
              DecimalFormat oNumberFormatter = new DecimalFormat("0.##");
              DecimalFormat oConfFormat = new DecimalFormat("##0");
              SimpleDateFormat oDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
              oDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
              Date oDate = new Date();
              oOutputGenerator.writeArrayFieldStart("obs");

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
                int source = oResult.getInt("sourceid");
                float fConvfValue = oResult.getFloat("confValue");

                ISensor iSensor = m_oSensoDao.getSensor(oResult.getInt("sensorid"));

                if (iSensor == null)
                  continue;

                QualityCheckFlags oQchFlags = null;

                Array charArr = oResult.getArray("qchCharFlag");
                if (charArr != null)
                {
                  String[] strArray = (String[]) charArr.getArray();
                  //  char[] charArray = new char[16];
                  // Arrays.fill(charArray, '-');
                  char[] charArray = new char[strArray.length];
                  for (int i = 0; i < strArray.length; i++)
                    charArray[i] = strArray[i].charAt(0);

                  oQchFlags = QualityCheckFlagUtil.getFlags(source, charArray);
                }
                ObsType oObsType = m_oObsTypeDao.getObsType(nObsTypeId);

                oOutputGenerator.writeStartObject();

                oOutputGenerator.writeNumberField("si", iSensor.getSensorIndex());
                oOutputGenerator.writeStringField("ot", oObsType.getObsType());
                oDate.setTime(oObsTimestamp.getTime());
                oOutputGenerator.writeStringField("ts", oDateFormat.format(oDate));
                oOutputGenerator.writeStringField("mv", oNumberFormatter.format(dObsValue));
                oOutputGenerator.writeStringField("mu", oObsType.getObsInternalUnit());
                oOutputGenerator.writeStringField("ev", oNumberFormatter.format(m_oUnits.getConversion(oObsType.getObsInternalUnit(), oObsType.getObsEnglishUnit()).convert(dObsValue)));
                oOutputGenerator.writeStringField("eu", oObsType.getObsEnglishUnit());
                oOutputGenerator.writeStringField("cv", oConfFormat.format(fConvfValue));
                //lt/ln: lat/lng

                if (oQchFlags != null)
                {
                  oOutputGenerator.writeNumberField("rf", oQchFlags.getRunFlags());
                  oOutputGenerator.writeNumberField("pf", oQchFlags.getPassFlags());
                }
                else
                {
                  oOutputGenerator.writeNumberField("rf", 0);
                  oOutputGenerator.writeNumberField("pf", 0);
                }

                oOutputGenerator.writeEndObject();
              }

              oOutputGenerator.writeEndArray();
            }
          }
        }
      }

      IPlatform iPlatform = m_oPlatformDao.getPlatform(oObsRequest.getPlatformIds()[0]);

      DecimalFormat oElevationFormatter = new DecimalFormat("#,###");

      if (includeDescriptionInDetails())
      {
        oOutputGenerator.writeStringField("tnm", iPlatform.getDescription());
      }
      oOutputGenerator.writeStringField("tel", oElevationFormatter.format(iPlatform.getLocBaseElev()));

      oOutputGenerator.writeEndObject();
    }
  }

  protected boolean includeDescriptionInDetails()
  {
    return true;
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

  protected String getPlatformObsQueryTemplate()
  {
    return m_sStandardObsQueryTemplate;
  }

  protected PreparedStatement prepareObsStatement(Connection oConnection, ObsRequest oRequest) throws SQLException
  {
    long lStart = oRequest.getRequestTimestamp() - m_lSearchRangeInterval;
    long lEnd = oRequest.getRequestTimestamp();

    StringBuilder oInClauseBuilder = new StringBuilder(50);

    int nQCount = oRequest.getPlatformIds().length;
    while (--nQCount >= 0)
      oInClauseBuilder.append("?,");

    String sQuery = getPlatformObsQueryTemplate().replace(OBS_TABLE_PLACEHOLDER, getDateObsTableName(lStart)).replace(PLATFORM_LIST_PLACEHOLDER, oInClauseBuilder.substring(0, oInClauseBuilder.length() - 1));

    int nParameterCount = 0;
    PreparedStatement oStatement = oConnection.prepareStatement(sQuery);

    for (int platformId : oRequest.getPlatformIds())
      oStatement.setInt(++nParameterCount, platformId);

    oStatement.setTimestamp(++nParameterCount, new Timestamp(lStart));
    oStatement.setTimestamp(++nParameterCount, new Timestamp(lEnd));

    //this won't affect stationary platforms, but could affect mobile platforms
    oStatement.setInt(++nParameterCount, oRequest.getRequestBounds().getSouth());
    oStatement.setInt(++nParameterCount, oRequest.getRequestBounds().getNorth());
    oStatement.setInt(++nParameterCount, oRequest.getRequestBounds().getWest());
    oStatement.setInt(++nParameterCount, oRequest.getRequestBounds().getEast());
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
