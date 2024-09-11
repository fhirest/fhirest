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

package ee.fhir.fhirest.core.api.resource;

import ee.fhir.fhirest.core.model.ResourceId;
import ee.fhir.fhirest.core.model.ResourceVersion;
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.structure.api.ResourceContent;

/**
 * <p>Interceptor implementations called after resource saved in database.</p>
 *
 * @see ee.fhir.fhirest.core.service.resource.ResourceService
 * @see ResourceService#save(ResourceId, ResourceContent, String)
 */
public abstract class ResourceAfterSaveInterceptor {
  /**
   * <p><b>1</b></p>
   * <p>After resource is saved, but inside the transaction</p>
   */
  public static final String TRANSACTION = "TRANSACTION";
  /**
   * <p><b>2</b></p>
   * <p>After all operations are finished</p>
   */
  public static final String FINALIZATION = "FINALIZATION";

  private final String phase;

  /**
   * @param phase A phase to run this in. Possible values:
   * <ol>
   *   <li>{@link ResourceAfterSaveInterceptor#TRANSACTION}</li>
   *   <li>{@link ResourceAfterSaveInterceptor#FINALIZATION}</li>
   * </ol>
   */
  public ResourceAfterSaveInterceptor(String phase) {
    this.phase = phase;
  }

  /**
   * @see ResourceAfterSaveInterceptor#ResourceAfterSaveInterceptor(String)
   */
  public String getPhase() {
    return phase;
  }

  /**
   * @param version Saved resource
   */
  public abstract void handle(ResourceVersion version);
}
