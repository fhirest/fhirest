Publishing to maven central
==========

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
