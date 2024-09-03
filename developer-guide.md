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


Publishing to maven central
---------------
### Generate GPG key
see also https://central.sonatype.org/publish/requirements/gpg
```
gpg --gen-key
gpg --list-keys # get key id
gpg --keyserver keyserver.ubuntu.com --send-keys <key id>
```
a. For running locally
```
gpg --keyring secring.gpg --export-secret-keys > ~/.gnupg/secring.gpg

# ~/.gradle/gradle.properties
signing.keyId=<last 8 symbols of key id>
signing.password=<pw>
signing.secretKeyRingFile=/path/to/.gnupg/secring.gpg
```
b. For CI (using env variables)
```
gpg --armor --export-secret-keys <key id>
export GPG_KEY=<full key from previous command>
export GPG_PASS=<pw>
```
### Publish 
Note: Maven Central Portal does not support gradle (at the time of writing this), so custom script and Publisher API is used  
https://central.sonatype.org/publish/publish-portal-api/#deployment
```
export SONATYPE_USER=
export SONATYPE_PASSWORD=
./publish-to-maven-central.sh 1.0.0
```

Logging
---------------
* Slf4j should be used for logging
* One event per log
* Empty parameters should be replaced with a placeholder
* All errors should be logged

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
* Dependencies used should not have EOL sooner, than 2 years
* Code should be runnable on high availability distributed environment with multiple instances
    * Application should be horizontally scalable
    * Application should be stateless
    * Application should be runnable on different domains and different environments
    * All configurations should be configurable at runtime
    * Environment specific variables should be configurable
    * Application should work with databases on remote server
    * Application should be scalable both in data size and user count
    * All api interfaces should be high availability capable
    * Application should run on infrastructure with load balancers
* Database rules
  * Table relations should use foreign keys
  * Foreign keys should be indexed
  * Parameter binding should be used in queries
  * Database object names should be meaningful

