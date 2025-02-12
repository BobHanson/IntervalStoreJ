/*
 * Copyright (c) 2009, 2013, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package intervalstore.nonc;

import intervalstore.api.IntervalI;

/**
 * A dual pivot quicksort for int[] where the int is a pointer to something for
 * which the value needs to be checked. This class is not used; it was just an
 * idea I was trying. But it is sort of cool, so I am keeping it in the package
 * for possible future use.
 * 
 * Adapted from Java 7 java.util.DualPivotQuicksort -- int[] only. The only
 * difference is that wherever an a[] value is compared, we use val(a[i])
 * instead of a[i] itself. Pretty straightforward. Could be adapted for general
 * use. Why didn't they do this in Java?
 * 
 * val(i) is just a hack here, of course. A more general implementation might
 * use a Function call.
 * 
 * Just thought it was cool that you can do this.
 * 
 * @author Bob Hanson 2019.09.02
 * 
 */

class IntervalEndSorter
{

  private IntervalI[] intervals;

  private int val(int i)
  {
    return intervals[i].getEnd();
  }

  /*
   * Tuning parameters.
   */

  /**
   * The maximum number of runs in merge sort.
   */
  private static final int MAX_RUN_COUNT = 67;

  /**
   * The maximum length of run in merge sort.
   */
  private static final int MAX_RUN_LENGTH = 33;

  /**
   * If the length of an array to be sorted is less than this constant,
   * Quicksort is used in preference to merge sort.
   */
  private static final int QUICKSORT_THRESHOLD = 286;

  /**
   * If the length of an array to be sorted is less than this constant,
   * insertion sort is used in preference to Quicksort.
   */
  private static final int INSERTION_SORT_THRESHOLD = 47;

  /*
   * Sorting methods for seven primitive types.
   */

  /**
   * Sorts the specified range of the array using the given workspace array
   * slice if possible for merging
   *
   * @param a
   *          the array to be sorted
   * @param left
   *          the index of the first element, inclusive, to be sorted
   * @param right
   *          the index of the last element, inclusive, to be sorted
   * @param work
   *          a workspace array (slice)
   * @param workBase
   *          origin of usable space in work array
   * @param workLen
   *          usable size of work array
   */
  void sort(int[] a, IntervalI[] intervals, int len)
  {
    this.intervals = intervals;

    int left = 0, right = len - 1;
    // Use Quicksort on small arrays
    if (right - left < QUICKSORT_THRESHOLD)
    {
      sort(a, left, right, true);
      return;
    }

    /*
     * Index run[i] is the start of i-th run
     * (ascending or descending sequence).
     */
    int[] run = new int[MAX_RUN_COUNT + 1];
    int count = 0;
    run[0] = left;

    // Check if the array is nearly sorted
    for (int k = left; k < right; run[count] = k)
    {
      switch (Integer.signum(val(a[k + 1]) - val(a[k])))
      {
      case 1:
        // ascending
        while (++k <= right && val(a[k - 1]) <= val(a[k]))
          ;
        break;
      case -1:
        // descending
        while (++k <= right && val(a[k - 1]) >= val(a[k]))
          ;
        for (int lo = run[count] - 1, hi = k; ++lo < --hi;)
        {
          int t = a[lo];
          a[lo] = a[hi];
          a[hi] = t;
        }
        break;
      default:
        // equal
        for (int m = MAX_RUN_LENGTH; ++k <= right
                && val(a[k - 1]) == val(a[k]);)
        {
          if (--m == 0)
          {
            sort(a, left, right, true);
            return;
          }
        }
      }

      /*
       * The array is not highly structured,
       * use Quicksort instead of merge sort.
       */
      if (++count == MAX_RUN_COUNT)
      {
        sort(a, left, right, true);
        return;
      }
    }

    // Check special cases
    // Implementation note: variable "right" is increased by 1.
    if (run[count] == right++)
    { // The last run contains one element
      run[++count] = right;
    }
    else if (count == 1)
    { // The array is already sorted
      return;
    }

    // Determine alternation base for merge
    byte odd = 0;
    for (int n = 1; (n <<= 1) < count; odd ^= 1)
      ;

    // Use or create temporary array b for merging
    int[] b; // temp array; alternates with a
    int ao, bo; // array offsets from 'left'
    int blen = right - left; // space needed for b
    int[] work = new int[blen];
    int workBase = 0;
    if (odd == 0)
    {
      System.arraycopy(a, left, work, workBase, blen);
      b = a;
      bo = 0;
      a = work;
      ao = workBase - left;
    }
    else
    {
      b = work;
      ao = 0;
      bo = workBase - left;
    }

    // Merging
    for (int last; count > 1; count = last)
    {
      for (int k = (last = 0) + 2; k <= count; k += 2)
      {
        int hi = run[k], mi = run[k - 1];
        for (int i = run[k - 2], p = i, q = mi; i < hi; ++i)
        {
          if (q >= hi || p < mi && val(a[p + ao]) <= val(a[q + ao]))
          {
            b[i + bo] = a[p++ + ao];
          }
          else
          {
            b[i + bo] = a[q++ + ao];
          }
        }
        run[++last] = hi;
      }
      if ((count & 1) != 0)
      {
        for (int i = right, lo = run[count - 1]; --i >= lo; b[i + bo] = a[i
                + ao])
          ;
        run[++last] = right;
      }
      int[] t = a;
      a = b;
      b = t;
      int o = ao;
      ao = bo;
      bo = o;
    }
  }

