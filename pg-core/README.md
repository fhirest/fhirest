# FhirEST PostgreSQL core

`pg-core` module provides common utilities and configuration structures and logic to drive and support FHIR resource persistence on the FhirEST server.

## Dependencies

`pg-core` is a *direct* dependency to:

* [pg-store](https://github.com/fhirest/fhirest/tree/master/pg-store) - resource storage
* [fhirest-scheduler](https://github.com/fhirest/fhirest/tree/master/fhirest-scheduler) - action scheduler for stored resources

`pg-core` is a *transitive* dependency:

* [pg-search](https://github.com/fhirest/fhirest/tree/master/pg-store) - resource search (through [pg-store](https://github.com/fhirest/fhirest/tree/master/pg-store))
* [fhirest-hashchain](https://github.com/fhirest/fhirest/tree/master/fhirest-hashchain) - resource timestamping and integrity (through [pg-store](https://github.com/fhirest/fhirest/tree/master/pg-store))

`pg-core` depends on:

* [tx-manager](https://github.com/fhirest/fhirest/tree/master/tx-manager) - multi transaction manager support 

## Usage
1. Add gradle dependency for `pg-core`
```
implementation "ee.fhir.fhirest:pg-core:${fhirestVersion}"
```
