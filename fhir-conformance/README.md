# Fhir Conformance loading tools
Non-mandatory module. Provides tools and services to load initial conformance.


## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:fhir-conformance:${fhirestVersion}"
```

### 1. Automatic download
Automatically download fhir definitions zip and populate the server with its data.  
This may be helpful for a quick start, but most probably you will need your custom setup eventually.
Configure application property `fhirest.conformance.definitions-url` with definitions url

### 2. Manual
* `POST /conformance-tools/import-file?file=/path/to/local/file`  
  Read all files in this path and try to parse them as Bundles and save.
* `POST /conformance-tools/import-url?url=http://hl7.org/fhir/5.0.0-draft-final/definitions.json.zip`  
  Download definitions zip
