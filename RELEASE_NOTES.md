# Credential Issuer common libraries Release Notes

## 1.5.6

* Updated AuditEventFactory to create timestamps in milliseconds for audit events

## 1.5.5

* Added `PersonIdentityDetailedBuilder` with similar intent as `PersonIdentityDetailedFactory` for constructing PersonIdentityDetailed with a builder constructor no args option
* For cases where CRI's requires a PersonIdentityDetailed object without `nameparts` or `birthdate` for use with the `AuditContext`
* `PersonIdentityDetailedBuilder` can be used as an alternative to `PersonIdentityDetailedFactory`

## 1.5.4

* Updated Person Identity Mapper access to public so it can be tested and mocked by other services that use the person identity service

## 1.5.3

* Increased version number of nimbusds and awssdk dependencies to remove vulnerabilities, aligned tests to changes in the nimbus error message format
* Allowed isReleaseFlag to return false if there are no permissions to read the parameter
* Updated Pom alphagov to govuk-one-login

## 1.5.2

* Fix for the below, add UUID id to `VerifiableCredentialClaimsSetBuilder` as a top level attribute `jti` of the `jwt` 


## 1.5.1

* Added a UUID id to `VerifiableCredentialClaimsSetBuilder` ensuring the Claimset of the Verifiable Credential (VC) contains a unique identifier which allows VC's to distinguish each other

## 1.5.0

* Added new factory object `PersonIdentityDetailedFactory` with `createPersonIdentityDetailedWith` methods for creation of `PersonIdentityDetailed` with cri specific lists to restrict scope of any PersonIdentityDetailed constructor changes to just cri-lib
* Deprecated all `PersonIdentityDetailed` constructors to denote that direct use should be avoided to mitigate the antipattern of extending the constructor to add lists, then requiring CRI's during cri-lib updates to set lists they don't use to null

## 1.4.6

* Add default client id representing core stub

## 1.4.5

* Modified feature release flag for VC expiry

## 1.4.4

* Added feature release flag for VC expiry

## 1.4.3

* Added `PiiRedactingDeserializer`

## 1.4.2

* Added `DrivingPermit` and `DrivingPermitIssuer`
* Modified `PersonIdentityDetailed` and `PersonIdentityDetailed` to support  `DrivingPermit`

## 1.4.1

* Added `RESPONSE_RECEIVED` to replace `THIRD_PARTY_REQUEST_ENDED` 
* `THIRD_PARTY_REQUEST_ENDED` left for backward compatibility

## 1.4.0

* Added common clients/steps for cucumber integration-testing

## 1.3.1

* Added `END` AuditEventType to enable sending `CRI_END` audit event

## 1.3.0

* Added method to persistently log specified key/value data when a log entry is written 

## 1.2.0

* Added VerifiableCredentialClaimsSetBuilder class to centralise the creation of the verifiable credential JWT claims set

## 1.1.8

* Code smells removal (no functional change)

## 1.1.7

* Added additional expiry time validation to auth jar checks
* Added log helper to assist with adding logging identifiers

## 1.1.6

* Added attemptCount to sessionItem and initialised attemptCount in session creation
* Added OathErrorResponse for use with access denied error

## 1.1.5

* Convert the KMS der signature format into concat format

## 1.1.4

* Performance: cache SSM params and Secrets Manager secrets for longer

## 1.1.3

* Added `clientIpAddress` in session table `session-di-ipv-cri-kbv-*`.

## 1.1.2

* Remove `persistent_session_id` from being logged.

## 1.1.1

* Log `govuk_signin_journey_id` and `persistent_session_id` to CRI log messages.
