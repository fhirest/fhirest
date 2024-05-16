# FhirEST PostgreSQL search

pg-search module provides FHIR resource search on the FhirEST server using PostgreSQL as the backing database.

## Dependencies

pg-search depends on the [pg-store](https://github.com/fhirest/fhirest/tree/master/pg-store) module to provide the underlying resource storage mechanisms.

## Installation
1. Add gradle dependency for `pg-search`
```
implementation "ee.fhir.fhirest:pg-search:${fhirestVersion}"
```

## Configuration

A database connection and Liquibase configuration must be provided as specified by [pg-store](https://github.com/fhirest/fhirest/tree/master/pg-store#configuration).

## Usage

Use `ResourceSearchService` exposed by the `pg-core` module to interact with the search module.

Internally, `pg-search` implements the `ResourceSearchHandler` interface which is then used by the `ResourceSearchService`.
