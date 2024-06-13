# Simple oauth implementation
Implements `AuthenticationProvider` from [auth-core](../auth-core).  
Reads and validates user token in according to oauth openid specification.  
`User` is populated with claims from jwt token. 

## Usage
```
implementation "ee.fhir.fhirest:auth-openid:${fhirestVersion}"
```

## Configuration
* `fhirest.oauth.jwks-url` - path to sso jwks certificates to validate tokens against to

