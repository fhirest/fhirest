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

### 3.1 FHIREST core components

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

### 3.2 FHIREST other components

| Component  | What it does             | Repo      | 
|------------|--------------------------|-----------|
| HAPI FHIR  | Description of component | Repo link |
| Springboot | Description of component | Repo link |

## 4. Get started

### 4.1 Installation

#### 4.1.1 Demo application

For demo application setup, [see instructions ](https://github.com/fhirest/fhirest-examples/tree/master/fhirest-demo)

#### 4.1.2 ESTFHIR application

## 5. Get Involved

### 5.1 Features request, bug reporting

If you find any bug in the ESTFHIR product or have a new feature request, please insert it
via [git issues](https://github.com/fhirest/fhirest/issues/new/choose). The ESTFHIR community maintainers will review the inserted issues regularly. If the
request is critical, please add the name of the project and contact details to the description and send an e-mail to fhirest@tehik.ee

### 5.2 Contributing guidelines

Create a pull request..

### 5.3 Code of Conduct

The ESTFHIR project non-functional requirements are listed
here: https://www.tehik.ee/sites/default/files/2023-11/TEHIK%20Mittefunktsionaalsed%20nouded%20arenduste....pdf

## 6. Licence

MIT licence..
