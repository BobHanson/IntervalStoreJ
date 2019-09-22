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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.testng.annotations.Test;

import junit.extensions.PA;

public class NCNodeTest
{
  @Test(groups = "Functional")
  public void testAddNode()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    assertEquals(node.getBegin(), 10);
    NCNode<Range> n2 = new NCNode<>(new Range(10, 15));
    node.addNode(n2);

    List<Range> contents = new ArrayList<>();
    node.getEntries(contents);
    assertEquals(contents.size(), 2);
    assertTrue(contents.contains(r1));
    assertTrue(contents.contains(n2.getRegion()));
  }

  @Test(
    groups = "Functional",
    expectedExceptions =
    { IllegalArgumentException.class })
  public void testAddNode_invalidRangeStart()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    assertEquals(node.getBegin(), 10);
    NCNode<Range> n2 = new NCNode<>(new Range(9, 15));
    node.addNode(n2);
  }

  @Test(
    groups = "Functional",
    expectedExceptions =
    { IllegalArgumentException.class })
  public void testAddNode_invalidRangeEnd()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    assertEquals(node.getBegin(), 10);
    NCNode<Range> n2 = new NCNode<>(new Range(12, 21));
    node.addNode(n2);
  }

  @Test(groups = "Functional")
  public void testGetEntries()
  {
    Range r1 = new Range(10, 20);
    NCNode<Range> node = new NCNode<>(r1);
    List<Range> entries = new ArrayList<>();

    node.getEntries(entries);
    assertEquals(entries.size(), 1);
    assertTrue(entries.contains(r1));

    // clearing the returned list does not affect the NCNode
    entries.clear();
    node.getEntries(entries);
    assertEquals(entries.size(), 1);
    assertTrue(entries.contains(r1));

    NCNode<Range> n2 = new NCNode<>(new Range(15, 18));
    node.addNode(n2);
    entries.clear();
    node.getEntries(entries);
    assertEquals(entries.size(), 2);
    assertTrue(entries.contains(r1));
    assertTrue(entries.contains(n2.getRegion()));
  }

  /**
   * Tests for the contains method (uses entry.equals() test)
   */
  @Test(groups = "Functional")
  public void testContainsInterval()
  {
    SimpleFeature sf1 = new SimpleFeature(1, 10, "desc");
    SimpleFeature sf2 = new SimpleFeature(1, 10, "desc");
    SimpleFeature sf3 = new SimpleFeature(1, 10, "description");
    NCNode<SimpleFeature> node = new NCNode<>(sf1);

    assertFalse(node.contains(null));
    assertTrue(node.contains(sf1));
    assertTrue(node.contains(sf2)); // sf1.equals(sf2)
    assertFalse(node.contains(sf3)); // !sf1.equals(sf3)
  }

  /**
   * Test method that checks for valid structure. Valid means that all
   * subregions (if any) lie within the root range, and that all subregions have
   * valid structure.
   */
  @Test(groups = "Functional")
  public void testIsValid()
  {
    Range r1 = new Range(10, 20);
    Range r2 = new Range(14, 15);
    Range r3 = new Range(16, 17);

    NCNode<Range> node = new NCNode<>(r1);
    assertTrue(node.isValid());
    node.addNode(new NCNode<>(r2));
    assertTrue(node.isValid());
    node.addNode(new NCNode<>(r3));
    assertTrue(node.isValid());

    /*
     * node has root range [10-20] and contains an
     * NCList of [14-15, 16-17]
     */
    assertTrue(node.isValid());
    PA.setValue(r1, "start", 15);
    assertFalse(node.isValid()); // r2 not within r1
    PA.setValue(r1, "start", 10);
    assertTrue(node.isValid());
    PA.setValue(r1, "end", 16);
    assertFalse(node.isValid()); // r3 not within r1
    PA.setValue(r1, "end", 20);
    assertTrue(node.isValid());
    PA.setValue(r3, "start", 12);
    assertFalse(node.isValid()); // r3 should precede r2
    PA.setValue(r3, "start", 16);
    assertTrue(node.isValid());

    // expect an empty NCList to be nulled
    ((NCList) PA.getValue(node, "subregions")).clear();
    assertFalse(node.isValid());
  }

  @Test(groups = "Functional")
  public void testRemove()
  {
    Range r1 = new Range(10, 20);
    Range r2 = new Range(14, 15);
    Range r3 = new Range(16, 17);

    NCNode<Range> node = new NCNode<>(r1);
    node.addNode(new NCNode<>(r2));
    node.addNode(new NCNode<>(r3));

    assertFalse(node.remove(null));
    assertFalse(node.remove(new Range(10, 21))); // no match
    try
    {
      node.remove(r1); // can't remove root node
      fail("expected exception");
    } catch (IllegalArgumentException e)
    {
      // expected
    }
    assertEquals(node.toString(), "10-20 [14-15, 16-17]");
    assertTrue(node.remove(new Range(14, 15)));
    assertEquals(node.toString(), "10-20 [16-17]");
    assertTrue(node.remove(new Range(16, 17)));
    assertEquals(node.toString(), "10-20");

    // enclosed NCList is nulled when empty
    assertNull(node.getSubRegions());
  }

  @Test(groups = "Functional")
  public void testConstructor()
  {
    Range r1 = new Range(10, 20);
    Range r2 = new Range(14, 15);
    Range r3 = new Range(16, 17);

    NCNode<Range> node = new NCNode<>(Arrays.asList(r1, r2, r3));
    assertSame(node.getRegion(), r1);
    assertEquals(node.toString(), "10-20 [14-15, 16-17]");

    /*
     * an empty list is rejected
     */
    try
    {
      node = new NCNode<>(new ArrayList<Range>());
      fail("expected exception");
    } catch (IllegalArgumentException e)
    {
      // expected
    }
  }

  @Test(groups = "Functional")
  public void testIterator()
  {
    Range r1 = new Range(10, 20);
    Range r2 = new Range(14, 15);
    Range r3 = new Range(16, 17);

    NCNode<Range> node = new NCNode<>(Arrays.asList(r1, r2, r3));
    Iterator<Range> it = node.iterator();
    assertSame(it.next(), r1);
    assertSame(it.next(), r2);
    assertSame(it.next(), r3);
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
}
