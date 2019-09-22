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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

import intervalstore.api.IntervalI;
import junit.extensions.PA;

public class NCListTest
{
  /**
   * A basic sanity test of the constructor
   */
  @Test(groups = "Functional")
  public void testConstructor()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 20));
    ranges.add(new Range(10, 20));
    ranges.add(new Range(15, 30));
    ranges.add(new Range(10, 30));
    ranges.add(new Range(11, 19));
    ranges.add(new Range(10, 20));
    ranges.add(new Range(1, 100));

    NCList<Range> ncl = new NCList<>(ranges);
    String expected = "[1-100 [10-30 [10-20, 10-20 [11-19], 15-30 [20-20]]]]";
    assertEquals(ncl.toString(), expected);
    assertTrue(ncl.isValid());

    Collections.reverse(ranges);
    ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), expected);
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testFindOverlaps()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 50));
    ranges.add(new Range(30, 70));
    ranges.add(new Range(1, 100));
    ranges.add(new Range(70, 120));

    NCList<Range> ncl = new NCList<>(ranges);

    List<Range> overlaps = ncl.findOverlaps(121, 122);
    assertEquals(overlaps.size(), 0);

    overlaps = ncl.findOverlaps(21, 22);
    assertEquals(overlaps.size(), 2);
    assertEquals(((IntervalI) overlaps.get(0)).getBegin(), 1);
    assertEquals(((IntervalI) overlaps.get(0)).getEnd(), 100);
    assertEquals(((IntervalI) overlaps.get(1)).getBegin(), 20);
    assertEquals(((IntervalI) overlaps.get(1)).getEnd(), 50);

    overlaps = ncl.findOverlaps(110, 110);
    assertEquals(overlaps.size(), 1);
    assertEquals(((IntervalI) overlaps.get(0)).getBegin(), 70);
    assertEquals(((IntervalI) overlaps.get(0)).getEnd(), 120);
  }

  @Test(groups = "Functional")
  public void testAdd_onTheEnd()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-50]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(60, 70));
    assertEquals(ncl.toString(), "[20-50, 60-70]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testAdd_inside()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-50]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(30, 40));
    assertEquals(ncl.toString(), "[20-50 [30-40]]");
  }

  @Test(groups = "Functional")
  public void testAdd_onTheFront()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-50]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(5, 15));
    assertEquals(ncl.toString(), "[5-15, 20-50]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testAdd_enclosing()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 50));
    ranges.add(new Range(30, 60));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-50, 30-60]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(10, 70));
    assertEquals(ncl.toString(), "[10-70 [20-50, 30-60]]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testAdd_spanning()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 40));
    ranges.add(new Range(60, 70));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-40, 60-70]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(30, 50));
    assertEquals(ncl.toString(), "[20-40, 30-50, 60-70]");
    assertTrue(ncl.isValid());

    ncl.add(new Range(40, 65));
    assertEquals(ncl.toString(), "[20-40, 30-50, 40-65, 60-70]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testGetEntries()
  {
    List<Range> ranges = new ArrayList<>();
    Range r1 = new Range(20, 20);
    Range r2 = new Range(10, 20);
    Range r3 = new Range(15, 30);
    Range r4 = new Range(10, 30);
    Range r5 = new Range(11, 19);
    Range r6 = new Range(10, 20);
    ranges.add(r1);
    ranges.add(r2);
    ranges.add(r3);
    ranges.add(r4);
    ranges.add(r5);
    ranges.add(r6);

    NCList<Range> ncl = new NCList<>(ranges);
    Range r7 = new Range(1, 100);
    ncl.add(r7);

    List<Range> contents = ncl.getEntries();
    assertEquals(contents.size(), 7);
    assertTrue(contents.contains(r1));
    assertTrue(contents.contains(r2));
    assertTrue(contents.contains(r3));
    assertTrue(contents.contains(r4));
    assertTrue(contents.contains(r5));
    assertTrue(contents.contains(r6));
    assertTrue(contents.contains(r7));

    ncl = new NCList<>();
    assertTrue(ncl.getEntries().isEmpty());
  }

  @Test(groups = "Functional")
  public void testRemove()
  {
    List<Range> ranges = new ArrayList<>();
    Range r1 = new Range(20, 30);
    ranges.add(r1);
    NCList<Range> ncl = new NCList<>(ranges);
    assertTrue(ncl.contains(r1));

    Range r2 = new Range(20, 31);
    assertFalse(ncl.remove(null)); // null argument
    assertFalse(ncl.remove(r2)); // never added
    assertTrue(ncl.remove(r1)); // success
    assertTrue(ncl.getEntries().isEmpty());

    /*
     * tests where object.equals() == true
     */
    NCList<SimpleFeature> features = new NCList<>();
    SimpleFeature sf1 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "type");
    features.add(sf1);
    assertTrue(sf1.equals(sf2));
    assertTrue(features.remove(sf2)); // equal object deleted
    assertFalse(features.getEntries().contains(sf1)); // deleted
    assertTrue(features.getEntries().isEmpty()); // gone now

    /*
     * test with duplicate objects in NCList
     */
    features.add(sf1);
    features.add(sf1);
    assertEquals(features.getEntries().size(), 2);
    assertSame(features.getEntries().get(0), sf1);
    assertSame(features.getEntries().get(1), sf1);
    assertTrue(features.remove(sf1)); // first match only is deleted
    assertTrue(features.contains(sf1));
    assertEquals(features.size(), 1);
    assertTrue(features.remove(sf1));
    assertTrue(features.getEntries().isEmpty());
  }

  @Test(groups = "Functional")
  public void testAdd_overlapping()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(40, 50));
    ranges.add(new Range(20, 30));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-30, 40-50]");
    assertTrue(ncl.isValid());

    /*
     * add range overlapping internally
     */
    ncl.add(new Range(25, 35));
    assertEquals(ncl.toString(), "[20-30, 25-35, 40-50]");
    assertTrue(ncl.isValid());

    /*
     * add range overlapping last range
     */
    ncl.add(new Range(45, 55));
    assertEquals(ncl.toString(), "[20-30, 25-35, 40-50, 45-55]");
    assertTrue(ncl.isValid());

    /*
     * add range overlapping first range
     */
    ncl.add(new Range(15, 25));
    assertEquals(ncl.toString(), "[15-25, 20-30, 25-35, 40-50, 45-55]");
    assertTrue(ncl.isValid());
  }

  /**
   * Test the contains method (which uses object equals test)
   */
  @Test(groups = "Functional")
  public void testContains()
  {
    NCList<SimpleFeature> ncl = new NCList<>();

    assertFalse(ncl.contains(null));
    assertFalse(ncl.contains("xyz"));

    SimpleFeature sf1 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "type");
    SimpleFeature sf3 = new SimpleFeature(1, 10, "type2");
    ncl.add(sf1);

    assertTrue(ncl.contains(sf1));
    assertTrue(ncl.contains(sf2)); // sf1.equals(sf2)
    assertFalse(ncl.contains(sf3)); // !sf1.equals(sf3)

    /*
     * make some deeper structure in the NCList
     */
    SimpleFeature sf4 = new SimpleFeature(2, 9, "type");
    ncl.add(sf4);
    assertTrue(ncl.contains(sf4));
    SimpleFeature sf5 = new SimpleFeature(4, 5, "type");
    SimpleFeature sf6 = new SimpleFeature(6, 8, "type");
    ncl.add(sf5);
    ncl.add(sf6);
    assertTrue(ncl.contains(sf5));
    assertTrue(ncl.contains(sf6));
  }

  @Test(groups = "Functional")
  public void testIsValid()
  {
    List<Range> ranges = new ArrayList<>();
    Range r1 = new Range(40, 50);
    ranges.add(r1);
    NCList<Range> ncl = new NCList<>(ranges);
    assertTrue(ncl.isValid());

    Range r2 = new Range(42, 44);
    ncl.add(r2);
    assertTrue(ncl.isValid());
    Range r3 = new Range(46, 48);
    ncl.add(r3);
    assertTrue(ncl.isValid());
    Range r4 = new Range(43, 43);
    ncl.add(r4);
    Range r5 = new Range(47, 49);
    ncl.add(r5);
    assertTrue(ncl.isValid());

    assertEquals(ncl.toString(), "[40-50 [42-44 [43-43], 46-48, 47-49]]");
    assertTrue(ncl.isValid());

    /*
     * use PrivilegedAccessor to force invalid values, as
     * the public API should not allow construction of an invalid NCList
     */
    int size = ncl.size();
    PA.setValue(ncl, "size", size + 1);
    assertFalse(ncl.isValid());
    PA.setValue(ncl, "size", size);
    assertTrue(ncl.isValid());

    PA.setValue(r1, "start", 43);
    assertFalse(ncl.isValid()); // r2 not inside r1 (starts before)
    PA.setValue(r1, "start", 40);
    assertTrue(ncl.isValid());

    PA.setValue(r2, "end", 51);
    assertFalse(ncl.isValid()); // r2 not inside r1 (ends after)
    PA.setValue(r2, "end", 44);
    assertTrue(ncl.isValid());

    PA.setValue(r3, "start", 41);
    assertFalse(ncl.isValid()); // r3 should precede r2
    PA.setValue(r3, "start", 46);
    assertTrue(ncl.isValid());

    PA.setValue(r4, "start", 41);
    assertFalse(ncl.isValid()); // r4 not inside r2
    PA.setValue(r4, "start", 43);
    assertTrue(ncl.isValid());

    PA.setValue(r4, "start", 44);
    assertFalse(ncl.isValid()); // r4 has reverse range
    PA.setValue(r4, "start", 43);
    assertTrue(ncl.isValid());

    PA.setValue(r3, "start", 47);
    assertFalse(ncl.isValid()); // r3 enclosed by following r5
    PA.setValue(r3, "start", 46);
    assertTrue(ncl.isValid());

    PA.setValue(r5, "end", 48);
    assertFalse(ncl.isValid()); // r5 enclosed by preceding r3
    PA.setValue(r5, "end", 49);

  }

  @Test(groups = "Functional")
  public void testPrettyPrint()
  {
    /*
     * construct NCList from a list of ranges
     * they are sorted then assembled into NCList subregions
     * notice that 42-42 end up inside 41-46
     */
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(40, 50));
    ranges.add(new Range(45, 55));
    ranges.add(new Range(40, 45));
    ranges.add(new Range(41, 46));
    ranges.add(new Range(42, 42));
    ranges.add(new Range(42, 42));
    NCList<Range> ncl = new NCList<>(ranges);
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[40-50 [40-45, 41-46 [42-42, 42-42]], 45-55]");
    String expected = "40-50\n  40-45\n  41-46\n    42-42\n    42-42\n45-55\n";
    assertEquals(ncl.prettyPrint(), expected);

    /*
     * repeat but now add ranges one at a time
     * notice that 42-42 end up inside 40-50 so we get
     * a different but equally valid NCList structure
     */
    ranges.clear();
    ncl = new NCList<>(ranges);
    ncl.add(new Range(40, 50));
    ncl.add(new Range(45, 55));
    ncl.add(new Range(40, 45));
    ncl.add(new Range(41, 46));
    ncl.add(new Range(42, 42));
    ncl.add(new Range(42, 42)); // duplicates allowed
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[40-50 [40-45 [42-42, 42-42], 41-46], 45-55]");
    expected = "40-50\n  40-45\n    42-42\n    42-42\n  41-46\n45-55\n";
    assertEquals(ncl.prettyPrint(), expected);
  }

  /**
   * If intervals have an ambiguous order of containment, the NCList varies
   * depending on the order in which they are added to it
   */
  @Test(groups = "Functional")
  public void testLoadOrder()
  {
    List<SimpleFeature> ranges = new ArrayList<>();
    ranges.add(new SimpleFeature(1, 205602, "gene")); // ENSG00000157764
    ranges.add(new SimpleFeature(187, 90957, "transcript")); // ENST00000469930
    ranges.add(new SimpleFeature(187, 90957, "exon")); // ENSE00001154485
    ranges.add(new SimpleFeature(226, 90335, "cds")); // CDS:ENSP00000495858
    ranges.add(new SimpleFeature(251, 260, "indel")); // gnomAD VCF
    ranges.add(new SimpleFeature(256, 256, "SNV")); // dbSNP151 rs1225976306

    /*
     * add all features at once; initial sort and load results in a nested NCList
     */
    NCList<SimpleFeature> ncl = new NCList<>(ranges);
    assertEquals(ncl.size(), 6);
    assertEquals(ncl.getDepth(), 5);
    String asString = "[1:205602:gene [187:90957:transcript, 187:90957:exon [226:90335:cds [251:260:indel [256:256:SNV]]]]]";
    assertEquals(ncl.toString(), asString);

    /*
     * add features one at a time, in mixed order - same result as previous
     */
    ncl = new NCList<>();
    int[] order = new int[] { 3, 0, 5, 2, 4, 1 };
    for (int i : order)
    {
      ncl.add(ranges.get(i));
      assertTrue(ncl.isValid(), "iteration " + i);
    }
    assertEquals(ncl.toString(), asString);

    /*
     * add features one at a time, from 'small' to 'large' 
     * - same result apart from ordering of transcript / exon
     */
    ncl = new NCList<>();
    order = new int[] { 5, 4, 3, 2, 1, 0 };
    for (int i : order)
    {
      ncl.add(ranges.get(i));
    }
    String asString2 = "[1:205602:gene [187:90957:exon [226:90335:cds [251:260:indel [256:256:SNV]]], 187:90957:transcript]]";
    assertEquals(ncl.toString(), asString2);
    assertTrue(ncl.isValid());

    /*
     * add features one at a time, from 'large' to 'small'
     * - same result as previous
     */
    ncl = new NCList<>();
    order = new int[] { 0, 1, 2, 3, 4, 5 };
    for (int i : order)
    {
      ncl.add(ranges.get(i));
    }
    assertEquals(ncl.toString(), asString2);
    assertTrue(ncl.isValid());
  }

  /**
   * If intervals have alternative containment orderings, the NCList structure
   * may depend on the order in which they are added to it
   */
  @Test(groups = "Functional")
  public void testLoadOrder_alternates()
  {
    SimpleFeature domain1 = new SimpleFeature(100, 300, "domain1");
    SimpleFeature domain2 = new SimpleFeature(200, 400, "domain2");
    SimpleFeature domain3 = new SimpleFeature(240, 280, "domain3");
    SimpleFeature metal = new SimpleFeature(250, 250, "Iron-sulfur");
  
    NCList<SimpleFeature> ncl1 = new NCList<>();
    ncl1.add(domain1);
    ncl1.add(domain2);
    ncl1.add(domain3); // will be nested under domain1
    ncl1.add(metal); // will be nested under domain3
    assertTrue(ncl1.isValid());
    assertEquals(ncl1.toString(),
            "[100:300:domain1 [240:280:domain3 [250:250:Iron-sulfur]], 200:400:domain2]");
    assertEquals(ncl1.getDepth(), 3);

    /*
     * alternative order of construction
     */
    NCList<SimpleFeature> ncl3 = new NCList<>();
    ncl3.add(domain2);
    ncl3.add(metal); // will be nested under domain2
    ncl3.add(domain1);
    ncl3.add(domain3); // will be nested under domain1
    assertTrue(ncl3.isValid());
    assertEquals(ncl3.toString(),
            "[100:300:domain1 [240:280:domain3], 200:400:domain2 [250:250:Iron-sulfur]]");
    assertEquals(ncl3.getDepth(), 2);
    assertNotEquals(ncl1.toString(), ncl3.toString());

    /*
     * identical (or duplicated) intervals may end up on different branches:
     */
    NCList<SimpleFeature> ncl4 = new NCList<>();
    ncl4.add(domain2); // [200-400]
    ncl4.add(metal); // [250-250] nests in [200-400]
    ncl4.add(domain1); // [100-300]
    ncl4.add(metal); // [250-250] nested in [100-300]
    assertEquals(ncl4.toString(),
            "[100:300:domain1 [250:250:Iron-sulfur], 200:400:domain2 [250:250:Iron-sulfur]]");
  }

  @Test(groups = "Functional")
  public void testGetDepth()
  {
    NCList<Range> ncl = new NCList<>();
    assertEquals(ncl.getDepth(), 0);

    /*
     * add 3 ranges with no containment: flat NCList
     */
    ncl.add(new Range(10, 20));
    ncl.add(new Range(15, 25));
    ncl.add(new Range(30, 40));
    assertEquals(ncl.getDepth(), 1);

    /*
     * add some nested features at one level down
     */
    ncl.add(new Range(12, 18)); // within [10-20]
    ncl.add(new Range(15, 21)); // within [15-25]
    ncl.add(new Range(36, 40)); // within [30-40]
    assertEquals(ncl.getDepth(), 2);

    /*
     * add deeper nested features
     */
    ncl.add(new Range(13, 17)); // within [12-18]
    assertEquals(ncl.getDepth(), 3);
    ncl.add(new Range(13, 16)); // within [13-17]
    assertEquals(ncl.getDepth(), 4);
    ncl.add(new Range(13, 16)); // sibling to [13-16], duplicate
    assertEquals(ncl.getDepth(), 4);
  }

  /**
   * Demonstrates cases where load order affects tree balance / depth
   */
  @Test(groups = "Functional")
  public void testLoadOrder_treeBalance()
  {
    /*
     * make a set of pairs of highly overlapping ranges
     * - fromLeft: [1, N-1], [1, N-2], [1, N-3], ... 
     * - fromRight: [2, N], [2, N-1], [2, N-3], ...
     * ++++++++
     *  oooooooo
     *  ++++++
     *   oooooo
     *   ++++
     *    oooo
     *    ++
     *     oo
     */
    int N = 8;
    List<Range> fromLeft = new ArrayList<>();
    List<Range> fromRight = new ArrayList<>();
    int leftStart = 1;
    int leftEnd = N;
    while (leftStart <= leftEnd)
    {
      fromLeft.add(new Range(leftStart, leftEnd));
      fromRight.add(new Range(leftStart + 1, leftEnd + 1));
      leftStart++;
      leftEnd--;
    }
  
    /*
     * construct NCList with all intervals (sort and build)
     * results in a tree mostly nested under [2-9]
     */
    List<Range> all = new ArrayList<>(fromLeft);
    all.addAll(fromRight);
    NCList<Range> ncl1 = new NCList<>(all);
    assertTrue(ncl1.isValid());
    assertEquals(ncl1.getDepth(), 4);
    String deepRight = "[1-8, 2-9 [2-7, 3-8 [3-6, 4-7 [4-5, 5-6]]]]";
    assertEquals(ncl1.toString(), deepRight);

    /*
     * add intervals small to large in pairs, fromLeft then fromRight
     * results in a tree mostly nested under [1-8]
     */
    NCList<Range> ncl2 = new NCList<>();
    int j = fromLeft.size();
    for (int i = 1; i <= j; i++)
    {
      ncl2.add(fromLeft.get(j - i));
      ncl2.add(fromRight.get(j - i));
    }
    assertTrue(ncl2.isValid());
    assertEquals(ncl2.getDepth(), 4);
    String deepLeft = "[1-8 [2-7 [3-6 [4-5, 5-6], 4-7], 3-8], 2-9]";
    assertEquals(ncl2.toString(), deepLeft);

    /*
     * add intervals large to small in pairs, fromLeft then fromRight
     * - also results in a deeply nested tree mostly under [1-8]
     */
    NCList<Range> ncl3 = new NCList<>();
    for (int i = 0; i < j; i++)
    {
      ncl3.add(fromLeft.get(i));
      ncl3.add(fromRight.get(i));
    }
    assertTrue(ncl3.isValid());
    assertEquals(ncl3.getDepth(), 4);
    assertEquals(ncl3.toString(), deepLeft);

    /*
     * add intervals small to large in pairs, fromRight then fromLeft
     * - results in a deeply nested tree under [2-9]
     */
    NCList<Range> ncl4 = new NCList<>();
    for (int i = 1; i <= j; i++)
    {
      ncl4.add(fromRight.get(j - i));
      ncl4.add(fromLeft.get(j - i));
    }
    assertTrue(ncl4.isValid());
    assertEquals(ncl4.getDepth(), 4);
    assertEquals(ncl4.toString(), deepRight);

    /*
     * add all fromRight intervals, then all fromLeft (large to small)
     */
    NCList<Range> ncl5 = new NCList<>();
    for (int i = 0; i < j; i++)
    {
      ncl5.add(fromRight.get(i));
    }
    for (int i = 0; i < j; i++)
    {
      ncl5.add(fromLeft.get(i));
    }
    assertTrue(ncl5.isValid());
    assertEquals(ncl5.getDepth(), 4);
    String balanced = "[1-8 [2-7 [3-6 [4-5]]], 2-9 [3-8 [4-7 [5-6]]]]";
    assertEquals(ncl5.toString(), balanced);
  }

  @Test(groups = "Functional")
  public void testIterator()
  {
    NCList<Range> ncl = new NCList<>();
    assertFalse(ncl.iterator().hasNext());

    Range r1 = new Range(20, 40);
    Range r2 = new Range(20, 25);
    Range r3 = new Range(10, 30);
    Range r4 = new Range(30, 50);
    ncl.add(r1);
    assertTrue(ncl.iterator().hasNext());
    ncl.add(r2);
    ncl.add(r3);
    ncl.add(r4);
    assertEquals(ncl.toString(), "[10-30, 20-40 [20-25], 30-50]");

    Iterator<Range> it = ncl.iterator();
    assertSame(it.next(), r3);
    assertSame(it.next(), r1);
    assertSame(it.next(), r2);
    assertSame(it.next(), r4);
    assertFalse(it.hasNext());
    try
    {
      it.next();
      fail("expected exception");
    } catch (NoSuchElementException e)
    {
      // expected
    }
  }

  @Test(groups = "Functional")
  public void testFindFirstOverlap()
  {
    List<Range> ranges = new ArrayList<>();
    addRange(ranges, 1, 40);
    addRange(ranges, 10, 20);
    addRange(ranges, 15, 30);
    addRange(ranges, 50, 60);

    NCList<Range> ncl = new NCList<>(ranges);
    String expected = "[1-40 [10-20, 15-30], 50-60]";
    assertEquals(ncl.toString(), expected);
    assertTrue(ncl.isValid());

    int j = ncl.findFirstOverlap(35);
    assertEquals(j, 0, "findFirstOverlap for " + 35);

    j = ncl.findFirstOverlap(80);
    assertEquals(j, 2, "findFirstOverlap for " + 80);
  }

  /**
   * A helper method that constructs a Range and adds it to a list
   * 
   * @param l
   * @param from
   * @param to
   * @return
   */
  private Range addRange(List<Range> l, int from, int to)
  {
    Range r = new Range(from, to);
    l.add(r);
    return r;
  }

  @Test(groups = "Functional")
  public void testAdd_colocated()
  {
    List<Range> ranges = new ArrayList<>();
    ranges.add(new Range(20, 50));
    ranges.add(new Range(20, 50));
    NCList<Range> ncl = new NCList<>(ranges);
    assertEquals(ncl.toString(), "[20-50, 20-50]");
    assertTrue(ncl.isValid());
  }

  /**
   * Test that verifies correct NCList reorganisation after deleting an internal
   * node
   */
  @Test(groups = "Functional")
  public void testRemove_withPromotion()
  {
    /*
     * a simple example
     */
    NCList<Range> ncl = new NCList<>();
    ncl.add(new Range(10, 30));
    ncl.add(new Range(20, 30));
    ncl.add(new Range(20, 40));
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(), "[10-30 [20-30], 20-40]");
    ncl.remove(new Range(10, 30));
    assertEquals(ncl.toString(), "[20-40 [20-30]]");
    assertTrue(ncl.isValid());

    /*
     * add intervals in a cunningly designed order...
     */
    ncl.clear();
    ncl.add(new Range(20, 60));
    ncl.add(new Range(35, 55));
    ncl.add(new Range(40, 40));
    ncl.add(new Range(30, 40));
    ncl.add(new Range(35, 36));
    ncl.add(new Range(1, 50));
    ncl.add(new Range(45, 60));
    ncl.add(new Range(40, 70));
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[1-50, 20-60 [30-40 [35-36], 35-55 [40-40], 45-60], 40-70]");

    /*
     * removing 20-60 should result in: 
     * 30-40 and its child relocated inside 1-50
     * 35-55 and its child inserted between 1-50 and 40-70
     * 45-60 relocated inside 40-70
     */
    ncl.remove(new Range(20, 60));
    assertEquals(ncl.toString(),
            "[1-50 [30-40 [35-36]], 35-55 [40-40], 40-70 [45-60]]");
    assertTrue(ncl.isValid());
  }

  /**
   * test for the case that a promoted interval precedes all other subranges
   * (only possible if the deleted interval is the first of its siblings)
   */
  @Test(groups = "Functional")
  public void testRemove_withPromotionLeft()
  {
    NCList<Range> ncl = new NCList<>();
    ncl.clear();
    ncl.add(new Range(20, 60));
    ncl.add(new Range(30, 50));
    ncl.add(new Range(32, 40));
    ncl.add(new Range(40, 50));
    ncl.add(new Range(35, 60));
    ncl.add(new Range(38, 55));
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[20-60 [30-50 [32-40, 40-50], 35-60 [38-55]]]");

    /*
     * delete 30-50: 
     * child 32-40 becomes leftmost subrange
     * child 40-50 should nest inside 35-60 
     */
    ncl.remove(new Range(30, 50));
    assertEquals(ncl.toString(), "[20-60 [32-40, 35-60 [38-55 [40-50]]]]");
    assertTrue(ncl.isValid());
  }

  /**
   * test for the case that a promoted interval follows all other subranges
   * (only possible if the deleted interval is the last of its siblings)
   */
  @Test(groups = "Functional")
  public void testRemove_withPromotionRight()
  {
    NCList<Range> ncl = new NCList<>();
    ncl.clear();
    ncl.add(new Range(20, 60));
    ncl.add(new Range(30, 50));
    ncl.add(new Range(32, 40));
    ncl.add(new Range(40, 50));
    ncl.add(new Range(25, 40));
    ncl.add(new Range(28, 40));
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(),
            "[20-60 [25-40 [28-40], 30-50 [32-40, 40-50]]]");

    /*
     * delete 30-50: 
     * child 32-40 should nest inside 25-40 
     * child 40-50 becomes rightmost subrange
     */
    ncl.remove(new Range(30, 50));
    assertEquals(ncl.toString(), "[20-60 [25-40 [28-40 [32-40]], 40-50]]");
    assertTrue(ncl.isValid());
  }

  @Test(groups = "Functional")
  public void testPush()
  {
    List<Range> ranges = new ArrayList<>();
    addRange(ranges, 10, 20);
    addRange(ranges, 15, 30);
    addRange(ranges, 20, 40);
    NCList<Range> ncl = new NCList<>(ranges);
    assertTrue(ncl.isValid());
    assertEquals(ncl.toString(), "[10-20, 15-30, 20-40]");

    /*
     * push 15-30, 20-40 inside new node 12-45
     */
    ncl.push(new NCNode<>(new Range(12, 45)), 1, 2);
    assertEquals(ncl.toString(), "[10-20, 12-45 [15-30, 20-40]]");

    /*
     * push 10-20 inside new node 10-20
     */
    ncl.push(new NCNode<>(new Range(10, 20)), 0, 0);
    assertEquals(ncl.toString(), "[10-20 [10-20], 12-45 [15-30, 20-40]]");

    /*
     * try to push 12-45 inside 13-45 - should fail
     */
    try
    {
      ncl.push(new NCNode<>(new Range(13, 45)), 1, 1);
      fail("Expected exception");
    } catch (IllegalArgumentException e)
    {
      // expected;
    }
  }
}
