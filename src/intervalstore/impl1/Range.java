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

import intervalstore.api.IntervalI;

/**
 * An immutable data bean that models a start-end range
 */
public class Range implements IntervalI
{

  // no need for final here; these can be fully mutable as long as
  // store.revalidate() is run afterwords

  public int start;

  public int end;


  @Override
  public int getBegin()
  {
    return start;
  }

  @Override
  public int getEnd()
  {
    return end;
  }

  public Range(int i, int j)
  {
    start = i;
    end = j;
  }

  @Override
  public String toString()
  {
    return String.valueOf(start) + "-" + String.valueOf(end);
  }

  @Override
  public int hashCode()
  {
    return start * 31 + end;
  }

  @Override
  public boolean equals(Object o)
  {
    return (o != null && o instanceof Range && equalsInterval((Range) o));
  }

  @Override
  public boolean equalsInterval(IntervalI obj)
  {

    // override equalsInterval, not equals
    return (obj != null && start == ((Range) obj).start
            && end == ((Range) obj).end);

  }

  public void setStart(int pos)
  {
    start = pos;
  }

  public void setEnd(int pos)
  {
    end = pos;
  }


}
