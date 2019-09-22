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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import intervalstore.api.IntervalI;

/**
 * An adapted implementation of NCList as described in the paper
 * 
 * <pre>
 * Nested Containment List (NCList): a new algorithm for accelerating
 * interval query of genome alignment and interval databases
 * - Alexander V. Alekseyenko, Christopher J. Lee
 * https://doi.org/10.1093/bioinformatics/btl647
 * </pre>
 */
public class NCList<T extends IntervalI> extends AbstractCollection<T>
{

  // private static final boolean OPTION_FIND_ANY = false;

  /**
   * A depth-first iterator over the elements stored in the NCList
   */
  private class NCListIterator implements Iterator<T>
  {
    int subrangeIndex;

    Iterator<T> nodeIterator;

    /**
     * Constructor bootstraps a pointer to an iterator over the first subrange
     * (if any)
     */
    NCListIterator()
    {
      subrangeIndex = nextSubrange(-1);
    }

    /**
     * Moves the subrange iterator to the next subrange, and answers its index
     * in the list of subranges. If there are no more, sets the iterator to null
     * and answers -1.
     * 
     * @return
     */
    private int nextSubrange(int after)
    {
      int nextIndex = after + 1;
      if (nextIndex < subranges.size())
      {
        nodeIterator = subranges.get(nextIndex).iterator();
        return nextIndex;
      }
      nodeIterator = null;
      return -1;
    }

    @Override
    public boolean hasNext()
    {
      return nodeIterator != null && nodeIterator.hasNext();
    }

    /**
     * Answers the next element returned by the current NCNode's iterator, and
     * advances the iterator (to the next NCNode if necessary)
     */
    @Override
    public T next()
    {
      if (nodeIterator == null || !nodeIterator.hasNext())
      {
        throw new NoSuchElementException();
      }
      T result = nodeIterator.next();

      if (!nodeIterator.hasNext())
      {
        subrangeIndex = nextSubrange(subrangeIndex);
      }
      return result;
    }

  }

  /*
   * the number of interval instances represented
   */
  private int size;

  /*
   * a list, in start position order, of sublists of ranges ordered so 
   * that each contains (or is the same as) the one that follows it
   */
  private List<NCNode<T>> subranges;

  /**
   * Constructor given a list of things that are each located on a contiguous
   * interval. Note that the constructor may reorder the list.
   * <p>
   * We assume here that for each range, start &lt;= end. Behaviour for reverse
   * ordered ranges is undefined.
   * 
   * @param ranges
   */
  public NCList(List<T> ranges)
  {
    this();
    build(ranges);
  }

  /**
   * Sorts and groups ranges into sublists where each sublist represents an
   * interval and its contained subintervals
   * 
   * @param ranges
   */
  protected void build(List<T> ranges)
  {
    /*
     * sort and partition into subranges 
     * which have no mutual containment
     */
    List<IntervalI> sublists = partitionNestedSublists(ranges);

    /*
     * convert each subrange to an NCNode consisting of a range and
     * (possibly) its contained NCList
     */
    for (IntervalI sublist : sublists)
    {
      subranges.add(new NCNode<>(
              ranges.subList(sublist.getBegin(), sublist.getEnd() + 1)));
    }

    size = ranges.size();
  }

  /**
   * Default constructor
   */
  public NCList()
  {
    subranges = new ArrayList<>();
  }

