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
 package ee.fhir.fhirest.auth.smart;

import ee.fhir.fhirest.core.exception.FhirException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class SmartScopeTest {

  @Test
  public void parser() {
    fails(null);
    fails("");
    fails("a");
    fails("a/b");
    fails("a.b");
    fails("a/.c");
    fails("/b.c");
    fails("a/b.");
    fails("a/b.");
    test("a/b.c", "a", "b", "c");
    test("a/*.c", "a", "*", "c");
  }

  private void fails(String input) {
    try {
      new SmartScope(input);
    } catch (FhirException e) {
      //ok
      System.out.println("a");
    }

  }

  private void test(String input, String a, String b, String c) {
    SmartScope s = new SmartScope(input);
    Assertions.assertEquals(s.getContext(), a);
    Assertions.assertEquals(s.getResourceType(), b);
    Assertions.assertEquals(s.getPermissions(), c);
  }
}
