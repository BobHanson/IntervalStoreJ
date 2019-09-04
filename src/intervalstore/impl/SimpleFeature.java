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

import intervalstore.api.IntervalI;

/**
 * A simplified feature instance sufficient for unit test purposes
 */
public class SimpleFeature extends Range
{

  private String description;


  /**
   * Constructor
   * 
   * @param from
   * @param to
   * @param desc
   */
  public SimpleFeature(int from, int to, String desc)
  {
    super(from, to);
    description = desc;
  }

  /**
   * Copy constructor
   * 
   * @param sf1
   */
  public SimpleFeature(SimpleFeature sf1)
  {
    this(sf1.start, sf1.end, sf1.description);
  }

  public String getDescription()
  {
    return description;
  }

  @Override
  public int hashCode()
  {
    return start + 37 * end
            + (description == null ? 0 : description.hashCode());
  }

  @Override
  public boolean equals(Object o)
  {
    return (o != null && o instanceof SimpleFeature
            && equalsInterval((SimpleFeature) o));
  }

  /**
   * Equals method that requires two instances to have the same description, as
   * well as start and end position. Does not do a test for null
   */
  @Override
  public boolean equalsInterval(IntervalI o)
  {
    // must override equalsInterval, not equals
    return (o != null && start == ((SimpleFeature) o).start
            && end == ((SimpleFeature) o).end)
            && (description == null
                    ? ((SimpleFeature) o).description == null
                    : description.equals(((SimpleFeature) o).description));
  }

  @Override
  public String toString()
  {
    return start + ":" + end + ":" + description;
  }


}
