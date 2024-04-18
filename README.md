# FhirEST

## 1. Introduction

FHIREST is an open-source FHIR server managed by the FHIREST community. It is developed under a xx licence.

## 2. Release history

> FHIREST using Semantic versioning https://semver.org/
> **Release** - Release number
>
> **Current version** - means the release branch
>
> **Supported FHIR version** - Supported FHIR versions
>
> **Spring version** - minimum version of spring framework
>
> **Java version** - Current FHIREST Java version

| Release | Version | Supported FHIR version | Spring version | Java Version |
|---------|---------|------------------------|----------------|--------------|
| 1.0     | 1.0.1   | R5 (conformance), R4   | 3.2            | 17           |

## 3. Components

The ESTFHIR FHIR server is open-source software that uses the following components.

### 3.1. FHIREST core components

FhirEST provides several Java modules that may be embedded into the source code of your application as libraries.

| Component                     | What it does                                                                                 |
|-------------------------------|----------------------------------------------------------------------------------------------|
| fhirest-core                  | Core module, responsible for fhir resouce lifecycle, conformance initialization              |
| fhir-structures               | Responsible for parsing and composing fhir resources into different formats (json, xml, ...) |
| fhir-rest                     | Provides fhir REST interfaces                                                                |
| fhir-conformance              | Provides possibilities to load initial conformance resources                                 |
| tx-manager                    | Database transaction support                                                                 |
| fhirest-scheduler             | Provides possibilities to perform some actions on resource at some given time in the future  |
| pg-core                       | PostgreSQL core: unitilities, configurations                                                 |
| pg-store                      | Fhir resource storage PostgreSQL implementation                                              |
| pg-search                     | Fhir resource search PostgreSQL implementation                                               |
| validation-profile            | Resource profile validations                                                                 |
| openapi                       | Provides Openapi support                                                                     |
| feature-confitional-reference | Adds conditional reference support to all resources and interactions                         |
| operation-patient-everything  | Implementation of Patient $everything operation                                              |
| fhirest-blockchain            | Example implementation of resource forward to some blockchain                                |
| auth-core                     | Common authentication objects                                                                |
| auth-rest                     | Base interfaces for adding authentication to rest endpoints                                  |
| auth-openid                   | Simple oauth implementation                                                                  |
| auth-smart                    | smart-app-launch implementation                                                              |
| fhirest-test-app              | Simple application to run FhirEST                                                            |

### 3.2. FhirEST other components

| Component  | What it does             | Repo      | 
|------------|--------------------------|-----------|
| HAPI FHIR  | Description of component | Repo link |
| Springboot | Description of component | Repo link |

## 4. Using FhirEST

#### 4.1. Example projects

To run example applications, follow the instructions under the corresponding projects:

* [fhirest-demo](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-demo)
* [fhirest-multidatabase](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-multidatabase)

#### 4.2. Using the libraries

FhirEST modules are published as artifacts to GitHub Packages registry. To access and use them in your project:

* Using a build tool of your choice, declare Maven repository with URL `https://maven.pkg.github.com/fhirest/fhirest`. For detailed instructions, refer to official guides:
  * [Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#installing-a-package)
  * [Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
* GitHub Packages requires authentication using a personal access token (classic) with at least `read:packages` scope to install packages. For detailed instructions, refer to official guides:
  * [Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-with-a-personal-access-token)
  * [Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#authenticating-with-a-personal-access-token)
* Example config using `build.gradle` (Gradle Groovy) where the personal token is provided through environment variables `GITHUB_USER` and `GITHUB_TOKEN`:

```groovy
maven {
    url = uri("https://maven.pkg.github.com/fhirest/fhirest")
    credentials {
      username = project.findProperty("gpr.user") ?: System.getenv("GITHUB_USER")
      password = project.findProperty("gpr.key") ?: System.getenv("GITHUB_TOKEN")
    }
  }
```

* Based on the requirements of your project, choose and add components as dependencies to your project from the [package index](https://github.com/orgs/fhirest/packages?repo_name=fhirest).

#### 4.3. Quickstart

The quickest way to get started is to clone and inspect the [demo project](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-demo) or use it as a starter. It provides a minimal setup with necessary dependency management and configurations already set up for the following components:

```groovy
implementation "ee.fhir.fhirest:fhirest-core:${fhirestVersion}"
implementation "ee.fhir.fhirest:fhir-conformance:${fhirestVersion}"
implementation "ee.fhir.fhirest:fhir-rest:${fhirestVersion}"
implementation "ee.fhir.fhirest:pg-core:${fhirestVersion}"
implementation "ee.fhir.fhirest:pg-store:${fhirestVersion}"
implementation "ee.fhir.fhirest:pg-search:${fhirestVersion}"
implementation "ee.fhir.fhirest:validation-profile:${fhirestVersion}"
implementation "ee.fhir.fhirest:feature-conditional-reference:${fhirestVersion}"
implementation "ee.fhir.fhirest:operation-patient-everything:${fhirestVersion}"
implementation "ee.fhir.fhirest:openapi:${fhirestVersion}"
```

#### 4.4. Configuration

#### 4.4.1. Resource persistence and search

FhirEST `pg-store` and `pg-search` components provide FHIR resource storage and search implementation. If either of these components are found on the classpath, then the following must be provided and configured:
1. PostgreSQL database connection
   1. Role-based configuration
      1. `spring.datasource.default` - datasource credentials for reading and inserting resources
      2. `spring.datasource.admin` - credentials for altering the database, e.g. for running migration scripts, adding/removing new resource definitions or updating indexes
   2. Schema-based configuration
      1. `spring.datasource.store-app` - reading and inserting stored resources
      2. `spring.datasource.search-app` - reading and inserting search indexing related data 
      3. `spring.datasource.store-admin` - altering resource storage tables
      4. `spring.datasource.search-admin` - altering index storage tables

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
2. Path to Liquibase changelog file

```xml
<!-- example changelog.xml -->
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

#### 4.4.2. Conformance

#### 4.4.3. REST API

#### 4.4.4. Validation

#### 4.4.5. Authentication

#### 4.4.6. Scheduler


## 5. Get Involved

### 5.1 Features request, bug reporting

If you find any bug in the ESTFHIR product or have a new feature request, please insert it
via [git issues](https://github.com/fhirest/fhirest/issues/new/choose). The ESTFHIR community maintainers will review the inserted issues regularly. If the
request is critical, please add the name of the project and contact details to the description and send an e-mail to fhirest@tehik.ee

### 5.2 Contributing
TODO: link contribution.md

### 5.3 Developing
[Developer Guide](https://github.com/fhirest/fhirest/blob/master/developer-guide.md)

## 6. Licence

MIT licence..
