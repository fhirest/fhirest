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

package ee.fhir.fhirest.auth.smart;

import ee.fhir.fhirest.core.exception.FhirException;
import lombok.Getter;
import org.hl7.fhir.r5.model.OperationOutcome.IssueType;

@Getter
public class SmartScope {
  private final String context; //patient, user...
  private final String resourceType;
  private final String permissions;

  public SmartScope(String scope) {
    if (scope == null) {
      throw new FhirException(400, IssueType.PROCESSING, "empty auth scope arg");
    }

    int s = scope.indexOf("/");
    int d = scope.indexOf(".");

    if (s <= 0 || d <= s) {
      throw new FhirException(400, IssueType.PROCESSING, "invalid auth scope arg");
    }

    this.context = scope.substring(0, s);
    this.resourceType = scope.substring(s + 1, d);
    this.permissions = scope.substring(d + 1);
  }

}
