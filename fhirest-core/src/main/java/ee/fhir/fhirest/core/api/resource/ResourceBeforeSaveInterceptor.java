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
import ee.fhir.fhirest.core.service.resource.ResourceService;
import ee.fhir.fhirest.structure.api.ResourceContent;

/**
 * <p>Interceptor implementations called before resource saved in database.</p>
 *
 * @see ee.fhir.fhirest.core.service.resource.ResourceService
 * @see ResourceService#save(ResourceId, ResourceContent, String)
 */
public abstract class ResourceBeforeSaveInterceptor {
  /**
   * <p><b>1</b></p>
   * <p>Structure validations. Make sure input is structurally correct</p>
   */
  public static final String INPUT_VALIDATION = "INPUT_VALIDATION";

  /**
   * <p><b>2</b></p>
   * Modify resource if required
   */
  public static final String NORMALIZATION = "NORMALIZATION";

  /**
   * <p><b>3</b></p>
   * Custom business validations before transaction starts
   */
  public static final String BUSINESS_VALIDATION = "BUSINESS_VALIDATION";

  /**
   * <p><b>4</b></p>
   * Inside main resource transaction
   */
  public static final String TRANSACTION = "TRANSACTION";

  private final String phase;

  /**
   * @param phase A phase to run this in. Possible values:
   * <ol>
   *   <li>{@link ResourceBeforeSaveInterceptor#INPUT_VALIDATION}</li>
   *   <li>{@link ResourceBeforeSaveInterceptor#NORMALIZATION}</li>
   *   <li>{@link ResourceBeforeSaveInterceptor#BUSINESS_VALIDATION}</li>
   *   <li>{@link ResourceBeforeSaveInterceptor#TRANSACTION}</li>
   * </ol>
   */
  public ResourceBeforeSaveInterceptor(String phase) {
    this.phase = phase;
  }

  /**
   * @see ResourceBeforeSaveInterceptor#ResourceBeforeSaveInterceptor(String)
   */
  public String getPhase() {
    return phase;
  }

  /**
   * @param id          Resource id. May be empty on <i>create</i> interaction
   * @param content     Resource content
   * @param interaction FHIR interaction
   */
  public abstract void handle(ResourceId id, ResourceContent content, String interaction);
}
