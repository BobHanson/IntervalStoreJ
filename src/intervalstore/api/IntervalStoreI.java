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
package intervalstore.api;

import java.util.Collection;
import java.util.List;

import intervalstore.impl.NCList;

public interface IntervalStoreI<T extends IntervalI> extends Collection<T>
{

  /**
   * Returns a (possibly empty) list of items whose extent overlaps the given
   * range
   * 
   * @param from
   *          start of overlap range (inclusive)
   * @param to
   *          end of overlap range (inclusive)
   * @return
   */
  List<T> findOverlaps(long from, long to);

  /**
   * Ensures that the IntervalStore is ready for findOverlap.
   * 
   * @return true iff the data held satisfy the rules of construction of an
   *         IntervalStore.
   * 
   */
  boolean isValid();

  /**
   * Answers the level of nesting of intervals in the store, as
   * <ul>
   * <li>0 if the store is empty</li>
   * <li>1 if all intervals are 'top level' (non nested)</li>
   * <li>else 1 plus the depth of the enclosed NCList</li>
   * </ul>
   * 
   * @return
   * @see NCList#getDepth()
   */
  int getDepth();

  /**
   * Return the number of top-level (not-contained) intervals.
   * 
   * @return
   */
  int getWidth();

  List<T> findOverlaps(long start, long end, List<T> result);

  String prettyPrint();

  /**
   * Resort and rebuild links.
   * 
   * @return
   */
  boolean revalidate();

  IntervalI get(int i);

}