Publishing to maven central
==========

### Publish
Note: Maven Central Portal does not support gradle (at the time of writing this), so custom script and Publisher API is used  
https://central.sonatype.org/publish/publish-portal-api/#deployment
```
export SONATYPE_USER=
export SONATYPE_PASSWORD=
./publish-to-maven-central.sh 1.0.0
```

### GPG key

FHIRest Maven Central JAR signature key

Fingerprint: `AC8E 73FA 2EBC 1C67 2E2C  1B05 763E B0DD DF26 7DDB`

You can import this key using a public key server:
```
$ gpg --keyserver keyserver.ubuntu.com --recv AC8E73FA2EBC1C672E2C1B05763EB0DDDF267DDB
```


### Publications signing
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

### Generating GPG key
see also https://central.sonatype.org/publish/requirements/gpg
```
gpg --gen-key
gpg --list-keys # get key id
gpg --keyserver keyserver.ubuntu.com --send-keys <key id>
```

