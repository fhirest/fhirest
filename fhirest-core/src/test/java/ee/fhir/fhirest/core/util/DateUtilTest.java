/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package ee.fhir.fhirest.core.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DateUtilTest {

  @Test
  public void testPeriods() {
    testPeriods("10", null, null, true);
    testPeriods("10", "05", null, true);
    testPeriods("10", "15", null, false);
    testPeriods("10", "05", "15", true);
    testPeriods("10", "10", "10", true);
    testPeriods("10", null, "15", true);
    testPeriods("10", null, "05", false);
  }

  private void testPeriods(String date, String start, String end, boolean shouldBeInPeriod) {
    assertEquals(shouldBeInPeriod, DateUtil.isInPeriod(toDate(date), toDate(start), toDate(end)));
  }

  private Date toDate(String d) {
    try {
      return d == null ? null : new SimpleDateFormat("ddMMyyyy").parse(d + "012001");
    } catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

}
