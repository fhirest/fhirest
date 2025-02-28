# FHIRest

## 1. Introduction

FHIRest is an open-source FHIR server managed by the FHIRest community.

## 2. Release history

List of released versions with changelog available in the [github releases](https://github.com/fhirest/fhirest/releases)

> FHIRest using Semantic versioning https://semver.org/

#### Major changes

| Release | FHIR version | Supported FHIR versions | Spring version | Java Version |
|---------|--------------|-------------------------|----------------|--------------|
| 1.2     | R5           | R5, R4                  | 3.4            | 17           |
| 1.0     | R5           | R5, R4                  | 3.2            | 17           |

## 3. Components

The FHIRest FHIR server is open-source software that uses the following components.

### 3.1. FHIRest core components

FHIRest provides several Java modules that may be embedded into the source code of your application as libraries.

| Component                                                        | What it does                                                                                 |
|------------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| [fhirest-core](./fhirest-core)                                   | Core module, responsible for fhir resource lifecycle, conformance initialization             |
| [fhir-structures](./fhir-structures)                             | Responsible for parsing and composing fhir resources into different formats (json, xml, ...) |
| [fhir-rest](./fhir-rest)                                         | Provides fhir REST interfaces                                                                |
| [fhir-conformance](./fhir-conformance)                           | Provides possibilities to load initial conformance resources                                 |
| [tx-manager](./tx-manager)                                       | Database transaction support                                                                 |
| [fhirest-scheduler](./fhirest-scheduler)                         | Provides possibilities to perform some actions on resource at some given time in the future  |
| [pg-core](./pg-core)                                             | PostgreSQL core: utilities, configurations                                                   |
| [pg-store](./pg-store)                                           | Fhir resource storage PostgreSQL implementation                                              |
| [pg-search](./pg-search)                                         | Fhir resource search PostgreSQL implementation                                               |
| [validation-profile](./validation-profile)                       | Resource profile validations                                                                 |
| [openapi](./openapi)                                             | Provides Openapi support                                                                     |
| [feature-conditional-reference](./feature-conditional-reference) | Adds conditional reference support to all resources and interactions                         |
| [operation-patient-everything](./operation-patient-everything)   | Implementation of Patient $everything operation                                              |
| [fhirest-hashchain](./fhirest-hashchain)                         | Timestamps all reasources in a hash chain                                                    |
| [fhirest-blockchain](./fhirest-blockchain)                       | Example implementation of resource forward to some blockchain                                |
| [auth-core](./auth-core)                                         | Common authentication objects                                                                |
| [auth-rest](./auth-rest)                                         | Base interfaces for adding authentication to rest endpoints                                  |
| [auth-openid](./auth-openid)                                     | Simple oauth implementation                                                                  |
| [auth-smart](./auth-smart)                                       | smart-app-launch implementation                                                              |
| [fhirest-test-app](./fhirest-test-app)                           | Simple application to run FHIRest                                                            |

### 3.2. FHIRest other components

| Component  | What it does             | Repo      | 
|------------|--------------------------|-----------|
| HAPI FHIR  | Description of component | Repo link |
| Springboot | Description of component | Repo link |

## 4. Using FHIRest

#### 4.1. Example projects

To run example applications, follow the instructions under the corresponding projects:

* [fhirest-demo](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-demo)
* [fhirest-multidatabase](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-multidatabase)

#### 4.2. Using the libraries

FHIRest modules are published to:

* Maven Central: **releases**
* GitHub Packages registry: **snapshots** and **releases**.

##### 4.2.1 Maven Central

* Simply refer to https://mvnrepository.com/artifact/ee.fhir.fhirest for a list of available packages and versions

##### 4.2.2 Github Packages

* Using a build tool of your choice, declare Maven repository with URL `https://maven.pkg.github.com/fhirest/fhirest`. For detailed instructions, refer to
  official guides:
    * [Maven](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#installing-a-package)
    * [Gradle](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry#using-a-published-package)
* GitHub Packages requires authentication using a personal access token (classic) with at least `read:packages` scope to install packages. For detailed
  instructions, refer to official guides:
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

* Based on the requirements of your project, choose and add components as dependencies to your project from
  the [package index](https://github.com/orgs/fhirest/packages?repo_name=fhirest).

#### 4.3. Quickstart

The quickest way to get started is to clone and inspect the [demo project](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-demo) or use it as a
starter. It provides a minimal setup with necessary dependency management and configurations already set up for the following components:

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

## 5. Get Involved

### 5.1 Features request, bug reporting

If you find any bug in the FHIRest product or have a new feature request, please insert it
via [git issues](https://github.com/fhirest/fhirest/issues/new/choose). The FHIRest community maintainers will review the inserted issues regularly. If the
request is critical, please add the name of the project and contact details to the description and send an e-mail to fhirest@tehik.ee

### 5.2 Contributing

Please refer to the FHIRest community [CONTRIBUTING.md](../.github/CONTRIBUTING.md)

### 5.3 Developing

* [Building and Running](./developer-guide.md)
* [Publishing](./publishing.md)
* [Code style](../.github/codestyle)
* [Coding rules](../.github/CODING_RULES.md)

## 6. Licence

FHIRest is developed under [MIT licence](./LICENSE.md).
