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

  private final boolean DO_PRESORT;

  private boolean isSorted;

  private List<T> intervals;

  private boolean isTainted;

  private IntervalI[] orderedIntervalStarts;

  private static Comparator<IntervalI> icompare = new IntervalComparator();

  // private Comparator<IntervalI> icompare = new IntervalComparator();

  /**
   * Constructor
   */
  public IntervalStore()
  {
    intervals = new ArrayList<>();
    DO_PRESORT = true;
  };
  
  public IntervalStore(boolean presort)
  {
    intervals = new ArrayList<>();
    DO_PRESORT = presort;
  };

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
    this.intervals = intervals;
    DO_PRESORT = presort;
    if (DO_PRESORT)
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

    synchronized (intervals)
    {
      if (DO_PRESORT)
      {
        int insertPosition = findFirstBegin(intervals, interval.getBegin());
        intervals.add(insertPosition, interval);
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

  @Override
  public void clear()
  {
    intervals.clear();
    orderedIntervalStarts = null;
    isTainted = true;
  }

  @Override
  public boolean contains(Object entry)
  {
    return listContains(intervals, entry);
  }

  private void ensureFinalized()
  {
    if (isTainted && intervals.size() >= 2)
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
   * Sort intervals by start (lowest first) and end (highest first).
   */
  private void sort()
  {
    Collections.sort(intervals, icompare);
    isSorted = true;
  }

  protected int findFirstBegin(List<T> list, long pos)
  {
    int start = 0;
    int end = list.size() - 1;
    int matched = list.size();

    while (start <= end)
    {
      int mid = (start + end) / 2;
      if (list.get(mid).getBegin() >= pos)
      {
        matched = mid;
        end = mid - 1;
      }
      else
      {
        start = mid + 1;
      }
    }
    return matched;
  }

  protected int findFirstEnd(List<T> list, long pos)
  {
    int start = 0;
    int end = list.size() - 1;
    int matched = list.size();

    while (start <= end)
    {
      int mid = (start + end) / 2;
      if (list.get(mid).getEnd() >= pos)
      {
        matched = mid;
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
   * Adds non-nested intervals to the result list that lie within the target
   * range
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findIntervalOverlaps(long from, long to,
          List<T> result)
  {

    int startIndex = findFirstEnd(intervals, from);
    final int startIndex1 = startIndex;
    int i = startIndex1;
    while (i < intervals.size())
    {
      T sf = intervals.get(i);
      if (sf.getBegin() > to)
      {
        break;
      }
      if (sf.getBegin() <= to && sf.getEnd() >= from)
      {
        result.add(sf);
      }
      i++;
    }
  }


  @Override
  public List<T> findOverlaps(long start, long end)
  {
    return findOverlaps(start, end, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<T> findOverlaps(long start, long end, List<T> result)
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
      if (sf.getBegin() <= end && sf.getEnd() >= start)
      {
        result.add(sf);
      }
      return result;
    }

    ensureFinalized();

    // (1) Find the closest feature to this position.

    int index = getClosestFeature(orderedIntervalStarts, start);
    IntervalI sf = (index < 0 ? null : orderedIntervalStarts[index]);

    // (2) Traverse the containedBy field, checking for overlap.

    while (sf != null)
    {
      if (sf.getEnd() >= start)
      {
        result.add((T) sf);
      }
      sf = sf.getContainedBy();
    }

    // (3) For an interval, find the last feature that starts in this interval,
    // and add all features up through that feature.

    if (end >= start)
    {
      // fill in with all features that start within this interval, fully
      // inclusive
      int index2 = getClosestFeature(orderedIntervalStarts, end);
      while (++index <= index2)
      {
        result.add((T) orderedIntervalStarts[index]);
      }

    }
    return result;
  }

  private int getClosestFeature(IntervalI[] l, long pos)
  {
    int low = 0;
    int high = l.length - 1;
    while (low <= high)
    {
      int mid = (low + high) >>> 1;
      IntervalI f = l[mid];
      switch (Long.signum(f.getBegin() - pos))
      {
      case -1:
        low = mid + 1;
        continue;
      case 1:
        high = mid - 1;
        continue;
      case 0:

        while (++mid <= high && l[mid].getBegin() == pos)
        {
          ;
        }
        return --mid;
      }
    }
    return (high < 0 ? -1 : high);
  }

  @SuppressWarnings("unchecked")
  public T getContainedBy(T sf, T sf0)
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
    int maxEnd = features[0].getEnd();
    for (int i = 1; i < n; i++)
    {
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
  protected boolean listContains(List<T> intervals, Object entry)
  {
    if (intervals == null || entry == null)
    {
      return false;
    }

    if (!isSorted)
    {
      return intervals.contains(entry);
    }

    @SuppressWarnings("unchecked")
    T interval = (T) entry;

    /*
     * locate the first entry in the list which does not precede the interval
     */
    int pos = findFirstBegin(intervals, interval.getBegin());
    int len = intervals.size();
    while (pos < len)
    {
      T sf = intervals.get(pos);
      if (sf.getBegin() > interval.getBegin())
      {
        return false; // no match found
      }
      if (sf.getEnd() == interval.getEnd() && sf.equals(interval))
      {
        return true;
      }
      pos++;
    }
    return false;
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

    /*
     * find the first interval that might match, i.e. whose 
     * start position is not less than the target range start
     * (NB inequality test ensures the first match if any is found)
     */
    int startIndex = findFirstBegin(intervals, entry.getBegin());

    /*
     * traverse intervals to look for a match
     */
    int from = entry.getBegin();
    int to = entry.getEnd();
    for (int i = startIndex, size = intervals.size(); i < size; i++)
    {
      T sf = intervals.get(i);
      if (sf.getBegin() > from)
      {
        return false;
      }
      if (sf.getEnd() == to && sf.equals(entry))
      {
        intervals.remove(i).setContainedBy(null);
        return (isTainted = true);
      }
    }
    return false;
  }

  @Override
  public int size()
  {
    return intervals.size();
  }

  @Override
  public String toString()
  {
    return prettyPrint();
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

  @Override
  public boolean revalidate()
  {
    isTainted = true;
    isSorted = true;
    ensureFinalized();
    return true;
  }

  @Override
  public IntervalI get(int i)
  {
    ensureFinalized();
    return (i < 0 || i >= orderedIntervalStarts.length ? null
            : orderedIntervalStarts[i]);
  }

}
