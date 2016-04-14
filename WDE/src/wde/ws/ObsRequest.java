package wde.ws;

/**
 *
 * @author scot.lange
 */
public class ObsRequest
{

  private LatLngBounds m_sRequestBounds;
  private long m_lRequestTimestamp;
  private int[] m_nlatformIds;

  public LatLngBounds getRequestBounds()
  {
    return m_sRequestBounds;
  }

  public void setRequestBounds(LatLngBounds requestBounds)
  {
    this.m_sRequestBounds = requestBounds;
  }

  public long getRequestTimestamp()
  {
    return m_lRequestTimestamp;
  }

  public void setRequestTimestamp(long lRequestTimestamp)
  {
    this.m_lRequestTimestamp = lRequestTimestamp;
  }

  public int[] getPlatformIds()
  {
    return m_nlatformIds;
  }

  public void setPlatformIds(int... nPlatformIds)
  {
    this.m_nlatformIds = nPlatformIds;
  }
}
