package wde.ws;

import java.io.Serializable;

/**
 *
 * @author scot.lange
 */
public class PlatformRequest implements Serializable
{

  private LatLngBounds m_oRequestBounds;
  private int m_nRequestZoom;
  private long m_lRequestTimestamp;
  private int m_nRequestObsTypeId;

  public LatLngBounds getRequestBounds()
  {
    return m_oRequestBounds;
  }

  public void setRequestBounds(LatLngBounds oRequestBounds)
  {
    this.m_oRequestBounds = oRequestBounds;
  }

  public int getRequestZoom()
  {
    return m_nRequestZoom;
  }

  public void setRequestZoom(int requestZoom)
  {
    this.m_nRequestZoom = requestZoom;
  }

  public long getRequestTimestamp()
  {
    return m_lRequestTimestamp;
  }

  public void setRequestTimestamp(long lRequestTimestamp)
  {
    this.m_lRequestTimestamp = lRequestTimestamp;
  }

  public int getRequestObsType()
  {
    return m_nRequestObsTypeId;
  }

  public boolean hasObsType()
  {
    return m_nRequestObsTypeId > 0;
  }

  public void setRequestObsType(int nRequestObstypeId)
  {
    this.m_nRequestObsTypeId = nRequestObstypeId;
  }
}
