package wde.ws;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import wde.dao.ObsTypeDao;
import wde.metadata.ObsType;

/**
 *
 * @author scot.lange
 */
@WebServlet(name = "ObsTypeServlet", urlPatterns =
{
  "/ObsType/list"
})
public class ObsTypeServlet extends HttpServlet
{

  private final Logger m_oLogger = Logger.getLogger(ObsTypeServlet.class);

  private final JsonFactory m_oJsonFactory = new JsonFactory();

  private final ObsTypeDao m_oObsTypeDao = ObsTypeDao.getInstance();

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

    if (sRequestUrl.endsWith("/list"))
      listAll(oResponse);
    else
    {
      oResponse.setStatus(HttpStatus.SC_BAD_REQUEST);
    }
  }

  private void listAll(HttpServletResponse response) throws IOException
  {

    try (JsonGenerator oJsonGenerator = m_oJsonFactory.createJsonGenerator(response.getOutputStream(), JsonEncoding.UTF8))
    {
      oJsonGenerator.writeStartArray();
      for (ObsType oObsType : m_oObsTypeDao.getObsTypeList())
      {
        if (!oObsType.isActive())
          continue;
        if (Integer.parseInt(oObsType.getId()) < 20) // is it an alert obstype
          continue;

        oJsonGenerator.writeStartObject();
        oJsonGenerator.writeStringField("id", oObsType.getId());
        oJsonGenerator.writeStringField("name", oObsType.getObsType());
        oJsonGenerator.writeStringField("englishUnits", oObsType.getObsEnglishUnit());
        oJsonGenerator.writeStringField("internalUnits", oObsType.getObsInternalUnit());
        oJsonGenerator.writeEndObject();
      }
      oJsonGenerator.writeEndArray();
    }
    catch (Exception oEx)
    {
      m_oLogger.error("", oEx);
    }
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
