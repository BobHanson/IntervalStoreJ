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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import intervalstore.impl.BinarySearcher.Compare;

public class BinarySearcherTest
{
  /**
   * Tests for the binary search for the first list element that satisfies a
   * given test
   */
  @Test(groups = "Functional")
  public void testFindFirst()
  {
    List<Range> ranges = Arrays.asList(new Range(1, 4), new Range(1, 4),
            new Range(3, 6), new Range(5, 10), new Range(8, 10));

    // first start following 1
    assertEquals(BinarySearcher.findFirst(ranges, true, Compare.GT, 1), 2);

    // first start >= 3
    assertEquals(BinarySearcher.findFirst(ranges, true, Compare.GE, 3), 2);

    // first start > 3
    assertEquals(BinarySearcher.findFirst(ranges, true, Compare.GT, 3), 3);

    // first start > 8
    assertEquals(BinarySearcher.findFirst(ranges, true, Compare.GT, 8),
            ranges.size());

    // first end > 0
    assertEquals(BinarySearcher.findFirst(ranges, false, Compare.GT, 0), 0);

    // first end >= 6
    assertEquals(BinarySearcher.findFirst(ranges, false, Compare.GE, 6), 2);

    // first end > 6
    assertEquals(BinarySearcher.findFirst(ranges, false, Compare.GT, 6), 3);

    // first end > 10
    assertEquals(BinarySearcher.findFirst(ranges, false, Compare.GT, 10),
            ranges.size());

    /*
     * 'test' including as a warning that this method should not be
     * used to find an entry - see the Javadoc for details!
     */
    int pos = BinarySearcher.findFirst(ranges, true, Compare.EQ, 1);
    assertNotEquals(pos, 0);
    assertEquals(pos, ranges.size());
  }
}
