package wde.cs.ext.indiana;

import java.util.Map;

/**
 *
 * @author scot.lange
 */
public class FileSummary
{
  private final String m_sFileName;

  private final Map<String, Integer> m_oColumns;

  public FileSummary(String m_sFileName, Map<String, Integer> m_oColumns)
  {
    this.m_sFileName = m_sFileName;
    this.m_oColumns = m_oColumns;
  }

  /**
   * @return the m_sFileName
   */
  public String getFileName()
  {
    return m_sFileName;
  }

  /**
   * @return the m_oColumns
   */
  public Map<String, Integer> getColumns()
  {
    return m_oColumns;
  }

}
