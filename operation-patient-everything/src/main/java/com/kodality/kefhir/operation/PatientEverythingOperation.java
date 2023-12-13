package com.kodality.kefhir.operation;

import com.kodality.kefhir.core.api.resource.InstanceOperationDefinition;
import com.kodality.kefhir.core.model.ResourceId;
import com.kodality.kefhir.core.model.ResourceVersion;
import com.kodality.kefhir.core.model.search.SearchResult;
import com.kodality.kefhir.core.service.resource.ResourceSearchService;
import com.kodality.kefhir.core.service.resource.ResourceService;
import com.kodality.kefhir.rest.util.BundleUtil;
import com.kodality.kefhir.structure.api.ResourceContent;
import com.kodality.kefhir.structure.service.ResourceFormatService;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;
import org.hl7.fhir.r5.model.Bundle.BundleType;
import org.hl7.fhir.r5.model.ResourceType;

@Singleton
@RequiredArgsConstructor
public class PatientEverythingOperation implements InstanceOperationDefinition {
  private final ResourceFormatService formatService;
  private final ResourceService resourceService;
  private final ResourceSearchService resourceSearchService;

  @Override
  public String getResourceType() {
    return ResourceType.Patient.name();
  }

  @Override
  public String getOperationName() {
    return "everything";
  }

  @Override
  public ResourceContent run(ResourceId id, ResourceContent parameters) {
//    Parameters params = formatService.parse(parameters.getValue());
    String pId = id.getResourceId();
    Stream<Supplier<SearchResult>> x = Stream.of(
        () -> resourceSearchService.search("Patient", "_id", pId),
        () -> resourceSearchService.search("RelatedPerson", "patient", pId),
        () -> resourceSearchService.search("EpisodeOfCare", "patient", pId),
        () -> resourceSearchService.search("Encounter", "patient", pId),
        () -> resourceSearchService.search("AllergyIntolerance", "patient", pId),
        () -> resourceSearchService.search("Condition", "patient", pId),
        () -> resourceSearchService.search("Procedure", "patient", pId),
        () -> resourceSearchService.search("Observation", "patient", pId),
        () -> resourceSearchService.search("DiagnosticReport", "patient", pId),
        () -> resourceSearchService.search("MedicationRequest", "patient", pId),
        () -> resourceSearchService.search("MedicationAdministration", "patient", pId),
        () -> resourceSearchService.search("MedicationDispense", "patient", pId),
        () -> resourceSearchService.search("MedicationStatement", "patient", pId),
        () -> resourceSearchService.search("Immunization", "patient", pId),
        () -> resourceSearchService.search("CarePlan", "patient", pId),
        () -> resourceSearchService.search("ServiceRequest", "patient", pId),
        () -> resourceSearchService.search("Coverage", "patient", pId),
        () -> resourceSearchService.search("Claim", "patient", pId)
    );
    List<ResourceVersion> versions = x.map(CompletableFuture::supplyAsync).flatMap(cf -> cf.join().getEntries().stream()).collect(Collectors.toList());
    return formatService.compose(BundleUtil.compose(versions, BundleType.SEARCHSET), "json");
  }
}
