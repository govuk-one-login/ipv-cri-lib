package uk.gov.di.ipv.cri.common.library.util;

import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.di.ipv.cri.common.library.domain.personidentity.PersonIdentityDetailed;
import uk.gov.di.ipv.cri.common.library.service.ConfigurationService;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import java.text.ParseException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static com.nimbusds.jwt.JWTClaimNames.EXPIRATION_TIME;
import static com.nimbusds.jwt.JWTClaimNames.NOT_BEFORE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder.ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID;
import static uk.gov.di.ipv.cri.common.library.util.VerifiableCredentialClaimsSetBuilder.ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED;

@ExtendWith(MockitoExtension.class)
@ExtendWith(SystemStubsExtension.class)
class VerifiableCredentialClaimsSetBuilderTest {
    private static final String TEST_SUBJECT = UUID.randomUUID().toString();
    private static final String TEST_ISSUER = "kbv-issuer";
    private static final String TEST_VC_TYPE = "IdentityCheckCredential";
    private static final PersonIdentityDetailed TEST_PERSON_IDENTITY =
            mock(PersonIdentityDetailed.class);

    // Needs to be created here
    @SystemStub private EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Mock private ConfigurationService mockConfigurationService;
    private Clock clock;
    private VerifiableCredentialClaimsSetBuilder builder;

