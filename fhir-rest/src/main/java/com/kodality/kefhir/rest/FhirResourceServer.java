package com.kodality.kefhir.rest;

import com.kodality.kefhir.core.model.InteractionType;
import com.kodality.kefhir.rest.interaction.FhirInteraction;
import com.kodality.kefhir.rest.model.KefhirRequest;
import com.kodality.kefhir.rest.model.KefhirResponse;

public interface FhirResourceServer {

  String getTargetType();

  @FhirInteraction(interaction = InteractionType.READ, mapping = "GET /{}")
  KefhirResponse read(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.VREAD, mapping = "GET /{}/_history/{}")
  KefhirResponse vread(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.CREATE, mapping = "POST /")
  KefhirResponse create(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.UPDATE, mapping = "PUT /{}")
  KefhirResponse update(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.UPDATE, mapping = "PUT /")
  KefhirResponse conditionalUpdate(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.DELETE, mapping = "DELETE /{}")
  KefhirResponse delete(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.HISTORYINSTANCE, mapping = "GET /{}/_history")
  KefhirResponse history(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.HISTORYTYPE, mapping = "GET /_history")
  KefhirResponse historyType(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "GET /")
  KefhirResponse search(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.SEARCHTYPE, mapping = "POST /_search")
  KefhirResponse search_(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /{}/${}")
  KefhirResponse instanceOperation(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /{}/${}")
  KefhirResponse instanceOperation_(String resourceId, String operation);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "POST /${}")
  KefhirResponse typeOperation(KefhirRequest req);

  @FhirInteraction(interaction = InteractionType.OPERATION, mapping = "GET /${}")
  KefhirResponse typeOperation_(KefhirRequest req);
}

