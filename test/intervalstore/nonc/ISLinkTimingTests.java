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
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Random;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import intervalstore.api.IntervalI;
import intervalstore.impl.NCList;
import intervalstore.impl.Range;

/**
 * Note: This class has been extensively modified by Bob Hanson. It is a work in
 * progress, and it no longer matches the original Testing.xlsx page.
 * 
 * Modifications include:
 * 
 * (1) adding several final static options;
 * 
 * (2) standardizing to provide three orders of magnitude in N;
 * 
 * (3) standardizing width of queries to not use count twice in any test;
 * 
 * (4) addition of third nonc (no-NCList) test; and
 * 
 * (5) addition of sort for naive test to match sort that is part of other
 * tests.
 * 
 * (6) uses nanoTime, not currentTimeMillis
 * 
 * 
 * A class with methods to inspect the performance and scalability of loading
 * and querying IntervalStore and NCList, and also a 'naive' (unordered) list
 * for comparison
 * <ul>
 * <li>Enable this test by setting @Test(enabled = true)</li>
 * <li>Run the class as TestNG test</li>
 * <li>Copy the data rows from console output</li>
 * <li>Paste into spreadsheet Timings.xlsx, columns A to G</li>
 * <li>- use 'Paste Special - Text' to paste tab-delimited values into
 * columns</li>
 * <li>other columns compute derived values from raw data</li>
 * </ul>
 * 
 * 
 * 
 * @author gmcarstairs
 */
// this is a long running test (several minutes) so normally left disabled
@Test(enabled = true)
public class ISLinkTimingTests
{

  /**
   * run IntervalStore tests only
   */
  private static final boolean INTERVAL_STORE_ONLY = false;

  /**
   * include Naive tests (when not IntervalStore-only)
   */
  private static final boolean INCLUDE_NAIVE = false;

  /**
   * just test query times
   */
  private static final boolean QUERY_ONLY = true;


  /**
   * flag to allow testing of alternate versions of impl.IntervalStore
   */

  private static final boolean TEST_STORE0 = true;

  private static final boolean TEST_NCLIST0 = true;

  /**
   * flag to allow testing of alternate versions of nonc.IntervalStore
   */

  private static final boolean TEST_STORE2 = true;

  // /**
  // * add # result line to check that all queries are returning the same set.
  // */
  // private static final boolean QUERY_SHOW_RESULT_COUNT = true;

  /**
   * maximum number of seconds per log cycle to wait before bailing out
   */
  private static final double MAX_SEC = 1.0;

  /**
   * factor to multiply first parameter of generateIntervals(sequenceWidth,
   * count, length) by to set store sequence width; higher number reduces number
   * of overlaps
   * 
   * set to 10 generally; 4 for comparison with earlier versions of tests
   * 
   */
  private static final int QUERY_STORE_SEQUENCE_SIZE_FACTOR = 4;// 10;

  /**
   * interval size for the store; absolute(negative) or maximum(positive);
   * 
   * 50 generally; -1 for SNPs
   * 
   */
  private static final int QUERY_STORE_INTERVAL_SIZE = 50;

  /**
   * width of query intervals; negative for absolute, positive for max value
   * 
   * -1 overview single-pixel; -1000 standard view
   * 
   * 
   */
  private static final int QUERY_WINDOW = -1000;

  /**
   * number of queries to generate (independently of the size of the sequence
   * 
   */
  private static final int QUERY_COUNT = 100000;

  /**
   * number of intervals to delete in each test
   */

  private static final int DELETE_COUNT = 1000;

  /**
   * initial value for loop [LOG_0, MAX_LOG] inclusive
   * 
   * 10 starts at 2K; 18 starts at 1M
   */
  private static final int LOG_0 = 18;

  /**
   * final value for loop [LOG_0, MAX_LOG] inclusive
   * 
   * 18 ends at 1M
   */
  private static final int MAX_LOGN = 18;

  /**
   * factor in Math.pow(10, j / LOG_F)
   * 
   * 3.0 is a factor of 10^(1/3) increase (2.15, roughly doubling) for each loop
   */
  private static final double LOG_F = 3.0;

  /**
   * a fixed random seed for repeatable tests
   */
  static final int RANDOM_SEED = 732;

  /**
   * repeat count for each test, to check consistency
   */
  static final int REPEATS = 10;

