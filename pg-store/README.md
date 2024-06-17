# Postgresql storage implementation
Default storage implementation in the FhirEST using PostgreSQL as the backing database.  
Resources are saved ‘as-is’ in json format.  
Resources tables are partitioned of a common table.  
Module will automatically read conformance configuration and create needed table structure and partitions based on StructureDefinition resource.

## Dependencies

`pg-store` depends on:
* [pg-core](../pg-store) - common persistence utilities and configuration structures
* [auth-core](../auth-core) - common authentication structures
* [fhirest-core](../fhirest-core) - common logic and structures for resource lifecycle and conformance initialization
* [fhir-structures](../fhir-structures) - multi-format FHIR resource parser and composer


## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:fhirest-store:${fhirestVersion}"
```
2. Include changeset in main liquibase changelog
```
pg-store/changelog/changelog.xml
```

`pg-store` module will use [pg-core](../pg-core) defined datasources.  
You can also define separate datasource and liquibase configurations with prefixes `store-app` and `store-admin`
```yml
spring:
  datasource:
    store-app:
      url: jdbc:postgresql://localhost:5190/fhirestdb
      username: fhirest_app
      password: test
      maxActive: 10
      driverClassName: org.postgresql.Driver
      type: com.zaxxer.hikari.HikariDataSource
    store-admin:
      url: jdbc:postgresql://localhost:5190/fhirestdb
      username: fhirest_admin
      password: test
      maxActive: 1
      driverClassName: org.postgresql.Driver
      type: org.springframework.jdbc.datasource.SimpleDriverDataSource
      liquibase:
        change-log: 'classpath:changelog-store.xml'
        parameters:
          app-username: ${spring.datasource.store-app.username}
```


## Database structure

Every version is saved in one table resource, which is partitioned by resource type.  
Table names are case-insensitive.  
Every resource version creates new record in the table.  
Data is never deleted or changed in the store.  

Every table has the same structure with described columns:  
* `uid` - generated primary key
* `type` and `id` - resource type and resource id, as in url (Patient/123)
* `version` - resource version number. Generated sequentially
* `updated` and `author` - When and who created this version.
* `content` - full json content of a version
* `profiles` - contains references to the profiling StructureDefinition resource by uid
* `security_labels` - list of resource security labels
* `sys_status` - system status of a version:
  * `A` - active. There can be only one active version (last)
  * `T` - terminated. Version has been updated by a newer version
  * `C` - cancelled or deleted. Resource is deleted.
