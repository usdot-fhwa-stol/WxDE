package wde.security.filters;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Checks the Referer header in HTTP requests and validates it against
 * a configured regular expression. If the referer does not match the pattern,
 * then the request is rejected. A set of entry points can be defined to list
 * pages that will allow a referer value that does not pass validation.
 *
 * @author scot.lange
 */
public class RefererValidationFilter implements Filter
{

  private final Set<String> m_oEntryPoints = new HashSet<String>();

  private Pattern m_oAllowedRefererPattern;

  private boolean m_bAllowMissingReferer;

  private String m_sRejectRedirectUrl;

  /**
   * Entry points are URLs that will not be tested for the presence of a valid
   * nonce. They are used to provide a way to navigate back to a protected
   * application after navigating away from it. Entry points will be limited
   * to HTTP GET requests and should not trigger any security sensitive
   * actions.
   *
   * @param sEntryPoints
   *          Comma separated list of URLs to be configured as
   *          entry points.
   */
  public void setEntryPoints(String sEntryPoints)
  {
    String values[] = sEntryPoints.split(",");
    for(String sValue : values)
    {
      this.m_oEntryPoints.add(sValue.trim());
    }
  }

  public void setRefererPattern(String sReferers)
  {
    m_oAllowedRefererPattern = Pattern.compile(sReferers);
  }

  @Override
  public void init(FilterConfig oFilterConfig) throws ServletException
  {
    if(oFilterConfig.getInitParameter("entryPoints") != null)
      setEntryPoints(oFilterConfig.getInitParameter("entryPoints"));

    if(oFilterConfig.getInitParameter("allowMissingReferer") != null)
      m_bAllowMissingReferer = Boolean.valueOf(oFilterConfig.getInitParameter("allowMissingReferer"));

    setRefererPattern(oFilterConfig.getInitParameter("allowedReferersPattern"));

    if(oFilterConfig.getInitParameter("rejectRedirectUrl") != null)
      m_sRejectRedirectUrl = oFilterConfig.getInitParameter("rejectRedirectUrl");

    if(m_oAllowedRefererPattern == null)
      throw new ServletException("No allowed referers configured");
  }

  @Override
  public void doFilter(ServletRequest oRequest, ServletResponse oResponse, FilterChain oChain) throws IOException, ServletException
  {

    if(oRequest instanceof HttpServletRequest &&
        oResponse instanceof HttpServletResponse)
    {

      HttpServletRequest oHttpReq = (HttpServletRequest) oRequest;
      HttpServletResponse oHttpResp = (HttpServletResponse) oResponse;

      String sPath = oHttpReq.getServletPath();
      if(oHttpReq.getPathInfo() != null)
      {
        sPath = sPath + oHttpReq.getPathInfo();
      }

      if(!m_oEntryPoints.contains(sPath))
      {
        String sReferer = oHttpReq.getHeader("Referer");

        if(sReferer == null ? !m_bAllowMissingReferer : !m_oAllowedRefererPattern.matcher(sReferer).matches())
        {
          if(m_sRejectRedirectUrl != null)
            oHttpResp.sendRedirect(m_sRejectRedirectUrl);
          else
            oHttpResp.sendError(HttpServletResponse.SC_FORBIDDEN);

          return;
        }
      }
    }

    oChain.doFilter(oRequest, oResponse);
  }

  @Override
  public void destroy()
  {

  }

}
