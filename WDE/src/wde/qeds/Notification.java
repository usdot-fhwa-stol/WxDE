package wde.qeds;

import java.util.List;
import javax.xml.bind.annotation.XmlRootElement;
import wde.util.MathUtil;

/**
 *
 * @author scot.lange
 */
@XmlRootElement
public class Notification
{
  private String message;
  private List<NotificationCondition> conditions;
  private int lat1;
  private int lon1;
  private int lat2;
  private int lon2;

  private boolean usingMetric;
  private boolean triggered;

  private int id;
  private String username;

  /**
   * @return the message
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * @param message the message to set
   */
  public void setMessage(String message)
  {
    this.message = message;
  }

  /**
   * @return the conditions
   */
  public List<NotificationCondition> getConditions()
  {
    return conditions;
  }

  /**
   * @param conditions the conditions to set
   */
  public void setConditions(List<NotificationCondition> conditions)
  {
    this.conditions = conditions;
  }

  /**
   * @return the lat1
   */
  public double getLat1()
  {
    return MathUtil.fromMicro(lat1);
  }

  public int getLat1Micros()
  {
    return lat1;
  }

  /**
   * @param lat1 the lat1 to set
   */
  public void setLat1(double lat1)
  {
    this.lat1 = MathUtil.toMicro(lat1);
  }

  /**
   * @param lat1 the lat1 to set
   */
  public void setLat1(int lat1)
  {
    this.lat1 = lat1;
  }

  public void setLat1(String lat1)
  {
    setLat1(Double.parseDouble(lat1));
  }

  public void setLon1(String lon1)
  {
    setLon1(Double.parseDouble(lon1));
  }

  public void setLat2(String lat2)
  {
    setLat2(Double.parseDouble(lat2));
  }

  public void setLon2(String lon2)
  {
    setLon2(Double.parseDouble(lon2));
  }

  /**
   * @return the lon1
   */
  public double getLon1()
  {
    return MathUtil.fromMicro(lon1);
  }

  public int getLon1Micros()
  {
    return lon1;
  }

  /**
   * @param lon1 the lon1 to set
   */
  public void setLon1(double lon1)
  {
    this.lon1 = MathUtil.toMicro(lon1);
  }

  /**
   * @param lon1 the lon1 to set
   */
  public void setLon1(int lon1)
  {
    this.lon1 = lon1;
  }

  /**
   * @return the lat2
   */
  public double getLat2()
  {
    return MathUtil.fromMicro(lat2);
  }

  public int getLat2Micros()
  {
    return lat2;
  }

  /**
   * @param lat2 the lat2 to set
   */
  public void setLat2(double lat2)
  {
    this.lat2 = MathUtil.toMicro(lat2);
  }

  /**
   * @param lat2 the lat2 to set
   */
  public void setLat2(int lat2)
  {
    this.lat2 = lat2;
  }

  /**
   * @return the lon2
   */
  public double getLon2()
  {
    return MathUtil.fromMicro(lon2);
  }

  public int getLon2Micros()
  {
    return lon2;
  }

  /**
   * @param lon2 the lon2 to set
   */
  public void setLon2(double lon2)
  {
    this.lon2 = MathUtil.toMicro(lon2);
  }

  /**
   * @param lon2 the lon2 to set
   */
  public void setLon2(int lon2)
  {
    this.lon2 = lon2;
  }

  /**
   * @return the id
   */
  public int getId()
  {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(int id)
  {
    this.id = id;
  }

  /**
   * @return the username
   */
  public String getUsername()
  {
    return username;
  }

  /**
   * @param username the username to set
   */
  public void setUsername(String username)
  {
    this.username = username;
  }

  public boolean hasTriggeredCondition()
  {
    for(NotificationCondition condition : conditions)
    {
      if(condition.isTriggered())
        return true;
    }
      return false;
  }

  public boolean hasAllConditionslTriggered()
  {
    for(NotificationCondition condition : conditions)
    {
      if(!condition.isTriggered())
        return false;
    }
      return true;
  }

  /**
   * @return the usingMetric
   */
  public boolean isUsingMetric()
  {
    return usingMetric;
  }

  /**
   * @param usingMetric the usingMetric to set
   */
  public void setUsingMetric(boolean usingMetric)
  {
    this.usingMetric = usingMetric;
  }

  /**
   * @return the triggered
   */
  public boolean isTriggered()
  {
    return triggered;
  }

  /**
   * @param triggered the triggered to set
   */
  public void setTriggered(boolean triggered)
  {
    this.triggered = triggered;
  }

  @Override
  public String toString()
  {
    return "Notification{" + "id=" + id + ", triggered=" + triggered + ", message=" + message + ", conditions=" + (conditions == null ? "null" : conditions.size() ) + ", lat1=" + lat1 + ", lon1=" + lon1 + ", lat2=" + lat2 + ", lon2=" + lon2 + ", usingMetric=" + usingMetric + ", username=" + username + '}';
  }


}
