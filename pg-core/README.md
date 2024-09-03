# Postgresql Core module
Postgresql core/commons module. Provides common database connections, utilities and configuration structures for PostgreSQL.

## Dependencies

`pg-core` is a *direct* dependency to:
* [pg-store](../pg-store) - resource storage
* [pg-search](../pg-store) - resource search
* [fhirest-scheduler](../fhirest-scheduler) - action scheduler for stored resources
* [fhirest-hashchain](../fhirest-hashchain) - resource timestamping and integrity (through [pg-store](https://github.com/fhirest/fhirest/tree/master/pg-store))

`pg-core` depends on:
* [tx-manager](../tx-manager) - multi transaction manager support

## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:pg-core:${fhirestVersion}"
```

## Configuration
pg-core supports default spring datasource definition 
* `spring.datasource`  

as well as separate datasource definitions for crud operations and database alterations
* `spring.datasource.default` - datasource for reading and inserting resources
* `spring.datasource.admin` - datasource for altering the database, e.g. for running migration scripts, adding/removing new resource definitions or updating indexes

where both use same configuration properties, as in spring, with an addition of liquibase configuration under admin datasource.

All connections are using HikariCP connection, you can add any Hikari specific configuration properties under all 
datasources.

```yml
# example spring config 
spring:
  datasource:
    hikari:
      jdbc-url: ${DB_URL:jdbc:postgresql://localhost:5151/fhirestdb}
      username: ${DB_APP_USER:fhirest_app}
      password: ${DB_APP_PASSWORD:test}
      maximum-pool-size: ${DB_POOL_SIZE:1}
      driver-class-name: org.postgresql.Driver
  liquibase:
    url: ${DB_URL:jdbc:postgresql://localhost:5151/fhirestdb}
    user: postgres
    password: postgres
    change-log: 'classpath:changelog.yml'
    parameters:
      app-username: ${spring.datasource.username}
```

```yml
# example separate ds config
spring:
  datasource:
    default:
      jdbc-url: ${DB_URL:jdbc:postgresql://localhost:5151/fhirestdb}
      username: ${DB_APP_USER:fhirest_app}
      password: ${DB_APP_PASSWORD:test}
      maximum-pool-size: ${DB_POOL_SIZE:1}
      driver-class-name: org.postgresql.Driver
    admin:
      url: ${spring.datasource.default.jdbc-url}
      username: ${DB_ADMIN_USER:fhirest_admin}
      password: ${DB_ADMIN_PASSWORD:test}
      maximum-pool-size: 1
      driver-class-name: org.postgresql.Driver
      liquibase:
        change-log: 'classpath:changelog.xml'
        parameters:
          app-username: ${spring.datasource.default.username}
```
