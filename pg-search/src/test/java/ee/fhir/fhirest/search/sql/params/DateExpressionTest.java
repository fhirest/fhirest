/*
 * MIT License
 *
 * Copyright (c) 2024 FhirEST community
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

package ee.fhir.fhirest.search.sql.params;

import ee.fhir.fhirest.TestConformanceHolder;
import ee.fhir.fhirest.core.model.search.QueryParam;
import ee.fhir.fhirest.search.model.Blindex;
import ee.fhir.fhirest.search.repository.BlindexRepository;
import ee.fhir.fhirest.util.sql.SqlBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.TimeZone;
import org.hl7.fhir.r5.model.Enumerations.SearchParamType;
import org.hl7.fhir.r5.model.Enumerations.VersionIndependentResourceTypesAllEnumFactory;
import org.hl7.fhir.r5.model.SearchParameter;
import org.hl7.fhir.r5.model.StringType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DateExpressionTest {
  private final DateExpressionProvider testMe = new DateExpressionProvider();

  @BeforeEach
  public void mocks() {
    SearchParameter sp = new SearchParameter();
    sp.setBase(Collections.singletonList(new VersionIndependentResourceTypesAllEnumFactory().fromType(new StringType("Patient"))));
    sp.setCode("barabashka");
    sp.setExpression("Patient.h.o.y");
    TestConformanceHolder.apply(sp);

    new BlindexRepository() {
      @Override
      public java.util.List<Blindex> loadIndexes() {
        Blindex b = new Blindex();
        b.setResourceType("Patient");
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
    QueryParam param = new QueryParam("barabashka", null, SearchParamType.DATE, "Patient");
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
