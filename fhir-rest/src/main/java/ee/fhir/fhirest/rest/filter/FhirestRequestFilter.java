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

package ee.fhir.fhirest.rest.filter;

import ee.fhir.fhirest.rest.model.FhirestRequest;

/**
 *   <p>Called after HTTP request is accepted and before any work on resource is started. May be used for authentication, logging, etc.</p>
 *   <p>Implementations are called in order according to `getOrder` method.</p>
 *   <p>You can do any changes to request object, containing HTTP request data.</p>
 *   <p>Throwing exception is also allowed.</p>
 */
public interface FhirestRequestFilter {
  Integer RECEIVE = 10;
  Integer READ = 20;
  Integer VALIDATE = 30;

  /**
   * Less = earlier
   */
  default Integer getOrder() {
    return 40;
  }

  void handleRequest(FhirestRequest request);
}
