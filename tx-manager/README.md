# Transaction manager
Module provides interfaces for transaction handling.

Since multiple and unknown data providers are allowed, FhirEST cannot automatically handle their transactions.
When implementing your own storage, you need to think of transactionality as well.


## Usage
```
implementation "ee.fhir.fhirest:fhirest-tx-manager:${fhirestVersion}"
```


You need to implement `TransactionManager`, which is being called for all data providers every time a save is performed.

Please refer to [Postgresql implementation](../pg-core/src/main/java/ee/fhir/fhirest/PgTransactionManager.java) for an example