  /**
   * number of iterations to run before starting timings
   */
  static final int WARMUPS = 3;

  /**
   * set true to log raw data and averages, false to log average and stderr of
   * 10 iterations of each test
   */
  static final boolean LOG_RAW_DATA = false;

  private static final String MODE_INTERVAL_STORE_NCLIST = "IntStoreNCList";

  private static final String MODE_INTERVAL_STORE_NCLIST0 = "IntStoreNCList0";

  private static final String MODE_INTERVAL_STORE_LINK = "IntStoreLink";

  private static final String MODE_INTERVAL_STORE_LINK2 = "IntStoreLink2";

  private static final String MODE_NCLIST = "NCList";

  private static final String MODE_NCLIST0 = "NCList0";

  private static final String MODE_NAIVE = "Naive";

  private Random rand;

  /**
   * if logging raw data values, collect averages here, and print them out
   * together at the end, to make them easier to select as graph ranges in Excel
   */
  private StringBuilder averages;

  /**
   * comparator for "naive" tests
   */
  private Comparator<Range> naiveComp = new Comparator<Range>()
  {

    @Override
    public int compare(Range o1, Range o2)
    {

      int order = Integer.compare(o1.getBegin(), o2.getBegin());
      if (order == 0)
      {
        /*
         * if tied on start position, longer length sorts to left
         * i.e. the negation of normal ordering by length
         */
        order = Integer.compare(o2.getEnd(), o1.getEnd());
      }
      return order;

    }

  };

  /**
   * Print system information and column headings.
   */
  @BeforeClass
  public void setUp()
  {
    rand = new Random(RANDOM_SEED);
    averages = new StringBuilder(2345);
    System.out
            .println("Java version: " + System.getProperty("java.version"));
    System.out.println(System.getProperty("os.arch") + " "
            + System.getProperty("os.name") + " "
            + System.getProperty("os.version") + " cores:"
            + Runtime.getRuntime().availableProcessors());
    System.out.println(
            new Date(System.currentTimeMillis()).toGMTString() + "\n");
    System.out.println(
            "Test             \tsize N \ttests\ttime/ms\trate/(N/ms)\ttime stderr\trate stderr");
  }

  /**
   * Log the accumulated test averages at the end (if we are logging raw data).
   */
  @AfterClass
  public void tearDown()
  {
    System.out.println(averages.toString());
  }

  /**
   * Generates a list of <code>count</code> intervals of length 1-50 in the
   * range [1, 4*count]
   * 
   * @param count
   * @return
   */
  protected synchronized List<Range> generateIntervals(int count)
  {
    return generateIntervals(count * QUERY_STORE_SEQUENCE_SIZE_FACTOR,
            count, 50);
  }

  /**
   * Generates a list of <code>count</code> intervals of length [1,length] in
   * the range [1, sequenceWidth]
   * 
   * @param sequenceWidth
   *          scale of the sequence, based on the number of intervals present,
   *          not the number of queries
   * @param count
   *          the number of intervals to generate
   * @param length
   *          maximum (positive) or absolute(negative) number of intervals to
   *          generate
   * 
   * @return
   */
  private synchronized List<Range> generateIntervals(int sequenceWidth,
          int count, int length)
  {
    int maxPos = sequenceWidth - Math.abs(length);
    List<Range> ranges = new ArrayList<>();
    for (int j = 0; j < count; j++)
    {
      int from = 1 + rand.nextInt(maxPos);
      int to = from + (length < 0 ? -length - 1 : rand.nextInt(length));
      ranges.add(new Range(from, to));
    }
    return ranges;
  }

  /**
   * Computes mean and standard error for an array of values and appends values
   * to what will be the console output
   * 
   * @param testName
   * @param count
   * @param data
   */
  private boolean logResults(String testName, int count, double[] data)
  {

    if (data == null)
    {
      System.out.println(String.format("%s\t%d", testName, count));
      return false;
    }
    // compute the rates count/data e.g. queries per millisecond

    double[] rate = new double[data.length];
    double totRate = 0D;
    for (int i = 0; i < data.length; i++)
    {
      rate[i] = data[i] == 0 ? 0D : count / data[i];
      totRate += rate[i];
    }

    double totRaw = 0d;
    for (int i = 0; i < data.length; i++)
    {
      totRaw += data[i];
      if (LOG_RAW_DATA)
      {
        String line = String.format("%s\t%d\t%.0f\t%.1f", testName, count,
                data[i], rate[i]);
        System.out.println(line);
      }
    }

    /*
     * calculate mean and standard error of the raw data
     */
    double mean = totRaw / data.length;
    double stderr = standardError(data, mean);
    double rateMean = totRate / data.length;
    double rateStderr = standardError(rate, rateMean);
    String line = String.format("%s\t%d\t%d\t%.1f\t%.1f\t%.2f\t%.2f",
            testName, count, REPEATS, mean / 1000000, rateMean * 1000000,
            stderr / 1000000, rateStderr * 1000000);
    if (LOG_RAW_DATA)
    {
      averages.append(line);
    }
    else
    {
      System.out.println(line);
    }
    return mean / 1e9 < MAX_SEC;
  }