    @BeforeEach
    void setup() {
        clock = Clock.fixed(Instant.parse("2016-11-03T10:15:30Z"), ZoneId.of("UTC"));

        // Defaults
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED, null);
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, null);

        builder = new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);
    }

    @Test
    void shouldBuildVerifiableCredentialWhenVcExpiryRemovedReleaseFlagIsNotSpecified()
            throws ParseException {
        String[] testContexts = new String[] {"context1", "context2"};
        Map<String, String> evidence =
                Map.of(
                        "evidence-key-1", "evidence-value-1",
                        "evidence-key-2", "evidence-value-2");

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                this.builder
                        .subject(TEST_SUBJECT)
                        .timeToLive(6L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .verifiableCredentialContext(testContexts)
                        .verifiableCredentialEvidence(evidence)
                        .build();

        assertNotNull(builtClaimSet);
        assertEquals(TEST_SUBJECT, builtClaimSet.getSubject());
        assertEquals(TEST_ISSUER, builtClaimSet.getIssuer());
        assertEquals(this.clock.instant().getEpochSecond(), builtClaimSet.getLongClaim(NOT_BEFORE));
        assertTrue(
                builtClaimSet.getLongClaim(EXPIRATION_TIME)
                        > this.clock.instant().getEpochSecond());
        assertEquals(
                TEST_PERSON_IDENTITY,
                builtClaimSet.getJSONObjectClaim("vc").get("credentialSubject"));
        assertArrayEquals(
                new String[] {"VerifiableCredential", TEST_VC_TYPE},
                (String[]) builtClaimSet.getJSONObjectClaim("vc").get("type"));
        assertEquals(testContexts, builtClaimSet.getJSONObjectClaim("vc").get("@context"));
        assertEquals(evidence, builtClaimSet.getJSONObjectClaim("vc").get("evidence"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "false", "anything"})
    void shouldBuildVerifiableCredential(String expiryRemovedReleasedFlagValue)
            throws ParseException {

        environmentVariables.set(
                ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED, expiryRemovedReleasedFlagValue);
        builder = new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);

        String[] testContexts = new String[] {"context1", "context2"};
        Map<String, String> evidence =
                Map.of(
                        "evidence-key-1", "evidence-value-1",
                        "evidence-key-2", "evidence-value-2");

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                this.builder
                        .subject(TEST_SUBJECT)
                        .timeToLive(6L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .verifiableCredentialContext(testContexts)
                        .verifiableCredentialEvidence(evidence)
                        .build();

        assertNotNull(builtClaimSet);
        assertEquals(TEST_SUBJECT, builtClaimSet.getSubject());
        assertEquals(TEST_ISSUER, builtClaimSet.getIssuer());
        assertEquals(this.clock.instant().getEpochSecond(), builtClaimSet.getLongClaim(NOT_BEFORE));
        assertTrue(
                builtClaimSet.getLongClaim(EXPIRATION_TIME)
                        > this.clock.instant().getEpochSecond());
        assertEquals(
                TEST_PERSON_IDENTITY,
                builtClaimSet.getJSONObjectClaim("vc").get("credentialSubject"));
        assertArrayEquals(
                new String[] {"VerifiableCredential", TEST_VC_TYPE},
                (String[]) builtClaimSet.getJSONObjectClaim("vc").get("type"));
        assertEquals(testContexts, builtClaimSet.getJSONObjectClaim("vc").get("@context"));
        assertEquals(evidence, builtClaimSet.getJSONObjectClaim("vc").get("evidence"));
    }

    @Test
    void shouldBuildVerifiableCredentialWithoutAnExpiryTime() throws ParseException {
        String[] testContexts = new String[] {"context1", "context2"};

        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED, true);
        builder = new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);

        Map<String, String> evidence =
                Map.of(
                        "evidence-key-1", "evidence-value-1",
                        "evidence-key-2", "evidence-value-2");

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                builder.subject(TEST_SUBJECT)
                        .timeToLive(6L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .verifiableCredentialContext(testContexts)
                        .verifiableCredentialEvidence(evidence)
                        .build();

        assertNotNull(builtClaimSet);
        assertEquals(TEST_SUBJECT, builtClaimSet.getSubject());
        assertEquals(TEST_ISSUER, builtClaimSet.getIssuer());
        assertEquals(this.clock.instant().getEpochSecond(), builtClaimSet.getLongClaim(NOT_BEFORE));
        assertEquals(
                TEST_PERSON_IDENTITY,
                builtClaimSet.getJSONObjectClaim("vc").get("credentialSubject"));
        assertArrayEquals(
                new String[] {"VerifiableCredential", TEST_VC_TYPE},
                (String[]) builtClaimSet.getJSONObjectClaim("vc").get("type"));
        assertEquals(testContexts, builtClaimSet.getJSONObjectClaim("vc").get("@context"));
        assertEquals(evidence, builtClaimSet.getJSONObjectClaim("vc").get("evidence"));

        assertNull(builtClaimSet.getLongClaim(EXPIRATION_TIME));
    }

    @Test
    void shouldOverrideJtiWhenContainUniqueIdIsFalse() {
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, "override");
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        var claimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock)
                        .subject(TEST_SUBJECT)
                        .timeToLive(1L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY);

        var originalBuilder = claimsSetBuilder.build();

        claimsSetBuilder.overrideJti("dummyJti");
        claimsSetBuilder.build();

        assertNotEquals(originalBuilder.getJWTID(), claimsSetBuilder.build().getJWTID());
        assertEquals(null, originalBuilder.getJWTID());

        assertEquals("dummyJti", claimsSetBuilder.build().getJWTID());
    }

    @Test
    void cannotOverrideJtiWhenContainUniqueIdIsFalse() {
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, false);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        var claimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock)
                        .subject(TEST_SUBJECT)
                        .timeToLive(1L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY);

        var originalBuilder = claimsSetBuilder.build();

        claimsSetBuilder.overrideJti("dummyJti");
        claimsSetBuilder.build();

        assertNull(claimsSetBuilder.build().getJWTID());
        assertEquals(null, originalBuilder.getJWTID());
        assertEquals(null, claimsSetBuilder.build().getJWTID());
    }

    @Test
    void cannotOverrideJtiWhenContainUniqueIdIsTrue() {
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, true);
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        var claimsSetBuilder =
                new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock)
                        .subject(TEST_SUBJECT)
                        .timeToLive(1L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY);

        var originalClaimsSet = claimsSetBuilder.build();

        claimsSetBuilder.overrideJti("dummyJti");
        var newClaimsSet = claimsSetBuilder.build();

        assertNotEquals("dummyJti", newClaimsSet.getJWTID());
        assertNotEquals(originalClaimsSet.getJWTID(), newClaimsSet.getJWTID());
        assertNotNull(originalClaimsSet.getJWTID());
        assertNotNull(newClaimsSet.getJWTID());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "false", "anything"})
    void shouldBuildVerifiableCredentialWhenVcContainsUniqueIdReleaseFlagIsNotSpecified(
            String expiryRemovedReleasedFlagValue) throws ParseException {

        environmentVariables.set(
                ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED, expiryRemovedReleasedFlagValue);
        builder = new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);

        String[] testContexts = new String[] {"context1", "context2"};
        Map<String, String> evidence = Collections.emptyMap();

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                this.builder
                        .subject(TEST_SUBJECT)
                        .timeToLive(6L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .verifiableCredentialContext(testContexts)
                        .verifiableCredentialEvidence(evidence)
                        .build();

        assertNotNull(builtClaimSet);
        assertEquals(TEST_SUBJECT, builtClaimSet.getSubject());
        assertEquals(TEST_ISSUER, builtClaimSet.getIssuer());
        assertEquals(this.clock.instant().getEpochSecond(), builtClaimSet.getLongClaim(NOT_BEFORE));
        assertTrue(
                builtClaimSet.getLongClaim(EXPIRATION_TIME)
                        > this.clock.instant().getEpochSecond());
        assertEquals(
                TEST_PERSON_IDENTITY,
                builtClaimSet.getJSONObjectClaim("vc").get("credentialSubject"));
        assertArrayEquals(
                new String[] {"VerifiableCredential", TEST_VC_TYPE},
                (String[]) builtClaimSet.getJSONObjectClaim("vc").get("type"));
        assertEquals(testContexts, builtClaimSet.getJSONObjectClaim("vc").get("@context"));
        assertEquals(evidence, builtClaimSet.getJSONObjectClaim("vc").get("evidence"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "false", "anything"})
    void shouldBuildVerifiableCredentialWhenVcContainsUniqueIdReleaseFlagIsSpecified(
            String expiryRemovedReleasedFlagValue) throws ParseException {

        environmentVariables.set(
                ENV_VAR_FEATURE_FLAG_VC_EXPIRY_REMOVED, expiryRemovedReleasedFlagValue);
        environmentVariables.set(ENV_VAR_FEATURE_FLAG_VC_CONTAINS_UNIQUE_ID, true);
        builder = new VerifiableCredentialClaimsSetBuilder(mockConfigurationService, clock);

        String[] testContexts = new String[] {"context1", "context2"};
        Map<String, String> evidence = Collections.emptyMap();

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                this.builder
                        .subject(TEST_SUBJECT)
                        .timeToLive(6L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .verifiableCredentialContext(testContexts)
                        .verifiableCredentialEvidence(evidence)
                        .build();

        assertEquals(TEST_SUBJECT, builtClaimSet.getSubject());
        assertEquals(TEST_ISSUER, builtClaimSet.getIssuer());
        assertEquals(this.clock.instant().getEpochSecond(), builtClaimSet.getLongClaim(NOT_BEFORE));
        assertTrue(
                builtClaimSet.getLongClaim(EXPIRATION_TIME)
                        > this.clock.instant().getEpochSecond());
        assertEquals(
                TEST_PERSON_IDENTITY,
                builtClaimSet.getJSONObjectClaim("vc").get("credentialSubject"));
        assertArrayEquals(
                new String[] {"VerifiableCredential", TEST_VC_TYPE},
                (String[]) builtClaimSet.getJSONObjectClaim("vc").get("type"));
        assertEquals(testContexts, builtClaimSet.getJSONObjectClaim("vc").get("@context"));
        assertEquals(evidence, builtClaimSet.getJSONObjectClaim("vc").get("evidence"));
        assertNotNull(builtClaimSet.getJWTID());
        assertThrows(
                IllegalArgumentException.class,
                () -> getUuidString("urn:uuid:this-does-not-look-like-a-uuid"));
        assertTrue(builtClaimSet.getJWTID().contains("urn:uuid:"));
        assertJWTClaimsSetContainsAnIdentifierSimilarToAUuid(builtClaimSet);
    }

    private static void assertJWTClaimsSetContainsAnIdentifierSimilarToAUuid(
            JWTClaimsSet builtClaimSet) {
        assertTrue(builtClaimSet.getJWTID().contains(getUuidString(builtClaimSet.getJWTID())));
    }

    private static String getUuidString(String jwtId) {
        return UUID.fromString(jwtId.split(":")[2]).toString();
    }

    @Test
    void shouldBuildVerifiableCredentialWithoutContextAndEvidence() throws ParseException {
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                this.builder
                        .subject(TEST_SUBJECT)
                        .timeToLive(1L, ChronoUnit.MONTHS)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .build();

        assertNotNull(builtClaimSet);
        assertEquals(TEST_SUBJECT, builtClaimSet.getSubject());
        assertEquals(TEST_ISSUER, builtClaimSet.getIssuer());
        assertEquals(this.clock.instant().getEpochSecond(), builtClaimSet.getLongClaim(NOT_BEFORE));
        assertTrue(
                builtClaimSet.getLongClaim(EXPIRATION_TIME)
                        > this.clock.instant().getEpochSecond());
        assertEquals(
                TEST_PERSON_IDENTITY,
                builtClaimSet.getJSONObjectClaim("vc").get("credentialSubject"));
        assertArrayEquals(
                new String[] {"VerifiableCredential", TEST_VC_TYPE},
                (String[]) builtClaimSet.getJSONObjectClaim("vc").get("type"));

        assertNull(builtClaimSet.getJSONObjectClaim("vc").get("@context"));
        assertNull(builtClaimSet.getJSONObjectClaim("vc").get("evidence"));
    }

    @ParameterizedTest
    @CsvSource({
        "66,SECONDS,2016-11-03T10:16:36Z",
        "30,MINUTES,2016-11-03T10:45:30Z",
        "4,HOURS,2016-11-03T14:15:30Z",
        "2,DAYS,2016-11-05T10:15:30Z",
        "1,MONTHS,2016-12-03T10:15:30Z",
        "2,YEARS,2018-11-03T10:15:30Z"
    })
    void shouldBuildVerifiableCredentialWithAppropriateExpirationTime(
            long ttl, ChronoUnit ttlUnit, String expectedExpirationTime) throws ParseException {

        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);

        JWTClaimsSet builtClaimSet =
                this.builder
                        .subject(TEST_SUBJECT)
                        .timeToLive(ttl, ttlUnit)
                        .verifiableCredentialType(TEST_VC_TYPE)
                        .verifiableCredentialSubject(TEST_PERSON_IDENTITY)
                        .build();

        assertEquals(
                builtClaimSet.getLongClaim(EXPIRATION_TIME),
                Instant.parse(expectedExpirationTime).getEpochSecond());
    }

    @Test
    void shouldThrowErrorWhenVerifiableCredentialTypeNotSet() {
        IllegalStateException thrownException =
                assertThrows(IllegalStateException.class, () -> this.builder.build());
        assertEquals(
                "The verifiable credential type must be specified", thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenSubjectNotSet() {
        this.builder.verifiableCredentialType("IdentityCheckCredential");
        IllegalStateException thrownException =
                assertThrows(IllegalStateException.class, () -> this.builder.build());
        assertEquals("The subject must be specified", thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenVerifiableCredentialSubjectNotSet() {
        this.builder.verifiableCredentialType("IdentityCheckCredential").subject("test-subject");
        IllegalStateException thrownException =
                assertThrows(IllegalStateException.class, () -> this.builder.build());
        assertEquals(
                "The verifiable credential subject must be specified",
                thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenIssuerNotSet() {
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("");

        this.builder
                .verifiableCredentialType("IdentityCheckCredential")
                .verifiableCredentialSubject(mock(PersonIdentityDetailed.class))
                .subject("test-subject");

        IllegalStateException thrownException =
                assertThrows(IllegalStateException.class, () -> this.builder.build());
        assertEquals(
                "An empty/null verifiable credential issuer was retrieved from configuration",
                thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenJwtTtlNotSet() {
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn("issuer");
        this.builder
                .verifiableCredentialType("IdentityCheckCredential")
                .verifiableCredentialSubject(mock(PersonIdentityDetailed.class))
                .subject("test-subject");

        IllegalStateException thrownException =
                assertThrows(IllegalStateException.class, () -> this.builder.build());
        assertEquals(
                "An invalid verifiable credential time-to-live was encountered",
                thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenInvalidTtlSupplied() {
        IllegalArgumentException thrownException =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> this.builder.timeToLive(0L, ChronoUnit.DAYS));
        assertEquals("ttl must be greater than zero", thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenInvalidTtlUnitSupplied() {
        NullPointerException thrownException =
                assertThrows(NullPointerException.class, () -> this.builder.timeToLive(1L, null));
        assertEquals("ttlUnit must not be null", thrownException.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowErrorWhenInvalidSubjectSupplied(String testSubject) {
        IllegalArgumentException thrownException =
                assertThrows(
                        IllegalArgumentException.class, () -> this.builder.subject(testSubject));
        assertEquals("The subject must not be null or empty.", thrownException.getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void shouldThrowErrorWhenInvalidTypeSupplied(String testVerifiableCredential) {
        IllegalArgumentException thrownException =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> this.builder.verifiableCredentialType(testVerifiableCredential));
        assertEquals(
                "The verifiable credential type must not be null or empty.",
                thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenNullVerifiableCredentialSubjectSupplied() {
        NullPointerException thrownException =
                assertThrows(
                        NullPointerException.class,
                        () -> this.builder.verifiableCredentialSubject(null));
        assertEquals("subject must not be null", thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenNullVerifiableCredentialContextSupplied() {
        NullPointerException thrownException =
                assertThrows(
                        NullPointerException.class,
                        () -> this.builder.verifiableCredentialContext(null));
        assertEquals("contexts must not be null", thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenNullVerifiableCredentialEvidenceSupplied() {
        NullPointerException thrownException =
                assertThrows(
                        NullPointerException.class,
                        () -> this.builder.verifiableCredentialEvidence(null));
        assertEquals("evidence must not be null", thrownException.getMessage());
    }

    @Test
    void shouldThrowErrorWhenInvalidTimeToLiveUnitSupplied() {
        when(mockConfigurationService.getVerifiableCredentialIssuer()).thenReturn(TEST_ISSUER);
        this.builder
                .subject(TEST_SUBJECT)
                .timeToLive(5L, ChronoUnit.MILLIS)
                .verifiableCredentialType(TEST_VC_TYPE)
                .verifiableCredentialSubject(TEST_PERSON_IDENTITY);
        IllegalStateException thrownException =
                assertThrows(IllegalStateException.class, () -> this.builder.build());
        assertEquals(
                "Unexpected time-to-live unit encountered: Millis", thrownException.getMessage());
    }
}
