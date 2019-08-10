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

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;



/**
 * A class with methods to inspect the performance and scalability of loading
 * and querying IntervalStore with no NCList, and also a 'naive' (unordered)
 * list for comparison
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
 * @author gmcarstairs
 */

@Test(enabled = true)
public class NoNCListTimingTests
{

  /**
   * flag for only doing IntervalStore tests
   */
  private static final boolean INTERVAL_ONLY = true;

  private static final boolean INCLUDE_NAIVE = false;

  private static final boolean QUERY_ONLY = true;

  private static final int QUERY_STORE_INTERVAL_SIZE = 50;

  private static final int QUERY_STORE_SIZE_FACTOR = 10; // 0 for maxLength; 1
  // for original

  /**
   * maximum number of milliseconds we are willing to wait; otherwise abort
   * looping
   */
  private static final float MAX_MS = 1000;

  /**
   * initial value for loop [LOG_0, MAX_LOG] inclusive
   */
  private static final int LOG_0 = 10;

  /**
   * final value for loop [LOG_0, MAX_LOG] inclusive
   */
  private static final int MAX_LOGN = 17;

  /**
   * factor in Math.pow(10, j / LOG_F)
   */
  private static final double LOG_F = 3.0;

  /*
   * use a fixed random seed for repeatable tests
   */
  static final int RANDOM_SEED = 732;

  /*
   * repeat count for each test, to check consistency
   */
  static final int REPEATS = 10;

  /*
   * number of iterations to run before starting timings
   */
  static final int WARMUPS = 3;

  /*
   * set true to log raw data and averages, false to 
   * log average and stderr of 10 iterations of each test
   */
  static final boolean LOG_RAW_DATA = false;
  
  private Random rand;

  /*
   * if logging raw data values, collect averages here, and
   * print them out together at the end, to make them
   * easier to select as graph ranges in Excel
   */
  private StringBuilder averages;

  private class SimpleComparator implements Comparator<Range>
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

  private Comparator<Range> simpleComp = new SimpleComparator();

  private final static int ALLOW_DUPLICATES = 1;

  private final static int NO_DUPLICATES = 0;

  private final static int NO_PRESORT = 0;

  private final static int DO_PRESORT = 2;

  /**
   * Prints system information and column headings
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
            + System.getProperty("os.version") + "\n");
    System.out.println(
            "Test      \tsize N\titeration\tms\tN/ms\tms stderr\trate stderr");
  }

  /**
   * Logs the accumulated test averages at the end (if we have been logging raw
   * data)
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
    return generateIntervals(count, 50);
  }

  /**
   * Generates a list of <code>count</code> intervals of length 1:maxLength in
   * the range [1, 4*count]
   * 
   * @param count
   * @param maxLength
   * @return
   */
  private synchronized List<Range> generateIntervals(int count,
          int maxLength)
  {
    int maxPos = 4 * count * (QUERY_STORE_SIZE_FACTOR == 0 ? maxLength
            : QUERY_STORE_SIZE_FACTOR);
    List<Range> ranges = new ArrayList<>();
    for (int j = 0; j < count; j++)
    {
      int from = 1 + rand.nextInt(maxPos);
      int to = from + rand.nextInt(maxLength);
      ranges.add(new Range(from, to));
    }
    return ranges;
  }


