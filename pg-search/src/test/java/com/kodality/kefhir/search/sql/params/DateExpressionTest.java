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
 package com.kodality.kefhir.search.sql.params;

import com.kodality.kefhir.TestConformanceHolder;
import com.kodality.kefhir.core.model.search.QueryParam;
import com.kodality.kefhir.search.model.Blindex;
import com.kodality.kefhir.search.repository.BlindexRepository;
import com.kodality.kefhir.util.sql.SqlBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;
import org.hl7.fhir.r5.model.CodeType;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.SearchParameter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateExpressionTest {
  private final DateExpressionProvider testMe = new DateExpressionProvider();

  @BeforeEach
  public void mocks() {
    SearchParameter sp = new SearchParameter();
    sp.setBase(Collections.singletonList(new CodeType("NotAResource")));
    sp.setCode("barabashka");
    sp.setExpression("NotAResource.h.o.y");
    TestConformanceHolder.apply(sp);

    new BlindexRepository() {
      @Override
      public java.util.List<Blindex> loadIndexes() {
        Blindex b = new Blindex();
        b.setResourceType("NotAResource");
        b.setPath("h.o.y");
        b.setName("date_index_table");
        return Collections.singletonList(b);
      };
    }.refreshCache();
  }

  @Test
  public void test() {
    TimeZone.setDefault(TimeZone.getTimeZone("Europe/Tallinn"));
    test(null, null);
    test("", null);
    test("2000", "i.range && search.range('2000-01-01T00:00:00+02:00', '1 year')");
    test("le2000", "(i.range && search.range('2000-01-01T00:00:00+02:00', '1 year') OR i.range << search.range('2000-01-01T00:00:00+02:00', '1 year'))");
    test("lt2000", "i.range << search.range('2000-01-01T00:00:00+02:00', '1 year')");
    test("ge2000", "(i.range && search.range('2000-01-01T00:00:00+02:00', '1 year') OR i.range >> search.range('2000-01-01T00:00:00+02:00', '1 year'))");
    test("gt2000", "i.range >> search.range('2000-01-01T00:00:00+02:00', '1 year')");
    test("2000-11", "i.range && search.range('2000-11-01T00:00:00+02:00', '1 month')");
    test("2000-11-11", "i.range && search.range('2000-11-11T00:00:00+02:00', '1 day')");
    test("2000-11-11T11", "i.range && search.range('2000-11-11T11:00:00+02:00', '1 hour')");
    test("2000-11-11T11:11", "i.range && search.range('2000-11-11T11:11:00+02:00', '1 minute')");
    test("2000-11-11T11:11:11", "i.range && search.range('2000-11-11T11:11:11+02:00', '1 second')");

    test("2000-11-11T11:11:11Z", "i.range && search.range('2000-11-11T11:11:11+00:00', '1 second')");
    test("2000-11-11T11:11:11+07:00", "i.range && search.range('2000-11-11T11:11:11+07:00', '1 second')");
  }

  private void test(String input, String expectedCondition) {
    QueryParam param = new QueryParam("barabashka", null, SearchParamType.DATE, "NotAResource");
    param.setValues(Arrays.asList(input));
    SqlBuilder result = testMe.makeExpression(param, "a");
    if (expectedCondition == null) {
      Assertions.assertEquals("", result.getSql());
    } else {
      String expected =
          String.format("EXISTS (SELECT 1 FROM search.date_index_table i WHERE i.active = true and i.sid = a.sid  AND %s)", expectedCondition);
      Assertions.assertEquals(expected, result.getSql());
    }
  }
}
