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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import junit.extensions.PA;

public class IntervalStoreTest
{
  @Test(groups = "Functional")
  public void testConstructor_noNesting()
  {
    /*
     * no-arg constructor is not terribly interesting
     */
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertTrue(store.isValid());
    assertTrue(store.isEmpty());
    assertNull(PA.getValue(store, "nested"));
    List nonNested = (List) PA.getValue(store, "nonNested");
    assertNotNull(nonNested);
    assertTrue(nonNested.isEmpty());

    /*
     * make some ranges that are overlapping but non-nested
     */
    Range r1 = new Range(10, 20);
    Range r2 = new Range(10, 20);
    Range r3 = new Range(15, 21);
    Range r4 = new Range(20, 30);
    Range r5 = new Range(40, 40);
    Range r6 = new Range(40, 40);
    // add to a list in unsorted order so constructor has to sort
    List<Range> ranges = Arrays.asList(r6, r1, r4, r3, r2, r5);

    IntervalStore<Range> store2 = new IntervalStore<>(ranges);
    assertTrue(store2.isValid());
    assertEquals(store2.size(), 6);
    assertNull(PA.getValue(store2, "nested"));
    nonNested = (List) PA.getValue(store2, "nonNested");
    assertNotNull(nonNested);
    assertEquals(nonNested.size(), 6);
    assertSame(nonNested.get(0), r1);
    assertSame(nonNested.get(1), r2);
    assertSame(nonNested.get(2), r3);
    assertSame(nonNested.get(3), r4);
    // co-located intervals stay in their original respective order
    // (because Collections.sort() is 'stable')
    assertSame(nonNested.get(4), r6);
    assertSame(nonNested.get(5), r5);
  }

  @Test(groups = "Functional")
  public void testConstructor_nesting()
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

    /*
     * note the list is now sorted by start ascending, end descending
     */
    assertEquals(ranges,
            Arrays.asList(r10, r1, r2, r3, r4, r7, r8, r9, r5, r6));

    assertTrue(store2.isValid());
    assertEquals(store2.size(), 10);

    NCList<Range> nested = (NCList<Range>) PA.getValue(store2, "nested");
    List<Range> nonNested = (List<Range>) PA.getValue(store2, "nonNested");

    /*
     * r1 and r2[10-20] nest in r10[20-21]
     * r7[22-28] nests in r4[20-30]
     * r8[22-28] nests in r7
     * r9[24-26] nests in r8
     */
    assertNotNull(nested);
    assertEquals(nested.size(), 5);
    assertTrue(nested.isValid());
    assertTrue(nested.contains(r1));
    assertTrue(nested.contains(r2));
    assertTrue(nested.contains(r7));
    assertTrue(nested.contains(r8));
    assertTrue(nested.contains(r9));

