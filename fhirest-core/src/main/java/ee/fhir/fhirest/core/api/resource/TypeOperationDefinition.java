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

import ee.fhir.fhirest.structure.api.ResourceContent;

/**
 * Interface should be used to provide actual functionality of an <b>type</b> level operation.
 * @see OperationDefinition
 */
public interface TypeOperationDefinition extends OperationDefinition {
  /**
   * @return FHIR resource type this operation is applied to
   */
  String getResourceType();

  /**
   * @param parameters Unparsed resource of <b>Parameters</b> resource type
   * @return <b>Parameters</b> or <b>Resource</b>, as in <a href="https://www.hl7.org/fhir/operations.html#response">https://www.hl7.org/fhir/operations.html#response</a>
   */
  ResourceContent run(ResourceContent parameters);

}
