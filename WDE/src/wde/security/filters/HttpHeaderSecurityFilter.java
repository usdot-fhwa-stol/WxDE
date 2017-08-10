package wde.security.filters;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

/**
 * Provides a single configuration point for security measures that required the
 * addition of one or more HTTP headers to the response.
 * <br/>
 * <br/>
 * Based off of Apache's
 * filter by the same name that is provided in the Catalina jar, but it adds
 * XSS protection (which was added in a newer version of Tomcat than we are
 * currently using), and it adds the Content-Security-Policy header.
 */
public class HttpHeaderSecurityFilter implements Filter
{

  // HSTS
  private static final String HSTS_HEADER_NAME = "Strict-Transport-Security";
  private boolean m_bHstsEnabled = true;
  private int m_nHstsMaxAgeSeconds = 0;
  private boolean m_bHstsIncludeSubDomains = false;
  private String m_sHstsHeaderValue;

  // Click-jacking protection
  private static final String ANTI_CLICK_JACKING_HEADER_NAME = "X-Frame-Options";
  private boolean m_bAntiClickJackingEnabled = true;
  private XFrameOption m_oAntiClickJackingOption = XFrameOption.DENY;
  private URI m_oAntiClickJackingUri;
  private String m_sAntiClickJackingHeaderValue;

  // Content Security policy
  private static final String CONTENT_SECURITY_POLICY_HEADER_NAME = "Content-Security-Policy";
  private boolean m_bContentSecurityEnabled;
  private String m_bContentSecurityHeaderValue;

  // Block content sniffing
  private static final String BLOCK_CONTENT_TYPE_SNIFFING_HEADER_NAME = "X-Content-Type-Options";
  private static final String BLOCK_CONTENT_TYPE_SNIFFING_HEADER_VALUE = "nosniff";
  private boolean m_bBlockContentTypeSniffingEnabled = true;

  // Cross-site scripting filter protection
  private static final String XSS_PROTECTION_HEADER_NAME = "X-XSS-Protection";
  private static final String XSS_PROTECTION_HEADER_VALUE = "1; mode=block";
  private boolean m_bXssProtectionEnabled = true;

  @Override
  public void init(FilterConfig oFilterConfig) throws ServletException
  {

    if(oFilterConfig.getInitParameter("antiClickJackingEnabled") != null)
      setAntiClickJackingEnabled(Boolean.valueOf(oFilterConfig.getInitParameter("antiClickJackingEnabled")));

    if(oFilterConfig.getInitParameter("antiClickJackingHeaderValue") != null)
      setAntiClickJackingOption(oFilterConfig.getInitParameter("antiClickJackingHeaderValue"));

    if(oFilterConfig.getInitParameter("antiClickJackingHeaderValue") != null)
      setAntiClickJackingUri(oFilterConfig.getInitParameter("antiClickJackingHeaderValue"));

    if(oFilterConfig.getInitParameter("blockContentTypeSniffingEnabled") != null)
      setBlockContentTypeSniffingEnabled(Boolean.valueOf(oFilterConfig.getInitParameter("blockContentTypeSniffingEnabled")));

    if(oFilterConfig.getInitParameter("hstsEnabled") != null)
      setHstsEnabled(Boolean.valueOf(oFilterConfig.getInitParameter("hstsEnabled")));

    if(oFilterConfig.getInitParameter("hstsIncludeSubDomains") != null)
      setHstsIncludeSubDomains(Boolean.valueOf(oFilterConfig.getInitParameter("hstsIncludeSubDomains")));

    if(oFilterConfig.getInitParameter("hstsMaxAgeSeconds") != null)
      setHstsMaxAgeSeconds(Integer.parseInt(oFilterConfig.getInitParameter("hstsMaxAgeSeconds")));

    if(oFilterConfig.getInitParameter("xssProtectionEnabled") != null)
      setXssProtectionEnabled(Boolean.valueOf(oFilterConfig.getInitParameter("xssProtectionEnabled")));

    if(oFilterConfig.getInitParameter("contentSecurityEnabled") != null)
      setContentSecurityEnabled(Boolean.valueOf(oFilterConfig.getInitParameter("contentSecurityEnabled")));

    if(oFilterConfig.getInitParameter("contentSecurityHeaderValue") != null)
      setContentSecurityHeaderValue(oFilterConfig.getInitParameter("contentSecurityHeaderValue"));

    // Build HSTS header value
    StringBuilder sHstsValue = new StringBuilder("max-age=");
    sHstsValue.append(m_nHstsMaxAgeSeconds);
    if(m_bHstsIncludeSubDomains)
    {
      sHstsValue.append(";includeSubDomains");
    }
    m_sHstsHeaderValue = sHstsValue.toString();

    // Anti click-jacking
    StringBuilder oCjValue = new StringBuilder(m_oAntiClickJackingOption.m_sHeaderValue);
    if(m_oAntiClickJackingOption == XFrameOption.ALLOW_FROM)
    {
      oCjValue.append(' ');
      oCjValue.append(m_oAntiClickJackingUri);
    }
    m_sAntiClickJackingHeaderValue = oCjValue.toString();
  }

