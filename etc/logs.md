| file | log |
| ---- | --- |
|/home/daniel/dev/tehik/fhirest/fhirest-scheduler/src/main/java/ee/fhir/fhirest/scheduler/SchedulerJobRunner.java|log.debug("starting scheduler job runner");|
|/home/daniel/dev/tehik/fhirest/fhirest-scheduler/src/main/java/ee/fhir/fhirest/scheduler/SchedulerJobRunner.java|log.debug("found 0 jobs");|
|/home/daniel/dev/tehik/fhirest/fhirest-scheduler/src/main/java/ee/fhir/fhirest/scheduler/SchedulerJobRunner.java|log.info("found " + jobs.size() + " jobs");|
|/home/daniel/dev/tehik/fhirest/fhirest-scheduler/src/main/java/ee/fhir/fhirest/scheduler/SchedulerJobRunner.java|log.info("could not lock " + job.getId() + ", continuing");|
|/home/daniel/dev/tehik/fhirest/fhirest-scheduler/src/main/java/ee/fhir/fhirest/scheduler/SchedulerJobRunner.java|log.error("error during job " + job.getId() + "execution: ", e);|
|/home/daniel/dev/tehik/fhirest/validation-profile/src/main/java/ee/fhir/fhirest/validation/ResourceProfileValidator.java|log.error("exception during profile validation", e);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.error("blindex: will not run. conformance not yet initialized");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.info("refreshing search indexes...");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("currently indexed: " + current);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("need to create: " + create);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("need to remove: " + drop);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.info("blindex initialization finished");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.error("could not init index: " + res.getType() + "." + sp.getName() + "@" + sp.getDefinition() + ", definition of search parameter not found");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("creating index on " + b.getKey());|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("failed " + b.getKey() + ": " + " unknown yet");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("failed " + b.getKey() + ": " + " not configures");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("failed " + b.getKey() + ": " + err);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.info("failed to create " + errors.size() + " indexes");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.info("created " + createdIndexed.size() + " indexes");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.info("recalculating " + blindexes.size() + " indexes for " + resourceTypeBlindexes.size() + " resources");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.error("failed to recalculate indexes for " + type, e);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.info("index recalculation finished");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexInitializer.java|log.debug("failed " + b.getKey() + ": " + err);|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexCleanupService.java|log.debug("starting index cleanup");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/BlindexCleanupService.java|log.debug("index cleanup finished");|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/repository/PgSearchRepository.java|log.debug(sb.getPretty());|
|/home/daniel/dev/tehik/fhirest/pg-search/src/main/java/ee/fhir/fhirest/search/repository/PgSearchRepository.java|log.debug(sb.getPretty());|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.debug("Operation '{}' with implementation '{}' is present for resource {}, but it missing in CapabilityStatement", e.getKey(), e.getValue(), r.getType())|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.error("Missing OperationDefinition for referenced in CapabilityStatement operation {} for resource {}",|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.error("Cannot find implementation for declared in capability statement operation '{}'", opName);|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.debug("There is {} implementations for operation '{}' for resource '{}'", implTypes, opName, resourceType);|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.trace("Beans {} are bound to operation '{}' for resource '{}'", implTypes, opName, resourceType);|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.info("Started " + (rest.getResource().size() + 1) + " rest services.");|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/FhirestEndpointInitializer.java|log.debug("Starting: " + type + ": " + String.join(", ", interactions));|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/exception/FhirExceptionHandler.java|log.error("Fhir error occurred", e);|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/exception/FhirExceptionHandler.java|log.error(msg);|
|/home/daniel/dev/tehik/fhirest/fhir-rest/src/main/java/ee/fhir/fhirest/rest/exception/FhirExceptionHandler.java|log.info(msg);|
|/home/daniel/dev/tehik/fhirest/fhir-conformance/src/main/java/ee/fhir/fhirest/ConformanceFileImportService.java|log.info("Loading initial conformance data. This may take several minutes.");|
|/home/daniel/dev/tehik/fhirest/fhir-conformance/src/main/java/ee/fhir/fhirest/ConformanceFileImportService.java|log.info("processing " + f);|
|/home/daniel/dev/tehik/fhirest/fhir-conformance/src/main/java/ee/fhir/fhirest/ConformanceDownloadService.java|log.info("conformance seems to be initialized. will not download.");|
|/home/daniel/dev/tehik/fhirest/fhir-conformance/src/main/java/ee/fhir/fhirest/ConformanceDownloadService.java|log.error("", e);|
|/home/daniel/dev/tehik/fhirest/fhir-conformance/src/main/java/ee/fhir/fhirest/ConformanceDownloadService.java|log.info("downloading '" + url + "'");|
|/home/daniel/dev/tehik/fhirest/fhir-conformance/src/main/java/ee/fhir/fhirest/ConformanceDownloadService.java|log.info("unzipping " + zip.getName() + " to " + outputDir.getAbsolutePath());|
|/home/daniel/dev/tehik/fhirest/fhirest-core/src/main/java/ee/fhir/fhirest/core/service/cache/FhirestCacheManager.java|log.info("Closing cache manager...");|
|/home/daniel/dev/tehik/fhirest/fhirest-core/src/main/java/ee/fhir/fhirest/core/service/conformance/ConformanceInitializationService.java|log.info("refreshing conformance...");|
|/home/daniel/dev/tehik/fhirest/fhirest-core/src/main/java/ee/fhir/fhirest/core/service/conformance/ConformanceInitializationService.java|log.info("conformance loaded");|
|/home/daniel/dev/tehik/fhirest/fhirest-core/src/main/java/ee/fhir/fhirest/core/service/conformance/ConformanceInitializationService.java|log.info("conformance not initialized");|
|/home/daniel/dev/tehik/fhirest/auth-rest/src/main/java/ee/fhir/fhirest/auth/http/HttpAuthInterceptor.java|log.debug("could not authenticate. tried services: " + authenticators);|
