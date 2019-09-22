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

import java.util.List;
import java.util.function.ToIntFunction;

import intervalstore.api.IntervalI;

/**
 * Provides a method to perform binary search of an ordered list for the first
 * entry that satisfies a supplied condition
 * 
 * @author gmcarstairs
 */
public final class BinarySearcher
{

  public static ToIntFunction<IntervalI> fbegin = new ToIntFunction<IntervalI>()
  {

    @Override
    public int applyAsInt(IntervalI value)
    {
      return value.getBegin();
    }

  };

  public static ToIntFunction<IntervalI> fend = new ToIntFunction<IntervalI>()
  {

    @Override
    public int applyAsInt(IntervalI value)
    {
      return value.getEnd();
    }

  };

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
   * @param test
   * @return
   * @see java.util.Collections#binarySearch(List, Object)
   */
  public static <T> int findFirst(List<? extends T> list, int pos,
          ToIntFunction<T> test)
  {
    int start = 0;
    int end = list.size() - 1;
    int matched = list.size();
  
    while (start <= end)
    {
      int mid = (start + end) / 2;
      T entry = list.get(mid);
      boolean itsTrue = test.applyAsInt(entry) >= pos;
      if (itsTrue)
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
}
