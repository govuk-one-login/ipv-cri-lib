# Credential Issuer common libraries

A library of common functions that can be used across credential issuers for validation, event transmission, describing common data etc.

## Releasing CRI libs to Maven Central

Update the `buildVersion` in `/build.gradle` to reflect the latest major, minor or patch version of this library and then update the [RELEASE_NOTES](./RELEASE_NOTES.md).

If this version is distinct, it should be deployed to Maven Central at [uk.gov.account:cri-common-lib](https://search.maven.org/artifact/uk.gov.account/cri-common-lib). Note that this is a manual process and needs be done using the `pipeline-cri-libs-to-maven-central` pipeline in the `di-tools-dev` account, see [docs](https://govukverify.atlassian.net/wiki/spaces/OJ/pages/3357605906/di-ipv-cri-lib+deployment+to+maven+central) for more info.

test to remove
