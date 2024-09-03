# FHIRest Hashchain
Module timestamps all saved resource versions and saves the hashes in a table.  
Depends on pg-store module and uses same database schema and connections.  
After every resource save, a hash is calculated and saved based on user configuration and previous hash, thus providing with a chain of hashes for every resource.
For hash calculation [Postgresql pgcrypto](https://www.postgresql.org/docs/current/pgcrypto.html) module is used.

## Installation
1. Add gradle dependency
```
implementation "ee.fhir.fhirest:fhirest-hashchain:${fhirestVersion}"
```
2. Include changeset in main liquibase 
```
fhirest-hashchain/changelog/changelog.yml
```

## Configuration
Hashing algorithm and resource values are configurable in table `store.hashchain_config`.  
Do not update any rows, if some hashes are already generated. Change `sys_status` to some other value and add new row with new configuration.  
For generating new hash, single active row will be taken (`sys_status = 'A'`).  
For hash validation, same configuration will be used, as in initial calculation.  
Configuration options:  
* `algorithm` - hashing algorithm. See [Postgresql pgcrypto](https://www.postgresql.org/docs/current/pgcrypto.html) for supported values.  
* `version` - version of internal hashing algorithm (values to consider for digesting).
  * Currently supported versions: `v1` 

Default configuration for `sha256` will be added by liquibase.

## Validation
`select * from store.validate_resource_hash(xxx)`, where `xxx` is `store.resource.uid` value  
or  
`select * from store.validate_resource_hash(type, id, versionNr)`, where `type`, `id` and `versionNr` are corresponding resource reference.  
Both requests will return two hash values - saved and calculated. If they match - resource is untouched.
