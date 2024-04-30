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

package ee.fhir.fhirest.rest;

import ee.fhir.fhirest.core.model.InteractionType;
import ee.fhir.fhirest.rest.interaction.FhirInteraction;
import ee.fhir.fhirest.rest.model.FhirestRequest;
import ee.fhir.fhirest.rest.model.FhirestResponse;

public interface FhirResourceServer {

  String getTargetType();

  @FhirInteraction(interaction = InteractionType.READ, mapping = "GET /{}")
  FhirestResponse read(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.VREAD, mapping = "GET /{}/_history/{}")
  FhirestResponse vread(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.CREATE, mapping = "POST /")
  FhirestResponse create(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.UPDATE, mapping = "PUT /{}")
  FhirestResponse update(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.UPDATE, mapping = "PUT /")
  FhirestResponse conditionalUpdate(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.DELETE, mapping = "DELETE /{}")
  FhirestResponse delete(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.HISTORYINSTANCE, mapping = "GET /{}/_history")
  FhirestResponse history(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.HISTORYTYPE, mapping = "GET /_history")
  FhirestResponse historyType(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "GET /")
  FhirestResponse search(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "GET /{}/{}")
  FhirestResponse searchCompartment(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "POST /_search")
  FhirestResponse search_(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /{}/${}")
  FhirestResponse instanceOperation(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /{}/${}")
  FhirestResponse instanceOperation_(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /${}")
  FhirestResponse typeOperation(FhirestRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /${}")
  FhirestResponse typeOperation_(FhirestRequest req);
}