  /**
   * Sorts and traverses the ranges to identify sublists, whose start intervals
   * are overlapping or disjoint but not mutually contained. Answers a list of
   * start-end indices of the sorted list of ranges.
   * 
   * @param ranges
   * @return
   */
  protected List<IntervalI> partitionNestedSublists(List<T> ranges)
  {
    List<IntervalI> sublists = new ArrayList<>();

    if (ranges.isEmpty())
    {
      return sublists;
    }

    /*
     * sort by start ascending, length descending, so that
     * contained intervals follow their containing interval
     */
    Collections.sort(ranges, IntervalI.COMPARATOR_BIGENDIAN);

    int listStartIndex = 0;

    IntervalI lastParent = ranges.get(0);
    boolean first = true;

    for (int i = 0; i < ranges.size(); i++)
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

  /**
   * Adds one entry to the stored set
   * 
   * @param entry
   */
  @Override
  public synchronized boolean add(final T entry)
  {
    final NCNode<T> newNode = new NCNode<>(entry);
    addNode(newNode);
    return true;
  }

  /**
   * Adds one NCNode to this NCList
   * <p>
   * This method does not update the <code>size</code> (interval count) of this
   * NCList, as it may be used to rearrange nodes without changing their count.
   * Callers should increment the count if needed.
   * 
   * @param newNode
   */
  protected void addNode(final NCNode<T> newNode)
  {
    final long start = newNode.getBegin();
    final long end = newNode.getEnd();
    size += newNode.size();

    /*
     * cases:
     * 1) precedes all subranges - add as NCNode on front of list
     * 2) follows all subranges - add as NCNode on end of list
     * 3) matches a subrange - add as a sibling in the list
     * 4) properly enclosed by a subrange - add recursively to subrange
     * 5) properly encloses one or more subranges - push them inside it
     * 6) spans two subranges - insert between them
     */

    /*
     * find the first subrange whose end does not precede entry's start
     */
    int candidateIndex = findFirstOverlap(start);

    /*
     * search for maximal span of subranges i-k that the new entry
     * encloses; or a subrange that encloses the new entry
     */
    boolean enclosing = false;
    int firstEnclosed = 0;
    int lastEnclosed = 0;

    for (int j = candidateIndex; j < subranges.size(); j++)
    {
      NCNode<T> subrange = subranges.get(j);

      if (subrange.equalsInterval(newNode))
      {
        /*
         * matching interval - insert adjacent
         */
        subranges.add(j, newNode);
        return;
      }

      if (end < subrange.getBegin() && !enclosing)
      {
        /*
         * new entry lies between subranges j-1 j
         */
        subranges.add(j, newNode);
        return;
      }

      if (subrange.properlyContainsInterval(newNode))
      {
        /*
         * push new entry inside this subrange as it encloses it
         */
        subrange.addNode(newNode);
        return;
      }

      if (start <= subrange.getBegin())
      {
        if (end >= subrange.getEnd())
        {
          /*
           * new entry encloses this subrange (and possibly preceding ones);
           * continue to find the maximal list it encloses
           */
          if (!enclosing)
          {
            firstEnclosed = j;
          }
          lastEnclosed = j;
          enclosing = true;
          continue;
        }
        else
        {
          /*
           * entry spans from before this subrange to inside it
           */
          if (enclosing)
          {
            /*
             * entry encloses one or more preceding subranges
             */
            push(newNode, firstEnclosed, lastEnclosed);
          }
          else
          {
            /*
             * entry overlaps two subranges but doesn't enclose either
             * so just add it 
             */
            subranges.add(j, newNode);
          }
          return;
        }
      }
    }

    /*
     * drops through to here if new range encloses all others
     * or overlaps the last one
     */
    if (enclosing)
    {
      push(newNode, firstEnclosed, lastEnclosed);
    }
    else
    {
      subranges.add(newNode);
    }
  }

  @Override
  public boolean contains(Object entry)
  {
    if (!(entry instanceof IntervalI))
    {
      return false;
    }
    IntervalI interval = (IntervalI) entry;

    /*
     * find the first sublist that might overlap, i.e. 
     * the first whose end position is >= from
     */
    int candidateIndex = findFirstOverlap(interval.getBegin());

    int to = interval.getEnd();

    for (int i = candidateIndex; i < subranges.size(); i++)
    {
      NCNode<T> candidate = subranges.get(i);
      if (candidate.getBegin() > to)
      {
        /*
         * we are past the end of our target range
         */
        break;
      }
      if (candidate.contains(interval))
      {
        return true;
      }
    }
    return false;
  }

  /**
   * Update the tree so that the new node encloses current subranges i to j
   * (inclusive). That is, replace subranges i-j (inclusive) with a new subrange
   * that contains them.
   * 
   * @param node
   * @param i
   * @param j
   * @throws IllegalArgumentException
   *           if any of the subranges is not contained by the node's start-end
   *           range
   */
  protected synchronized void push(NCNode<T> node, final int i,
          final int j)
  {
    for (int k = i; k <= j; k++)
    {
      NCNode<T> n = subranges.get(k);
      if (!node.containsInterval(n)) {
        throw new IllegalArgumentException("Can't push " + n.toString()
                + " inside " + node.toString());
      }
      node.addNode(n);
    }

    for (int k = j; k >= i; k--)
    {
      subranges.remove(k);
    }
    subranges.add(i, node);
  }

  /**
   * Answers a list of contained intervals that overlap the given range
   * 
   * @param from
   * @param to
   * @return
   */
  public List<T> findOverlaps(long from, long to)
  {
    List<T> result = new ArrayList<>();

    findOverlaps(from, to, result);

    return result;
  }

  /**
   * Recursively searches the NCList adding any items that overlap the from-to
   * range to the result list
   * 
   * @param from
   * @param to
   * @param result
   */
  protected List<T> findOverlaps(long from, long to, List<T> result)
  {

    // if (OPTION_FIND_ANY)
    // {
    // return findAnyOverlaps(from, to, result);
    // }
    /*
     * find the first sublist that might overlap, i.e. 
     * the first whose end position is >= from
     */
    int candidateIndex = findFirstOverlap(from);

    for (int i = candidateIndex; i < subranges.size(); i++)
    {
      NCNode<T> candidate = subranges.get(i);
      if (candidate.getBegin() > to)
      {
        /*
         * we are past the end of our target range
         */
        break;
      }
      candidate.findOverlaps(from, to, result);
    }
    return result;
  }
  
  // /**
  // * Recursively searches the NCList adding any items that overlap the from-to
  // * range to the result list
  // *
  // * @param from
  // * @param to
  // * @param result
  // */
  // protected List<T> findAnyOverlaps(long from, long to, List<T> result)
  // {
  //
  // // BH find ANY overlap
  //
  // int candidateIndex = findAnyOverlap(subranges, from, to);
  //
  // if (candidateIndex < 0)
  // return result;
  // for (int i = candidateIndex, n = subranges.size(); i < n; i++)
  // {
  // NCNode<T> candidate = subranges.get(i);
  // if (candidate.getBegin() > to)
  // {
  // /*
  // * we are past the end of our target range
  // */
  // break;
  // }
  // candidate.findOverlaps(from, to, result);
  // }
  //
  // // BH adds dual-direction check
  //
  // for (int i = candidateIndex; --i >= 0;)
  // {
  // NCNode<T> candidate = subranges.get(i);
  // if (candidate.getEnd() < from)
  // {
  // break;
  // }
  // candidate.findOverlaps(from, to, result);
  // }
  // return result;
  // }
  //
  // private int findAnyOverlap(List<NCNode<T>> ranges, long from, long to)
  // {
  // int start = 0;
  // int end = ranges.size() - 1;
  // while (start <= end)
  // {
  // int mid = (start + end) >>> 1;
  // NCNode<T> r = ranges.get(mid);
  // if (r.getEnd() >= from)
  // {
  // if (r.getBegin() <= to)
  // return mid;
  // end = mid - 1;
  // }
  // else
  // {
  // start = mid + 1;
  // }
  // }
  // return -1;
  // }

  /**
   * Search subranges for the first one whose end position is not before the
   * target range's start position, i.e. the first one that may overlap the
   * target range. Returns the index in the list of the first such range found,
   * or the length of the list if none found.
   * 
   * @param from
   * @return
   */
  protected int findFirstOverlap(final long from)
  {
    return BinarySearcher.findFirst(subranges, (int) from,
            BinarySearcher.fend);
  }

  /**
   * Formats the tree as a bracketed list e.g.
   * 
   * <pre>
   * [1-100 [10-30 [10-20]], 15-30 [20-20]]
   * </pre>
   */
  @Override
  public String toString()
  {
    return subranges.toString();
  }

  /**
   * Answers the NCList as an indented list
   * 
   * @return
   */
  public String prettyPrint()
  {
    StringBuilder sb = new StringBuilder(512);
    int offset = 0;
    int indent = 2;
    prettyPrint(sb, offset, indent);
    sb.append('\n');// System.lineSeparator());
    return sb.toString();
  }

  /**
   * @param sb
   * @param offset
   * @param indent
   */
  void prettyPrint(StringBuilder sb, int offset, int indent)
  {
    boolean first = true;
    for (NCNode<T> subrange : subranges)
    {
      if (!first)
      {
        sb.append('\n');// System.lineSeparator());
      }
      first = false;
      subrange.prettyPrint(sb, offset, indent);
    }
  }

  /**
   * Answers true if the store's structure is valid (nesting containment rules
   * are obeyed), else false. For use in testing and debugging.
   * 
   * @return
   */
  public boolean isValid()
  {
    int count = 0;
    for (NCNode<T> subrange : subranges)
    {
      count += subrange.size();
    }
    if (count != size)
    {
      return false;
    }
    return isValid(Integer.MIN_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Answers true if the data held satisfy the rules of construction of an
   * NCList bounded within the given start-end range, else false.
   * <p>
   * Each subrange must lie within start-end (inclusive). Subranges must be
   * ordered by start position ascending, and within that by end position
   * descending.
   * <p>
   * 
   * @param start
   * @param end
   * @return
   */
  boolean isValid(final int start, final int end)
  {
    NCNode<T> lastRange = null;

    for (NCNode<T> subrange : subranges)
    {
      if (subrange.getBegin() < start)
      {
        System.err.println("error in NCList: range " + subrange.toString()
                + " starts before " + end);
        return false;
      }
      if (subrange.getEnd() > end)
      {
        System.err.println("error in NCList: range " + subrange.toString()
                + " ends after " + end);
        return false;
      }

      if (lastRange != null)
      {
        if (subrange.getBegin() < lastRange.getBegin())
        {
          System.err.println("error in NCList: range " + subrange.toString()
                  + " starts before " + lastRange.toString());
          return false;
        }
        if (subrange.properlyContainsInterval(lastRange))
        {
          System.err.println("error in NCList: range " + subrange.toString()
                  + " encloses preceding: " + lastRange.toString());
          return false;
        }
        if (lastRange.properlyContainsInterval(subrange))
        {
          System.err.println("error in NCList: range " + subrange.toString()
                  + " enclosed by preceding: " + lastRange.toString());
          return false;
        }
      }
      lastRange = subrange;

      if (!subrange.isValid())
      {
        return false;
      }
    }
    return true;
  }

  /**
   * Answers the number of intervals stored
   * 
   * @return
   */
  @Override
  public int size()
  {
    return size;
  }

  /**
   * Answers a list of all entries stored, in no guaranteed order. This method
   * is not synchronized, so is not thread-safe.
   */
  public List<T> getEntries()
  {
    List<T> result = new ArrayList<>();
    getEntries(result);
    return result;
  }

  /**
   * Adds all contained entries to the given list
   * 
   * @param result
   */
  void getEntries(List<T> result)
  {
    for (NCNode<T> subrange : subranges)
    {
      subrange.getEntries(result);
    }
  }

  /**
   * Removes the first interval <code>I</code>found that is equal to T
   * (<code>I.equals(T)</code>). Answers true if an interval is removed, false
   * if no match is found. This method is synchronized so thread-safe.
   * 
   * @param entry
   * @return
   */
  public synchronized boolean remove(T entry)
  {
    if (entry == null)
    {
      return false;
    }
    int i = findFirstOverlap(entry.getBegin());

    for (; i < subranges.size(); i++)
    {
      NCNode<T> subrange = subranges.get(i);
      if (subrange.getBegin() > entry.getBegin())
      {
        /*
         * not found
         */
        return false;
      }
      NCList<T> subRegions = subrange.getSubRegions();

      if (subrange.getRegion().equals(entry))
      {
        /*
         * if the subrange is rooted on this entry, remove it,
         * and remove and promote its subregions (if any)  
         */
        subranges.remove(i);
        size -= subrange.size();
        if (subRegions != null)
        {
          for (NCNode<T> r : subRegions.subranges)
          {
            addNode(r);
          }
        }
        return true;
      }
      else
      {
        if (subrange.remove(entry))
        {
          size--;
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Answers the depth of interval nesting of this object, where 1 means there
   * are no nested sub-intervals
   * 
   * @return
   */
  public int getDepth()
  {
    int subDepth = 0;
    for (NCNode<T> subrange : subranges)
    {
      subDepth = Math.max(subDepth, subrange.getDepth());
    }

    return subDepth;
  }

  /**
   * Answers an iterator over the contained intervals, with no particular order
   * guaranteed. The iterator does not support the optional <code>remove</code>
   * operation (throws <code>UnsupportedOperationException</code> if attempted).
   */
  @Override
  public Iterator<T> iterator()
  {
    return new NCListIterator();
  }

  @Override
  public synchronized void clear()
  {
    subranges.clear();
    size = 0;
  }

  public int getWidth()
  {
    return subranges.size();
  }
}
