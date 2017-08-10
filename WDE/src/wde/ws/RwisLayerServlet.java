package wde.ws;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

/**
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns = "/RwisLayer/*")
public class RwisLayerServlet extends PointLayerServletBase
{

  private final String m_sBaseSelect = "SELECT\n"
          + "p.id\n"
          + ",p.category\n"
          + ",p.platformcode\n"
          + ",p.locbaselat AS latitude\n"
          + ",p.locbaselong AS longitude\n";

  private final String m_sBaseFromWhere
          = "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON p.id = s.platformid\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON s.id = o.sensorid\n"
          + "WHERE\n"
          + "p.contribid<>4\n"
          + "AND p.category IN ('P', 'T')\n"
          + "AND p.locbaselat >= ?\n"
          + "AND p.locbaselat <= ?\n"
          + "AND p.locbaselong >= ?\n"
          + "AND p.locbaselong <= ?\n"
          + "AND s.distgroup IN (" + DISTGROUP_LIST_PLACEHOLDER + ")\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n";

  private final String m_sQueryWithoutObsTemplate
          = m_sBaseSelect
          + ",NULL AS value\n"
          + m_sBaseFromWhere
          + "GROUP BY p.id, p.platformcode, p.locbaselat, p.locbaselong, s.distgroup";

  private final String m_sQueryWithObsTemplate
          = m_sBaseSelect
          + ",o.value\n"
          + m_sBaseFromWhere
          + "AND o.obstypeid = ?\n"
          + "ORDER BY platformid, obstime desc";

  public RwisLayerServlet() throws NamingException
  {
    super(true, 7);
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
