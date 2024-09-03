# Fhir REST api

This module responsible for starting and managing REST endpoints.  
Starts `/fhir` endpoint to accept FHIR requests, process on HTTP layer and forward them to [fhirest-core](../fhirest-core).   
Example FHIR url would be http://localhost:8181/fhir/Patient.

By default, it will read CapabilityStatement and StructureDefinition resources and start REST services accordingly.

## Installation
1. Add gradle dependency

```
implementation "ee.fhir.fhirest:fhir-rest:${fhirestVersion}"
```

## Custom implementation

By default, all fhir interactions will be processed by [DefaultFhirResourceServer](./src/main/java/ee/fhir/fhirest/rest/DefaultFhirResourceServer.java)  
This is overridable per resource type simply by providing your own implementation
of [FhirResourceServer](./src/main/java/ee/fhir/fhirest/rest/FhirResourceServer.java)
Example [FhirBinaryServer](./src/main/java/ee/fhir/fhirest/rest/resource/FhirBinaryServer.java)

## Filters

* [FhirestRequestFilter](./src/main/java/ee/fhir/fhirest/rest/filter/FhirestRequestFilter.java)  
  Called after HTTP request is accepted and before any work on resource is started. May be used for authentication, logging, etc.  
  Implementations are called in order according to `getOrder` method.
  You can do any changes to request object, containing HTTP request data.  
  Throwing exception is also allowed.  
  [Example](./src/main/java/ee/fhir/fhirest/rest/filter/ContentTypeValidationFilter.java)
* [FhirestResponseFilter](./src/main/java/ee/fhir/fhirest/rest/filter/FhirestResponseFilter.java)  
  All implementations called before response is flushed, but after all transactions are committed and work is done. Useful for logging or contexts cleanup  
  Implementations are called in order according to `getOrder` method.
  You can do any changes to response object if you would like to modify response.
* [FhirestRequestExecutionInterceptor](./src/main/java/ee/fhir/fhirest/rest/filter/FhirestRequestExecutionInterceptor.java)  
  Called for every resource interaction = for every resource in `transaction` + simple CRUD requests.

## Excception handling

According to FHIR specification, in case of any error an OperationOutcome resource should be returned.  
FHIRest automatically takes care of this and composes OperationOutcome in case of any exception.  
You can take advantage of FhirestResponseFilter in order to perform some custom actions on error.

Yet, you can still rewrite default handling behaviour by
replacing [FhirExceptionHandler](./src/main/java/ee/fhir/fhirest/rest/exception/FhirExceptionHandler.java) bean with your own implementation. Make sure you
extend this bean.

```
@Service
@Primary
public class CustomExceptionHandler extends FhirExceptionHandler {
  public CustomExceptionHandler(ResourceFormatService resourceFormatService) {
    super(resourceFormatService);
  }

  @Override
  public HttpResponse<?> handle(HttpRequest request, Throwable e) {
    if (e instanceof MyCustomException) {
      return HttpResponse.badRequest("the inevitable has happened");
    }
    return super.handle(request, e);
  }
}
```
