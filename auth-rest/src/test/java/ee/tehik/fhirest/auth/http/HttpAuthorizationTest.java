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
 package ee.tehik.fhirest.auth.http;

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
