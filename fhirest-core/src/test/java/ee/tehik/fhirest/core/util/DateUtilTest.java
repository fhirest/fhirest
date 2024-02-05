/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
 package ee.tehik.fhirest.core.util;

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
