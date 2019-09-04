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
package intervalstore.impl;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import intervalstore.api.IntervalI;

/**
 * Each node of the NCList tree consists of a range, and (optionally) the NCList
 * of ranges it encloses
 *
 * @param <T>
 */
class NCNode<T extends IntervalI> implements IntervalI
{
  /**
   * A depth-first iterator over the intervals stored in the NCNode. The
   * optional <code>remove</code> operation is not supported.
   * 
   * @author gmcarstairs
   *
   */
  private class NCNodeIterator implements Iterator<T>
  {
    boolean first = true;
    Iterator<T> subregionIterator;

    @Override
    public boolean hasNext()
    {
      return first
              || (subregionIterator != null && subregionIterator.hasNext());
    }

    /**
     * Answers the next interval - initially the top level interval for this
     * node, thereafter the intervals returned by the NCList's iterator
     */
    @Override
    public T next()
    {
      if (first)
      {
        subregionIterator = subregions == null ? null
                : subregions.iterator();
        first = false;
        return region;
      }
      if (subregionIterator == null || !subregionIterator.hasNext())
      {
        throw new NoSuchElementException();
      }
      return subregionIterator.next();
    }
  }

  private T region;

  /*
   * null, or an object holding contained subregions of this nodes region
   */
  private NCList<T> subregions;

  /**
   * Constructor given a list of ranges. The list not be empty, and should be
   * ordered so that the first range contains all the others. If not, behaviour
   * will likely be invalid.
   * 
   * @param ranges
   * @throws IllegalArgumentException
   *           if the list is empty
   */
  NCNode(List<T> ranges)
  {
    if (ranges.isEmpty())
    {
      throw new IllegalArgumentException("List may not be empty");
    }
    region = ranges.get(0);

    if (ranges.size() > 1)
    {
      subregions = new NCList<>(ranges.subList(1, ranges.size()));
    }
  }

  /**
   * Constructor given a single range
   * 
   * @param range
   */
  NCNode(T range)
  {
    region = range;
  }

  @Override
  public int getBegin()
  {
    return region.getBegin();
  }

  @Override
  public int getEnd()
  {
    return region.getEnd();
  }

  /**
   * Formats the node as a bracketed list e.g.
   * 
   * <pre>
   * [1-100 [10-30 [10-20]], 15-30 [20-20]]
   * </pre>
   * 
   * where the format for each interval is as given by <code>T.toString()</code>
   */
  @Override
  public String toString()
  {
    StringBuilder sb = new StringBuilder(10 * size());
    sb.append(region.toString());
    if (subregions != null)
    {
      sb.append(" ").append(subregions.toString());
    }
    return sb.toString();
  }

  void prettyPrint(StringBuilder sb, int offset, int indent)
  {
    for (int i = 0; i < offset; i++)
    {
      sb.append(" ");
    }
    sb.append(region.toString());
    if (subregions != null)
    {
      sb.append('\n');// System.lineSeparator());
      subregions.prettyPrint(sb, offset + 2, indent);
    }
  }

  /**
   * Add any ranges that overlap the from-to range to the result list
   * 
   * @param from
   * @param to
   * @param result
   */
  void findOverlaps(long from, long to, List<T> result)
  {
    if (region.getBegin() <= to && region.getEnd() >= from)
    {
      result.add(region);
      if (subregions != null)
      {
        subregions.findOverlaps(from, to, result);
      }
    }
  }

  /**
   * Add one node to this node's subregions.
   * 
   * @param entry
   * @throws IllegalArgumentException
   *           if the added node is not contained by the node's start-end range
   */
  synchronized void addNode(NCNode<T> entry)
  {
    if (!region.containsInterval(entry))
    {
      throw new IllegalArgumentException(
              String.format("adding improper subrange %d-%d to range %d-%d",
                      entry.getBegin(), entry.getEnd(), region.getBegin(),
                      region.getEnd()));
    }
    if (subregions == null)
    {
      subregions = new NCList<>();
    }

    subregions.addNode(entry);
  }

  /**
   * Answers true if the data held satisfy the rules of construction of an
   * NCList, else false
   * 
   * @return
   */
  boolean isValid()
  {
    /*
     * we don't handle reverse ranges
     */
    if (region != null && region.getBegin() > region.getEnd())
    {
      return false;
    }
    if (subregions == null)
    {
      return true;
    }
    if (subregions.isEmpty())
    {
      /*
       * we expect empty subregions to be nulled
       */
      return false;
    }
    return subregions.isValid(getBegin(), getEnd());
  }

  /**
   * Adds all contained entries to the given list
   * 
   * @param entries
   */
  void getEntries(List<T> entries)
  {
    entries.add(region);
    if (subregions != null)
    {
      subregions.getEntries(entries);
    }
  }

  /**
   * Answers true if this object contains the given entry (by object equals
   * test), else false
   * 
   * @param entry
   * @return
   */
  boolean contains(IntervalI entry)
  {
    if (entry == null)
    {
      return false;
    }
    if (entry.equals(region))
    {
      return true;
    }
    return subregions == null ? false : subregions.contains(entry);
  }

  /**
   * Answers the 'root' region modelled by this object
   * 
   * @return
   */
  T getRegion()
  {
    return region;
  }

  /**
   * Answers the (possibly null) contained regions within this object
   * 
   * @return
   */
  NCList<T> getSubRegions()
  {
    return subregions;
  }

  /**
   * Answers the (deep) size of this node i.e. the number of intervals it models
   * 
   * @return
   */
  int size()
  {
    return subregions == null ? 1 : 1 + subregions.size();
  }

  /**
   * Answers the depth of NCNode / NCList nesting in the data tree
   * 
   * @return
   */
  int getDepth()
  {
    return subregions == null ? 1 : 1 + subregions.getDepth();
  }

  /**
   * Answers an iterator over the intervals stored in this node. The iterator
   * does not support the optional <code>remove</code> operation (throws
   * <code>UnsupportedOperationException</code> if attempted).
   * 
   * @return
   */
  public Iterator<T> iterator()
  {
    return new NCNodeIterator();
  }

  /**
   * Removes the first interval found equal to the given entry. Answers true if
   * a matching interval is found and removed, else false.
   * 
   * @param entry
   * @return
   */
  boolean remove(T entry)
  {
    if (region.equals(entry))
    {
      /*
       * this case must be handled by NCList, to allow any
       * children of a deleted interval to be promoted
       */
      throw new IllegalArgumentException("NCNode can't remove self");
    }
    if (subregions == null)
    {
      return false;
    }
    if (subregions.remove(entry))
    {
      if (subregions.isEmpty())
      {
        subregions = null;
      }
      return true;
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean equals(Object o)
  {
    return o != null && o instanceof NCNode
            && equalsInterval((NCNode<T>) o);
  }

  @Override
  public boolean equalsInterval(IntervalI i)
  {
    return i != null && getBegin() == i.getBegin()
            && getEnd() == i.getEnd();

  }

}
