Developer Guide
===============


Building
---------------

Project can be built using Java 17 and gradle

```
./gradlew build
```

Running
---------------
Although FhirEST is a library, for testing purposes it has a runnable application
```
./gradlew bootRun
```
```
./gradlew bootRun -Pdebug=5005
```

Testing
---------------

Junit tests can be run using gradle
```
./gradlew test
```

Fhir integration tests can be executed using the script
```
./fhirest-test-app/run-tests.sh
```

Or run them manually using Postman. Collections available at `fhirest-test-app/postman`

Logging
---------------
* Slf4j should be used for logging

example
```
@Slf4j
<...>
log.info("your log");
```

Run `etc/collect-logs.sh` to collect all logs.
See `etc/logs.md` for a collected list of all logs
Example logback configuration `fhirest-test-app/src/main/resources/logback.xml`

Coding rules
---------------

* 64-bit architecture should be supported
* All code, scripts and application itself should use UTF-8 encoding
* Code should not store anything directly in server filesystem
* All configuration parameters should be meaningful
* Configuration parameters should be reused where possible. No duplications.
* Database connections should use connection pooling
* It should be possible to define different database users for database structure modifications and for basic usage 
* Database changes should be automated
* If any cryptographic algorithm is used, it should be configurable and replaceable

* All code, comments, scripts etc. should be written in English
* All variables, type and function names should be meaningful
* Unused code should be removed
* Code should be runnable on high availability distributed environment with multiple instances
    * Applicaiton should be horizontally scalable
    * Application should be stateless
    * Application should be runnable on different domains and different environments
    * All configurations should be configurable at runtime
    * Environment specific variables should be configurable
    * Application should work with databases on remote server
    * Application should be scalable both in data size and user count
    * All api interfaces should be high availability capable
    * Application should run on infrastructure with load balancers
