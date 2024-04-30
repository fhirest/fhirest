/*
 * MIT License
 *
 * Copyright (c) 2024 FHIREST community
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

package ee.fhir.fhirest.structure.util;

import java.util.Date;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r5.model.Period;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StuUtilTest {

  @Test
  public void periodTest() {
    Assertions.assertTrue(StuUtil.isInPeriod(new Date(), null));
    Assertions.assertTrue(StuUtil.isInPeriod(new Date(), new Period()));
    Assertions.assertTrue(StuUtil.isInPeriod(new Date(), new Period().setStart(DateUtils.addHours(new Date(), -1))));
    Assertions.assertFalse(StuUtil.isInPeriod(new Date(), new Period().setStart(DateUtils.addHours(new Date(), 1))));
    Assertions.assertTrue(StuUtil.isInPeriod(new Date(), new Period().setEnd(DateUtils.addHours(new Date(), 1))));
    Assertions.assertFalse(StuUtil.isInPeriod(new Date(), new Period().setEnd(DateUtils.addHours(new Date(), -1))));
  }
}
