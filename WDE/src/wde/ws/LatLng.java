package wde.ws;

import java.io.Serializable;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
public class LatLng implements Serializable
{

  private int m_nLat;
  private int m_nLng;

  public LatLng()
  {
  }

  public LatLng(int nLat, int nlng)
  {
    this.m_nLat = nLat;
    this.m_nLng = nlng;
  }

  public LatLng(double nLat, double nLng)
  {
    this(MathUtil.toMicro(nLat), MathUtil.toMicro(nLng));
  }

  public int getLat()
  {
    return m_nLat;
  }

  public void setLat(int nLat)
  {
    this.m_nLat = nLat;
  }

  public int getLng()
  {
    return m_nLng;
  }

  public void setLng(int nLng)
  {
    this.m_nLng = nLng;
  }

}
