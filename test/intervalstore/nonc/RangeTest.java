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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.util.Comparator;

import org.testng.annotations.Test;

import intervalstore.api.IntervalI;
import intervalstore.impl.Range;

public class RangeTest
{

  @Test(groups = "Functional")
  public void testCompare()
  {
    Comparator<? super IntervalI> comp = IntervalI.COMPARATOR_BIGENDIAN;

    // same position, same length
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 20)), 0);

    // same position, len1 > len2
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 19)), -1);

    // same position, len1 < len2
    assertEquals(comp.compare(new Range(10, 20), new Range(10, 21)), 1);

    // pos1 > pos2
    assertEquals(comp.compare(new Range(11, 20), new Range(10, 20)), 1);

    // pos1 < pos2
    assertEquals(comp.compare(new Range(10, 20), new Range(11, 11)), -1);
    
    try {
      comp.compare(new Range(10, 20), null);
      fail("Expected exception");
    } catch (NullPointerException e)
    {
      // expected
    }

    try
    {
      comp.compare(null, new Range(10, 20));
      fail("Expected exception");
    } catch (NullPointerException e)
    {
      // expected
    }
  }
}
