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
