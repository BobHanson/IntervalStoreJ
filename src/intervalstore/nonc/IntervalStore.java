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



/**
 * 
 * A Collection class to store interval-associated data, with options for "lazy"
 * sorting so as to speed incremental construction of the data prior to issuing
 * a findOverlap call.
 * 
 * 
 * with O(log N) performance for overlap queries, insertion and deletion (where
 * N is the size of the store).
 * 
 * Accepts duplicate entries but not null values.
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
   * Search for the ANY overlapping interval. The innovation here is to take
   * advantage of the fact that we are searching for intervals, not just
   * numbers. We can match any overlapping interval of any sort, because all
   * overlapping intervals are contiguous.
   * 
   * So once we find one, we have found all of them. We just might be in the
   * middle of them. The containedBy linked list gets us the backward set, and
   * our ordered array gets us the forward set.
   * 
   * Basically, we have established a binary partition tree that we can navigate
   * easily.
   * 
   * 
   * 
   * @param a
   * @param from
   * @param to
   * @return the matching index, or -1 if there is no match
   */
  public static int binaryAnyIntervalSearch(IntervalI[] a, long from,
          long to)
  {
    int start = 0;
    int end = a.length - 1;
    while (start <= end)
    {
      int mid = (start + end) >>> 1;
      if (a[mid].getEnd() >= from)
      {
        if (a[mid].getBegin() <= to)
          return mid;
        end = mid - 1;
      }
      else
      {
        start = mid + 1;
      }
    }
    return -1;
  }

  /**
   * Search for the LAST overlapping interval. This is useful when we do not
   * have an NCList
   * 
   * 
   * @param a
   * @param from
   * @param to
   * @return the matching index, or -1 if there is no match
   */
  public static int binaryLastIntervalSearch(IntervalI[] a, long from,
          long to)
  {
    int start = 0;
    int matched = -1;
    int end = a.length - 1;
    while (start <= end)
    {
      int mid = (start + end) >>> 1;
      if (a[mid].getBegin() > to)
      {
        end = mid - 1;
      }
      else
      {
        if (a[mid].getEnd() >= from)
          matched = mid;
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
  
  private IntervalI[] orderedIntervalStarts;

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
    this(intervals, presort, null, true);
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
  private int binaryIdentitySearch(List<T> list, T interval)
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
    orderedIntervalStarts = null;
    isTainted = true;
    minStart = maxEnd = Integer.MAX_VALUE;
    maxStart = Integer.MIN_VALUE;
  }

  /**
   * Compare an interval t to a from/to range.
   * 
   * @param t
   * @param from
   * @param to
   * @return -1 if t comes before range, 1 if after, 0 if overlapping
   */
  private int compareOverlap(T t, long from, long to)
  {
    int order = Long.signum(t.getBegin() - from);
    return (order == 0
            ? Long.signum(bigendian ? to - t.getEnd() : t.getEnd() - to)
            : order);
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

  private void ensureFinalized()
  {

    if (isTainted && intervals.size() > 1)
    {
      if (!isSorted)
      {
        sort();
      }
      orderedIntervalStarts = intervals.toArray(new IntervalI[0]);
      linkFeatures(orderedIntervalStarts);
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
    // Collections.reverse(list);
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
    int pt = binaryLastIntervalSearch(orderedIntervalStarts, from, to);
    if (pt < 0)
      return result;
    IntervalI sf = orderedIntervalStarts[pt++];
    while (sf != null)
    {
      int index = sf.getIndex();
      if (sf.getBegin() >= from)
      {
        // fully contained -- take all
        while (--pt > index)
        {
          result.add((T) orderedIntervalStarts[pt]);
        }
        result.add((T) sf);
      }
      else if (sf.getEnd() >= from)
      {
        // partially contained

        // fill in the gaps only if the first partially contained interval

        while (--pt > index)
        {
          T t = (T) orderedIntervalStarts[pt];
          if (t.getEnd() >= from)
          {
            result.add(t);
          }
        }
        pt = 0; // no more gap filling
        result.add((T) sf);
      }
      sf = sf.getContainedBy();
    }
    return result;
  }


  @Override
  public IntervalI get(int i)
  {
    ensureFinalized();
    return (i < 0 || i >= orderedIntervalStarts.length ? null
            : orderedIntervalStarts[i]);
  }

  @SuppressWarnings("unchecked")
  private T getContainedBy(T sf, T sf0)
  {
    int begin = sf0.getBegin();
    while (sf != null)
    {
      if (begin <= sf.getEnd())
      {
        // System.out.println("\nIS found " + sf0.getIndex1() + ":" + sf0
        // + "\nFS in " + sf.getIndex1() + ":" + sf);
        return sf;
      }
      sf = (T) sf.getContainedBy();
    }
    return null;
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
    T root = null;
    for (int i = 0; i < n; i++)
    {
      T element = intervals.get(i);
      IntervalI container = element;
      if (element.getContainedBy() == null)
      {
        root = element;
      }
      int depth = 1;
      while ((container = container.getContainedBy()) != null)
      {
        if (++depth > maxDepth && container == root)
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
    for (int i = intervals.size(); --i >= 0;)
    {
      if (intervals.get(i).getContainedBy() == null)
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

  @SuppressWarnings("unchecked")
  private void linkFeatures(IntervalI[] features)
  {
    int n = features.length;
    if (n < 2)
    {
      return;
    }
    maxEnd = features[0].getEnd();
    features[0].setIndex(0);
    for (int i = 1; i < n; i++)
    {
      features[i].setIndex(i);
      T sf = (T) features[i];
      if (sf.getBegin() <= maxEnd)
      {
        sf.setContainedBy(getContainedBy((T) features[i - 1], sf));
      }
      if (sf.getEnd() > maxEnd)
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
  @SuppressWarnings("unchecked")
  protected boolean listContains(List<T> intervals, Object entry)
  {
    return (intervals != null && entry != null
            && (isSorted ? binaryIdentitySearch(intervals, (T) entry) >= 0
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
      IntervalI range = orderedIntervalStarts[i];
      IntervalI container = range.getContainedBy();
      while (container != null)
      {
        sb.append(sep);
        container = container.getContainedBy();
      }
      sb.append(range.toString()).append('\n');
    }
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  @Override
  public synchronized boolean remove(Object o)
  {
    // if (o == null)
    // {
    // throw new NullPointerException();
    // }
    return (o != null && intervals.size() > 0
            && removeInterval((T) o));
  }

  /**
   * Uses a binary search to find the entry and removes it if found.
   * 
   * @param entry
   * @return
   */
  protected boolean removeInterval(T entry)
  {
    if (!isSorted)
    {
      return intervals.remove(entry);
    }
    int i = binaryIdentitySearch(intervals, entry);
    if (i < 0)
      return false;
    intervals.remove(i).setContainedBy(null);
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