  public void testLoadTimeBulk()
  {

    if (QUERY_ONLY)
      return;

    testBulkLoad(MODE_INTERVAL_STORE_NCLIST);
    if (TEST_STORE0)
      testBulkLoad(MODE_INTERVAL_STORE_NCLIST0);
    testBulkLoad(MODE_INTERVAL_STORE_LINK);
    if (TEST_STORE2)
      testBulkLoad(MODE_INTERVAL_STORE_LINK2);

    if (INTERVAL_STORE_ONLY)
      return;

    testBulkLoad(MODE_NCLIST);
    if (TEST_NCLIST0)
      testBulkLoad(MODE_NCLIST0);

    if (!INCLUDE_NAIVE)
      return;

    testBulkLoad(MODE_NAIVE);
  }

  public void testLoadTimeIncrementalNoDuplicates()
  {

    if (QUERY_ONLY)
      return;

    testIncrLoad(MODE_INTERVAL_STORE_NCLIST, false);
    if (TEST_STORE0)
      testIncrLoad(MODE_INTERVAL_STORE_NCLIST0, false);
    testIncrLoad(MODE_INTERVAL_STORE_LINK, false);
    if (TEST_STORE2)
      testIncrLoad(MODE_INTERVAL_STORE_LINK2, false);

    if (INTERVAL_STORE_ONLY)
      return;

    testIncrLoad(MODE_NCLIST, false);
    if (TEST_NCLIST0)
      testIncrLoad(MODE_NCLIST0, false);

    if (!INCLUDE_NAIVE)
      return;

    testIncrLoad(MODE_NAIVE, false);
  }

  public void testLoadTimeIncrementalAllowDulicates()
  {

    if (QUERY_ONLY)
      return;

    testIncrLoad(MODE_INTERVAL_STORE_NCLIST, true);
    if (TEST_STORE0)
      testIncrLoad(MODE_INTERVAL_STORE_NCLIST0, true);
    testIncrLoad(MODE_INTERVAL_STORE_LINK, true);
    if (TEST_STORE2)
      testIncrLoad(MODE_INTERVAL_STORE_LINK2, true);

    if (INTERVAL_STORE_ONLY)
      return;

    testIncrLoad(MODE_NCLIST, true);
    if (TEST_NCLIST0)
      testIncrLoad(MODE_NCLIST0, true);

    if (!INCLUDE_NAIVE)
      return;

    testIncrLoad(MODE_NAIVE, true);
  }

  private int[] hashcodes = new int[MAX_LOGN + 1];

  private int[] resultcounts = new int[MAX_LOGN + 1];

  public void testQueryTime()
  {
    testQuery(MODE_INTERVAL_STORE_NCLIST);
    if (TEST_STORE0)
      testQuery(MODE_INTERVAL_STORE_NCLIST0);

    testQuery(MODE_INTERVAL_STORE_LINK);
    if (TEST_STORE2)
      testQuery(MODE_INTERVAL_STORE_LINK2);

    if (!INTERVAL_STORE_ONLY)
    {

    testQuery(MODE_NCLIST);
    if (TEST_NCLIST0)
      testQuery(MODE_NCLIST0);

      if (INCLUDE_NAIVE)
    testQuery(MODE_NAIVE);
    }
    System.out.println("# resultcounts " + Arrays.toString(resultcounts));
  }

