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

/**
 * An interface describing a store of (possibly overlapping) features which may
 * be queried to find features which overlap a given start-end range
 * 
 * @author gmcarstairs
 *
 * @param <T>
 */
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
   * Returns a (possibly empty) list of items whose extent overlaps the given
   * range. If the {@code results} parameter is not null, items are appended to
   * this list and the (possibly extended) list is returned. There is no check
   * for duplicate entries in the result list.
   * 
   * @param from
   *          start of overlap range (inclusive)
   * @param to
   *          end of overlap range (inclusive)
   * @param result
   * @return
   */
  List<T> findOverlaps(long from, long to, List<T> result);

  /**
   * Adds the entry to the store, unless {@code allowDuplicates} is false and
   * the entry is already contained in the store. The test for containment
   * should match that used in the {@code contains} method.
   */
  boolean add(T entry, boolean allowDuplicates);

  /**
   * Returns a string representation of the data where containment is shown by
   * indentation on new lines
   * 
   * @return
   */
  String prettyPrint();

  /**
   * Answers the depth of nesting of intervals in the store. The precise
   * definition depends on the implementation.
   * 
   * @return
   */
  int getDepth();

}