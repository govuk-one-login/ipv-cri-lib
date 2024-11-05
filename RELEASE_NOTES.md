# Credential Issuer common libraries Release Notes

## 3.4.0

    - Adds addressRegion to Address

## 3.3.0

    - Updated audit event tests to use the new test harness events endpoint
    - Added AWS credentials to sign the HTTP request for the events endpoint
    - Added a Test Harness API client to interact with the test resources API
    - Updated CriTestContext to expose test harness responses to the tests

## 3.2.1

    - Loosen Driving Permit Postcode extraction from DVA licenses

## 3.2.0

    - Adds addressRegion to CanonicalAddress

## 3.1.3

    - Add support for extracting (best effort) a postcode from the full address field in driving permit shared claims.
    - Update GoogleJavaFormat used by Spotless to 1.18.1
    - Increase AWS SDK to 2.28.2
    - Add missing SSM gradle configuration
    - Correct static method DataStore.getClient() having a hidden DynamoDbEnhancedClient builder and not using the DynamoDbEnhancedClient provided by the clientProviderFactory
    - Mark DataStore.getClient() as deprecated for removal as the approach leads to a client per datastore and prevents sharing a single DynamoDbEnhancedClient. 

## 3.1.2
    - Refactor getClaimsForUser to allow single parameter
    - Removed original getClaimsForUser, use new getClaimsForUser
    - Removed getClaimsForUserWithEvidenceRequested, use new getClaimsForUser
    - Added new commonsstep for user with a context


## 3.1.1
    - Updated drivingPermit model to include fullAddress
    - Updated PersonIdentity to include DrivingPermit
    - Updated SharedClaims to include DrivingPermit
    - Created PersonIdentityDrivingPermit Dynamo Bean and added this to personIdentity for mapping purposes
    - Updated PersonIdentityMapper to map drivingPermit in shared claims to the PersonIdentity

## 3.1.0
    Added context field to SessionRequest and SessionItem. 

## 3.0.6
    Added a new counterMetric method that accepts a Unit as an additional parameter.

## 3.0.5
    Amend SignedJWTFactory generateHeaders: 
    - Changed character prior to 'unique key identifier' from colon to hash so it is now {DID-method-specific identifier}#{unique key identifier}
    - Strip https:// from the start of the issuer if present

## 3.0.4
    Added methods to SignedJWTFactory to allow CRIs to add kid to jwt headers

## 3.0.3

    Added a overrideJti method to allow setting jti claims value in Contract-tests
## 3.0.2

    Changed getSecretsManagerClient() to public in  ClientProviderFactory, inadvertently set as private when first added.

## 3.0.1

    Remove versions specified for the main Jackson dependencies and defer to the ones pull in from `software.amazon.awssdk:bom`. There is a breakage in a later jackson version, which crashes in the sdk classes which expect the older versions. 
    Note : jackson-datatype-jsr310 and jackson-datatype-jdk8 are not in the aws sdk and are custom dependencies these version have been set at the aws pom versions to avoid a mismatch.

## 3.0.0
 ***BREAKING CHANGES***
 
 Removal of default constuctors for
 - ConfigurationService
 - SessionService
 - AccessTokenService
 - AuditService

 Removed feature flags set using parameter store variables (EXPIRY_REMOVED/ CONTAINS_UNIQUE_ID) - to remove some parameter reads
 - Now set via ENV vars `ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED` / `ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID` on the VC generating lambda - fallback behavour is unchanged

Exclude the netty-nio-client and apache-client now that the AwsCrtHttpClient is the only client in use.
- This exclusion needs replicated in any Java Lambdas to exclude these clients from the build step and reduce deployment size a small amount and improve lambda initialization. 
Note: Aws service clients create in a CRI without a http client configured should use the ClientProviderFactory and not rely on the class path to resolve a http client - as there can be more than one http client on the classpath.

**CHANGES**

Add ClientProviderFactory to enable a single source of clients for services.
The AwsCrtHttpClient used as the sdkHttpClient in all clients, with clients provided by this factory are pre-configured with awsRegion, sdkHttpClient (AwsCrtHttpClient), credentialsProvider and defaultsMode to reduce lambda startup time.

Reworked constructors of ConfigurationService, SessionService, AccessTokenService and AuditService to require reusing already constucted AWS Clients, ConfigurationService's and other parameters.

SSMProvider and SecretsProvider now have a random default max age configured at creation - to mitigate muliple scaled lambdas expiring caches at the same moment and hitting rate limits. Default is a random value between 5 - 15 mins. Optionally configurable per lambda by the env vars `CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MIN_MINUTES` and `CONFIG_SERVICE_SSM_OPTIMIZED_CACHE_AGE_MAX_MINUTES`.

**Dependency Changes**

AWS SDK 2.20.162 -> 2.26.16

New Aws Crt Http Client aligned with AWS SDK 2.26.16

AWS Lambda Events 3.11.0 -> 3.11.6

AWS Lambda Powertools 1.12.0 -> 1.18.0

Nimbusds Oauth 11.2 -> 11.4

## 2.3.0
Added requested_verification_score from evidenceRequested to the logger in the SessionService

## 2.2.4
* Merged dependabot PRs

## 2.2.3
* Added evidence request parameters to the shared claims endpoint to use verificationScore and strengthPolicy

## 2.2.2
Testing of the automated publishing to Maven - no new changes

## 2.2.1

Fix Javadoc warnings

## 2.2.0

* Made the SessionItem be able to access the `evidence_request` field 

## 2.1.0

* Made the SQS helper test util work better with shared queues and multiple tests running at the same time

## 2.0.0

* Added support for National Insurance number as SocialSecurityRecord
* Bumped version to 2.0.0 following change to remove public constructor on ListUtil

## 1.7.0

### Added utilities for integration testing with AWS services

* Added a CloudFormation helper to retrieve information about stacks, such as output values
* Added an SQS helper to read and delete messages from SQS queues

### Added tests and new methods to ListUtil

* Added a method to split a list into batches
* Added methods to merge or exclude items from lists using custom comparators

## 1.6.2

* Created new step definition for posting session request to include new `txma-audit-encoded` header to be referenced in the address repository tests.

## 1.6.1

* Update README

## 1.6.0

* Append `device_information` to `restricted` if the `txma-audit-encoded` header is present

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
