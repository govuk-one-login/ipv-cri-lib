# Credential Issuer common libraries

A library of common functions that can be used across credential issuers for validation, event transmission, describing common data etc.

## Publishing to Maven Central

To publish a new version to Maven Central, follow these steps:

1. **Bump the version**<br>
   Create a pull request that updates the `buildVersion` in `build.gradle`.

2. **Merge the PR**<br>
   Once approved, merge the pull request into the `main` branch.

3. **Create a new GitHub release**<br>
   Go to [Create new release](https://github.com/govuk-one-login/ipv-cri-lib/releases/new) and:
    * Set the tag name to match the version from `build.gradle`, prefixed with v (e.g. v1.0.0)
    * Use the same value for the release title (e.g. v1.0.0)
    * Click "Generate release notes" and edit them for clarity and readability
    * Click "Publish release"

4. **Trigger the Maven publish action**<br>
   Publishing will start automatically via the [Publish to Maven Central](https://github.com/govuk-one-login/ipv-cri-lib/actions/workflows/publish-to-maven.yml) GitHub Action.

5. **Verify the release**<br>
   It may take up to 15 minutes for the new version to appear on [Maven Central](https://central.sonatype.com/artifact/uk.gov.account/cri-common-lib).

## Testing local changes to cri_common_lib with a dev CRI stack

In cri-common-lib:
- Update the `buildVersion` in `/build.gradle` to avoid any potential caching issues.
- Comment out `sign publishing.publications` in `/build.gradle`
- run `./gradlew publishToMavenLocal`

In your cri repo:
- Update the `dependencyVersions` section for `cri_common_lib` to the same version set above in `/build.gradle`
- Add `mavenLocal()` to the `repositories` sections in `/build.gradle` (In kbv for example there are 2 of these to update). Add it above the existing `maven` field.

You should now be able to build your cri stack and deploy to AWS with your local cri_common_lib changes.
