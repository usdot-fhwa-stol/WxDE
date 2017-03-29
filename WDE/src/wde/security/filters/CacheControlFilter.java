package wde.security.filters;

import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Sets the Cache-Control header to no-store, and the Pragma header to no-cache.
 */
public class CacheControlFilter implements Filter
{
  private static final String CACHE_CONTROL_HEADER_NAME = "Cache-Control";
  private static final String PRAGMA_HEADER_NAME = "Pragma";
  private boolean m_bDisableCache;

  @Override
  public void init(FilterConfig oFilterConfig) throws ServletException
  {

    if(oFilterConfig.getInitParameter("disableCache") != null)
      setDisableCache(Boolean.valueOf(oFilterConfig.getInitParameter("disableCache")));

  }

  @Override
  public void doFilter(ServletRequest oRequest, ServletResponse oResponse,
      FilterChain oChain) throws IOException, ServletException
  {

    if(oResponse instanceof HttpServletResponse)
    {
      HttpServletResponse oHttpResponse = (HttpServletResponse) oResponse;

      if(oResponse.isCommitted())
        throw new ServletException("httpHeaderSecurityFilter.committed");

      if(isDisableCache())
      {
        oHttpResponse.setHeader(PRAGMA_HEADER_NAME, "no-cache");
        oHttpResponse.setHeader(CACHE_CONTROL_HEADER_NAME, "no-store");
      }
    }
    oChain.doFilter(oRequest, oResponse);
  }

  /**
   * @return the disableCache
   */
  public boolean isDisableCache()
  {
    return m_bDisableCache;
  }

  /**
   * @param bDisableCache
   *          the disableCache to set
   */
  public void setDisableCache(boolean bDisableCache)
  {
    this.m_bDisableCache = bDisableCache;
  }

  @Override
  public void destroy()
  {
  }

}
