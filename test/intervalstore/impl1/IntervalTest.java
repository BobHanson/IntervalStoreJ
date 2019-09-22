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

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

import intervalstore.api.IntervalI;

public class IntervalTest
{
  @Test(groups="Functional")
  public void testContainsInterval()
  {
    IntervalI i1 = new Range(10, 20);

    assertTrue(i1.containsInterval(i1));
    assertTrue(i1.containsInterval(new Range(10, 19)));
    assertTrue(i1.containsInterval(new Range(11, 20)));
    assertTrue(i1.containsInterval(new Range(15, 16)));

    assertFalse(i1.containsInterval(null));
    assertFalse(i1.containsInterval(new Range(9, 10)));
    assertFalse(i1.containsInterval(new Range(8, 9)));
    assertFalse(i1.containsInterval(new Range(20, 21)));
    assertFalse(i1.containsInterval(new Range(23, 24)));
    assertFalse(i1.containsInterval(new Range(1, 100)));
  }

  @Test(groups = "Functional")
  public void testProperlyContainsInterval()
  {
    IntervalI i1 = new Range(10, 20);

    assertTrue(i1.properlyContainsInterval(new Range(10, 19)));
    assertTrue(i1.properlyContainsInterval(new Range(11, 20)));
    assertTrue(i1.properlyContainsInterval(new Range(15, 16)));

    assertFalse(i1.properlyContainsInterval(null));
    assertFalse(i1.properlyContainsInterval(i1));
    assertFalse(i1.properlyContainsInterval(new Range(9, 10)));
    assertFalse(i1.properlyContainsInterval(new Range(8, 9)));
    assertFalse(i1.properlyContainsInterval(new Range(20, 21)));
    assertFalse(i1.properlyContainsInterval(new Range(23, 24)));
    assertFalse(i1.properlyContainsInterval(new Range(1, 100)));
  }

  @Test(groups = "Functional")
  public void testEqualsInterval()
  {
    IntervalI i1 = new Range(10, 20);
    assertTrue(i1.equalsInterval(i1));
    assertTrue(i1.equalsInterval(new Range(10, 20)));

    assertFalse(i1.equalsInterval(new Range(10, 21)));
    assertFalse(i1.equalsInterval(null));
  }

  @Test(groups = "Functional")
  public void testOverlapsInterval()
  {
    IntervalI i1 = new Range(10, 20);
    assertTrue(i1.overlapsInterval(i1));
    assertTrue(i1.overlapsInterval(new Range(5, 10)));
    assertTrue(i1.overlapsInterval(new Range(5, 15)));
    assertTrue(i1.overlapsInterval(new Range(12, 18)));
    assertTrue(i1.overlapsInterval(new Range(15, 30)));
    assertTrue(i1.overlapsInterval(new Range(20, 30)));
    assertTrue(i1.overlapsInterval(new Range(1, 100)));

    assertFalse(i1.overlapsInterval(null));
    assertFalse(i1.overlapsInterval(new Range(1, 9)));
    assertFalse(i1.overlapsInterval(new Range(21, 21)));
  }
}
