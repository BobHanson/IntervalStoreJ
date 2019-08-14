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
package intervalstore.nonc;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import intervalstore.api.IntervalI;
import intervalstore.api.IntervalStoreI;

/**
 * 
 * A second idea, doing a double binary sort for the full interval. Seemed like
 * a good idea, but is 50% slower.
 * 
 * A Collection class to store interval-associated data, with options for "lazy"
 * sorting so as to speed incremental construction of the data prior to issuing
 * a findOverlap call.
 * 
 * 
 * Accepts duplicate entries but not null values.
 * 
 * 
 * 
 * @author Bob Hanson 2019.08.06
 *
 * @param <T>
 *          any type providing <code>getBegin()</code>, <code>getEnd()</code>
 *          <code>getContainedBy()</code>, and <code>setContainedBy()</code>
 */
public class IntervalStore<T extends IntervalI>
        extends AbstractCollection<T> implements IntervalStoreI<T>
{

  /**
   * Search for the last interval that starts before or at the specified from/to
   * range and the first interval that starts after it. In the situation that
   * there are multiple intervals starting at from, this method returns the
   * first of those.
   * 
   * @param a
   * @param from
   * @param to
   * @param ret
   * @return
   */
  public static int binaryLastIntervalSearch(IntervalI[] a, long from,
          long to, int[] ret)
  {
    int start = 0, start2 = 0;
    int matched = 0;
    int end = a.length - 1, end2 = a.length;
    int mid, begin;
    IntervalI e;
    while (start <= end)
    {
      mid = (start + end) >>> 1;
      e = a[mid];
      begin = e.getBegin();
      switch (Long.signum(begin - from))
      {
      case -1:
        matched = mid;
        start = mid + 1;
        break;
      case 0:
      case 1:
        end = mid - 1;
        if (begin > to)
        {
          end2 = mid;
        }
        else
        {
          start2 = mid;
        }
        break;
      }
    }
    ret[0] = end2;
    start = Math.max(start2, end);
    end = end2 - 1;

    while (start <= end)
    {
      mid = (start + end) >>> 1;
      e = a[mid];
      begin = e.getBegin();
      if (begin > to)
      {
        ret[0] = mid;
        end = mid - 1;
      }
      else
      {
        start = mid + 1;
      }

    }
    return matched;
  }

  /**
   * My preference is for a bigendian comparison, but you may differ.
   */
  private Comparator<? super IntervalI> icompare;

  /**
   * bigendian is what NCList does; change icompare to switch to that
   */
  private boolean bigendian;

  private final boolean DO_PRESORT;

  private boolean isSorted;

  private List<T> intervals;

  private int minStart = Integer.MAX_VALUE, maxStart = Integer.MIN_VALUE,
          maxEnd = Integer.MAX_VALUE;

  // private Comparator<IntervalI> icompare = new IntervalComparator();

  private boolean isTainted;
  
  private IntervalI[] ordered;

  private int[] offsets;

  private int[] ret = new int[1];

  /**
   * Constructor
   */
  public IntervalStore()
  {
    this(true);
  }

  public IntervalStore(boolean presort)
  {
    this(null, presort);
  }

  /**
   * Constructor given a list of intervals. Note that the list may get sorted as
   * a side-effect of calling this constructor.
   */
  public IntervalStore(List<T> intervals)
  {
    this(intervals, true);
  }

  /**
   * Allows a presort option, which can speed up initial loading of individual
   * features but will delay the first findOverlap if set to true.
   * 
   * @param intervals
   * @param presort
   */
  public IntervalStore(List<T> intervals, boolean presort)
  {
    this(intervals, presort, null, false);
  }

  /**
   * 
   * @param intervals
   *          intervals to initialize with (others may still be added)
   * @param presort
   *          whether or not to presort the list as additions are made
   * @param comparator
   *          IntervalI.COMPARATOR_LITTLEENDIAN or
   *          IntervalI.COMPARATOR_BIGENDIAN, but this could also be one that
   *          sorts by description as well, for example.
   * @param bigendian
   *          true if the comparator sorts [10-30] before [10-20]
   */
  public IntervalStore(List<T> intervals, boolean presort,
          Comparator<? super IntervalI> comparator, boolean bigendian)
  {
    this.intervals = (intervals == null ? new ArrayList<>() : intervals);
    DO_PRESORT = presort;
    icompare = (comparator != null ? comparator
            : bigendian ? IntervalI.COMPARATOR_BIGENDIAN
                    : IntervalI.COMPARATOR_LITTLEENDIAN);
    this.bigendian = bigendian;

    if (DO_PRESORT && this.intervals.size() > 1)
    {
      sort();
    }
    isTainted = true;
  }

  /**
   * Adds one interval to the store.
   * 
   * @param interval
   */
  @Override
  public boolean add(T interval)
  {
    if (interval == null)
    {
      return false;
    }

    int start = interval.getBegin();
    int end = interval.getEnd();
    minStart = Math.min(minStart, start);
    maxStart = Math.max(maxStart, start);

    synchronized (intervals)
    {
      if (DO_PRESORT)
      {
        intervals.add(binaryInsertionSearch(intervals, start, end),
                interval);
        isSorted = true;
      }
      else
      {
        intervals.add(interval);
        isSorted = false;
      }
      isTainted = true;
      return true;
    }
  }

  /**
   * for remove() and contains()
   * 
   * @param list
   * @param interval
   * @return
   */
  private int binaryIdentitySearch(List<T> list, IntervalI interval)
  {
    int start = 0;
    int r0 = interval.getBegin();
    int r1 = interval.getEnd();
    int end = list.size() - 1;
    if (end < 0 || r0 > maxStart || r1 > maxEnd || r0 < minStart)
      return -1;
    while (start <= end)
    {
      int mid = (start + end) >>> 1;
      IntervalI r = list.get(mid);
      switch (Integer.signum(r.getBegin() - r0))
      {
      case -1:
        start = mid + 1;
        continue;
      case 1:
        end = mid - 1;
        continue;
      case 0:
        // found one; just scan up and down now
        for (int i = mid; i <= end; i++)
          if ((r = list.get(i)).getEnd() == r1 && r.getBegin() == r0
                  && r.equals(interval))
            return i;
        for (int i = mid; --i >= start;)
          if ((r = list.get(i)).getEnd() == r1 && r.getBegin() == r0
                  && r.equals(interval))
            return i;
        return -1;
      }
    }
    return -1;
  }

  private int binaryInsertionSearch(List<T> list, long from, long to)
  {
    int matched = list.size();
    int end = matched - 1;
    int start = matched;
    if (end < 0 || from > list.get(end).getEnd()
            || from < list.get(start = 0).getBegin())
      return start;
    while (start <= end)
    {
      int mid = (start + end) >>> 1;
      switch (compareRange(list.get(mid), from, to))
      {
      case 0:
        return mid;
      case 1:
        matched = mid;
        end = mid - 1;
        continue;
      case -1:
        start = mid + 1;
        continue;
      }

    }
    return matched;
  }


  @Override
  public void clear()
  {
    intervals.clear();
    ordered = null;
    offsets = null;
    isTainted = true;
    minStart = maxEnd = Integer.MAX_VALUE;
    maxStart = Integer.MIN_VALUE;
  }
  /**
   * Compare an interval t to a from/to range for insertion purposes
   * 
   * @param t
   * @param from
   * @param to
   * @return 0 if same, 1 if start is after from, or start equals from and
   *         [bigendian: end is before to | littleendian: end is after to], else
   *         -1
   */
  private int compareRange(T t, long from, long to)
  {
    int order = Long.signum(t.getBegin() - from);
    return (order == 0
            ? Long.signum(bigendian ? to - t.getEnd() : t.getEnd() - to)
            : order);
  }

  @Override
  public boolean contains(Object entry)
  {
    return listContains(intervals, entry);
  }

  public boolean containsInterval(IntervalI outer, IntervalI inner)
  {
    ensureFinalized();
    int index = binaryIdentitySearch(intervals, inner);
    if (index >= 0)
      while ((index = index - Math.abs(offsets[index])) >= 0)
      {
        if (ordered[index] == outer)
        {
          return true;
        }
      }
    return false;
  }

  private void ensureFinalized()
  {

    int n = intervals.size();
    if (isTainted && n > 1)
    {
      if (!isSorted)
      {
        sort();
      }
      ordered = intervals.toArray(new IntervalI[n]);
      offsets = new int[n];
      linkFeatures(ordered);
      isTainted = false;
    }
  }

  /**
   * Find all overlaps within the given range, inclusively.
   * 
   * @return a list sorted in ascending order of start position
   * 
   */
  @Override
  public List<T> findOverlaps(long from, long to)
  {
    List<T> list = findOverlaps(from, to, null);
    Collections.reverse(list);
    return list;
  }

  /**
   * Find all overlaps within the given range, inclusively.
   * 
   * @return a list sorted in descending order of start position
   * 
   */
  
  @SuppressWarnings("unchecked")
  @Override
  public List<T> findOverlaps(long from, long to, List<T> result)
  {
    if (result == null)
    {
      result = new ArrayList<>();
    }
    int n = intervals.size();
    switch (n)
    {
    case 0:
      return result;
    case 1:
      T sf = intervals.get(0);
      if (sf.getBegin() <= to && sf.getEnd() >= from)
      {
        result.add(sf);
      }
      return result;
    }

    ensureFinalized();

    if (from > maxEnd || to < minStart)
      return result;
    int index = binaryLastIntervalSearch(ordered, from, to, ret);
    int index1 = ret[0];
    if (index1 < 0)
      return result;

    if (index1 > index + 1)
    {
      while (--index1 > index)
      {
        result.add((T) ordered[index1]);
      }
    }
    boolean isMonotonic = false;
    while (index >= 0)
    {
      IntervalI sf = ordered[index];
      if (sf.getEnd() >= from)
      {
        result.add((T) sf);
      }
      else if (isMonotonic)
      {
        break;
      }
      int offset = offsets[index];
      isMonotonic = (offset < 0);
      index -= (isMonotonic ? -offset : offset);
    }
    return result;
  }

  @Override
  public IntervalI get(int i)
  {
    ensureFinalized();
    return (i < 0 || i >= ordered.length ? null : ordered[i]);
  }

  private int getContainedBy(int index, int begin)
  {
    while (index >= 0)
    {
      IntervalI sf = ordered[index];
      if (sf.getEnd() >= begin)
      {
        // System.out.println("\nIS found " + sf0.getIndex1() + ":" + sf0
        // + "\nFS in " + sf.getIndex1() + ":" + sf);
        return index;
      }
      index -= Math.abs(offsets[index]);
    }
    return IntervalI.NOT_CONTAINED;
  }

  @Override
  public int getDepth()
  {
    int n = intervals.size();
    if (n < 2)
    {
      return n;
    }
    ensureFinalized();
    int maxDepth = 1;
    IntervalI root = null;
    for (int i = 0; i < n; i++)
    {
      IntervalI element = ordered[i];
      if (offsets[i] == IntervalI.NOT_CONTAINED)
      {
        root = element;
      }
      int depth = 1;
      int index = i;
      int offset;
      while ((index = index - Math.abs(offset = offsets[index])) >= 0)
      {
        element = ordered[index];
        if (++depth > maxDepth && (element == root || offset < 0))
        {
          maxDepth = depth;
          break;
        }
      }
    }
    return maxDepth;
  }

  @Override
  public int getWidth()
  {
    ensureFinalized();
    int w = 0;
    for (int i = offsets.length; --i >= 0;)
    {
      if (offsets[i] > 0)
      {
        w++;
      }
    }
    return w;
  }

  @Override
  public boolean isValid()
  {
    ensureFinalized();
    return true;
  }

  /**
   * Answers an iterator over the intervals in the store, with no particular
   * ordering guaranteed. The iterator does not support the optional
   * <code>remove</code> operation (throws
   * <code>UnsupportedOperationException</code> if attempted).
   */
  @Override
  public Iterator<T> iterator()
  {
    return intervals.iterator();
  }

  private void linkFeatures(IntervalI[] features)
  {
    int n = features.length;
    if (n == 0)
      return;
    maxEnd = features[0].getEnd();
    offsets[0] = IntervalI.NOT_CONTAINED;
    if (n == 1)
    {
      return;
    }
    boolean isMonotonic = true;
    for (int i = 1; i < n; i++)
    {
      IntervalI sf = features[i];
      int begin = sf.getBegin();
      int index = (begin <= maxEnd ? getContainedBy(i - 1, begin) : -1);
        // System.out.println(sf + " is contained by "
      // + (index < 0 ? null : starts[index]));

      offsets[i] = (index < 0 ? IntervalI.NOT_CONTAINED
              : isMonotonic ? index - i : i - index);
      isMonotonic = (sf.getEnd() > maxEnd);
      if (isMonotonic)
      {
        maxEnd = sf.getEnd();
      }
    }

  }

  /**
   * Answers true if the list contains the interval, else false. This method is
   * optimised for the condition that the list is sorted on interval start
   * position ascending, and will give unreliable results if this does not hold.
   * 
   * @param intervals
   * @param entry
   * @return
   */
  protected boolean listContains(List<T> intervals, Object entry)
  {
    return (intervals != null && entry != null
            && (isSorted
                    ? binaryIdentitySearch(intervals,
                            (IntervalI) entry) >= 0
                    : intervals.contains(entry)));
  }

  @Override
  public String prettyPrint()
  {
    int n = intervals.size();
    if (n == 0)
    {
      return "";
    }
    if (n == 1)
    {
      return intervals.get(0) + "\n";
    }
    ensureFinalized();
    String sep = "\t";
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < n; i++)
    {
      IntervalI range = ordered[i];
      int index = i;
      while ((index = index - Math.abs(offsets[index])) >= 0)
      {
        sb.append(sep);
      }
      sb.append(range.toString()).append('\n');
    }
    return sb.toString();
  }

  @Override
  public synchronized boolean remove(Object o)
  {
    // if (o == null)
    // {
    // throw new NullPointerException();
    // }
    return (o != null && intervals.size() > 0
            && removeInterval((IntervalI) o));
  }

  /**
   * Uses a binary search to find the entry and removes it if found.
   * 
   * @param entry
   * @return
   */
  protected boolean removeInterval(IntervalI entry)
  {
    if (!isSorted)
    {
      return intervals.remove(entry);
    }
    int i = binaryIdentitySearch(intervals, entry);
    if (i < 0)
      return false;
    intervals.remove(i);
    return (isTainted = true);
  }

  @Override
  public boolean revalidate()
  {
    isTainted = true;
    isSorted = true;
    ensureFinalized();
    return true;
  }

  @Override
  public int size()
  {
    return intervals.size();
  }

  /**
   * Sort intervals by start (lowest first) and end (highest first).
   */
  private void sort()
  {
    Collections.sort(intervals, icompare);
    if (intervals.size() > 0)
    {
      T r = intervals.get(intervals.size() - 1);
      minStart = Math.min(minStart, intervals.get(0).getBegin());
      maxStart = Math.max(maxStart, r.getBegin());
    }
    isSorted = true;
  }

  @Override
  public String toString()
  {
    return prettyPrint();
  }

}
