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

package ee.fhir.fhirest.auth.http;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class HttpAuthorizationTest {

  @Test
  public void httpAuthorizationTest() {
    test(null);
    test("");
    test("invalid");
    test("Basic 123", new HttpAuthorization("Basic", "123"));
    test("Basic 123, Bearer 321", new HttpAuthorization("Basic", "123"), new HttpAuthorization("Bearer", "321"));
    test("Bearer 321, Basic 123", new HttpAuthorization("Bearer", "321"), new HttpAuthorization("Basic", "123"));
  }

  private void test(String header, HttpAuthorization... expect) {
    List<HttpAuthorization> result = HttpAuthorization.parse(Collections.singletonList(header));
    Assertions.assertEquals(expect.length, result.size());
    IntStream.range(0, result.size()).forEach(i -> {
      Assertions.assertEquals(expect[i].getType(), result.get(i).getType());
      Assertions.assertEquals(expect[i].getCredential(), result.get(i).getCredential());
    });
  }
}
