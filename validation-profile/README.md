# Resource profile validations
Reads defined StructureDefinitions and performs according profile validations on all incoming resources

## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:validation-profile:${fhirestVersion}"
```

## Configurations
* `fhirest.validation-profile.enabled` - `true`/`false` - ***true*** by default.  Enables or disables validations
