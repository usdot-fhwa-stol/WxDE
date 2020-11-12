package wde.ws;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

/**
 *
 * @author scot.lange
 */
public class RwisMetadataServlet extends PointLayerServletBase
{

  @Override
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    String sContribCondition = config.getInitParameter("contribCondition");


    m_sQueryTemplate = "SELECT\n"
        + "p.id\n"
        + ",p.category\n"
        + ",p.platformcode\n"
        + ",p.locbaselat AS latitude\n"
        + ",p.locbaselong AS longitude\n"
        + "FROM\n"
        + "meta.platform p\n"
        + "INNER JOIN meta.sensor s ON p.id = s.platformid\n"
        + "WHERE p.totime IS NULL\n"
        + "AND p.category IN ('P', 'T')\n"
        + "AND p.locbaselat >= ?\n"
        + "AND p.locbaselat <= ?\n"
        + "AND p.locbaselong >= ?\n"
        + "AND p.locbaselong <= ?\n"
        + "AND s.distgroup IN (" + DISTGROUP_LIST_PLACEHOLDER + ")\n";

    if(sContribCondition != null)
      m_sQueryTemplate += " AND p.contribid " + sContribCondition + " \n";

    m_sQueryTemplate += "ORDER BY p.id;";
  }

  String m_sQueryTemplate;

  public RwisMetadataServlet() throws NamingException
  {
    super(false, 7);
  }

  @Override
  protected String getQueryWithObsType()
  {
    return null;
  }

  @Override
  protected String getQueryWithoutObstype()
  {
    return m_sQueryTemplate;
  }

  @Override
  protected boolean includeSensorsInDetails()
  {
    return true;
  }

}
