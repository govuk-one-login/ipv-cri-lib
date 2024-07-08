# Credential Issuer common libraries

A library of common functions that can be used across credential issuers for validation, event transmission, describing common data etc.

## Releasing CRI libs to Maven Central

Update the `buildVersion` in `/build.gradle` to reflect the latest major, minor or patch version of this library and then update the [RELEASE_NOTES](./RELEASE_NOTES.md).

If this version is distinct, it should be deployed to Maven Central at [uk.gov.account:cri-common-lib](https://search.maven.org/artifact/uk.gov.account/cri-common-lib). Note that this is a manual process and needs be done using the `pipeline-cri-libs-to-maven-central` pipeline in the `di-tools-dev` account, see [docs](https://govukverify.atlassian.net/wiki/spaces/OJ/pages/3357605906/di-ipv-cri-lib+deployment+to+maven+central) for more info.

## Testing local changes to cri_common_lib with a dev CRI stack

In cri-common-lib:
- Update the `buildVersion` in `/build.gradle` to avoid any potential caching issues.
- Comment out `sign publishing.publications` in `/build.gradle`
- run `./gradlew publishToMavenLocal`

In your cri repo:
- Update the `dependencyVersions` section for `cri_common_lib` to the same version set above in `/build.gradle`
- Add `mavenLocal()` to the `repositories` sections in `/build.gradle` (In kbv for example there are 2 of these to update). Add it above the existing `maven` field.

You should now be able to build your cri stack and deploy to AWS with your local cri_common_lib changes.
