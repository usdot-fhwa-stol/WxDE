package wde.ws;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import wde.dao.ObsTypeDao;
import wde.dao.UnitConv;
import wde.dao.Units;
import wde.metadata.ObsType;

/**
 * Provides base functionality and data common to all Emdss servlets.
 *
 * @author scot.lange
 */
public abstract class EmdssServlet extends HttpServlet
{

  private static final Logger g_oLogger = Logger.getLogger(EmdssServlet.class);

  protected DataSource m_oDatasource;
  private final JsonFactory m_oJsonFactory = new JsonFactory();

  protected static final String RESPONSE_DATE_PATTERN = "yyyyMMddHHmm";

  protected static final String REQUEST_DATE_PATTERN = "yyyyMMdd.HHmm";

  protected static final String OBSTABLE_DATE_PATTERN = "'obs.\"obs_'yyyy-MM-dd'\"'";

  protected static final String OBS_TABLE_PLACEHOLDER = "[OBS_DATE_TABLE]";

  protected long m_lSearchRangeInterval = 60 * 60 * 1000;

  protected static final Units g_oUnits = Units.getInstance();

  protected static final ObsTypeDao g_oObstypes = ObsTypeDao.getInstance();

  public EmdssServlet() throws NamingException
  {
    this.m_oDatasource = (DataSource) ((Context) new InitialContext().lookup("java:comp/env")).lookup("jdbc/wxde");
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

    try (JsonGenerator oOutputGenerator = m_oJsonFactory.createJsonGenerator(oResponse.getOutputStream(), JsonEncoding.UTF8))
    {
      writeResponse(oOutputGenerator, oRequest);
    }
    catch (Exception ex)
    {
      g_oLogger.error("Error generating response", ex);
    }
  }

  protected abstract void writeResponse(JsonGenerator oOutputGenerator, HttpServletRequest oRequest) throws IOException;

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

  protected void writeFirstObsValue(ResultSet oDataResult, ArrayList<Integer> oObstypesWritten, HashMap<Integer, String> oObsFields, HashMap<Integer, String> oObstypeUnits, JsonGenerator oOutputGenerator, boolean bNextAlreadyCalled, boolean bWriteObsType) throws SQLException, IOException
  {
    boolean bHaveRecords;
    if (!bNextAlreadyCalled)
      bHaveRecords = oDataResult.next();
    else
      bHaveRecords = true;

    if (bHaveRecords)
    {
      do
      {
        int nObstypeId = oDataResult.getInt("obstypeid");

        String sObsField = oObsFields.get(nObstypeId);
        if (sObsField == null)
          continue;

        //if we hav already written a value for this obstype, skip this record
        int nObstypeInd = Collections.binarySearch(oObstypesWritten, nObstypeId);
        if (nObstypeInd >= 0)
          continue;

        oObstypesWritten.add(~nObstypeInd, nObstypeId);

        double dObsValue = oDataResult.getDouble("value");
        String sObsType = null;
        //convert the value if a unit was set for this obstype, or look up the obstype if we are going to write it out
        if (oObstypeUnits.containsKey(nObstypeId) || bWriteObsType)
        {
          sObsType = oObstypeUnits.get(nObstypeId);
          ObsType oObstype = g_oObstypes.getObsType(nObstypeId);
          if (sObsType != null) // if there was an obstype in the map, then do the conversion
          {
            UnitConv oUnitConv = g_oUnits.getConversion(oObstype.getObsInternalUnit(), sObsType);
            dObsValue = oUnitConv.convert(dObsValue);
          }
          else // if there is no obstype, then we are writing the obs type in the response and need to look it up
            sObsType = oObstype.getObsInternalUnit();
        }

        //write out the obs field
        String sValue = Double.toString(dObsValue);
        if (sObsType == null)
          oOutputGenerator.writeStringField(sObsField, sValue);
        else
          oOutputGenerator.writeStringField(sObsField, sValue + " " + sObsType);

        //Don't keep looping through obs records if we have found values for all obstypes,
        if (oObstypesWritten.size() == oObsFields.size())
          break;
      }
      while (oDataResult.next());

      //write out the 'no-data' value for anything that we didn't get a value for.
      for (Integer nObstypeId : oObsFields.keySet())
      {
        if (Collections.binarySearch(oObstypesWritten, nObstypeId) < 0)
          oOutputGenerator.writeStringField(oObsFields.get(nObstypeId), "-9999.0");
      }
    }
  }

  protected static String getState(HttpServletRequest oReq)
  {
    return oReq.getParameter("state");
  }

}