  @Override
  public void doFilter(ServletRequest oRequest, ServletResponse oResponse,
      FilterChain oChain) throws IOException, ServletException
  {

    if(oResponse instanceof HttpServletResponse)
    {
      HttpServletResponse oHttpResp = (HttpServletResponse) oResponse;

      if(oResponse.isCommitted())
      {
        throw new ServletException("httpHeaderSecurityFilter.committed");
      }

      // HSTS
      if(m_bHstsEnabled && oRequest.isSecure())
      {
        oHttpResp.setHeader(HSTS_HEADER_NAME, m_sHstsHeaderValue);
      }

      // anti click-jacking
      if(m_bAntiClickJackingEnabled)
      {
        oHttpResp.setHeader(ANTI_CLICK_JACKING_HEADER_NAME, m_sAntiClickJackingHeaderValue);
      }

      if(m_bContentSecurityEnabled)
        oHttpResp.setHeader(CONTENT_SECURITY_POLICY_HEADER_NAME, m_bContentSecurityHeaderValue);

      // Block content type sniffing
      if(m_bBlockContentTypeSniffingEnabled)
      {
        oHttpResp.setHeader(BLOCK_CONTENT_TYPE_SNIFFING_HEADER_NAME,
            BLOCK_CONTENT_TYPE_SNIFFING_HEADER_VALUE);
      }

      // cross-site scripting filter protection
      if(m_bXssProtectionEnabled)
      {
        oHttpResp.setHeader(XSS_PROTECTION_HEADER_NAME, XSS_PROTECTION_HEADER_VALUE);
      }
    }

    oChain.doFilter(oRequest, oResponse);
  }

  public boolean isHstsEnabled()
  {
    return m_bHstsEnabled;
  }

  public void setHstsEnabled(boolean bHstsEnabled)
  {
    this.m_bHstsEnabled = bHstsEnabled;
  }

  public int getHstsMaxAgeSeconds()
  {
    return m_nHstsMaxAgeSeconds;
  }

  public void setHstsMaxAgeSeconds(int nHstsMaxAgeSeconds)
  {
    if(nHstsMaxAgeSeconds < 0)
    {
      this.m_nHstsMaxAgeSeconds = 0;
    }
    else
    {
      this.m_nHstsMaxAgeSeconds = nHstsMaxAgeSeconds;
    }
  }

  public boolean isHstsIncludeSubDomains()
  {
    return m_bHstsIncludeSubDomains;
  }

  public void setHstsIncludeSubDomains(boolean bHstsIncludeSubDomains)
  {
    this.m_bHstsIncludeSubDomains = bHstsIncludeSubDomains;
  }

  public boolean isAntiClickJackingEnabled()
  {
    return m_bAntiClickJackingEnabled;
  }

  public void setAntiClickJackingEnabled(boolean bAntiClickJackingEnabled)
  {
    this.m_bAntiClickJackingEnabled = bAntiClickJackingEnabled;
  }

  public String getAntiClickJackingOption()
  {
    return m_oAntiClickJackingOption.toString();
  }

  public void setAntiClickJackingOption(String sAntiClickJackingOption)
  {
    for(XFrameOption option : XFrameOption.values())
    {
      if(option.getHeaderValue().equalsIgnoreCase(sAntiClickJackingOption))
      {
        this.m_oAntiClickJackingOption = option;
        return;
      }
    }
    throw new IllegalArgumentException("httpHeaderSecurityFilter.clickjack.invalid");
  }

  public String getAntiClickJackingUri()
  {
    return m_oAntiClickJackingUri.toString();
  }

  public boolean isBlockContentTypeSniffingEnabled()
  {
    return m_bBlockContentTypeSniffingEnabled;
  }

  public void setBlockContentTypeSniffingEnabled(
      boolean bBlockContentTypeSniffingEnabled)
  {
    this.m_bBlockContentTypeSniffingEnabled = bBlockContentTypeSniffingEnabled;
  }

  public void setAntiClickJackingUri(String sAntiClickJackingUri)
  {
    URI oUri;
    try
    {
      oUri = new URI(sAntiClickJackingUri);
    }
    catch(URISyntaxException oEx)
    {
      throw new IllegalArgumentException(oEx);
    }
    this.m_oAntiClickJackingUri = oUri;
  }

  public boolean isXssProtectionEnabled()
  {
    return m_bXssProtectionEnabled;
  }

  public void setXssProtectionEnabled(boolean bXssProtectionEnabled)
  {
    this.m_bXssProtectionEnabled = bXssProtectionEnabled;
  }

  @Override
  public void destroy()
  {

  }

  private static enum XFrameOption
  {
    DENY("DENY"),
    SAME_ORIGIN("SAMEORIGIN"),
    ALLOW_FROM("ALLOW-FROM");

    private final String m_sHeaderValue;

    private XFrameOption(String sHeaderValue)
    {
      this.m_sHeaderValue = sHeaderValue;
    }

    public String getHeaderValue()
    {
      return m_sHeaderValue;
    }
  }

  /**
   * @return the contentSecurityEnabled
   */
  public boolean isContentSecurityEnabled()
  {
    return m_bContentSecurityEnabled;
  }

  /**
   * @param bContentSecurityEnabled
   *          the contentSecurityEnabled to set
   */
  public void setContentSecurityEnabled(boolean bContentSecurityEnabled)
  {
    this.m_bContentSecurityEnabled = bContentSecurityEnabled;
  }

  /**
   * @return the contentSecurityHeaderValue
   */
  public String getContentSecurityHeaderValue()
  {
    return m_bContentSecurityHeaderValue;
  }

  /**
   * @param sContentSecurityHeaderValue
   *          the contentSecurityHeaderValue to set
   */
  public void setContentSecurityHeaderValue(String sContentSecurityHeaderValue)
  {
    this.m_bContentSecurityHeaderValue = sContentSecurityHeaderValue;
  }

}
