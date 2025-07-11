/*
 * MIT License
 *
 * Copyright (c) 2024 FHIRest community
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

package ee.fhir.fhirest.core.util;

import ee.fhir.fhirest.core.exception.FhirException;
import ee.fhir.fhirest.core.exception.FhirestIssue;
import ee.fhir.fhirest.core.model.VersionId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

public final class ResourceUtil {

  private static final String HISTORY = "_history";

  private ResourceUtil() {
    // no init
  }

  public static VersionId parseReference(String uri) {
//    if (StringUtils.isEmpty(uri) || uri.startsWith("#")) {
//      return null;
//    }
    Validate.isTrue(StringUtils.isNotEmpty(uri));
    String[] tokens = StringUtils.split(uri, "/");
    VersionId id = new VersionId(tokens[0]);
    if (tokens.length > 1) {
      id.setResourceId(tokens[1]);
    }
    if (tokens.length > 3) {
      if (!HISTORY.equals(tokens[2])) {
        throw new FhirException(FhirestIssue.FEST_036, "ref", uri);
      }
      id.setVersion(Integer.valueOf(tokens[3]));
    }
    return id;
  }

  public static List<VersionId> filterUnique(List<VersionId> versions) {
    Set<String> seen = new HashSet<>();
    return versions.stream()
        .filter(version -> seen.add(version.getReference()))
        .toList();
  }

}
