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
package ee.tehik.fhirest.structure.util;

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
