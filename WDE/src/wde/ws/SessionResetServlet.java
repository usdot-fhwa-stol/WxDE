package wde.ws;

import java.io.IOException;
import java.util.Enumeration;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author scot.lange
 */
@WebServlet(name = "SessionResetServlet", urlPatterns =
{
  "/ResetSession/*"
})
public class SessionResetServlet extends HttpServlet
{

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
    HttpSession oSession = oRequest.getSession(false);
    if (oSession == null)
      return;

    Enumeration<String> oSessionAttributes = oSession.getAttributeNames();

    while (oSessionAttributes.hasMoreElements())
    {
      String sAttributeName = oSessionAttributes.nextElement();
      if (sAttributeName.endsWith("LastRequestBounds"))
        oSession.removeAttribute(sAttributeName);
    }

    oSession.setAttribute(RoadSegmentServlet.SHOW_ALL_ROADS, oRequest.getParameter(RoadSegmentServlet.SHOW_ALL_ROADS));
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
