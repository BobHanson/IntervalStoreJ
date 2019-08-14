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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.testng.annotations.Test;

import intervalstore.impl.Range;

public class ISLinkBSTest1
{
  @Test(enabled = true) // groups = "functional")
  public void test1()
  {
    /*
     * no-arg constructor is not terribly interesting
     */

    Range r0a = new Range(3, 3);
    Range r0b = new Range(4, 4);
    Range r0c = new Range(5, 7);
    Range r1 = new Range(10, 80);
    Range r1a = new Range(10, 100);
    Range r1b = new Range(10, 100);
    Range r2 = new Range(20, 30);
    Range r3 = new Range(35, 40);

    Range r4 = new Range(50, 80);
    Range r4a = new Range(51, 51);
    Range r4b = new Range(52, 52);
    Range r5 = new Range(55, 60);
    Range r5b = new Range(56, 56);
    Range r6 = new Range(70, 120);
    Range r7 = new Range(78, 78);
    // add to a list in unsorted order so constructor has to sort
    List<Range> ranges = Arrays.asList(r0a, r0b, r0c, r1, r1a, r1b, r2, r3,
            r4,
            r4a, r4b,
            r5, r5b, r6, r7);
    IntervalStore0<Range> store = new IntervalStore0<>(ranges);
    System.out.println(store);

    
    checkInterval(store, 57, 128,
            new Range[]
            { r1, r1a, r1b, r4, r5, r6, r7 });

    checkInterval(store, 14, 41, new Range[] { r1, r1a, r1b, r2, r3 });

    checkInterval(store, 4, 5, new Range[] { r0b, r0c });

    checkInterval(store, 86, 113, new Range[] { r1a, r1b, r6 });



    checkInterval(store, 37, 37, new Range[] { r1, r1a, r1b, r3 });

    checkInterval(store, -114, -41, new Range[] {});


    checkInterval(store, 71, 113, new Range[] { r1, r1a, r1b, r4, r6, r7 });


    checkInterval(store, 52, 75,
            new Range[]
            { r1, r1a, r1b, r4, r4b, r5, r5b, r6 });
    checkInterval(store, 37, 75,
            new Range[]
            { r1, r1a, r1b, r3, r4, r4a, r4b, r5, r5b, r6 });

    checkInterval(store, 37, 37, new Range[] { r1, r1a, r1b, r3 });


    Random r = new Random();
    for (int i = 0; i < 1000; i++)
    {
      int from = r.nextInt(150) - 10;
      int to = r.nextInt(140);
      Range range = new Range(Math.min(from, to), Math.max(from, to));
      List<Range> list = new ArrayList<>();
      for (int ir = 0; ir < ranges.size(); ir++)
      {
        if (range.overlapsInterval(ranges.get(ir)))
        {
          list.add(ranges.get(ir));
        }
      }
      checkInterval(store, range.getBegin(), range.getEnd(),
              list.toArray(new Range[list.size()]));
    }

  }

  private void checkInterval(IntervalStore0<Range> store, int from, int to,
          Range[] target)
  {
    System.out.println("checking interval " + from + "-" + to);
    for (int i = 0; i < target.length; i++)
      System.out.println(">" + target[i]);
    List<Range> result = store.findOverlaps(from, to);
    for (int i = 0; i < result.size(); i++)
      System.out.println("<" + result.get(i));
    assertEquals(result.toArray(), target);
  }

}