  public void testRemoveTime()
  {

    if (QUERY_ONLY)
      return;

    testRemove(MODE_INTERVAL_STORE_NCLIST);
    if (TEST_STORE0)
      testRemove(MODE_INTERVAL_STORE_NCLIST0);
    testRemove(MODE_INTERVAL_STORE_LINK);
    if (TEST_STORE2)
      testRemove(MODE_INTERVAL_STORE_LINK2);

    if (INTERVAL_STORE_ONLY)
      return;

    testRemove(MODE_NCLIST);
    if (TEST_NCLIST0)
      testRemove(MODE_NCLIST0);

    if (!INCLUDE_NAIVE)
      return;

    testRemove(MODE_NAIVE);
  }

  private void testBulkLoad(String mode)
  {
    System.out.println("# bulk load " + mode);
    rand = new Random(RANDOM_SEED);
    String testName = mode + " bulk load";
    boolean ok = true;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!ok)
      {
        logResults(testName, count, null);
        continue;
      }
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        long now = System.nanoTime();
        switch (mode)
        {
        case MODE_INTERVAL_STORE_NCLIST:
          new intervalstore.impl0.IntervalStore<>(ranges);
          break;
        case MODE_INTERVAL_STORE_NCLIST0:
          new intervalstore.impl0.IntervalStore<>(ranges);
          break;
        case MODE_INTERVAL_STORE_LINK2:
          new intervalstore.nonc.IntervalStore2<>(ranges);
          break;
        case MODE_INTERVAL_STORE_LINK:
          new intervalstore.nonc.IntervalStore0<>(ranges);
          break;
        case MODE_NCLIST:
          new NCList<>(ranges);
          break;
        case MODE_NCLIST0:
          new intervalstore.impl0.NCList<>(ranges);
          break;
        case MODE_NAIVE:
          List<Range> simple = new ArrayList<>();
          simple.addAll(ranges);
          simple.sort(naiveComp);
          break;
        }
        long elapsed = System.nanoTime() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      ok = logResults(testName, count, data);
    }

  }

  private void testIncrLoad(String mode, boolean allowDuplicates)
  {
    System.out.println(
            "# incr allowDuplicates:" + allowDuplicates + " " + mode);
    rand = new Random(RANDOM_SEED);
    String testName = mode + " incr load "
            + (allowDuplicates ? "dup" : "nodup");
    boolean ok = true;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!ok)
      {
        logResults(testName, count, null);
        continue;
      }
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        long now = System.nanoTime();
        switch (mode)
        {
        case MODE_INTERVAL_STORE_NCLIST:
          intervalstore.impl0.IntervalStore<Range> store1 = new intervalstore.impl0.IntervalStore<>();
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (allowDuplicates || !store1.contains(r))
            {
              store1.add(r);
            }
          }
          break;
        case MODE_INTERVAL_STORE_NCLIST0:
          intervalstore.impl0.IntervalStore<Range> store0 = new intervalstore.impl0.IntervalStore<>();
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (allowDuplicates || !store0.contains(r))
            {
              store0.add(r);
            }
          }
          break;

        case MODE_INTERVAL_STORE_LINK2:
          intervalstore.nonc.IntervalStore2<Range> store2 = new intervalstore.nonc.IntervalStore2<>();
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (allowDuplicates || !store2.contains(r))
            {
              store2.add(r);
            }
          }
          break;
        case MODE_INTERVAL_STORE_LINK:
          intervalstore.nonc.IntervalStore0<Range> store3 = new intervalstore.nonc.IntervalStore0<>();
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (allowDuplicates || !store3.contains(r))
            {
              store3.add(r);
            }
          }
          break;
        case MODE_NCLIST:
          NCList<Range> nclist = new NCList<>(ranges);
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (allowDuplicates || !nclist.contains(r))
            {
              nclist.add(r);
            }
          }
          break;
        case MODE_NCLIST0:
          intervalstore.impl0.NCList<Range> nclist0 = new intervalstore.impl0.NCList<>(
                  ranges);
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (allowDuplicates || !nclist0.contains(r))
            {
              nclist0.add(r);
            }
          }
        case MODE_NAIVE:
          List<Range> simple = new ArrayList<>();
          for (int ir = 0; ir < count; ir++)
          {
            Range r = ranges.get(ir);
            if (!simple.contains(r))
            {
              simple.add(r);
              // should do this here, but it would be prohibitive, even for just
              // one iteration
              // simple.sort(naiveComp);
            }
          }
          simple.sort(naiveComp);
          break;
        }
        long elapsed = System.nanoTime() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      ok = logResults(testName, count, data);
    }
  }

  private void testQuery(String mode)
  {
    intervalstore.impl0.IntervalStore<Range> store0 = null;
    intervalstore.impl.IntervalStore<Range> store1 = null;
    intervalstore.nonc.IntervalStore2<Range> store2 = null;
    intervalstore.nonc.IntervalStore0<Range> store3 = null;
    intervalstore.impl0.NCList<Range> nclist0 = null;
    NCList<Range> nclist = null;
    List<Range> simple = null;

    System.out.println("# Query " + mode + " store interval size "
            + QUERY_STORE_INTERVAL_SIZE + " store sequence factor "
            + QUERY_STORE_SEQUENCE_SIZE_FACTOR + " query width "
            + QUERY_WINDOW + " query count " + QUERY_COUNT);

    rand = new Random(RANDOM_SEED);
    String testName = mode + " query";
    boolean ok = true;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!ok)
      {
        logResults(testName, count, null);
        continue;
      }

      double[] data = new double[REPEATS];
      List<Range> result = null;
      int sequenceWidth = count * QUERY_STORE_SEQUENCE_SIZE_FACTOR;
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(sequenceWidth, count,
                QUERY_STORE_INTERVAL_SIZE);
        List<Range> queries = generateIntervals(sequenceWidth, QUERY_COUNT,
                QUERY_WINDOW);
        switch (mode)
        {
        case MODE_INTERVAL_STORE_NCLIST0:
          store0 = new intervalstore.impl0.IntervalStore<>(ranges);
          break;
        case MODE_INTERVAL_STORE_NCLIST:
          store1 = new intervalstore.impl.IntervalStore<>(ranges);
          break;
        case MODE_INTERVAL_STORE_LINK2:
          store2 = new intervalstore.nonc.IntervalStore2<>(ranges);
          break;
        case MODE_INTERVAL_STORE_LINK:
          store3 = new intervalstore.nonc.IntervalStore0<>(ranges);
          break;
        case MODE_NCLIST:
          nclist = new NCList<>(ranges);
          break;
        case MODE_NCLIST0:
          nclist0 = new intervalstore.impl0.NCList<>(ranges);
          break;
        case MODE_NAIVE:
          simple = new ArrayList<>();
          simple.addAll(ranges);
          simple.sort(naiveComp);
          break;
        }

        long now = System.nanoTime();

        for (int iq = 0; iq < QUERY_COUNT; iq++)
        {
          Range q = queries.get(iq);
          switch (mode)
          {
          case MODE_INTERVAL_STORE_NCLIST0:
            result = store0.findOverlaps(q.getBegin(), q.getEnd());
            break;
          case MODE_INTERVAL_STORE_NCLIST:
            result = store1.findOverlaps(q.getBegin(), q.getEnd());
            break;
          case MODE_INTERVAL_STORE_LINK2:
            result = store2.findOverlaps(q.getBegin(), q.getEnd());
            break;
          case MODE_INTERVAL_STORE_LINK:
            result = store3.findOverlaps(q.getBegin(), q.getEnd());
            break;
          case MODE_NCLIST:
            result = nclist.findOverlaps(q.getBegin(), q.getEnd());
            break;
          case MODE_NCLIST0:
            result = nclist0.findOverlaps(q.getBegin(), q.getEnd());
            break;
          case MODE_NAIVE:
            result = new ArrayList<>();
            for (int ir = ranges.size(); --ir >= 0;)
            {
              Range r = ranges.get(ir);
              if (r.overlapsInterval(q))
              {
                result.add(r);
              }
            }
            break;
          }

        }
        long elapsed = System.nanoTime() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      ok = logResults(testName, count, data);
      // if (QUERY_SHOW_RESULT_COUNT)
      {
        // just check the very last result, to save time
        int ntotal = result.size();
        result.sort(IntervalI.COMPARATOR_BIGENDIAN);
        int hashcode = result.hashCode();
        if (hashcodes[j] == 0)
        {
          resultcounts[j] = ntotal;
          hashcodes[j] = hashcode;
        }
        else
        {
          assertEquals(ntotal, resultcounts[j]);
          assertEquals(hashcode, hashcodes[j]);
        }
        // System.out.println("# results " + ntotal + " 0x"
        // + Integer.toHexString(hashcode));
      }
    }

    switch (mode)
    {
    case MODE_INTERVAL_STORE_NCLIST0:
      System.out.println("# dimensions [" + store0.getDepth() + " "
              + store0.getWidth() + "]");

      break;
    case MODE_INTERVAL_STORE_NCLIST:
      System.out.println("# dimensions [" + store1.getDepth() + " "
              + store1.getWidth() + "]");

      break;
    case MODE_INTERVAL_STORE_LINK2:
      System.out.println("# dimensions [" + store2.getDepth() + " "
              + store2.getWidth() + "]");
      break;
    case MODE_INTERVAL_STORE_LINK:
      System.out.println("# dimensions [" + store3.getDepth() + " "
              + store3.getWidth() + "]");
      break;
    case MODE_NCLIST:
      System.out.println("# dimensions [" + nclist.getDepth() + " "
              + nclist.getWidth() + "]");
      break;
    case MODE_NCLIST0:
      System.out.println(
              "# dimensions [" + nclist0.getDepth() + " " + "?" + "]");
      break;
    case MODE_NAIVE:
      System.out.println("# dimensions [1 " + simple.size() + "]");
      break;
    }

  }

  private void testRemove(String mode)
  {
    System.out.println("# remove " + mode);
    intervalstore.impl0.IntervalStore<Range> store0 = null;
    intervalstore.impl.IntervalStore<Range> store1 = null;
    intervalstore.nonc.IntervalStore2<Range> store2 = null;
    intervalstore.nonc.IntervalStore0<Range> store3 = null;
    intervalstore.impl0.NCList<Range> nclist0 = null;
    NCList<Range> nclist = null;
    List<Range> simple = null;

    rand = new Random(RANDOM_SEED);
    Random rand2 = new Random(RANDOM_SEED);

    String testName = mode + " remove";
    boolean ok = true;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!ok)
      {
        logResults(testName, count, null);
        continue;
      }
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        int[] toDelete = new int[DELETE_COUNT];
        for (int id = 0; id < DELETE_COUNT; id++)
        {
          toDelete[id] = rand2.nextInt(count - DELETE_COUNT);
        }
        switch (mode)
        {
        case MODE_INTERVAL_STORE_NCLIST:
          store1 = new intervalstore.impl.IntervalStore<>(ranges);
          break;
        case MODE_INTERVAL_STORE_NCLIST0:
          store0 = new intervalstore.impl0.IntervalStore<>(ranges);
          break;
        case MODE_INTERVAL_STORE_LINK2:
          store2 = new intervalstore.nonc.IntervalStore2<>(ranges);
          break;
        case MODE_INTERVAL_STORE_LINK:
          store3 = new intervalstore.nonc.IntervalStore0<>(ranges);
          break;
        case MODE_NCLIST:
          nclist = new NCList<>(ranges);
          break;
        case MODE_NCLIST0:
          nclist0 = new intervalstore.impl0.NCList<>(ranges);
          break;
        case MODE_NAIVE:
          simple = new ArrayList<>();
          simple.addAll(ranges);
          simple.sort(naiveComp);
          break;
        }
        long now = System.nanoTime();
        for (int id = 0; id < DELETE_COUNT; id++)
        {
          // allow for random duplicate requests
          Range r = ranges.get(toDelete[id]);
          switch (mode)
          {
          case MODE_INTERVAL_STORE_NCLIST:
            store1.remove(r);
            break;
          case MODE_INTERVAL_STORE_NCLIST0:
            store0.remove(r);
            break;
          case MODE_INTERVAL_STORE_LINK2:
            store2.remove(r);
            break;
          case MODE_INTERVAL_STORE_LINK:
            store3.remove(r);
            break;
          case MODE_NCLIST:
            nclist.remove(r);
            break;
          case MODE_NCLIST0:
            nclist0.remove(r);
            break;
          case MODE_NAIVE:
            ranges.remove(r);
            break;
          }
        }

        long elapsed = System.nanoTime() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      ok = logResults(testName, count, data);
    }

  }

  /**
   * Computes the standard error of a data set
   * 
   * @param data
   * @param mean
   * @return
   */
  private double standardError(double[] data, double mean)
  {
    double sum = 0;
    int n = data.length;
    for (int i = 0; i < n; i++)
    {
      double diff = data[i] - mean;
      sum = sum + diff * diff;
    }
    double stdev = Math.sqrt(sum / (n - 1));
    double stderr = stdev / Math.sqrt(n);

    return stderr;
  }
}
