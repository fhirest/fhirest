# OpenAPI support
Reads defined Conformance resources and composes OpenAPI descriptions in yaml format


## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:openapi:${fhirestVersion}"
```

Openapi will be available on `/fhir-openapi` endpoint.

## Configurations
* `fhirest.openapi.path` - `openapi-path` - ***fhir-openapi*** by default.  Openapi endpoint
