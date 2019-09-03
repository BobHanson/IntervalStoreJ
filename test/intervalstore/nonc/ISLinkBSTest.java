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
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.testng.annotations.Test;

import intervalstore.impl.Range;

public class ISLinkBSTest
{

  @Test(enabled = true)
  public void test0()
  {

    /*
     * make some ranges including co-located, overlapping and nested
     */
    Range r1 = new Range(10, 20);
    Range r2 = new Range(10, 20);
    Range r3 = new Range(15, 22);
    Range r4 = new Range(20, 30);
    Range r5 = new Range(40, 40);
    Range r6 = new Range(40, 40);
    Range r7 = new Range(22, 28);
    Range r8 = new Range(22, 28);
    Range r9 = new Range(24, 26);
    Range r10 = new Range(10, 21);
    // add to a list in unsorted order so constructor has to sort
    List<Range> ranges = Arrays.asList(r6, r7, r1, r10, r4, r9, r3, r2, r8,
            r5);

    IntervalStore<Range> store2 = new IntervalStore<>(ranges);

    System.out.println(store2);

  }

  @Test(enabled = true) // groups = "functional")
  public void test1()
  {

    /*
     * no-arg constructor is not terribly interesting
     */

    Range r0a = new Range(5, 5);
    Range r0b = new Range(6, 8);
    Range r0c = new Range(5, 7);
    Range r0d = new Range(5, 8);
    Range r0e = new Range(5, 6);

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

    // check add not allowing duplicates
    IntervalStore<Range> store;

    store = new IntervalStore<>();
    Range r1080 = new Range(10, 80);
    Range r2070 = new Range(20, 70);
    Range r7275 = new Range(72, 75);
    Range r3090 = new Range(30, 90);
    Range r4060 = new Range(40, 60);
    Range r5099 = new Range(50, 99);
    store.add(r1080);
    store.add(r2070);
    store.add(r7275);
    store.add(r3090);
    store.add(r4060);
    store.add(r5099);

    System.out.println(store);

    store = new IntervalStore<>();

    assertTrue(store.add(r0e));
    assertFalse(store.add(r0e, false));

    System.out.println(store.binaryIdentitySearch(r0e));
    System.out.println(store.binaryIdentitySearch(r0b));
    System.out.println(store.binaryIdentitySearch(r0c));

    assertTrue(store.binaryIdentitySearch(r0e) == 0);
    assertTrue(store.binaryIdentitySearch(r0b) == -2);
    assertTrue(store.binaryIdentitySearch(r0c) == -1);

    assertTrue(store.add(r0e, true));
    assertTrue(store.add(r0e, true));
    assertTrue(store.add(r0e, true));

    System.out.println(store);
    System.out.println(store.binaryIdentitySearch(r0a));
    System.out.println(store.binaryIdentitySearch(r0e));
    System.out.println(store.binaryIdentitySearch(r0b));
    System.out.println(store.binaryIdentitySearch(r0c));
    assertTrue(store.binaryIdentitySearch(r0a) == -5);
    assertTrue(store.binaryIdentitySearch(r0b) == -5);
    assertTrue(store.binaryIdentitySearch(r0c) == -1);
    store.add(r0c);
    System.out.println(store);
    System.out.println(store.binaryIdentitySearch(r0d));
    System.out.println(store.binaryIdentitySearch(r0a));
    assertTrue(store.binaryIdentitySearch(r0d) == -1);
    assertTrue(store.binaryIdentitySearch(r0a) == -6);
    // edge case -- one SNP
    checkInterval(new IntervalStore<>(Arrays.asList(r0a)), 5, 5,
            new Range[]
            { r0a });
    checkInterval(new IntervalStore<>(Arrays.asList(r0a)), -1, 6,
            new Range[]
            { r0a });
    checkInterval(new IntervalStore<>(Arrays.asList(r0a)), -1, 4,
            new Range[] {});
    checkInterval(new IntervalStore<>(Arrays.asList(r0a)), 6, 6,
            new Range[] {});

    // edge case -- one interval
    checkInterval(new IntervalStore<>(Arrays.asList(r0b)), 6, 6,
            new Range[]
            { r0b });
    checkInterval(new IntervalStore<>(Arrays.asList(r0b)), 8, 8,
            new Range[]
            { r0b });
    checkInterval(new IntervalStore<>(Arrays.asList(r0b)), -1, 6,
            new Range[]
            { r0b });
    checkInterval(new IntervalStore<>(Arrays.asList(r0b)), -1, 9,
            new Range[]
            { r0b });

    // add to a list in unsorted order so constructor has to sort
    List<Range> ranges = new ArrayList<>(
            Arrays.asList(r0a, r0b, r1a, r1b, r1, r2, r3, r4,
            r4a, r4b,
                    r5, r5b, r6, r7));

    store = new IntervalStore<>(ranges);
    System.out.println(store);

    checkInterval(store, 5, 5, new Range[] { r0a });
    checkInterval(store, 6, 6, new Range[] { r0b });
    checkInterval(store, 8, 8, new Range[] { r0b });
    assertTrue(store.remove(r0b));
    System.out.println(store);
    assertTrue(store.remove(r0a));
    System.out.println(store);
    assertFalse(store.remove(r0b));

    store.add(r0a);
    store.add(r0b);

    System.out.println(store.toString());

    checkInterval(store, 2, 19, new Range[] { r0a, r0b, r1, r1a, r1b });

    checkInterval(store, 86, 113, new Range[] { r1a, r1b, r6 });

    checkInterval(store, 57, 128,
            new Range[]
            { r1a, r1b, r1, r4, r5, r6, r7 });

    checkInterval(store, 86, 113, new Range[] { r1a, r1b, r6 });

    checkInterval(store, 14, 41, new Range[] { r1a, r1b, r1, r2, r3 });

    checkInterval(store, 37, 37, new Range[] { r1a, r1b, r1, r3 });

    checkInterval(store, -114, -41, new Range[] {});


    checkInterval(store, 71, 113, new Range[] { r1a, r1b, r1, r4, r6, r7 });


    checkInterval(store, 52, 75,
            new Range[]
            { r1a, r1b, r1, r4, r4b, r5, r5b, r6 });
    checkInterval(store, 37, 75,
            new Range[]
            { r1a, r1b, r1, r3, r4, r4a, r4b, r5, r5b, r6 });

    checkInterval(store, 37, 37, new Range[] { r1a, r1b, r1, r3 });


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
    System.out.println(store);
    store.remove(r3);
    store.remove(r4);
    store.remove(r5);
    System.out.println(store);
    ranges.remove(r3);
    ranges.remove(r4);
    ranges.remove(r5);
    checkInterval(store, -1000, 1000, ranges.toArray(new Range[0]));

  }

  private void checkInterval(IntervalStore<Range> store, int from, int to,
          Range[] target)
  {
    System.out.println("checking interval " + from + "-" + to);
    for (int i = 0; i < target.length; i++)
      System.out.println(">" + target[i]);
    List<Range> result = store.findOverlaps(from, to);
    for (int i = 0; i < result.size(); i++)
      System.out.println("<" + result.get(i));
    assertTrue(result.size() == target.length);
    for (int i = 0; i < target.length; i++)
      assertTrue(result.contains(target[i]));
  }

}
