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

package ee.fhir.fhirest.core.api.resource;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.structure.api.ResourceContent;

public abstract class ResourceBeforeSaveInterceptor {
  public static final String INPUT_VALIDATION = "INPUT_VALIDATION"; //structure validations. make sure input is structurally correct
  public static final String NORMALIZATION = "NORMALIZATION";//modify resource if required

  public static final String BUSINESS_VALIDATION = "BUSINESS_VALIDATION";
  public static final String TRANSACTION = "TRANSACTION";

  private final String phase;

  public ResourceBeforeSaveInterceptor(String phase) {
    this.phase = phase;
  }

  public String getPhase() {
    return phase;
  }

  public abstract void handle(ResourceId id, ResourceContent content, String interaction);
}
