package ee.fhir.fhirest.core.api.resource;

public interface BaseOperationDefinition {
  String getResourceType();
  String getOperationName();
}
