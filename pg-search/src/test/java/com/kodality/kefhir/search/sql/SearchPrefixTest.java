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
 package com.kodality.kefhir.search.sql;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SearchPrefixTest {
  private static final String[] prefixes = new String[] { SearchPrefix.eq, SearchPrefix.le };

  @Test
  public void test() {
    test(null, null, null);
    test("", null, "");
    test("2", null, "2");
    test("eq2", "eq", "2");
    test("le2", "le", "2");
    test("le", "le", "");
    test("eqle2", "eq", "le2");
  }

  private void test(String value, String expectedPrefix, String expectedValue) {
    SearchPrefix result = SearchPrefix.parse(value, prefixes);
    Assertions.assertEquals(result.getPrefix(), expectedPrefix);
    Assertions.assertEquals(result.getValue(), expectedValue);
  }
}