    assertNotNull(nonNested);
    assertEquals(nonNested.size(), 5);
    assertSame(nonNested.get(0), r10);
    assertSame(nonNested.get(1), r3);
    assertSame(nonNested.get(2), r4);
    // co-located intervals stay in their original respective order
    // (because Collections.sort() is 'stable')
    assertSame(nonNested.get(3), r6);
    assertSame(nonNested.get(4), r5);
  }

  @Test(groups = "Functional")
  public void testFindOverlaps_nonNested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 20);
    // same range different description
    SimpleFeature sf2 = new SimpleFeature(10, 20, "desc");
    store.add(sf2);
    SimpleFeature sf3 = add(store, 15, 25);
    SimpleFeature sf4 = add(store, 20, 35);

    assertTrue(store.isValid());
    assertEquals(store.size(), 4);
    assertNull(PA.getValue(store, "nested"));
    List<SimpleFeature> overlaps = store.findOverlaps(1, 9);
    assertTrue(overlaps.isEmpty());

    overlaps = store.findOverlaps(8, 10);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(12, 16);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf3));

    overlaps = store.findOverlaps(33, 33);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf4));

    /*
     * ensure edge cases are covered
     */
    overlaps = store.findOverlaps(35, 40);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf4));
    assertTrue(store.findOverlaps(36, 100).isEmpty());
    assertTrue(store.findOverlaps(1, 9).isEmpty());
  }

  @Test(groups = "Functional")
  public void testFindOverlaps_nested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 50);
    SimpleFeature sf2 = add(store, 10, 40);
    SimpleFeature sf3 = add(store, 20, 30);
    // feature at same location but different description
    SimpleFeature sf4 = new SimpleFeature(20, 30, "different desc");
    store.add(sf4);
    SimpleFeature sf5 = add(store, 35, 36);

    List<SimpleFeature> overlaps = store.findOverlaps(1, 9);
    assertTrue(overlaps.isEmpty());

    overlaps = store.findOverlaps(10, 15);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(45, 60);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf1));

    overlaps = store.findOverlaps(32, 38);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf5));

    overlaps = store.findOverlaps(15, 25);
    assertEquals(overlaps.size(), 4);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));
  }

  @Test(groups = "Functional")
  public void testFindOverlaps_mixed()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 50);
    SimpleFeature sf2 = add(store, 1, 15);
    SimpleFeature sf3 = add(store, 20, 30);
    SimpleFeature sf4 = add(store, 40, 100);
    SimpleFeature sf5 = add(store, 60, 100);
    SimpleFeature sf6 = add(store, 70, 70);

    List<SimpleFeature> overlaps = store.findOverlaps(200, 200);
    assertTrue(overlaps.isEmpty());

    overlaps = store.findOverlaps(1, 9);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(5, 18);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));

    overlaps = store.findOverlaps(30, 40);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));

    overlaps = store.findOverlaps(80, 90);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));

    overlaps = store.findOverlaps(68, 70);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
    assertTrue(overlaps.contains(sf6));
  }

  /**
   * Helper method to add a feature with type "desc"
   * 
   * @param store
   * @param from
   * @param to
   * @return
   */
  SimpleFeature add(IntervalStore<SimpleFeature> store, int from,
          int to)
  {
    SimpleFeature sf1 = new SimpleFeature(from, to, "desc");
    store.add(sf1);
    return sf1;
  }

  /**
   * Tests for the method that returns false for an attempt to add an interval
   * that would enclose, or be enclosed by, another interval
   */
  @Test(groups = "Functional")
  public void testAddNonNestedInterval()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();

    String type = "Domain";
    SimpleFeature sf1 = new SimpleFeature(10, 20, type);
    assertTrue(store.addNonNestedInterval(sf1));

    // co-located feature is ok
    SimpleFeature sf2 = new SimpleFeature(10, 20, type);
    assertTrue(store.addNonNestedInterval(sf2));

    // overlap left is ok
    SimpleFeature sf3 = new SimpleFeature(5, 15, type);
    assertTrue(store.addNonNestedInterval(sf3));

    // overlap right is ok
    SimpleFeature sf4 = new SimpleFeature(15, 25, type);
    assertTrue(store.addNonNestedInterval(sf4));

    // add enclosing feature is not ok
    SimpleFeature sf5 = new SimpleFeature(10, 21, type);
    assertFalse(store.addNonNestedInterval(sf5));
    SimpleFeature sf6 = new SimpleFeature(4, 15, type);
    assertFalse(store.addNonNestedInterval(sf6));
    SimpleFeature sf7 = new SimpleFeature(1, 50, type);
    assertFalse(store.addNonNestedInterval(sf7));

    // add enclosed feature is not ok
    SimpleFeature sf8 = new SimpleFeature(10, 19, type);
    assertFalse(store.addNonNestedInterval(sf8));
    SimpleFeature sf9 = new SimpleFeature(16, 25, type);
    assertFalse(store.addNonNestedInterval(sf9));
    SimpleFeature sf10 = new SimpleFeature(7, 7, type);
    assertFalse(store.addNonNestedInterval(sf10));

    store.remove(sf4);
    SimpleFeature sf11 = new SimpleFeature(30, 40, type);
    assertTrue(store.addNonNestedInterval(sf11));
    SimpleFeature sf12 = new SimpleFeature(10, 19, type);
    assertFalse(store.addNonNestedInterval(sf12));

    assertEquals(store.toString(),
            "[5:15:Domain, 10:20:Domain, 10:20:Domain, 30:40:Domain]");
    SimpleFeature sf13 = new SimpleFeature(35, 55, type);
    assertTrue(store.addNonNestedInterval(sf13));
    SimpleFeature sf14 = new SimpleFeature(1, 2, type);
    assertTrue(store.addNonNestedInterval(sf14));
  }

  @Test(groups = "Functional")
  public void testRemove()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 20);
    assertTrue(store.contains(sf1));

    assertFalse(store.remove("what is this?"));
    assertFalse(store.remove(null));

    /*
     * simple deletion
     */
    assertTrue(store.remove(sf1));
    assertTrue(store.isEmpty());

    SimpleFeature sf2 = add(store, 0, 0);
    SimpleFeature sf2a = add(store, 30, 40);
    assertTrue(store.contains(sf2));
    assertFalse(store.remove(sf1));
    assertTrue(store.remove(sf2));
    assertTrue(store.remove(sf2a));
    assertTrue(store.isEmpty());

    /*
     * nested feature deletion
     */
    SimpleFeature sf4 = add(store, 20, 30);
    SimpleFeature sf5 = add(store, 22, 26); // to NCList
    SimpleFeature sf6 = add(store, 23, 24); // child of sf5
    SimpleFeature sf7 = add(store, 25, 25); // sibling of sf6
    SimpleFeature sf8 = add(store, 24, 24); // child of sf6
    SimpleFeature sf9 = add(store, 23, 23); // child of sf6
    assertEquals(store.size(), 6);

    // delete a node with children - they take its place
    assertTrue(store.remove(sf6)); // sf8, sf9 should become children of sf5
    assertEquals(store.size(), 5);
    assertFalse(store.contains(sf6));

    // delete a node with no children
    assertTrue(store.remove(sf7));
    assertEquals(store.size(), 4);
    assertFalse(store.contains(sf7));

    // delete root of NCList
    assertTrue(store.remove(sf5));
    assertEquals(store.size(), 3);
    assertFalse(store.contains(sf5));

    // continue the killing fields
    assertTrue(store.remove(sf4));
    assertEquals(store.size(), 2);
    assertFalse(store.contains(sf4));

    assertTrue(store.remove(sf9));
    assertEquals(store.size(), 1);
    assertFalse(store.contains(sf9));

    assertTrue(store.remove(sf8));
    assertTrue(store.isEmpty());
  }

  @Test(groups = "Functional")
  public void testRemoveNonNested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 15, 25);
    SimpleFeature sf4 = new SimpleFeature(25, 35, "desc");
    store.add(sf4);
    SimpleFeature sf5 = new SimpleFeature(25, 35, "desc");
    store.add(sf5);
    SimpleFeature sf6 = new SimpleFeature(25, 35, "desc2");
    store.add(sf6);
    SimpleFeature sf7 = new SimpleFeature(25, 35, "desc2");
    store.add(sf7);

    // remove unmatched entry fails
    assertFalse(store.removeNonNested(new SimpleFeature(10, 12, "desc")));
    assertFalse(store.removeNonNested(new SimpleFeature(25, 35, "desc3")));

    assertEquals(store.size(), 5);
    assertTrue(store.removeNonNested(new SimpleFeature(15, 25, "desc")));
    assertEquals(store.size(), 4);
    assertFalse(store.contains(sf1));

    /*
     * remove an entry; the first matching entry is removed
     * there is no guarantee which object instance this is
     * if more than one entry matches
     */
    assertTrue(store.contains(sf4));
    assertTrue(store.removeNonNested(sf4));
    assertEquals(store.size(), 3);

    /*
     * 'contains' answers true for both (by equals test),
     * even though only one is actually still in the list
     */
    assertTrue(store.contains(sf4));
    assertTrue(store.contains(sf5));
    List<SimpleFeature> nonNested = (List<SimpleFeature>)PA.getValue(store,  "nonNested");
    assertFalse(containsObject(nonNested, sf4)
            && containsObject(nonNested, sf5));

    // remove an entry by matching first equal item
    assertTrue(store.contains(sf6));
    assertTrue(store.contains(sf7));
    // sf6.equals(sf7) so is the removed item
    assertTrue(store.removeNonNested(sf7));
    assertEquals(store.size(), 2);
    assertTrue(store.contains(sf5));
    assertTrue(store.contains(sf6));
    assertTrue(store.contains(sf7));
    assertFalse(containsObject(nonNested, sf6)
            && containsObject(nonNested, sf7));
  }

  /**
   * A helper method to test whether a list contains a specific object (by
   * object identity, not equality test as used by List.contains())
   * 
   * @param list
   * @param o
   * @return
   */
  private static boolean containsObject(List<? extends Object> list,
          Object o)
  {
    for (Object i : list)
    {
      if (i == o)
      {
        return true;
      }
    }
    return false;
  }

  @Test(groups = "Functional")
  public void testAdd()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();

    assertFalse(store.add(null));

    SimpleFeature sf1 = new SimpleFeature(10, 20, "Cath");
    SimpleFeature sf2 = new SimpleFeature(10, 20, "Cath");

    assertTrue(store.add(sf1));
    assertEquals(store.size(), 1);

    /*
     * contains should return true for the same or an identical feature
     */
    assertTrue(store.contains(sf1));
    assertTrue(store.contains(sf2));

    /*
     * duplicates are accepted
     */
    assertTrue(store.add(sf2));
    assertEquals(store.size(), 2);

    SimpleFeature sf3 = new SimpleFeature(0, 0, "Cath");
    assertTrue(store.add(sf3));
    assertEquals(store.size(), 3);
  }

  @Test(groups = "Functional")
  public void testIsEmpty()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertTrue(store.isEmpty());
    assertEquals(store.size(), 0);

    /*
     * non-nested feature
     */
    SimpleFeature sf1 = new SimpleFeature(10, 20, "Cath");
    store.add(sf1);
    assertFalse(store.isEmpty());
    assertEquals(store.size(), 1);
    store.remove(sf1);
    assertTrue(store.isEmpty());
    assertEquals(store.size(), 0);

    sf1 = new SimpleFeature(0, 0, "Cath");
    store.add(sf1);
    assertFalse(store.isEmpty());
    assertEquals(store.size(), 1);
    store.remove(sf1);
    assertTrue(store.isEmpty());
    assertEquals(store.size(), 0);

    /*
     * sf2, sf3 added as nested features
     */
    sf1 = new SimpleFeature(19, 49, "Cath");
    SimpleFeature sf2 = new SimpleFeature(20, 40, "Cath");
    SimpleFeature sf3 = new SimpleFeature(25, 35, "Cath");
    store.add(sf1);
    assertEquals(store.size(), 1);
    store.add(sf2);
    assertEquals(store.size(), 2);
    store.add(sf3);
    assertEquals(store.size(), 3);
    assertTrue(store.remove(sf1));
    assertEquals(store.size(), 2);
    // IntervalStore should now only contain features in the NCList
    List<SimpleFeature> nonNested = (List<SimpleFeature>) PA.getValue(store,
            "nonNested");
    NCList<SimpleFeature> nested = (NCList<SimpleFeature>) PA.getValue(store,
            "nested");
    assertTrue(nonNested.isEmpty());
    assertEquals(nested.size(), 2);
    assertFalse(store.isEmpty());
    assertTrue(store.remove(sf2));
    assertEquals(store.size(), 1);
    assertFalse(store.isEmpty());
    assertTrue(store.remove(sf3));
    assertEquals(store.size(), 0);
    assertTrue(store.isEmpty()); // all gone
  }

  @Test(groups = "Functional")
  public void testListContains()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertFalse(store.listContains(null, null));
    List<SimpleFeature> features = new ArrayList<>();
    assertFalse(store.listContains(features, null));
    assertFalse(store.listContains(features, "wossis?"));

    SimpleFeature sf1 = new SimpleFeature(20, 30, "desc1");
    assertFalse(store.listContains(null, sf1));
    assertFalse(store.listContains(features, sf1));

    features.add(sf1);
    SimpleFeature sf2 = new SimpleFeature(20, 30, "desc1");
    SimpleFeature sf3 = new SimpleFeature(20, 40, "desc1");
    SimpleFeature sf4 = new SimpleFeature(0, 10, "desc1");

    // sf2.equals(sf1) so contains should return true
    assertTrue(store.listContains(features, sf2));
    assertFalse(store.listContains(features, sf3));
    assertFalse(store.listContains(features, sf4));
  }

  @Test(groups = "Functional")
  public void testRemove_readd()
  {
    /*
     * add a feature and a nested feature
     */
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 20);
    // sf2 is nested in sf1 so will be stored in nestedFeatures
    SimpleFeature sf2 = add(store, 12, 14);
    assertEquals(store.size(), 2);
    assertTrue(store.contains(sf1));
    assertTrue(store.contains(sf2));

    @SuppressWarnings("unchecked")
    List<SimpleFeature> nonNested = (List<SimpleFeature>) PA.getValue(store,
            "nonNested");
    @SuppressWarnings("unchecked")
    NCList<SimpleFeature> nested = (NCList<SimpleFeature>) PA
            .getValue(store, "nested");
    assertTrue(nonNested.contains(sf1));
    assertTrue(nested.contains(sf2));
  
    /*
     * delete the first feature
     */
    assertTrue(store.remove(sf1));
    assertFalse(store.contains(sf1));
    assertTrue(store.contains(sf2));

    /*
     * re-add the 'nested' feature; it is now duplicated
     */
    store.add(sf2);
    assertEquals(store.size(), 2);
    assertTrue(store.contains(sf2));
  }

  @Test(groups = "Functional")
  public void testContains()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = new SimpleFeature(10, 20, "Cath");
    SimpleFeature sf2 = new SimpleFeature(10, 20, "Pfam");

    store.add(sf1);
    assertTrue(store.contains(sf1));
    assertTrue(store.contains(new SimpleFeature(sf1))); // identical feature
    assertFalse(store.contains(sf2)); // different description

    /*
     * add a nested feature
     */
    SimpleFeature sf3 = new SimpleFeature(12, 16, "Cath");
    store.add(sf3);
    assertTrue(store.contains(sf3));
    assertTrue(store.contains(new SimpleFeature(sf3)));

    /*
     * delete the outer (enclosing, non-nested) feature
     */
    store.remove(sf1);
    assertFalse(store.contains(sf1));
    assertTrue(store.contains(sf3));
  }

  @Test(groups = "Functional")
  public void testToString()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertEquals(store.toString(), "[]");
    add(store, 20, 30);
    assertEquals(store.toString(), "[20:30:desc]");
    add(store, 25, 35);
    assertEquals(store.toString(), "[20:30:desc, 25:35:desc]");
    add(store, 22, 28);
    add(store, 22, 28);
    add(store, 24, 26);
    assertEquals(store.toString(),
            "[20:30:desc, 25:35:desc]\n[22:28:desc [24:26:desc], 22:28:desc]");
  }

  @Test(groups = "Functional")
  public void testPrettyPrint()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertEquals(store.prettyPrint(), "[]");
    add(store, 20, 30);
    assertEquals(store.prettyPrint(), "[20:30:desc]");
    add(store, 25, 35);
    assertEquals(store.prettyPrint(), "[20:30:desc, 25:35:desc]");
    add(store, 22, 28);
    add(store, 22, 28);
    add(store, 24, 26);
    assertEquals(store.prettyPrint(),
            "[20:30:desc, 25:35:desc]\n22:28:desc\n  24:26:desc\n22:28:desc\n");
  }

  @Test(groups = "Functional")
  public void testIsValid()
  {
    IntervalStore<Range> store = new IntervalStore<>();
    assertTrue(store.isValid());
    Range r1 = new Range(10, 30);
    Range r2 = new Range(20, 40);
    Range r3 = new Range(60, 70);
    store.add(r1);
    store.add(r2);
    store.add(r3);
    assertTrue(store.isValid());
    assertNull(PA.getValue(store, "nested")); // all top level ranges so far

    PA.setValue(r1, "start", 20);
    assertFalse(store.isValid()); // r2 encloses preceding r1
    PA.setValue(r1, "end", 40);
    assertTrue(store.isValid()); // r1 and r2 coincide - this is ok
    PA.setValue(r1, "end", 41);
    assertFalse(store.isValid()); // r1 encloses following r2

    // reset
    PA.setValue(r1, "start", 10);
    PA.setValue(r1, "end", 30);
    assertTrue(store.isValid());

    // make r3 precede r2 (without containment)
    PA.setValue(r3, "start", 15);
    PA.setValue(r3, "end", 35);
    assertFalse(store.isValid());

    // reset
    PA.setValue(r3, "start", 60);
    PA.setValue(r3, "end", 70);
    assertTrue(store.isValid());

    Range r4 = new Range(25, 28); // nested in r1
    Range r5 = new Range(26, 27); // nested in r4
    store.add(r4);
    store.add(r5);
    assertNotNull(PA.getValue(store, "nested"));

    // break the nested NCList to show it is checked
    PA.setValue(r5, "end", 29); // should no longer nest in r4!
    assertFalse(store.isValid());
  }

  @Test(groups = "Functional")
  public void testGetDepth()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    assertEquals(store.getDepth(), 0);
    add(store, 10, 20);
    assertEquals(store.getDepth(), 1);
    add(store, 15, 25); // non-nested
    add(store, 30, 40); // non-nested
    assertEquals(store.getDepth(), 1);
    SimpleFeature sf3 = add(store, 32, 38); // nested
    assertEquals(store.getDepth(), 2);
    SimpleFeature sf1 = add(store, 33, 35); // nested in 30-40
    assertEquals(store.getDepth(), 3);
    SimpleFeature sf2 = add(store, 34, 37); // nested sibling
    assertEquals(store.getDepth(), 3);
    add(store, 35, 36); // nested in 34-37
    assertEquals(store.getDepth(), 4);
    store.remove(sf2); // 35-36 gets promoted
    assertEquals(store.getDepth(), 3);
    store.remove(sf1); // leaves sibling 35-36
    assertEquals(store.getDepth(), 3);
    store.remove(sf3); // 35-36 goes up one level
    assertEquals(store.getDepth(), 2);

    store = new IntervalStore<>();
    sf1 = add(store, 10, 20); // non-nested
    add(store, 12, 18); // nested
    assertEquals(store.getDepth(), 2);
    store.remove(sf1); // leaves an empty non-nested list
    assertEquals(store.getDepth(), 1);
  }

  @Test(groups = "Functional")
  public void testFindOverlaps_resultsArg_mixed()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 50);
    SimpleFeature sf2 = add(store, 1, 15);
    SimpleFeature sf3 = add(store, 20, 30);
    SimpleFeature sf4 = add(store, 40, 100);
    SimpleFeature sf5 = add(store, 60, 100);
    SimpleFeature sf6 = add(store, 70, 70);
  
    List<SimpleFeature> overlaps = new ArrayList<>();
    List<SimpleFeature> overlaps2 = store.findOverlaps(200, 200, overlaps);
    assertSame(overlaps, overlaps2);
    assertTrue(overlaps.isEmpty());
  
    overlaps.clear();
    store.findOverlaps(1, 9, overlaps);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf2));
  
    overlaps.clear();
    store.findOverlaps(5, 18, overlaps);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
  
    overlaps.clear();
    store.findOverlaps(30, 40, overlaps);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));
  
    overlaps.clear();
    store.findOverlaps(80, 90, overlaps);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
  
    overlaps.clear();
    store.findOverlaps(68, 70, overlaps);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
    assertTrue(overlaps.contains(sf6));

    /*
     * and without clearing the list first
     * note that sf4 is included twice, as an
     * overlap of 68-70 and also of 30-40
     */
    store.findOverlaps(30, 40, overlaps);
    assertEquals(overlaps.size(), 6);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));
    assertTrue(overlaps.contains(sf5));
    assertTrue(overlaps.contains(sf6));
    assertSame(sf4, overlaps.get(0));
    assertSame(sf4, overlaps.get(4));
  }

  @Test(groups = "Functional")
  public void testFindOverlaps_resultsArg_nested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 50);
    SimpleFeature sf2 = add(store, 10, 40);
    SimpleFeature sf3 = add(store, 20, 30);
    // feature at same location but different description
    SimpleFeature sf4 = new SimpleFeature(20, 30, "different desc");
    store.add(sf4);
    SimpleFeature sf5 = add(store, 35, 36);
  
    List<SimpleFeature> overlaps = new ArrayList<>();
    store.findOverlaps(1, 9, overlaps);
    assertTrue(overlaps.isEmpty());
  
    store.findOverlaps(10, 15, overlaps);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
  
    overlaps.clear();
    store.findOverlaps(45, 60, overlaps);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf1));
  
    overlaps.clear();
    store.findOverlaps(32, 38, overlaps);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf5));
  
    overlaps.clear();
    store.findOverlaps(15, 25, overlaps);
    assertEquals(overlaps.size(), 4);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf3));
    assertTrue(overlaps.contains(sf4));
  }

  @Test(groups = "Functional")
  public void testFindOverlaps_resultsArg_nonNested()
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>();
    SimpleFeature sf1 = add(store, 10, 20);
    // same range different description
    SimpleFeature sf2 = new SimpleFeature(10, 20, "desc");
    store.add(sf2);
    SimpleFeature sf3 = add(store, 15, 25);
    SimpleFeature sf4 = add(store, 20, 35);
  
    assertTrue(store.isValid());
    assertEquals(store.size(), 4);
    assertNull(PA.getValue(store, "nested"));
    List<SimpleFeature> overlaps = new ArrayList<>();
    store.findOverlaps(1, 9, overlaps);
    assertTrue(overlaps.isEmpty());
  
    store.findOverlaps(8, 10, overlaps);
    assertEquals(overlaps.size(), 2);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
  
    overlaps.clear();
    store.findOverlaps(12, 16, overlaps);
    assertEquals(overlaps.size(), 3);
    assertTrue(overlaps.contains(sf1));
    assertTrue(overlaps.contains(sf2));
    assertTrue(overlaps.contains(sf3));
  
    overlaps.clear();
    store.findOverlaps(33, 33, overlaps);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf4));
  
    /*
     * ensure edge cases are covered
     */
    overlaps.clear();
    store.findOverlaps(35, 40, overlaps);
    assertEquals(overlaps.size(), 1);
    assertTrue(overlaps.contains(sf4));

    overlaps.clear();
    assertTrue(store.findOverlaps(36, 100, overlaps).isEmpty());
    assertTrue(store.findOverlaps(1, 9, overlaps).isEmpty());
  }
}
