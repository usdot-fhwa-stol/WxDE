package wde.ws;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

/**
 *
 * @author scot.lange
 */
@WebServlet(name = "StateStatusServlet", urlPatterns =
{
  "/states/status"
})
public class StateStatusServlet extends HttpServlet
{

  private final Logger m_oLogger = Logger.getLogger(StateStatusServlet.class);

  private final JsonFactory m_oJsonFactory = new JsonFactory();

  protected static final String OBS_TABLE_PLACEHOLDER = "[OBS_DATE_TABLE]";

  private final SimpleDateFormat m_oDateTableFormat;

  protected long m_lSearchRangeInterval = 60 * 60 * 1000;

  protected DataSource m_oDatasource;

  private static final String m_sStateStatusQuery
          = "SELECT distinct c.postalcode, COUNT(*)\n"
          + "FROM meta.contrib c \n"
          + "INNER JOIN meta.platform p\n"
          + "  ON p.contribid = c.id\n"
          + "INNER JOIN meta.sensor s \n"
          + "  ON p.id = s.platformid\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o\n"
          + "  ON o.sensorid = s.id\n"
          + "WHERE c.postalcode IS NOT NULL\n"
          + "  AND o.obstypeid = 5733\n"
          + "  AND o.obstime >= ?\n"
          + "  AND o.obstime <= ?\n"
          + "GROUP BY postalcode";

  /**
   * States with a data sharing agreement
   */
  private String[] m_sDataSharingStates =
  {
    "US.AK", "US.AZ", "US.CA", "US.CO", "US.DE", "US.FL", "US.GA", "US.IA", "US.ID", "US.IL", "US.IN", "US.KS", "US.KY", "US.MI", "US.MN", "US.MO", "US.MT", "US.ND", "US.NH", "US.NJ", "US.NV", "US.NY", "US.OR", "US.SD", "US.VT", "US.WI", "US.WY", "US.PN"
  };

  /**
   * States providing metadata
   */
  private  String[] m_sMetadataStates =
  {
    "US.AK", "US.AZ", "US.CA", "US.CO", "US.DE", "US.FL", "US.GA", "US.IA", "US.ID", "US.IL", "US.IN", "US.KS", "US.KY", "US.MA", "US.MD", "US.ME", "US.MI", "US.MN", "US.MO", "US.MT", "US.ND", "US.NE", "US.NH", "US.NH", "US.NM", "US.NV", "US.NY", "US.OH", "US.OK", "US.OR", "US.SC", "US.SD", "US.TN", "US.TX", "US.UT", "US.VA", "US.VT", "US.WA", "US.WI", "US.WV", "US.WY", "US.PN"
  };

  /**
   * Combined list of states with metadata and/or data sharing agreements
   */
  private String[] m_sAllStates;


  @Override
  public void init(ServletConfig config) throws ServletException
  {
    String metaStates = config.getServletContext().getInitParameter("metaDataStates");
    String dataSharingStates = config.getServletContext().getInitParameter("dataSharingStates");

    if(metaStates != null)
      m_sMetadataStates = metaStates.split("\\,");

    if(dataSharingStates != null)
      m_sDataSharingStates = dataSharingStates.split("\\,");

    Arrays.sort(m_sDataSharingStates);

    Arrays.sort(m_sMetadataStates);

    ArrayList<String> oAllStatesList = new ArrayList<String>();
    oAllStatesList.addAll(Arrays.asList(m_sDataSharingStates));
    for (String sMetadataState : m_sMetadataStates)
    {
      int nIndex = Collections.binarySearch(oAllStatesList, sMetadataState);
      if (nIndex < 0)
        oAllStatesList.add(~nIndex, sMetadataState);
    }
    m_sAllStates = new String[oAllStatesList.size()];
    oAllStatesList.toArray(m_sAllStates);
  }



  public StateStatusServlet() throws NamingException
  {
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
   * @param oRequest servlet request
   * @param oResponse servlet response
   * @throws ServletException if a servlet-specific error occurs
   * @throws IOException if an I/O error occurs
   */
  protected void processRequest(HttpServletRequest oRequest, HttpServletResponse oResponse)
          throws ServletException, IOException
  {

    String sRequestUrl = oRequest.getRequestURI();

    if (sRequestUrl.endsWith("/status"))
      listAll(oResponse);
    else
    {
      oResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
    }
  }

  private void listAll(HttpServletResponse response) throws IOException
  {

    try (Connection oCon = m_oDatasource.getConnection())
    {
      SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
      sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
      //long lNow = sdf.parse("2016-03-07 23:00").getTime();

      long lNow = System.currentTimeMillis();
      try (PreparedStatement oStmt = oCon.prepareStatement(m_sStateStatusQuery.replace(OBS_TABLE_PLACEHOLDER, getDateObsTableName(lNow))))
      {
        oStmt.setTimestamp(1, new Timestamp(lNow - m_lSearchRangeInterval));
        oStmt.setTimestamp(2, new Timestamp(lNow));

        try (ResultSet oResult = oStmt.executeQuery())
        {
          ArrayList<String> oActiveStateList = new ArrayList<String>();

          while (oResult.next())
            oActiveStateList.add(oResult.getString("postalcode"));

          Collections.sort(oActiveStateList);

          try (JsonGenerator oJsonGenerator = m_oJsonFactory.createJsonGenerator(response.getOutputStream(), JsonEncoding.UTF8))
          {
            oJsonGenerator.writeStartObject();

            for (String sState : m_sAllStates)
            {
              // 1 = data agreement with data available
              // 2 = data agreement with no data available
              // 3 = metadata only
              String sStatus;
              if (Arrays.binarySearch(m_sDataSharingStates, sState) >= 0)
                sStatus = Collections.binarySearch(oActiveStateList, sState) >= 0 ? "1" : "2";
              else
                sStatus = "3";

              oJsonGenerator.writeStringField(sState, sStatus);
            }

            oJsonGenerator.writeEndObject();
          }
        }
      }
    }
    catch (SQLException oEx)
    {
      m_oLogger.error("Error checking state status", oEx);

      try (JsonGenerator oJsonGenerator = m_oJsonFactory.createJsonGenerator(response.getOutputStream(), JsonEncoding.UTF8))
      {
        oJsonGenerator.writeStartObject();

        for (String sState : m_sAllStates)
        {
          // since we couldn't actually check the status, anything with an agreement will be status 2
          String sStatus = Arrays.binarySearch(m_sDataSharingStates, sState) >= 0 ? "2" : "3";

          oJsonGenerator.writeStringField(sState, sStatus);
        }

        oJsonGenerator.writeEndObject();
      }
    }
    catch (Exception oEx)
    {
      m_oLogger.error("", oEx);
      response.sendError(500);
    }
  }

  protected synchronized String getDateObsTableName(long lTimestamp)
  {

    return m_oDateTableFormat.format(new Date(lTimestamp));
  }

  // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
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

  /**
   * Returns a short description of the servlet.
   *
   * @return a String containing servlet description
   */
  @Override
  public String getServletInfo()
  {
    return "Short description";
  }// </editor-fold>

}
