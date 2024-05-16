# FhirEST PostgreSQL store

`pg-store` module provides FHIR resource storage on the FhirEST server using PostgreSQL as the backing database.

## Dependencies

`pg-store` is a *direct* dependency to:

* [pg-search](https://github.com/fhirest/fhirest/tree/master/pg-store) - resource search
* [fhirest-hashchain](https://github.com/fhirest/fhirest/tree/master/fhirest-hashchain) - resource timestamping and integrity

`pg-store` depends on:

* [pg-core](https://github.com/fhirest/fhirest/tree/master/pg-store) - common persistence utilities and configuration structures
* [auth-core](https://github.com/fhirest/fhirest/tree/master/auth-core) - common authentication structures
* [fhirest-core](https://github.com/fhirest/fhirest/tree/master/fhirest-core) - common logic and structures for resource lifecycle and conformance initialization
* [fhir-structures](https://github.com/fhirest/fhirest/tree/master/pg-fhir-structures) - multi-format FHIR resource parser and composer

## Installation
1. Add gradle dependency for `pg-store`
```
implementation "ee.fhir.fhirest:pg-store:${fhirestVersion}"
```

## Configuration

If `pg-store` is found on the classpath, then the following must be provided and configured:
1. PostgreSQL database connection
    1. Role-based configuration
        1. `spring.datasource.default` - datasource credentials for reading and inserting resources
        2. `spring.datasource.admin` - credentials for altering the database, e.g. for running migration scripts, adding/removing new resource definitions or updating indexes
    2. Schema-based configuration
        1. `spring.datasource.store-app` - reading and inserting stored resources
        2. `spring.datasource.search-app` - reading and inserting search indexing related data
        3. `spring.datasource.store-admin` - altering resource storage tables
        4. `spring.datasource.search-admin` - altering index storage tables

If both, role-based and schema-based configurations are provided, then latter takes the priority.

```yml
# example application.yml
spring:
  datasource:
    default:
      url: ${DB_URL:jdbc:postgresql://localhost:5151/fhirestdb}
      username: ${DB_APP_USER:fhirest_app}
      password: ${DB_APP_PASSWORD:test}
      maxActive: ${DB_POOL_SIZE:1}
      driverClassName: org.postgresql.Driver
      type: com.zaxxer.hikari.HikariDataSource
    admin:
      url: ${spring.datasource.default.url}
      username: ${DB_ADMIN_USER:fhirest_admin}
      password: ${DB_ADMIN_PASSWORD:test}
      maxActive: 1
      driverClassName: org.postgresql.Driver
      type: org.springframework.jdbc.datasource.SimpleDriverDataSource
      liquibase:
        change-log: 'classpath:changelog.xml'
        parameters:
          app-username: ${spring.datasource.default.username}
```
2. Liquibase changelog file

```xml
<!-- example changelog.xml given pg-search is also used -->
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
		http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.5.xsd">

  <include file="pg-core/changelog/changelog.xml" relativeToChangelogFile="false"/>
  <include file="pg-store/changelog/changelog.xml" relativeToChangelogFile="false"/>
  <include file="pg-search/changelog/changelog.xml" relativeToChangelogFile="false"/>

</databaseChangeLog>
```

## Usage

Use `ResourceService` exposed by the `pg-core` module to interact with the storage.

Internally, `pg-store` implements the `ResourceStorage` interface which is used through `ResourceStorageService` -> `ResourceService` component chain.
