# Credential Issuer common libraries Release Notes

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
