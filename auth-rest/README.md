# Authentication rest core
Base interfaces for adding authentication to rest endpoints.  
Intercepts all HTTP requests and attempts to authenticate user based on provided implementations.

## Usage
```
implementation "ee.fhir.fhirest:auth-rest:${fhirestVersion}"
```

## Authenticated user
Given user is authenticated, he can be accessed using service `ClientIdentity`.
```
public class SomeServiceMaybe {
  @Autowired
  private ClientIdentity clientIdentity;

  public void someMethod() {
    User currentUser = clientIdentity.get();
    ...
  }
```

## Implementing authentications
By implementing `AuthenticationProvider` interface, you can allow multiple authentication methods at one.  
If at least one `AuthenticationProvider` returns a User object - he is considered authenticated.

Example:
```
public class BasicAuthenticator implements AuthenticationProvider {
  public User autheticate(KefhirRequest request) {
    if (req.getHeader("Authorization").equals("Basic qwerty123")) {
      return new User();
    }
    return null;
  }
}
```
