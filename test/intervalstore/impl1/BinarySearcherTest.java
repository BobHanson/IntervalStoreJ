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

import org.testng.annotations.Test;

public class BinarySearcherTest
{
  /**
   * Tests for the binary search for the first list element that satisfies a
   * given test
   */
  @Test(groups = "Functional")
  public void testFindFirst()
  {
    // List<String> names = Arrays.asList("anne", "anne", "bob", "colin",
    // "sally");
    // // first value following "anne"
    // assertEquals(BinarySearcher.findFirst(names,
    // name -> "anne".compareTo(name) < 0), 2);
    // // first value equal to or following "bob"
    // assertEquals(BinarySearcher.findFirst(names,
    // name -> "bob".compareTo(name) <= 0), 2);
    // // first value following "bob"
    // assertEquals(BinarySearcher.findFirst(names,
    // name -> "bob".compareTo(name) < 0), 3);
    // // first value following "sally"
    // assertEquals(BinarySearcher.findFirst(names,
    // name -> "sally".compareTo(name) < 0), names.size());
    //
    // /*
    // * 'test' including as a warning that this method should not be
    // * used to find an entry - see the Javadoc for details!
    // */
    // int pos = BinarySearcher.findFirst(names, name -> "anne".equals(name));
    // assertNotEquals(pos, 0);
    // assertEquals(pos, names.size());
    //
    // List<Integer> nums = Arrays.asList(1, 1, 1, 1, 1, 2, 2, 2, 2, 2, 2, 2,
    // 2, 2, 3, 3, 3, 3);
    // // no value is larger than 3
    // assertEquals(BinarySearcher.findFirst(nums, num -> num > 3),
    // nums.size());
    // // entry 14 is first larger than 2
    // assertEquals(BinarySearcher.findFirst(nums, num -> num > 2), 14);
    // // entry 5 is first larger than 1
    // assertEquals(BinarySearcher.findFirst(nums, num -> num > 1), 5);
    // // entry 0 is first larger than 0
    // assertEquals(BinarySearcher.findFirst(nums, num -> num > 0), 0);
    //
    // /*
    // * 'test' included as a warning that Collections.binarySearch is not
    // * guaranteed to find the first matching entry, so is not an
    // * alternative to findFirst()
    // */
    // pos = Collections.binarySearch(nums, 2);
    // assertNotEquals(pos, 5);
    // assertEquals(pos, 8);
  }
}
