package wde.ws;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

/**
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns = "/MobileRwisLayer/*")
public class MobilePlatformServlet extends PointLayerServletBase
{

  private final String m_sBaseSelect
          = "SELECT\n"
          + "p.id\n"
          + ",p.category\n"
          + ",p.platformcode\n"
          + ",o.obstime\n"
          + ",o.latitude \n"
          + ",o.longitude \n"
          + ",s.sensorindex \n";

  private final String m_sBaseFromWhere
          = "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON s.platformid = p.id\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON o.sensorid = s.id\n"
          + "WHERE\n"
          + "p.contribid<>4\n"
          + "AND p.category IN ('M')\n"
          + "AND o.latitude >= ?\n"
          + "AND o.latitude <= ?\n"
          + "AND o.longitude >= ?\n"
          + "AND o.longitude <= ?\n"
          + "AND s.distgroup IN (" + DISTGROUP_LIST_PLACEHOLDER + ")\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n";

  private final String m_sQueryWithoutObsTemplate
          = m_sBaseSelect
          + ",null AS value\n"
          + m_sBaseFromWhere
          + "ORDER BY platformid, s.sensorindex, obstime desc";

  private final String m_sQueryWithObsTemplate
          = m_sBaseSelect
          + ",o.value\n"
          + m_sBaseFromWhere
          + "AND o.obstypeid = ?\n"
          + "ORDER BY platformid, s.sensorindex, obstime desc";

  public MobilePlatformServlet() throws NamingException
  {
    super(true, 7);
    m_oQueryUsersMicros = true;

  }

  @Override
  protected String getQueryWithObsType()
  {
    return m_sQueryWithObsTemplate;
  }

  @Override
  protected String getQueryWithoutObstype()
  {
    return m_sQueryWithoutObsTemplate;
  }
}
