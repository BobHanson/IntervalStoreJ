package intervalstore.impl1;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;

import org.testng.annotations.Test;

/**
 * A simple test to keep the test coverage stats high ;-)
 */
public class RangeTest
{
  @Test
  public void testEquals_hashCode()
  {
    Range r1 = new Range(10, 20);
    Range r2 = new Range(10, 20);
    Range r3 = new Range(10, 21);

    assertEquals(r1, r1);
    assertEquals(r1, r2);
    assertEquals(r2, r1);
    assertNotEquals(r1, r3);
    assertNotEquals(r3, r1);
    assertFalse(r1.equals(null));
    assertFalse(r1.equals("?"));

    assertEquals(r1.hashCode(), r2.hashCode());
  }
}
