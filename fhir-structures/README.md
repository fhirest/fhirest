# Fhir Resource representations
Responsible for parsing and composing fhir resources into different formats.  
Provides default formats:
* json
* xml
* html

## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:fhir-structures:${fhirestVersion}"
```

## Usage
### ResourceFormatService
This service provides api for parsing and composing resources from/to representation formats (json, xml).

* `ResourceContent compose(resource, format)`  
  `format` may be in any supported representation (`json`, `application/json`, `application/json+fhir`, ...)
* `Resource parse(content)`  
   Format will be automatically detected


## Implementing other formats:
To provide support of some other resource format, implements [ResourceRepresentation](./src/main/java/ee/fhir/fhirest/structure/api/ResourceRepresentation.java)




