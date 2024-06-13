# OpenAPI support
Example implementation of resource forward to some blockchain.  
After a resource is stored, its content is then sent to configured url

## Usage
```
implementation "ee.fhir.fhirest:fhirest-blockchain:${fhirestVersion}"
```

## Configuration
* `gateway.endpoint` - endpoint where to send resources to
