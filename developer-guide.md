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
Although FHIRest is a library, for testing purposes it has a runnable application
1. Create a database. See `etc/run-postgres.sh`
2. Run
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
example
```
@Slf4j
<...>
log.info("your log");
```

Run `etc/collect-logs.sh` to collect all logs.  
See `etc/logs.md` for a collected list of all logs.  
Example logback configuration `fhirest-test-app/src/main/resources/logback.xml`.  