  /**
   * Timing tests of loading a simple list, with all intervals loaded in the
   * constructor and sorted
   */
  public synchronized void testLoadTime_naiveList_bulkLoad()
  {
    if (INTERVAL_ONLY || QUERY_ONLY || !INCLUDE_NAIVE)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> simple = new ArrayList<>();
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        simple.addAll(ranges);
        simple.sort(simpleComp);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      if (!logResults("Naive bulk load", count, data))
        break;
    }
  }

  /**
   * Timing tests of loading a simple list, with intervals loaded one at a time
   */
  public synchronized void testLoadTime_naiveList_noDuplicates()
  {
    if (INTERVAL_ONLY || QUERY_ONLY || !INCLUDE_NAIVE)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> simple = new ArrayList<>();
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (int ir = 0; ir < count; ir++)
        {
          Range r = ranges.get(ir);
          if (!simple.contains(r))
          {
            simple.add(r);
          }
        }
        simple.sort(simpleComp);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      if (!logResults("Naive no duplicates", count, data))
        break;
    }
  }

  /**
   * Computes mean and standard error for an array of values and appends values
   * to what will be the console output
   * 
   * @param testName
   * @param count
   * @param data
   * @return false if we don't want to wait any longer
   */
  private synchronized boolean logResults(String testName, int count,
          double[] data)
  {
    /*
     * compute the rates count/data e.g. queries per millisecond
     */
    double[] rate = new double[data.length];
    double totRate = 0;
    for (int i = 0; i < data.length; i++)
    {
      rate[i] = data[i] == 0 ? 0 : count / data[i];
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
            testName, count, REPEATS, mean, rateMean, stderr, rateStderr);
    if (LOG_RAW_DATA)
    {
      averages.append(line);
    }
    else
    {
      System.out.println(line);
    }
    return mean < MAX_MS;
  }

  /**
   * Timing tests of querying a simple ArrayList for overlaps
   */
  public synchronized void testQueryTime_naive()
  {
    if (INTERVAL_ONLY || !INCLUDE_NAIVE)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count,
                QUERY_STORE_INTERVAL_SIZE);

        List<Range> queries = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (int iq = 0; iq < count; iq++)
        {
          findOverlaps(ranges, queries.get(iq));
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      if (!logResults("Naive query", count, data))
        break;
    }
  }

  /**
   * 'Naive' exhaustive search of an list of intervals for overlaps
   * 
   * @param ranges
   * @param begin
   * @param end
   */
  private synchronized List<Range> findOverlaps(List<Range> ranges,
          Range query)
  {
    List<Range> result = new ArrayList<>();
    for (int ir = ranges.size(); --ir >= 0;)
    {
      Range r = ranges.get(ir);
      if (r.overlapsInterval(query))
      {
        result.add(r);
      }
    }
    return result;
  }

  /**
   * Performs a number of repeats of a timing test which adds a number of
   * intervals one at a time to an IntervalStore, optionally testing first for
   * duplicate (i.e. whether the list already contains the interval)
   * 
   * @param count
   * @param mode
   * @param testName
   */
  private synchronized boolean loadIntervalStore(int count,
          int mode,
          String testName)
  {
    double[] data = new double[REPEATS];

    for (int i = 0; i < REPEATS + WARMUPS; i++)
    {
      IntervalStore<Range> ncl = new IntervalStore<>(
              (mode & DO_PRESORT) == DO_PRESORT);
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (int ir = 0; ir < count; ir++)
      {
        Range r = ranges.get(ir);
        if ((mode & ALLOW_DUPLICATES ) == ALLOW_DUPLICATES || !ncl.contains(r))
        {
          ncl.add(r);
        }
      }
      if ((mode & DO_PRESORT) != DO_PRESORT)
      {
        ncl.isValid();
      }
      long elapsed = System.currentTimeMillis() - now;
      if (i >= WARMUPS)
      {
        data[i - WARMUPS] = elapsed;
      }
      assertTrue(ncl.isValid());
    }
    return logResults(testName, count, data);
  }

  /**
   * Timing tests of loading an IntervalStore, with all intervals loaded in the
   * constructor
   */
  public synchronized void testLoadTime_intervalstore_bulkLoad()
  {
    if (QUERY_ONLY)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        new IntervalStore<>(ranges, true);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      if (!logResults("IntervalStore bulk presort", count, data))
        break;
    }
  }

  /**
   * Timing tests of loading an IntervalStore, with all intervals loaded in the
   * constructor
   */
  public synchronized void testLoadTime_intervalstore_bulkLoad_nopresort()
  {
    if (QUERY_ONLY)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        new IntervalStore<>(ranges, false);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      if (!logResults("IntervalStore bulk nopresort", count, data))
        break;
    }
  }


  /**
   * Timing tests of loading an IntervalStore, with intervals loaded one at a
   * time
   */
  public synchronized void testLoadTime_intervalstore_incremental_nodupl_nopresort()
  {
    if (QUERY_ONLY)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!loadIntervalStore(count, NO_DUPLICATES | NO_PRESORT,
              "IntervalStore incr nodupl nosort"))
        break;
    }
  }

  /**
   * Timing tests of loading an IntervalStore, with intervals loaded one at a
   * time
   */
  public synchronized void testLoadTime_intervalstore_incremental_nodupl_presort()
  {
    if (QUERY_ONLY)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!loadIntervalStore(count, NO_DUPLICATES | DO_PRESORT,
              "IntervalStore incr nodupl presort"))
        break;
    }
  }

  /**
   * Timing tests of loading an IntervalStore, with intervals loaded one at a
   * time, and a check for duplicates before adding each interval
   */
  public synchronized void testLoadTime_intervalstore_incr_dupl_presort()
  {
    if (QUERY_ONLY)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!loadIntervalStore(count, ALLOW_DUPLICATES | DO_PRESORT,
              "IntervalStore dupl presort"))
        break;
    }
  }

  /**
   * Timing tests of loading an IntervalStore, with intervals loaded one at a
   * time, and a check for duplicates before adding each interval
   */
  public synchronized void testLoadTime_intervalstore_incr_dupl_nopresort()
  {
    if (QUERY_ONLY)
      return;
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      if (!loadIntervalStore(count, ALLOW_DUPLICATES | NO_PRESORT,
              "IntervalStore dupl nopresort"))
        break;
    }
  }

  /**
   * Timing tests of querying an IntervalStore for overlaps
   */
  public synchronized void testQueryTime_intervalstore()
  {
    for (int j = LOG_0; j <= MAX_LOGN; j++)
    {
      int count = (int) Math.pow(10, j / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count,
                QUERY_STORE_INTERVAL_SIZE);
        IntervalStore<Range> ncl = new IntervalStore<>(ranges);
        ncl.isValid();
        if (j == LOG_0 && i == 0)
          System.out.println("Query interval " + QUERY_STORE_INTERVAL_SIZE
                  + " factor " + QUERY_STORE_SIZE_FACTOR + " dimensions ["
                  + ncl.getDepth() + " " + ncl.getWidth() + "]");
        List<Range> queries = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (int ir = 0; ir < count; ir++)
        {
          Range q= queries.get(ir); 
          ncl.findOverlaps(q.getBegin(), q.getEnd(), null);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      if (!logResults("IntervalStore query", count, data))
        break;
    }
  }

  /**
   * Timing tests for deleting from an IntervalStore
   */
  public synchronized void testRemoveTime_intervalstore()
  {
    if (QUERY_ONLY)
      return;
    /*
     * time to delete 1000 entries from stores of various sizes N
     */
    final int deleteCount = 1000;
    for (int k = LOG_0; k <= MAX_LOGN; k++)
    {
      int count = (int) Math.pow(10, k / LOG_F);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        Range[] list = ranges.toArray(new Range[count]);

        IntervalStore<Range> ncl = new IntervalStore<>(ranges);
  
        /*
         * remove intervals picked pseudo-randomly; attempts to remove the
         * same interval may fail but that doesn't affect the test timings
         */
        long now = System.currentTimeMillis();
        for (int j = 0; j < deleteCount; j++)
        {
          ncl.remove(list[this.rand.nextInt(count)]);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      if (!logResults("IntervalStore remove", count, data))
        break;
    }
  }

  /**
   * A sanity check that ArrayList.remove is O(N) (it is)
   */
  public synchronized void testRemove_ArrayList()
  {
    if (INTERVAL_ONLY || QUERY_ONLY)
      return;
    final int deleteCount = 1000;
    for (int k = LOG_0; k <= MAX_LOGN; k++)
    {
      int count = (int) Math.pow(10, k / LOG_F);
      double[] data = new double[REPEATS];

      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        int[] toDelete = new int[deleteCount];
        for (int j = 0; j < deleteCount; j++)
        {
          toDelete[j] = this.rand.nextInt(count - deleteCount);
        }

        /*
         * remove list entries picked pseudo-randomly
         */
        long now = System.currentTimeMillis();
        for (int id = deleteCount; --id >= 0;)
        {
          ranges.remove(toDelete[id]);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      if (!logResults("ArrayList remove", count, data))
        break;
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
