package wde.ws;

import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 *
 * @author scot.lange
 */
public class RwisLayerServlet extends PointLayerServletBase
{

  @Override
  public void init(ServletConfig config) throws ServletException
  {
    super.init(config);
    String sContribCondition = config.getInitParameter("contribCondition");


    String sBaseSelect = "SELECT\n"
          + "p.id\n"
          + ",p.category\n"
          + ",p.platformcode\n"
          + ",p.locbaselat AS latitude\n"
          + ",p.locbaselong AS longitude\n"
          + ",s.sensorindex\n";

    String sBaseFromWhere
          = "FROM\n"
          + "meta.platform p\n"
          + "INNER JOIN meta.sensor s ON p.id = s.platformid\n"
          + "INNER JOIN " + OBS_TABLE_PLACEHOLDER + " o ON s.id = o.sensorid\n"
          + "WHERE\n"
          + "p.category IN ('P', 'T')\n"
          + "AND p.locbaselat >= ?\n"
          + "AND p.locbaselat <= ?\n"
          + "AND p.locbaselong >= ?\n"
          + "AND p.locbaselong <= ?\n"
          + "AND s.distgroup IN (" + DISTGROUP_LIST_PLACEHOLDER + ")\n"
          + "AND o.obstime >= ?\n"
          + "AND o.obstime <= ?\n";

    if(sContribCondition != null)
      sBaseFromWhere +=  " AND p.contribid " + sContribCondition + "\n";

    m_sQueryWithoutObsTemplate
          = sBaseSelect
          + ",NULL AS value\n"
          + sBaseFromWhere
          + "GROUP BY p.id, s.sensorindex, p.platformcode, p.locbaselat, p.locbaselong, s.distgroup";

    m_sQueryWithObsTemplate
          = sBaseSelect
          + ",o.value\n"
          + sBaseFromWhere
          + "AND o.obstypeid = ?\n"
          + "ORDER BY platformid, s.sensorindex, obstime desc";
  }



  private  String m_sQueryWithoutObsTemplate;

  private  String m_sQueryWithObsTemplate;

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
