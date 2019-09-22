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

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import intervalstore.api.IntervalI;
import intervalstore.api.IntervalStoreI;

/**
 * A collection class to store interval-associated data, with O(log N)
 * performance for overlap queries, insertion and deletion (where N is the size
 * of the store). Accepts duplicate entries but not null values.
 * 
 * @author gmcarstairs
 *
 * @param <T>
 *          any type providing <code>getBegin()</code> and <code>getEnd()</code>
 */
public class IntervalStore<T extends IntervalI>
        extends AbstractCollection<T> implements IntervalStoreI<T>
{
  /**
   * An iterator over the intervals held in this store, with no particular
   * ordering guaranteed. The iterator does not support the optional
   * <code>remove</code> operation (throws
   * <code>UnsupportedOperationException</code> if attempted).
   * 
   * @author gmcarstairs
   *
   * @param <V>
   */
  private class IntervalIterator<V extends IntervalI> implements Iterator<V>
  {
    /*
     * iterator over top level non-nested intervals
     */
    Iterator<? extends IntervalI> topLevelIterator;

    /*
     * iterator over NCList (if any)
     */
    Iterator<? extends IntervalI> nestedIterator;

    /**
     * Constructor initialises iterators over the top level list and any nested
     * NCList
     * 
     * @param intervalStore
     */
    public IntervalIterator(
            IntervalStore<? extends IntervalI> intervalStore)
    {
      topLevelIterator = nonNested.iterator();
      if (nested != null)
      {
        nestedIterator = nested.iterator();
      }
    }

    @Override
    public boolean hasNext()
    {
      return topLevelIterator.hasNext() ? true
              : (nestedIterator != null && nestedIterator.hasNext());
    }

    @SuppressWarnings("unchecked")
    @Override
    public V next()
    {
      if (topLevelIterator.hasNext())
      {
        return (V) topLevelIterator.next();
      }
      if (nestedIterator != null)
      {
        return (V) nestedIterator.next();
      }
      throw new NoSuchElementException();
    }

  }

  private List<T> nonNested;

  private NCList<T> nested;

  /**
   * Constructor
   */
  public IntervalStore()
  {
    nonNested = new ArrayList<>();
  }

  /**
   * Constructor given a list of intervals. Note that the list may get sorted as
   * a side-effect of calling this constructor.
   */
  public IntervalStore(List<T> intervals)
  {
    this();

    /*
     * partition into subranges whose root intervals
     * have no mutual containment (if no intervals are nested,
     * each subrange is of length 1 i.e. a single interval)
     */
    List<IntervalI> sublists = new NCListBuilder<T>()
            .partitionNestedSublists(intervals);

    /*
     * add all 'subrange root intervals' (and any co-located intervals)
     * to our top level list of 'non-nested' intervals; 
     * put aside any left over for our NCList
     */
    List<T> nested = new ArrayList<>();

    for (IntervalI subrange : sublists)
    {
      int listIndex = subrange.getBegin();
      IntervalI root = intervals.get(listIndex);
      while (listIndex <= subrange.getEnd())
      {
        T t = intervals.get(listIndex);
        if (root.equalsInterval(t))
        {
          nonNested.add(t);
        }
        else
        {
          nested.add(t);
        }
        listIndex++;
      }
    }

    if (!nested.isEmpty())
    {
      this.nested = new NCList<>(nested);
    }
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
    if (!addNonNestedInterval(interval))
    {
      /*
       * detected a nested interval - put it in the NCList structure
       */
      addNestedInterval(interval);
    }
    return true;
  }

  @Override
  public boolean contains(Object entry)
  {
    if (listContains(nonNested, entry))
    {
      return true;
    }

    return nested == null ? false : nested.contains(entry);
  }

  protected boolean addNonNestedInterval(T entry)
  {
    synchronized (nonNested)
    {
      /*
       * find the first stored interval which doesn't precede the new one
       */
      int insertPosition = BinarySearcher.findFirst(nonNested,
              entry.getBegin(),
              BinarySearcher.fbegin);
      /*
       * fail if we detect interval enclosure 
       * - of the new interval by the one before or after it
       * - of the next interval by the new one
       */
      if (insertPosition > 0)
      {
        if (nonNested.get(insertPosition - 1)
                .properlyContainsInterval(entry))
        {
          return false;
        }
      }
      if (insertPosition < nonNested.size())
      {
        T following = nonNested.get(insertPosition);
        if (entry.properlyContainsInterval(following)
                || following.properlyContainsInterval(entry))
        {
          return false;
        }
      }

      /*
       * checks passed - add the interval
       */
      nonNested.add(insertPosition, entry);

      return true;
    }
  }

  @Override
  public List<T> findOverlaps(long from, long to)
  {
    List<T> result = new ArrayList<>();

    findNonNestedOverlaps(from, to, result);

    if (nested != null)
    {
      nested.findOverlaps(from, to, result);
    }

    return result;
  }

  @Override
  public String prettyPrint()
  {
    String pp = nonNested.toString();
    if (nested != null)
    {
      pp += '\n' + nested.prettyPrint();
    }
    return pp;
  }

  public boolean isValid()
  {
    for (int i = 0; i < nonNested.size() - 1; i++)
    {
      IntervalI i1 = nonNested.get(i);
      IntervalI i2 = nonNested.get(i + 1);

      if (i2.getBegin() < i1.getBegin())
      {
        System.err.println("nonNested wrong start order : " + i1.toString()
                + ", " + i2.toString());
        return false;
      }
      if (i1.properlyContainsInterval(i2)
              || i2.properlyContainsInterval(i1))
      {
        System.err.println("nonNested invalid containment!: "
                + i1.toString()
                + ", " + i2.toString());
        return false;
      }
    }
    return nested == null ? true : nested.isValid();
  }

  @Override
  public int size()
  {
    int i = nonNested.size();
    if (nested != null)
    {
      i += nested.size();
    }
    return i;
  }

  @Override
  public synchronized boolean remove(Object o)
  {
    if (o == null)
    {
      return false;
    }
    try
    {
      @SuppressWarnings("unchecked")
      T entry = (T) o;

      /*
       * try the non-nested positional intervals first
       */
      boolean removed = removeNonNested(entry);

      /*
       * if not found, try nested intervals
       */
      if (!removed && nested != null)
      {
        removed = nested.remove(entry);
      }

      return removed;
    } catch (ClassCastException e)
    {
      return false;
    }
  }

  /**
   * Removes the given entry from the list of non-nested entries, returning true
   * if found and removed, or false if not found. Specifically, removes the
   * first item in the list for which <code>item.equals(entry)</code>.
   * 
   * @param entry
   * @return
   */
  protected boolean removeNonNested(T entry)
  {
    /*
     * find the first interval that might match, i.e. whose 
     * start position is not less than the target range start
     * (NB inequality test ensures the first match if any is found)
     */
    int from = entry.getBegin();
    int startIndex = BinarySearcher.findFirst(nonNested, from,
            BinarySearcher.fbegin);

    /*
     * traverse intervals to look for a match
     */

    int i = startIndex;
    int size = nonNested.size();
    while (i < size)
    {
      T sf = nonNested.get(i);
      if (sf.getBegin() > from)
      {
        break;
      }
      if (sf.equals(entry))
      {
        nonNested.remove(i);
        return true;
      }
      i++;
    }
    return false;
  }

  @Override
  public int getDepth()
  {
    if (size() == 0)
    {
      return 0;
    }
    return (nonNested.isEmpty() ? 0 : 1)
            + (nested == null ? 0 : nested.getDepth());
  }

  /**
   * Adds one interval to the NCList that can manage nested intervals (creating
   * the NCList if necessary)
   */
  protected synchronized void addNestedInterval(T interval)
  {
    if (nested == null)
    {
      nested = new NCList<>();
    }
    nested.add(interval);
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
    if (intervals == null || entry == null || !(entry instanceof IntervalI))
    {
      return false;
    }

    IntervalI interval = (IntervalI) entry;

    /*
     * locate the first entry in the list which does not precede the interval
     */
    int from = interval.getBegin();
    int pos = BinarySearcher.findFirst(intervals, from,
            BinarySearcher.fbegin);
    int len = intervals.size();
    while (pos < len)
    {
      T sf = intervals.get(pos);
      if (sf.getBegin() > from)
      {
        return false; // no match found
      }
      if (sf.equals(interval))
      {
        return true;
      }
      pos++;
    }
    return false;
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
    return new IntervalIterator<>(this);
  }

  @Override
  public void clear()
  {
    this.nonNested.clear();
    this.nested = new NCList<>();
  }

  /**
   * Adds non-nested intervals to the result list that lie within the target
   * range
   * 
   * @param from
   * @param to
   * @param result
   */
  protected void findNonNestedOverlaps(long from, long to,
          List<T> result)
  {
    /*
     * find the first interval whose end position is
     * after the target range start
     */
    int startIndex = BinarySearcher.findFirst(nonNested, (int) from,
            BinarySearcher.fend);
    for (int i = startIndex, n = nonNested.size(); i < n; i++)
    {
      T sf = nonNested.get(i);
      if (sf.getBegin() > to)
      {
        break;
      }
      if (sf.getEnd() >= from)
      {
        result.add(sf);
      }
    }
  }

  @Override
  public String toString()
  {
    String s = nonNested.toString();
    if (nested != null)
    {
      s = s + '\n'// + System.lineSeparator()
              + nested.toString();
    }
    return s;
  }

  @Override
  public List<T> findOverlaps(long start, long end, List<T> result)
  {
    return findOverlaps(start, end);
  }

  @Override
  public boolean add(T interval, boolean checkForDuplicate)
  {
    return add(interval);
  }
}
