package intervalstore.nonc;

import java.util.Comparator;

public class IntervalComparator implements Comparator<IntervalI>
{

  @Override
  public int compare(IntervalI o1, IntervalI o2)
  {
    int order = Integer.compare(o1.getBegin(), o2.getBegin());
    return (order == 0 ? Integer.compare(o2.getEnd(), o1.getEnd()) : order);
  }

}
