package wde.ws;

import javax.naming.NamingException;
import javax.servlet.annotation.WebServlet;

/**
 *
 * @author scot.lange
 */
@WebServlet(urlPatterns = "/MetaDataLayer/*")
public class RwisMetadataServlet extends PointLayerServletBase
{

  String m_sQueryTemplate
          = "SELECT\n"
          + "p.id\n"
          + ",p.category\n"
          + ",p.platformcode\n"
          + ",s.description\n"
          + ",p.locbaselat AS latitude\n"
          + ",p.locbaselong AS longitude\n"
          + "FROM\n"
          + "meta.platform p, meta.site s\n"
          + "WHERE p.totime IS NULL\n"
          + "AND p.category IN ('P', 'T')\n"
          + "AND p.contribid <> 4\n"
          + "AND p.siteId = s.id\n"
          + "AND p.locbaselat >= ?\n"
          + "AND p.locbaselat <= ?\n"
          + "AND p.locbaselong >= ?\n"
          + "AND p.locbaselong <= ?\n"
          + "ORDER BY p.id;";

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

}
