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
package intervalstore.impl0;

import java.util.List;

import intervalstore.api.IntervalI;

/**
 * Provides a method to perform binary search of an ordered list of
 * {@code IntervalI} objects for the first entry that satisfies a supplied
 * condition on either the begin or end value of the intervals
 * 
 * @author gmcarstairs
 */
public final class BinarySearcher
{
  /*
   * compare using <, <=, =, >=, >
   */
  public enum Compare
  {
    LT, LE, EQ, GE, GT
  }

  private BinarySearcher()
  {
  }

  /**
   * Performs a binary search of the list to find the index of the first entry
   * for which the test returns true. Answers the length of the list if there is
   * no such entry.
   * <p>
   * For correct behaviour, the provided list must be ordered consistent with
   * the test, that is, any entries returning false must precede any entries
   * returning true. Note that this means that this method is <em>not</em>
   * usable to search for equality (to find a specific entry), as all unequal
   * entries will answer false to the test. To do that, use
   * <code>Collections.binarySearch</code> instead.
   * 
   * @param list
   *          the list to be searched
   * @param compareValue
   *          a function that extracts the value to be compared from an entry
   * @param compareTo
   *          the value to compare to
   * @param test
   *          a function that compares (compareValue, compareTo) and returns
   *          true or false
   * @return
   * @see java.util.Collections#binarySearch(List, Object)
   */
  public static int findFirst(List<? extends IntervalI> list,
          boolean compareBegin,
          Compare comp, int compareto)
  {
    int start = 0;
    int matched = list.size();
    int end = matched - 1;
  
    /*
     * shortcut for the case that the last entry in the list
     * fails the test (this arises when adding non-overlapping
     * intervals in ascending start position order) 
     */
    if (end > 0)
    {
      IntervalI last = list.get(end);
      if (!compare(last, compareBegin, comp, compareto))
      {
        return matched;
      }
    }

    while (start <= end)
    {
      int mid = (start + end) / 2;
      IntervalI entry = list.get(mid);
      if (compare(entry, compareBegin, comp, compareto))
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
   * Applies the comparison specified by {@code comp} to either the
   * {@code begin} value of {@code entry} (if {@code compareBegin} is true) or
   * the {@code end} value, and the {@code compareTo} value, and returns the
   * result of the comparison
   * 
   * @param entry
   * @param compareBegin
   * @param comp
   * @param compareTo
   * @return
   */
  private static boolean compare(IntervalI entry, boolean compareBegin,
          Compare comp, int compareTo)
  {
    int val = compareBegin ? entry.getBegin() : entry.getEnd();
    boolean result;
    switch (comp)
    {
    case LT:
      result = val < compareTo;
      break;
    case LE:
      result = val <= compareTo;
      break;
    case EQ:
      result = val == compareTo;
      break;
    case GE:
      result = val >= compareTo;
      break;
    case GT:
    default:
      result = val > compareTo;
    }
    return result;
  }
}
