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

