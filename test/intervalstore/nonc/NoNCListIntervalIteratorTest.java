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
package intervalstore.nonc;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

public class NoNCListIntervalIteratorTest
{
  @Test(groups = "Functional")
  public void testNext()
  {
    IntervalStore<Range> store = new IntervalStore<>();

    Iterator<Range> it = store.iterator();
    assertFalse(it.hasNext());
    try
    {
      it.next();
      fail("expected exception");
    } catch (NoSuchElementException e)
    {
      // expected
    }

    Range range1 = new Range(11, 20);
    store.add(range1);
    it = store.iterator();
    assertTrue(it.hasNext());
    assertSame(range1, it.next());
    assertFalse(it.hasNext());

    Range range2 = new Range(4, 8);
    store.add(range2);
    Range range3 = new Range(40, 60);
    store.add(range3);
    it = store.iterator();
    assertSame(range2, it.next());
    assertSame(range1, it.next());
    assertSame(range3, it.next());
    assertFalse(it.hasNext());

    /*
     * add nested intervals
     */
    store.clear();
    store.add(range1);
    Range range4 = new Range(15, 18);
    store.add(range4);
    it = store.iterator();
    assertTrue(it.hasNext());
    assertSame(range1, it.next());
    assertSame(range4, it.next());
    assertFalse(it.hasNext());
  }
}
