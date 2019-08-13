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
package intervalstore.api;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public interface IntervalI
{

  /**
   * Compare intervals by start position ascending and end position descending.
   */
  static Comparator<? super IntervalI> COMPARATOR_BIGENDIAN = new Comparator<IntervalI>()
  {
    @Override
    public int compare(IntervalI o1, IntervalI o2)
    {
      int ret = Integer.signum(o1.getBegin() - o2.getBegin());
      return (ret == 0 ? Integer.signum(o2.getEnd() - o1.getEnd()) : ret);
    }
  };

  /**
   * Compare intervals by start position ascending and end position ascending.
   */
  static Comparator<? super IntervalI> COMPARATOR_LITTLEENDIAN = new Comparator<IntervalI>()
  {
    @Override
    public int compare(IntervalI o1, IntervalI o2)
    {
      int ret = Integer.signum(o1.getBegin() - o2.getBegin());
      return (ret == 0 ? Integer.signum(o1.getEnd() - o2.getEnd()) : ret);
    }
  };

  /**
   * a comparator for sorting intervals by start position ascending
   */
  static Comparator<? super IntervalI> FORWARD_STRAND = new Comparator<IntervalI>()
  {
    @Override
    public int compare(IntervalI o1, IntervalI o2)
    {
      return Integer.signum(o1.getBegin() - o2.getBegin());
    }
  };

  /**
   * a comparator for sorting intervals by end position descending
   */
  static Comparator<? super IntervalI> REVERSE_STRAND = new Comparator<IntervalI>()
  {
    @Override
    public int compare(IntervalI o1, IntervalI o2)
    {
      return Integer.signum(o2.getEnd() - o1.getEnd());
    }
  };

  static int NOT_CONTAINED = Integer.MIN_VALUE;
  static int CONTAINMENT_UNKNOWN = 0;

  int getBegin();
  int getEnd();

  /**
   * Answers true if this interval contains (or matches) the given interval
   * based solely on start and end.
   * 
   * @param i
   * @return
   */
  default boolean containsInterval(IntervalI i)
  {
    return i != null && i.getBegin() >= getBegin()
            && i.getEnd() <= getEnd();
  }

  /**
   * Answers true if this interval properly contains the given interval, that
   * is, it contains it and is larger than it
   * 
   * @param i
   * @return
   */
  default boolean properlyContainsInterval(IntervalI i)
  {
    return containsInterval(i)
            && (i.getBegin() > getBegin() || i.getEnd() < getEnd());
  }

  default boolean equalsInterval(IntervalI i)
  {
    return i != null && i.getBegin() == getBegin()
            && i.getEnd() == getEnd();
  }

  default boolean overlapsInterval(IntervalI i)
  {
    if (i == null)
    {
      return false;
    }
    if (i.getBegin() < getBegin())
    {
      return i.getEnd() >= getBegin();
    }
    if (i.getEnd() > getEnd())
    {
      return i.getBegin() <= getEnd();
    }
    return true; // i internal to this
  }

  /**
   * Sorts the list by start position ascending (if forwardString==true), or by
   * end position descending
   * 
   * @param intervals
   * @param forwardStrand
   */
  static void sortIntervals(List<? extends IntervalI> intervals,
          final boolean forwardStrand)
  {
    Collections.sort(intervals,
            forwardStrand ? FORWARD_STRAND : REVERSE_STRAND);
  }


}