  /**
   * Sorts the specified range of the array by Dual-Pivot Quicksort.
   *
   * @param a
   *          the array to be sorted
   * @param left
   *          the index of the first element, inclusive, to be sorted
   * @param right
   *          the index of the last element, inclusive, to be sorted
   * @param leftmost
   *          indicates if this part is the leftmost in the range
   */
  private void sort(int[] a, int left, int right, boolean leftmost)
  {
    int length = right - left + 1;

    // Use insertion sort on tiny arrays
    if (length < INSERTION_SORT_THRESHOLD)
    {
      if (leftmost)
      {
        /*
         * Traditional (without sentinel) insertion sort,
         * optimized for server VM, is used in case of
         * the leftmost part.
         */
        for (int i = left, j = i; i < right; j = ++i)
        {
          int ai = a[i + 1];
          while (val(ai) < val(a[j]))
          {
            a[j + 1] = a[j];
            if (j-- == left)
            {
              break;
            }
          }
          a[j + 1] = ai;
        }
      }
      else
      {
        /*
         * Skip the longest ascending sequence.
         */
        do
        {
          if (left >= right)
          {
            return;
          }
        } while (val(a[++left]) >= val(a[left - 1]));

        /*
         * Every element from adjoining part plays the role
         * of sentinel, therefore this allows us to avoid the
         * left range check on each iteration. Moreover, we use
         * the more optimized algorithm, so called pair insertion
         * sort, which is faster (in the context of Quicksort)
         * than traditional implementation of insertion sort.
         */
        for (int k = left; ++left <= right; k = ++left)
        {
          int a1 = a[k], a2 = a[left];

          if (val(a1) < val(a2))
          {
            a2 = a1;
            a1 = a[left];
          }
          while (val(a1) < val(a[--k]))
          {
            a[k + 2] = a[k];
          }
          a[++k + 1] = a1;

          while (val(a2) < val(a[--k]))
          {
            a[k + 1] = a[k];
          }
          a[k + 1] = a2;
        }
        int last = a[right];

        while (val(last) < val(a[--right]))
        {
          a[right + 1] = a[right];
        }
        a[right + 1] = last;
      }
      return;
    }

    // Inexpensive approximation of length / 7
    int seventh = (length >> 3) + (length >> 6) + 1;

    /*
     * Sort five evenly spaced elements around (and including) the
     * center element in the range. These elements will be used for
     * pivot selection as described below. The choice for spacing
     * these elements was empirically determined to work well on
     * a wide variety of inputs.
     */
    int e3 = (left + right) >>> 1; // The midpoint
    int e2 = e3 - seventh;
    int e1 = e2 - seventh;
    int e4 = e3 + seventh;
    int e5 = e4 + seventh;

    // Sort these elements using insertion sort
    if (val(a[e2]) < val(a[e1]))
    {
      int t = a[e2];
      a[e2] = a[e1];
      a[e1] = t;
    }

    if (val(a[e3]) < val(a[e2]))
    {
      int t = a[e3];
      a[e3] = a[e2];
      a[e2] = t;
      if (val(t) < val(a[e1]))
      {
        a[e2] = a[e1];
        a[e1] = t;
      }
    }
    if (val(a[e4]) < val(a[e3]))
    {
      int t = a[e4];
      a[e4] = a[e3];
      a[e3] = t;
      int vt = val(t);
      if (vt < val(a[e2]))
      {
        a[e3] = a[e2];
        a[e2] = t;
        if (vt < val(a[e1]))
        {
          a[e2] = a[e1];
          a[e1] = t;
        }
      }
    }
    if (val(a[e5]) < val(a[e4]))
    {
      int t = a[e5];
      a[e5] = a[e4];
      a[e4] = t;
      int vt = val(t);
      if (vt < val(a[e3]))
      {
        a[e4] = a[e3];
        a[e3] = t;
        if (vt < val(a[e2]))
        {
          a[e3] = a[e2];
          a[e2] = t;
          if (vt < val(a[e1]))
          {
            a[e2] = a[e1];
            a[e1] = t;
          }
        }
      }
    }

    // Pointers
    int less = left; // The index of the first element of center part
    int great = right; // The index before the first element of right part

    if (val(a[e1]) != val(a[e2]) && val(a[e2]) != val(a[e3])
            && val(a[e3]) != val(a[e4]) && val(a[e4]) != val(a[e5]))
    {
      /*
       * Use the second and fourth of the five sorted elements as pivots.
       * These values are inexpensive approximations of the first and
       * second terciles of the array. Note that pivot1 <= pivot2.
       */
      int pivot1 = val(a[e2]);
      int pivot2 = val(a[e4]);
      int pivot1k = a[e2];
      int pivot2k = a[e4];

      /*
       * The first and the last elements to be sorted are moved to the
       * locations formerly occupied by the pivots. When partitioning
       * is complete, the pivots are swapped back into their final
       * positions, and excluded from subsequent sorting.
       */
      a[e2] = a[left];
      a[e4] = a[right];

      /*
       * Skip elements, which are less or greater than pivot values.
       */
      while (val(a[++less]) < pivot1)
        ;
      while (val(a[--great]) > pivot2)
        ;

      /*
       * Partitioning:
       *
       *   left part           center part                   right part
       * +--------------------------------------------------------------+
       * |  < pivot1  |  pivot1 <= && <= pivot2  |    ?    |  > pivot2  |
       * +--------------------------------------------------------------+
       *               ^                          ^       ^
       *               |                          |       |
       *              less                        k     great
       *
       * Invariants:
       *
       *              all in (left, less)   < pivot1
       *    pivot1 <= all in [less, k)     <= pivot2
       *              all in (great, right) > pivot2
       *
       * Pointer k is the first index of ?-part.
       */
      outer: for (int k = less - 1; ++k <= great;)
      {
        int ak = a[k];
        if (val(ak) < pivot1)
        { // Move a[k] to left part
          a[k] = a[less];
          /*
          * Here and below we use "a[i] = b; i++;" instead
          * of "a[i++] = b;" due to performance issue.
          */
          a[less] = ak;
          ++less;
        }
        else if (val(ak) > pivot2)
        { // Move a[k] to right part
          while (val(a[great]) > pivot2)
          {
            if (great-- == k)
            {
              break outer;
            }
          }
          if (val(a[great]) < pivot1)
          { // a[great] <= pivot2
            a[k] = a[less];
            a[less] = a[great];
            ++less;
          }
          else
          { // pivot1 <= a[great] <= pivot2
            a[k] = a[great];
          }
          /*
          * Here and below we use "a[i] = b; i--;" instead
          * of "a[i--] = b;" due to performance issue.
          */
          a[great] = ak;
          --great;
        }
      }

      // Swap pivots into their final positions
      a[left] = a[less - 1];
      a[less - 1] = pivot1k;
      a[right] = a[great + 1];
      a[great + 1] = pivot2k;

      // Sort left and right parts recursively, excluding known pivots
      sort(a, left, less - 2, leftmost);
      sort(a, great + 2, right, false);

      /*
       * If center part is too large (comprises > 4/7 of the array),
       * swap internal pivot values to ends.
       */
      if (less < e1 && e5 < great)
      {
        /*
         * Skip elements, which are equal to pivot values.
         */
        while (val(a[less]) == pivot1)
        {
          ++less;
        }

        while (val(a[great]) == pivot2)
        {
          --great;
        }

        /*
         * Partitioning:
         *
         *   left part         center part                  right part
         * +----------------------------------------------------------+
         * | == pivot1 |  pivot1 < && < pivot2  |    ?    | == pivot2 |
         * +----------------------------------------------------------+
         *              ^                        ^       ^
         *              |                        |       |
         *             less                      k     great
         *
         * Invariants:
         *
         *              all in (*,  less) == pivot1
         *     pivot1 < all in [less,  k)  < pivot2
         *              all in (great, *) == pivot2
         *
         * Pointer k is the first index of ?-part.
         */
        outer: for (int k = less - 1; ++k <= great;)
        {
          int ak = a[k];
          if (val(ak) == pivot1)
          { // Move a[k] to left part
            a[k] = a[less];
            a[less] = ak;
            ++less;
          }
          else if (val(ak) == pivot2)
          { // Move a[k] to right part
            while (val(a[great]) == pivot2)
            {
              if (great-- == k)
              {
                break outer;
              }
            }
            if (val(a[great]) == pivot1)
            { // a[great] < pivot2
              a[k] = a[less];
              /*
              * Even though a[great] equals to pivot1, the
              * assignment a[less] = pivot1 may be incorrect,
              * if a[great] and pivot1 are floating-point zeros
              * of different signs. Therefore in float and
              * double sorting methods we have to use more
              * accurate assignment a[less] = a[great].
              */
              a[less] = pivot1k;
              ++less;
            }
            else
            { // pivot1 < a[great] < pivot2
              a[k] = a[great];
            }
            a[great] = ak;
            --great;
          }
        }
      }

      // Sort center part recursively
      sort(a, less, great, false);

    }
    else
    { // Partitioning with one pivot
      /*
       * Use the third of the five sorted elements as pivot.
       * This value is inexpensive approximation of the median.
       */
      int pivot = val(a[e3]);

      /*
       * Partitioning degenerates to the traditional 3-way
       * (or "Dutch National Flag") schema:
       *
       *   left part    center part              right part
       * +-------------------------------------------------+
       * |  < pivot  |   == pivot   |     ?    |  > pivot  |
       * +-------------------------------------------------+
       *              ^              ^        ^
       *              |              |        |
       *             less            k      great
       *
       * Invariants:
       *
       *   all in (left, less)   < pivot
       *   all in [less, k)     == pivot
       *   all in (great, right) > pivot
       *
       * Pointer k is the first index of ?-part.
       */
      for (int k = less; k <= great; ++k)
      {
        if (val(a[k]) == pivot)
        {
          continue;
        }
        int ak = a[k];
        if (val(ak) < pivot)
        { // Move a[k] to left part
          a[k] = a[less];
          a[less] = ak;
          ++less;
        }
        else
        { // a[k] > pivot - Move a[k] to right part
          while (val(a[great]) > pivot)
          {
            --great;
          }
          if (val(a[great]) < pivot)
          { // a[great] <= pivot
            a[k] = a[less];
            a[less] = a[great];
            ++less;
          }
          else
          { // a[great] == pivot
            /*
            * Even though a[great] equals to pivot, the
            * assignment a[k] = pivot may be incorrect,
            * if a[great] and pivot are floating-point
            * zeros of different signs. Therefore in float
            * and double sorting methods we have to use
            * more accurate assignment a[k] = a[great].
            */
            // So, guess what?
            //
            // Actually, we do need a[great] for IntervalStore,
            // because here, two, the numbers are not necessarily the same item
            //
            // a[k] = pivot;
            a[k] = a[great];
          }
          a[great] = ak;
          --great;
        }
      }

      /*
       * Sort left and right parts recursively.
       * All elements from center part are equal
       * and, therefore, already sorted.
       */
      sort(a, left, less - 1, leftmost);
      sort(a, great + 1, right, false);
    }
  }

}
