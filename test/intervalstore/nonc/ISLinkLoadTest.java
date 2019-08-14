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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipInputStream;

import org.testng.annotations.Test;

import intervalstore.impl.SimpleFeature;
import junit.extensions.PA;

/**
 * Tests that load and analyze real world datasets
 * 
 * @author gmcarstairs
 *
 */
public class ISLinkLoadTest
{
  /*
   * Ensembl and gnomAD variants on human BRAF gene 
   */
  private static final String VARIANTS_FILENAME = "brafVariants.csv";

  /*
   * Coding, non-coding and pseudo-gene start-end loci on each human chromosome 
   */
  private static final String GENES_FILENAME = "humanGenes.csv";


  /**
   * This 'test' loads a file of all human gene locus data, builds an
   * IntervalStore per chromosome, and report the size and depth of each
   * contained NCList
   * 
   * @throws IOException
   */
  @Test(enabled = true) // groups = "Functional")
  public void testIntervalStoreDepth_genes() throws IOException
  {
    System.out.println("\ntestIntervalStoreDepth_genes: start");
    try (BufferedReader br = getReader(GENES_FILENAME + ".zip"))

    {
      String lastChr = null;
      IntervalStore<SimpleFeature> fs = new IntervalStore<>();
      String line = br.readLine();

      while (line != null)
      {
        if (!line.startsWith("#"))
        {
          String[] tokens = line.split("\\,");
          int from = Integer.parseInt(tokens[1]);
          int to = Integer.parseInt(tokens[2]);
          String chr = tokens[3];
          String desc = tokens[4];
          if (lastChr == null)
          {
            lastChr = chr;
          }

          if (!lastChr.equals(chr))
          {
            /*
             * end of chromosome - construct NCList for last chromosome
             */

            if ("2".equals(lastChr) || "5".equals(lastChr))
            {
              // System.out.println(ncl.prettyPrint());
            }
            int size = fs.size();
            int w = fs.getWidth();
            String msg = String.format(
                    "chr%s size=%d, width=%d (%.1f%%), nested = %d, depth=%d",
                    lastChr, size, w, w * 100 / (float) size,
                    (fs.size() - w),
                    fs.getDepth());
            System.out.println(msg);

            lastChr = chr;

            fs = new IntervalStore<>();
          }
          String description = chr + ":" + desc;
          SimpleFeature sf = new SimpleFeature(from, to, description);
          fs.add(sf);
        }
        line = br.readLine();
      }

      int w = fs.getWidth();
      int size = fs.size();
      String msg = String.format(
              "chr%s size=%d, width=%d (%.1f%%), size = %d, depth=%d",
              lastChr, size, w, w * 100 / (float) size, (size - w),
              fs.getDepth());
      System.out.println(msg);
    }

    System.out.println("testIntervalStoreDepth_genes: end\n");
  }

  /**
   * This 'test' loads a file of variants interval data to an IntervalStore then
   * queries the list to report its greatest depth, and number of single-locus
   * entries
   * 
   * @throws IOException
   */
  @Test(enabled = true) // groups = "Functional")
  public void testIntervalStoreDepth_variants() throws IOException
  {
    System.out.println("\ntestIntervalStoreDepth_variants: start");
    List<SimpleFeature> intervals = new ArrayList<>();
    try (BufferedReader br = getReader(VARIANTS_FILENAME + ".zip"))
    {
      int snvCount = 0;
      int colocatedCount = 0;
      SimpleFeature lastFeature = null;

      String line = br.readLine();
      while (line != null)
      {
        if (!line.startsWith("#"))
        {
          String[] tokens = line.split("\\,");
          int from = Integer.parseInt(tokens[0]);
          int to = Integer.parseInt(tokens[1]);
          /*
           * in BED format, 'to' is exclusive - adjust
           */
          if (to > from)
          {
            to--;
          }
          String desc = tokens[2];
          SimpleFeature feature = new SimpleFeature(from, to, desc);
          intervals.add(feature);
          if (from == to)
          {
            snvCount++;
          }
          if (lastFeature != null && lastFeature.equalsInterval(feature))
          {
            colocatedCount++;
          }
          lastFeature = feature;
        }
        line = br.readLine();
      }

      System.out.println(String.format(
              "Found %d variants including %d SNVs and %d colocated",
              intervals.size(), snvCount, colocatedCount));
      // IntervalStore2<SimpleFeature> ncl = buildIntervalStore(
      // intervals,
      // "Variants");
      // System.out.println(ncl.prettyPrint());
    }

    System.out.println("testIntervalStoreDepth_variants: end\n");
  }

  /**
   * Helper method that constructs an IntervalStore from the given intervals,
   * and reports its size, width and depth
   * 
   * @param intervals
   * @param title
   */
  protected IntervalStore<SimpleFeature> buildIntervalStore(
          List<SimpleFeature> intervals, String title)
  {
    IntervalStore<SimpleFeature> store = new IntervalStore<>(
            intervals);
    assertTrue(store.isValid());
    @SuppressWarnings("unchecked")
    int w = ((List<SimpleFeature>) PA.getValue(store, "nonNested")).size();
    String msg = String.format(
            "%s size=%d, width=%d (%.1f%%), depth=%d", title,
            store.size(), w, w * 100 / (float) store.size(),
            store.getDepth());
    System.out.println(msg);

    return store;
  }

  private BufferedReader getReader(String f) throws IOException
  {
    InputStream bzis = getClass().getResourceAsStream(f);
    ZipInputStream zis = new ZipInputStream(new BufferedInputStream(bzis));
    zis.getNextEntry();
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final byte[] buf = new byte[1024];
    int length;
    while ((length = zis.read(buf, 0, buf.length)) >= 0)
    {
      bos.write(buf, 0, length);
    }
    return new BufferedReader(
            new InputStreamReader(
                    new ByteArrayInputStream(bos.toByteArray())));
  }

}
