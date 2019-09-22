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

import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
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
 * @author gmcarstairs
 */
// this is a long running test (c 1 hr) so normally left disabled
@Test(enabled = false)
public class TimingTests
{
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
            "Test\tsize N\titeration\tms\tN/ms\tms stderr\trate stderr");
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
   * Timing tests of loading an NCList, with all intervals loaded in the
   * constructor
   */
  public void testLoadTime_nclist_bulkLoad()
  {
    for (int j = 1; j <= 5; j++)
    {
      int count = j * 100 * 1000; // 100K - 500K
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        NCList<Range> ncl = new NCList<>(ranges);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      logResults("NCList bulk load", count, data);
    }
  }

  /**
   * Generates a list of <code>count</code> intervals of length 1-50 in the
   * range [1, 4*count]
   * 
   * @param count
   * @return
   */
  protected List<Range> generateIntervals(Integer count)
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
  private List<Range> generateIntervals(Integer count, int maxLength)
  {
    int maxPos = 4 * count;
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
   * Timing tests of loading an NCList, with intervals loaded one at a time
   */
  public void testLoadTime_nclist_incremental()
  {
    for (int j = 1; j <= 5; j++)
    {
      int count = j * 100 * 1000; // 100K - 500K
      loadNclist(count, false, "NCList incr");
    }
  }

  /**
   * Timing tests of loading a simple list, with all intervals loaded in the
   * constructor
   */
  public void testLoadTime_naiveList_bulkLoad()
  {
    int[] counts = { 100 * 1000, 500 * 1000 };
    for (int count : counts)
    {
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> simple = new ArrayList<>();
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        simple.addAll(ranges);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      logResults("Naive bulk load", count, data);
    }
  }

  /**
   * Timing tests of loading a simple list, with intervals loaded one at a time
   */
  public void testLoadTime_naiveList_noDuplicates()
  {
    /*
     * N = 100K, 110K, ..., 190K, 200K
     */
    for (int j = 0; j <= 100; j += 10)
    {
      int count = 1000 * (100 + j);
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> simple = new ArrayList<>();
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (Range r : ranges)
        {
          if (!simple.contains(r))
          {
            simple.addAll(ranges);
          }
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      logResults("Naive no duplicates", count, data);
    }
  }

  /**
   * Timing tests of loading an NCList, with intervals loaded one at a time, and
   * a check for duplicates before adding each interval
   */
  public void testLoadTime_nclist_incrementalNoDuplicates()
  {
    for (int j = 1; j <= 5; j++)
    {
      int count = j * 100 * 1000; // 100K - 500K
      loadNclist(count, true, "NCList no duplicates");
    }
  }

  /**
   * Performs a number of repeats of a timing test which adds a number of
   * intervals one at a time to an NCList, optionally testing first for
   * duplicate (i.e. whether the list already contains the interval)
   * 
   * @param count
   * @param allowDuplicates
   * @param testName
   */
  private void loadNclist(Integer count, boolean allowDuplicates,
          String testName)
  {
    double[] data = new double[REPEATS];
    for (int i = 0; i < REPEATS + WARMUPS; i++)
    {
      NCList<Range> ncl = new NCList<>();
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (Range r : ranges)
      {
        if (allowDuplicates || !ncl.contains(r))
        {
          ncl.add(r);
        }
      }
      long elapsed = System.currentTimeMillis() - now;
      if (i >= WARMUPS)
      {
        data[i - WARMUPS] = elapsed;
      }
      assertTrue(ncl.isValid());
    }
    logResults(testName, count, data);
  }

  /**
   * Timing tests of querying an NCList for overlaps
   */
  public void testQueryTime_nclist()
  {
    /*
     * N = 100K, ... 1000K
     */
    for (int k = 1; k <= 10; k++)
    {
      int count = k * 100 * 1000;
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count, 1);
        NCList<Range> ncl = new NCList<>(ranges);

        List<Range> queries = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (Range q : queries)
        {
          ncl.findOverlaps(q.getBegin(), q.getEnd());
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      logResults("NCList query", count, data);
    }
  }

  /**
   * Computes mean and standard error for an array of values and appends values
   * to what will be the console output
   * 
   * @param testName
   * @param count
   * @param data
   */
  private void logResults(String testName, int count, double[] data)
  {
    /*
     * compute the rates count/data e.g. queries per millisecond
     */
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
            testName, count, 0, mean, rateMean, stderr, rateStderr);
    if (LOG_RAW_DATA)
    {
      averages.append(line);
    }
    else
    {
      System.out.println(line);
    }
  }

  /**
   * Timing tests of querying an NCList for overlaps
   */
  public void testQueryTime_naive()
  {
    for (int j = 2; j <= 6; j++)
    {
      int count = j * 10 * 1000; // 20K - 60K
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);

        List<Range> queries = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (Range q : queries)
        {
          findOverlaps(ranges, q);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      logResults("Naive query", count, data);
    }
  }

  /**
   * 'Naive' exhaustive search of an list of intervals for overlaps
   * 
   * @param ranges
   * @param begin
   * @param end
   */
  private List<Range> findOverlaps(List<Range> ranges, Range query)
  {
    List<Range> result = new ArrayList<>();
    for (Range r : ranges)
    {
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
   * @param allowDuplicates
   * @param testName
   */
  private void loadIntervalStore(Integer count, boolean allowDuplicates,
          String testName)
  {
    double[] data = new double[REPEATS];

    for (int i = 0; i < REPEATS + WARMUPS; i++)
    {
      IntervalStore<Range> ncl = new IntervalStore<>();
      List<Range> ranges = generateIntervals(count);
      long now = System.currentTimeMillis();
      for (Range r : ranges)
      {
        if (allowDuplicates || !ncl.contains(r))
        {
          ncl.add(r);
        }
      }
      long elapsed = System.currentTimeMillis() - now;
      if (i >= WARMUPS)
      {
        data[i - WARMUPS] = elapsed;
      }
      assertTrue(ncl.isValid());
    }
    logResults(testName, count, data);
  }

  /**
   * Timing tests of loading an IntervalStore, with all intervals loaded in the
   * constructor
   */
  public void testLoadTime_intervalstore_bulkLoad()
  {
    for (int j = 1; j <= 5; j++)
    {
      int count = j * 100 * 1000; // 100K - 500K
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        long now = System.currentTimeMillis();
        new IntervalStore<>(ranges);
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      logResults("IntervalStore bulk load", count, data);
    }
  }

  /**
   * Timing tests of loading an IntervalStore, with intervals loaded one at a
   * time
   */
  public void testLoadTime_intervalstore_incremental()
  {
    for (int j = 1; j <= 5; j++)
    {
      int count = j * 100 * 1000; // 100K - 500K
      loadIntervalStore(count, false, "IntervalStore incr");
    }
  }

  /**
   * Timing tests of loading an IntervalStore, with intervals loaded one at a
   * time, and a check for duplicates before adding each interval
   */
  public void testLoadTime_intervalstore_incrementalNoDuplicates()
  {
    for (int j = 1; j <= 5; j++)
    {
      int count = j * 100 * 1000; // 100K - 500K
      loadIntervalStore(count, true, "IntervalStore no duplicates");
    }
  }

  /**
   * Timing tests of querying an IntervalStore for overlaps
   */
  public void testQueryTime_intervalstore()
  {
    /*
     * N = 100K, ... 1000K
     */
    for (int k = 1; k <= 10; k++)
    {
      int count = k * 100 * 1000;
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count, 1);
        IntervalStore<Range> ncl = new IntervalStore<>(ranges);
  
        List<Range> queries = generateIntervals(count);
        long now = System.currentTimeMillis();
        for (Range q : queries)
        {
          ncl.findOverlaps(q.getBegin(), q.getEnd());
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      logResults("IntervalStore query", count, data);
    }
  }

  /**
   * Timing tests for deleting from an IntervalStore
   */
  public void testRemoveTime_intervalstore()
  {
    /*
     * time to delete 1000 entries from stores of various sizes N
     */
    final int deleteCount = 1000;
    for (int k = 2; k <= 30; k++)
    {
      /*
       * N = 200K, 300K, ...
       */
      int count = k * 100 * 1000;
      double[] data = new double[REPEATS];
      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        IntervalStore<Range> ncl = new IntervalStore<>(ranges);
  
        /*
         * remove intervals picked pseudo-randomly; attempts to remove the
         * same interval may fail but that doesn't affect the test timings
         */
        long now = System.currentTimeMillis();
        for (int j = 0; j < deleteCount; j++)
        {
          Range toDelete = ranges.get(this.rand.nextInt(count));
          ncl.remove(toDelete);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      logResults("IntervalStore remove", count, data);
    }
  }

  /**
   * Timing tests for deleting from an NCList
   */
  public void testRemoveTime_nclist()
  {
    /*
     * time deleting 1000 entries from stores of various sizes N
     */
    final int deleteCount = 1000;
    for (int k = 2; k <= 30; k++)
    {
      /*
       * N = 200K, ..., 30000K
       */
      int count = k * 100 * 1000;
      double[] data = new double[REPEATS];

      for (int i = 0; i < REPEATS + WARMUPS; i++)
      {
        List<Range> ranges = generateIntervals(count);
        NCList<Range> ncl = new NCList<>(ranges);
  
        /*
         * remove intervals picked pseudo-randomly; attempts to remove the
         * same interval may fail but that doesn't affect the test timings
         */
        long now = System.currentTimeMillis();
        for (int j = 0; j < deleteCount; j++)
        {
          Range toDelete = ranges.get(this.rand.nextInt(count));
          ncl.remove(toDelete);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
        assertTrue(ncl.isValid());
      }
      logResults("NCList remove", count, data);
    }
  }

  /**
   * A sanity check that ArrayList.remove is O(N) (it is)
   */
  public void testRemove_ArrayList()
  {
    final int deleteCount = 1000;
    for (int k = 2; k <= 20; k++)
    {
      /*
       * N = 200K, ..., 2000K
       */
      int count = k * 100 * 1000;
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
        for (int index : toDelete)
        {
          ranges.remove(index);
        }
        long elapsed = System.currentTimeMillis() - now;
        if (i >= WARMUPS)
        {
          data[i - WARMUPS] = elapsed;
        }
      }
      logResults("ArrayList remove", count, data);
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
