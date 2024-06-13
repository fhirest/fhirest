# FhirEST core module
Core module, responsible for fhir resource lifecycle, conformance initialization.  
Manages most of the extension points.  
Takes care of handling storage and search implementations with their transactions.  
Search and store implementations may be independent of each other.  
Manages processing of search parameters and composing reference chains.

## Usage
```
implementation "ee.fhir.fhirest:fhirest-core:${fhirestVersion}"
```

## Services
### [ResourceService](./src/main/java/ee/fhir/fhirest/core/service/resource/ResourceService.java)
Main service to perform load, save and delete operations.
* `ResourceVersion save(id, content, interaction)`
* `ResourceVersion load(reference)`
* `ResourceVersion load(id)`
* `void delete(id)`

Examples:
```
private ResourceService resourceService;
private ResourceFormatService resourceFormatService;

public void updateMedicationRequestStatus(ResourceId id) {
  ResourceVersion mrVersion = resourceService.load(id);
  MedicationRequest mr = resourceFormatService.parse(mrVersion.getContent().getValue());
  mr.setStatus(MedicationRequestStatus.CANCELLED);
  resourceService.save(id, resourceFormatService.compose(mr, "json"), InteractionType.UPDATE);
}
```

### [ResourceSearchService](./src/main/java/ee/fhir/fhirest/core/service/resource/ResourceSearchService.java)
Ideally this service should solve all your searching requirements.
* `SearchResult search(resourceType, String... params)`
* `SearchResult search(resourceType, Map params)`
* `SearchResult search(SearchCriterion criteria)`

Examples:
```
SearchResult r = resourceSearchService.search(ResourceType.Patient.name(), "_count", "30", "name", "Albert");
```
```
Map<String, List<String>> params = Map.of(
  "target:MedicationRequest", List.of("111", "222", "333"),
  "end", List.of("gt" + DateUtil.format(new Date(), DateUtil.ISO_DATETIME))
);
return resourceSearchService.search(ResourceType.Provenance.name(), params);
```

### [ConformanceHolder](./src/main/java/ee/fhir/fhirest/core/service/conformance/ConformanceHolder.java)
Conformance setup is also available statically using this class.
See `ConformanceHolder.get*`


## Conformance
On every startup, FhirEST will need to configure itself based on conformance, thus conformance resources should be provided.  
By default, FhirEST will load them from storage.  
To provide conformance resources in other way, implement [ConformanceLoader](./src/main/java/ee/fhir/fhirest/core/service/conformance/loader/ConformanceLoader.java).  
There are also 2 prepared implementations you can base on:
* [ConformanceStaticLoader](./src/main/java/ee/fhir/fhirest/core/service/conformance/loader/ConformanceStaticLoader.java).  
  Simple way to provide preloaded resources, for example read from local files.
* [ConformanceStorageLoader](./src/main/java/ee/fhir/fhirest/core/service/conformance/loader/ConformanceStorageLoader.java).  
  ***Default*** used implementation. Loads conformance resources from storage.

If you would like to have full control of Conformance initialization, you can overwrite [ConformanceInitializationService](./src/main/java/ee/fhir/fhirest/core/service/conformance/ConformanceInitializationService.java) with your own implementation.

To access conformance resources statically, you can use [ConformanceHolder](./src/main/java/ee/fhir/fhirest/core/service/conformance/ConformanceHolder.java).


## Storage implementation
You can provide your own storage solution by implementing [ResourceStorage](./src/main/java/ee/fhir/fhirest/core/api/resource/ResourceStorage.java) interface.
There can be multiple implementations for different resource types. If not implementation found by resource type, implementation with type `DEFAULT` will be used.

See [Postgresql implementation](../pg-store) for examples.

## Search implementation
You can provide your own search solution by implementing [ResourceStorage](./src/main/java/ee/fhir/fhirest/core/api/resource/ResourceSearchHandler.java) interface.

See [Postgresql implementation](../pg-search) for examples.

## Interceptors
* [ResourceBeforeSaveInterceptor](./src/main/java/ee/fhir/fhirest/core/api/resource/ResourceBeforeSaveInterceptor.java)  
  Implementations of this interface are called before resource is stored into database.
  Consist of several phases:
  * `INPUT_VALIDATION` - structural and profile validation. Content is given as-is, not modified. Needed to make sure content is structurally correct before any other work is done.
  * `NORMALIZATION` - for some resource modification, metadata, reference id replacements etc.
  * `BUSINESS_VALIDATION` - Business validations with prepared resources.
  * `TRANSACTION` - When database transactions are opened 
* [ResourceAfterSaveInterceptor](./src/main/java/ee/fhir/fhirest/core/api/resource/ResourceAfterSaveInterceptor.java)  
  After resource is stored in database.  
  Phases:
  * `TRANSACTION` - within not yet closed database transactions.
  * `FINALIZATION` - after transactions are committed.
* [ResourceAfterDeleteInterceptor](./src/main/java/ee/fhir/fhirest/core/api/resource/ResourceAfterDeleteInterceptor.java)  
  After delete is done in storage
* [ResourceBeforeSearchInterceptor](./src/main/java/ee/fhir/fhirest/core/api/resource/ResourceBeforeSearchInterceptor.java)  
  Before search is performed. May be useful to validate or modify search request.  
* [OperationInterceptor](./src/main/java/ee/fhir/fhirest/core/api/resource/OperationInterceptor.java)  
  Called before any fhir operation is called
* [ConformanceUpdateListener](./src/main/java/ee/fhir/fhirest/core/api/conformance/ConformanceUpdateListener.java)  
  On any conformance resource change.

## Operations
Every Fhir [Operation](https://www.hl7.org/fhir/operations.html) requires an implementation

According to specification and requiremenrs, implement one of
* [InstanceOperationDefinition](./src/main/java/ee/fhir/fhirest/core/api/resource/InstanceOperationDefinition.java)
* [TypeOperationDefinition](./src/main/java/ee/fhir/fhirest/core/api/resource/TypeOperationDefinition.java)
