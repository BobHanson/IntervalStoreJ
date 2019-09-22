/*
BSD 3-Clause License

Copyright (c) 2018, Mungo Carstairs
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this
  list of conditions and the following disclaimer.

* Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation
  and/or other materials provided with the distribution.

* Neither the name of the copyright holder nor the names of its
  contributors may be used to endorse or promote products derived from
  this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package intervalstore.impl1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import intervalstore.api.IntervalI;

/**
 * A comparator that orders ranges by either start position ascending. If
 * position matches, ordering is by length descending. This provides the
 * canonical ordering of intervals into subranges in order to build a nested
 * containment list.
 * 
 * @author gmcarstairs
 */
public class NCListBuilder<T extends IntervalI>
{
  // class NCListComparator<V extends IntervalI> implements Comparator<V>
  // {
  // /**
  // * Compares two intervals in a way that will sort a list by start position
  // * ascending, then by length descending. Answers
  // * <ul>
  // * <li>a negative value if o1.begin < o2.begin</li>
  // * <li>else a positive value if o1.begin > o2.begin</li>
  // * <li>else a negative value if o1.end > o2.end</li>
  // * <li>else a positive value of o1.end < o2.end</li>
  // * <li>else zero</li
  // * </ul>
  // */
  // @Override
  // public int compare(V o1, V o2)
  // {
  // int order = Integer.compare(o1.getBegin(), o2.getBegin());
  // if (order == 0)
  // {
  // /*
  // * if tied on start position, longer length sorts to left
  // * i.e. the negation of normal ordering by length
  // */
  // order = Integer.compare(o2.getEnd(), o1.getEnd());
  // }
  // return order;
  // }
  // }

  private Comparator<? super IntervalI> comparator = IntervalI.COMPARATOR_BIGENDIAN;// new
                                                                                    // NCListComparator<>();
  
  /**
   * Default constructor
   */
  public NCListBuilder()
  {
  }

  /**
   * Answers a comparator which sorts items of type T by start position
   * ascending, and within that by end position (length) descending)
   * 
   * @return
   */
  Comparator<? super IntervalI> getComparator()
  {
    return comparator;
  }

  /**
   * Sorts and traverses the ranges to identify sublists, whose start intervals
   * are overlapping or disjoint but not mutually contained. Answers a list of
   * start-end indices of the sorted list of ranges.
   * 
   * @param ranges
   * @return
   */
  List<IntervalI> partitionNestedSublists(List<T> ranges)
  {
    List<IntervalI> sublists = new ArrayList<>();
  
    /*
     * sort by start ascending, length descending, so that
     * contained intervals follow their containing interval
     */
    Collections.sort(ranges, comparator);
  
    int listStartIndex = 0;
  
    IntervalI lastParent = ranges.get(0);
    boolean first = true;
  
    for (int i = 0, n = ranges.size(); i < n; i++)
    {
      IntervalI nextInterval = ranges.get(i);
      if (!first && !lastParent.properlyContainsInterval(nextInterval))
      {
        /*
         * this interval is not properly contained in the parent; 
         * close off the last sublist
         */
        sublists.add(new Range(listStartIndex, i - 1));
        listStartIndex = i;
        lastParent = nextInterval;
      }
      first = false;
    }
  
    sublists.add(new Range(listStartIndex, ranges.size() - 1));
    return sublists;
  }
}
