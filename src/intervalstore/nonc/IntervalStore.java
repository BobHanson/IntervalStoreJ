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
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import intervalstore.api.IntervalI;
import intervalstore.api.IntervalStoreI;

/**
 * 
 * A fourth idea, implementing NCList as a pointer system identical in operation
 * to IntervalStoreJ's implementation using ArrayLists but here using just two
 * int[] arrays and a single IntervalI[] array that is in the proper order for
 * holding all nested and unnested arrays.
 * 
 * Use of unnesting is optional and can be experimented with by changing the
 * createUnnested flag to false.
 * 
 * Preliminary testing suggests that this implementation is about 10% faster for
 * store interval size 50, store sequence factor 10, query width -1000 (fixed
 * 1000-unit-wide window), and query count 100000.
 * 
 * Origional note (Mungo Carstairs, IntervalStoreJ)
 * 
 * A Collection class to store interval-associated data, with options for "lazy"
 * sorting so as to speed incremental construction of the data prior to issuing
 * a findOverlap call.
 * 
 * Accepts duplicate entries but not null values.
 * 
 * 
 * 
 * @author Bob Hanson 2019.09.01
 *
 * @param <T>
 *          any type providing <code>getBegin()</code> and
 *          <code>getEnd()</code>, primarily
 */
public class IntervalStore<T extends IntervalI>
        extends AbstractCollection<T> implements IntervalStoreI<T>
{

  /**
   * Search for the last interval that ends at or just after the specified
   * position. In the situation that there are multiple intervals starting at
   * pos, this method returns the first of those.
   * 
   * @param nests
   *          the nest-ordered array from createArrays()
   * @param from
   *          the position at the start of the interval of interest
   * @param start
   *          the starting point for the subarray search
   * @param end
   *          the ending point for the subarray search
   * @return index into the nests array or one greater than end if not found
   */
  public static int binarySearchFirstEndNotBefore(IntervalI[] nests, long from,
          int start, int end)
  {
    int matched = end + 1;
    int mid;
    while (start <= end)
    {
      mid = (start + end) >>> 1;
      if (nests[mid].getEnd() >= from)
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
   * My preference is for a bigendian comparison, but you may differ.
   */
  private Comparator<? super IntervalI> icompare;

  /**
   * bigendian is what NCList does; change icompare to switch to that
   */
  private boolean bigendian;

  private final boolean DO_PRESORT;

  private boolean isSorted;

  private boolean createUnnested = true;

  private int minStart = Integer.MAX_VALUE, maxStart = Integer.MIN_VALUE,
          maxEnd = Integer.MAX_VALUE;

  private boolean isTainted;

  private int capacity = 8;

  protected IntervalI[] intervals = new IntervalI[capacity];

  private int[] offsets;

  protected int intervalCount;

  private int added;

  private int deleted;

  private BitSet bsDeleted;

  /**
   * the key array that lists the intervals in sub-interval order so that the
   * binary search can be isolated to a single subinterval just by indicating
   * start and end within one array
   */
  private IntervalI[] nests;

  /**
   * pointers to the starting positions in nests[] for a subinterval; the first
   * element is the "unnested" pointer when unnesting (2) or the root level nest
   * pointer when not unnesting (1); the second element is root level nest when
   * unnesting or the start of nest data when not unnesting; after that, nests
   * are in contiguous sets of binary-searchable blocks
   * 
   */
  private int[] nestOffsets;

  /**
   * the count of intervals within a nest
   * 
   */
  private int[] nestLengths;

  /**
   * pointer to the root nest (intervalCount)
   */
  private int root;

  /**
   * pointer to the unnested set (intervalCount + 1);
   */
  private int unnested;

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
    // setting default to BIG_ENDIAN, meaning
    // the order will be [10,100] before [10,80]
    // this order doesn't really matter much.
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
   *          IntervalI.COMPARE_BEGIN_ASC_END_DESC, but this could also be one that
   *          sorts by description as well, for example.
   * @param bigendian
   *          true if the comparator sorts [10-100] before [10-80]; defaults to
   *          true
   */
  public IntervalStore(List<T> intervals, boolean presort,
          Comparator<? super IntervalI> comparator, boolean bigendian)
  {
    icompare = (comparator != null ? comparator
            : bigendian ? IntervalI.COMPARE_BEGIN_ASC_END_DESC
                    : IntervalI.COMPARE_BEGIN_ASC_END_ASC);
    this.bigendian = bigendian;

    if (intervals != null)
    {
      // So, five hours later, we learn that all my timing has been thrown off
      // because I used Array.sort, which if you look in the Java JDK is exactly
      // what Collections.sort is, but for whatever reason, all my times were
      // high by about 100-200 ms 100% reproducibly. Just one call to Array.sort
      // prior to the nanotimer start messed it all up. Some sort of memory or
      // garbage issue; I do not know. But using Collections.sort here fixes the
      // problem.

      Collections.sort(intervals, icompare);
      intervals.toArray(
              this.intervals = new IntervalI[capacity = intervalCount = intervals
                      .size()]);
    }
    DO_PRESORT = presort;
    if (DO_PRESORT && intervalCount > 1)
    {
      updateMinMaxStart();
      isSorted = true;
      isTainted = true;
      ensureFinalized();
    }
    else
    {
      isSorted = DO_PRESORT;
      isTainted = true;
    }
  }

  /**
   * Adds one interval to the store, allowing duplicates.
   * 
   * @param interval
   */
  @Override
  public boolean add(T interval)
  {
    return add(interval, true);
  }

  /**
   * Adds one interval to the store, optionally checking for duplicates.
   * 
   * This fast-adding algorithm uses a double-length int[] (offsets) to hold
   * pointers into intervals[] that allows continual sorting of an expanding
   * array buffer. When the time comes, this is cleaned up and packed back into
   * a standard array, but in the mean time, it can be added to with no loss of
   * sorting.
   * 
   * @param interval
   * @param allowDuplicates
   */
  @Override
  public boolean add(T interval, boolean allowDuplicates)
  {
    if (interval == null)
    {
      return false;
    }

    if (deleted > 0)
    {
      finalizeDeletion();
    }
    if (!isTainted)
    {
      offsets = null;
      isTainted = true;
    }

    synchronized (intervals)
    {
      int index = intervalCount;
      int start = interval.getBegin();

      if (intervalCount + added + 1 >= capacity)
      {
        intervals = finalizeAddition(
                new IntervalI[capacity = capacity << 1]);

      }

      if (DO_PRESORT && isSorted)
      {
        if (intervalCount == 0)
        {
          // ignore
        }
        else
        {
          index = findInterval(interval);
          // System.out.println("index = " + index + " for " + interval + "\n"
          // + Arrays.toString(intervals) + "\n"
          // + Arrays.toString(offsets));
          if (!allowDuplicates && index >= 0)
          {
            return false;
          }
          if (index < 0)
          {
            index = -1 - index;
          }
          else
          {
            index++;
          }
        }

      }
      else
      {
        if (!allowDuplicates && findInterval(interval) >= 0)
        {
          return false;
        }
        isSorted = false;
      }

      if (index == intervalCount)
      {
        intervals[intervalCount++] = interval;
        // System.out.println("added " + intervalCount + " " + interval);
      }
      else
      {
        int pt = capacity - ++added;
        intervals[pt] = interval;
        // System.out.println("stashed " + pt + " " + interval + " for "
        // + index + " " + intervals[index]);
        if (offsets == null)
        {
          offsets = new int[capacity];
        }

        offsets[pt] = offsets[index];

        offsets[index] = pt;
      }

      minStart = Math.min(minStart, start);
      maxStart = Math.max(maxStart, start);
      return true;
    }
  }

  /**
   * Clean up the intervals array into a simple ordered array.
   * 
   * @param dest
   * @return
   */
  private IntervalI[] finalizeAddition(IntervalI[] dest)
  {
    if (dest == null)
    {
      dest = intervals;
    }
    if (added == 0)
    {
      if (intervalCount > 0 && dest != intervals)
      {
        System.arraycopy(intervals, 0, dest, 0, intervalCount);
      }
      capacity = dest.length;
      return dest;
    }
    // System.out.println("finalizing " + intervalCount + " " + added);

    // array is [(intervalCount)...null...(added)]

    int ntotal = intervalCount + added;
    for (int ptShift = ntotal, pt = intervalCount; pt >= 0;)
    {
      int pt0 = pt;
      while (--pt >= 0 && offsets[pt] == 0)
      {
        ;
      }
      if (pt < 0)
      {
        pt = 0;
      }
      int nOK = pt0 - pt;
      // shift upper intervals right
      ptShift -= nOK;
      if (nOK > 0)
      {
        System.arraycopy(intervals, pt, dest, ptShift, nOK);
      }
      if (added == 0)
      {
        break;
      }
      for (int offset = offsets[pt]; offset > 0; offset = offsets[offset])
      {
        dest[--ptShift] = intervals[offset];
        --added;
      }
    }
    offsets = null;
    intervalCount = ntotal;
    capacity = dest.length;
    // System.out.println(Arrays.toString(dest));
    return dest;
  }

  /**
   * A binary search for a duplicate.
   * 
   * @param interval
   * @return
   */
  public int binaryIdentitySearch(IntervalI interval)
  {
    return binaryIdentitySearch(interval, null);
  }

  /**
   * for remove() and contains()
   * 
   * @param list
   * @param interval
   * @param bsIgnore
   *          for deleted
   * @return index or, if not found, -1 - "would be here"
   */
  public int binaryIdentitySearch(IntervalI interval, BitSet bsIgnore)
  {
    int start = 0;
    int r0 = interval.getBegin();
    int r1 = interval.getEnd();
    int end = intervalCount - 1;
    if (end < 0 || r0 < minStart)
    {
      return -1;
    }
    if (r0 > maxStart)
    {
      return -1 - intervalCount;
    }
    while (start <= end)
    {
      int mid = (start + end) >>> 1;
      IntervalI r = intervals[mid];
      switch (compareRange(r, r0, r1))
      {
      case -1:
        start = mid + 1;
        continue;
      case 1:
        end = mid - 1;
        continue;
      case 0:
        IntervalI iv = intervals[mid];
        if ((bsIgnore == null || !bsIgnore.get(mid))
                && iv.equalsInterval(interval))
        {
          return mid;
          // found one; just scan up and down now, first checking the range, but
          // also checking other possible aspects of equivalence.
        }

        for (int i = mid; ++i <= end;)
        {
          if ((iv = intervals[i]).getBegin() != r0 || iv.getEnd() != r1)
          {
            break;
          }
          if ((bsIgnore == null || !bsIgnore.get(i))
                  && iv.equalsInterval(interval))
          {
            return i;
          }
        }
        for (int i = mid; --i >= start;)
        {
          if ((iv = intervals[i]).getBegin() != r0
                  || (bigendian ? r1 < iv.getEnd() : iv.getEnd() < r1))
          {
            return -1 - ++i;
          }
          if ((bsIgnore == null || !bsIgnore.get(i))
                  && iv.equalsInterval(interval))
          {
            return i;
          }
        }
        return -1 - mid;
      }
    }
    return -1 - start;
  }

  public boolean canCheckForDuplicates()
  {
    return true;
  }

  /**
   * Reset all arrays.
   * 
   */
  @Override
  public void clear()
  {
    intervalCount = added = 0;
    isSorted = true;
    isTainted = true;
    offsets = null;
    intervals = new IntervalI[8];
    nestOffsets = nestLengths = null;
    nests = null;
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
  private int compareRange(IntervalI t, long from, long to)
  {
    int order = Long.signum(t.getBegin() - from);
    return (order == 0
            ? Long.signum(bigendian ? to - t.getEnd() : t.getEnd() - to)
            : order);
  }

  @Override
  public boolean contains(Object entry)
  {
    if (entry == null || intervalCount == 0 && added == 0 && deleted == 0)
    {
      return false;
    }
    if (!isSorted || deleted > 0)
    {
      sort();
    }
    int n = findInterval((IntervalI) entry);
    return (n >= 0);
  }

  /**
   * Check to see if a given interval is within another.
   * 
   * Not implemented.
   * 
   * @param outer
   * @param inner
   * @return
   */
  public boolean containsInterval(IntervalI outer, IntervalI inner)
  {
    return false; // not applicable
  }

  /**
   * Ensure that all addition, deletion, and sorting has been done, and that the
   * nesting arrays have been created so that we are ready for findOverlaps().
   * 
   */

  private void ensureFinalized()
  {
    if (isTainted)
    {
      if (!isSorted || added > 0 || deleted > 0)
      {
        sort();
      }
      if (intervalCount > 0)
      {
        createArrays();
      }
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
    return findOverlaps(from, to, null);
  }

  /**
   * Find all overlaps within the given range, inclusively.
   * 
   * @return a list sorted in the order provided by the features list comparator
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
    switch (intervalCount + added)
    {
    case 0:
      return result;
    case 1:
      IntervalI sf = intervals[0];
      if (sf.getBegin() <= to && sf.getEnd() >= from)
      {
        result.add((T) sf);
      }
      return result;
    }

    ensureFinalized();

    if (from > maxEnd || to < minStart)
    {
      return result;
    }
    if (createUnnested)
    {
      if (nestLengths[unnested] > 0)
      {
        searchUnnested(from, to, (List<IntervalI>) result);
      }
    }
    if (nestLengths[root] > 0)
    {
      search(nests, from, to, root, result);
    }
    return result;
  }

  /**
   * A simpler search, since we know we don't have any subintervals. Not
   * necessary, actually.
   * 
   * @param nestOffsets
   * @param nestLengths
   * @param nests
   * @param from
   * @param to
   * @param result
   */
  private void searchUnnested(long from, long to, List<IntervalI> result)
  {
    int start = nestOffsets[unnested];
    int end = start + nestLengths[unnested] - 1;
    for (int pt = binarySearchFirstEndNotBefore(nests, from, start,
            end); pt <= end; pt++)
    {
      IntervalI ival = nests[pt];
      if (ival.getBegin() > to)
      {
        break;
      }
      result.add(ival);
    }
  }

  /**
   * The main search of the nests[] array's subarrays
   * 
   * @param nests
   * @param from
   * @param to
   * @param nest
   * @param result
   */
  @SuppressWarnings("unchecked")
  private void search(IntervalI[] nests, long from, long to, int nest,
          List<T> result)
  {
    int start = nestOffsets[nest];
    int n = nestLengths[nest];
    int end = start + n - 1;
    IntervalI first = nests[start];
    IntervalI last = nests[end];

    // quick tests for common cases:
    // out of range
    if (last.getEnd() < from || first.getBegin() > to)
    {
      return;
    }
    int pt;
    switch (n)
    {
    case 1:
      // just one interval and hasn't failed begin/end test
      pt = start;
      break;
    case 2:
      // just two and didn't fail begin/end test
      // so there is only one option: either the first or the second is our
      // winner
      pt = (first.getEnd() >= from ? start : end);
      break;
    default:
      // do the binary search
      pt = binarySearchFirstEndNotBefore(nests, from, start, end);
      break;
    }
    for (; pt <= end; pt++)
    {
      IntervalI ival = nests[pt];
      // check for out of range
      if (ival.getBegin() > to)
      {
        break;
      }
      result.add((T) ival);
      if (nestLengths[pt] > 0)
      {
        // check subintervals in this nest
        search(nests, from, to, pt, result);
      }
    }
  }

  /**
   * return the i-th interval in the designated order (bigendian or
   * littleendian)
   */
  public IntervalI get(int i)
  {
    if (i < 0 || i >= intervalCount + added)
    {
      return null;
    }
    ensureFinalized();
    return intervals[i];
  }

  /**
   * Return the deepest level of nesting.
   * 
   */
  @Override
  public int getDepth()
  {
    ensureFinalized();
    BitSet bsTested = new BitSet();
    return Math.max((createUnnested ? getDepth(unnested, bsTested) : 0),
            getDepth(root, bsTested));
  }

  /**
   * Iteratively dive deeply.
   * 
   * @param pt
   * @param bsTested
   * @return
   */
  private int getDepth(int pt, BitSet bsTested)
  {
    int maxDepth = 0;
    int depth;
    int n = nestLengths[pt];
    if (n == 0 || bsTested.get(pt))
    {
      return 1;
    }
    bsTested.set(pt);
    for (int st = nestOffsets[pt], i = st + n; --i >= st;)
    {
      if ((depth = getDepth(i, bsTested)) > maxDepth)
      {
        maxDepth = depth;
      }
    }
    return maxDepth + 1;
  }

  /**
   * Get the number of root-level nests.
   * 
   */
  public int getWidth()
  {
    ensureFinalized();
    return nestLengths[root] + (createUnnested ? nestLengths[unnested] : 0);
  }

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
    ensureFinalized();
    return new Iterator<T>()
    {

      private int next;

      @Override
      public boolean hasNext()
      {
        return next < intervalCount;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T next()
      {
        if (next >= intervalCount)
        {
          throw new NoSuchElementException();
        }
        return (T) intervals[next++];
      }

    };
  }

  /**
   * Indented printing of the intervals.
   * 
   */
  @Override
  public String prettyPrint()
  {
    ensureFinalized();
    StringBuffer sb = new StringBuffer();
    if (createUnnested)
    {
      sb.append("unnested:");
      dump(intervalCount + 1, sb, "\n");
      sb.append("\nnested:");
    }
    dump(intervalCount, sb, "\n");
    return sb.toString();
  }

  /**
   * Iterative nest dump.
   * 
   * @param nest
   * @param sb
   * @param sep
   */
  private void dump(int nest, StringBuffer sb, String sep)
  {
    int pt = nestOffsets[nest];
    int n = nestLengths[nest];
    sep += "  ";

    for (int i = 0; i < n; i++)
    {
      sb.append(sep).append(nests[pt + i].toString());
      if (nestLengths[pt + i] > 0)
        dump(pt + i, sb, sep + "  ");
    }
  }

  @Override
  public synchronized boolean remove(Object o)
  {
    // if (o == null)
    // {
    // throw new NullPointerException();
    // }
    return (o != null && intervalCount > 0
            && removeInterval((IntervalI) o));
  }

  /**
   * Find the interval or return where it should go, possibly into the add
   * buffer
   * 
   * @param interval
   * @return index (nonnegative) or index where it would go (negative)
   */

  private int findInterval(IntervalI interval)
  {

    if (isSorted)
    {
      int pt = binaryIdentitySearch(interval, null);
      // if (addPt == intervalCount || offsets[pt] == 0)
      // return pt;
      if (pt >= 0 || added == 0 || pt == -1 - intervalCount)
      {
        return pt;
      }
      pt = -1 - pt;
      int start = interval.getBegin();
      int end = interval.getEnd();

      int match = pt;

      while ((pt = offsets[pt]) != 0)
      {
        IntervalI iv = intervals[pt];
        switch (compareRange(iv, start, end))
        {
        case -1:
          break;
        case 0:
          if (iv.equalsInterval(interval))
          {
            return pt;
          }
          // fall through
        case 1:
          match = pt;
          continue;
        }
      }
      return -1 - match;
    }
    else
    {
      int i = intervalCount;
      while (--i >= 0 && !intervals[i].equalsInterval(interval))
      {
        ;
      }
      return i;
    }
  }

  /**
   * Uses a binary search to find the entry and removes it if found.
   * 
   * @param interval
   * @return
   */
  protected boolean removeInterval(IntervalI interval)
  {

    if (!isSorted || added > 0)
    {
      sort();
    }
    int i = binaryIdentitySearch(interval, bsDeleted);
    if (i < 0)
    {
      return false;
    }
    if (deleted == 0)
    {
      if (bsDeleted == null)
      {
        bsDeleted = new BitSet(intervalCount);
      }
      else
      {
        bsDeleted.clear();
      }
    }
    bsDeleted.set(i);
    deleted++;
    return (isTainted = true);
  }

  /**
   * Fill in the gaps of the intervals array after one or more deletions.
   * 
   */
  private void finalizeDeletion()
  {
    if (deleted == 0)
    {
      return;
    }

    // ......xxx.....xxxx.....xxxxx....
    // ......^i,pt
    // ...... .......
    // ............
    for (int i = bsDeleted.nextSetBit(0), pt = i; i >= 0;)
    {
      i = bsDeleted.nextClearBit(i + 1);
      int pt1 = bsDeleted.nextSetBit(i + 1);
      if (pt1 < 0)
      {
        pt1 = intervalCount;
      }
      int n = pt1 - i;
      System.arraycopy(intervals, i, intervals, pt, n);
      pt += n;
      if (pt1 == intervalCount)
      {
        for (i = pt1; --i >= pt;)
        {
          intervals[i] = null;
        }
        intervalCount -= deleted;
        deleted = 0;
        bsDeleted.clear();
        break;
      }
      i = pt1;
    }

  }

  /**
   * Recreate the key nest arrays.
   * 
   */
  public boolean revalidate()
  {
    isTainted = true;
    isSorted = false;
    ensureFinalized();
    return true;
  }

  /**
   * Return the total number of intervals in the store.
   * 
   */
  @Override
  public int size()
  {
    return intervalCount + added - deleted;
  }

  /**
   * AbstractCollection override to ensure that we have finalized the store.
   */
  @Override
  public Object[] toArray()
  {
    ensureFinalized();
    return super.toArray();
  }

  /**
   * Sort intervals by start.
   */
  private void sort()
  {
    if (added > 0)
    {
      intervals = finalizeAddition(new IntervalI[intervalCount + added]);
    }
    else if (deleted > 0)
    {
      finalizeDeletion();
    }
    else
    {
      // SOMETHING HAPPENS WHEN Arrays.sort is run that
      // adds 100 ms to a 150 ms run time.
      // I don't know why.
      Arrays.sort(intervals, 0, intervalCount, icompare);
    }
    updateMinMaxStart();
    isSorted = true;
  }

  /**
   * Create the key arrays: nests, nestOffsets, and nestLengths. The starting
   * point is getting the container array, which may point to then root nest or
   * the unnested set.
   * 
   * This is a pretty complicated method; it was way simpler before I decided to
   * support unnesting as an option.
   *
   */
  private void createArrays()
  {

    // goal here is to create an array, nests[], that is a block

    /**
     * When unnesting, we need a second top-level listing.
     * 
     */
    int len = intervalCount + (createUnnested ? 2 : 1);
    root = intervalCount;
    unnested = intervalCount + 1;

    /**
     * The three key arrays produced by this method:
     */
    nests = new IntervalI[intervalCount];
    nestOffsets = new int[len];
    nestLengths = new int[len];

    /**
     * the objectives of Phase One
     */

    int[] myContainer = new int[intervalCount];
    int[] counts = new int[len];

    // Phase One: Get the temporary container array myContainer.

    myContainer[0] = (createUnnested ? unnested : root);
    counts[myContainer[0]] = 1;

    // memories for previous begin and end

    int beginLast = intervals[0].getBegin();
    int endLast = intervals[0].getEnd();

    // memories for previous unnested pt, begin, and end

    int ptLastNot2 = root;
    int beginLast2 = beginLast;
    int endLast2 = endLast;

    for (int i = 1; i < intervalCount; i++)
    {
      int pt = i - 1;
      int begin = intervals[i].getBegin();
      int end = intervals[i].getEnd();

      // initialize the container to either root or unnested

      myContainer[i] = myContainer[0];

      // OK, now figure it all out...

      boolean isNested;
      if (createUnnested)
      {
        // Using a method isNested(...) here, because there are different
        // ways of defining "nested" when start or end are the
        // same. The definition used here would not be my first choice,
        // but it matches results for IntervalStoreJ
        // perfectly, down to the exact number of times that the
        // binary search runs through its start/mid/end loops in findOverlap.

        // beginLast2 and endLast2 refer to the last unnested level

        isNested = isNested(begin, end, beginLast2, endLast2);
        if (isNested)
        {
          // this is tricky; making sure we properly get the
          // nests that are to be removed from the top-level
          // unnested list assigned a container root, while all
          // top-level nests get the pointer to unnested.

          pt = ptLastNot2;
          isNested = (pt == root || isNested(begin, end,
                  intervals[pt].getBegin(), intervals[pt].getEnd()));
          if (!isNested)
          {
            myContainer[i] = root;
          }
        }
      }
      else
      {
        isNested = isNested(begin, end, beginLast, endLast);
      }

      // ...almost done...

      if (isNested)
      {
        myContainer[i] = pt;
      }
      else
      {

        // monotonic -- find the parent that is doing the nesting

        while ((pt = myContainer[pt]) < root)
        {
          if (isNested(begin, end, intervals[pt].getBegin(),
                  intervals[pt].getEnd()))
          {
            myContainer[i] = pt;
            // fully contained by a previous interval
            // System.out.println("mycontainer " + i + " = " + pt);
            break;
          }
        }
      }

      // update the counts and pointers

      counts[myContainer[i]]++;
      if (myContainer[i] == unnested)
      {
        endLast2 = end;
        beginLast2 = begin;
      }
      else
      {
        ptLastNot2 = i;
        endLast = end;
        beginLast = begin;
      }
    }

    // System.out.println("intervals " + Arrays.toString(intervals));
    // System.out.println("conts " + Arrays.toString(myContainer));
    // System.out.println("counts " + Arrays.toString(counts));

    // Phase Two: Construct the nests[] array and its associated
    // starting pointer array and nest element counts. These counts
    // are actually produced above, but we reconstruct it as a set
    // of dynamic pointers during construction.
    
    // The reason this is two phases is that only now do we know where to start
    // the nested set. If we did not unnest, we could do this all in one pass
    // using a reverse sort for the root, and then just reverse that, but with
    // unnesting, we have two unknown starts, and that doesn't give us that
    // opportunity.

    /**
     * this temporary array tracks the pointer within nestOffsets to the nest
     * block offset for intervals[i]'s container.
     */
    int[] startPt = new int[len];

    startPt[root] = root;

    int nextStart = counts[root];
    
    if (unnested > 0)
    {
      nestOffsets[root] = counts[unnested];
      nextStart += counts[unnested];
      startPt[unnested] = unnested;
    }

    // Now get all the pointers right and set the nests[] pointer into intervals
    // correctly.

    for (int i = 0; i < intervalCount; i++)
    {
      int ptOffset = startPt[myContainer[i]];
      int p = nestOffsets[ptOffset] + nestLengths[ptOffset]++;
      nests[p] = intervals[i];
      if (counts[i] > 0)
      {
        // this is a container
        startPt[i] = p;
        nestOffsets[p] = nextStart;
        nextStart += counts[i];
      }
    }

    // System.out.println("intervals " + Arrays.toString(intervals));
    // System.out.println("nests " + Arrays.toString(nests));
    // System.out.println("conts " + Arrays.toString(myContainer));
    // System.out.println("offsets " + Arrays.toString(nestOffsets));
    // System.out.println("lengths " + Arrays.toString(nestLengths));
    // System.out.println(
    // "done " + nestLengths[root]
    // + (unnested > 0 ? " " + nestLengths[unnested] : ""));
  }

  /**
   * Child-Parent relationships to match IntervalStoreJ. Perhaps a bit arcane?
   * Objective is to minimize the depth when we can.
   * 
   * @param childStart
   * @param childEnd
   * @param parentStart
   * @param parentEnd
   * @return
   */
  private static boolean isNested(int childStart, int childEnd,
          int parentStart, int parentEnd)
  {
    return (parentStart <= childStart && parentEnd > childEnd
            || parentStart < childStart && parentEnd == childEnd);
  }

  /**
   * Just a couple of pointers to help speed findOverlaps along a bit.
   * 
   */
  private void updateMinMaxStart()
  {
    if (intervalCount > 0)
    {
      minStart = intervals[0].getBegin();
      maxStart = intervals[intervalCount - 1].getBegin();
    }
    else
    {
      minStart = Integer.MAX_VALUE;
      maxStart = Integer.MIN_VALUE;
    }
  }

  @Override
  public String toString()
  {
    return prettyPrint();
  }


}
