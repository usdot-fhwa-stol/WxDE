package wde.qeds;

/**
 *
 * @author scot.lange
 */
public class NotificationCondition
{
  private boolean triggered;
  private ConditionFilter filter;
  private ConditionOperator operator;
  private double value;
  private double tolerance;
  private int obstypeId;
  private int index;

  /**
   * @return the filter
   */
  public ConditionFilter getFilter()
  {
    return filter;
  }

  /**
   * @param filter the filter to set
   */
  public void setFilter(ConditionFilter filter)
  {
    this.filter = filter;
  }

  /**
   * @return the operator
   */
  public ConditionOperator getOperator()
  {
    return operator;
  }

  /**
   * @param operator the operator to set
   */
  public void setOperator(ConditionOperator operator)
  {
    this.operator = operator;
  }

  /**
   * @return the value
   */
  public double getValue()
  {
    return value;
  }

  /**
   * @param value the value to set
   */
  public void setValue(double value)
  {
    this.value = value;
  }

  /**
   * @return the tolerance
   */
  public double getTolerance()
  {
    return tolerance;
  }

  /**
   * @param tolerance the tolerance to set
   */
  public void setTolerance(double tolerance)
  {
    this.tolerance = Math.abs(tolerance);
  }

  /**
   * @return the obstypeId
   */
  public int getObstypeId()
  {
    return obstypeId;
  }

  /**
   * @param obstypeId the obstypeId to set
   */
  public void setObstypeId(int obstypeId)
  {
    this.obstypeId = obstypeId;
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

  /**
   * @return the index
   */
  public int getIndex()
  {
    return index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(int index)
  {
    this.index = index;
  }


  @Override
  public String toString()
  {
    return "NotificationCondition{" + "triggered=" + triggered + ", filter=" + filter + ", operator=" + operator + ", value=" + value + ", tolerance=" + tolerance + ", obstypeId=" + obstypeId + ", index=" + index + '}';
  }

}
