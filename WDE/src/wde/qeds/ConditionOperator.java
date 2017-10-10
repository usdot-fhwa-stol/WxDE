package wde.qeds;

import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author scot.lange
 */
public enum ConditionOperator
{
  Gt(1), Lt(-1), GtEq(1,0), LtEq(-1,0);

  private final Set<Integer> matchingComparatorValues = new HashSet<>(3);


  private ConditionOperator(int... matchingComparatorValues)
  {
    for(int value : matchingComparatorValues)
      this.matchingComparatorValues.add(value);
  }

  /**
   * Compares the two specified {@code double} values using {@link Double#compare(double, double) }.
   * @param   d1        the first {@code double} to compare
   * @param   d2        the second {@code double} to compare
   * @return {@code true} if the comparison result matches the operator
   */
  public boolean matches(double d1, double d2)
  {
    int compareResult = Double.compare(d1, d2 );
    if(compareResult < 0)
      compareResult = -1;
    else if (compareResult > 0)
      compareResult = 1;

    return matchingComparatorValues.contains(compareResult);
  }




}
