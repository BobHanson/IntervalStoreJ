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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import intervalstore.impl.SimpleFeature;

public class IntervalITest
{
  SimpleFeature sf(int from, int to, String desc)
  {
    return new SimpleFeature(from, to, desc);
  }

  @Test(groups = "Functional")
  public void testEqualsInterval()
  {
    assertTrue(sf(30, 80, "Pfam").equalsInterval(sf(30, 80, "Pfam")));
    assertTrue(sf(30, 80, "Pfam").equalsInterval(sf(30, 80, "Cath")));
    assertFalse(sf(30, 80, "Pfam").equalsInterval(sf(30, 81, "Pfam")));
    assertFalse(sf(30, 81, "Pfam").equalsInterval(sf(30, 80, "Pfam")));
    assertFalse(sf(30, 80, "Pfam").equalsInterval(sf(29, 80, "Pfam")));
    assertFalse(sf(29, 80, "Pfam").equalsInterval(sf(30, 80, "Pfam")));
    assertFalse(sf(30, 80, "Pfam").equalsInterval(null));
  }

  @Test(groups = "Functional")
  public void testContainsInterval()
  {
    assertTrue(sf(30, 80, "Pfam").containsInterval(sf(30, 80, "Pfam")));
    assertTrue(sf(30, 80, "Pfam").containsInterval(sf(30, 79, "Pfam")));
    assertTrue(sf(30, 80, "Pfam").containsInterval(sf(31, 80, "Pfam")));
    assertTrue(sf(30, 80, "Pfam").containsInterval(sf(35, 40, "Cath")));
    assertFalse(sf(30, 80, "Pfam").containsInterval(sf(20, 40, "Pfam")));
    assertFalse(sf(30, 81, "Pfam").containsInterval(sf(40, 90, "Pfam")));
    assertFalse(sf(30, 80, "Pfam").containsInterval(null));
  }

  @Test(groups = "Functional")
  public void testProperlyContainsInterval()
  {
    assertFalse(sf(30, 80, "Pfam")
            .properlyContainsInterval(sf(30, 80, "Pfam")));
    assertTrue(sf(30, 80, "Pfam")
            .properlyContainsInterval(sf(30, 79, "Pfam")));
    assertTrue(sf(30, 80, "Pfam")
            .properlyContainsInterval(sf(31, 80, "Pfam")));
    assertTrue(sf(30, 80, "Pfam")
            .properlyContainsInterval(sf(35, 40, "Cath")));
    assertFalse(sf(30, 80, "Pfam")
            .properlyContainsInterval(sf(20, 40, "Pfam")));
    assertFalse(sf(30, 81, "Pfam")
            .properlyContainsInterval(sf(40, 90, "Pfam")));
    assertFalse(sf(30, 80, "Pfam").properlyContainsInterval(null));
  }

  @Test(groups = "Functional")
  public void testOverlapsInterval()
  {
    // same
    assertTrue(sf(30, 80, "Pfam").overlapsInterval(sf(30, 80, "Pfam")));
    // overlap right
    assertTrue(sf(30, 80, "Pfam").overlapsInterval(sf(80, 81, "Pfam")));
    // overlap left
    assertTrue(sf(30, 80, "Pfam").overlapsInterval(sf(21, 31, "Pfam")));
    // this encloses that
    assertTrue(sf(30, 80, "Pfam").overlapsInterval(sf(35, 40, "Cath")));
    // that encloses this
    assertTrue(sf(30, 80, "Pfam").overlapsInterval(sf(20, 90, "Pfam")));
    // disjoint
    assertFalse(sf(30, 81, "Pfam").overlapsInterval(sf(90, 100, "Pfam")));
    // null
    assertFalse(sf(30, 80, "Pfam").overlapsInterval(null));
  }
}
