package wde.ws;

import java.io.Serializable;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
public class LatLngBounds implements Serializable
{

  private LatLng m_oNorthWest;
  private LatLng m_oSouthEast;

  public LatLngBounds(int nLat1, int nLng1, int nLat2, int nLng2)
  {
    m_oNorthWest = new LatLng(Math.max(nLat1, nLat2), Math.min(nLng1, nLng2));
    m_oSouthEast = new LatLng(Math.min(nLat1, nLat2), Math.max(nLng1, nLng2));
  }

  public LatLngBounds(double dLat1, double dLng1, double dLat2, double dLng2)
  {
    this(MathUtil.toMicro(dLat1), MathUtil.toMicro(dLng1), MathUtil.toMicro(dLat2), MathUtil.toMicro(dLng2));
  }

  public LatLngBounds(LatLng oLatLng1, LatLng oLatLng2)
  {
    if (oLatLng1.getLat() >= oLatLng2.getLat() && oLatLng1.getLng() <= oLatLng2.getLng())
    {
      m_oNorthWest = oLatLng1;
      m_oSouthEast = oLatLng2;
    }
    else if (oLatLng2.getLat() >= oLatLng1.getLat() && oLatLng2.getLng() <= oLatLng1.getLng())
    {
      m_oNorthWest = oLatLng1;
      m_oSouthEast = oLatLng2;
    }
    else
    {
      m_oNorthWest = new LatLng(Math.max(oLatLng1.getLat(), oLatLng2.getLat()), Math.min(oLatLng1.getLng(), oLatLng1.getLng()));
      m_oSouthEast = new LatLng(Math.min(oLatLng1.getLat(), oLatLng2.getLat()), Math.max(oLatLng2.getLng(), oLatLng2.getLng()));
    }
  }

  public boolean contains(LatLng oPoint)
  {
    return oPoint.getLat() <= getNorthWest().getLat() && oPoint.getLat() >= getSouthEast().getLat()
            && oPoint.getLng() >= getNorthWest().getLng() && oPoint.getLng() <= getSouthEast().getLng();
  }

  public boolean containsOrIsEqual(LatLngBounds oBounds)
  {
    return oBounds.getSouthEast().getLat() >= this.getSouthEast().getLat() && oBounds.getSouthEast().getLng() <= this.getSouthEast().getLng() && oBounds.getNorthWest().getLat() <= this.getNorthWest().getLat() && oBounds.getNorthWest().getLng() >= this.getNorthWest().getLng();
  }

  public boolean intersects(LatLngBounds oBounds)
  {
    return oBounds.getNorth() >= this.getSouth() && oBounds.getSouth() <= this.getNorth() && oBounds.getWest() <= this.getEast() && oBounds.getEast() >= this.getWest();
  }

  public boolean intersects(double dLat, double dLng)
  {
    int nLat = MathUtil.toMicro(dLat);
    int nLng = MathUtil.toMicro(dLng);

    return nLat >= this.getSouth() && nLat <= this.getNorth() && nLng <= this.getEast() && nLng >= this.getWest();
  }

  public boolean intersects(int nLat, int nLng)
  {

    return nLat >= this.getSouth() && nLat <= this.getNorth() && nLng <= this.getEast() && nLng >= this.getWest();
  }

  public boolean intersects(int nLat1, int nLng1, int nLat2, int nLng2)
  {
    int nMinLat = Math.min(nLat1, nLat2);
    int nMaxLat = Math.max(nLat1, nLat2);
    int nMinLng = Math.min(nLng1, nLng2);
    int nMaxLng = Math.max(nLng1, nLng2);

    return nMaxLat >= this.getSouth() && nMinLat <= this.getNorth() && nMaxLng <= this.getEast() && nMinLng >= this.getWest();
  }

  public boolean intersects(LatLng oPoint)
  {
    return contains(oPoint);
  }

  /**
   * @return the northWest
   */
  public LatLng getNorthWest()
  {
    return m_oNorthWest;
  }

  /**
   * @return the southEast
   */
  public LatLng getSouthEast()
  {
    return m_oSouthEast;
  }

  public int getNorth()
  {
    return m_oNorthWest.getLat();
  }

  public int getSouth()
  {
    return m_oSouthEast.getLat();
  }

  public int getEast()
  {
    return m_oSouthEast.getLng();
  }

  public int getWest()
  {
    return m_oNorthWest.getLng();
  }
}
